package com.qiniu.service.media;

import com.google.gson.JsonObject;
import com.qiniu.config.JsonFile;
import com.qiniu.model.media.Avinfo;
import com.qiniu.model.media.VideoStream;
import com.qiniu.persistence.FileMap;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.util.FileNameUtils;
import com.qiniu.util.JsonConvertUtils;
import com.qiniu.util.UrlSafeBase64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PfopCommand implements ILineProcess<Map<String, String>>, Cloneable {

    private String processName;
    private MediaManager mediaManager;
    private String resultPath;
    private String resultTag;
    private int resultIndex;
    private FileMap fileMap;
    private boolean hasDuration;
    private boolean hasSize;
    private List<JsonObject> pfopConfigs = new ArrayList<>();

    public PfopCommand(String jsonPath, boolean hasDuration, boolean hasSize, String resultPath, int resultIndex)
            throws IOException {
        JsonFile jsonFile = new JsonFile(jsonPath);
        for (String key : jsonFile.getConfigKeys()) {
            JsonObject jsonObject = jsonFile.getElement(key).getAsJsonObject();
            List<Integer> scale = JsonConvertUtils.fromJsonArray(jsonObject.get("scale").getAsJsonArray());
            if (scale.size() < 1) throw new IOException(jsonPath + " miss the scale field in \"" + key + "\"");
            else if (scale.size() == 1) scale.add(Integer.MAX_VALUE);
            if (!jsonObject.keySet().contains("cmd") || !jsonObject.keySet().contains("saveas"))
                throw new IOException(jsonPath + " miss the \"cmd\" or \"saveas\" fields in \"" + key + "\"");
            else if (!jsonObject.get("saveas").getAsString().contains(":"))
                throw new IOException(jsonPath + " miss the <bucket> field of \"saveas\" field in \"" + key + "\"");
            jsonObject.addProperty("name", key);
            pfopConfigs.add(jsonObject);
        }
        this.processName = "pfopcmd";
        this.mediaManager = new MediaManager();
        this.hasDuration = hasDuration;
        this.hasSize = hasSize;
        this.resultPath = resultPath;
        this.resultTag = "";
        this.resultIndex = resultIndex;
        this.fileMap = new FileMap(resultPath, processName, String.valueOf(resultIndex));
        this.fileMap.initDefaultWriters();
    }

    public PfopCommand(String jsonPath, boolean hasDuration, boolean hasSize, String resultPath) throws IOException {
        this(jsonPath, hasDuration, hasSize, resultPath, 0);
    }

    public String getProcessName() {
        return this.processName;
    }

    public void setResultTag(String resultTag) {
        this.resultTag = resultTag == null ? "" : resultTag;
    }

    public PfopCommand clone() throws CloneNotSupportedException {
        PfopCommand pfopCommand = (PfopCommand)super.clone();
        pfopCommand.fileMap = new FileMap(resultPath, processName, resultTag + String.valueOf(++resultIndex));
        try {
            pfopCommand.fileMap.initDefaultWriters();
        } catch (IOException e) {
            throw new CloneNotSupportedException("init writer failed.");
        }
        return pfopCommand;
    }

    private String generateFopCmd(String srcKey, JsonObject pfopJson) throws IOException {
        String saveAs = pfopJson.get("saveas").getAsString();
        String saveAsKey = saveAs.substring(saveAs.indexOf(":"));
        if (saveAsKey.contains("$(key)")) {
            if (saveAsKey.contains(".")) {
                String[] nameParts = saveAsKey.split("(\\$\\(key\\)|\\.)");
                saveAsKey = FileNameUtils.addPrefixAndSuffixWithExt(srcKey, nameParts[0], nameParts[1], nameParts[2]);
            } else {
                String[] nameParts = saveAsKey.split("\\$\\(key\\)");
                saveAsKey = FileNameUtils.addPrefixAndSuffixKeepExt(srcKey, nameParts[0], nameParts[1]);
            }
            saveAs = saveAs.replace(saveAs.substring(saveAs.indexOf(":")), saveAsKey);
        }
        return pfopJson.get("cmd").getAsString() + "|saveas/" + UrlSafeBase64.encodeToString(saveAs);
    }

    public void processLine(List<Map<String, String>> lineList) throws IOException {
        String key;
        String info;
        Avinfo avinfo;
        StringBuilder other = new StringBuilder("");
        VideoStream videoStream;
        List<Integer> scale;
        for (JsonObject pfopConfig : pfopConfigs) {
            scale = JsonConvertUtils.fromJsonArray(pfopConfig.get("scale").getAsJsonArray());
            List<String> commandList = new ArrayList<>();
            for (Map<String, String> line : lineList) {
                key = line.get("key");
                info = line.get("avinfo");
                if (key == null || "".equals(key) || info == null || "".equals(info))
                    throw new IOException("target value is empty.");
                try {
                    avinfo = mediaManager.getAvinfoByJson(info);
                    if (hasDuration) other.append("\t").append(Double.valueOf(avinfo.getFormat().duration));
                    if (hasSize) other.append("\t").append(Long.valueOf(avinfo.getFormat().size));
                    videoStream = avinfo.getVideoStream();
                    if (videoStream == null) throw new Exception("videoStream is null");
                    if (scale.get(0) > videoStream.width && videoStream.width < scale.get(1)) {
                        commandList.add(key + "\t" + generateFopCmd(key, pfopConfig) + other.toString());
                    }
                } catch (Exception e) {
                    fileMap.writeError(String.valueOf(line) + "\t" + e.getMessage(), false);
                }
            }

            if (commandList.size() > 0)
                fileMap.writeKeyFile(pfopConfig.get("name").getAsString() + resultIndex,
                        String.join("\n", commandList), false);
        }
    }

    public void closeResource() {
        fileMap.closeWriters();
    }
}
