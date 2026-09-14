package com.gkstudy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.gkstudy") @SpringBootApplication
public class GkStudyApplication { public static void main(String[] args) { SpringApplication.run(GkStudyApplication.class, args); } }
