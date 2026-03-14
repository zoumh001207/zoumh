package com.zoumh.hotelmonitor.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.zoumh.hotelmonitor.domain.CrawlExecutionResult;
import com.zoumh.hotelmonitor.domain.HotelPriceMonitor;
import com.zoumh.hotelmonitor.service.IHotelPriceMonitorService;
import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
public class HotelPriceMonitorController extends BaseController {

    private final IHotelPriceMonitorService hotelPriceMonitorService;

    public HotelPriceMonitorController(IHotelPriceMonitorService hotelPriceMonitorService) {
        this.hotelPriceMonitorService = hotelPriceMonitorService;
    }

    @RequiresPermissions("hotel:monitor:list")
    @GetMapping("/list")
    public TableDataInfo list(HotelPriceMonitor monitor) {
        startPage();
        List<HotelPriceMonitor> list = hotelPriceMonitorService.selectHotelPriceMonitorList(monitor);
        return getDataTable(list);
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/{monitorId}")
    public AjaxResult getInfo(@PathVariable("monitorId") Long monitorId) {
        return success(hotelPriceMonitorService.selectHotelPriceMonitorById(monitorId));
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/{monitorId}/history")
    public AjaxResult history(@PathVariable("monitorId") Long monitorId) {
        return success(hotelPriceMonitorService.selectHotelPriceHistoryList(monitorId));
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return success(hotelPriceMonitorService.selectOverview());
    }

    @RequiresPermissions("hotel:monitor:query")
    @PostMapping("/{monitorId}/crawl")
    public AjaxResult crawl(@PathVariable("monitorId") Long monitorId) {
        CrawlExecutionResult result = hotelPriceMonitorService.executeCrawler(monitorId, SecurityUtils.getUsername());
        if (!result.isSuccess()) {
            return AjaxResult.error(result.getErrorMessage());
        }
        return success(result);
    }

    @RequiresPermissions("hotel:monitor:add")
    @Log(title = "酒店价格监控", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody HotelPriceMonitor monitor) {
        monitor.setCreateBy(SecurityUtils.getUsername());
        if (monitor.getLastCheckedTime() == null && monitor.getCurrentPrice() != null) {
            monitor.setLastCheckedTime(new Date());
        }
        return toAjax(hotelPriceMonitorService.insertHotelPriceMonitor(monitor));
    }

    @RequiresPermissions("hotel:monitor:edit")
    @Log(title = "酒店价格监控", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody HotelPriceMonitor monitor) {
        monitor.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(hotelPriceMonitorService.updateHotelPriceMonitor(monitor));
    }

    @RequiresPermissions("hotel:monitor:remove")
    @Log(title = "酒店价格监控", businessType = BusinessType.DELETE)
    @DeleteMapping("/{monitorIds}")
    public AjaxResult remove(@PathVariable("monitorIds") Long[] monitorIds) {
        return toAjax(hotelPriceMonitorService.deleteHotelPriceMonitorByIds(monitorIds));
    }
}
