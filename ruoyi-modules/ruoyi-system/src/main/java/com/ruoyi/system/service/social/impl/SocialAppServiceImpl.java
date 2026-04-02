package com.ruoyi.system.service.social.impl;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.social.SocialLikeResult;
import com.ruoyi.system.domain.social.SocialUserProfile;
import com.ruoyi.system.mapper.social.SocialConnectionMapper;
import com.ruoyi.system.mapper.social.SocialUserProfileMapper;
import com.ruoyi.system.service.social.ISocialAppService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialAppServiceImpl implements ISocialAppService
{
    private static final String PROFILE_STATUS_APPROVED = "A";

    private final SocialUserProfileMapper socialUserProfileMapper;
    private final SocialConnectionMapper socialConnectionMapper;

    public SocialAppServiceImpl(SocialUserProfileMapper socialUserProfileMapper, SocialConnectionMapper socialConnectionMapper)
    {
        this.socialUserProfileMapper = socialUserProfileMapper;
        this.socialConnectionMapper = socialConnectionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialUserProfile getCurrentUserProfile(Long userId, String username)
    {
        ensureProfileExists(userId, username, null);
        return socialUserProfileMapper.selectSocialUserProfileByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCurrentUserProfile(Long userId, String username, SocialUserProfile profile)
    {
        SocialUserProfile current = getCurrentUserProfile(userId, username);
        profile.setProfileId(current.getProfileId());
        profile.setUserId(userId);
        profile.setUpdateBy(username);
        if (StringUtils.isBlank(profile.getProfileStatus()))
        {
            profile.setProfileStatus(current.getProfileStatus());
        }
        if (StringUtils.isBlank(profile.getRealVerified()))
        {
            profile.setRealVerified(current.getRealVerified());
        }
        if (profile.getAlbumCount() == null)
        {
            profile.setAlbumCount(current.getAlbumCount());
        }
        return socialUserProfileMapper.updateSocialUserProfile(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialLikeResult likeUser(Long userId, String username, Long targetUserId)
    {
        if (userId.equals(targetUserId))
        {
            throw new ServiceException("不能给自己加好友");
        }

        getCurrentUserProfile(userId, username);
        SocialUserProfile target = socialUserProfileMapper.selectSocialUserProfileByUserId(targetUserId);
        if (target == null || !PROFILE_STATUS_APPROVED.equals(target.getProfileStatus()))
        {
            throw new ServiceException("目标用户资料不存在或暂未上架");
        }

        Integer reverseLikeCount = socialConnectionMapper.countReverseLike(targetUserId, userId);
        boolean matched = reverseLikeCount != null && reverseLikeCount > 0;

        socialConnectionMapper.insertLikeRecord(userId, targetUserId, "LIKE", matched ? "Y" : "N");

        if (matched)
        {
            long userA = Math.min(userId, targetUserId);
            long userB = Math.max(userId, targetUserId);
            socialConnectionMapper.markMatchedBetweenUsers(userId, targetUserId);
            if (socialConnectionMapper.countMatchRecord(userA, userB) == 0)
            {
                socialConnectionMapper.insertMatchRecord(userA, userB);
            }
        }

        SocialLikeResult result = new SocialLikeResult();
        result.setTargetUserId(targetUserId);
        result.setTargetNickname(target.getNickname());
        result.setMatched(matched);
        result.setMessage(matched ? "你们已经互相关注，已加入好友列表" : "已发送喜欢，等待对方回应");
        return result;
    }

    @Override
    public List<SocialUserProfile> getCurrentUserLikedProfiles(Long userId)
    {
        return socialConnectionMapper.selectLikedProfilesByUserId(userId);
    }

    @Override
    public List<SocialUserProfile> getCurrentUserFriends(Long userId)
    {
        return socialConnectionMapper.selectMatchedProfilesByUserId(userId);
    }

    @Override
    public List<Long> getCurrentUserLikedTargetIds(Long userId)
    {
        List<Long> likedIds = socialConnectionMapper.selectLikedTargetUserIdsByUserId(userId);
        return likedIds == null ? Collections.emptyList() : likedIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSocialProfileForUser(Long userId, String username, String nickname)
    {
        ensureProfileExists(userId, username, nickname);
    }

    private void ensureProfileExists(Long userId, String username, String nickname)
    {
        SocialUserProfile current = socialUserProfileMapper.selectSocialUserProfileByUserId(userId);
        if (current != null)
        {
            return;
        }
        SocialUserProfile profile = new SocialUserProfile();
        profile.setUserId(userId);
        profile.setNickname(StringUtils.isNotBlank(nickname) ? nickname : username);
        profile.setGender("U");
        profile.setCityCode("");
        profile.setProfession("");
        profile.setRelationshipGoal("MAKE_FRIENDS");
        profile.setAvatarUrl("");
        profile.setBio("你好呀，很高兴认识你。");
        profile.setAlbumCount(0);
        profile.setRealVerified("N");
        profile.setProfileStatus(PROFILE_STATUS_APPROVED);
        profile.setRemark("App注册自动初始化");
        profile.setCreateBy(username);
        profile.setUpdateBy(username);
        socialUserProfileMapper.insertSocialUserProfile(profile);
    }
}
