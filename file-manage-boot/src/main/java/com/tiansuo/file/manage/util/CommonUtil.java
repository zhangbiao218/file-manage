package com.tiansuo.file.manage.util;

import cn.hutool.core.date.LocalDateTimeUtil;

/**
 * 对象存储工具类
 * @author zhangb
 * @since  2024/05/23
 */
public class CommonUtil {

    /**
     * 取得对象名称
     * @param fileMd5 文件fileMd5
     * @return 对象名称
     */
    public static String getObjectName(String fileMd5){
        return CommonUtil.getPathByDate() + "/" + fileMd5;
    }

    /**
     * 根据当前时间取得路径
     * @return 路径
     */
    public static String getPathByDate(){
        return LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "yyyy/MM");
    }

}
