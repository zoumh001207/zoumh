package com.zoumh;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


/**
 * zoumh模块
 *
 * @author hegc
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PlatformApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(PlatformApplication.class, args);
        System.out.println(">>zoumh模块启动成功  \n" +
                " __      __ __ _  _ __   _ __  ___  _ __  \n" +
                " \\ \\ /\\ / // _` || '_ \\ | '__|/ _ \\| '_ \\ \n" +
                "  \\ V  V /| (_| || | | || |  |  __/| | | |\n" +
                "   \\_/\\_/  \\__,_||_| |_||_|   \\___||_| |_|");
    }
}
