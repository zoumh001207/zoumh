package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import java.util.List;

public interface IHotelCollectionSnapshotService {

    List<HotelCollectionSnapshot> selectHotelCollectionSnapshotList(Long taskId);

    List<HotelRoomSnapshot> selectHotelRoomSnapshotList(Long snapshotId);
}
