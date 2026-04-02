package com.ruoyi.system.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.social.SocialUserProfile;
import com.ruoyi.system.service.social.ISocialUserProfileService;
import jakarta.validation.Valid;
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
@RequestMapping("/social/profile")
public class SocialUserProfileController extends BaseController
{
    private final ISocialUserProfileService socialUserProfileService;

    public SocialUserProfileController(ISocialUserProfileService socialUserProfileService)
    {
        this.socialUserProfileService = socialUserProfileService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SocialUserProfile profile)
    {
        startPage();
        List<SocialUserProfile> list = socialUserProfileService.selectSocialUserProfileList(profile);
        return getDataTable(list);
    }

    @GetMapping("/{profileId}")
    public AjaxResult getInfo(@PathVariable Long profileId)
    {
        return success(socialUserProfileService.selectSocialUserProfileById(profileId));
    }

    @Log(title = "社交资料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody SocialUserProfile profile)
    {
        profile.setCreateBy(SecurityUtils.getUsername());
        return toAjax(socialUserProfileService.insertSocialUserProfile(profile));
    }

    @Log(title = "社交资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody SocialUserProfile profile)
    {
        profile.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(socialUserProfileService.updateSocialUserProfile(profile));
    }

    @Log(title = "社交资料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{profileIds}")
    public AjaxResult remove(@PathVariable Long[] profileIds)
    {
        return toAjax(socialUserProfileService.deleteSocialUserProfileByIds(profileIds));
    }
}
