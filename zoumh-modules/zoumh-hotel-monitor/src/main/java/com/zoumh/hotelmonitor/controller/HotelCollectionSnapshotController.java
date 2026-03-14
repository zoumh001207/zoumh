package com.zoumh.hotelmonitor.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zoumh.hotelmonitor.service.IHotelCollectionSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collect/snapshot")
public class HotelCollectionSnapshotController extends BaseController {

    private final IHotelCollectionSnapshotService hotelCollectionSnapshotService;

    public HotelCollectionSnapshotController(IHotelCollectionSnapshotService hotelCollectionSnapshotService) {
        this.hotelCollectionSnapshotService = hotelCollectionSnapshotService;
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam("taskId") Long taskId) {
        return success(hotelCollectionSnapshotService.selectHotelCollectionSnapshotList(taskId));
    }

    @RequiresPermissions("hotel:monitor:query")
    @GetMapping("/{snapshotId}/rooms")
    public AjaxResult rooms(@PathVariable("snapshotId") Long snapshotId) {
        return success(hotelCollectionSnapshotService.selectHotelRoomSnapshotList(snapshotId));
    }
}
