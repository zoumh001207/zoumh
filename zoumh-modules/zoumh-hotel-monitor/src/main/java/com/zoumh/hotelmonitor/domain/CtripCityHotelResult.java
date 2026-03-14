package com.zoumh.hotelmonitor.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CtripCityHotelResult {

    private Integer platformHotelCount;

    private final List<HotelCollectionSnapshot> hotels = new ArrayList<>();

    private final List<HotelRoomSnapshot> rooms = new ArrayList<>();
}
