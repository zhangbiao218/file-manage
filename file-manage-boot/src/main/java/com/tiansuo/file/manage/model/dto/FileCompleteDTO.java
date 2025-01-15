package com.tiansuo.file.manage.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 文件完成入参DTO
 *
 * @author zhangb
 */
@Getter
@Setter
@ToString
@ApiModel("文件完成入参DTO")
public class FileCompleteDTO {

    @ApiModelProperty(value = "文件md5", required = true)
    private List<String> partMd5List;

    @ApiModelProperty(value = "文件的唯一key", required = true)
    private String fileKey;

}