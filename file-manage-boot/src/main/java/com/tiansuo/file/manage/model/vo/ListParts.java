package com.tiansuo.file.manage.model.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片信息列表
 *
 * @author zhangb
 */
@Getter
@Setter
@ToString
public class ListParts {

    private String bucketName;

    private String objectName;

    private int maxParts;

    private String uploadId;

    private List<Part> partList = null;

    /**
     * 构造方法
     * @return 分块列表
     */
    public static ListParts build(){
        ListParts listParts = new ListParts();
        listParts.setPartList(new ArrayList<>());
        return listParts;
    }

    /**
     * 文件分块信息定义
     */
    @Getter
    @Setter
    @ToString
    public static class Part{

        // 分块序号
        private int partNumber;
        // 分块标签(默认是MD5)
        private String etag;
        // 修改时间
        private ZonedDateTime lastModified;
        // 分块大小
        private Long size;
    }

    /**
     * 增加分块
     * @param partNumber 分块序号
     * @param etag 分块标签
     * @param lastModified 最后修改时间
     * @param size 分块大小
     */
    public void addPart(int partNumber, String etag, ZonedDateTime lastModified, Long size){

        Part part = new Part();
        part.setPartNumber(partNumber);
        part.setEtag(etag);
        part.setLastModified(lastModified);
        part.setSize(size);

        if(this.partList == null){
            partList = new ArrayList<>();
        }
        partList.add(part);
    }

}
