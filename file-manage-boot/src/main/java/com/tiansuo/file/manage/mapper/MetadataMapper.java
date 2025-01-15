package com.tiansuo.file.manage.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiansuo.file.manage.model.entity.FileMetadataInfo;
import com.tiansuo.file.manage.model.vo.FileUploadResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件元数据服务接口定义
 *
 * @author zhangb
 */
public interface MetadataMapper extends BaseMapper<FileMetadataInfo> {

    Integer updateBusinessKey(@Param("fileKey") String fileKey, @Param("businessKey") String businessKey);

    List<FileUploadResultVo> queryByBusinessKey(@Param("businessKey")String businessKey);

}
