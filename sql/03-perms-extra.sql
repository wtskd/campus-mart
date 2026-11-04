-- =====================================================================
-- CampusMart 权限补丁 + 订单表索引调整
-- 执行顺序：01-schema.sql -> 02-seed-business.sql -> 03-perms-extra.sql
-- 幂等：可重复执行
-- =====================================================================

USE campus_mart;

-- ---------------------------------------------------------------------
-- 1. 订单表：取消 item_id 唯一索引
--    二手商品是单品，但订单取消后商品要恢复在售、可以再次成交，
--    因此 item_id 不能唯一；并发安全由下单时的行锁 + 有效订单校验保证。
-- ---------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = 'campus_mart' AND TABLE_NAME = 'biz_order'
                     AND INDEX_NAME = 'uk_item');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE biz_order DROP INDEX uk_item', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = 'campus_mart' AND TABLE_NAME = 'biz_order'
                     AND INDEX_NAME = 'idx_item_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE biz_order ADD INDEX idx_item_id (item_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 权限标识归一化（统一 canonical 写法：模块:资源:动作）
-- ---------------------------------------------------------------------
UPDATE sys_menu SET perms = 'item:item:list'    WHERE menu_id = 201;
UPDATE sys_menu SET perms = 'item:item:audit'   WHERE menu_id = 200;
UPDATE sys_menu SET perms = 'item:category:list' WHERE menu_id = 202;
UPDATE sys_menu SET perms = 'item:order:list'   WHERE menu_id = 203;
UPDATE sys_menu SET perms = 'item:comment:list' WHERE menu_id = 204;
UPDATE sys_menu SET perms = 'item:report:list'  WHERE menu_id = 205;
UPDATE sys_menu SET perms = 'analysis:overview' WHERE menu_id = 300;
UPDATE sys_menu SET perms = 'analysis:trade'    WHERE menu_id = 301;
UPDATE sys_menu SET perms = 'analysis:user'     WHERE menu_id = 302;
UPDATE sys_menu SET perms = 'analysis:funnel'   WHERE menu_id = 303;
UPDATE sys_menu SET perms = 'analysis:screen'   WHERE menu_id = 304;
UPDATE sys_menu SET perms = 'ai:log:list'       WHERE menu_id = 400;
UPDATE sys_menu SET perms = 'ai:prompt:list'    WHERE menu_id = 401;
UPDATE sys_menu SET perms = 'ai:knowledge:list' WHERE menu_id = 402;

-- ---------------------------------------------------------------------
-- 3. 按钮级权限（menu_type = 'F'，只在权限树/按钮控制中使用，不出现在侧边栏）
-- ---------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 500 AND 579;
DELETE FROM sys_menu WHERE menu_id BETWEEN 500 AND 579;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms) VALUES
  -- 商品列表
  (500, '商品查询', 201, 1, 'F', '0', '0', 'item:item:query'),
  (501, '商品新增', 201, 2, 'F', '0', '0', 'item:item:add'),
  (502, '商品修改', 201, 3, 'F', '0', '0', 'item:item:edit'),
  (503, '商品删除', 201, 4, 'F', '0', '0', 'item:item:remove'),
  (504, '商品下架', 201, 5, 'F', '0', '0', 'item:item:offline'),
  -- 分类管理
  (510, '分类新增', 202, 1, 'F', '0', '0', 'item:category:add'),
  (511, '分类修改', 202, 2, 'F', '0', '0', 'item:category:edit'),
  (512, '分类删除', 202, 3, 'F', '0', '0', 'item:category:remove'),
  -- 订单管理
  (520, '订单确认', 203, 1, 'F', '0', '0', 'item:order:confirm'),
  (521, '订单发货', 203, 2, 'F', '0', '0', 'item:order:deliver'),
  (522, '订单完成', 203, 3, 'F', '0', '0', 'item:order:finish'),
  (523, '订单取消', 203, 4, 'F', '0', '0', 'item:order:cancel'),
  (524, '订单退款', 203, 5, 'F', '0', '0', 'item:order:refund'),
  (525, '订单导出', 203, 6, 'F', '0', '0', 'item:order:export'),
  -- 评价管理
  (530, '评价回复', 204, 1, 'F', '0', '0', 'item:comment:reply'),
  (531, '评价删除', 204, 2, 'F', '0', '0', 'item:comment:remove'),
  (532, '评价审核', 204, 3, 'F', '0', '0', 'item:comment:audit'),
  -- 举报处理
  (540, '举报处理', 205, 1, 'F', '0', '0', 'item:report:handle'),
  -- 公告/轮播
  (550, '公告新增', 106, 1, 'F', '0', '0', 'system:notice:add'),
  (551, '公告修改', 106, 2, 'F', '0', '0', 'system:notice:edit'),
  (552, '公告删除', 106, 3, 'F', '0', '0', 'system:notice:remove'),
  (553, '轮播新增', 107, 1, 'F', '0', '0', 'system:banner:add'),
  (554, '轮播修改', 107, 2, 'F', '0', '0', 'system:banner:edit'),
  (555, '轮播删除', 107, 3, 'F', '0', '0', 'system:banner:remove'),
  -- AI 管理
  (560, 'Prompt新增', 401, 1, 'F', '0', '0', 'ai:prompt:add'),
  (561, 'Prompt修改', 401, 2, 'F', '0', '0', 'ai:prompt:edit'),
  (562, '知识库新增', 402, 1, 'F', '0', '0', 'ai:knowledge:add'),
  (563, '知识库修改', 402, 2, 'F', '0', '0', 'ai:knowledge:edit'),
  (564, '知识库删除', 402, 3, 'F', '0', '0', 'ai:knowledge:remove'),
  -- 数据分析（导出与刷新属于写操作，单独授权）
  (570, '数据导出', 300, 1, 'F', '0', '0', 'analysis:export'),
  (571, '统计重算', 300, 2, 'F', '0', '0', 'analysis:refresh');

-- ---------------------------------------------------------------------
-- 4. 角色授权
-- ---------------------------------------------------------------------
-- 超级管理员：全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu;

-- 运营管理员（operator）：商品、订单、评价、举报、公告、轮播、数据统计、AI 日志
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
  (2, 2), (2, 200), (2, 201), (2, 202), (2, 203), (2, 204), (2, 205),
  (2, 500), (2, 501), (2, 502), (2, 503), (2, 504),
  (2, 510), (2, 511), (2, 512),
  (2, 520), (2, 521), (2, 522), (2, 523), (2, 524), (2, 525),
  (2, 530), (2, 531), (2, 532),
  (2, 540),
  (2, 1), (2, 106), (2, 107), (2, 550), (2, 551), (2, 552), (2, 553), (2, 554), (2, 555),
  (2, 3), (2, 300), (2, 301), (2, 302), (2, 303), (2, 304), (2, 570), (2, 571),
  (2, 4), (2, 400), (2, 401), (2, 402), (2, 560), (2, 561), (2, 562), (2, 563), (2, 564);

-- 审核专员（auditor）：商品审核、评价、举报
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
  (3, 2), (3, 200), (3, 201), (3, 204), (3, 205),
  (3, 500), (3, 503), (3, 504), (3, 530), (3, 531), (3, 532), (3, 540);

-- 普通用户（student）：无后台菜单
DELETE FROM sys_role_menu WHERE role_id = 4;

-- ---------------------------------------------------------------------
-- 5. 字典补充：消息类型 msg_type（1系统/2订单/3审核/4AI）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_dict_type (dict_id, dict_name, dict_type, status, remark)
VALUES (8, '消息类型', 'msg_type', '0', '站内消息分类');

DELETE FROM sys_dict_data WHERE dict_type = 'msg_type';
INSERT INTO sys_dict_data (dict_code, dict_type, dict_label, dict_value, dict_sort, is_default, status) VALUES
  (101, 'msg_type', '系统通知', '1', 1, 'Y', '0'),
  (102, 'msg_type', '订单消息', '2', 2, 'N', '0'),
  (103, 'msg_type', '审核结果', '3', 3, 'N', '0'),
  (104, 'msg_type', 'AI 消息', '4', 4, 'N', '0');

-- AI 场景字典补 price（智能估价）
INSERT IGNORE INTO sys_dict_data (dict_code, dict_type, dict_label, dict_value, dict_sort, is_default, status) VALUES
  (105, 'ai_scene', '智能估价', 'price', 5, 'N', '0'),
  (106, 'ai_scene', '商品摘要', 'summary', 6, 'N', '0');

-- ---------------------------------------------------------------------
-- 6. 校验
-- ---------------------------------------------------------------------
SELECT r.role_key, COUNT(rm.menu_id) AS menu_count
FROM sys_role r LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
GROUP BY r.role_id, r.role_key;
