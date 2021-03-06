package com.qiniu.process.baidu;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.GeneratePresignedUrlRequest;
import com.qiniu.interfaces.ILineProcess;
import com.qiniu.process.Base;
import com.qiniu.util.CloudApiUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class PrivateUrl extends Base<Map<String, String>> {

    private BosClientConfiguration configuration;
    private int expires;
    private Map<String, String> queries;
    private GeneratePresignedUrlRequest request;
    private BosClient bosClient;
    private ILineProcess<Map<String, String>> nextProcessor;

    public PrivateUrl(String accessKeyId, String accessKeySecret, String bucket, String endpoint, int expires,
                      Map<String, String> queries) {
        super("baiduprivate", accessKeyId, accessKeySecret, bucket);
        this.expires = expires;
        this.queries = queries;
        request = new GeneratePresignedUrlRequest(bucket, "");
        request.setBucketName(bucket);
        request.setKey("");
        request.setExpiration(expires);
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet())
                request.addRequestParameter(entry.getKey(), entry.getValue());
        }
        configuration = new BosClientConfiguration().withEndpoint(endpoint).withCredentials(
                new DefaultBceCredentials(accessKeyId, accessKeySecret));
        bosClient = new BosClient(configuration);
        try {
            CloudApiUtils.checkBaidu(bosClient);
        } catch (Exception e) {
            bosClient.shutdown();
            throw e;
        }
    }

    public PrivateUrl(String accessKeyId, String accessKeySecret, String bucket, String endpoint, int expires,
                      Map<String, String> queries, String savePath, int saveIndex) throws IOException {
        super("baiduprivate", accessKeyId, accessKeySecret, bucket, savePath, saveIndex);
        this.expires = expires;
        this.queries = queries;
        request = new GeneratePresignedUrlRequest(bucket, "");
        request.setExpiration(expires);
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet())
                request.addRequestParameter(entry.getKey(), entry.getValue());
        }
        configuration = new BosClientConfiguration().withEndpoint(endpoint).withCredentials(
                new DefaultBceCredentials(accessKeyId, accessKeySecret));
        bosClient = new BosClient(configuration);
        try {
            CloudApiUtils.checkBaidu(bosClient);
        } catch (Exception e) {
            bosClient.shutdown();
            throw e;
        }
    }

    public PrivateUrl(String accessKeyId, String accessKeySecret, String bucket, String endpoint, int expires,
                      Map<String, String> queries, String savePath) throws IOException {
        this(accessKeyId, accessKeySecret, bucket, endpoint, expires, queries, savePath, 0);
    }

    @Override
    public void setNextProcessor(ILineProcess<Map<String, String>> nextProcessor) {
        this.nextProcessor = nextProcessor;
        if (nextProcessor != null) processName = String.join("_with_", nextProcessor.getProcessName(), processName);
    }

    @Override
    public PrivateUrl clone() throws CloneNotSupportedException {
        PrivateUrl privateUrl = (PrivateUrl)super.clone();
        privateUrl.request = new GeneratePresignedUrlRequest(bucket, "");
        privateUrl.request.setExpiration(expires);
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet())
                privateUrl.request.addRequestParameter(entry.getKey(), entry.getValue());
        }
        privateUrl.bosClient = new BosClient(configuration);
        try {
            if (nextProcessor != null) privateUrl.nextProcessor = nextProcessor.clone();
            return privateUrl;
        } catch (Exception e) {
            bosClient.shutdown();
            throw e;
        }
    }

    @Override
    public String resultInfo(Map<String, String> line) {
        return line.get("key");
    }

    @Override
    public String singleResult(Map<String, String> line) throws Exception {
        String key = line.get("key");
        if (key == null) throw new IOException("no key in " + line);
        request.setKey(key);
        URL url = bosClient.generatePresignedUrl(request);
        if (nextProcessor != null) {
            line.put("url", url.toString());
            return nextProcessor.processLine(line);
        }
        return String.join("\t", key, url.toString());
    }

    @Override
    public void closeResource() {
        super.closeResource();
        configuration = null;
        queries = null;
        request = null;
        if (bosClient != null) {
            bosClient.shutdown();
            bosClient = null;
        }
        if (nextProcessor != null) nextProcessor.closeResource();
        nextProcessor = null;
    }
}
