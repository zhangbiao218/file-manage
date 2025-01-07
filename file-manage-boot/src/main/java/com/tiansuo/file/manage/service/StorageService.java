package com.tiansuo.file.manage.service;

import cn.hutool.core.lang.Pair;
import com.tiansuo.file.manage.model.dto.FileMetadataInfoDTO;
import com.tiansuo.file.manage.model.vo.*;
import io.minio.UploadPartResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * MinIO Plus 接口定义
 * @author zhangb
 * @since  2024/06/05
 */
public interface StorageService {

    /**
     * 文件预分片
     * @param fileSize 文件大小
     * @return 预分片结果
     */
    FilePreShardingVo sharding(long fileSize);

    /**
     * 上传任务初始化
     * @param fileMd5 文件md5
     * @param fullFileName 文件名（含扩展名）
     * @param fileSize 文件长度
     * @param isPrivate 是否私有 false:否 true:是
     * @param userId  用户编号
     * @return {@link FileCheckResultVo}
     */
    FileCheckResultVo init(String fileMd5, String fullFileName, long fileSize, Boolean isPrivate, String userId);


    /**
     * 合并已分块的文件
     * @param fileKey 文件关键
     * @param partMd5List 文件分块md5列表
     * @param userId  用户编号
     * @return {@link CompleteResultVo}
     */
    CompleteResultVo complete(String fileKey, List<String> partMd5List, String userId);

    /**
     * 取得文件下载地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 文件下载地址
     */
    String download(String fileKey, String userId);

    /**
     * 取得原图地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 原图地址
     */
    String image(String fileKey, String userId);

    /**
     * 取得缩略图地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 缩略图地址
     */
    String preview(String fileKey, String userId);

    /**
     * 查询元数据信息
     * @param key 文件key
     * @return 文件元数据信息
     */
    FileMetadataInfoVo one(String key);

    /**
     * 查询元数据信息
     * @param fileMetadataInfo 查询入参
     * @return 文件元数据信息集合
     */
    List<FileMetadataInfoVo> list(FileMetadataInfoDTO fileMetadataInfo);

    /**
     * 根据文件key删除文件
     * @param fileKey 文件key
     * @return 是否成功
     */
    Boolean remove(String fileKey);

    /**
     * 上传文件(小文件不分片)
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    FileUploadResultVo uploadFile(MultipartFile file);

    /**
     * 上传分片文件
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    UploadPartResponse uploadPartFile(MultipartFile file);
}