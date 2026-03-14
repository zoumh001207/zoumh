package com.zoumh.hotelmonitor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotelPriceMonitor extends BaseEntity {

    private Long monitorId;

    @NotBlank(message = "酒店名称不能为空")
    @Size(max = 128, message = "酒店名称长度不能超过128个字符")
    private String hotelName;

    @NotBlank(message = "监控平台不能为空")
    @Size(max = 64, message = "监控平台长度不能超过64个字符")
    private String platform;

    @Size(max = 64, message = "城市长度不能超过64个字符")
    private String city;

    @Size(max = 128, message = "房型长度不能超过128个字符")
    private String roomType;

    @NotNull(message = "入住日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date checkInDate;

    @NotNull(message = "离店日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date checkOutDate;

    @NotNull(message = "目标价格不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "目标价格必须大于0")
    private BigDecimal targetPrice;

    @DecimalMin(value = "0.00", inclusive = false, message = "当前价格必须大于0")
    private BigDecimal currentPrice;

    @Size(max = 16, message = "币种长度不能超过16个字符")
    private String currency;

    @Size(max = 512, message = "链接长度不能超过512个字符")
    private String channelUrl;

    private String crawlEnabled;

    @Size(max = 32, message = "抓取策略长度不能超过32个字符")
    private String crawlStrategy;

    @Size(max = 2000, message = "抓取配置长度不能超过2000个字符")
    private String crawlConfig;

    @Size(max = 16, message = "状态长度不能超过16个字符")
    private String status;

    private BigDecimal latestChange;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCrawledAt;

    private String lastErrorMessage;

    private String notifyEnabled;
}
