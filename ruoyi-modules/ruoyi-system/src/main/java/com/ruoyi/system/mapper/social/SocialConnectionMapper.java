package com.ruoyi.system.mapper.social;

import com.ruoyi.system.domain.social.SocialUserProfile;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SocialConnectionMapper
{
    int insertLikeRecord(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId,
        @Param("actionType") String actionType, @Param("matched") String matched);

    Integer countReverseLike(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    int markMatchedBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    Integer countMatchRecord(@Param("userA") Long userA, @Param("userB") Long userB);

    int insertMatchRecord(@Param("userA") Long userA, @Param("userB") Long userB);

    List<SocialUserProfile> selectLikedProfilesByUserId(Long userId);

    List<SocialUserProfile> selectMatchedProfilesByUserId(Long userId);

    List<Long> selectLikedTargetUserIdsByUserId(Long userId);
}
