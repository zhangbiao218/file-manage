package com.tiansuo.file.manage.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 文件元数据信息VO
 *
 * @author zhangb
 * @since 2023-06-26
 **/
@Getter
@Setter
@ApiModel(value = "文件元数据信息")
public class FileMetadataInfoVo {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "文件KEY")
    private String fileKey;

    @ApiModelProperty(value = "文件md5")
    private String fileMd5;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "MIME类型")
    private String fileMimeType;

    @ApiModelProperty(value = "文件后缀")
    private String fileSuffix;

    @ApiModelProperty(value = "文件长度")
    private Long fileSize;

    @ApiModelProperty(value = "存储引擎")
    private String storageEngine;

    @ApiModelProperty(value = "存储桶")
    private String storageBucket;

    @ApiModelProperty(value = "存储路径")
    private String storagePath;

    @ApiModelProperty(value = "minio切片任务id")
    private String uploadTaskId;

    @ApiModelProperty(value = "状态 0:未完成 1:已完成")
    private Boolean isFinished;

    @ApiModelProperty(value = "是否分块 0:否 1:是")
    private Boolean isPart;

    @ApiModelProperty(value = "分块数量")
    private Integer partNumber;

    @ApiModelProperty(value = "预览图 0:无 1:有")
    private Boolean isPreview;

    @ApiModelProperty(value = "是否私有 0:否 1:是")
    private Boolean isPrivate;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改人")
    private String updateUser;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

}