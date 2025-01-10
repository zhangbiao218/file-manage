package com.tiansuo.file.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.tiansuo.file.manage.mapper")
@SpringBootApplication
public class FileManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileManageApplication.class, args);
        System.out.println("=================================服务启动成功!=================================");
    }

}
