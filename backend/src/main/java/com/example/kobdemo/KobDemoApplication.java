package com.example.kobdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.kobdemo.mapper")
public class KobDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(KobDemoApplication.class, args);
    }
}
