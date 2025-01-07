package com.tiansuo.file.manage.controller;

import cn.hutool.core.io.IoUtil;
import com.sun.xml.internal.bind.v2.TODO;
import com.tiansuo.file.manage.constant.MinioPlusErrorCode;
import com.tiansuo.file.manage.constant.StorageBucketEnums;
import com.tiansuo.file.manage.exception.MinioPlusException;
import com.tiansuo.file.manage.model.dto.FileCheckDTO;
import com.tiansuo.file.manage.model.dto.FileCompleteDTO;
import com.tiansuo.file.manage.model.dto.PreShardingDTO;
import com.tiansuo.file.manage.model.vo.CompleteResultVo;
import com.tiansuo.file.manage.model.vo.FileCheckResultVo;
import com.tiansuo.file.manage.model.vo.FilePreShardingVo;
import com.tiansuo.file.manage.model.vo.FileUploadResultVo;
import com.tiansuo.file.manage.response.ResultModel;
import com.tiansuo.file.manage.service.StorageService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

/**
 * 对象存储标准接口定义
 * 本类的方法是给前端使用的方法
 *
 * @author zhangb
 * @since 2024/6/18
 */
@Slf4j
@RestController
@RequestMapping("/storage")
public class StorageController {

    /**
     * 重定向
     */
    private static final String REDIRECT_PREFIX = "redirect:";

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
     * @param file 上传的文件
     * @return 上传成功后的返回信息
     */
    @ApiOperation(value = "文件上传(普通)")
    @PostMapping("/upload/file")
    public ResultModel<FileUploadResultVo> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResultVo fileUploadResultVo = storageService.uploadFile(file);
        return ResultModel.success(fileUploadResultVo);
    }


    /**
     * 文件分片上传步骤:
     *  step1:前端调用/upload/sharding,传参文件大小,进行预分片,返回给前端分片次数
     *  step2:
     */
    /**
     * 上传分片
     * 说明：前端通过后端上传分片到MinIO，对比：preSignUploadUrl
     * @author 明快de玄米61
     * @date   2022/12/15 14:56
     * @param  file 分片文件
     * @param  uploadId 任务id
     * @param  partNumber 当前分片编号
     **/
    @PostMapping("/uploadPart")
    public ResultModel uploadPart(@RequestPart MultipartFile file, @RequestParam String uploadId, @RequestParam Integer partNumber) {

        // 上传分片
        return storageService.uploadPart(file,uploadId,partNumber)
        try {
            UploadPartResponse response = minIoUtil.uploadPart(task.getBucketName(), null, task.getRemoteFileUrl(), file.getInputStream(), file.getSize(), task.getUploadId(), partNumber, null, null
            );
            return AjaxResult.success();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return AjaxResult.error();
    }

    /**
     * 文件预分片方法
     * 在大文件上传时，为了防止前端重复计算文件MD5值，提供该方法
     *
     * @param preShardingDTO 文件预分片入参DTO
     * @return 预分片结果
     */
    @ApiOperation(value = "文件预分片方法")
    @PostMapping("/upload/sharding")
    public ResultModel<FilePreShardingVo> sharding(@RequestBody @Validated PreShardingDTO preShardingDTO) {

        FilePreShardingVo resultVo = storageService.sharding(preShardingDTO.getFileSize());

        return ResultModel.success(resultVo);
    }

    /**
     * 上传任务初始化
     * 上传前的预检查：秒传、分块上传和断点续传等特性均基于该方法实现
     *
     * @param fileCheckDTO 文件预检查入参
     * @return 检查结果
     */
    @ApiOperation(value = "文件预分片方法")
    @PostMapping("/upload/init")
    public ResultModel<FileCheckResultVo> init(@RequestBody FileCheckDTO fileCheckDTO) {

        // 取得当前登录用户信息
        String userId = "admin";

        FileCheckResultVo resultVo = storageService.init(fileCheckDTO.getFileMd5(), fileCheckDTO.getFullFileName(), fileCheckDTO.getFileSize(), fileCheckDTO.getIsPrivate(), userId);

        return ResultModel.success(resultVo);
    }

    /**
     * 文件上传完成
     *
     * @param fileKey         文件KEY
     * @param fileCompleteDTO 文件完成入参DTO
     * @return 是否成功
     */
    @ApiOperation(value = "文件上传完成")
    @PostMapping("/upload/complete/{fileKey}")
    public ResultModel<Object> complete(String fileKey, FileCompleteDTO fileCompleteDTO) {

        // 取得当前登录用户信息
        String userId = "admin";

        // 打印调试日志
        log.debug("合并文件开始fileKey=" + fileKey + ",partMd5List=" + fileCompleteDTO.getPartMd5List());
        CompleteResultVo completeResultVo = storageService.complete(fileKey, fileCompleteDTO.getPartMd5List(), userId);

        return ResultModel.success(completeResultVo);
    }

    /**
     * 文件下载
     *
     * @param fileKey 文件KEY
     * @return 文件下载地址
     */
    @GetMapping("/download")
    public String download(@RequestParam(value = "fileKey") String fileKey) {

        // 取得当前登录用户信息
        String userId = "admin";

        // 取得文件读取路径
        return REDIRECT_PREFIX + storageService.download(fileKey, userId);
    }

    /**
     * 获取图像
     *
     * @param fileKey 文件KEY
     * @return 原图地址
     */
    @ApiOperation(value = "图片预览 - 原图")
    @GetMapping("/image/{fileKey}")
    public String previewOriginal(String fileKey) {

        // 取得当前登录用户信息
        String userId = "admin";

        // 取得文件读取路径
        return REDIRECT_PREFIX + storageService.image(fileKey, userId);
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
    @GetMapping("/preview/{fileKey}")
    public String previewMedium(String fileKey) {

        // 取得当前登录用户信息
        String userId = "admin";

        String url = storageService.preview(fileKey, userId);
        if (url.length() < 10) {
            // 当返回值为文件类型时，取得图标
            url = ICON_PATH + url;
        }

        // 取得文件读取路径
        return REDIRECT_PREFIX + url;
    }

    /**
     * 根据文件类型取得图标
     *
     * @param fileType 文件扩展名
     */
    @ApiOperation(value = "获取图标")
    @GetMapping("/icon/{fileType}")
    public void icon(String fileType) {
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

}