package com.ruoyi.system.service.social;

import com.ruoyi.system.domain.social.SocialUserProfile;
import java.util.List;

public interface ISocialUserProfileService
{
    SocialUserProfile selectSocialUserProfileById(Long profileId);

    List<SocialUserProfile> selectSocialUserProfileList(SocialUserProfile profile);

    int insertSocialUserProfile(SocialUserProfile profile);

    int updateSocialUserProfile(SocialUserProfile profile);

    int deleteSocialUserProfileById(Long profileId);

    int deleteSocialUserProfileByIds(Long[] profileIds);
}
