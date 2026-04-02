package com.ruoyi.system.domain.social;

public record SocialPublicProfileCard(
    Long profileId,
    String nickname,
    String gender,
    String cityCode,
    String profession,
    String relationshipGoal,
    String avatarUrl,
    String bio
)
{
}
