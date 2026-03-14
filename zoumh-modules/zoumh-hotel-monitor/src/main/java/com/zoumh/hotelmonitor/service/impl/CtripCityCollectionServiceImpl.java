package com.zoumh.hotelmonitor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.zoumh.hotelmonitor.domain.CtripCityHotelResult;
import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.domain.HotelLocationOption;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import com.zoumh.hotelmonitor.service.ICtripCityCollectionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CtripCityCollectionServiceImpl implements ICtripCityCollectionService {

    private static final Pattern HOTEL_TOTAL_PATTERN = Pattern.compile("\"hotelTotalCount\":(\\d+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(CNY|RMB|¥)\\s*(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
    private static final Set<String> LOCATION_TYPES = Set.of(
        "COUNTY",
        "DISTRICT",
        "COMMERCIAL",
        "LANDMARK",
        "SUB_LANDMARK",
        "AIRPORT_AND_STATION",
        "SUB_CITY",
        "CITY"
    );

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public CtripCityCollectionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-HK,zh-TW;q=0.9,zh;q=0.8,en;q=0.7")
            .build();
    }

    @Override
    public CtripCityHotelResult collect(HotelCollectionTask task) {
        String html = fetchTripHtml(task);
        Document document = Jsoup.parse(html);
        CtripCityHotelResult result = new CtripCityHotelResult();
        Date now = DateUtils.getNowDate();
        List<Element> cards = document.select(".hotel-card");
        result.setPlatformHotelCount(extractHotelCount(html, cards.size()));
        for (Element card : cards) {
            HotelRoomSnapshot room = buildRoomSnapshot(card);
            HotelCollectionSnapshot snapshot = buildHotelSnapshot(task, card, room, now);
            result.getHotels().add(snapshot);
            if (room != null) {
                result.getRooms().add(room);
            }
        }
        return result;
    }

    @Override
    public List<HotelLocationOption> listLocationOptions(String cityCode, Date checkInDate, Date checkOutDate) {
        String html = fetchCtripHtml(cityCode, checkInDate, checkOutDate);
        Document document = Jsoup.parse(html);
        Element script = document.selectFirst("script#webcore_internal");
        if (script == null) {
            return List.of();
        }
        Map<String, HotelLocationOption> options = new LinkedHashMap<>();
        try {
            JsonNode root = objectMapper.readTree(script.html());
            collectLocationOptions(root, options);
        } catch (Exception ex) {
            throw new IllegalStateException("解析位置筛选项失败", ex);
        }
        return new ArrayList<>(options.values());
    }

    private HotelCollectionSnapshot buildHotelSnapshot(HotelCollectionTask task, Element card, HotelRoomSnapshot room, Date now) {
        HotelCollectionSnapshot snapshot = new HotelCollectionSnapshot();
        snapshot.setTaskId(task.getTaskId());
        snapshot.setPlatform(task.getPlatform());
        snapshot.setCityName(task.getCityName());
        String hotelId = firstNonBlank(card.id(), attr(card.selectFirst(".right-card"), "data-offline-hotelid"));
        snapshot.setPlatformHotelId(hotelId);
        snapshot.setHotelName(text(card, ".hotelName"));
        snapshot.setHotelType("");
        snapshot.setHotelUrl(buildHotelUrl(hotelId));
        snapshot.setMainImage(attr(card.selectFirst(".m-lazyImg__img"), "src"));
        snapshot.setStarLabel(buildStarLabel(card));
        snapshot.setCommentScore(text(card, ".comment-score .score"));
        snapshot.setReviewCount(text(card, ".comment-num"));
        snapshot.setMinPrice(extractDisplayPrice(room));
        snapshot.setCurrency(room != null && room.getCurrency() != null && !room.getCurrency().isBlank() ? room.getCurrency() : "CNY");
        snapshot.setPreviewRoomName(room == null ? "" : room.getRoomName());
        snapshot.setLocationText(text(card, ".position-desc"));
        snapshot.setCrawledAt(now);
        snapshot.setRawJson(card.outerHtml());
        snapshot.setRemark(room == null ? "0" : "1");
        return snapshot;
    }

    private HotelRoomSnapshot buildRoomSnapshot(Element card) {
        String roomName = text(card, ".room-name");
        String saleText = text(card, ".room-price .sale");
        String originalText = text(card, ".room-price .delete");
        String totalText = text(card, ".room-price .price-explain");
        if (roomName.isBlank() && saleText.isBlank() && originalText.isBlank() && totalText.isBlank()) {
            return null;
        }
        HotelRoomSnapshot room = new HotelRoomSnapshot();
        room.setPlatformRoomId("");
        room.setRoomName(roomName);
        room.setBedInfo(extractBedInfo(card));
        room.setBreakfastInfo(joinTagsByKeywords(card, "早餐", "早"));
        room.setCancelPolicy(joinTagsByKeywords(card, "取消", "退"));
        room.setPayType(joinTagsByKeywords(card, "到店付", "在线付", "预付"));
        room.setRoomQuantity("");
        room.setOriginalPrice(parsePriceValue(originalText));
        room.setSalePrice(parsePriceValue(saleText));
        room.setTotalPrice(parsePriceValue(totalText));
        room.setCurrency(firstNonBlank(parseCurrency(saleText), parseCurrency(originalText), parseCurrency(totalText), "CNY"));
        room.setPriceDescription(totalText);
        room.setRawJson(card.selectFirst(".room-info") == null ? card.outerHtml() : card.selectFirst(".room-info").outerHtml());
        return room;
    }

    private void collectLocationOptions(JsonNode node, Map<String, HotelLocationOption> options) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode data = node.path("data");
            JsonNode operation = node.path("operation");
            String type = extractLocationType(operation.path("selfMutexIds"));
            String title = data.path("title").asText("");
            if (!type.isBlank() && !title.isBlank()) {
                String key = type + ":" + title;
                options.putIfAbsent(key, new HotelLocationOption(title, title, toLocationLabel(type)));
            }
            node.fields().forEachRemaining(entry -> collectLocationOptions(entry.getValue(), options));
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> collectLocationOptions(item, options));
        }
    }

    private String fetchTripHtml(HotelCollectionTask task) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://hk.trip.com/hotels/list")
            .queryParam("city", task.getCityCode())
            .queryParam("checkin", DateUtils.parseDateToStr("yyyy-MM-dd", defaultCheckIn(task.getCheckInDate())))
            .queryParam("checkout", DateUtils.parseDateToStr("yyyy-MM-dd", defaultCheckOut(task.getCheckOutDate(), task.getCheckInDate())))
            .queryParam("curr", "CNY");
        if (task.getLocationKeyword() != null && !task.getLocationKeyword().isBlank()) {
            builder.queryParam("searchWord", task.getLocationKeyword().trim());
        }
        return webClient.get()
            .uri(builder.build(true).toUri())
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(30));
    }

    private String fetchCtripHtml(String cityCode, Date checkInDate, Date checkOutDate) {
        return webClient.get()
            .uri(UriComponentsBuilder.fromHttpUrl("https://hotels.ctrip.com/hotels/list")
                .queryParam("city", cityCode)
                .queryParam("checkin", DateUtils.parseDateToStr("yyyy-MM-dd", defaultCheckIn(checkInDate)))
                .queryParam("checkout", DateUtils.parseDateToStr("yyyy-MM-dd", defaultCheckOut(checkOutDate, checkInDate)))
                .build(true)
                .toUri())
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(30));
    }

    private int extractHotelCount(String html, int fallback) {
        Matcher matcher = HOTEL_TOTAL_PATTERN.matcher(html);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return fallback;
    }

    private BigDecimal extractDisplayPrice(HotelRoomSnapshot room) {
        if (room == null) {
            return null;
        }
        if (room.getSalePrice() != null) {
            return room.getSalePrice();
        }
        if (room.getTotalPrice() != null) {
            return room.getTotalPrice();
        }
        return room.getOriginalPrice();
    }

    private String buildStarLabel(Element card) {
        int stars = card.select(".hotelStar .star-icon").size();
        return stars > 0 ? stars + "星" : "";
    }

    private String extractBedInfo(Element card) {
        Set<String> values = new LinkedHashSet<>();
        for (Element element : card.select(".room-bedInfo [aria-label], .room-bedInfo [title]")) {
            String text = firstNonBlank(element.attr("aria-label"), element.attr("title"), element.text());
            if (!text.isBlank()) {
                values.add(text);
            }
        }
        String directText = card.select(".room-bedInfo").text();
        if (!directText.isBlank()) {
            values.add(directText);
        }
        return String.join(" / ", values);
    }

    private String joinTagsByKeywords(Element card, String... keywords) {
        Set<String> values = new LinkedHashSet<>();
        for (Element element : card.select(".room-advantageTag .hotel-tag-content, .price-tags .hotel-tag-content")) {
            String text = element.text().trim();
            if (text.isBlank()) {
                continue;
            }
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    values.add(text);
                    break;
                }
            }
        }
        return String.join(" / ", values);
    }

    private BigDecimal parsePriceValue(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = PRICE_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(2));
        }
        matcher = NUMBER_PATTERN.matcher(text.replace(",", ""));
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return null;
    }

    private String parseCurrency(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = PRICE_PATTERN.matcher(text);
        if (matcher.find()) {
            String raw = matcher.group(1).toUpperCase();
            return "¥".equals(raw) ? "CNY" : raw;
        }
        return "";
    }

    private String buildHotelUrl(String hotelId) {
        return hotelId == null || hotelId.isBlank() ? "" : "https://hotels.ctrip.com/hotels/detail/?hotelId=" + hotelId;
    }

    private String extractLocationType(JsonNode typeArray) {
        if (!typeArray.isArray()) {
            return "";
        }
        for (JsonNode item : typeArray) {
            String value = item.asText("");
            if (LOCATION_TYPES.contains(value)) {
                return value;
            }
        }
        return "";
    }

    private String toLocationLabel(String type) {
        return switch (type) {
            case "COUNTY" -> "区县";
            case "DISTRICT" -> "商圈/地标";
            case "COMMERCIAL" -> "商圈";
            case "LANDMARK", "SUB_LANDMARK" -> "地标";
            case "AIRPORT_AND_STATION" -> "机场车站";
            case "SUB_CITY" -> "片区";
            case "CITY" -> "城市";
            default -> "位置";
        };
    }

    private Date defaultCheckIn(Date checkInDate) {
        return checkInDate == null ? DateUtils.addDays(DateUtils.getNowDate(), 1) : checkInDate;
    }

    private Date defaultCheckOut(Date checkOutDate, Date checkInDate) {
        if (checkOutDate != null) {
            return checkOutDate;
        }
        return DateUtils.addDays(defaultCheckIn(checkInDate), 1);
    }

    private String text(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? "" : element.text().trim();
    }

    private String attr(Element element, String attrName) {
        return element == null ? "" : element.attr(attrName).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
