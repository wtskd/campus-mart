-- ============================================================
-- CampusMart 权限补充 04：公告/轮播/消息 权限码归一到 item 模块
-- 幂等：可重复执行。执行顺序：01-schema -> 02-seed-business -> 03-perms-extra -> 04-perms-item-trade
-- ============================================================
USE `campus_mart`;

-- ---- 1. 公告 / 轮播：权限码由 system:* 改为 item:*（表与接口都在 campus-item） ----
UPDATE `sys_menu` SET `perms` = 'item:notice:list'  WHERE menu_id = 106;
UPDATE `sys_menu` SET `perms` = 'item:banner:list'  WHERE menu_id = 107;

UPDATE `sys_menu` SET `perms` = 'item:notice:add'    WHERE menu_id = 550;
UPDATE `sys_menu` SET `perms` = 'item:notice:edit'   WHERE menu_id = 551;
UPDATE `sys_menu` SET `perms` = 'item:notice:remove' WHERE menu_id = 552;
UPDATE `sys_menu` SET `perms` = 'item:banner:add'    WHERE menu_id = 553;
UPDATE `sys_menu` SET `perms` = 'item:banner:edit'   WHERE menu_id = 554;
UPDATE `sys_menu` SET `perms` = 'item:banner:remove' WHERE menu_id = 555;

-- ---- 2. 新增「消息通知」菜单（读取 biz_message） ----
DELETE FROM `sys_menu` WHERE menu_id = 206;
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`)
VALUES
(206, '消息通知', 1, 9, 'message', 'item/message/index', NULL,
 '1', '0', 'C', '0', '0', 'item:message:list', 'message');

-- ---- 3. 角色授权（先清后插，保证幂等；admin 用全量补齐） ----
DELETE FROM `sys_role_menu` WHERE role_id = 2 AND menu_id IN (206);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
 (2, 206);

-- admin（超级管理员）不足的全部补齐
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu`;

-- 审核专员：商品审核相关 + 评价审核 + 举报处理 + 公告查看
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
 (3, 1), (3, 106), (3, 200), (3, 201), (3, 204), (3, 205),
 (3, 500), (3, 501), (3, 502), (3, 503), (3, 504),
 (3, 530), (3, 531), (3, 532), (3, 540);

-- 运营：消息通知 + 公告/轮播全量 + 订单全量
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
 (2, 106), (2, 107), (2, 206),
 (2, 550), (2, 551), (2, 552), (2, 553), (2, 554), (2, 555),
 (2, 520), (2, 521), (2, 522), (2, 523), (2, 524), (2, 525);

-- ---- 4. 校验输出 ----
SELECT menu_id, menu_name, perms FROM `sys_menu` WHERE menu_id IN (106, 107, 206, 550, 551, 552, 553, 554, 555) ORDER BY menu_id;
SELECT r.role_key, COUNT(*) AS menu_cnt FROM `sys_role_menu` rm JOIN `sys_role` r ON r.role_id = rm.role_id GROUP BY r.role_key;
