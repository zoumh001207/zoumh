package com.zoumh.hotelmonitor.mapper;

import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HotelCollectionSnapshotMapper {

    List<HotelCollectionSnapshot> selectHotelCollectionSnapshotList(@Param("taskId") Long taskId);

    List<HotelRoomSnapshot> selectHotelRoomSnapshotList(@Param("snapshotId") Long snapshotId);

    int deleteSnapshotsByTaskId(@Param("taskId") Long taskId);

    int insertHotelCollectionSnapshot(HotelCollectionSnapshot snapshot);

    int insertHotelRoomSnapshot(HotelRoomSnapshot snapshot);

    int countAllSnapshots();

    int countAllRooms();
}
