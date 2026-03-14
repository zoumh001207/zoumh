package com.zoumh.hotelmonitor.mapper;

import com.zoumh.hotelmonitor.domain.HotelPriceHistory;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HotelPriceMonitorMapper {

    List<HotelPriceMonitor> selectHotelPriceMonitorList(HotelPriceMonitor monitor);

    HotelPriceMonitor selectHotelPriceMonitorById(@Param("monitorId") Long monitorId);

    int insertHotelPriceMonitor(HotelPriceMonitor monitor);

    int updateHotelPriceMonitor(HotelPriceMonitor monitor);

    int deleteHotelPriceMonitorByIds(@Param("monitorIds") Long[] monitorIds);

    int countAllMonitors();

    int countByStatus(@Param("status") String status);

    int countTargetReached();

    List<HotelPriceHistory> selectHotelPriceHistoryList(@Param("monitorId") Long monitorId);

    HotelPriceHistory selectLatestHotelPriceHistory(@Param("monitorId") Long monitorId);

    int insertHotelPriceHistory(HotelPriceHistory history);

    List<HotelPriceMonitor> selectCrawlEnabledMonitors();
}
