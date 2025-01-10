package com.tiansuo.file.manage.service;

import com.google.common.collect.Multimap;
import com.tiansuo.file.manage.model.vo.ListParts;
import io.minio.UploadPartResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * MinIO S3文件存储引擎接口定义
 *
 * @author zhangb
 * @since  2024/06/03
 */
public interface MinioS3Client {

    /**
     * 判断存储桶是否存在
     * @param bucketName 桶名称
     * @return 是否存在
     */
    Boolean bucketExists(String bucketName);

    /**
     * 创建桶
     * @param bucketName 桶名称
     */
    void makeBucket(String bucketName);

    /**
     * 创建分片上传任务
     * @param bucketName 桶名称
     * @param objectName 对象名称（含路径）
     * @param contentType 内容类型
     * @return UploadId 上传任务编号
     */
    String createMultipartUpload(String bucketName, String objectName,String contentType);

    /**
     * 合并分片
     * @param bucketName 桶名称
     * @param objectName 对象名称（含路径）
     * @param uploadId 上传任务编号
     * @param parts 分片信息 partNumber 和 etag
     * @return 是否成功
     */
    Boolean completeMultipartUpload(String bucketName, String objectName, String uploadId, List<ListParts.Part> parts);

    /**
     * 获取分片信息列表
     * @param bucketName 桶名称
     * @param objectName 对象名称（含路径）
     * @param maxParts 分片数量
     * @param uploadId 上传任务编号
     * @return 分片信息
     */
    ListParts listParts(String bucketName,String objectName,Integer maxParts,String uploadId);

    /**
     * 获得对象和分片上传链接
     * @param bucketName  桶名称
     * @param objectName  对象名称（含路径）
     * @param uploadId  上传任务编号
     * @param partNumber 分片序号
     * @return {@link String}
     */
    String getUploadObjectUrl(String bucketName, String objectName, String uploadId, String partNumber);

    /**
     * 取得下载链接
     * @param fileName 文件全名含扩展名
     * @param contentType 数据类型
     * @param bucketName 桶名称
     * @param objectName 对象名称含路径
     * @return 下载地址
     */
    String getDownloadUrl(String fileName, String contentType, String bucketName, String objectName);

    /**
     * 取得下载文件流
     * @param fileName 文件全名含扩展名
     * @param bucketName 桶名称
     * @param response 响应
     * @return 下载地址
     */
    void getDownloadObject(String fileName, String bucketName, HttpServletResponse response);

    /**
     * 取得图片预览链接
     * @param contentType 数据类型
     * @param bucketName 桶名称
     * @param objectName 对象名称含路径
     * @return 图片预览链接
     */
    String getPreviewUrl(String contentType, String bucketName, String objectName);


    /**
     * 前端通过后端上传分片到MinIO
     * @param  bucketName MinIO桶名称
     * @param  region 一般填null就行
     * @param  objectName MinIO中文件全路径
     * @param  data 分片文件，只能接收RandomAccessFile、InputStream类型的，一般使用InputStream类型
     * @param  length 当前分片文件大小
     * @param  uploadId 文件上传uploadId
     * @param  partNumber 分片编号
     * @param  extraHeaders 一般填null就行
     * @param  extraQueryParams 一般填null就行
     * @return UploadPartResponse对象
     **/
    UploadPartResponse uploadPart(String bucketName, String region, String objectName, Object data, long length, String uploadId, int partNumber, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams);


    /**
     * 文件上传
     * @param bucketName 桶名称
     * @param objectName 对象名称含路径
     * @param stream 文件流
     * @param size 文件长度
     * @param contentType 文件类型
     * @return 是否成功
     */
    Boolean putObject(String bucketName, String objectName, InputStream stream, long size, String contentType);

    /**
     * 读取文件
     * @param bucketName 桶名称
     * @param objectName 对象名称含路径
     * @return 文件流
     */
    byte[] getObject(String bucketName, String objectName);

    /**
     * 删除文件
     * @param bucketName 桶名称
     * @param objectName 对象名称含路径
     */
    void removeObject(String bucketName, String objectName);

}