package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.domain.HotelLocationOption;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IHotelCollectionTaskService {

    List<HotelCollectionTask> selectHotelCollectionTaskList(HotelCollectionTask task);

    HotelCollectionTask selectHotelCollectionTaskById(Long taskId);

    int insertHotelCollectionTask(HotelCollectionTask task);

    int updateHotelCollectionTask(HotelCollectionTask task);

    int deleteHotelCollectionTaskByIds(Long[] taskIds);

    int executeCollectionTask(Long taskId, String operator);

    Map<String, Object> selectOverview();

    List<HotelLocationOption> listLocationOptions(String cityCode, Date checkInDate, Date checkOutDate);
}
