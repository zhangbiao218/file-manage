package com.tiansuo.file.manage.service.impl;

import com.google.common.collect.Multimap;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Part;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * 继承MinioClient重写
 * @author zhangb
 */
public class CustomMinioClient extends MinioAsyncClient {

    /**
     * 构造方法
     * @param client Minio异步客户端
     */
    public CustomMinioClient(MinioAsyncClient client) {
        super(client);
    }

    /**
     * 创建分片上传任务
     *
     * @param bucketName       存储桶
     * @param region           区域,一般传null即可
     * @param objectName       对象名
     * @param headers          消息头,一般传null即可
     * @param extraQueryParams 额外查询参数,一般传null即可
     */
    @Override
    public CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadAsync(String bucketName, String region, String objectName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.createMultipartUploadAsync(bucketName, region, objectName, headers, extraQueryParams);
    }

    /**
     * 分片上传文件
     *
     * @param bucketName       存储桶
     * @param region           区域, 一般传null即可
     * @param objectName       对象名
     * @param data             分片文件，只能接收RandomAccessFile、InputStream类型的，一般使用InputStream类型
     * @param length           当前分片文件大小
     * @param uploadId         上传ID
     * @param partNumber       分片号
     * @param extraHeaders     额外消息头,一般传null即可
     * @param extraQueryParams 额外查询参数,一般传null即可
     */
    @Override
    public CompletableFuture<UploadPartResponse> uploadPartAsync(String bucketName, String region, String objectName, Object data, long length, String uploadId, int partNumber, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.uploadPartAsync(bucketName, region, objectName, data, length, uploadId, partNumber, extraHeaders, extraQueryParams);
    }

    /**
     * 查询分片数据
     *
     * @param bucketName       存储桶
     * @param region           区域,一般传null即可
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param extraHeaders     额外消息头,一般传null即可
     * @param extraQueryParams 额外查询参数,一般传null即可
     */
    @Override
    public CompletableFuture<ListPartsResponse> listPartsAsync(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams);
    }

    /**
     * 完成分片上传，执行合并文件
     *
     * @param bucketName       存储桶
     * @param region           区域,一般传null即可
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param parts            分片
     * @param extraHeaders     额外消息头,一般传null即可
     * @param extraQueryParams 额外查询参数,一般传null即可
     */
    @Override
    public CompletableFuture<ObjectWriteResponse> completeMultipartUploadAsync(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams);
    }

    /**
     * 完成分片上传，执行合并文件
     *
     * @param bucketName       存储桶
     * @param region           区域,一般传null即可
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param extraHeaders     额外消息头,一般传null即可
     * @param extraQueryParams 额外查询参数,一般传null即可
     */
    @Override
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUploadAsync(String bucketName, String region, String objectName, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.abortMultipartUploadAsync(bucketName, region, objectName, uploadId, extraHeaders, extraQueryParams);
    }


}
