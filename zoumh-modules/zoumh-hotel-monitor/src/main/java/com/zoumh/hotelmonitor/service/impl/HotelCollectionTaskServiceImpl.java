package com.zoumh.hotelmonitor.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zoumh.hotelmonitor.domain.CtripCityHotelResult;
import com.zoumh.hotelmonitor.domain.HotelCollectionSnapshot;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.domain.HotelLocationOption;
import com.zoumh.hotelmonitor.domain.HotelRoomSnapshot;
import com.zoumh.hotelmonitor.mapper.HotelCollectionSnapshotMapper;
import com.zoumh.hotelmonitor.mapper.HotelCollectionTaskMapper;
import com.zoumh.hotelmonitor.service.ICtripCityCollectionService;
import com.zoumh.hotelmonitor.service.IHotelCollectionTaskService;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelCollectionTaskServiceImpl implements IHotelCollectionTaskService {

    private final HotelCollectionTaskMapper hotelCollectionTaskMapper;
    private final HotelCollectionSnapshotMapper hotelCollectionSnapshotMapper;
    private final ICtripCityCollectionService ctripCityCollectionService;

    public HotelCollectionTaskServiceImpl(
        HotelCollectionTaskMapper hotelCollectionTaskMapper,
        HotelCollectionSnapshotMapper hotelCollectionSnapshotMapper,
        ICtripCityCollectionService ctripCityCollectionService
    ) {
        this.hotelCollectionTaskMapper = hotelCollectionTaskMapper;
        this.hotelCollectionSnapshotMapper = hotelCollectionSnapshotMapper;
        this.ctripCityCollectionService = ctripCityCollectionService;
    }

    @Override
    public List<HotelCollectionTask> selectHotelCollectionTaskList(HotelCollectionTask task) {
        return hotelCollectionTaskMapper.selectHotelCollectionTaskList(task);
    }

    @Override
    public HotelCollectionTask selectHotelCollectionTaskById(Long taskId) {
        return hotelCollectionTaskMapper.selectHotelCollectionTaskById(taskId);
    }

    @Override
    public int insertHotelCollectionTask(HotelCollectionTask task) {
        normalizeTask(task);
        task.setCreateTime(DateUtils.getNowDate());
        return hotelCollectionTaskMapper.insertHotelCollectionTask(task);
    }

    @Override
    public int updateHotelCollectionTask(HotelCollectionTask task) {
        normalizeTask(task);
        task.setUpdateTime(DateUtils.getNowDate());
        return hotelCollectionTaskMapper.updateHotelCollectionTask(task);
    }

    @Override
    public int deleteHotelCollectionTaskByIds(Long[] taskIds) {
        return hotelCollectionTaskMapper.deleteHotelCollectionTaskByIds(taskIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executeCollectionTask(Long taskId, String operator) {
        HotelCollectionTask task = hotelCollectionTaskMapper.selectHotelCollectionTaskById(taskId);
        if (task == null) {
            return 0;
        }
        task.setStatus("running");
        task.setUpdateBy(operator);
        task.setUpdateTime(DateUtils.getNowDate());
        hotelCollectionTaskMapper.updateHotelCollectionTask(task);
        try {
            CtripCityHotelResult result = ctripCityCollectionService.collect(task);
            hotelCollectionSnapshotMapper.deleteRoomSnapshotsByTaskId(taskId);
            hotelCollectionSnapshotMapper.deleteSnapshotsByTaskId(taskId);
            for (HotelCollectionSnapshot snapshot : result.getHotels()) {
                snapshot.setTaskId(taskId);
                snapshot.setCreateBy(operator);
                snapshot.setCreateTime(DateUtils.getNowDate());
                snapshot.setUpdateBy(operator);
                snapshot.setUpdateTime(DateUtils.getNowDate());
                hotelCollectionSnapshotMapper.insertHotelCollectionSnapshot(snapshot);
            }
            int roomCursor = 0;
            for (HotelCollectionSnapshot snapshot : result.getHotels()) {
                int roomCount = 0;
                try {
                    roomCount = Integer.parseInt(snapshot.getRemark());
                } catch (NumberFormatException ignored) {
                    roomCount = 0;
                }
                for (int i = 0; i < roomCount && roomCursor < result.getRooms().size(); i++, roomCursor++) {
                    HotelRoomSnapshot room = result.getRooms().get(roomCursor);
                    room.setSnapshotId(snapshot.getSnapshotId());
                    room.setCreateBy(operator);
                    room.setCreateTime(DateUtils.getNowDate());
                    room.setUpdateBy(operator);
                    room.setUpdateTime(DateUtils.getNowDate());
                    hotelCollectionSnapshotMapper.insertHotelRoomSnapshot(room);
                }
                snapshot.setRemark(null);
            }
            task.setPlatformHotelCount(result.getPlatformHotelCount());
            task.setCapturedHotelCount(result.getHotels().size());
            task.setStatus("success");
            task.setLastErrorMessage("");
            task.setLastCrawledAt(DateUtils.getNowDate());
            task.setUpdateBy(operator);
            task.setUpdateTime(DateUtils.getNowDate());
            return hotelCollectionTaskMapper.updateHotelCollectionTask(task);
        } catch (Exception ex) {
            task.setStatus("failed");
            task.setLastErrorMessage(truncateErrorMessage(ex));
            task.setLastCrawledAt(DateUtils.getNowDate());
            task.setUpdateBy(operator);
            task.setUpdateTime(DateUtils.getNowDate());
            hotelCollectionTaskMapper.updateHotelCollectionTask(task);
            throw ex;
        }
    }

    @Override
    public Map<String, Object> selectOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tasks", hotelCollectionTaskMapper.countAllTasks());
        result.put("running", hotelCollectionTaskMapper.countByStatus("running"));
        result.put("success", hotelCollectionTaskMapper.countByStatus("success"));
        result.put("failed", hotelCollectionTaskMapper.countByStatus("failed"));
        result.put("hotels", hotelCollectionSnapshotMapper.countAllSnapshots());
        result.put("rooms", hotelCollectionSnapshotMapper.countAllRooms());
        return result;
    }

    @Override
    public List<HotelLocationOption> listLocationOptions(String cityCode, Date checkInDate, Date checkOutDate) {
        if (cityCode == null || cityCode.isBlank()) {
            return List.of();
        }
        return ctripCityCollectionService.listLocationOptions(cityCode, checkInDate, checkOutDate);
    }

    private void normalizeTask(HotelCollectionTask task) {
        if (task.getPlatform() == null || task.getPlatform().isBlank()) {
            task.setPlatform("ctrip");
        }
        if (task.getStatus() == null || task.getStatus().isBlank()) {
            task.setStatus("pending");
        }
        if (task.getLocationKeyword() != null) {
            task.setLocationKeyword(task.getLocationKeyword().trim());
        }
    }

    private String truncateErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
