package com.ruoyi.system.domain.social.chat;

import java.util.Date;

public class SocialChatConversationVO
{
    private Long conversationId;

    private Long peerUserId;

    private String peerNickname;

    private String peerAvatarUrl;

    private String peerCityCode;

    private String peerProfession;

    private String lastMessage;

    private Long lastSenderId;

    private Date lastMessageTime;

    private Integer unreadCount;

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getPeerUserId()
    {
        return peerUserId;
    }

    public void setPeerUserId(Long peerUserId)
    {
        this.peerUserId = peerUserId;
    }

    public String getPeerNickname()
    {
        return peerNickname;
    }

    public void setPeerNickname(String peerNickname)
    {
        this.peerNickname = peerNickname;
    }

    public String getPeerAvatarUrl()
    {
        return peerAvatarUrl;
    }

    public void setPeerAvatarUrl(String peerAvatarUrl)
    {
        this.peerAvatarUrl = peerAvatarUrl;
    }

    public String getPeerCityCode()
    {
        return peerCityCode;
    }

    public void setPeerCityCode(String peerCityCode)
    {
        this.peerCityCode = peerCityCode;
    }

    public String getPeerProfession()
    {
        return peerProfession;
    }

    public void setPeerProfession(String peerProfession)
    {
        this.peerProfession = peerProfession;
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage)
    {
        this.lastMessage = lastMessage;
    }

    public Long getLastSenderId()
    {
        return lastSenderId;
    }

    public void setLastSenderId(Long lastSenderId)
    {
        this.lastSenderId = lastSenderId;
    }

    public Date getLastMessageTime()
    {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime)
    {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUnreadCount()
    {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount)
    {
        this.unreadCount = unreadCount;
    }
}
