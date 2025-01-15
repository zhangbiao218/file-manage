package com.tiansuo.file.manage.controller;

import com.tiansuo.file.manage.enums.MinioPlusErrorCode;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;


@Api("文件服务接口")
@Slf4j
@RestController
@RequestMapping("/storage")
public class StorageController {

    /**
     * 存储引擎Service接口定义
     */
    @Autowired
    private StorageService storageService;


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
    @ApiOperation(value = "文件上传完成(合并)")
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
     * 获取图片原图
     *
     * @param fileKey 文件KEY
     * @return 原图地址
     */
    @ApiOperation(value = "图片预览(原图)")
    @GetMapping("/image")
    public ResultModel<String> previewOriginal(@RequestParam(value = "fileKey") String fileKey) {
        return ResultModel.success(storageService.image(fileKey));
    }

    /**
     * 获取图片的缩略图
     *
     * @param fileKey 文件KEY
     * @return 缩略图地址
     */
    @ApiOperation(value = "图片预览 - 缩略图")
    @GetMapping("/preview")
    public  ResultModel<String> previewMedium(@RequestParam(value = "fileKey") String fileKey) {
        String url = storageService.preview(fileKey);
        return  ResultModel.success(url) ;
    }

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