package com.qiniu.entry;

import com.qiniu.common.*;
import com.qiniu.model.parameter.ListBucketParams;
import com.qiniu.model.parameter.ListFieldSaveParams;
import com.qiniu.service.datasource.ListBucket;
import com.qiniu.service.interfaces.IEntryParam;
import com.qiniu.service.interfaces.ILineProcess;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;

import java.util.List;
import java.util.Map;

public class ListBucketEntry {

    public static void run(IEntryParam entryParam) throws Exception {

        ListBucketParams listBucketParams = new ListBucketParams(entryParam);
        String accessKey = listBucketParams.getAccessKey();
        String secretKey = listBucketParams.getSecretKey();
        String bucket = listBucketParams.getBucket();
        boolean multiStatus = listBucketParams.getMultiStatus();
        int maxThreads = listBucketParams.getMaxThreads();
        int unitLen = listBucketParams.getUnitLen();
        String customPrefix = listBucketParams.getCustomPrefix();
        List<String> antiPrefix = listBucketParams.getAntiPrefix();
        boolean saveTotal = listBucketParams.getSaveTotal();
        String resultFormat = listBucketParams.getResultFormat();
        String resultSeparator = listBucketParams.getResultSeparator();
        String resultFileDir = listBucketParams.getResultFileDir();
        Auth auth = Auth.create(accessKey, secretKey);
        Configuration configuration = new Configuration(Zone.autoZone());
        ILineProcess<Map<String, String>> processor = new ProcessorChoice(entryParam).getFileProcessor();
        ListBucket listBucket = new ListBucket(auth, configuration, bucket, unitLen, maxThreads, customPrefix,
                antiPrefix, 3, resultFileDir);
        listBucket.setSaveTotalOptions(saveTotal, resultFormat, resultSeparator);
        ListFieldSaveParams fieldSaveParams = new ListFieldSaveParams(entryParam);
        if (multiStatus) {
            listBucket.concurrentlyList(maxThreads, fieldSaveParams.getUsedFields(), processor);
        } else {
            listBucket.straightlyList(listBucketParams.getMarker(), listBucketParams.getEnd(),
                    fieldSaveParams.getUsedFields(), processor);
        }
        if (processor != null) processor.closeResource();
    }
}
