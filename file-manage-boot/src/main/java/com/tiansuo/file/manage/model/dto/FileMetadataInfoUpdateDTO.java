package com.tiansuo.file.manage.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 文件元数据信息修改入参
 * @author zhangb
 * @since  2023-06-26
 **/
@Getter
@Setter
@ToString
@ApiModel( "文件元数据信息修改入参")
public class FileMetadataInfoUpdateDTO {

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

    @ApiModelProperty(value = "上传任务id,用于合并切片")
    private String uploadTaskId;

    @ApiModelProperty(value = "状态 false:未完成 true:已完成")
    private Boolean isFinished;

    @ApiModelProperty(value = "是否分块 false:否 true:是")
    private Boolean isPart;

    @ApiModelProperty(value = "分块数量")
    private Integer partNumber;

    @ApiModelProperty(value = "预览图 false:无 true:有")
    private Boolean isPreview;

    @ApiModelProperty(value = "是否私有 false:否 true:是")
    private Boolean isPrivate;

    @ApiModelProperty(value = "修改人")
    private String updateUser;

}