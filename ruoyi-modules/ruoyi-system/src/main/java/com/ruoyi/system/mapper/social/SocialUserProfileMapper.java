package com.ruoyi.system.mapper.social;

import com.ruoyi.system.domain.social.SocialUserProfile;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SocialUserProfileMapper
{
    SocialUserProfile selectSocialUserProfileById(Long profileId);

    SocialUserProfile selectSocialUserProfileByUserId(Long userId);

    List<SocialUserProfile> selectSocialUserProfileList(SocialUserProfile profile);

    List<SocialUserProfile> selectPublicProfilesForApp(@Param("currentUserId") Long currentUserId);

    int insertSocialUserProfile(SocialUserProfile profile);

    int updateSocialUserProfile(SocialUserProfile profile);

    int deleteSocialUserProfileById(Long profileId);

    int deleteSocialUserProfileByIds(Long[] profileIds);
}
