package com.ruoyi.system.service.social;

import com.ruoyi.system.domain.social.SocialAdminOverview;
import com.ruoyi.system.domain.social.SocialLandingOverview;
import com.ruoyi.system.domain.social.SocialPublicProfileCard;
import java.util.List;

public interface ISocialProductService
{
    SocialLandingOverview getLandingOverview();

    SocialAdminOverview getAdminOverview();

    List<SocialPublicProfileCard> getPublicProfiles();
}
