package com.qiniu.process.filtration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.qiniu.config.JsonFile;
import com.qiniu.util.ConvertingUtils;
import com.qiniu.util.JsonUtils;

import java.io.IOException;
import java.util.*;

public abstract class SeniorFilter<T> {

    final private String checkName;
    private Set<String> extMimeList;
    private Set<String> extMimeTypeList;

    public SeniorFilter(String checkName, String configPath, boolean rewrite) throws IOException {
        if (!FilterHelp.checkList.contains(checkName)) throw new IOException("unsupported check operation: " + checkName);
        this.checkName = checkName;
        this.extMimeList = new HashSet<>();
        this.extMimeTypeList = new HashSet<>();
        if (configPath != null && !"".equals(configPath)) {
            JsonFile customJson = new JsonFile(configPath);
            JsonElement jsonElement = customJson.getElement("ext-mime");
            if (jsonElement instanceof JsonArray) this.extMimeTypeList = new HashSet<>(
                    JsonUtils.fromJsonArray(jsonElement.getAsJsonArray(), new TypeToken<List<String>>(){})
            );
        }
        if (checkExtMime() && !rewrite) {
            if (FilterHelp.defaultCheckJson == null) FilterHelp.loadCheckJson();
            JsonObject extMime = FilterHelp.defaultCheckJson.getElement("ext-mime").getAsJsonObject();
            List<String> defaultList = JsonUtils.fromJsonArray(extMime.get("image").getAsJsonArray(),
                    new TypeToken<List<String>>(){});
            defaultList.addAll(JsonUtils.fromJsonArray(extMime.get("audio").getAsJsonArray(),
                    new TypeToken<List<String>>(){}));
            defaultList.addAll(JsonUtils.fromJsonArray(extMime.get("video").getAsJsonArray(),
                    new TypeToken<List<String>>(){}));
            this.extMimeList.addAll(defaultList);
            this.extMimeTypeList.addAll(JsonUtils.fromJsonArray(extMime.get("other").getAsJsonArray(),
                    new TypeToken<List<String>>(){}));
        }
    }

    public boolean checkExtMime() {
        return "ext-mime".equals(checkName);
    }

    public List<T> checkMimeType(List<T> lineList) {
        String key;
        String mime;
        List<T> filteredList = new ArrayList<>();
        for (T line : lineList) {
            try {
                if (line == null) continue;
                key = valueFrom(line, "key");
                if (key.contains(".")) {
                    mime = valueFrom(line, ConvertingUtils.defaultMimeField);
                    if (mime == null) mime = valueFrom(line, "mimeType");
                    if (mime == null) mime = valueFrom(line, "contentType");
                    String finalKeyMimePair = key.substring(key.lastIndexOf(".") + 1) + ":" + mime;
                    if (extMimeList.parallelStream().anyMatch(extMime ->
                            finalKeyMimePair.split("/")[0].equalsIgnoreCase(extMime))) {
                        continue;
                    }
                    if (extMimeTypeList.parallelStream().noneMatch(extMime -> finalKeyMimePair.startsWith(extMime) ||
                            finalKeyMimePair.equalsIgnoreCase(extMime))) {
                        filteredList.add(line);
                    }
                } else {
                    filteredList.add(line);
                }
            } catch (Exception e) {
                filteredList.add(line);
            }
        }
        return filteredList;
    }

    public boolean checkMimeType(T line) {
        String key = null;
        String mime;
        try {
            key = valueFrom(line, "key");
            if (key.contains(".")) {
                mime = valueFrom(line, ConvertingUtils.defaultMimeField);
                if (mime == null) mime = valueFrom(line, "mimeType");
                if (mime == null) mime = valueFrom(line, "contentType");
                String finalKeyMimePair = key.substring(key.lastIndexOf(".") + 1) + ":" + mime;
                if (extMimeList.parallelStream().anyMatch(extMime ->
                        finalKeyMimePair.split("/")[0].equalsIgnoreCase(extMime))) {
                    return false;
                }
                return extMimeTypeList.parallelStream().noneMatch(extMime -> finalKeyMimePair.startsWith(extMime) ||
                        finalKeyMimePair.equalsIgnoreCase(extMime));
            } else {
                return true;
            }
        } catch (Exception e) {
            return key != null;
        }
    }

    protected abstract String valueFrom(T item, String key);
}
