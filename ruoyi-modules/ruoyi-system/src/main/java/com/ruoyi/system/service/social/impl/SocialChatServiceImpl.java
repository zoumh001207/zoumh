package com.ruoyi.system.service.social.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.social.chat.SocialChatConversation;
import com.ruoyi.system.domain.social.chat.SocialChatConversationVO;
import com.ruoyi.system.domain.social.chat.SocialChatMessage;
import com.ruoyi.system.domain.social.chat.SocialChatMessageVO;
import com.ruoyi.system.mapper.social.SocialChatMapper;
import com.ruoyi.system.service.social.ISocialChatService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialChatServiceImpl implements ISocialChatService
{
    private final SocialChatMapper socialChatMapper;

    public SocialChatServiceImpl(SocialChatMapper socialChatMapper)
    {
        this.socialChatMapper = socialChatMapper;
    }

    @Override
    public List<SocialChatConversationVO> getConversationList(Long userId)
    {
        List<SocialChatConversationVO> list = socialChatMapper.selectConversationListByUserId(userId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SocialChatMessageVO> getConversationMessages(Long userId, Long targetUserId)
    {
        if (userId.equals(targetUserId))
        {
            throw new ServiceException("不能查看和自己的聊天记录");
        }
        Long userA = Math.min(userId, targetUserId);
        Long userB = Math.max(userId, targetUserId);
        SocialChatConversation conversation = socialChatMapper.selectConversationBetweenUsers(userA, userB);
        if (conversation == null)
        {
            return Collections.emptyList();
        }
        socialChatMapper.markMessagesRead(conversation.getConversationId(), userId);
        socialChatMapper.clearUnreadCount(conversation.getConversationId(), userId);
        return socialChatMapper.selectMessagesByConversationId(conversation.getConversationId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialChatMessageVO sendMessage(Long userId, Long targetUserId, String content)
    {
        if (userId.equals(targetUserId))
        {
            throw new ServiceException("不能给自己发消息");
        }
        if (StringUtils.isBlank(content))
        {
            throw new ServiceException("消息内容不能为空");
        }

        Long userA = Math.min(userId, targetUserId);
        Long userB = Math.max(userId, targetUserId);
        SocialChatConversation conversation = socialChatMapper.selectConversationBetweenUsers(userA, userB);
        if (conversation == null)
        {
            conversation = new SocialChatConversation();
            conversation.setUserA(userA);
            conversation.setUserB(userB);
            conversation.setLastSenderId(userId);
            conversation.setLastMessage(content);
            socialChatMapper.insertConversation(conversation);
        }
        socialChatMapper.updateConversationAfterSend(conversation.getConversationId(), userId, content, targetUserId);

        SocialChatMessage message = new SocialChatMessage();
        message.setConversationId(conversation.getConversationId());
        message.setFromUserId(userId);
        message.setToUserId(targetUserId);
        message.setMessageType("TEXT");
        message.setContent(content);
        message.setReadStatus("N");
        socialChatMapper.insertMessage(message);

        SocialChatMessageVO result = new SocialChatMessageVO();
        result.setMessageId(message.getMessageId());
        result.setConversationId(conversation.getConversationId());
        result.setFromUserId(userId);
        result.setToUserId(targetUserId);
        result.setMessageType("TEXT");
        result.setContent(content);
        result.setReadStatus("N");
        result.setCreatedAt(message.getCreatedAt());
        return result;
    }
}
