package com.zoumh.hotelmonitor.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoumh.hotelmonitor.domain.CrawlExecutionResult;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import com.zoumh.hotelmonitor.service.HotelPriceCrawlerService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class HotelPriceCrawlerServiceImpl implements HotelPriceCrawlerService {

    private static final Pattern DEFAULT_PRICE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HotelPriceCrawlerServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();
    }

    @Override
    public CrawlExecutionResult crawl(HotelPriceMonitor monitor) {
        CrawlExecutionResult result = new CrawlExecutionResult();
        result.setCrawledAt(new Date());
        if (!StringUtils.hasText(monitor.getChannelUrl())) {
            result.setErrorMessage("未配置抓取链接");
            return result;
        }
        try {
            Map<String, String> config = parseConfig(monitor.getCrawlConfig());
            String html = webClient.get()
                .uri(monitor.getChannelUrl())
                .accept(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));
            if (!StringUtils.hasText(html)) {
                result.setErrorMessage("抓取结果为空");
                return result;
            }
            Document document = Jsoup.parse(html, monitor.getChannelUrl());
            BigDecimal price = extractPrice(document, html, config);
            if (price == null) {
                result.setErrorMessage("未匹配到价格，请检查抓取配置");
                return result;
            }
            result.setSuccess(true);
            result.setPrice(price);
            result.setCurrency(firstNonBlank(config.get("currency"), monitor.getCurrency(), "CNY"));
            result.setAvailability(extractAvailability(document, config));
            result.setSourceNote(buildSourceNote(config));
            return result;
        } catch (Exception ex) {
            result.setErrorMessage(ex.getMessage());
            return result;
        }
    }

    private Map<String, String> parseConfig(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of("priceRegex", raw);
        }
    }

    private BigDecimal extractPrice(Document document, String html, Map<String, String> config) {
        String selector = config.get("priceSelector");
        String attr = config.get("priceAttr");
        String raw = null;
        if (StringUtils.hasText(selector)) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                raw = StringUtils.hasText(attr) ? element.attr(attr) : element.text();
            }
        }
        if (!StringUtils.hasText(raw)) {
            raw = document.select("meta[property=product:price:amount],meta[itemprop=price],meta[name=price]").stream()
                .map(element -> element.attr("content"))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        }
        if (!StringUtils.hasText(raw)) {
            raw = html;
        }
        Pattern pattern = compilePattern(config.get("priceRegex"));
        Matcher matcher = pattern.matcher(raw.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }

    private String extractAvailability(Document document, Map<String, String> config) {
        String selector = config.get("availabilitySelector");
        if (!StringUtils.hasText(selector)) {
            return "tracking";
        }
        Element element = document.selectFirst(selector);
        if (element == null) {
            return "tracking";
        }
        String text = element.text().toLowerCase(Locale.ROOT);
        if (text.contains("售罄") || text.contains("无房") || text.contains("sold out")) {
            return "closed";
        }
        return "tracking";
    }

    private Pattern compilePattern(String configured) {
        if (!StringUtils.hasText(configured)) {
            return DEFAULT_PRICE_PATTERN;
        }
        return Pattern.compile(configured);
    }

    private String buildSourceNote(Map<String, String> config) {
        String source = config.get("source");
        String selector = config.get("priceSelector");
        if (StringUtils.hasText(source)) {
            return "爬虫采集: " + source;
        }
        if (StringUtils.hasText(selector)) {
            return "爬虫采集: selector=" + selector;
        }
        return "爬虫采集: 自动识别";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
