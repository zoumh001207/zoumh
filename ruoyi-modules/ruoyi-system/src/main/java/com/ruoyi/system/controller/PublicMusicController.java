package com.ruoyi.system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页公开音乐接口
 */
@RestController
@RequestMapping("/music")
public class PublicMusicController extends BaseController
{
    private static final String ITUNES_HOST = "itunes.apple.com";
    private static final String ITUNES_AUDIO_HOST = "audio-ssl.itunes.apple.com";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/search")
    public AjaxResult search(@RequestParam("keyword") String keyword) throws IOException, InterruptedException
    {
        String cleanKeyword = StringUtils.trim(keyword);
        if (StringUtils.isEmpty(cleanKeyword))
        {
            return AjaxResult.success(new ArrayList<>());
        }

        String url = "https://" + ITUNES_HOST + "/search?term="
            + URLEncoder.encode(cleanKeyword, StandardCharsets.UTF_8)
            + "&entity=song&country=cn&limit=12";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
        {
            return error("音乐搜索源暂时不可用");
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<MusicTrack> tracks = new ArrayList<>();
        for (JsonNode item : root.path("results"))
        {
            String previewUrl = item.path("previewUrl").asText();
            if (StringUtils.isEmpty(previewUrl))
            {
                continue;
            }
            tracks.add(new MusicTrack(
                item.path("trackId").asLong(),
                item.path("trackName").asText(),
                item.path("artistName").asText(),
                item.path("collectionName").asText(),
                item.path("artworkUrl100").asText(),
                previewUrl,
                item.path("trackViewUrl").asText(),
                "Apple Music"
            ));
        }
        return AjaxResult.success(tracks);
    }

    @GetMapping("/audio")
    public void audio(@RequestParam("url") String url,
                      @RequestParam(value = "name", required = false) String name,
                      @RequestParam(value = "download", defaultValue = "false") boolean download,
                      HttpServletResponse response) throws IOException, InterruptedException
    {
        URI uri = URI.create(url);
        if (!ITUNES_AUDIO_HOST.equalsIgnoreCase(uri.getHost()))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法音频地址");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        HttpResponse<InputStream> remote = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (remote.statusCode() != 200)
        {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "音频源访问失败");
            return;
        }

        String contentType = remote.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        if (download)
        {
            String fileName = sanitizeFileName(name);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName + ".m4a", StandardCharsets.UTF_8));
        }

        try (InputStream inputStream = remote.body())
        {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    private String sanitizeFileName(String name)
    {
        String fallback = StringUtils.isNotEmpty(name) ? name : "music-preview";
        return fallback.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public record MusicTrack(Long id, String title, String artist, String album, String artwork,
                             String previewUrl, String detailUrl, String source)
    {
    }
}
