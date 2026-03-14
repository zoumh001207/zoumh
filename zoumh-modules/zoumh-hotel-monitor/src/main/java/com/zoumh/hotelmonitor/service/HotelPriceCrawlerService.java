package com.zoumh.hotelmonitor.service;

import com.zoumh.hotelmonitor.domain.CrawlExecutionResult;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;

public interface HotelPriceCrawlerService {

    CrawlExecutionResult crawl(HotelPriceMonitor monitor);
}
