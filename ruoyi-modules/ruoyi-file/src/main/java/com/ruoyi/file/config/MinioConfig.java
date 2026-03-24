package com.ruoyi.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

/**
 * Minio 配置信息
 *
 * @author ruoyi
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig
{
    /**
     * 服务地址
     */
    private String url;

    /**
     * 用户名
     */
    private String accessKey;

    /**
     * 密码
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 是否自动创建桶
     */
    private boolean autoCreateBucket;

    /**
     * 是否自动写入公开读桶策略
     */
    private boolean manageBucketPolicy;

    /**
     * 文件访问域名（用于生成前端可访问的 HTTPS URL）
     * 例如: https://zoumh.com/minio-data
     */
    private String domain;
    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        this.bucketName = bucketName;
    }

    public boolean isAutoCreateBucket()
    {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket)
    {
        this.autoCreateBucket = autoCreateBucket;
    }

    public boolean isManageBucketPolicy()
    {
        return manageBucketPolicy;
    }

    public void setManageBucketPolicy(boolean manageBucketPolicy)
    {
        this.manageBucketPolicy = manageBucketPolicy;
    }

    @Bean
    public MinioClient getMinioClient()
    {
        MinioClient client = MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
        client.disableVirtualStyleEndpoint();
        return client;
    }
}
