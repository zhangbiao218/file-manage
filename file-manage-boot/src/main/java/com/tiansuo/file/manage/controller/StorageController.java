package com.tiansuo.file.manage.controller;

import cn.hutool.core.io.IoUtil;
import com.sun.xml.internal.bind.v2.TODO;
import com.tiansuo.file.manage.constant.MinioPlusErrorCode;
import com.tiansuo.file.manage.constant.StorageBucketEnums;
import com.tiansuo.file.manage.exception.MinioPlusException;
import com.tiansuo.file.manage.model.dto.BusinessBindFileDTO;
import com.tiansuo.file.manage.model.dto.FileCheckDTO;
import com.tiansuo.file.manage.model.dto.FileCompleteDTO;
import com.tiansuo.file.manage.model.vo.CompleteResultVo;
import com.tiansuo.file.manage.model.vo.FileCheckResultVo;
import com.tiansuo.file.manage.model.vo.FilePreShardingVo;
import com.tiansuo.file.manage.model.vo.FileUploadResultVo;
import com.tiansuo.file.manage.response.ResultModel;
import com.tiansuo.file.manage.service.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 对象存储标准接口定义
 * 本类的方法是给前端使用的方法
 *
 * @author zhangb
 * @since 2024/6/18
 */
@Api("文件上传")
@Slf4j
@RestController
@RequestMapping("/storage")
public class StorageController {

    /**
     * 存储引擎Service接口定义
     */
    @Autowired
    private StorageService storageService;


    //TODO 1,不需要分片的文件上传,2需要分片的文件上传


    //1,前端调用文件上传,
    // 2,返回给前端地址,文件名,id,大小,
    // 3,前端把这些参数以及表单的其他内容一起提交表单,后端处理表单的时候,根据前端传的这些参数在文件记录表里插入一条数据,需要在提供一个接口保存文件记录以及和业务的关联关系
    //4,后端想再次获取文件的时候,需要根据业务的id去文件记录表查询到相应的路径,包括对文件的删除,修改,都是在后端保存表单信息的时候调用处理
    //5,后续需增加定时任务处理,前端表单只上传了个文件,未保存表单,导致文件是冗余文件的逻辑

    /**
     * 上传文件(小文件不分片)
     *
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    @ApiOperation(value = "文件上传(普通)")
    @GetMapping("/upload/file")
    public ResultModel<FileUploadResultVo> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResultVo fileUploadResultVo = storageService.uploadFile(file);
        return ResultModel.success(fileUploadResultVo);
    }



    /**
     * 文件预分片方法
     * 在大文件上传时，为了防止前端重复计算文件MD5值，提供该方法
     *
     * @param fileSize 文件大小
     * @return 预分片结果
     */
    @ApiOperation(value = "文件预分片方法")
    @GetMapping("/upload/sharding")
    public ResultModel<FilePreShardingVo> sharding(@RequestParam("fileSize") Long fileSize) {

        FilePreShardingVo resultVo = storageService.sharding(fileSize);

        return ResultModel.success(resultVo);
    }

    /**
     * 分片上传任务初始化
     * 上传前的预检查：秒传、分块上传和断点续传等特性均基于该方法实现
     *
     * @param fileCheckDTO 文件预检查入参
     * @return 检查结果
     */
    @ApiOperation(value = "分片上传任务初始化")
    @PostMapping("/upload/init")
    public ResultModel<FileCheckResultVo> init(@RequestBody FileCheckDTO fileCheckDTO) {
        FileCheckResultVo resultVo = storageService.init(fileCheckDTO.getFileMd5(), fileCheckDTO.getFullFileName(), fileCheckDTO.getFileSize(), fileCheckDTO.getIsPrivate());
        return ResultModel.success(resultVo);
    }

    /**
     * 文件上传完成
     *
     * @param fileCompleteDTO 文件完成入参DTO
     * @return 是否成功
     */
    @ApiOperation(value = "文件上传完成")
    @PostMapping("/upload/complete")
    public ResultModel<Object> complete(@RequestBody FileCompleteDTO fileCompleteDTO) {
        CompleteResultVo completeResultVo = storageService.complete(fileCompleteDTO.getFileKey(), fileCompleteDTO.getPartMd5List());
        return ResultModel.success(completeResultVo);
    }

    /**
     * 文件下载(返回文件地址供前端访问下载)
     *
     * @param fileKey 文件KEY
     * @return 文件下载地址
     */
    @ApiOperation(value = "文件下载(返回文件地址供前端访问下载)")
    @GetMapping("/download/url")
    public ResultModel<String> download(@RequestParam(value = "fileKey") String fileKey) {
        return ResultModel.success(storageService.download(fileKey));
    }


    /**
     * 文件下载(返回文件流直接下载)
     *
     * @param fileKey 文件KEY
     */
    @ApiOperation(value = "文件下载(返回文件流直接下载)")
    @GetMapping("/download/stream")
    public void download(@RequestParam(value = "fileKey") String fileKey, HttpServletResponse response) {
        storageService.getDownloadObject(fileKey,response);

    }

    /**
     * 获取图像
     *
     * @param fileKey 文件KEY
     * @return 原图地址
     */
    @ApiOperation(value = "图片预览 - 原图")
    @GetMapping("/image")
    public ResultModel<String> previewOriginal(@RequestParam(value = "fileKey") String fileKey) {
        return ResultModel.success(storageService.image(fileKey));
    }

    /**
     * 文件预览
     * 当文件为图片时，返回图片的缩略图
     * 当文件不是图片时，返回文件类型图标
     *
     * @param fileKey 文件KEY
     * @return 缩略图地址
     */
    @ApiOperation(value = "图片预览 - 缩略图")
    @GetMapping("/preview")
    public  ResultModel<String> previewMedium(@RequestParam(value = "fileKey") String fileKey) {
        String url = storageService.preview(fileKey);
        if (url.length() < 10) {
            // 当返回值为文件类型时，取得图标
            //url = ICON_PATH + url;
        }

        // 取得文件读取路径
        return  ResultModel.success(url) ;
    }

    /**
     * 根据文件类型取得图标
     *
     * @param fileType 文件扩展名
     */
    @ApiOperation(value = "获取图标")
    @GetMapping("/icon")
    public void icon(@RequestParam(value = "fileType") String fileType) {
        try {
            // 根据文件后缀取得桶
            String storageBucket = StorageBucketEnums.getBucketByFileSuffix(fileType);

            ClassPathResource cpr = new ClassPathResource(storageBucket + ".png");

            byte[] bytes = FileCopyUtils.copyToByteArray(cpr.getInputStream());

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            attr.getResponse().setHeader("content-disposition", "inline");
            attr.getResponse().setHeader("Content-Length", String.valueOf(bytes.length));
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                IoUtil.copy(inputStream, attr.getResponse().getOutputStream());
            }
        } catch (Exception e) {
            log.error(MinioPlusErrorCode.FILE_ICON_FAILED.getMessage(), e);
            // 图标获取失败
            throw new MinioPlusException(MinioPlusErrorCode.FILE_ICON_FAILED);
        }
    }

    //todo 提供绑定businessKey的接口,和根据这个key获取文件下载地址的接口
    //0,1的优化,日志的优化,实体类的优化

    /**
     * 绑定业务数据和文件数据
     */
    @ApiOperation(value = "上传的文件绑定业务数据")
    @PostMapping("/bind/business")
    public ResultModel bindBusinessAndFile(@RequestBody BusinessBindFileDTO businessBindFileDTO) {
        if (storageService.bindBusinessAndFile(businessBindFileDTO.getFileKeyList(),businessBindFileDTO.getBusinessKey())){
            return ResultModel.success("绑定成功");
        }else{
            return ResultModel.fail(MinioPlusErrorCode.FILE_BIND_BUSINESS_FAILED.getCode(),MinioPlusErrorCode.FILE_BIND_BUSINESS_FAILED.getMessage());
        }
    }

    /**
     * 根据businessKey查询绑定的文件列表
     *
     * @param businessKey 业务唯一标识
     * @return 绑定的文件列表
     */
    @ApiOperation(value = "根据businessKey查询绑定的文件列表")
    @GetMapping("/query/file")
    public ResultModel<List<FileUploadResultVo>> getFileByBusinessKey(@RequestParam(value = "businessKey") String businessKey) {
        return ResultModel.success(storageService.getFileByBusinessKey(businessKey));
    }

    /**
     * 根据businessKey删除文件
     *
     * @param businessKey 业务唯一标识
     */
    @ApiOperation(value = "根据businessKey删除文件")
    @GetMapping("/delete/file/businessKey")
    public ResultModel deleteFileByBusinessKey(@RequestParam(value = "businessKey") String businessKey) {
        return ResultModel.success(storageService.deleteFileByBusinessKey(businessKey));
    }

    /**
     * 根据fileKey删除文件
     *
     * @param fileKey 文件唯一标识
     */
    @ApiOperation(value = "根据fileKey删除文件")
    @GetMapping("/delete/file/fileKey")
    public ResultModel deleteFileByFileKey(@RequestParam(value = "fileKey") String fileKey) {
        return ResultModel.success(storageService.deleteFileByFileKey(fileKey));
    }

}