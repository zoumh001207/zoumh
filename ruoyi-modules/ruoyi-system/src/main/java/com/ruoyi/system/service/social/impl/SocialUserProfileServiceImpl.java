package com.ruoyi.system.service.social.impl;

import com.ruoyi.system.domain.social.SocialUserProfile;
import com.ruoyi.system.mapper.social.SocialUserProfileMapper;
import com.ruoyi.system.service.social.ISocialUserProfileService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SocialUserProfileServiceImpl implements ISocialUserProfileService
{
    private final SocialUserProfileMapper socialUserProfileMapper;

    public SocialUserProfileServiceImpl(SocialUserProfileMapper socialUserProfileMapper)
    {
        this.socialUserProfileMapper = socialUserProfileMapper;
    }

    @Override
    public SocialUserProfile selectSocialUserProfileById(Long profileId)
    {
        return socialUserProfileMapper.selectSocialUserProfileById(profileId);
    }

    @Override
    public List<SocialUserProfile> selectSocialUserProfileList(SocialUserProfile profile)
    {
        return socialUserProfileMapper.selectSocialUserProfileList(profile);
    }

    @Override
    public int insertSocialUserProfile(SocialUserProfile profile)
    {
        return socialUserProfileMapper.insertSocialUserProfile(profile);
    }

    @Override
    public int updateSocialUserProfile(SocialUserProfile profile)
    {
        return socialUserProfileMapper.updateSocialUserProfile(profile);
    }

    @Override
    public int deleteSocialUserProfileById(Long profileId)
    {
        return socialUserProfileMapper.deleteSocialUserProfileById(profileId);
    }

    @Override
    public int deleteSocialUserProfileByIds(Long[] profileIds)
    {
        return socialUserProfileMapper.deleteSocialUserProfileByIds(profileIds);
    }
}
