-- 社交交友项目一期 SQL 草案
-- 当前文件先作为一期数据模型设计稿，不在本次自动执行。

CREATE TABLE IF NOT EXISTS social_user_profile (
  profile_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '资料ID',
  user_id BIGINT NOT NULL COMMENT '关联系统用户ID',
  nickname VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  gender CHAR(1) NOT NULL DEFAULT 'U' COMMENT '性别 M/F/U',
  birthday DATE DEFAULT NULL COMMENT '生日',
  height_cm INT DEFAULT NULL COMMENT '身高',
  city_code VARCHAR(32) DEFAULT '' COMMENT '城市编码',
  profession VARCHAR(64) DEFAULT '' COMMENT '职业',
  bio VARCHAR(500) DEFAULT '' COMMENT '个人介绍',
  relationship_goal VARCHAR(32) DEFAULT 'SERIOUS' COMMENT '交友目标',
  avatar_url VARCHAR(255) DEFAULT '' COMMENT '头像',
  album_count INT NOT NULL DEFAULT 0 COMMENT '相册数',
  real_verified CHAR(1) NOT NULL DEFAULT 'N' COMMENT '实名状态',
  profile_status CHAR(1) NOT NULL DEFAULT 'D' COMMENT '资料状态 D草稿 A通过 R驳回',
  created_by VARCHAR(64) DEFAULT '' COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by VARCHAR(64) DEFAULT '' COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_social_user_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交用户资料';

SET @social_profile_remark_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'social_user_profile'
    AND COLUMN_NAME = 'remark'
);
SET @social_profile_remark_sql := IF(
  @social_profile_remark_exists = 0,
  'ALTER TABLE social_user_profile ADD COLUMN remark VARCHAR(500) DEFAULT '''' COMMENT ''备注''',
  'SELECT 1'
);
PREPARE social_profile_remark_stmt FROM @social_profile_remark_sql;
EXECUTE social_profile_remark_stmt;
DEALLOCATE PREPARE social_profile_remark_stmt;

CREATE TABLE IF NOT EXISTS social_user_media (
  media_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '媒体ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  media_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT '媒体类型',
  media_url VARCHAR(255) NOT NULL COMMENT '资源地址',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  audit_status CHAR(1) NOT NULL DEFAULT 'P' COMMENT '审核状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (media_id),
  KEY idx_social_user_media_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交用户媒体';

CREATE TABLE IF NOT EXISTS social_like_record (
  like_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '喜欢记录ID',
  from_user_id BIGINT NOT NULL COMMENT '发起用户',
  to_user_id BIGINT NOT NULL COMMENT '目标用户',
  action_type VARCHAR(16) NOT NULL DEFAULT 'LIKE' COMMENT 'LIKE/PASS',
  matched CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否已匹配',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (like_id),
  KEY idx_social_like_from (from_user_id),
  KEY idx_social_like_to (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='喜欢与跳过记录';

CREATE TABLE IF NOT EXISTS social_match_record (
  match_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '匹配ID',
  user_a BIGINT NOT NULL COMMENT '用户A',
  user_b BIGINT NOT NULL COMMENT '用户B',
  match_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '匹配状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '匹配时间',
  PRIMARY KEY (match_id),
  KEY idx_social_match_user_a (user_a),
  KEY idx_social_match_user_b (user_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双向匹配记录';

CREATE TABLE IF NOT EXISTS social_chat_conversation (
  conversation_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  user_a BIGINT NOT NULL COMMENT '用户A',
  user_b BIGINT NOT NULL COMMENT '用户B',
  last_sender_id BIGINT DEFAULT NULL COMMENT '最后发送人',
  last_message VARCHAR(1000) DEFAULT '' COMMENT '最后一条消息',
  last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
  user_a_unread INT NOT NULL DEFAULT 0 COMMENT 'A未读数',
  user_b_unread INT NOT NULL DEFAULT 0 COMMENT 'B未读数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (conversation_id),
  UNIQUE KEY uk_social_chat_pair (user_a, user_b),
  KEY idx_social_chat_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交聊天会话';

CREATE TABLE IF NOT EXISTS social_chat_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  conversation_id BIGINT NOT NULL COMMENT '会话ID',
  from_user_id BIGINT NOT NULL COMMENT '发送者',
  to_user_id BIGINT NOT NULL COMMENT '接收者',
  message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型',
  content VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '消息内容',
  read_status CHAR(1) NOT NULL DEFAULT 'N' COMMENT '已读状态',
  read_at DATETIME DEFAULT NULL COMMENT '已读时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (message_id),
  KEY idx_social_chat_message_conv (conversation_id),
  KEY idx_social_chat_message_to (to_user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交聊天消息';

CREATE TABLE IF NOT EXISTS social_report_record (
  report_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  reporter_user_id BIGINT NOT NULL COMMENT '举报人',
  target_user_id BIGINT NOT NULL COMMENT '被举报人',
  report_reason VARCHAR(128) NOT NULL DEFAULT '' COMMENT '举报原因',
  detail_text VARCHAR(500) DEFAULT '' COMMENT '举报说明',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (report_id),
  KEY idx_social_report_target (target_user_id),
  KEY idx_social_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报记录';

CREATE TABLE IF NOT EXISTS social_banner (
  banner_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '运营位ID',
  title VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题',
  subtitle VARCHAR(255) DEFAULT '' COMMENT '副标题',
  image_url VARCHAR(255) DEFAULT '' COMMENT '图片',
  target_url VARCHAR(255) DEFAULT '' COMMENT '跳转链接',
  channel_scope VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '投放端',
  status CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '状态',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (banner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交运营位';

INSERT INTO social_user_profile (
  user_id, nickname, gender, city_code, profession, relationship_goal, avatar_url, bio,
  album_count, real_verified, profile_status, remark, created_by, created_at, updated_by, updated_at
)
SELECT 10001, '木子', 'F', 'shanghai', '品牌策划', 'SERIOUS', '',
       '喜欢城市散步、咖啡店和认真聊天，希望遇到情绪稳定的人。', 3, 'Y', 'A', '首页演示数据', 'sql_init', NOW(), 'sql_init', NOW()
WHERE NOT EXISTS (SELECT 1 FROM social_user_profile WHERE user_id = 10001);

INSERT INTO social_user_profile (
  user_id, nickname, gender, city_code, profession, relationship_goal, avatar_url, bio,
  album_count, real_verified, profile_status, remark, created_by, created_at, updated_by, updated_at
)
SELECT 10002, '阿泽', 'M', 'hangzhou', 'Java工程师', 'MAKE_FRIENDS', '',
       '工作在杭州，周末喜欢骑行和看展，想先从自然认识开始。', 2, 'Y', 'A', '首页演示数据', 'sql_init', NOW(), 'sql_init', NOW()
WHERE NOT EXISTS (SELECT 1 FROM social_user_profile WHERE user_id = 10002);

INSERT INTO social_user_profile (
  user_id, nickname, gender, city_code, profession, relationship_goal, avatar_url, bio,
  album_count, real_verified, profile_status, remark, created_by, created_at, updated_by, updated_at
)
SELECT 10003, 'Luna', 'F', 'shenzhen', '产品经理', 'ACTIVITY', '',
       '偏爱旅行和livehouse，想认识同频的朋友，也接受慢慢发展。', 4, 'N', 'A', '首页演示数据', 'sql_init', NOW(), 'sql_init', NOW()
WHERE NOT EXISTS (SELECT 1 FROM social_user_profile WHERE user_id = 10003);

-- 后台菜单初始化，可在需要时执行
SET @social_root := (SELECT IFNULL(MAX(menu_id), 2000) + 1 FROM sys_menu);
SET @social_profile_menu := @social_root + 1;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT @social_root, '社交运营', 0, 6, 'social', NULL, '', 'Social', 1, 0, 'M', '0', '0', '', 'peoples', 'sql_init', NOW(), '', NULL, '社交交友运营目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'social' AND parent_id = 0);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT @social_profile_menu, '资料中心', @social_root, 1, 'profile', 'social/profile/index', '', 'SocialProfile', 1, 0, 'C', '0', '0', 'social:profile:list', 'people', 'sql_init', NOW(), '', NULL, '社交资料管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE component = 'social/profile/index');
