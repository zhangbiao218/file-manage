package com.tiansuo.file.manage.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tiansuo.file.manage.config.MinioPlusProperties;
import com.tiansuo.file.manage.constant.MinioPlusErrorCode;
import com.tiansuo.file.manage.constant.StorageBucketEnums;
import com.tiansuo.file.manage.exception.MinioPlusException;
import com.tiansuo.file.manage.mapper.MetadataMapper;
import com.tiansuo.file.manage.model.bo.CreateUploadUrlReqBO;
import com.tiansuo.file.manage.model.bo.CreateUploadUrlRespBO;

import com.tiansuo.file.manage.model.entity.FileMetadataInfo;
import com.tiansuo.file.manage.model.vo.*;
import com.tiansuo.file.manage.service.MinioS3Client;
import com.tiansuo.file.manage.service.StorageService;
import com.tiansuo.file.manage.util.CommonUtil;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;

/**
 * 存储组件Service层公共方法实现类
 *
 * @author zhangb
 * @since 2023/06/26
 */
@Transactional
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    /**
     * 文件元数据服务接口定义
     */
    @Autowired
    private MetadataMapper fileMetadataMapper;

    /**
     * MinioPlus配置信息注入类
     */
    @Autowired
    private MinioPlusProperties properties;

    @Autowired
    private MetadataMapper metadataMapper;

    @Autowired
    private MinioS3Client minioS3Client;

    @Override
    public FilePreShardingVo sharding(long fileSize) {

        // 计算分块数量
        Integer chunkNum = this.computeChunkNum(fileSize);

        List<FilePreShardingVo.Part> partList = new ArrayList<>();

        long start = 0;
        for (int partNumber = 1; partNumber <= chunkNum; partNumber++) {

            long end = Math.min(start + properties.getPart().getSize(), fileSize);

            FilePreShardingVo.Part part = new FilePreShardingVo.Part();
            // 开始位置
            part.setStartPosition(start);
            // 结束位置
            part.setEndPosition(end);

            // 更改下一次的开始位置
            start = start + properties.getPart().getSize();
            partList.add(part);
        }

        FilePreShardingVo filePreShardingVo = new FilePreShardingVo();
        filePreShardingVo.setFileSize(fileSize);
        filePreShardingVo.setPartCount(chunkNum);
        filePreShardingVo.setPartSize(properties.getPart().getSize());
        filePreShardingVo.setPartList(partList);
        return filePreShardingVo;
    }

    /**
     * 上传任务初始化
     * <p>
     * 1.当前用户或其他用户上传过，且已完成，秒传，新增文件元数据
     * 2.当前用户上传过，未完成，断点续传
     * 3.其他用户上传过，未完成，断点续传，新增文件元数据
     * 4.从未上传过，下发上传链接，新增文件元数据
     *
     * @param fileMd5      文件md5,用来减少否相同的文件重复上传
     * @param fullFileName 文件名（含扩展名）
     * @param fileSize     文件长度
     * @param isPrivate    是否私有 false:否 true:是
     * @return {@link FileCheckResultVo}
     */
    @Override
    public FileCheckResultVo init(String fileMd5, String fullFileName, long fileSize, Integer isPrivate) {
        //分片上传,前端传参fileMd5不能为空,则可以支持断点续传
        if (StringUtils.isEmpty(fileMd5)) {
            throw new MinioPlusException(MinioPlusErrorCode.FILE_MD5_CHECK_FAILED);
        }

        CreateUploadUrlReqBO bo = new CreateUploadUrlReqBO();

        //根据fileMd5,isPart为1(分片),查询元数据
        List<FileMetadataInfo> list = this.getMetadataByFileMd5(fileMd5, null, 1);

        if (CollUtil.isNotEmpty(list)) {
            //  1.有上传记录，且状态是已完成，则秒传，新增一条文件元数据,指向曾经的文件
            FileMetadataInfo metadata = this.fileFastUpload(list, fileMd5, fullFileName, isPrivate);
            if (Objects.nonNull(metadata)) {
                log.info("文件在minio中已存在,走秒传逻辑");
                return this.buildResult(metadata, new ArrayList<>(1), 0, Boolean.TRUE);
            }

            //2, 有上传记录,但是状态都不是已完成,则进行断点续传
            FileMetadataInfo uploadingMetadata = list.get(0);
            // 上传过未完成-断点续传
            bo.setIsSequel(Boolean.TRUE);
            //创建断点的url
            CreateUploadUrlRespBO respBO = this.breakResume(uploadingMetadata);

            // 2.当上传过，未完成，断点续传
            //!respBO.getUploadTaskId().equals(uploadingMetadata.getUploadTaskId()),如果minio中所有的分片都丢失,则表示只在数据库记录了一条元数据日志,
            // minio中是失败的,那么此时的respBO.getUploadTaskId()是新生成的,就相当于重新上传了,将原来的失效元数据记录更新为最新的上传值
            if (CollUtil.isNotEmpty(respBO.getParts()) && !respBO.getUploadTaskId().equals(uploadingMetadata.getUploadTaskId())) {
                // 原uploadTaskId失效时，同时更新原记录(存在一个情况,一次分片都没有上传成功过)
                uploadingMetadata.setUploadTaskId(respBO.getUploadTaskId());
                FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
                fileMetadataInfo.setId(uploadingMetadata.getId());
                fileMetadataInfo.setUploadTaskId(uploadingMetadata.getUploadTaskId());
                metadataMapper.updateById(fileMetadataInfo);
            }
            return this.buildResult(uploadingMetadata, respBO.getParts(), respBO.getPartCount(), Boolean.FALSE);
        } else {
            // 4.从未上传过，下发上传链接，新增文件元数据
            bo.setFileMd5(fileMd5);
            bo.setFileSize(fileSize);
            bo.setFullFileName(fullFileName);

            //获取上传的url
            CreateUploadUrlRespBO createUploadUrlRespBO = this.createUploadUrl(bo);

            //保存原数据信息
            FileMetadataInfo metadataInfo = saveMetadataInfo(createUploadUrlRespBO, fileMd5, fullFileName, fileSize, isPrivate);
            return this.buildResult(metadataInfo, createUploadUrlRespBO.getParts(), createUploadUrlRespBO.getPartCount(), Boolean.FALSE);
        }
    }


    /**
     * 合并已分块的文件
     *
     * @param fileKey     文件关键
     * @param partMd5List 文件分块md5列表
     * @return {@link Boolean}
     */
    @Override
    public CompleteResultVo complete(String fileKey, List<String> partMd5List) {

        CompleteResultVo completeResultVo;
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileKey, fileKey);
        FileMetadataInfo metadata = metadataMapper.selectOne(queryWrapper);
        if (metadata == null) {
            log.error(fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
        }

        if (metadata.getIsFinished() == 1) {
            // 如果文件已上传完成，直接返回true，不进行合并
            completeResultVo = new CompleteResultVo();
            completeResultVo.setIsComplete(true);
            return completeResultVo;
        }

        completeResultVo = this.completeMultipartUpload(metadata, partMd5List);

        if (Boolean.TRUE.equals(completeResultVo.getIsComplete())) {
            // 更新自己上传的文件元数据状态
            FileMetadataInfo update = new FileMetadataInfo();
            update.setId(metadata.getId());
            //已完成
            update.setIsFinished(1);
            metadataMapper.updateById(update);

            // 搜索数据库中所有已分片,且状态时未完成的相同MD5元数据，更新为完成状态
            List<FileMetadataInfo> others = this.getMetadataByFileMd5(metadata.getFileMd5(), 0, 1);
            if (CollUtil.isNotEmpty(others)) {
                for (FileMetadataInfo other : others) {
                    FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
                    fileMetadataInfo.setId(other.getId());
                    //已完成
                    fileMetadataInfo.setIsFinished(1);
                    metadataMapper.updateById(fileMetadataInfo);
                }
            }
        } else {
            if (!metadata.getUploadTaskId().equals(completeResultVo.getUploadTaskId())) {
                FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
                fileMetadataInfo.setId(metadata.getId());
                fileMetadataInfo.setUploadTaskId(completeResultVo.getUploadTaskId());
                metadataMapper.updateById(fileMetadataInfo);
            }
        }

        if (CollectionUtils.isNotEmpty(completeResultVo.getPartList())) {
            for (FileCheckResultVo.Part part : completeResultVo.getPartList()) {
                part.setUrl(remakeUrl(part.getUrl()));
            }
        }

        return completeResultVo;
    }


    /**
     * 取得文件下载地址
     *
     * @param fileKey 文件KEY
     * @return 地址
     */
    @Override
    public String download(String fileKey) {
        FileMetadataInfo metadata = getFileMetadataInfo(fileKey);
        String downloadUrl;
        try {
            // 文件权限校验，元数据为抛出异常
            if (Objects.isNull(metadata)) {
                throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
            }
            downloadUrl = minioS3Client.getDownloadUrl(metadata.getFileName(), metadata.getFileMimeType(), metadata.getStorageBucket(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
        } catch (Exception e) {
            // 打印日志
            log.error(e.getMessage(), e);
            // 任何异常，统一返回给前端文件不存在
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED);
        }
        return remakeUrl(downloadUrl);
    }

    /**
     * 取得文件下载流
     *
     * @param fileKey 文件KEY
     * @return 地址
     */
    @Override
    public void getDownloadObject(String fileKey, HttpServletResponse response) {
        FileMetadataInfo metadata = getFileMetadataInfo(fileKey);

        if (Objects.isNull(metadata)) {
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
        }
        try {
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            String filename = URLEncoder.encode(metadata.getFileName(), "utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
            minioS3Client.getDownloadObject(metadata.getFileName(), metadata.getStorageBucket(), response);
        } catch (Exception e) {
            // 打印日志
            log.error(e.getMessage(), e);
            // 任何异常，统一返回给前端文件不存在
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED);
        }

    }


    @Override
    public String image(String fileKey) {
        FileMetadataInfo metadata = getFileMetadataInfo(fileKey);
        String previewUrl;
        try {
            // 元数据为空抛出异常
            if (Objects.isNull(metadata)) {
                throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
            }

            previewUrl = minioS3Client.getPreviewUrl(metadata.getFileMimeType(), metadata.getStorageBucket(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
        } catch (Exception e) {
            // 打印日志
            log.error(e.getMessage(), e);
            // 任何异常，统一返回给前端文件不存在
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED);
        }

        return remakeUrl(previewUrl);
    }

    @Override
    public String preview(String fileKey) {
        FileMetadataInfo metadata = getFileMetadataInfo(fileKey);
        String previewUrl;
        try {
            // 元数据为抛出异常
            if (Objects.isNull(metadata)) {
                throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), fileKey + MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage());
            }

            if (!StorageBucketEnums.IMAGE.getCode().equals(metadata.getStorageBucket())) {
                // 不是图片时，返回文件类型
                return metadata.getFileSuffix();
            }

            // 生成缩略图
            generatePreviewImage(metadata);
            // 创建图片预览地址
            previewUrl = minioS3Client.getPreviewUrl(metadata.getFileMimeType(), StorageBucketEnums.IMAGE_PREVIEW.getCode(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
        } catch (Exception e) {
            // 打印日志
            log.error(e.getMessage(), e);
            // 任何异常，统一返回给前端文件不存在
            throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED);
        }
        return remakeUrl(previewUrl);
    }


    @Override
    public Boolean remove(String fileKey) {
        // 查询元数据信息
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileKey, fileKey);
        FileMetadataInfo metadata = fileMetadataMapper.selectOne(queryWrapper);

        if (null != metadata) {
            // 删除元数据信息
            metadataMapper.deleteById(metadata.getId());
            //删除minio物理文件
            minioS3Client.removeObject(metadata.getStorageBucket(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
            if (metadata.getIsPreview() == 1) {
                // 当存在缩略图时，同步删除缩略图
                minioS3Client.removeObject(StorageBucketEnums.IMAGE_PREVIEW.getCode(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
            }
        }
        return true;
    }

    @Override
    public FileUploadResultVo uploadFile(MultipartFile file) {
        FileUploadResultVo fileUploadResultVo = new FileUploadResultVo();
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        if (CharSequenceUtil.isBlank(suffix)) {
            throw new MinioPlusException(MinioPlusErrorCode.FILE_SUFFIX_GET_FAILED);
        }

        // 文件key
        String fileKey = IdUtil.fastSimpleUUID();
        //存储路径
        String storagePath = CommonUtil.getPathByDate();
        // MIME类型
        String fileMimeType = FileUtil.getMimeType(originalFilename);
        // 存储桶
        String bucketName = StorageBucketEnums.getBucketByFileSuffix(suffix);
        // 创建桶
        minioS3Client.makeBucket(bucketName);

        long fileSize = file.getSize();

        InputStream fileInputStream = null;
        try {
            String fileMd5 = SecureUtil.md5().digestHex(file.getBytes());
            //1,查询是否有相同的md5是已完成的,有则只新插一条元数据信息,不往minio上传了
            //根据fileMd5查询不分片且状态是已完成状态的元数据
            List<FileMetadataInfo> list = this.getMetadataByFileMd5(fileMd5, 1, 0);
            if (CollectionUtils.isEmpty(list)) {
                //没有上传过,重新将文件上传minio
                fileInputStream = file.getInputStream();
                String objectName = CommonUtil.getObjectName(fileMd5);
                Boolean isSuccess = minioS3Client.putObject(bucketName, objectName, fileInputStream, fileSize, fileMimeType);
                log.info("{},文件上传minio成功!", originalFilename);
                if (!isSuccess) {
                    //上传minio失败,抛出异常
                    throw new MinioPlusException(MinioPlusErrorCode.FILE_UPLOAD_FAILED);
                }
            }
            //新增一条元数据,状态为已完成
            FileMetadataInfo metadataInfoFinished = this.createMetadataInfoFinished(fileKey, fileMd5, originalFilename, fileMimeType, suffix, bucketName, storagePath, fileSize);
            //更新同一个fileMd5下的其他不分片,未完成状态的元数据 isFinished状态改为已完成
            List<FileMetadataInfo> metadataList = this.getMetadataByFileMd5(fileMd5, 0, 0);
            metadataList.forEach(a -> {
                FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
                fileMetadataInfo.setId(a.getId());
                //已完成
                fileMetadataInfo.setIsFinished(1);
                metadataMapper.updateById(fileMetadataInfo);
            });
            //构建返回给前端的对象
            fileUploadResultVo.setFileKey(metadataInfoFinished.getFileKey())
                    .setFileSize(metadataInfoFinished.getFileSize())
                    .setFileName(metadataInfoFinished.getFileName())
                    .setStoragePath(metadataInfoFinished.getStoragePath())
                    .setFileSuffix(metadataInfoFinished.getFileSuffix())
                    .setFileMimeType(metadataInfoFinished.getFileMimeType());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileUploadResultVo;
    }

    /**
     * 将业务和file进行绑定
     *
     * @param fileKeyList     文件唯一标识list
     * @param businessKey 业务唯一标识
     * @return true 绑定成功,false绑定失败
     */
    @Override
    public Boolean bindBusinessAndFile(List<String> fileKeyList, String businessKey) {
        fileKeyList.forEach(fileKey -> {
            LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileMetadataInfo::getFileKey, fileKey);
            FileMetadataInfo fileMetadataInfo = metadataMapper.selectOne(queryWrapper);
            if (Objects.isNull(fileMetadataInfo)) {
                throw new MinioPlusException(MinioPlusErrorCode.FILE_EXIST_FAILED.getCode(), MinioPlusErrorCode.FILE_EXIST_FAILED.getMessage() + ":" + fileKey);
            }
            //绑定业务
            metadataMapper.updateBusinessKey(fileKey, businessKey);
        });

        return true;
    }

    @Override
    public List<FileUploadResultVo> getFileByBusinessKey(String businessKey) {
        return metadataMapper.queryByBusinessKey(businessKey);
    }

    @Override
    public Boolean deleteFileByBusinessKey(String businessKey) {
        //先删除绑定关系,在判断fileMd5是否有其他业务在使用此文件
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getBusinessKey, businessKey);
        List<FileMetadataInfo> list = metadataMapper.selectList(queryWrapper);
        list.forEach(a -> {
            LambdaQueryWrapper<FileMetadataInfo> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(FileMetadataInfo::getFileMd5, a.getFileMd5());
            queryWrapper2.ne(FileMetadataInfo::getFileKey, a.getFileKey());
            List<FileMetadataInfo> fileMetadataInfoList = metadataMapper.selectList(queryWrapper2);
            if (CollectionUtils.isEmpty(fileMetadataInfoList)) {
                //删除元数据以及minio中的文件
                this.deleteMetadataAndMinioFile(a);
            } else {
                //只删除元数据
                this.deleteMetadataOnly(a);
            }
        });
        return true;
    }

    @Override
    public Boolean deleteFileByFileKey(String fileKey) {
        //先删除绑定关系,在判断fileMd5是否有其他业务在使用此文件
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getBusinessKey, fileKey);
        FileMetadataInfo metadataInfo = metadataMapper.selectOne(queryWrapper);
        if (Objects.nonNull(metadataInfo)) {
            LambdaQueryWrapper<FileMetadataInfo> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(FileMetadataInfo::getFileMd5, metadataInfo.getFileMd5());
            queryWrapper2.ne(FileMetadataInfo::getFileKey, metadataInfo.getFileKey());
            List<FileMetadataInfo> list = metadataMapper.selectList(queryWrapper2);
            if (CollectionUtils.isNotEmpty(list)) {
                //有其他原数据在使用此文件,则不删除minio中文件,仅删除原数据
                this.deleteMetadataOnly(metadataInfo);
            } else {
                //删除元数据以及minio中的文件
                this.deleteMetadataAndMinioFile(metadataInfo);
            }
        }
        return true;
    }

    /**
     * 删除元数据及minio中文件
     */
    public void deleteMetadataAndMinioFile(FileMetadataInfo metadata) {
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileKey, metadata.getFileKey());
        metadataMapper.delete(queryWrapper);
        //删除minio物理文件
        minioS3Client.removeObject(metadata.getStorageBucket(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
        if (metadata.getIsPreview() == 1) {
            // 当存在缩略图时，同步删除缩略图
            minioS3Client.removeObject(StorageBucketEnums.IMAGE_PREVIEW.getCode(), metadata.getStoragePath() + "/" + metadata.getFileMd5());
        }
    }

    /**
     * 仅删除元数据不删除minio中文件
     */
    public void deleteMetadataOnly(FileMetadataInfo metadata) {
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileKey, metadata.getFileKey());
        metadataMapper.delete(queryWrapper);
    }

    /**
     * 插入文件元数据,状态为已完成
     */
    public FileMetadataInfo createMetadataInfoFinished(String fileKey, String fileMd5, String originalFilename, String fileMimeType, String suffix, String bucketName, String storagePath, Long fileSize) {
        //保存原数据信息
        FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
        // 保存文件元数据
        // 文件KEY
        fileMetadataInfo.setFileKey(fileKey);
        // 文件md5
        fileMetadataInfo.setFileMd5(fileMd5);
        // 文件名
        fileMetadataInfo.setFileName(originalFilename);
        // MIME类型
        fileMetadataInfo.setFileMimeType(fileMimeType);
        // 文件后缀
        fileMetadataInfo.setFileSuffix(suffix);
        // 文件长度
        fileMetadataInfo.setFileSize(fileSize);
        // 存储桶
        fileMetadataInfo.setStorageBucket(bucketName);
        // 存储路径
        fileMetadataInfo.setStoragePath(storagePath);
        // 上传任务id
        fileMetadataInfo.setUploadTaskId(null);
        // 状态 0:未完成 1:已完成
        fileMetadataInfo.setIsFinished(1);
        // 是否分块 0:否 1:是
        fileMetadataInfo.setIsPart(0);
        // 分片数量
        fileMetadataInfo.setPartNumber(0);
        // 预览图 0:无 1:有
        fileMetadataInfo.setIsPreview(0);
        // 是否私有 0:否 1:是
        fileMetadataInfo.setIsPrivate(0);
        metadataMapper.insert(fileMetadataInfo);
        return fileMetadataInfo;
    }

    /**
     * 断点续传-创建断点的URL
     *
     * @param fileMetadataVo 文件元数据信息
     * @return CreateUploadUrlRespBO 分片结果
     */
    public CreateUploadUrlRespBO breakResume(FileMetadataInfo fileMetadataVo) {

        CreateUploadUrlRespBO result = new CreateUploadUrlRespBO();
        result.setParts(new ArrayList<>());
        result.setPartCount(fileMetadataVo.getPartNumber());

        // 分块数量
        Integer chunkNum = fileMetadataVo.getPartNumber();
        // 获取分块信息
        ListParts listParts = this.getListParts(fileMetadataVo);
        List<ListParts.Part> parts = listParts.getPartList();
        if (!chunkNum.equals(parts.size())) {
            // 找到丢失的片
            boolean[] exists = new boolean[chunkNum + 1];
            // 遍历数组，标记存在的块号
            for (ListParts.Part item : parts) {
                int partNumber = item.getPartNumber();
                exists[partNumber] = true;
            }
            // 查找丢失的块号
            List<Integer> missingNumbers = new ArrayList<>();
            for (int i = 1; i <= chunkNum; i++) {
                if (!exists[i]) {
                    missingNumbers.add(i);
                }
            }
            CreateUploadUrlReqBO bo = new CreateUploadUrlReqBO();
            // 文件md5
            bo.setFileMd5(fileMetadataVo.getFileMd5());
            // 文件名（含扩展名）
            bo.setFullFileName(fileMetadataVo.getFileName());
            // "文件长度"
            bo.setFileSize(fileMetadataVo.getFileSize());
            // 是否断点续传 0:否 1:是,默认非断点续传
            bo.setIsSequel(Boolean.TRUE);
            // 丢失的块号-断点续传时必传
            bo.setMissPartNum(missingNumbers);

            if (missingNumbers.size() != chunkNum) {
                //minio中分片一个都没上传成功,原本的uploadId会失效,如果有上传成功的分片,则继续使用之前的uploadId
                // minio中分片信息为空的话,会出现任务id失效的情况，此时createUploadUrl()会重新创建新的任务产生新的uploadId
                bo.setUploadId(fileMetadataVo.getUploadTaskId());
            }

            // 存储桶
            bo.setStorageBucket(fileMetadataVo.getStorageBucket());
            // 存储路径
            bo.setStoragePath(fileMetadataVo.getStoragePath());
            // 文件id
            bo.setFileKey(fileMetadataVo.getFileKey());
            result = this.createUploadUrl(bo);

        }

        return result;

    }

    /**
     * 构建响应给前端的分片信息
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称（含路径）
     * @param uploadId   上传任务编号
     * @param fileSize   文件大小
     * @param start      开始位置
     * @param partNumber 块号
     * @return {@link FileCheckResultVo.Part}
     */
    private FileCheckResultVo.Part buildResultPart(String bucketName, String objectName, String uploadId, Long fileSize, long start, Integer partNumber) {
        // 计算起始位置
        long end = Math.min(start + properties.getPart().getSize(), fileSize);
        String uploadUrl = minioS3Client.getUploadObjectUrl(bucketName, objectName, uploadId, String.valueOf(partNumber));
        FileCheckResultVo.Part part = new FileCheckResultVo.Part();
        part.setUploadId(uploadId);
        // 上传地址
        part.setUrl(uploadUrl);
        // 开始位置
        part.setStartPosition(start);
        // 结束位置
        part.setEndPosition(end);
        return part;
    }


    /**
     * 创建上传链接
     *
     * @param bo 创建上传url
     * @return 创建上传链接请求参数
     */
    public CreateUploadUrlRespBO createUploadUrl(CreateUploadUrlReqBO bo) {
        // 计算分块数量
        Integer chunkNum = this.computeChunkNum(bo.getFileSize());
        // 分块信息集合
        List<FileCheckResultVo.Part> partList = new ArrayList<>();
        // 存储桶
        String bucketName;
        // 存储路径
        String storagePath;
        // 文件key
        String fileKey;
        // 上传任务编号
        String uploadId;
        // 断点续传
        if (Boolean.TRUE.equals(bo.getIsSequel()) && CollUtil.isNotEmpty(bo.getMissPartNum()) && CharSequenceUtil.isNotBlank(bo.getUploadId())) {
            // 断点续传需要使用已创建的任务信息构建分片信息
            // 存储桶
            bucketName = bo.getStorageBucket();
            // 文件key
            fileKey = bo.getFileKey();

            storagePath = bo.getStoragePath();
            uploadId = bo.getUploadId();
            // 开始位置
            long start = (long) (bo.getMissPartNum().get(0) - 1) * properties.getPart().getSize();
            for (int partNumber : bo.getMissPartNum()) {
                FileCheckResultVo.Part part = this.buildResultPart(bucketName, CommonUtil.getObjectName(bo.getFileMd5()), uploadId, bo.getFileSize(), start, partNumber);
                // 更改下一次的开始位置
                start = start + properties.getPart().getSize();
                partList.add(part);
            }
        } else {
            // 获取文件后缀
            String suffix = FileUtil.getSuffix(bo.getFullFileName());
            if (CharSequenceUtil.isBlank(suffix)) {
                throw new MinioPlusException(MinioPlusErrorCode.FILE_SUFFIX_GET_FAILED);
            }
            // 文件key
            fileKey = IdUtil.fastSimpleUUID();
            // 存储路径
            storagePath = CommonUtil.getPathByDate();
            // MIME类型
            String fileMimeType = FileUtil.getMimeType(bo.getFullFileName());

            // 存储桶
            bucketName = StorageBucketEnums.getBucketByFileSuffix(suffix);
            // 创建桶
            minioS3Client.makeBucket(bucketName);

            // 创建分片请求,获取uploadId
            uploadId = minioS3Client.createMultipartUpload(bucketName, CommonUtil.getObjectName(bo.getFileMd5()), fileMimeType);
            long start = 0;
            for (Integer partNumber = 1; partNumber <= chunkNum; partNumber++) {
                FileCheckResultVo.Part part = this.buildResultPart(bucketName, CommonUtil.getObjectName(bo.getFileMd5()), uploadId, bo.getFileSize(), start, partNumber);
                // 更改下一次的开始位置
                start = start + properties.getPart().getSize();
                partList.add(part);
            }
        }
        CreateUploadUrlRespBO respBO = new CreateUploadUrlRespBO();
        // 桶名字
        respBO.setBucketName(bucketName);
        // 文件存储路径
        respBO.setStoragePath(storagePath);
        // 文件id-必填
        respBO.setFileKey(fileKey);
        // 分块数量-可选,分片后必须重新赋值 默认1
        respBO.setPartCount(chunkNum);
        // 切片上传任务id
        respBO.setUploadTaskId(uploadId);
        // 分片信息-必填
        respBO.setParts(partList);
        return respBO;
    }

    /**
     * 计算分块的数量
     *
     * @param fileSize 文件大小
     * @return {@link Integer}
     */
    public Integer computeChunkNum(Long fileSize) {
        // 计算分块数量
        double tempNum = (double) fileSize / properties.getPart().getSize();
        // 向上取整
        return ((Double) Math.ceil(tempNum)).intValue();
    }

    /**
     * 保存文件源信息
     *
     * @param createUploadUrlRespBO 上传链接参数
     * @param fileMd5               文件md5
     * @param fullFileName          文件名（含扩展名）
     * @param fileSize              文件长度
     * @param isPrivate             是否私有 0:否 1:是
     * @return {@link FileMetadataInfoVo}
     */
    private FileMetadataInfo saveMetadataInfo(CreateUploadUrlRespBO createUploadUrlRespBO,
                                              String fileMd5, String fullFileName, long fileSize, Integer isPrivate) {
        FileMetadataInfo fileMetadataInfo = new FileMetadataInfo();
        // 保存文件元数据
        String suffix = FileUtil.getSuffix(fullFileName);
        // 文件KEY
        fileMetadataInfo.setFileKey(createUploadUrlRespBO.getFileKey());
        // 文件md5
        fileMetadataInfo.setFileMd5(fileMd5);
        // 文件名
        fileMetadataInfo.setFileName(fullFileName);
        // MIME类型
        fileMetadataInfo.setFileMimeType(FileUtil.getMimeType(fullFileName));
        // 文件后缀
        fileMetadataInfo.setFileSuffix(suffix);
        // 文件长度
        fileMetadataInfo.setFileSize(fileSize);
        // 存储桶
        fileMetadataInfo.setStorageBucket(createUploadUrlRespBO.getBucketName());
        // 存储路径
        fileMetadataInfo.setStoragePath(createUploadUrlRespBO.getStoragePath());
        // 上传任务id
        fileMetadataInfo.setUploadTaskId(createUploadUrlRespBO.getUploadTaskId());
        // 状态 0:未完成 1:已完成
        fileMetadataInfo.setIsFinished(0);
        // 是否分块 0:否 1:是
        fileMetadataInfo.setIsPart(createUploadUrlRespBO.getPartCount() > 1 ? 1 : 0);
        // 分片数量
        fileMetadataInfo.setPartNumber(createUploadUrlRespBO.getPartCount());
        // 预览图 0:无 1:有
        fileMetadataInfo.setIsPreview(0);
        // 是否私有 0:否 1:是
        fileMetadataInfo.setIsPrivate(isPrivate);
        metadataMapper.insert(fileMetadataInfo);
        return fileMetadataInfo;
    }

    /**
     * 构建结果
     * 构建文件预检结果
     *
     * @param metadataInfo 元数据信息
     * @param partList     块信息
     * @param partCount    块数量
     * @param isDone       是否秒传
     * @return {@link FileCheckResultVo}
     */
    private FileCheckResultVo buildResult(FileMetadataInfo metadataInfo, List<FileCheckResultVo.Part> partList, Integer partCount, Boolean isDone) {
        FileCheckResultVo fileCheckResultVo = new FileCheckResultVo();
        // 主键
        fileCheckResultVo.setId(metadataInfo.getId());
        // 文件KEY
        fileCheckResultVo.setFileKey(metadataInfo.getFileKey());
        // 文件md5
        fileCheckResultVo.setFileMd5(metadataInfo.getFileMd5());
        // 文件名
        fileCheckResultVo.setFileName(metadataInfo.getFileName());
        // MIME类型
        fileCheckResultVo.setFileMimeType(metadataInfo.getFileMimeType());
        // 文件后缀
        fileCheckResultVo.setFileSuffix(metadataInfo.getFileSuffix());
        // 文件长度
        fileCheckResultVo.setFileSize(metadataInfo.getFileSize());
        // 是否秒传
        fileCheckResultVo.setIsDone(isDone);
        // 分块数量
        fileCheckResultVo.setPartCount(partCount);
        // 分块大小
        fileCheckResultVo.setPartSize(properties.getPart().getSize());
        // 分块信息
        fileCheckResultVo.setPartList(partList);
        return fileCheckResultVo;
    }

    /**
     * 合并分片
     *
     * @param metadataInfo 文件元数据信息
     * @param partMd5List  分片集合
     * @return 合并结果
     */
    public CompleteResultVo completeMultipartUpload(FileMetadataInfo metadataInfo, List<String> partMd5List) {

        CompleteResultVo completeResultVo = new CompleteResultVo();

        // 获取所有的分片信息
        ListParts listParts = this.getListParts(metadataInfo);

        List<Integer> missingNumbers = new ArrayList<>();

        // 分块数量
        Integer chunkNum = metadataInfo.getPartNumber();

        if (partMd5List == null || chunkNum != partMd5List.size()) {
            throw new MinioPlusException(MinioPlusErrorCode.FILE_PART_NUM_CHECK_FAILED);
        }

        // 校验文件完整性
        for (int i = 1; i <= chunkNum; i++) {
            boolean findPart = false;
            for (ListParts.Part part : listParts.getPartList()) {
                if (part.getPartNumber() == i && CharSequenceUtil.equalsIgnoreCase(part.getEtag(), partMd5List.get(i - 1))) {
                    findPart = true;
                }
            }
            if (!findPart) {
                missingNumbers.add(i);
            }
        }

        if (CollUtil.isNotEmpty(missingNumbers)) {
            CreateUploadUrlReqBO bo = new CreateUploadUrlReqBO();
            // 文件md5
            bo.setFileMd5(metadataInfo.getFileMd5());
            // 文件名（含扩展名）
            bo.setFullFileName(metadataInfo.getFileName());
            // "文件长度"
            bo.setFileSize(metadataInfo.getFileSize());
            // 是否断点续传 0:否 1:是,默认非断点续传
            bo.setIsSequel(Boolean.TRUE);
            // 丢失的块号-断点续传时必传
            bo.setMissPartNum(missingNumbers);
            if (missingNumbers.size() != chunkNum) {
                // 任务id，任务id可能会失效
                bo.setUploadId(metadataInfo.getUploadTaskId());
            }
            // 存储桶
            bo.setStorageBucket(metadataInfo.getStorageBucket());
            // 存储路径
            bo.setStoragePath(metadataInfo.getStoragePath());
            // 文件id
            bo.setFileKey(metadataInfo.getFileKey());
            CreateUploadUrlRespBO createUploadUrlRespBO = this.createUploadUrl(bo);

            completeResultVo.setIsComplete(false);
            completeResultVo.setUploadTaskId(createUploadUrlRespBO.getUploadTaskId());
            completeResultVo.setPartList(createUploadUrlRespBO.getParts());
        } else {
            // 合并分块
            boolean writeResponse = minioS3Client.completeMultipartUpload(metadataInfo.getStorageBucket()
                    , listParts.getObjectName()
                    , metadataInfo.getUploadTaskId()
                    , listParts.getPartList()
            );
            completeResultVo.setIsComplete(writeResponse);
            completeResultVo.setPartList(new ArrayList<>());
        }

        return completeResultVo;
    }

    /**
     * 获取分片信息
     *
     * @param metadataInfo 文件元数据信息
     * @return {@link ListParts}    分片任务信息
     */
    private ListParts getListParts(FileMetadataInfo metadataInfo) {
        String objectName = CommonUtil.getObjectName(metadataInfo.getFileMd5());
        // 获取所有的分片信息
        return minioS3Client.listParts(metadataInfo.getStorageBucket(), objectName, metadataInfo.getPartNumber(), metadataInfo.getUploadTaskId());
    }


    /**
     * 根据用户取得文件元数据信息
     * 当userId匹配时直接返回，不匹配时检查是否存在公有元数据
     *
     * @param fileKey 文件KEY
     * @return 文件元数据信息
     */
    private FileMetadataInfo getFileMetadataInfo(String fileKey) {
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileKey, fileKey);
        // 取得元数据
        return metadataMapper.selectOne(queryWrapper);
    }

    /**
     * 文件元数据
     *
     * @param metadata 文件元数据
     * @author <a href="mailto:tianxiang.deng@foxmail.com">BaldHead</a>
     * @since 2024-06-14 14:44:52
     */
    private void generatePreviewImage(FileMetadataInfo metadata) {
        try {
            if (metadata.getIsPreview() == 1) {

                // 获取原图的bytes
                byte[] imageOriginBytes = minioS3Client.getObject(StorageBucketEnums.IMAGE.getCode(), metadata.getStoragePath() + "/" + metadata.getFileMd5());

                // 定义文件流
                @Cleanup ByteArrayInputStream imageOriginInputStream = new ByteArrayInputStream(imageOriginBytes);
                @Cleanup ByteArrayOutputStream previewImage = new ByteArrayOutputStream();

                // 根据文件流和重设宽度进行图片压缩
                Thumbnails.of(imageOriginInputStream)
                        .width(properties.getThumbnail().getSize())
                        .outputQuality(0.9f)
                        .toOutputStream(previewImage);

                byte[] previewImageBytes = previewImage.toByteArray();
                @Cleanup ByteArrayInputStream previewImageInputStream = new ByteArrayInputStream(previewImageBytes);

                minioS3Client.putObject(StorageBucketEnums.IMAGE_PREVIEW.getCode(), CommonUtil.getObjectName(metadata.getFileMd5()), previewImageInputStream, previewImageBytes.length, metadata.getFileMimeType());
                metadata.setIsPreview(1);
                //更新为可预览
                FileMetadataInfo updateMetadataInfo = new FileMetadataInfo();
                updateMetadataInfo.setId(metadata.getId());
                updateMetadataInfo.setIsPreview(1);
                metadataMapper.updateById(updateMetadataInfo);
            }
        } catch (Exception e) {
            // 打印日志
            log.error(e.getMessage(), e);
            // 缩略图生成失败
            throw new MinioPlusException(MinioPlusErrorCode.FILE_PREVIEW_WRITE_FAILED);
        }
    }

    /**
     * 重写文件地址
     *
     * @param url 文件地址
     * @return 重写后的文件地址
     */
    private String remakeUrl(String url) {

        if (CharSequenceUtil.isNotBlank(properties.getBrowserUrl())) {
            return url.replace(properties.getBackend(), properties.getBrowserUrl());
        }
        return url;
    }

    /**
     * 根据fileMd5 查询文件元数据信息
     *
     * @param fileMd5    文件md5值
     * @param isFinished 上传状态,0未完成,1已完成
     * @return list
     */
    public List<FileMetadataInfo> getMetadataByFileMd5(String fileMd5, Integer isFinished, Integer isPart) {
        // 根据MD5查询元数据 文件是否已上传过
        LambdaQueryWrapper<FileMetadataInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadataInfo::getFileMd5, fileMd5);
        //是否上传完成
        if (isFinished != null) {
            queryWrapper.eq(FileMetadataInfo::getIsFinished, isFinished);
        }
        //是否分片上传
        if (isPart != null) {
            queryWrapper.eq(FileMetadataInfo::getIsPart, isPart);
        }
        return metadataMapper.selectList(queryWrapper);
    }

    /**
     * 是否支持秒传判断
     * fileMd5 已存在,且上传状态IsFinished是已完成
     *
     * @param list         元数据集合
     * @param fileMd5      文件md5
     * @param fullFileName 文件全名
     * @param isPrivate    是否私有
     */
    public FileMetadataInfo fileFastUpload(List<FileMetadataInfo> list, String fileMd5, String fullFileName, Integer isPrivate) {
        //  1.有上传记录，且状态是已完成，则秒传，新增一条文件元数据,指向曾经的文件
        for (FileMetadataInfo fileMetadataInfo : list) {
            if (fileMetadataInfo.getIsFinished() == 1) {
                FileMetadataInfo metadata = new FileMetadataInfo();
                // 秒传
                metadata.setFileKey(IdUtil.fastSimpleUUID()); // 文件KEY
                metadata.setFileMd5(fileMd5); // 文件md5
                metadata.setFileName(fullFileName); // 文件名
                metadata.setFileMimeType(fileMetadataInfo.getFileMimeType()); // MIME类型
                metadata.setFileSuffix(fileMetadataInfo.getFileSuffix()); // 文件后缀
                metadata.setFileSize(fileMetadataInfo.getFileSize()); // 文件长度
                metadata.setStorageBucket(fileMetadataInfo.getStorageBucket()); // 存储桶
                metadata.setStoragePath(fileMetadataInfo.getStoragePath()); // 存储桶路径
                metadata.setIsFinished(fileMetadataInfo.getIsFinished()); // 状态 0:未完成 1:已完成
                metadata.setIsPart(1); // 是否分片 0:不分片 1:分片
                metadata.setPartNumber(fileMetadataInfo.getPartNumber()); // 分片数量
                metadata.setIsPreview(fileMetadataInfo.getIsPreview()); // 预览图 0:无 1:有
                metadata.setIsPrivate(isPrivate); // 是否私有 0:否 1:是
                metadataMapper.insert(metadata);
                return metadata;
            }
        }
        return null;
    }
}
