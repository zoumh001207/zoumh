package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.CtripCityHotelResult;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;

public interface ICtripCityCollectionService {

    CtripCityHotelResult collect(HotelCollectionTask task);
}
