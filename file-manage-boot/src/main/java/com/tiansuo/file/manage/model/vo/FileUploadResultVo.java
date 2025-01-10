package com.tiansuo.file.manage.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class FileUploadResultVo {

    @ApiModelProperty(value = "文件KEY,唯一标记")
    private String fileKey;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "MIME类型")
    private String fileMimeType;

    @ApiModelProperty(value = "md5值")
    private String fileMd5;

    @ApiModelProperty(value = "文件后缀")
    private String fileSuffix;

    @ApiModelProperty(value = "文件长度")
    private Long fileSize;

    @ApiModelProperty(value = "存储路径")
    private String storagePath;


}
