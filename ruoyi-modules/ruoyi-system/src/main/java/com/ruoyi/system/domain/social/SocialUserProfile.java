package com.ruoyi.system.domain.social;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.xss.Xss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SocialUserProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long profileId;

    private Long userId;

    private String nickname;

    private String gender;

    private String cityCode;

    private String profession;

    private String relationshipGoal;

    private String avatarUrl;

    private String bio;

    private Integer albumCount;

    private String realVerified;

    private String profileStatus;

    public Long getProfileId()
    {
        return profileId;
    }

    public void setProfileId(Long profileId)
    {
        this.profileId = profileId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    @Xss(message = "昵称不能包含脚本字符")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getCityCode()
    {
        return cityCode;
    }

    public void setCityCode(String cityCode)
    {
        this.cityCode = cityCode;
    }

    @Size(max = 64, message = "职业长度不能超过64个字符")
    public String getProfession()
    {
        return profession;
    }

    public void setProfession(String profession)
    {
        this.profession = profession;
    }

    public String getRelationshipGoal()
    {
        return relationshipGoal;
    }

    public void setRelationshipGoal(String relationshipGoal)
    {
        this.relationshipGoal = relationshipGoal;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    @Size(max = 500, message = "个人介绍长度不能超过500个字符")
    public String getBio()
    {
        return bio;
    }

    public void setBio(String bio)
    {
        this.bio = bio;
    }

    public Integer getAlbumCount()
    {
        return albumCount;
    }

    public void setAlbumCount(Integer albumCount)
    {
        this.albumCount = albumCount;
    }

    public String getRealVerified()
    {
        return realVerified;
    }

    public void setRealVerified(String realVerified)
    {
        this.realVerified = realVerified;
    }

    public String getProfileStatus()
    {
        return profileStatus;
    }

    public void setProfileStatus(String profileStatus)
    {
        this.profileStatus = profileStatus;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("profileId", getProfileId())
            .append("userId", getUserId())
            .append("nickname", getNickname())
            .append("gender", getGender())
            .append("cityCode", getCityCode())
            .append("profession", getProfession())
            .append("relationshipGoal", getRelationshipGoal())
            .append("avatarUrl", getAvatarUrl())
            .append("bio", getBio())
            .append("albumCount", getAlbumCount())
            .append("realVerified", getRealVerified())
            .append("profileStatus", getProfileStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
