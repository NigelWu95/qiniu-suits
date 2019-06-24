package com.qiniu.datasource;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.qiniu.common.SuitsException;
import com.qiniu.interfaces.ITypeConvert;
import com.qiniu.persistence.IResultOutput;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AwsS3Container extends CloudStorageContainer<S3ObjectSummary, BufferedWriter, Map<String, String>> {

    private String accessKeyId;
    private String secretKey;
    private ClientConfiguration clientConfig;
    private String region;

    public AwsS3Container(String accessKeyId, String secretKey, ClientConfiguration clientConfig, String region,
                          String bucket, List<String> antiPrefixes, Map<String, String[]> prefixesMap, boolean prefixLeft,
                          boolean prefixRight, Map<String, String> indexMap, int unitLen, int threads) {
        super(bucket, antiPrefixes, prefixesMap, prefixLeft, prefixRight, indexMap, unitLen, threads);
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.clientConfig = clientConfig;
        this.region = region;
    }

    @Override
    public String getSourceName() {
        return "aws";
    }

    @Override
    protected ITypeConvert<S3ObjectSummary, Map<String, String>> getNewConverter() {
        return null;
    }

    @Override
    protected ITypeConvert<S3ObjectSummary, String> getNewStringConverter() throws IOException {
        return null;
    }

    @Override
    protected IResultOutput<BufferedWriter> getNewResultSaver(String order) throws IOException {
        return null;
    }

    @Override
    protected ILister<S3ObjectSummary> getLister(String prefix, String marker, String end) throws SuitsException {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)))
                .withRegion(region)
                .withClientConfiguration(clientConfig)
                .build();
        return new S3Lister(s3Client, bucket, prefix, marker, end, null, unitLen);
    }
}
