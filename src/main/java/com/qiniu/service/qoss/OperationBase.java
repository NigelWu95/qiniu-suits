package com.qiniu.service.qoss;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.qiniu.persistence.FileMap;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.*;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;
import com.qiniu.util.HttpResponseUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class OperationBase implements ILineProcess<Map<String, String>>, Cloneable {

    final protected String accessKey;
    final protected String secretKey;
    final protected Configuration configuration;
    protected BucketManager bucketManager;
    final protected String bucket;
    final protected String processName;
    protected int retryCount;
    protected volatile BatchOperations batchOperations;
    protected volatile List<String> errorLineList;
    final protected String savePath;
    protected String saveTag;
    protected int saveIndex;
    protected FileMap fileMap;

    public OperationBase(String processName, String accessKey, String secretKey, Configuration configuration,
                         String bucket, String savePath, int saveIndex) throws IOException {
        this.processName = processName;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.configuration = configuration;
        this.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        this.bucket = bucket;
        this.batchOperations = new BatchOperations();
        this.errorLineList = new ArrayList<>();
        this.savePath = savePath;
        this.saveTag = "";
        this.saveIndex = saveIndex;
        this.fileMap = new FileMap(savePath, processName, String.valueOf(saveIndex));
        this.fileMap.initDefaultWriters();
    }

    public String getProcessName() {
        return this.processName;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount < 1 ? 1 : retryCount;
    }

    public void setSaveTag(String saveTag) {
        this.saveTag = saveTag == null ? "" : saveTag;
    }

    public OperationBase clone() throws CloneNotSupportedException {
        OperationBase operationBase = (OperationBase)super.clone();
        operationBase.bucketManager = new BucketManager(Auth.create(accessKey, secretKey), configuration);
        operationBase.batchOperations = new BatchOperations();
        operationBase.errorLineList = new ArrayList<>();
        operationBase.fileMap = new FileMap(savePath, processName, saveTag + String.valueOf(++saveIndex));
        try {
            operationBase.fileMap.initDefaultWriters();
        } catch (IOException e) {
            throw new CloneNotSupportedException("init writer failed.");
        }
        return operationBase;
    }

    // 实现从 fileInfoList 转换得到 batchOperations 时先清除 batchOperations 中可能存在的上次的内容
    protected abstract BatchOperations getOperations(List<Map<String, String>> lineList);

    // 获取输入行中的关键参数，将其保存到对应结果的行当中，方便确定对应关系和失败重试
    protected abstract String getInputParams(Map<String, String> line);

    public void parseBatchResult(List<Map<String, String>> processList, String result) throws IOException {
        if (result == null || "".equals(result)) throw new QiniuException(null, "not valid json.");
        JsonArray jsonArray;
        try {
            jsonArray = new Gson().fromJson(result, JsonArray.class);
        } catch (JsonParseException e) {
            throw new QiniuException(null, "parse to json array error.");
        }
        JsonObject jsonObject;
        for (int j = 0; j < processList.size(); j++) {
            jsonObject = jsonArray.get(j).getAsJsonObject();
            if (j < jsonArray.size()) {
                if (jsonObject.get("code").getAsInt() == 200)
                    fileMap.writeSuccess(getInputParams(processList.get(j)) + "\t" + jsonObject, false);
                else
                    fileMap.writeError(getInputParams(processList.get(j)) + "\t" + jsonObject, false);
            } else {
                fileMap.writeError(getInputParams(processList.get(j)) + "\tempty result", false);
            }
        }
    }

    public void processLine(List<Map<String, String>> lineList, int retryCount) throws IOException {
        int times = lineList.size()/1000 + 1;
        List<Map<String, String>> processList;
        Response response;
        String result;
        int retry;
        for (int i = 0; i < times; i++) {
            processList = lineList.subList(1000 * i, i == times - 1 ? lineList.size() : 1000 * (i + 1));
            if (processList.size() > 0) {
                batchOperations = getOperations(processList);
                retry = retryCount;
                while (retry > 0) {
                    try {
                        response = bucketManager.batch(batchOperations);
                        result = HttpResponseUtils.getResult(response);
                        parseBatchResult(processList, result);
                        retry = 0;
                    } catch (QiniuException e) {
                        retry--;
                        HttpResponseUtils.processException(e, retry, fileMap,
                                processList.stream().map(this::getInputParams).collect(Collectors.toList()));
                    }
                }
            }
        }
        if (errorLineList.size() > 0) {
            fileMap.writeError(String.join("\n", errorLineList), false);
            errorLineList.clear();
        }
    }

    public void processLine(List<Map<String, String>> lineList) throws IOException {
        processLine(lineList, retryCount);
    }

    public void closeResource() {
        fileMap.closeWriters();
    }
}
