package com.zoumh.hotelmonitor;

import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCustomConfig
@EnableRyFeignClients
@EnableScheduling
@MapperScan("com.zoumh.hotelmonitor.mapper")
@SpringBootApplication
public class HotelMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelMonitorApplication.class, args);
    }
}
