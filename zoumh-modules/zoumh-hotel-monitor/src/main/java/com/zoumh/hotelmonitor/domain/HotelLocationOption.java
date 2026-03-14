package com.zoumh.hotelmonitor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelLocationOption {

    private String label;
    private String value;
    private String type;
}
