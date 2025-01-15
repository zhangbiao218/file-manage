package com.tiansuo.file.manage.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件预分片结果
 * @author zhangb
 **/
@Getter
@Setter
@ApiModel(value = "文件预分片结果")
public class FilePreShardingVo {

    /**
     * 文件长度
     */
    @ApiModelProperty(value = "文件长度")
    private Long fileSize;

    /**
     * 分块数量
     */
    @ApiModelProperty(value = "分块数量")
    private Integer partCount;

    /**
     * 分块大小
     */
    @ApiModelProperty(value = "分块大小")
    private Integer partSize;

    /**
     * 分块信息
     */
    @ApiModelProperty(value = "分块信息")
    private List<Part> partList = new ArrayList<>();

    /**
     * 分块信息实体定义
     */
    @Getter
    @Setter
    public static class Part {

        /**
         * 开始位置
         */
        @ApiModelProperty(value = "开始位置")
        private Long startPosition;
        /**
         * 结束位置
         */
        @ApiModelProperty(value = "结束位置")
        private Long endPosition;

    }

}