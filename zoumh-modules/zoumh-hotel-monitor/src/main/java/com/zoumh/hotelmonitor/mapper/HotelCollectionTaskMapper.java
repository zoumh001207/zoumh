package com.zoumh.hotelmonitor.mapper;

import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HotelCollectionTaskMapper {

    List<HotelCollectionTask> selectHotelCollectionTaskList(HotelCollectionTask task);

    HotelCollectionTask selectHotelCollectionTaskById(@Param("taskId") Long taskId);

    int insertHotelCollectionTask(HotelCollectionTask task);

    int updateHotelCollectionTask(HotelCollectionTask task);

    int deleteHotelCollectionTaskByIds(@Param("taskIds") Long[] taskIds);

    int countAllTasks();

    int countByStatus(@Param("status") String status);
}
