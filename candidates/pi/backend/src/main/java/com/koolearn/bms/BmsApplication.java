package com.koolearn.bms;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.koolearn.bms.mapper")
public class BmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BmsApplication.class, args);
    }
}