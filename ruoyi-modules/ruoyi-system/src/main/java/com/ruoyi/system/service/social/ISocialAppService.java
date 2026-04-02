package com.ruoyi.system.service.social;

import com.ruoyi.system.domain.social.SocialLikeResult;
import com.ruoyi.system.domain.social.SocialUserProfile;
import java.util.List;

public interface ISocialAppService
{
    SocialUserProfile getCurrentUserProfile(Long userId, String username);

    int updateCurrentUserProfile(Long userId, String username, SocialUserProfile profile);

    SocialLikeResult likeUser(Long userId, String username, Long targetUserId);

    List<SocialUserProfile> getCurrentUserLikedProfiles(Long userId);

    List<SocialUserProfile> getCurrentUserFriends(Long userId);

    List<Long> getCurrentUserLikedTargetIds(Long userId);

    void initSocialProfileForUser(Long userId, String username, String nickname);
}
