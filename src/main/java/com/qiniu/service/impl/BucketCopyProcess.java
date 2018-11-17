package com.qiniu.service.impl;

import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuException;
import com.qiniu.interfaces.IOssFileProcess;
import com.qiniu.service.oss.BucketCopy;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class BucketCopyProcess implements IOssFileProcess, Cloneable {

    private BucketCopy bucketCopy;
    private String resultFileDir;
    private String processName;
    private FileReaderAndWriterMap fileReaderAndWriterMap = new FileReaderAndWriterMap();
    private String srcBucket;
    private String tarBucket;
    private boolean keepKey;
    private String keyPrefix;
    private QiniuException qiniuException = null;

    public BucketCopyProcess(Auth auth, Configuration configuration, String srcBucket, String tarBucket,
                             boolean keepKey, String keyPrefix, String resultFileDir, String processName,
                             int resultFileIndex) throws IOException {
        this.bucketCopy = new BucketCopy(auth, configuration);
        this.resultFileDir = resultFileDir;
        this.processName = processName;
        this.fileReaderAndWriterMap.initWriter(resultFileDir, processName, resultFileIndex);
        this.srcBucket = srcBucket;
        this.tarBucket = tarBucket;
        this.keepKey = keepKey;
        this.keyPrefix = StringUtils.isNullOrEmpty(keyPrefix) ? "" : keyPrefix;
    }

    public BucketCopyProcess(Auth auth, Configuration configuration, String srcBucket, String tarBucket,
                             boolean keepKey, String keyPrefix, String resultFileDir, String processName)
            throws IOException {
        this(auth, configuration, srcBucket, tarBucket, keepKey, keyPrefix, resultFileDir, processName, 0);
    }

    public BucketCopyProcess getNewInstance(int resultFileIndex) throws CloneNotSupportedException {
        BucketCopyProcess bucketCopyProcess = (BucketCopyProcess)super.clone();
        bucketCopyProcess.bucketCopy = bucketCopy.clone();
        bucketCopyProcess.fileReaderAndWriterMap = new FileReaderAndWriterMap();
        bucketCopyProcess.qiniuException = null;
        try {
            bucketCopyProcess.fileReaderAndWriterMap.initWriter(resultFileDir, processName, resultFileIndex);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CloneNotSupportedException();
        }
        return bucketCopyProcess;
    }

    public String getProcessName() {
        return this.processName;
    }

    public QiniuException qiniuException() {
        return qiniuException;
    }

    public void processFile(String fileKey, int retryCount) {

        try {
            String result = bucketCopy.run(srcBucket, fileKey, tarBucket, keepKey ? fileKey : null, keyPrefix,
                    false, retryCount);
            if (!StringUtils.isNullOrEmpty(result)) fileReaderAndWriterMap.writeSuccess(result);
        } catch (QiniuException e) {
            fileReaderAndWriterMap.writeErrorOrNull(srcBucket + "\t" + fileKey + "\t" + tarBucket + "\t" + fileKey
                    + "\t" + e.error());
            if (!e.response.needRetry()) qiniuException = e;
            else e.response.close();
        }
    }

    public void processFile(List<FileInfo> fileInfoList, int retryCount) {

        if (fileInfoList == null || fileInfoList.size() == 0) return;
        int times = fileInfoList.size()/1000 + 1;
        List<String> keyList = fileInfoList.stream().map(fileInfo -> fileInfo.key).collect(Collectors.toList());
        for (int i = 0; i < times; i++) {
            List<String> processList = keyList.subList(1000 * i, i == times - 1 ? keyList.size() : 1000 * (i + 1));
            if (processList.size() > 0) {
                try {
                    String result = bucketCopy.batchRun(srcBucket, tarBucket, keyPrefix, keepKey, processList,
                            retryCount);
                    if (!StringUtils.isNullOrEmpty(result)) fileReaderAndWriterMap.writeSuccess(result);
                } catch (QiniuException e) {
                    fileReaderAndWriterMap.writeErrorOrNull(srcBucket + "\t" + tarBucket + "\t" + keyPrefix + "\t"
                            + processList + "\t" + "\t" + e.error());
                    if (!e.response.needRetry()) qiniuException = e;
                    else e.response.close();
                }
            }
        }
    }

    public void closeResource() {
        fileReaderAndWriterMap.closeWriter();
    }
}
