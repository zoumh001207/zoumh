package com.ruoyi.system.service.social.impl;

import com.ruoyi.system.domain.social.SocialAdminOverview;
import com.ruoyi.system.domain.social.SocialChannelCard;
import com.ruoyi.system.domain.social.SocialFeatureCard;
import com.ruoyi.system.domain.social.SocialLandingOverview;
import com.ruoyi.system.domain.social.SocialMetricCard;
import com.ruoyi.system.domain.social.SocialModuleCard;
import com.ruoyi.system.domain.social.SocialPublicProfileCard;
import com.ruoyi.system.domain.social.SocialRoadmapItem;
import com.ruoyi.system.domain.social.SocialUserProfile;
import com.ruoyi.system.service.social.ISocialUserProfileService;
import com.ruoyi.system.service.social.ISocialProductService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SocialProductServiceImpl implements ISocialProductService
{
    private final ISocialUserProfileService socialUserProfileService;

    public SocialProductServiceImpl(ISocialUserProfileService socialUserProfileService)
    {
        this.socialUserProfileService = socialUserProfileService;
    }

    @Override
    public SocialLandingOverview getLandingOverview()
    {
        return new SocialLandingOverview(
            "Zoumh Social",
            "为认真交友的人，建立一个更安全、更真实、更可运营的连接场景。",
            "先聚焦城市年轻单身用户，通过小程序拉新、安卓端沉淀、后台运营提效。",
            List.of(
                new SocialChannelCard("后台管理端", "运营中台", "已纳入本次改造", "负责审核、推荐、活动、风控与数据管理"),
                new SocialChannelCard("微信小程序", "拉新入口", "一期规划中", "适合快速注册、资料完善、活动报名与分享裂变"),
                new SocialChannelCard("安卓 App", "主使用端", "一期规划中", "承载推荐、匹配、聊天、陪伴与会员权益")
            ),
            List.of(
                new SocialFeatureCard("真实资料卡", "支持头像、标签、自我介绍、交友目标与审核状态。", "Profile"),
                new SocialFeatureCard("双向匹配", "先喜欢后匹配，降低骚扰感，提升关系质量。", "Match"),
                new SocialFeatureCard("运营推荐位", "平台可以按城市、活动、主题做推荐流运营。", "Operate"),
                new SocialFeatureCard("举报风控", "举报、封禁、敏感审核会成为后台核心能力。", "Safety")
            ),
            List.of(
                new SocialRoadmapItem("第一阶段", "完成项目换壳、社交首页、后台驾驶舱、基础接口骨架", "进行中"),
                new SocialRoadmapItem("第二阶段", "完善资料、推荐流、喜欢/跳过、双向匹配", "待启动"),
                new SocialRoadmapItem("第三阶段", "补聊天、活动、举报审核、用户分层运营", "待启动")
            )
        );
    }

    @Override
    public SocialAdminOverview getAdminOverview()
    {
        return new SocialAdminOverview(
            "Zoumh Social",
            "MVP 骨架阶段",
            List.of(
                new SocialMetricCard("当前定位", "社交交友", "项目已从通用工具台切向社交产品"),
                new SocialMetricCard("管理端", "已接管", "现有若依后台作为社交运营中台继续使用"),
                new SocialMetricCard("小程序", "待建设", "作为拉新、分享、轻互动入口"),
                new SocialMetricCard("安卓 App", "待建设", "作为主业务承载端，后续重点投入")
            ),
            List.of(
                new SocialModuleCard("用户资料中心", "后端 + 审核后台", "优先级 P0", "管理昵称、头像、标签、实名、资料审核"),
                new SocialModuleCard("推荐与匹配", "推荐策略", "优先级 P0", "支持推荐流、喜欢/跳过、双向匹配"),
                new SocialModuleCard("举报与风控", "客服运营", "优先级 P1", "处理举报、封禁、敏感行为与异常账号"),
                new SocialModuleCard("活动与增长", "市场运营", "优先级 P1", "做 Banner、话题、城市活动和拉新配置")
            ),
            List.of(
                new SocialRoadmapItem("骨架搭建", "完成产品定位、接口、首页和运营台改造", "进行中"),
                new SocialRoadmapItem("业务建模", "落资料、推荐、匹配、举报表结构", "待启动"),
                new SocialRoadmapItem("多端落地", "小程序与安卓端接入同一套社交 API", "待启动")
            ),
            List.of(
                "先把用户资料和推荐流做成可联调接口",
                "补齐后台菜单、权限点与 SQL 初始化",
                "后续新建小程序仓和安卓仓，继续接 Jenkins 发版"
            )
        );
    }

    @Override
    public List<SocialPublicProfileCard> getPublicProfiles()
    {
        SocialUserProfile query = new SocialUserProfile();
        query.setProfileStatus("A");
        List<SocialUserProfile> profiles = socialUserProfileService.selectSocialUserProfileList(query);
        return profiles.stream()
            .limit(6)
            .map(item -> new SocialPublicProfileCard(
                item.getProfileId(),
                item.getNickname(),
                item.getGender(),
                item.getCityCode(),
                item.getProfession(),
                item.getRelationshipGoal(),
                item.getAvatarUrl(),
                item.getBio()
            ))
            .toList();
    }
}
