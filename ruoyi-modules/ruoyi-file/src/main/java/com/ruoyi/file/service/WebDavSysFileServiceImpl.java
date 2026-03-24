package com.ruoyi.file.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.file.config.WebDavConfig;
import com.ruoyi.file.utils.FileUploadUtils;

/**
 * WebDAV 文件存储
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "webdav")
public class WebDavSysFileServiceImpl implements ISysFileService
{
    private final WebDavConfig webDavConfig;
    private final HttpClient httpClient;

    public WebDavSysFileServiceImpl(WebDavConfig webDavConfig)
    {
        this.webDavConfig = webDavConfig;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception
    {
        String relativePath = normalizeRelativePath(FileUploadUtils.extractFilename(file));
        ensureParentDirectories(relativePath);

        HttpRequest request = requestBuilder(relativePath)
                .header(HttpHeaders.CONTENT_TYPE, StringUtils.isNotEmpty(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (!isSuccess(response.statusCode()))
        {
            throw new IllegalStateException("WebDAV upload failed with status " + response.statusCode());
        }

        return buildPublicUrl(relativePath);
    }

    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        String relativePath = decodeRelativePath(fileUrl);
        HttpRequest request = requestBuilder(relativePath).DELETE().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404)
        {
            return;
        }
        if (!isSuccess(response.statusCode()))
        {
            throw new IllegalStateException("WebDAV delete failed with status " + response.statusCode());
        }
    }

    public void writeFileToResponse(String token, HttpServletResponse response) throws Exception
    {
        String relativePath = decodeToken(token);
        HttpRequest request = requestBuilder(relativePath).GET().build();
        HttpResponse<byte[]> fileResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (fileResponse.statusCode() == 404)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!isSuccess(fileResponse.statusCode()))
        {
            throw new IllegalStateException("WebDAV read failed with status " + fileResponse.statusCode());
        }

        String contentType = fileResponse.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse("application/octet-stream");
        response.setContentType(contentType);
        fileResponse.headers().firstValue(HttpHeaders.CONTENT_LENGTH).ifPresent(value -> {
            try
            {
                response.setContentLengthLong(Long.parseLong(value));
            }
            catch (NumberFormatException ignored)
            {
            }
        });
        response.getOutputStream().write(fileResponse.body());
        response.flushBuffer();
    }

    private void ensureParentDirectories(String relativePath) throws Exception
    {
        int lastSlash = relativePath.lastIndexOf('/');
        if (lastSlash <= 0)
        {
            return;
        }

        String[] segments = relativePath.substring(0, lastSlash).split("/");
        StringBuilder current = new StringBuilder();
        for (String segment : segments)
        {
            if (StringUtils.isEmpty(segment))
            {
                continue;
            }
            if (current.length() > 0)
            {
                current.append('/');
            }
            current.append(segment);

            HttpRequest request = requestBuilder(current.toString())
                    .method("MKCOL", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 201 && response.statusCode() != 405 && response.statusCode() != 301)
            {
                throw new IllegalStateException("WebDAV MKCOL failed with status " + response.statusCode());
            }
        }
    }

    private HttpRequest.Builder requestBuilder(String relativePath)
    {
        return HttpRequest.newBuilder(buildObjectUri(relativePath))
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader());
    }

    private URI buildObjectUri(String relativePath)
    {
        String baseUrl = StringUtils.stripEnd(webDavConfig.getUrl(), "/");
        return URI.create(baseUrl + "/" + encodeRelativePath(relativePath));
    }

    private String buildPublicUrl(String relativePath)
    {
        String baseUrl = StringUtils.stripEnd(webDavConfig.getPublicUrl(), "/");
        return baseUrl + "/" + encodeToken(relativePath);
    }

    private String basicAuthHeader()
    {
        String raw = webDavConfig.getUsername() + ":" + webDavConfig.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeRelativePath(String relativePath)
    {
        String[] parts = normalizeRelativePath(relativePath).split("/");
        StringBuilder encoded = new StringBuilder();
        for (String part : parts)
        {
            if (encoded.length() > 0)
            {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return encoded.toString();
    }

    private String normalizeRelativePath(String relativePath)
    {
        return StringUtils.stripStart(relativePath.replace("\\", "/"), "/");
    }

    private String decodeRelativePath(String fileUrl)
    {
        String baseUrl = StringUtils.stripEnd(webDavConfig.getPublicUrl(), "/") + "/";
        if (!StringUtils.startsWith(fileUrl, baseUrl))
        {
            throw new IllegalArgumentException("Unsupported WebDAV file url");
        }
        return decodeToken(StringUtils.substringAfter(fileUrl, baseUrl));
    }

    private String encodeToken(String relativePath)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(normalizeRelativePath(relativePath).getBytes(StandardCharsets.UTF_8));
    }

    private String decodeToken(String token)
    {
        try
        {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid WebDAV token", e);
        }
    }

    private boolean isSuccess(int statusCode)
    {
        return statusCode >= 200 && statusCode < 300;
    }
}
