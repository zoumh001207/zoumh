package com.ruoyi.system.domain.social;

import jakarta.validation.constraints.NotNull;

public class SocialLikeRequest
{
    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;

    public Long getTargetUserId()
    {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId)
    {
        this.targetUserId = targetUserId;
    }
}
