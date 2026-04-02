package com.ruoyi.system.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.service.social.ISocialProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social")
public class SocialProductController extends BaseController
{
    private final ISocialProductService socialProductService;

    public SocialProductController(ISocialProductService socialProductService)
    {
        this.socialProductService = socialProductService;
    }

    @GetMapping("/public/landing")
    public AjaxResult landing()
    {
        return success(socialProductService.getLandingOverview());
    }

    @GetMapping("/admin/overview")
    public AjaxResult adminOverview()
    {
        return success(socialProductService.getAdminOverview());
    }

    @GetMapping("/public/profiles")
    public AjaxResult publicProfiles()
    {
        return success(socialProductService.getPublicProfiles());
    }
}
