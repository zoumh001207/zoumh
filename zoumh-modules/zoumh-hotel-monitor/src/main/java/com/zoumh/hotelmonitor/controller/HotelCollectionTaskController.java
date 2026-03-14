package com.zoumh.hotelmonitor.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.zoumh.hotelmonitor.domain.HotelCollectionTask;
import com.zoumh.hotelmonitor.service.IHotelCollectionTaskService;
import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collect/task")
public class HotelCollectionTaskController extends BaseController {

    private final IHotelCollectionTaskService hotelCollectionTaskService;

    public HotelCollectionTaskController(IHotelCollectionTaskService hotelCollectionTaskService) {
        this.hotelCollectionTaskService = hotelCollectionTaskService;
    }

    @RequiresPermissions("hotel:monitor:list")
    @GetMapping("/list")
    public TableDataInfo list(HotelCollectionTask task) {
        startPage();
        List<HotelCollectionTask> list = hotelCollectionTaskService.selectHotelCollectionTaskList(task);
        return getDataTable(list);
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId) {
        return success(hotelCollectionTaskService.selectHotelCollectionTaskById(taskId));
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return success(hotelCollectionTaskService.selectOverview());
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/location-options")
    public AjaxResult locationOptions(
        @RequestParam("cityCode") String cityCode,
        @RequestParam(value = "checkInDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date checkInDate,
        @RequestParam(value = "checkOutDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date checkOutDate
    ) {
        return success(hotelCollectionTaskService.listLocationOptions(cityCode, checkInDate, checkOutDate));
    }

    @RequiresPermissions("hotel:monitor:query")
    @PostMapping("/{taskId}/crawl")
    public AjaxResult crawl(@PathVariable("taskId") Long taskId) {
        hotelCollectionTaskService.executeCollectionTask(taskId, SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    @RequiresPermissions("hotel:monitor:add")
    @Log(title = "酒店城市采集任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody HotelCollectionTask task) {
        task.setCreateBy(SecurityUtils.getUsername());
        return toAjax(hotelCollectionTaskService.insertHotelCollectionTask(task));
    }

    @RequiresPermissions("hotel:monitor:edit")
    @Log(title = "酒店城市采集任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody HotelCollectionTask task) {
        task.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(hotelCollectionTaskService.updateHotelCollectionTask(task));
    }

    @RequiresPermissions("hotel:monitor:remove")
    @Log(title = "酒店城市采集任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable("taskIds") Long[] taskIds) {
        return toAjax(hotelCollectionTaskService.deleteHotelCollectionTaskByIds(taskIds));
    }
}
