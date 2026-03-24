package com.ruoyi.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebDAV 配置信息
 */
@Configuration
@ConfigurationProperties(prefix = "webdav")
public class WebDavConfig
{
    private boolean enabled;
    private String url;
    private String username;
    private String password;
    private String publicUrl;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPublicUrl()
    {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        this.publicUrl = publicUrl;
    }
}
