package com.hn.yuan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hn.yuan.mapper")
public class DemoWxpayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoWxpayApplication.class, args);
    }

}
