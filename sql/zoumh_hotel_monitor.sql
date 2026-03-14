CREATE TABLE IF NOT EXISTS hotel_price_monitor (
  monitor_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '监控ID',
  hotel_name VARCHAR(128) NOT NULL COMMENT '酒店名称',
  platform VARCHAR(64) NOT NULL COMMENT '监控平台',
  city VARCHAR(64) DEFAULT '' COMMENT '城市',
  room_type VARCHAR(128) DEFAULT '' COMMENT '房型',
  check_in_date DATE NOT NULL COMMENT '入住日期',
  check_out_date DATE NOT NULL COMMENT '离店日期',
  target_price DECIMAL(10,2) NOT NULL COMMENT '目标价格',
  current_price DECIMAL(10,2) DEFAULT NULL COMMENT '当前价格',
  currency VARCHAR(16) DEFAULT 'CNY' COMMENT '币种',
  channel_url VARCHAR(512) DEFAULT '' COMMENT '渠道链接',
  crawl_enabled CHAR(1) DEFAULT 'Y' COMMENT '是否启用爬虫',
  crawl_strategy VARCHAR(32) DEFAULT 'html' COMMENT '抓取策略',
  crawl_config VARCHAR(2000) DEFAULT NULL COMMENT '抓取配置(JSON)',
  status VARCHAR(16) DEFAULT 'tracking' COMMENT '监控状态',
  latest_change DECIMAL(10,2) DEFAULT 0.00 COMMENT '最近波动',
  last_checked_time DATETIME DEFAULT NULL COMMENT '最近检查时间',
  last_crawled_at DATETIME DEFAULT NULL COMMENT '最近爬取时间',
  last_error_message VARCHAR(500) DEFAULT '' COMMENT '最近爬取错误',
  notify_enabled CHAR(1) DEFAULT 'Y' COMMENT '是否提醒',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (monitor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店价格监控任务';

ALTER TABLE hotel_price_monitor ADD COLUMN IF NOT EXISTS crawl_enabled CHAR(1) DEFAULT 'Y' COMMENT '是否启用爬虫';
ALTER TABLE hotel_price_monitor ADD COLUMN IF NOT EXISTS crawl_strategy VARCHAR(32) DEFAULT 'html' COMMENT '抓取策略';
ALTER TABLE hotel_price_monitor ADD COLUMN IF NOT EXISTS crawl_config VARCHAR(2000) DEFAULT NULL COMMENT '抓取配置(JSON)';
ALTER TABLE hotel_price_monitor ADD COLUMN IF NOT EXISTS last_crawled_at DATETIME DEFAULT NULL COMMENT '最近爬取时间';
ALTER TABLE hotel_price_monitor ADD COLUMN IF NOT EXISTS last_error_message VARCHAR(500) DEFAULT '' COMMENT '最近爬取错误';

CREATE TABLE IF NOT EXISTS hotel_price_history (
  history_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  monitor_id BIGINT NOT NULL COMMENT '监控ID',
  observed_price DECIMAL(10,2) NOT NULL COMMENT '观测价格',
  change_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格变化',
  availability VARCHAR(32) DEFAULT 'tracking' COMMENT '可订状态',
  source_note VARCHAR(255) DEFAULT '' COMMENT '来源说明',
  observed_at DATETIME NOT NULL COMMENT '观测时间',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (history_id),
  KEY idx_monitor_time (monitor_id, observed_at),
  CONSTRAINT fk_hotel_price_history_monitor FOREIGN KEY (monitor_id) REFERENCES hotel_price_monitor (monitor_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店价格历史';

DELETE rm
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms LIKE 'hotel:monitor:%'
   OR (m.path = 'hotel' AND m.parent_id = 0)
   OR (m.path = 'monitor' AND m.component = 'hotel/monitor/index');

DELETE FROM sys_menu
WHERE perms LIKE 'hotel:monitor:%'
   OR (path = 'monitor' AND component = 'hotel/monitor/index')
   OR (path = 'hotel' AND parent_id = 0);

SET @hotel_root := (SELECT IFNULL(MAX(menu_id), 2000) + 1 FROM sys_menu);
SET @hotel_page := @hotel_root + 1;
SET @hotel_query := @hotel_root + 2;
SET @hotel_add := @hotel_root + 3;
SET @hotel_edit := @hotel_root + 4;
SET @hotel_remove := @hotel_root + 5;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(@hotel_root, '酒店监控', 0, 4, 'hotel', NULL, '', 'Hotel', 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '酒店价格监控目录'),
(@hotel_page, '价格监控', @hotel_root, 1, 'monitor', 'hotel/monitor/index', '', 'HotelMonitor', 1, 0, 'C', '0', '0', 'hotel:monitor:list', 'list', 'admin', NOW(), '', NULL, '酒店价格监控菜单'),
(@hotel_query, '监控查询', @hotel_page, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hotel:monitor:query', '#', 'admin', NOW(), '', NULL, ''),
(@hotel_add, '监控新增', @hotel_page, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hotel:monitor:add', '#', 'admin', NOW(), '', NULL, ''),
(@hotel_edit, '监控修改', @hotel_page, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hotel:monitor:edit', '#', 'admin', NOW(), '', NULL, ''),
(@hotel_remove, '监控删除', @hotel_page, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'hotel:monitor:remove', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id
FROM sys_menu
WHERE menu_id BETWEEN @hotel_root AND @hotel_remove;
