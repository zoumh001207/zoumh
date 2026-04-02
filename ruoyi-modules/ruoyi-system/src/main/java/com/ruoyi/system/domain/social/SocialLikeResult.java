package com.ruoyi.system.domain.social;

public class SocialLikeResult
{
    private Long targetUserId;

    private String targetNickname;

    private Boolean matched;

    private String message;

    public Long getTargetUserId()
    {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId)
    {
        this.targetUserId = targetUserId;
    }

    public String getTargetNickname()
    {
        return targetNickname;
    }

    public void setTargetNickname(String targetNickname)
    {
        this.targetNickname = targetNickname;
    }

    public Boolean getMatched()
    {
        return matched;
    }

    public void setMatched(Boolean matched)
    {
        this.matched = matched;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
