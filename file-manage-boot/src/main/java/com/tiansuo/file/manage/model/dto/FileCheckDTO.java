package com.tiansuo.file.manage.model.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 文件预检查DTO
 *
 * @author zhangb
 * @since 2023/6/26
 */
@Getter
@Setter
@ToString
@ApiModel("文件预检查入参DTO")
public class FileCheckDTO {

    @ApiModelProperty(value = "文件md5",required = true)
    private String fileMd5;

    @ApiModelProperty(value = "文件名（含扩展名）", required = true)
    private String fullFileName;

    @ApiModelProperty(value = "文件长度", required = true)
    private Long fileSize;

    @ApiModelProperty(value = "是否私有 0:否 1:是")
    private Integer isPrivate = 0;


}