package com.zoumh.hotelmonitor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotelCollectionSnapshot extends BaseEntity {

    private Long snapshotId;
    private Long taskId;
    private String platform;
    private String cityName;
    private String platformHotelId;
    private String hotelName;
    private String hotelType;
    private String hotelUrl;
    private String mainImage;
    private String starLabel;
    private String commentScore;
    private String reviewCount;
    private BigDecimal minPrice;
    private String currency;
    private String locationText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date crawledAt;

    private String rawJson;
}
