package com.ruoyi.system.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.social.SocialLikeRequest;
import com.ruoyi.system.domain.social.SocialUserProfile;
import com.ruoyi.system.service.social.ISocialAppService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social/app")
public class SocialAppController extends BaseController
{
    private final ISocialAppService socialAppService;

    public SocialAppController(ISocialAppService socialAppService)
    {
        this.socialAppService = socialAppService;
    }

    @GetMapping("/me")
    public AjaxResult currentProfile()
    {
        return success(socialAppService.getCurrentUserProfile(SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }

    @PutMapping("/me")
    public AjaxResult updateCurrentProfile(@Valid @RequestBody SocialUserProfile profile)
    {
        return toAjax(socialAppService.updateCurrentUserProfile(SecurityUtils.getUserId(), SecurityUtils.getUsername(), profile));
    }

    @PostMapping("/like")
    public AjaxResult like(@Valid @RequestBody SocialLikeRequest request)
    {
        return success(socialAppService.likeUser(SecurityUtils.getUserId(), SecurityUtils.getUsername(), request.getTargetUserId()));
    }

    @GetMapping("/likes")
    public AjaxResult myLikes()
    {
        return success(socialAppService.getCurrentUserLikedProfiles(SecurityUtils.getUserId()));
    }

    @GetMapping("/friends")
    public AjaxResult myFriends()
    {
        return success(socialAppService.getCurrentUserFriends(SecurityUtils.getUserId()));
    }

    @GetMapping("/state")
    public AjaxResult myState()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("likedUserIds", socialAppService.getCurrentUserLikedTargetIds(SecurityUtils.getUserId()));
        data.put("friends", socialAppService.getCurrentUserFriends(SecurityUtils.getUserId()));
        return success(data);
    }
}
