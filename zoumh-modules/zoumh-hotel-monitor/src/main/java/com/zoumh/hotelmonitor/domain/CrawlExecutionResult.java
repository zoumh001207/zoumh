package com.zoumh.hotelmonitor.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class CrawlExecutionResult {

    private boolean success;

    private BigDecimal price;

    private String currency;

    private String availability;

    private String sourceNote;

    private String errorMessage;

    private Date crawledAt;
}
