package com.ruoyi.file.service;

import java.io.InputStream;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.nacos.common.utils.IoUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.file.config.MinioConfig;
import com.ruoyi.file.utils.FileUploadUtils;

/**
 * Minio 文件存储
 *
 * @author ruoyi
 */
@Service
@Primary
public class MinioSysFileServiceImpl implements ISysFileService
{
    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient client;

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        InputStream inputStream = null;
        try
        {
            String fileName = FileUploadUtils.extractFilename(file);
            inputStream = file.getInputStream();

            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!bucketExists && minioConfig.isAutoCreateBucket())
            {
                client.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
                bucketExists = true;
            }

            if (!bucketExists)
            {
                throw new IllegalStateException("Bucket does not exist: " + minioConfig.getBucketName());
            }

            if (minioConfig.isManageBucketPolicy())
            {
                String policyJson = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [{\n" +
                        "    \"Effect\": \"Allow\",\n" +
                        "    \"Principal\": {\"AWS\": [\"*\"]},\n" +
                        "    \"Action\": [\"s3:GetObject\"],\n" +
                        "    \"Resource\": [\"arn:aws:s3:::" + minioConfig.getBucketName() + "/*\"]\n" +
                        "  }]\n" +
                        "}";
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .config(policyJson)
                        .build());
            }

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);

            String domain = minioConfig.getDomain();
            if (StringUtils.isNotEmpty(domain))
            {
                // 外部 S3 兼容存储直接走公开访问域名
                return domain + "/" + minioConfig.getBucketName() + "/" + fileName;
            }
            else
            {
                String url = minioConfig.getUrl().replace("http://", "https://");
                return url + "/" + minioConfig.getBucketName() + "/" + fileName;
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to upload file", e);
        }
        finally
        {
            IoUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Minio文件删除接口
     * 
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        try
        {
            String minioFile = StringUtils.substringAfter(fileUrl, minioConfig.getBucketName());
            minioFile = StringUtils.stripStart(minioFile, "/");
            client.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(minioFile).build());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to delete file", e);
        }
    }
}
