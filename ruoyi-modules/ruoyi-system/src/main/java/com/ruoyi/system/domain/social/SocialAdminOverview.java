package com.ruoyi.system.domain.social;

import java.util.List;

public record SocialAdminOverview(
    String productName,
    String phase,
    List<SocialMetricCard> metrics,
    List<SocialModuleCard> modules,
    List<SocialRoadmapItem> roadmap,
    List<String> todoItems
)
{
}
