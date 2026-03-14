package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.CrawlExecutionResult;
import com.zoumh.hotelmonitor.domain.HotelPriceHistory;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import java.util.List;
import java.util.Map;

public interface IHotelPriceMonitorService {

    List<HotelPriceMonitor> selectHotelPriceMonitorList(HotelPriceMonitor monitor);

    HotelPriceMonitor selectHotelPriceMonitorById(Long monitorId);

    int insertHotelPriceMonitor(HotelPriceMonitor monitor);

    int updateHotelPriceMonitor(HotelPriceMonitor monitor);

    int deleteHotelPriceMonitorByIds(Long[] monitorIds);

    List<HotelPriceHistory> selectHotelPriceHistoryList(Long monitorId);

    Map<String, Object> selectOverview();

    List<HotelPriceMonitor> selectCrawlEnabledMonitors();

    CrawlExecutionResult executeCrawler(Long monitorId, String operator);
}
