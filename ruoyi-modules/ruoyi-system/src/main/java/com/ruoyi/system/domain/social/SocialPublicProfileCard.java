package com.ruoyi.system.domain.social;

public record SocialPublicProfileCard(
    Long profileId,
    Long userId,
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
