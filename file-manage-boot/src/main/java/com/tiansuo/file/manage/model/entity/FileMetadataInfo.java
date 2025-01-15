package com.tiansuo.file.manage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Date;

/**
 * 文件元数据信息VO
 *
 * @author zhangb
 **/
@Accessors(chain = true)
@Getter
@Setter
@ApiModel(value = "文件元数据信息")
@TableName(value = "file_metadata_info")
public class FileMetadataInfo {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;

    @TableField(value = "file_key")
    @ApiModelProperty(value = "文件KEY")
    private String fileKey;

    @TableField(value = "business_key")
    @ApiModelProperty(value = "所属业务唯一标志")
    private String businessKey;

    @TableField(value = "file_md5")
    @ApiModelProperty(value = "文件md5")
    private String fileMd5;

    @TableField(value = "file_name")
    @ApiModelProperty(value = "文件名")
    private String fileName;

    @TableField(value = "file_mime_type")
    @ApiModelProperty(value = "MIME类型")
    private String fileMimeType;

    @TableField(value = "file_suffix")
    @ApiModelProperty(value = "文件后缀")
    private String fileSuffix;

    @TableField(value = "file_size")
    @ApiModelProperty(value = "文件长度")
    private Long fileSize;


    @TableField(value = "storage_bucket")
    @ApiModelProperty(value = "存储桶")
    private String storageBucket;

    @TableField(value = "storage_path")
    @ApiModelProperty(value = "存储路径")
    private String storagePath;

    @TableField(value = "upload_task_id")
    @ApiModelProperty(value = "minio切片任务id")
    private String uploadTaskId;

    @TableField(value = "is_finished")
    @ApiModelProperty(value = "状态 0:未完成 1:已完成")
    private Integer isFinished;

    @TableField(value = "is_part")
    @ApiModelProperty(value = "是否分块 0:否 1:是")
    private Integer isPart;

    @TableField(value = "part_number")
    @ApiModelProperty(value = "分块数量")
    private Integer partNumber;

    @TableField(value = "is_preview")
    @ApiModelProperty(value = "预览图 0:无 1:有")
    private Integer isPreview;

    @TableField(value = "is_private")
    @ApiModelProperty(value = "是否私有 0:否 1:是")
    private Integer isPrivate;

    @TableField(value = "create_time")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

}