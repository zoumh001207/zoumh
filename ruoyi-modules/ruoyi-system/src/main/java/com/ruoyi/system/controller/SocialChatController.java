package com.ruoyi.system.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.domain.social.chat.SocialChatSendRequest;
import com.ruoyi.system.service.social.ISocialChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/social/chat")
public class SocialChatController extends BaseController
{
    private final ISocialChatService socialChatService;

    public SocialChatController(ISocialChatService socialChatService)
    {
        this.socialChatService = socialChatService;
    }

    @GetMapping("/conversations")
    public AjaxResult conversations()
    {
        return success(socialChatService.getConversationList(SecurityUtils.getUserId()));
    }

    @GetMapping("/messages")
    public AjaxResult messages(@RequestParam("targetUserId") Long targetUserId)
    {
        return success(socialChatService.getConversationMessages(SecurityUtils.getUserId(), targetUserId));
    }

    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody SocialChatSendRequest request)
    {
        return success(socialChatService.sendMessage(SecurityUtils.getUserId(), request.getTargetUserId(), request.getContent()));
    }
}
