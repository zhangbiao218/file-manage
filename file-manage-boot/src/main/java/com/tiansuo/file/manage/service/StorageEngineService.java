package com.tiansuo.file.manage.service;

import cn.hutool.core.lang.Pair;
import com.tiansuo.file.manage.model.dto.FileMetadataInfoSaveDTO;
import com.tiansuo.file.manage.model.vo.CompleteResultVo;
import com.tiansuo.file.manage.model.vo.FileCheckResultVo;
import com.tiansuo.file.manage.model.vo.FileMetadataInfoVo;
import com.tiansuo.file.manage.model.vo.FileUploadResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 存储引擎Service接口定义
 *
 * @author zhangb
 * @since 2023/06/26
 */
public interface StorageEngineService {

    /**
     * 计算分块的数量
     *
     * @param fileSize 文件大小
     * @return {@link Integer}
     */
    Integer computeChunkNum(Long fileSize);

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
     *
     * @param fileKey 文件关键
     * @param partMd5List 文件分块md5列表
     * @param userId  用户编号
     *
     * @return {@link CompleteResultVo}
     */
    CompleteResultVo complete(String fileKey, List<String> partMd5List, String userId);

    /**
     * 取得文件下载地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 地址
     */
    String download(String fileKey, String userId);

    /**
     * 取得原图地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 地址
     */
    String image(String fileKey, String userId);

    /**
     * 取得缩略图地址
     *
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 地址
     */
    String preview(String fileKey, String userId);

    /**
     * 删除文件
     * @param fileKey 文件KEY
     * @return 是否成功
     */
    Boolean remove(String fileKey);

    /**
     * 删除文件
     * @param fileKey 文件KEY
     * @param userId  用户编号
     * @return 是否成功
     */
    Boolean remove(String fileKey, String userId);

    /**
     * 上传文件(小文件不分片)
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    FileUploadResultVo uploadFile(MultipartFile file);

    /**
     * 上传文件(小文件不分片)
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    FileUploadResultVo uploadPartFile(MultipartFile file);
}
