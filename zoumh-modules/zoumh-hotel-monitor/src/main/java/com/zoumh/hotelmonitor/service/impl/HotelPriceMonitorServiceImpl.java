package com.zoumh.hotelmonitor.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zoumh.hotelmonitor.domain.CrawlExecutionResult;
import com.zoumh.hotelmonitor.domain.HotelPriceHistory;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import com.zoumh.hotelmonitor.mapper.HotelPriceMonitorMapper;
import com.zoumh.hotelmonitor.service.HotelPriceCrawlerService;
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
    private final HotelPriceCrawlerService hotelPriceCrawlerService;

    public HotelPriceMonitorServiceImpl(
        HotelPriceMonitorMapper hotelPriceMonitorMapper,
        HotelPriceCrawlerService hotelPriceCrawlerService
    ) {
        this.hotelPriceMonitorMapper = hotelPriceMonitorMapper;
        this.hotelPriceCrawlerService = hotelPriceCrawlerService;
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
        appendHistory(monitor, "由监控任务自动写入");
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
        appendHistory(monitor, "由监控任务自动写入");
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

    @Override
    public List<HotelPriceMonitor> selectCrawlEnabledMonitors() {
        return hotelPriceMonitorMapper.selectCrawlEnabledMonitors();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrawlExecutionResult executeCrawler(Long monitorId, String operator) {
        HotelPriceMonitor monitor = hotelPriceMonitorMapper.selectHotelPriceMonitorById(monitorId);
        CrawlExecutionResult result = new CrawlExecutionResult();
        if (monitor == null) {
            result.setErrorMessage("监控任务不存在");
            return result;
        }
        result = hotelPriceCrawlerService.crawl(monitor);
        monitor.setUpdateBy(operator);
        monitor.setUpdateTime(DateUtils.getNowDate());
        monitor.setLastCrawledAt(result.getCrawledAt());
        if (!result.isSuccess()) {
            monitor.setLastErrorMessage(result.getErrorMessage());
            hotelPriceMonitorMapper.updateHotelPriceMonitor(monitor);
            return result;
        }
        BigDecimal before = monitor.getCurrentPrice();
        monitor.setCurrentPrice(result.getPrice());
        monitor.setCurrency(result.getCurrency());
        monitor.setStatus(resolveStatus(monitor, result));
        monitor.setLastCheckedTime(result.getCrawledAt());
        monitor.setLastErrorMessage("");
        monitor.setLatestChange(calculateChange(before, result.getPrice()));
        hotelPriceMonitorMapper.updateHotelPriceMonitor(monitor);
        appendHistory(monitor, result.getSourceNote());
        return result;
    }

    private void normalizeMonitor(HotelPriceMonitor monitor) {
        if (monitor.getCurrency() == null || monitor.getCurrency().isBlank()) {
            monitor.setCurrency("CNY");
        }
        if (monitor.getCrawlEnabled() == null || monitor.getCrawlEnabled().isBlank()) {
            monitor.setCrawlEnabled("Y");
        }
        if (monitor.getCrawlStrategy() == null || monitor.getCrawlStrategy().isBlank()) {
            monitor.setCrawlStrategy("html");
        }
        if (monitor.getStatus() == null || monitor.getStatus().isBlank()) {
            monitor.setStatus("tracking");
        }
        if (monitor.getNotifyEnabled() == null || monitor.getNotifyEnabled().isBlank()) {
            monitor.setNotifyEnabled("Y");
        }
    }

    private void appendHistory(HotelPriceMonitor monitor, String sourceNote) {
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
        history.setSourceNote(sourceNote);
        history.setCreateBy(monitor.getUpdateBy() != null ? monitor.getUpdateBy() : monitor.getCreateBy());
        history.setCreateTime(DateUtils.getNowDate());
        hotelPriceMonitorMapper.insertHotelPriceHistory(history);
    }

    private String resolveStatus(HotelPriceMonitor monitor, CrawlExecutionResult result) {
        if ("closed".equalsIgnoreCase(result.getAvailability())) {
            return "closed";
        }
        if (monitor.getTargetPrice() != null && result.getPrice() != null && result.getPrice().compareTo(monitor.getTargetPrice()) <= 0) {
            return "alerted";
        }
        if ("paused".equalsIgnoreCase(monitor.getStatus())) {
            return "paused";
        }
        return "tracking";
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
