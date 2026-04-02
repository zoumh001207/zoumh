package com.ruoyi.system.domain.social.chat;

import java.util.Date;

public class SocialChatConversation
{
    private Long conversationId;

    private Long userA;

    private Long userB;

    private Long lastSenderId;

    private String lastMessage;

    private Date lastMessageTime;

    private Integer userAUnread;

    private Integer userBUnread;

    private Date createdAt;

    private Date updatedAt;

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getUserA()
    {
        return userA;
    }

    public void setUserA(Long userA)
    {
        this.userA = userA;
    }

    public Long getUserB()
    {
        return userB;
    }

    public void setUserB(Long userB)
    {
        this.userB = userB;
    }

    public Long getLastSenderId()
    {
        return lastSenderId;
    }

    public void setLastSenderId(Long lastSenderId)
    {
        this.lastSenderId = lastSenderId;
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage)
    {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTime()
    {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime)
    {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUserAUnread()
    {
        return userAUnread;
    }

    public void setUserAUnread(Integer userAUnread)
    {
        this.userAUnread = userAUnread;
    }

    public Integer getUserBUnread()
    {
        return userBUnread;
    }

    public void setUserBUnread(Integer userBUnread)
    {
        this.userBUnread = userBUnread;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
