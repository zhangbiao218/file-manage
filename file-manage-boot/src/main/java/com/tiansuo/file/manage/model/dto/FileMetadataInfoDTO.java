package com.tiansuo.file.manage.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 文件元数据查询实体类
 *
 * @author zhangb
 * @since 2023/6/25
 */
@Getter
@Setter
@ToString
public class FileMetadataInfoDTO {

    @ApiModelProperty(value = "文件KEY")
    private String fileKey;

    @ApiModelProperty(value = "文件md5")
    private String fileMd5;

    @ApiModelProperty(value = "存储桶")
    private String bucket;

    @ApiModelProperty(value = "是否私有 false:否 true:是")
    private Boolean isPrivate;

    @ApiModelProperty(value = "状态 false:未完成 true:已完成")
    private Boolean isFinished;

    @ApiModelProperty(value = "创建人")
    private String createUser;

}