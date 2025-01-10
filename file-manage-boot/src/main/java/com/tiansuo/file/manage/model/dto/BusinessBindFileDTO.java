package com.tiansuo.file.manage.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@ApiModel( "业务绑定文件dto")
public class BusinessBindFileDTO {

    @ApiModelProperty(value = "文件KEY list")
    private List<String> fileKeyList;

    @ApiModelProperty(value = "业务唯一key")
    private String businessKey;



}