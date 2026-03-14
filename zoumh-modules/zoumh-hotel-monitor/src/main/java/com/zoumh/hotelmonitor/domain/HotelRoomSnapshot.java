package com.zoumh.hotelmonitor.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotelRoomSnapshot extends BaseEntity {

    private Long roomSnapshotId;
    private Long snapshotId;
    private String platformRoomId;
    private String roomName;
    private String bedInfo;
    private String breakfastInfo;
    private String cancelPolicy;
    private String payType;
    private String roomQuantity;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal totalPrice;
    private String currency;
    private String priceDescription;
    private String rawJson;
}
