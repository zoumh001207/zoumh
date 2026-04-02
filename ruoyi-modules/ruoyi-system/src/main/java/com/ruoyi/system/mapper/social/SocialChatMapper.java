package com.ruoyi.system.mapper.social;

import com.ruoyi.system.domain.social.chat.SocialChatConversation;
import com.ruoyi.system.domain.social.chat.SocialChatConversationVO;
import com.ruoyi.system.domain.social.chat.SocialChatMessage;
import com.ruoyi.system.domain.social.chat.SocialChatMessageVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SocialChatMapper
{
    Integer countActiveMatchBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    SocialChatConversation selectConversationBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    int insertConversation(SocialChatConversation conversation);

    int updateConversationAfterSend(@Param("conversationId") Long conversationId, @Param("lastSenderId") Long lastSenderId,
        @Param("lastMessage") String lastMessage, @Param("receiverUserId") Long receiverUserId);

    List<SocialChatConversationVO> selectConversationListByUserId(Long userId);

    List<SocialChatMessageVO> selectMessagesByConversationId(@Param("conversationId") Long conversationId);

    int insertMessage(SocialChatMessage message);

    int markMessagesRead(@Param("conversationId") Long conversationId, @Param("readerUserId") Long readerUserId);

    int clearUnreadCount(@Param("conversationId") Long conversationId, @Param("readerUserId") Long readerUserId);
}
