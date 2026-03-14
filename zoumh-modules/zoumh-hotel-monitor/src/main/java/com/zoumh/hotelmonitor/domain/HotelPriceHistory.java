package com.zoumh.hotelmonitor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotelPriceHistory extends BaseEntity {

    private Long historyId;

    private Long monitorId;

    private BigDecimal observedPrice;

    private BigDecimal changeAmount;

    private String availability;

    private String sourceNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date observedAt;
}
