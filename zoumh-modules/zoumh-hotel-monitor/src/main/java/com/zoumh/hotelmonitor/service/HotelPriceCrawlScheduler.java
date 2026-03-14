package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotelPriceCrawlScheduler {

    private final IHotelPriceMonitorService hotelPriceMonitorService;

    @Value("${hotel-monitor.crawl.scheduler-enabled:true}")
    private boolean schedulerEnabled;

    public HotelPriceCrawlScheduler(IHotelPriceMonitorService hotelPriceMonitorService) {
        this.hotelPriceMonitorService = hotelPriceMonitorService;
    }

    @Scheduled(fixedDelayString = "${hotel-monitor.crawl.fixed-delay-ms:1800000}")
    public void crawlEnabledMonitors() {
        if (!schedulerEnabled) {
            return;
        }
        List<HotelPriceMonitor> monitors = hotelPriceMonitorService.selectCrawlEnabledMonitors();
        for (HotelPriceMonitor monitor : monitors) {
            hotelPriceMonitorService.executeCrawler(monitor.getMonitorId(), "system");
        }
    }
}
