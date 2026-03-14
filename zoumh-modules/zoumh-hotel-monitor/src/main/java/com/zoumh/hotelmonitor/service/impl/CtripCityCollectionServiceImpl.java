package com.zoumh.hotelmonitor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.zoumh.hotelmonitor.domain.CtripCityHotelResult;
import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import com.zoumh.hotelmonitor.service.ICtripCityCollectionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CtripCityCollectionServiceImpl implements ICtripCityCollectionService {

    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
        "<script>self\\.__next_f\\.push\\(\\[1,\\\"([\\s\\S]*?)\\\"\\]\\)</script>"
    );

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public CtripCityCollectionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9")
            .build();
    }

    @Override
    public CtripCityHotelResult collect(HotelCollectionTask task) {
        String url = String.format(
            "https://hotels.ctrip.com/hotels/list?city=%s&checkin=%s&checkout=%s",
            task.getCityCode(),
            DateUtils.parseDateToStr("yyyy-MM-dd", task.getCheckInDate()),
            DateUtils.parseDateToStr("yyyy-MM-dd", task.getCheckOutDate())
        );
        String html = webClient.get()
            .uri(url)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(30));
        String decoded = decodeNextData(html);
        JsonNode initList = extractInitListData(decoded);
        return buildResult(task, initList);
    }

    private CtripCityHotelResult buildResult(HotelCollectionTask task, JsonNode initList) {
        CtripCityHotelResult result = new CtripCityHotelResult();
        Date now = DateUtils.getNowDate();
        result.setPlatformHotelCount(initList.path("hotelListAddtionInfo").path("hotelTotalCount").asInt(0));
        for (JsonNode hotelNode : initList.path("hotelList")) {
            HotelCollectionSnapshot snapshot = new HotelCollectionSnapshot();
            snapshot.setTaskId(task.getTaskId());
            snapshot.setPlatform(task.getPlatform());
            snapshot.setCityName(task.getCityName());
            snapshot.setPlatformHotelId(text(hotelNode, "hotelInfo.summary.hotelId"));
            snapshot.setHotelName(text(hotelNode, "hotelInfo.nameInfo.name"));
            snapshot.setHotelType(text(hotelNode, "hotelInfo.hotelCategory.categoryName"));
            snapshot.setHotelUrl(buildHotelUrl(snapshot.getPlatformHotelId()));
            snapshot.setMainImage(text(hotelNode, "hotelInfo.hotelImages.multiImgs.0.url"));
            snapshot.setStarLabel(starLabel(hotelNode.path("hotelInfo").path("hotelStar")));
            snapshot.setCommentScore(text(hotelNode, "hotelInfo.commentInfo.commentScore"));
            snapshot.setReviewCount(text(hotelNode, "hotelInfo.commentInfo.commenterNumber"));
            snapshot.setMinPrice(findMinPrice(hotelNode));
            snapshot.setCurrency("CNY");
            snapshot.setLocationText(locationText(hotelNode));
            snapshot.setCrawledAt(now);
            snapshot.setRawJson(hotelNode.toString());
            snapshot.setRemark(String.valueOf(hotelNode.path("roomInfo").size()));
            result.getHotels().add(snapshot);

            for (JsonNode roomNode : hotelNode.path("roomInfo")) {
                HotelRoomSnapshot room = new HotelRoomSnapshot();
                room.setPlatformRoomId(text(roomNode, "summary.roomId"));
                room.setRoomName(text(roomNode, "summary.saleRoomName"));
                room.setBedInfo(joinArray(roomNode.path("bedInfo").path("contentList")));
                room.setBreakfastInfo(extractTags(roomNode, "promotionTags"));
                room.setCancelPolicy(extractTags(roomNode, "advantageTags"));
                room.setPayType(payTypeLabel(roomNode.path("payInfo").path("payType").asInt(-1)));
                room.setRoomQuantity(roomNode.path("summary").path("roomQuantity").asText(""));
                room.setRawJson(roomNode.toString());
                result.getRooms().add(room);
            }
        }
        return result;
    }

    private String decodeNextData(String html) {
        Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String raw = matcher.group(1).replace("\\\"", "\\u0022");
            String quoted = "\"" + raw.replace("\"", "\\\"") + "\"";
            try {
                builder.append(objectMapper.readValue(quoted, String.class).replace("\\u0022", "\""));
            } catch (Exception ex) {
                throw new IllegalStateException("解析携程页面数据失败", ex);
            }
        }
        if (builder.length() == 0) {
            throw new IllegalStateException("未获取到携程酒店列表数据");
        }
        return builder.toString();
    }

    private JsonNode extractInitListData(String decoded) {
        int marker = decoded.indexOf("\"initListData\":");
        if (marker < 0) {
            throw new IllegalStateException("携程首屏数据中未包含酒店列表");
        }
        int start = decoded.indexOf('{', marker);
        int end = findJsonEnd(decoded, start);
        try {
            return objectMapper.readTree(decoded.substring(start, end + 1));
        } catch (Exception ex) {
            throw new IllegalStateException("解析携程酒店列表 JSON 失败", ex);
        }
    }

    private int findJsonEnd(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("未找到携程酒店列表 JSON 结束位置");
    }

    private BigDecimal findMinPrice(JsonNode node) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode child = node.get(name);
            if (name.toLowerCase().contains("price") && child.isValueNode()) {
                String value = child.asText("");
                if (value.matches("\\d+(\\.\\d+)?")) {
                    return new BigDecimal(value);
                }
            }
            if (child != null && child.isContainerNode()) {
                BigDecimal nested = findMinPrice(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String text(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            current = part.matches("\\d+") ? current.path(Integer.parseInt(part)) : current.path(part);
        }
        return current.isMissingNode() || current.isNull() ? "" : current.asText("");
    }

    private String starLabel(JsonNode node) {
        int star = node.path("star").asInt(0);
        return star > 0 ? star + "星" : "";
    }

    private String locationText(JsonNode hotelNode) {
        String[] candidates = {
            text(hotelNode, "hotelInfo.positionInfo.positionName"),
            text(hotelNode, "hotelInfo.positionInfo.location"),
            text(hotelNode, "hotelInfo.commentInfo.oneSentenceComment.0.tagTitle")
        };
        StringBuilder builder = new StringBuilder();
        for (String candidate : candidates) {
            if (!candidate.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append(candidate);
            }
        }
        return builder.toString();
    }

    private String joinArray(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : arrayNode) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(item.asText(""));
        }
        return builder.toString();
    }

    private String extractTags(JsonNode roomNode, String field) {
        JsonNode tags = roomNode.path("roomTags").path(field);
        if (!tags.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : tags) {
            String title = item.path("tagTitle").asText("");
            if (!title.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append(title);
            }
        }
        return builder.toString();
    }

    private String payTypeLabel(int payType) {
        return switch (payType) {
            case 1 -> "在线付";
            case 2 -> "到店付";
            default -> "";
        };
    }

    private String buildHotelUrl(String hotelId) {
        return hotelId == null || hotelId.isBlank() ? "" : "https://hotels.ctrip.com/hotels/detail/?hotelId=" + hotelId;
    }
}
