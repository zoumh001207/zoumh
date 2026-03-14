package com.zoumh.hotelmonitor.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zoumh.hotelmonitor.domain.HotelPriceHistory;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import com.zoumh.hotelmonitor.mapper.HotelPriceMonitorMapper;
import com.zoumh.hotelmonitor.service.IHotelPriceMonitorService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelPriceMonitorServiceImpl implements IHotelPriceMonitorService {

    private final HotelPriceMonitorMapper hotelPriceMonitorMapper;

    public HotelPriceMonitorServiceImpl(HotelPriceMonitorMapper hotelPriceMonitorMapper) {
        this.hotelPriceMonitorMapper = hotelPriceMonitorMapper;
    }

    @Override
    public List<HotelPriceMonitor> selectHotelPriceMonitorList(HotelPriceMonitor monitor) {
        return hotelPriceMonitorMapper.selectHotelPriceMonitorList(monitor);
    }

    @Override
    public HotelPriceMonitor selectHotelPriceMonitorById(Long monitorId) {
        return hotelPriceMonitorMapper.selectHotelPriceMonitorById(monitorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertHotelPriceMonitor(HotelPriceMonitor monitor) {
        normalizeMonitor(monitor);
        monitor.setCreateTime(DateUtils.getNowDate());
        int rows = hotelPriceMonitorMapper.insertHotelPriceMonitor(monitor);
        appendHistoryIfNeeded(monitor);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateHotelPriceMonitor(HotelPriceMonitor monitor) {
        normalizeMonitor(monitor);
        HotelPriceMonitor original = hotelPriceMonitorMapper.selectHotelPriceMonitorById(monitor.getMonitorId());
        if (original == null) {
            return 0;
        }
        if (monitor.getLastCheckedTime() == null && monitor.getCurrentPrice() != null
            && hasPriceChanged(original.getCurrentPrice(), monitor.getCurrentPrice())) {
            monitor.setLastCheckedTime(DateUtils.getNowDate());
        }
        if (monitor.getCurrentPrice() != null && hasPriceChanged(original.getCurrentPrice(), monitor.getCurrentPrice())) {
            monitor.setLatestChange(calculateChange(original.getCurrentPrice(), monitor.getCurrentPrice()));
        } else if (monitor.getLatestChange() == null) {
            monitor.setLatestChange(original.getLatestChange());
        }
        monitor.setUpdateTime(DateUtils.getNowDate());
        int rows = hotelPriceMonitorMapper.updateHotelPriceMonitor(monitor);
        appendHistoryIfNeeded(monitor);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHotelPriceMonitorByIds(Long[] monitorIds) {
        return hotelPriceMonitorMapper.deleteHotelPriceMonitorByIds(monitorIds);
    }

    @Override
    public List<HotelPriceHistory> selectHotelPriceHistoryList(Long monitorId) {
        return hotelPriceMonitorMapper.selectHotelPriceHistoryList(monitorId);
    }

    @Override
    public Map<String, Object> selectOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", hotelPriceMonitorMapper.countAllMonitors());
        result.put("tracking", hotelPriceMonitorMapper.countByStatus("tracking"));
        result.put("alerted", hotelPriceMonitorMapper.countByStatus("alerted"));
        result.put("paused", hotelPriceMonitorMapper.countByStatus("paused"));
        result.put("targetReached", hotelPriceMonitorMapper.countTargetReached());
        return result;
    }

    private void normalizeMonitor(HotelPriceMonitor monitor) {
        if (monitor.getCurrency() == null || monitor.getCurrency().isBlank()) {
            monitor.setCurrency("CNY");
        }
        if (monitor.getStatus() == null || monitor.getStatus().isBlank()) {
            monitor.setStatus("tracking");
        }
        if (monitor.getNotifyEnabled() == null || monitor.getNotifyEnabled().isBlank()) {
            monitor.setNotifyEnabled("Y");
        }
    }

    private void appendHistoryIfNeeded(HotelPriceMonitor monitor) {
        if (monitor.getMonitorId() == null || monitor.getCurrentPrice() == null) {
            return;
        }
        HotelPriceHistory latest = hotelPriceMonitorMapper.selectLatestHotelPriceHistory(monitor.getMonitorId());
        if (latest != null && !hasPriceChanged(latest.getObservedPrice(), monitor.getCurrentPrice())) {
            return;
        }
        HotelPriceHistory history = new HotelPriceHistory();
        history.setMonitorId(monitor.getMonitorId());
        history.setObservedPrice(monitor.getCurrentPrice());
        history.setAvailability(monitor.getStatus());
        history.setObservedAt(monitor.getLastCheckedTime() != null ? monitor.getLastCheckedTime() : DateUtils.getNowDate());
        history.setChangeAmount(calculateChange(latest == null ? null : latest.getObservedPrice(), monitor.getCurrentPrice()));
        history.setSourceNote("由监控任务自动写入");
        history.setCreateBy(monitor.getUpdateBy() != null ? monitor.getUpdateBy() : monitor.getCreateBy());
        history.setCreateTime(DateUtils.getNowDate());
        hotelPriceMonitorMapper.insertHotelPriceHistory(history);
    }

    private boolean hasPriceChanged(BigDecimal before, BigDecimal after) {
        if (before == null) {
            return after != null;
        }
        if (after == null) {
            return true;
        }
        return before.compareTo(after) != 0;
    }

    private BigDecimal calculateChange(BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return BigDecimal.ZERO;
        }
        return after.subtract(before);
    }
}
