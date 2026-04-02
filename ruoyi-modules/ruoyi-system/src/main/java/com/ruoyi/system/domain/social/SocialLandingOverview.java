package com.ruoyi.system.domain.social;

import java.util.List;

public record SocialLandingOverview(
    String productName,
    String tagline,
    String audience,
    List<SocialChannelCard> channels,
    List<SocialFeatureCard> features,
    List<SocialRoadmapItem> roadmap
)
{
}
