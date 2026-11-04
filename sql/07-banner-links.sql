-- =====================================================================
-- 07-banner-links.sql —— 首页轮播「点击跳转」数据修正（幂等，可重复执行）
--
-- 背景：biz_banner.link_url 字段早就有，但演示数据里填的是不存在的页面
--       （/pages/item/list、/pages/publish/index），点了会白屏。
-- 约定（后台「跳转类型」下拉按此生成，用户端 openBanner 按此解析）：
--   item:1234            商品详情
--   category:2           分类页并选中该一级分类
--   search:教材         搜索页并带上关键词
--   /pages/xxx/yyy       站内页面（tabBar 页面会自动 switchTab）
--   空字符串             不跳转
-- 执行顺序：01-schema → 02-seed-business → 03-perms-extra → 04-perms-item-trade
--           → 05-rich-data → 06-dict-fix → 07-banner-links
-- =====================================================================
USE `campus_mart`;

-- 1. 让闲置流动起来 → 分类页
UPDATE `biz_banner`
SET `link_url` = '/pages/category/category'
WHERE `banner_id` = 1;

-- 2. AI 一键发布 → 发布页（tabBar 页面，用户端走 switchTab）
UPDATE `biz_banner`
SET `link_url` = '/pages/item/publish'
WHERE `banner_id` = 2;

-- 3. 毕业季清仓专区 → 搜索「教材」
UPDATE `biz_banner`
SET `link_url` = 'search:教材'
WHERE `banner_id` = 3;

-- 自检
SELECT `banner_id`, `title`, `link_url`, `status`
FROM `biz_banner`
ORDER BY `sort`;
