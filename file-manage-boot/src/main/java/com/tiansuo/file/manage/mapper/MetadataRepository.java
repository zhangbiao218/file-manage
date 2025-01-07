package com.tiansuo.file.manage.mapper;



import com.tiansuo.file.manage.model.dto.FileMetadataInfoDTO;
import com.tiansuo.file.manage.model.dto.FileMetadataInfoSaveDTO;
import com.tiansuo.file.manage.model.dto.FileMetadataInfoUpdateDTO;
import com.tiansuo.file.manage.model.vo.FileMetadataInfoVo;

import java.util.List;

/**
 * 文件元数据服务接口定义
 *
 * @author zhangb
 * @since 2023/06/26
 */
public interface MetadataRepository {

    /**
     * 根据条件列表查询
     * @param searchDTO 查询条件
     * @return 列表结果集
     */
    List<FileMetadataInfoVo> list(FileMetadataInfoDTO searchDTO);

    /**
     * 根据条件单条查询
     *
     * @param searchDTO 查询条件
     * @return 单条结果
     */
    FileMetadataInfoVo one(FileMetadataInfoDTO searchDTO);

    /**
     * 新增
     *
     * @param saveDTO 数据实体
     * @return 执行结果
     */
    FileMetadataInfoVo save(FileMetadataInfoSaveDTO saveDTO);

    /**
     * 修改数据
     *
     * @param updateDTO 数据实体
     * @return 执行结果
     */
    FileMetadataInfoVo update(FileMetadataInfoUpdateDTO updateDTO);

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 执行结果
     */
    Boolean remove(Long id);

}
