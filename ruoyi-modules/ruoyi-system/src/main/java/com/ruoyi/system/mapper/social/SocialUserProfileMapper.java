package com.ruoyi.system.mapper.social;

import com.ruoyi.system.domain.social.SocialUserProfile;
import java.util.List;

public interface SocialUserProfileMapper
{
    SocialUserProfile selectSocialUserProfileById(Long profileId);

    List<SocialUserProfile> selectSocialUserProfileList(SocialUserProfile profile);

    int insertSocialUserProfile(SocialUserProfile profile);

    int updateSocialUserProfile(SocialUserProfile profile);

    int deleteSocialUserProfileById(Long profileId);

    int deleteSocialUserProfileByIds(Long[] profileIds);
}
