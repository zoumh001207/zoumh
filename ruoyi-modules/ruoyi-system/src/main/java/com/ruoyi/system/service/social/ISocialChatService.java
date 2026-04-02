package com.ruoyi.system.service.social;

import com.ruoyi.system.domain.social.chat.SocialChatConversationVO;
import com.ruoyi.system.domain.social.chat.SocialChatMessageVO;
import java.util.List;

public interface ISocialChatService
{
    List<SocialChatConversationVO> getConversationList(Long userId);

    List<SocialChatMessageVO> getConversationMessages(Long userId, Long targetUserId);

    SocialChatMessageVO sendMessage(Long userId, Long targetUserId, String content);
}
