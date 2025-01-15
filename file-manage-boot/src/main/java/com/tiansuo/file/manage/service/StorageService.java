package com.tiansuo.file.manage.service;


import com.tiansuo.file.manage.model.vo.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * MinIO Plus 接口定义
 * @author zhangb
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
     * @param fileMd5 文件md5值
     * @param fullFileName 文件名（含扩展名）
     * @param fileSize 文件长度
     * @param isPrivate 是否私有 0:否 1:是
     * @return {@link FileCheckResultVo}
     */
    FileCheckResultVo init(String fileMd5,String fullFileName, long fileSize, Integer isPrivate);


    /**
     * 合并已分块的文件
     * @param fileKey 文件关键
     * @param partMd5List 文件分块md5列表
     * @return {@link CompleteResultVo}
     */
    CompleteResultVo complete(String fileKey, List<String> partMd5List);

    /**
     * 取得文件下载地址
     *
     * @param fileKey 文件KEY
     * @return 文件下载地址
     */
    String download(String fileKey);

    /**
     * 取得文件下载地址
     *
     * @param fileKey 文件KEY
     * @return 文件下载地址
     */
    void getDownloadObject(String fileKey, HttpServletResponse response);


    /**
     * 取得原图地址
     *
     * @param fileKey 文件KEY
     * @return 原图地址
     */
    String image(String fileKey);

    /**
     * 取得缩略图地址
     *
     * @param fileKey 文件KEY
     * @return 缩略图地址
     */
    String preview(String fileKey);


    /**
     * 上传文件(小文件不分片)
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    FileUploadResultVo uploadFile(MultipartFile file);

    /**
     * 绑定上传文件所属业务数据
     * @param fileKeyList 待绑定文件的filekey
     * @param businessKey 业务数据唯一标识
     * @return true 成功
     */
    Boolean bindBusinessAndFile(List<String> fileKeyList, String businessKey);

    /**
     * 根据业务逐渐获取所绑定的文件信息
     * @param businessKey 业务主键
     * @return list 文件列表
     */
    List<FileUploadResultVo> getFileByBusinessKey(String businessKey);

    /**
     * 根据业务主键删除文件
     * @param businessKey  业务主键
     */
    Boolean deleteFileByBusinessKey(String businessKey);

    /**
     * 根据文件key删除文件
     * @param fileKey 文件key
     */
    Boolean deleteFileByFileKey(String fileKey);
}