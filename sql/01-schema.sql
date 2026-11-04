-- =====================================================================
-- 校园易站 CampusMart 数据库设计
-- MySQL 8.0+  utf8mb4  InnoDB
-- 说明：本文件只做建库建表与基础种子数据；演示用大批量数据由
--       docs/seed-data.sql 或 DemoDataGenerator 生成。
-- 默认账号：admin / admin123（超级管理员），student01 / 123456（小程序用户）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `campus_mart` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus_mart`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 一、系统域 sys_
-- =====================================================================

-- 学院/班级（数据权限维度）
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '学院ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父ID，0为根',
  `ancestors`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '祖级列表，逗号分隔',
  `dept_name`   VARCHAR(64)  NOT NULL COMMENT '学院/班级名称',
  `order_num`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `leader`      VARCHAR(32)  DEFAULT NULL COMMENT '负责人',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标记 0存在 1删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`dept_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='学院/班级表';

-- 用户表（管理端用户 + 小程序用户共用，用 user_type 区分）
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id`      BIGINT       DEFAULT NULL COMMENT '学院ID',
  `username`     VARCHAR(32)  NOT NULL COMMENT '登录账号',
  `nickname`     VARCHAR(32)  NOT NULL COMMENT '昵称',
  `user_type`    CHAR(2)      NOT NULL DEFAULT '01' COMMENT '用户类型 00管理端 01小程序端',
  `password`     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  `phone`        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`        VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  `avatar`       VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender`       CHAR(1)      NOT NULL DEFAULT '2' COMMENT '性别 0男 1女 2未知',
  `student_no`   VARCHAR(32)  DEFAULT NULL COMMENT '学号',
  `credit_score` INT          NOT NULL DEFAULT 100 COMMENT '信用分 0-100',
  `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  `login_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '最后登录IP',
  `login_date`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_type_status` (`user_type`, `status`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB COMMENT='用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   VARCHAR(32)  NOT NULL COMMENT '角色名称',
  `role_key`    VARCHAR(64)  NOT NULL COMMENT '角色标识，如 admin/operator',
  `role_sort`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `data_scope`  CHAR(1)      NOT NULL DEFAULT '1' COMMENT '数据范围 1全部 2本学院 3本人',
  `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB COMMENT='角色表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name`   VARCHAR(64)  NOT NULL COMMENT '菜单名称',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `path`        VARCHAR(200) DEFAULT '' COMMENT '路由地址',
  `component`   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `query_param` VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
  `is_frame`    CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否外链 0是 1否',
  `is_cache`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '是否缓存 0缓存 1不缓存',
  `menu_type`   CHAR(1)      NOT NULL DEFAULT 'C' COMMENT '类型 M目录 C菜单 F按钮',
  `visible`     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '显示 0显示 1隐藏',
  `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  `perms`       VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
  `icon`        VARCHAR(64)  DEFAULT NULL COMMENT '图标',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`menu_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='菜单权限表（动态菜单来源）';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB COMMENT='角色菜单关联';

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_name`   VARCHAR(64)  NOT NULL COMMENT '字典名称',
  `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型编码',
  `status`      CHAR(1)      NOT NULL DEFAULT '0',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB COMMENT='字典类型';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code`   BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型编码',
  `dict_label`  VARCHAR(64)  NOT NULL COMMENT '显示值',
  `dict_value`  VARCHAR(64)  NOT NULL COMMENT '实际值',
  `dict_sort`   INT          NOT NULL DEFAULT 0,
  `css_class`   VARCHAR(64)  DEFAULT NULL COMMENT '样式类',
  `list_class`  VARCHAR(64)  DEFAULT NULL COMMENT '标签样式',
  `is_default`  CHAR(1)      NOT NULL DEFAULT 'N',
  `status`      CHAR(1)      NOT NULL DEFAULT '0',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB COMMENT='字典数据';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `config_name`  VARCHAR(64)  NOT NULL COMMENT '参数名称',
  `config_key`   VARCHAR(128) NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '参数值',
  `config_type`  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否内置 Y/N',
  `remark`       VARCHAR(255) DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB COMMENT='系统参数';

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id`       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `title`         VARCHAR(64)   DEFAULT NULL COMMENT '模块标题',
  `business_type` CHAR(2)       NOT NULL DEFAULT '0' COMMENT '业务类型 0其他 1新增 2修改 3删除 4查询 5导出',
  `method`        VARCHAR(200)  DEFAULT NULL COMMENT '方法名',
  `request_method` VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
  `operator_type` CHAR(2)       NOT NULL DEFAULT '00' COMMENT '操作人类别',
  `oper_name`     VARCHAR(32)   DEFAULT NULL COMMENT '操作人',
  `dept_name`     VARCHAR(64)   DEFAULT NULL COMMENT '学院',
  `oper_url`      VARCHAR(255)  DEFAULT NULL COMMENT '请求URL',
  `oper_ip`       VARCHAR(64)   DEFAULT NULL COMMENT 'IP',
  `oper_param`    TEXT          COMMENT '请求参数（脱敏）',
  `json_result`   TEXT          COMMENT '返回结果',
  `status`        CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0成功 1失败',
  `error_msg`     VARCHAR(2000) DEFAULT NULL,
  `cost_time`     BIGINT        NOT NULL DEFAULT 0 COMMENT '耗时ms',
  `oper_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_oper_name` (`oper_name`)
) ENGINE=InnoDB COMMENT='操作日志表';

DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `info_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(32)  DEFAULT NULL,
  `ipaddr`       VARCHAR(64)  DEFAULT NULL,
  `login_location` VARCHAR(255) DEFAULT NULL,
  `browser`      VARCHAR(64)  DEFAULT NULL,
  `os`           VARCHAR(64)  DEFAULT NULL,
  `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0成功 1失败',
  `msg`          VARCHAR(255) DEFAULT NULL,
  `login_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`info_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB COMMENT='登录日志表';

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `file_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `file_name`     VARCHAR(128) NOT NULL COMMENT '存储文件名',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path`     VARCHAR(500) NOT NULL COMMENT '存储路径',
  `file_url`      VARCHAR(500) NOT NULL COMMENT '访问URL',
  `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '字节',
  `file_type`     VARCHAR(64)  DEFAULT NULL COMMENT 'MIME类型',
  `storage`       VARCHAR(16)  NOT NULL DEFAULT 'local' COMMENT 'local/minio',
  `biz_type`      VARCHAR(32)  DEFAULT NULL COMMENT '业务类型 item/avatar/...',
  `create_by`     BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB COMMENT='文件表';

-- =====================================================================
-- 二、业务域 biz_
-- =====================================================================

DROP TABLE IF EXISTS `biz_category`;
CREATE TABLE `biz_category` (
  `category_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类，0为一级',
  `category_name` VARCHAR(32)  NOT NULL,
  `icon`          VARCHAR(255) DEFAULT NULL,
  `order_num`     INT          NOT NULL DEFAULT 0,
  `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='商品分类';

DROP TABLE IF EXISTS `biz_item`;
CREATE TABLE `biz_item` (
  `item_id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `item_no`        VARCHAR(32)   NOT NULL COMMENT '商品编号',
  `seller_id`      BIGINT        NOT NULL COMMENT '卖家ID',
  `category_id`    BIGINT        NOT NULL COMMENT '分类ID',
  `title`          VARCHAR(100)  NOT NULL COMMENT '标题',
  `summary`        VARCHAR(255)  DEFAULT NULL COMMENT '摘要',
  `description`    TEXT          COMMENT '详细描述',
  `price`          DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `item_condition` TINYINT       NOT NULL DEFAULT 9 COMMENT '成色 1-10（10全新）',
  `trade_type`     TINYINT       NOT NULL DEFAULT 1 COMMENT '1出售 2求购 3交换',
  `cover_url`      VARCHAR(500)  DEFAULT NULL COMMENT '封面图',
  `images`         JSON          DEFAULT NULL COMMENT '图片列表',
  `tags`           JSON          DEFAULT NULL COMMENT '标签数组',
  `location`       VARCHAR(64)   DEFAULT NULL COMMENT '交易地点',
  `contact_type`   TINYINT       NOT NULL DEFAULT 0 COMMENT '0站内 1手机 2微信',
  `ai_generated`   CHAR(1)       NOT NULL DEFAULT '0' COMMENT '是否AI生成 0否 1是',
  `view_count`     INT           NOT NULL DEFAULT 0,
  `favorite_count` INT           NOT NULL DEFAULT 0,
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规',
  `audit_remark`   VARCHAR(500)  DEFAULT NULL COMMENT '审核意见',
  `publish_time`   DATETIME      DEFAULT NULL,
  `del_flag`       CHAR(1)       NOT NULL DEFAULT '0',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_no` (`item_no`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_category_status` (`category_id`, `status`),
  KEY `idx_status_publish` (`status`, `publish_time`),
  KEY `idx_price` (`price`),
  FULLTEXT KEY `ft_item` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='商品表';

DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
  `order_id`      BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`      VARCHAR(32)   NOT NULL COMMENT '订单号',
  `item_id`       BIGINT        NOT NULL COMMENT '商品ID（二手单品，一单一物）',
  `buyer_id`      BIGINT        NOT NULL,
  `seller_id`     BIGINT        NOT NULL,
  `amount`        DECIMAL(10,2) NOT NULL COMMENT '成交金额',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待确认 1待交付 2已完成 3已取消 4退款中 5已退款',
  `pay_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '0线下 1模拟在线',
  `trade_place`   VARCHAR(64)   DEFAULT NULL COMMENT '交易地点',
  `buyer_remark`  VARCHAR(255)  DEFAULT NULL,
  `cancel_reason` VARCHAR(255)  DEFAULT NULL,
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time`      DATETIME      DEFAULT NULL,
  `finish_time`   DATETIME      DEFAULT NULL,
  `cancel_time`   DATETIME      DEFAULT NULL,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_item` (`item_id`),
  KEY `idx_buyer` (`buyer_id`, `status`),
  KEY `idx_seller` (`seller_id`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='订单表';

DROP TABLE IF EXISTS `biz_favorite`;
CREATE TABLE `biz_favorite` (
  `favorite_id` BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `item_id`     BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB COMMENT='收藏表';

DROP TABLE IF EXISTS `biz_comment`;
CREATE TABLE `biz_comment` (
  `comment_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`     BIGINT       NOT NULL,
  `item_id`      BIGINT       NOT NULL,
  `from_user_id` BIGINT       NOT NULL COMMENT '评价人',
  `to_user_id`   BIGINT       NOT NULL COMMENT '被评价人',
  `score`        TINYINT      NOT NULL DEFAULT 5 COMMENT '1-5 星',
  `content`      VARCHAR(500) DEFAULT NULL,
  `images`       JSON         DEFAULT NULL,
  `reply_content` VARCHAR(500) DEFAULT NULL COMMENT '卖家回复',
  `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0正常 1隐藏',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_to_user` (`to_user_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB COMMENT='评价表';

DROP TABLE IF EXISTS `biz_report`;
CREATE TABLE `biz_report` (
  `report_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `item_id`       BIGINT       NOT NULL,
  `reporter_id`   BIGINT       NOT NULL,
  `reason_type`   VARCHAR(32)  NOT NULL COMMENT '字典 report_reason',
  `content`       VARCHAR(500) DEFAULT NULL,
  `images`        JSON         DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2驳回',
  `handle_user_id` BIGINT      DEFAULT NULL,
  `handle_remark` VARCHAR(500) DEFAULT NULL,
  `handle_time`   DATETIME     DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  KEY `idx_status` (`status`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB COMMENT='举报表';

DROP TABLE IF EXISTS `biz_message`;
CREATE TABLE `biz_message` (
  `message_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '接收人',
  `msg_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1系统 2订单 3举报 4AI',
  `title`       VARCHAR(100) NOT NULL,
  `content`     VARCHAR(500) DEFAULT NULL,
  `biz_id`      BIGINT       DEFAULT NULL COMMENT '关联业务ID',
  `is_read`     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB COMMENT='站内消息';

DROP TABLE IF EXISTS `biz_notice`;
CREATE TABLE `biz_notice` (
  `notice_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(100) NOT NULL,
  `content`      TEXT         NOT NULL,
  `notice_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '1公告 2活动 3规则',
  `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0发布 1草稿',
  `publish_time` DATETIME     DEFAULT NULL,
  `create_by`    BIGINT       DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB COMMENT='公告表';

DROP TABLE IF EXISTS `biz_banner`;
CREATE TABLE `biz_banner` (
  `banner_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `title`      VARCHAR(64)  NOT NULL,
  `image_url`  VARCHAR(500) NOT NULL,
  `link_url`   VARCHAR(500) DEFAULT NULL,
  `sort`       INT          NOT NULL DEFAULT 0,
  `status`     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0上架 1下架',
  `start_time` DATETIME     DEFAULT NULL,
  `end_time`   DATETIME     DEFAULT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`banner_id`)
) ENGINE=InnoDB COMMENT='首页轮播';

-- =====================================================================
-- 三、统计域 stat_（预聚合结果，管理端只查这些表）
-- =====================================================================

DROP TABLE IF EXISTS `stat_overview_daily`;
CREATE TABLE `stat_overview_daily` (
  `stat_date`          DATE          NOT NULL COMMENT '统计日期',
  `gmv`                DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '成交额',
  `order_count`        INT           NOT NULL DEFAULT 0 COMMENT '下单数',
  `finish_order_count` INT           NOT NULL DEFAULT 0 COMMENT '完成订单数',
  `cancel_order_count` INT           NOT NULL DEFAULT 0 COMMENT '取消订单数',
  `item_publish_count` INT           NOT NULL DEFAULT 0 COMMENT '商品发布数',
  `item_sold_count`    INT           NOT NULL DEFAULT 0 COMMENT '成交商品数',
  `new_user_count`     INT           NOT NULL DEFAULT 0 COMMENT '新增用户数',
  `active_user_count`  INT           NOT NULL DEFAULT 0 COMMENT '活跃用户数',
  `view_count`         INT           NOT NULL DEFAULT 0 COMMENT '商品浏览数',
  `favorite_count`     INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `ai_call_count`      INT           NOT NULL DEFAULT 0 COMMENT 'AI调用次数',
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB COMMENT='每日概览统计';

DROP TABLE IF EXISTS `stat_category_daily`;
CREATE TABLE `stat_category_daily` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `stat_date`   DATE          NOT NULL,
  `category_id` BIGINT        NOT NULL,
  `item_count`  INT           NOT NULL DEFAULT 0 COMMENT '发布数',
  `order_count` INT           NOT NULL DEFAULT 0,
  `gmv`         DECIMAL(14,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_cat` (`stat_date`, `category_id`)
) ENGINE=InnoDB COMMENT='分类每日统计';

DROP TABLE IF EXISTS `stat_funnel_daily`;
CREATE TABLE `stat_funnel_daily` (
  `stat_date`    DATE NOT NULL,
  `view_count`   INT  NOT NULL DEFAULT 0 COMMENT '浏览',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏',
  `order_count`  INT  NOT NULL DEFAULT 0 COMMENT '下单',
  `pay_count`    INT  NOT NULL DEFAULT 0 COMMENT '支付',
  `finish_count` INT  NOT NULL DEFAULT 0 COMMENT '完成',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB COMMENT='转化漏斗每日统计';

DROP TABLE IF EXISTS `stat_user_rfm`;
CREATE TABLE `stat_user_rfm` (
  `user_id`      BIGINT        NOT NULL,
  `recency_days` INT           NOT NULL DEFAULT 999 COMMENT '最近一次交易距今天数',
  `frequency`    INT           NOT NULL DEFAULT 0 COMMENT '交易次数',
  `monetary`     DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '交易金额',
  `r_score`      TINYINT       NOT NULL DEFAULT 1,
  `f_score`      TINYINT       NOT NULL DEFAULT 1,
  `m_score`      TINYINT       NOT NULL DEFAULT 1,
  `segment`      VARCHAR(16)   NOT NULL DEFAULT '一般发展' COMMENT '用户分层',
  `calc_date`    DATE          NOT NULL,
  PRIMARY KEY (`user_id`, `calc_date`),
  KEY `idx_segment` (`calc_date`, `segment`)
) ENGINE=InnoDB COMMENT='用户RFM分层';

-- =====================================================================
-- 四、AI 域 ai_
-- =====================================================================

DROP TABLE IF EXISTS `ai_prompt_template`;
CREATE TABLE `ai_prompt_template` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `code`          VARCHAR(64)  NOT NULL COMMENT '模板编码，如 ITEM_COPYWRITE',
  `name`          VARCHAR(64)  NOT NULL,
  `scene`         VARCHAR(32)  NOT NULL COMMENT '场景 copywrite/chat/audit/price',
  `system_prompt` TEXT         NOT NULL,
  `user_prompt`   TEXT         NOT NULL COMMENT '支持 {{变量}} 占位',
  `model`         VARCHAR(64)  DEFAULT NULL COMMENT '为空用默认模型',
  `temperature`   DECIMAL(3,2) NOT NULL DEFAULT 0.70,
  `max_tokens`    INT          NOT NULL DEFAULT 1024,
  `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  `remark`        VARCHAR(255) DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='Prompt模板（后台可运营）';

DROP TABLE IF EXISTS `ai_call_log`;
CREATE TABLE `ai_call_log` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT       DEFAULT NULL,
  `scene`             VARCHAR(32)  NOT NULL COMMENT '场景',
  `template_code`     VARCHAR(64)  DEFAULT NULL,
  `provider`          VARCHAR(16)  NOT NULL DEFAULT 'remote' COMMENT 'remote/local',
  `model`             VARCHAR(64)  DEFAULT NULL,
  `request_summary`   VARCHAR(500) DEFAULT NULL COMMENT '请求摘要（脱敏截断）',
  `prompt_tokens`     INT          NOT NULL DEFAULT 0,
  `completion_tokens` INT          NOT NULL DEFAULT 0,
  `total_tokens`      INT          NOT NULL DEFAULT 0,
  `cost_ms`           BIGINT       NOT NULL DEFAULT 0,
  `status`            CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0成功 1失败 2降级兜底',
  `error_msg`         VARCHAR(500) DEFAULT NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_time` (`scene`, `create_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB COMMENT='AI调用日志（可观测性）';

DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `kb_type`     VARCHAR(32)  NOT NULL DEFAULT 'faq' COMMENT 'faq/rule/category',
  `title`       VARCHAR(200) NOT NULL,
  `content`     TEXT         NOT NULL,
  `keywords`    VARCHAR(255) DEFAULT NULL,
  `status`      CHAR(1)      NOT NULL DEFAULT '0',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FULLTEXT KEY `ft_knowledge` (`title`, `content`) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='RAG知识库';

DROP TABLE IF EXISTS `ai_recommend`;
CREATE TABLE `ai_recommend` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `item_id`    BIGINT       NOT NULL,
  `score`      DECIMAL(10,4) NOT NULL DEFAULT 0,
  `reason`     VARCHAR(64)  DEFAULT NULL COMMENT '推荐理由，如 同校同分类',
  `scene`      VARCHAR(16)  NOT NULL DEFAULT 'home' COMMENT 'home/detail/cart',
  `batch_date` DATE         NOT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item_scene` (`user_id`, `item_id`, `scene`, `batch_date`),
  KEY `idx_user_batch` (`user_id`, `batch_date`)
) ENGINE=InnoDB COMMENT='个性化推荐结果';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 五、种子数据
-- =====================================================================

-- 学院
INSERT INTO `sys_dept` (`dept_id`,`parent_id`,`ancestors`,`dept_name`,`order_num`,`leader`) VALUES
(100, 0,   '0',       '示范大学',   0, '校长'),
(101, 100, '0,100',   '计算机学院', 1, '张院长'),
(102, 100, '0,100',   '软件工程学院', 2, '李院长'),
(103, 100, '0,100',   '人工智能学院', 3, '王院长'),
(104, 100, '0,100',   '经济管理学院', 4, '赵院长');

-- 角色
INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`role_sort`,`data_scope`,`remark`) VALUES
(1, '超级管理员', 'admin',    1, '1', '拥有全部权限'),
(2, '运营管理员', 'operator', 2, '1', '商品审核、用户管理、数据统计'),
(3, '审核专员',   'auditor',  3, '2', '仅商品审核与举报处理'),
(4, '普通用户',   'student',  4, '3', '小程序端用户');

-- 用户（密码：admin 为 admin123，其余为 123456；BCrypt）
INSERT INTO `sys_user` (`user_id`,`dept_id`,`username`,`nickname`,`user_type`,`password`,`phone`,`email`,`gender`,`student_no`,`credit_score`,`remark`) VALUES
(1, 101, 'admin',     '超级管理员', '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800000001', 'admin@campus.edu', '0', NULL, 100, '系统内置管理员'),
(2, 101, 'operator',  '运营小王',   '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800000002', 'op@campus.edu',    '1', NULL, 100, NULL),
(3, 103, 'auditor',   '审核小李',   '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13800000003', 'au@campus.edu',    '0', NULL, 100, NULL),
(11, 101, 'student01', '小明同学',  '01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13900000001', NULL, '0', '2021001', 98, NULL),
(12, 102, 'student02', '小红同学',  '01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13900000002', NULL, '1', '2021002', 95, NULL),
(13, 103, 'student03', '小刚同学',  '01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13900000003', NULL, '0', '2021003', 100, NULL);

INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES (1,1),(2,2),(3,3),(11,4),(12,4),(13,4);

-- 菜单：目录 + 菜单（按钮级权限在 02-menu-buttons.sql 中补充）
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`menu_type`,`perms`,`icon`) VALUES
-- 系统管理
(1,  '系统管理', 0, 1, '/system', NULL, 'M', NULL, 'setting'),
(100,'用户管理', 1, 1, 'user',   'system/user/index',   'C', 'system:user:list',   'user'),
(101,'角色管理', 1, 2, 'role',   'system/role/index',   'C', 'system:role:list',   'peoples'),
(102,'菜单管理', 1, 3, 'menu',   'system/menu/index',   'C', 'system:menu:list',   'tree-table'),
(103,'学院管理', 1, 4, 'dept',   'system/dept/index',   'C', 'system:dept:list',   'tree'),
(104,'字典管理', 1, 5, 'dict',   'system/dict/index',   'C', 'system:dict:list',   'dict'),
(105,'参数设置', 1, 6, 'config', 'system/config/index', 'C', 'system:config:list', 'edit'),
(106,'公告管理', 1, 7, 'notice', 'system/notice/index', 'C', 'system:notice:list', 'message'),
(107,'轮播管理', 1, 8, 'banner', 'system/banner/index', 'C', 'system:banner:list', 'picture'),
(108,'文件管理', 1, 9, 'file',   'system/file/index',   'C', 'system:file:list',   'upload'),
(109,'操作日志', 1, 10,'operlog','system/operlog/index','C', 'system:operlog:list','form'),
(110,'登录日志', 1, 11,'loginlog','system/loginlog/index','C','system:loginlog:list','logininfor'),
-- 商品管理
(2,  '商品管理', 0, 2, '/item', NULL, 'M', NULL, 'shopping'),
(200,'商品审核', 2, 1, 'audit',    'item/audit/index',    'C', 'item:audit:list',    'checkbox'),
(201,'商品列表', 2, 2, 'list',     'item/list/index',     'C', 'item:list',          'list'),
(202,'分类管理', 2, 3, 'category', 'item/category/index', 'C', 'item:category:list', 'tree'),
(203,'订单管理', 2, 4, 'order',    'item/order/index',    'C', 'item:order:list',    'documentation'),
(204,'评价管理', 2, 5, 'comment',  'item/comment/index',  'C', 'item:comment:list',  'star'),
(205,'举报处理', 2, 6, 'report',   'item/report/index',   'C', 'item:report:list',   'bug'),
-- 数据统计
(3,  '数据统计', 0, 3, '/analysis', NULL, 'M', NULL, 'chart'),
(300,'数据概览', 3, 1, 'overview', 'analysis/overview/index', 'C', 'analysis:overview', 'dashboard'),
(301,'交易分析', 3, 2, 'trade',    'analysis/trade/index',    'C', 'analysis:trade',    'money'),
(302,'用户分析', 3, 3, 'user',     'analysis/user/index',     'C', 'analysis:user',     'peoples'),
(303,'转化漏斗', 3, 4, 'funnel',   'analysis/funnel/index',   'C', 'analysis:funnel',   'example'),
(304,'数据大屏', 3, 5, 'screen',   'analysis/screen/index',   'C', 'analysis:screen',   'monitor'),
-- AI 管理
(4,  'AI 管理', 0, 4, '/ai', NULL, 'M', NULL, 'star'),
(400,'AI调用日志', 4, 1, 'log',      'ai/log/index',      'C', 'ai:log:list',      'log'),
(401,'Prompt模板', 4, 2, 'prompt',   'ai/prompt/index',   'C', 'ai:prompt:list',   'edit'),
(402,'知识库',     4, 3, 'knowledge','ai/knowledge/index','C', 'ai:knowledge:list','education');

-- 角色-菜单：超管全部
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1, menu_id FROM `sys_menu`;
-- 运营：系统管理除菜单/参数、全部商品、全部统计、AI日志
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
(2,1),(2,100),(2,101),(2,103),(2,104),(2,106),(2,107),(2,108),(2,109),(2,110),
(2,2),(2,200),(2,201),(2,202),(2,203),(2,204),(2,205),
(2,3),(2,300),(2,301),(2,302),(2,303),(2,304),(2,4),(2,400);
-- 审核：只给商品审核、举报、日志
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
(3,2),(3,200),(3,205),(3,109);

-- 字典
INSERT INTO `sys_dict_type` (`dict_id`,`dict_name`,`dict_type`,`remark`) VALUES
(1,'商品状态','item_status','二手商品状态'),
(2,'订单状态','order_status',NULL),
(3,'交易方式','trade_type','出售/求购/交换'),
(4,'商品成色','item_condition','1-10级'),
(5,'举报原因','report_reason',NULL),
(6,'用户分层','user_segment','RFM分层'),
(7,'AI场景','ai_scene',NULL);

INSERT INTO `sys_dict_data` (`dict_type`,`dict_label`,`dict_value`,`dict_sort`,`list_class`) VALUES
('item_status','草稿','0',0,'info'),('item_status','待审核','1',1,'warning'),
('item_status','在售','2',2,'success'),('item_status','已预订','3',3,'primary'),
('item_status','已售出','4',4,'danger'),('item_status','已下架','5',5,'info'),
('item_status','违规','6',6,'danger'),
('order_status','待确认','0',0,'warning'),('order_status','待交付','1',1,'primary'),
('order_status','已完成','2',2,'success'),('order_status','已取消','3',3,'info'),
('order_status','退款中','4',4,'warning'),('order_status','已退款','5',5,'danger'),
('trade_type','出售','1',1,NULL),('trade_type','求购','2',2,NULL),('trade_type','交换','3',3,NULL),
('item_condition','全新','10',10,NULL),('item_condition','几乎全新','9',9,NULL),
('item_condition','轻微使用痕迹','7',7,NULL),('item_condition','明显使用痕迹','5',5,NULL),
('item_condition','较旧','3',3,NULL),
('report_reason','虚假信息','fake',1,NULL),('report_reason','违禁物品','illegal',2,NULL),
('report_reason','价格欺诈','price',3,NULL),('report_reason','骚扰谩骂','harass',4,NULL),
('report_reason','其他','other',9,NULL),
('user_segment','重要价值','重要价值',1,NULL),('user_segment','重要保持','重要保持',2,NULL),
('user_segment','重要发展','重要发展',3,NULL),('user_segment','一般价值','一般价值',4,NULL),
('user_segment','流失预警','流失预警',5,NULL);

-- 系统参数
INSERT INTO `sys_config` (`config_name`,`config_key`,`config_value`,`config_type`,`remark`) VALUES
('系统名称','sys.app.name','校园易站','Y',NULL),
('商品发布是否需要审核','item.publish.audit','true','Y','true/false'),
('单用户每日发布上限','item.publish.daily.limit','5','Y',NULL),
('AI 默认provider','ai.provider','remote','Y','remote/local'),
('AI 单用户每分钟次数','ai.rate.limit','20','Y',NULL),
('订单自动取消小时','order.auto.cancel.hours','24','Y',NULL);

-- 商品分类
INSERT INTO `biz_category` (`category_id`,`parent_id`,`category_name`,`icon`,`order_num`) VALUES
(1,0,'数码电子','phone',1),(2,0,'教材书籍','education',2),(3,0,'生活用品','shopping',3),
(4,0,'运动户外','run',4),(5,0,'美妆个护','star',5),(6,0,'宿舍家具','home',6),
(7,0,'乐器文娱','music',7),(8,0,'其他','more',8),
(101,1,'手机','phone',1),(102,1,'电脑','computer',2),(103,1,'配件','component',3),
(201,2,'专业教材','book',1),(202,2,'考研资料','book',2),(203,2,'课外读物','book',3),
(601,6,'桌椅','home',1),(602,6,'收纳','home',2);

-- Prompt 模板
INSERT INTO `ai_prompt_template` (`code`,`name`,`scene`,`system_prompt`,`user_prompt`,`model`,`temperature`,`max_tokens`,`remark`) VALUES
('ITEM_COPYWRITE','商品文案生成','copywrite',
 '你是校园二手交易平台的文案助手。请根据用户提供的商品信息，生成吸引人的标题和描述。要求：标题不超过30字，描述分3段（成色说明、使用体验、交易说明），不夸大事实，不编造成色。只返回JSON：{"title":"","summary":"","description":"","tags":["",""],"suggestPrice":0}',
 '商品关键词：{{keywords}}\n分类：{{category}}\n成色：{{condition}}\n原价：{{originalPrice}}\n期望价：{{price}}\n补充说明：{{remark}}',
 NULL, 0.80, 800, 'AI一键发布'),
('CUSTOMER_SERVICE','智能客服问答','chat',
 '你是校园易站平台的智能客服。只能依据【知识库】内容回答，知识库没有的内容请回答"这个问题我暂时不清楚，建议联系人工客服"。回答简洁，不超过150字。',
 '【知识库】\n{{context}}\n\n【用户问题】\n{{question}}', NULL, 0.30, 500, 'RAG问答'),
('ITEM_AUDIT','商品内容审核','audit',
 '你是校园二手平台的审核员。判断商品标题与描述是否违规（违禁品、诈骗、色情、辱骂、引流到站外）。只返回JSON：{"pass":true/false,"reason":"","riskLevel":"low/medium/high"}',
 '标题：{{title}}\n描述：{{description}}', NULL, 0.10, 300, '内容审核'),
('ITEM_PRICE','智能估价','price',
 '你是二手商品估价助手。根据商品分类、成色、原价和描述，给出建议售价区间。只返回JSON：{"low":0,"high":0,"reason":""}',
 '分类：{{category}}\n成色：{{condition}}\n原价：{{originalPrice}}\n描述：{{description}}', NULL, 0.50, 300, 'AI估价');

-- RAG 知识库
INSERT INTO `ai_knowledge` (`kb_type`,`title`,`content`,`keywords`) VALUES
('rule','如何发布二手商品','进入首页点击"发布"，填写标题、描述、价格，上传1-9张图片，选择分类和交易地点，提交后等待审核。审核通常在2小时内完成，通过后商品自动上架。每人每天最多发布5件商品。','发布,上架,审核,流程'),
('rule','交易流程说明','买家在商品详情页点击"我想要"，提交订单后卖家确认，双方约定线下交易地点完成面交，买家确认收货后订单完成，双方可互评。平台不收取任何佣金。','交易,面交,订单,流程'),
('rule','退款与取消规则','卖家未确认前买家和卖家都可取消订单；卖家确认后需双方协商，协商一致可取消。线下交易平台不提供资金担保，建议在校园内公共区域交易。','退款,取消,规则'),
('rule','信用分规则','初始信用分100分。成交并获好评+1分，被举报成立-10分，恶意取消-5分。信用分低于60分将被限制发布商品。','信用分,信誉,规则'),
('faq','商品审核不通过怎么办','常见原因：图片不清晰、描述与实物不符、包含站外联系方式、属于违禁品。可在"我的-我的发布"中查看审核意见，修改后重新提交。','审核,不通过,修改'),
('faq','如何联系卖家','商品详情页点击"我想要"后，可在订单详情中查看卖家公开的联系方式（视卖家设置的contact_type而定），也可通过站内消息联系。','联系,卖家,消息'),
('faq','支持哪些支付方式','平台演示版支持线下交易和模拟在线支付。真实交易建议当面验货后付款，切勿提前转账。','支付,付款,方式');

-- 公告 & 轮播
INSERT INTO `biz_notice` (`title`,`content`,`notice_type`,`status`,`publish_time`,`create_by`) VALUES
('校园易站正式上线','欢迎使用校园易站！发布闲置，让好物流转起来。','1','0',NOW(),1),
('交易安全提示','请尽量在校园公共区域面交，谨防电信诈骗。','3','0',NOW(),1);
INSERT INTO `biz_banner` (`title`,`image_url`,`link_url`,`sort`) VALUES
('开学季·闲置焕新','/images/banner1.png','/pages/index/index',1),
('毕业季·好物转让','/images/banner2.png','/pages/index/index',2);
