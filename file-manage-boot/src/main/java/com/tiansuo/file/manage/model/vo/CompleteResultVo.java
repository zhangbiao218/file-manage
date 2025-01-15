package com.tiansuo.file.manage.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件完整性校验结果VO
 *
 * @author zhangb
 **/
@Getter
@Setter
@ApiModel(value = "文件完整性校验结果")
public class CompleteResultVo {

    @ApiModelProperty(value = "是否完成")
    private Boolean isComplete;

    @ApiModelProperty(value = "上传任务编号")
    private String uploadTaskId;

    @ApiModelProperty(value = "补传的分块信息")
    private List<FileCheckResultVo.Part> partList = new ArrayList<>();

}