package com.zoumh.hotelmonitor.service.impl;

import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import com.zoumh.hotelmonitor.mapper.HotelCollectionSnapshotMapper;
import com.zoumh.hotelmonitor.service.IHotelCollectionSnapshotService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelCollectionSnapshotServiceImpl implements IHotelCollectionSnapshotService {

    private final HotelCollectionSnapshotMapper hotelCollectionSnapshotMapper;

    public HotelCollectionSnapshotServiceImpl(HotelCollectionSnapshotMapper hotelCollectionSnapshotMapper) {
        this.hotelCollectionSnapshotMapper = hotelCollectionSnapshotMapper;
    }

    @Override
    public List<HotelCollectionSnapshot> selectHotelCollectionSnapshotList(Long taskId) {
        return hotelCollectionSnapshotMapper.selectHotelCollectionSnapshotList(taskId);
    }

    @Override
    public List<HotelRoomSnapshot> selectHotelRoomSnapshotList(Long snapshotId) {
        return hotelCollectionSnapshotMapper.selectHotelRoomSnapshotList(snapshotId);
    }
}
