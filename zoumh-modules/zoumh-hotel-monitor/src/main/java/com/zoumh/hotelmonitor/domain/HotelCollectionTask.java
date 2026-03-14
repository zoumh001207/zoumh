package com.zoumh.hotelmonitor.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotelCollectionTask extends BaseEntity {

    private Long taskId;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过128个字符")
    private String taskName;

    @NotBlank(message = "平台不能为空")
    @Size(max = 32, message = "平台长度不能超过32个字符")
    private String platform;

    @NotBlank(message = "城市名称不能为空")
    @Size(max = 64, message = "城市名称长度不能超过64个字符")
    private String cityName;

    @NotBlank(message = "城市编码不能为空")
    @Size(max = 32, message = "城市编码长度不能超过32个字符")
    private String cityCode;

    @Size(max = 128, message = "位置关键词长度不能超过128个字符")
    private String locationKeyword;

    @NotNull(message = "入住日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date checkInDate;

    @NotNull(message = "离店日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date checkOutDate;

    @Size(max = 16, message = "任务状态长度不能超过16个字符")
    private String status;

    private Integer platformHotelCount;

    private Integer capturedHotelCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCrawledAt;

    private String lastErrorMessage;
}
