package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.CtripCityHotelResult;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.domain.HotelLocationOption;
import java.util.Date;
import java.util.List;

public interface ICtripCityCollectionService {

    CtripCityHotelResult collect(HotelCollectionTask task);

    List<HotelLocationOption> listLocationOptions(String cityCode, Date checkInDate, Date checkOutDate);
}
