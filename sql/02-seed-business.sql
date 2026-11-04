-- =====================================================================
-- CampusMart 业务演示数据（商品/订单/评价/收藏/举报/消息/公告/轮播/AI 调用日志）
-- 执行顺序：01-schema.sql -> 02-seed-business.sql -> 03-perms-extra.sql
-- 特点：时间全部相对 NOW() 生成，保证趋势图/漏斗/RFM/大屏任何时候都有近 30 天数据
-- 幂等：先删除本次生成范围内的数据再插入，可重复执行
-- =====================================================================

USE campus_mart;
SET NAMES utf8mb4;

-- 清理
DELETE FROM biz_comment  WHERE comment_id BETWEEN 3001 AND 3200;
DELETE FROM biz_favorite WHERE favorite_id BETWEEN 4001 AND 5000;
DELETE FROM biz_order    WHERE order_id BETWEEN 2001 AND 2100;
DELETE FROM biz_item     WHERE item_id BETWEEN 1001 AND 1060;
DELETE FROM biz_report   WHERE report_id BETWEEN 5001 AND 5100;
DELETE FROM biz_message  WHERE message_id BETWEEN 6001 AND 6200;
DELETE FROM ai_call_log  WHERE id BETWEEN 7001 AND 7200;
DELETE FROM biz_notice   WHERE notice_id BETWEEN 1 AND 9;
DELETE FROM biz_banner   WHERE banner_id BETWEEN 1 AND 9;

-- 1. 商品 60 条（24 已售出 / 6 已预订 / 25 在售 / 4 待审核 / 3 下架 / 2 违规 / 1 草稿）
INSERT INTO biz_item (item_id, item_no, seller_id, category_id, title, summary, description, price,
  original_price, item_condition, trade_type, cover_url, images, tags, location, contact_type,
  ai_generated, view_count, favorite_count, status, audit_remark, publish_time, del_flag, create_time, update_time) VALUES
(1001,'CM202610041001',11,101,'iPhone 12 128G 国行 95新 无拆修','iPhone 12 128G 国行 95新 无拆修','iPhone 12 128G 国行 95新 无拆修。成色 9 成新，功能完好无维修。原价 4999.00 元，现价 2400.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',2400.00,4999.00,9,2,'/upload/demo/item-01.png','["/upload/demo/item-01.png"]','["二手","校园","自提"]','西区宿舍',0,'1',862,71,4,NULL,DATE_SUB(NOW(), INTERVAL 26 DAY),'0',DATE_SUB(NOW(), INTERVAL 27 DAY),DATE_SUB(NOW(), INTERVAL 27 DAY)),
(1002,'CM202610061002',12,101,'小米 13 8+256 骁龙版 带原装充电器','小米 13 8+256 骁龙版 带原装充电器','小米 13 8+256 骁龙版 带原装充电器。成色 9 成新，功能完好无维修。原价 4299.00 元，现价 2100.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',2100.00,4299.00,9,2,'/upload/demo/item-02.png','["/upload/demo/item-02.png"]','["二手","校园","自提"]','南苑',0,'0',640,53,4,NULL,DATE_SUB(NOW(), INTERVAL 24 DAY),'0',DATE_SUB(NOW(), INTERVAL 25 DAY),DATE_SUB(NOW(), INTERVAL 25 DAY)),
(1003,'CM202610081003',13,102,'联想拯救者 Y7000 2021 游戏本 i7/16G/512G','联想拯救者 Y7000 2021 游戏本 i7/16G/512G','联想拯救者 Y7000 2021 游戏本 i7/16G/512G。成色 8 成新，功能完好无维修。原价 7299.00 元，现价 4200.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',4200.00,7299.00,8,0,'/upload/demo/item-03.png','["/upload/demo/item-03.png"]','["二手","校园","自提"]','北苑',0,'0',1180,98,4,NULL,DATE_SUB(NOW(), INTERVAL 22 DAY),'0',DATE_SUB(NOW(), INTERVAL 23 DAY),DATE_SUB(NOW(), INTERVAL 23 DAY)),
(1004,'CM202610091004',2,102,'华为 MatePad 11 平板 6+128 附原装键盘','华为 MatePad 11 平板 6+128 附原装键盘','华为 MatePad 11 平板 6+128 附原装键盘。成色 8 成新，功能完好无维修。原价 2799.00 元，现价 1500.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',1500.00,2799.00,8,1,'/upload/demo/item-04.png','["/upload/demo/item-04.png"]','["二手","校园","自提"]','教学楼',1,'0',502,41,4,NULL,DATE_SUB(NOW(), INTERVAL 21 DAY),'0',DATE_SUB(NOW(), INTERVAL 22 DAY),DATE_SUB(NOW(), INTERVAL 22 DAY)),
(1005,'CM202610101005',3,103,'罗技 MX Keys 无线键盘 全键位','罗技 MX Keys 无线键盘 全键位','罗技 MX Keys 无线键盘 全键位。成色 9 成新，功能完好无维修。原价 799.00 元，现价 380.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',380.00,799.00,9,2,'/upload/demo/item-05.png','["/upload/demo/item-05.png"]','["二手","校园","自提"]','东区宿舍',0,'0',431,35,4,NULL,DATE_SUB(NOW(), INTERVAL 20 DAY),'0',DATE_SUB(NOW(), INTERVAL 21 DAY),DATE_SUB(NOW(), INTERVAL 21 DAY)),
(1006,'CM202610111006',11,103,'索尼 WH-1000XM4 头戴降噪耳机','索尼 WH-1000XM4 头戴降噪耳机','索尼 WH-1000XM4 头戴降噪耳机。成色 9 成新，功能完好无维修。原价 1999.00 元，现价 900.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',900.00,1999.00,9,1,'/upload/demo/item-06.png','["/upload/demo/item-06.png"]','["二手","校园","自提"]','西区宿舍',1,'0',733,61,4,NULL,DATE_SUB(NOW(), INTERVAL 19 DAY),'0',DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1007,'CM202610121007',12,103,'Anker 65W 氮化镓充电器 三口','Anker 65W 氮化镓充电器 三口','Anker 65W 氮化镓充电器 三口。成色 9 成新，功能完好无维修。原价 149.00 元，现价 70.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',70.00,149.00,9,2,'/upload/demo/item-07.png','["/upload/demo/item-07.png"]','["二手","校园","自提"]','南苑',0,'0',268,22,4,NULL,DATE_SUB(NOW(), INTERVAL 18 DAY),'0',DATE_SUB(NOW(), INTERVAL 19 DAY),DATE_SUB(NOW(), INTERVAL 19 DAY)),
(1008,'CM202610131008',13,102,'iPad 2021 64G WiFi 版 送保护壳','iPad 2021 64G WiFi 版 送保护壳','iPad 2021 64G WiFi 版 送保护壳。成色 8 成新，功能完好无维修。原价 2499.00 元，现价 1600.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',1600.00,2499.00,8,1,'/upload/demo/item-08.png','["/upload/demo/item-08.png"]','["二手","校园","自提"]','北苑',1,'1',594,49,4,NULL,DATE_SUB(NOW(), INTERVAL 17 DAY),'0',DATE_SUB(NOW(), INTERVAL 18 DAY),DATE_SUB(NOW(), INTERVAL 18 DAY)),
(1009,'CM202610141009',2,201,'《高等数学》同济第七版 上下册 笔记齐全','《高等数学》同济第七版 上下册 笔记齐全','《高等数学》同济第七版 上下册 笔记齐全。成色 7 成新，有正常使用痕迹。原价 89.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,89.00,7,2,'/upload/demo/item-09.png','["/upload/demo/item-09.png"]','["二手","校园","自提"]','教学楼',0,'0',312,26,4,NULL,DATE_SUB(NOW(), INTERVAL 16 DAY),'0',DATE_SUB(NOW(), INTERVAL 17 DAY),DATE_SUB(NOW(), INTERVAL 17 DAY)),
(1010,'CM202610151010',3,201,'《数据结构》严蔚敏 第2版 无划线','《数据结构》严蔚敏 第2版 无划线','《数据结构》严蔚敏 第2版 无划线。成色 8 成新，功能完好无维修。原价 45.00 元，现价 20.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',20.00,45.00,8,2,'/upload/demo/item-10.png','["/upload/demo/item-10.png"]','["二手","校园","自提"]','东区宿舍',0,'0',190,15,4,NULL,DATE_SUB(NOW(), INTERVAL 15 DAY),'0',DATE_SUB(NOW(), INTERVAL 16 DAY),DATE_SUB(NOW(), INTERVAL 16 DAY)),
(1011,'CM202610151011',11,201,'《计算机网络》谢希仁 第8版','《计算机网络》谢希仁 第8版','《计算机网络》谢希仁 第8版。成色 8 成新，功能完好无维修。原价 49.00 元，现价 22.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',22.00,49.00,8,2,'/upload/demo/item-11.png','["/upload/demo/item-11.png"]','["二手","校园","自提"]','西区宿舍',0,'0',168,14,4,NULL,DATE_SUB(NOW(), INTERVAL 15 DAY),'0',DATE_SUB(NOW(), INTERVAL 16 DAY),DATE_SUB(NOW(), INTERVAL 16 DAY)),
(1012,'CM202610161012',12,201,'《算法导论》第三版 中文版 硬壳','《算法导论》第三版 中文版 硬壳','《算法导论》第三版 中文版 硬壳。成色 9 成新，功能完好无维修。原价 128.00 元，现价 60.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',60.00,128.00,9,1,'/upload/demo/item-12.png','["/upload/demo/item-12.png"]','["二手","校园","自提"]','南苑',1,'0',246,20,4,NULL,DATE_SUB(NOW(), INTERVAL 14 DAY),'0',DATE_SUB(NOW(), INTERVAL 15 DAY),DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1013,'CM202610161013',13,202,'考研英语一历年真题 2015-2025 全新未拆','考研英语一历年真题 2015-2025 全新未拆','考研英语一历年真题 2015-2025 全新未拆。成色 9 成新，功能完好无维修。原价 120.00 元，现价 45.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',45.00,120.00,9,2,'/upload/demo/item-01.png','["/upload/demo/item-01.png"]','["二手","校园","自提"]','北苑',0,'0',388,32,4,NULL,DATE_SUB(NOW(), INTERVAL 14 DAY),'0',DATE_SUB(NOW(), INTERVAL 15 DAY),DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1014,'CM202610171014',2,202,'张宇考研数学基础30讲 附笔记本','张宇考研数学基础30讲 附笔记本','张宇考研数学基础30讲 附笔记本。成色 8 成新，功能完好无维修。原价 89.00 元，现价 30.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',30.00,89.00,8,2,'/upload/demo/item-02.png','["/upload/demo/item-02.png"]','["二手","校园","自提"]','教学楼',0,'0',275,22,4,NULL,DATE_SUB(NOW(), INTERVAL 13 DAY),'0',DATE_SUB(NOW(), INTERVAL 14 DAY),DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1015,'CM202610181015',3,202,'肖秀荣考研政治1000题 未做题','肖秀荣考研政治1000题 未做题','肖秀荣考研政治1000题 未做题。成色 9 成新，功能完好无维修。原价 69.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,69.00,9,2,'/upload/demo/item-03.png','["/upload/demo/item-03.png"]','["二手","校园","自提"]','东区宿舍',0,'1',241,20,4,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),'0',DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1016,'CM202610181016',11,203,'《平凡的世界》全三册 经典版','《平凡的世界》全三册 经典版','《平凡的世界》全三册 经典版。成色 9 成新，功能完好无维修。原价 108.00 元，现价 35.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',35.00,108.00,9,1,'/upload/demo/item-04.png','["/upload/demo/item-04.png"]','["二手","校园","自提"]','西区宿舍',1,'0',156,13,4,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),'0',DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1017,'CM202610191017',12,203,'村上春树作品集 5本打包','村上春树作品集 5本打包','村上春树作品集 5本打包。成色 9 成新，功能完好无维修。原价 200.00 元，现价 60.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',60.00,200.00,9,1,'/upload/demo/item-05.png','["/upload/demo/item-05.png"]','["二手","校园","自提"]','南苑',1,'0',203,16,4,NULL,DATE_SUB(NOW(), INTERVAL 11 DAY),'0',DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1018,'CM202610191018',13,3,'小米 LED 护眼台灯 无极调光','小米 LED 护眼台灯 无极调光','小米 LED 护眼台灯 无极调光。成色 9 成新，功能完好无维修。原价 149.00 元，现价 55.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',55.00,149.00,9,2,'/upload/demo/item-06.png','["/upload/demo/item-06.png"]','["二手","校园","自提"]','北苑',0,'0',322,26,4,NULL,DATE_SUB(NOW(), INTERVAL 11 DAY),'0',DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1019,'CM202610201019',2,601,'宿舍折叠桌 可升降 免安装','宿舍折叠桌 可升降 免安装','宿舍折叠桌 可升降 免安装。成色 8 成新，功能完好无维修。原价 199.00 元，现价 80.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',80.00,199.00,8,0,'/upload/demo/item-07.png','["/upload/demo/item-07.png"]','["二手","校园","自提"]','教学楼',0,'0',410,34,4,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),'0',DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1020,'CM202610201020',3,602,'靠墙免打孔收纳架 四层','靠墙免打孔收纳架 四层','靠墙免打孔收纳架 四层。成色 9 成新，功能完好无维修。原价 79.00 元，现价 30.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',30.00,79.00,9,0,'/upload/demo/item-08.png','["/upload/demo/item-08.png"]','["二手","校园","自提"]','东区宿舍',0,'0',187,15,4,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),'0',DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1021,'CM202610211021',11,3,'电煮锅 1.5L 学生宿舍款 已擦净','电煮锅 1.5L 学生宿舍款 已擦净','电煮锅 1.5L 学生宿舍款 已擦净。成色 9 成新，功能完好无维修。原价 119.00 元，现价 45.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',45.00,119.00,9,2,'/upload/demo/item-09.png','["/upload/demo/item-09.png"]','["二手","校园","自提"]','西区宿舍',0,'0',356,29,4,NULL,DATE_SUB(NOW(), INTERVAL 9 DAY),'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1022,'CM202610211022',12,3,'加湿器 静音 3L 大容量','加湿器 静音 3L 大容量','加湿器 静音 3L 大容量。成色 9 成新，功能完好无维修。原价 139.00 元，现价 60.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',60.00,139.00,9,1,'/upload/demo/item-10.png','["/upload/demo/item-10.png"]','["二手","校园","自提"]','南苑',1,'1',228,19,4,NULL,DATE_SUB(NOW(), INTERVAL 9 DAY),'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1023,'CM202610221023',13,4,'迪卡侬羽毛球拍套装 双拍','迪卡侬羽毛球拍套装 双拍','迪卡侬羽毛球拍套装 双拍。成色 8 成新，功能完好无维修。原价 199.00 元，现价 90.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',90.00,199.00,8,2,'/upload/demo/item-11.png','["/upload/demo/item-11.png"]','["二手","校园","自提"]','北苑',0,'0',264,22,4,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),'0',DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1024,'CM202610221024',2,4,'山地自行车 26寸 21速 刚做过保养','山地自行车 26寸 21速 刚做过保养','山地自行车 26寸 21速 刚做过保养。成色 7 成新，有正常使用痕迹。原价 899.00 元，现价 320.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',320.00,899.00,7,0,'/upload/demo/item-12.png','["/upload/demo/item-12.png"]','["二手","校园","自提"]','教学楼',0,'0',512,42,4,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),'0',DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1025,'CM202610171025',3,4,'捷安特骑行头盔 L码','捷安特骑行头盔 L码','捷安特骑行头盔 L码。成色 9 成新，功能完好无维修。原价 199.00 元，现价 80.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',80.00,199.00,9,1,'/upload/demo/item-01.png','["/upload/demo/item-01.png"]','["二手","校园","自提"]','东区宿舍',1,'0',143,11,3,NULL,DATE_SUB(NOW(), INTERVAL 13 DAY),'0',DATE_SUB(NOW(), INTERVAL 14 DAY),DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1026,'CM202610181026',11,4,'瑜伽垫 加厚防滑 8mm','瑜伽垫 加厚防滑 8mm','瑜伽垫 加厚防滑 8mm。成色 9 成新，功能完好无维修。原价 79.00 元，现价 30.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',30.00,79.00,9,2,'/upload/demo/item-02.png','["/upload/demo/item-02.png"]','["二手","校园","自提"]','西区宿舍',0,'0',96,8,3,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),'0',DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1027,'CM202610191027',12,5,'兰蔻小黑瓶精华 30ml 专柜正品','兰蔻小黑瓶精华 30ml 专柜正品','兰蔻小黑瓶精华 30ml 专柜正品。成色 9 成新，功能完好无维修。原价 760.00 元，现价 260.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',260.00,760.00,9,1,'/upload/demo/item-03.png','["/upload/demo/item-03.png"]','["二手","校园","自提"]','南苑',1,'0',336,28,3,NULL,DATE_SUB(NOW(), INTERVAL 11 DAY),'0',DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1028,'CM202610201028',13,5,'飞利浦电动牙刷 附3个刷头','飞利浦电动牙刷 附3个刷头','飞利浦电动牙刷 附3个刷头。成色 9 成新，功能完好无维修。原价 399.00 元，现价 120.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',120.00,399.00,9,1,'/upload/demo/item-04.png','["/upload/demo/item-04.png"]','["二手","校园","自提"]','北苑',1,'0',208,17,3,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),'0',DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1029,'CM202610201029',2,601,'实木书桌 1.2m 桌面无划痕','实木书桌 1.2m 桌面无划痕','实木书桌 1.2m 桌面无划痕。成色 8 成新，功能完好无维修。原价 499.00 元，现价 150.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',150.00,499.00,8,0,'/upload/demo/item-05.png','["/upload/demo/item-05.png"]','["二手","校园","自提"]','教学楼',0,'1',254,21,3,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),'0',DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1030,'CM202610211030',3,602,'四层收纳柜 白色 九成新','四层收纳柜 白色 九成新','四层收纳柜 白色 九成新。成色 8 成新，功能完好无维修。原价 199.00 元，现价 70.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',70.00,199.00,8,0,'/upload/demo/item-06.png','["/upload/demo/item-06.png"]','["二手","校园","自提"]','东区宿舍',0,'0',171,14,3,NULL,DATE_SUB(NOW(), INTERVAL 9 DAY),'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1031,'CM202610151031',11,7,'尤克里里 23寸 初学者套装','尤克里里 23寸 初学者套装','尤克里里 23寸 初学者套装。成色 9 成新，功能完好无维修。原价 399.00 元，现价 130.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',130.00,399.00,9,1,'/upload/demo/item-07.png','["/upload/demo/item-07.png"]','["二手","校园","自提"]','西区宿舍',1,'0',289,24,2,NULL,DATE_SUB(NOW(), INTERVAL 15 DAY),'0',DATE_SUB(NOW(), INTERVAL 16 DAY),DATE_SUB(NOW(), INTERVAL 16 DAY)),
(1032,'CM202610161032',12,7,'民谣吉他 41寸 单板 送背包','民谣吉他 41寸 单板 送背包','民谣吉他 41寸 单板 送背包。成色 8 成新，功能完好无维修。原价 899.00 元，现价 380.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',380.00,899.00,8,0,'/upload/demo/item-08.png','["/upload/demo/item-08.png"]','["二手","校园","自提"]','南苑',0,'0',497,41,2,NULL,DATE_SUB(NOW(), INTERVAL 14 DAY),'0',DATE_SUB(NOW(), INTERVAL 15 DAY),DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1033,'CM202610171033',13,7,'卡西欧电子琴 61键 带琴架','卡西欧电子琴 61键 带琴架','卡西欧电子琴 61键 带琴架。成色 8 成新，功能完好无维修。原价 1299.00 元，现价 450.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',450.00,1299.00,8,0,'/upload/demo/item-09.png','["/upload/demo/item-09.png"]','["二手","校园","自提"]','北苑',0,'0',341,28,2,NULL,DATE_SUB(NOW(), INTERVAL 13 DAY),'0',DATE_SUB(NOW(), INTERVAL 14 DAY),DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1034,'CM202610181034',2,103,'机械键盘 84键 青轴 热插拔','机械键盘 84键 青轴 热插拔','机械键盘 84键 青轴 热插拔。成色 8 成新，功能完好无维修。原价 459.00 元，现价 180.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',180.00,459.00,8,1,'/upload/demo/item-10.png','["/upload/demo/item-10.png"]','["二手","校园","自提"]','教学楼',1,'0',463,38,2,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),'0',DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1035,'CM202610191035',3,601,'人体工学椅 网布透气 可调腰托','人体工学椅 网布透气 可调腰托','人体工学椅 网布透气 可调腰托。成色 8 成新，功能完好无维修。原价 699.00 元，现价 220.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',220.00,699.00,8,0,'/upload/demo/item-11.png','["/upload/demo/item-11.png"]','["二手","校园","自提"]','东区宿舍',0,'0',385,32,2,NULL,DATE_SUB(NOW(), INTERVAL 11 DAY),'0',DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1036,'CM202610171036',11,102,'Switch OLED 主机 港版 含底座','Switch OLED 主机 港版 含底座','Switch OLED 主机 港版 含底座。成色 8 成新，功能完好无维修。原价 2499.00 元，现价 1400.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',1400.00,2499.00,8,1,'/upload/demo/item-12.png','["/upload/demo/item-12.png"]','["二手","校园","自提"]','西区宿舍',1,'1',728,60,2,NULL,DATE_SUB(NOW(), INTERVAL 13 DAY),'0',DATE_SUB(NOW(), INTERVAL 14 DAY),DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1037,'CM202610181037',12,103,'AirPods Pro 2 充电盒 单卖','AirPods Pro 2 充电盒 单卖','AirPods Pro 2 充电盒 单卖。成色 9 成新，功能完好无维修。原价 379.00 元，现价 90.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',90.00,379.00,9,1,'/upload/demo/item-01.png','["/upload/demo/item-01.png"]','["二手","校园","自提"]','南苑',1,'0',217,18,2,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),'0',DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1038,'CM202610211038',13,102,'显示器 24寸 144Hz 电竞屏','显示器 24寸 144Hz 电竞屏','显示器 24寸 144Hz 电竞屏。成色 8 成新，功能完好无维修。原价 1099.00 元，现价 520.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',520.00,1099.00,8,0,'/upload/demo/item-02.png','["/upload/demo/item-02.png"]','["二手","校园","自提"]','北苑',0,'0',604,50,2,NULL,DATE_SUB(NOW(), INTERVAL 9 DAY),'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1039,'CM202610221039',2,602,'台灯 + 书架组合 宿舍适用','台灯 + 书架组合 宿舍适用','台灯 + 书架组合 宿舍适用。成色 9 成新，功能完好无维修。原价 169.00 元，现价 60.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',60.00,169.00,9,0,'/upload/demo/item-03.png','["/upload/demo/item-03.png"]','["二手","校园","自提"]','教学楼',0,'0',132,11,2,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),'0',DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1040,'CM202610221040',3,202,'四六级真题全套 近十年','四六级真题全套 近十年','四六级真题全套 近十年。成色 9 成新，功能完好无维修。原价 60.00 元，现价 20.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',20.00,60.00,9,2,'/upload/demo/item-04.png','["/upload/demo/item-04.png"]','["二手","校园","自提"]','东区宿舍',0,'0',214,17,2,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),'0',DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1041,'CM202610231041',11,3,'冬季帽子围巾套装 全新','冬季帽子围巾套装 全新','冬季帽子围巾套装 全新。成色 10 成新，功能完好无维修。原价 89.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,89.00,10,2,'/upload/demo/item-05.png','["/upload/demo/item-05.png"]','["二手","校园","自提"]','西区宿舍',0,'0',88,7,2,NULL,DATE_SUB(NOW(), INTERVAL 7 DAY),'0',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1042,'CM202610231042',12,203,'文学名著套装 6本','文学名著套装 6本','文学名著套装 6本。成色 9 成新，功能完好无维修。原价 90.00 元，现价 30.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',30.00,90.00,9,1,'/upload/demo/item-06.png','["/upload/demo/item-06.png"]','["二手","校园","自提"]','南苑',1,'0',119,9,2,NULL,DATE_SUB(NOW(), INTERVAL 7 DAY),'0',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1043,'CM202610241043',13,3,'洗衣液 4kg 未开封','洗衣液 4kg 未开封','洗衣液 4kg 未开封。成色 10 成新，功能完好无维修。原价 59.00 元，现价 30.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',30.00,59.00,10,2,'/upload/demo/item-07.png','["/upload/demo/item-07.png"]','["二手","校园","自提"]','北苑',0,'1',152,12,2,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),'0',DATE_SUB(NOW(), INTERVAL 7 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1044,'CM202610241044',2,3,'自动折叠雨伞 加固防风','自动折叠雨伞 加固防风','自动折叠雨伞 加固防风。成色 9 成新，功能完好无维修。原价 59.00 元，现价 20.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',20.00,59.00,9,2,'/upload/demo/item-08.png','["/upload/demo/item-08.png"]','["二手","校园","自提"]','教学楼',0,'0',76,6,2,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),'0',DATE_SUB(NOW(), INTERVAL 7 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1045,'CM202610251045',3,103,'U盘 64G USB3.0','U盘 64G USB3.0','U盘 64G USB3.0。成色 9 成新，功能完好无维修。原价 69.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,69.00,9,2,'/upload/demo/item-09.png','["/upload/demo/item-09.png"]','["二手","校园","自提"]','东区宿舍',0,'0',143,11,2,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),'0',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1046,'CM202610251046',11,103,'充电宝 20000mAh 双向快充','充电宝 20000mAh 双向快充','充电宝 20000mAh 双向快充。成色 9 成新，功能完好无维修。原价 149.00 元，现价 60.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',60.00,149.00,9,2,'/upload/demo/item-10.png','["/upload/demo/item-10.png"]','["二手","校园","自提"]','西区宿舍',0,'0',331,27,2,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),'0',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1047,'CM202610261047',12,3,'桌面吸尘器 静音','桌面吸尘器 静音','桌面吸尘器 静音。成色 9 成新，功能完好无维修。原价 99.00 元，现价 35.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',35.00,99.00,9,1,'/upload/demo/item-11.png','["/upload/demo/item-11.png"]','["二手","校园","自提"]','南苑',1,'0',97,8,2,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),'0',DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1048,'CM202610261048',13,103,'磁吸手机支架 车载/桌面两用','磁吸手机支架 车载/桌面两用','磁吸手机支架 车载/桌面两用。成色 9 成新，功能完好无维修。原价 39.00 元，现价 15.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',15.00,39.00,9,1,'/upload/demo/item-12.png','["/upload/demo/item-12.png"]','["二手","校园","自提"]','北苑',1,'0',64,5,2,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),'0',DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1049,'CM202610271049',2,201,'编程书籍三本打包（Java/算法/MySQL）','编程书籍三本打包（Java/算法/MySQL）','编程书籍三本打包（Java/算法/MySQL）。成色 8 成新，功能完好无维修。原价 150.00 元，现价 45.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',45.00,150.00,8,2,'/upload/demo/item-01.png','["/upload/demo/item-01.png"]','["二手","校园","自提"]','教学楼',0,'0',178,14,2,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),'0',DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1050,'CM202610271050',3,103,'显示器支架 单屏 气压式','显示器支架 单屏 气压式','显示器支架 单屏 气压式。成色 9 成新，功能完好无维修。原价 159.00 元，现价 70.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',70.00,159.00,9,1,'/upload/demo/item-02.png','["/upload/demo/item-02.png"]','["二手","校园","自提"]','东区宿舍',1,'1',126,10,2,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),'0',DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1051,'CM202610281051',11,103,'蓝牙音箱 便携 防水','蓝牙音箱 便携 防水','蓝牙音箱 便携 防水。成色 9 成新，功能完好无维修。原价 199.00 元，现价 80.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',80.00,199.00,9,1,'/upload/demo/item-03.png','["/upload/demo/item-03.png"]','["二手","校园","自提"]','西区宿舍',1,'0',41,3,1,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),'0',DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1052,'CM202610281052',12,8,'装饰挂画 三幅 宿舍改造','装饰挂画 三幅 宿舍改造','装饰挂画 三幅 宿舍改造。成色 9 成新，功能完好无维修。原价 39.00 元，现价 12.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',12.00,39.00,9,2,'/upload/demo/item-04.png','["/upload/demo/item-04.png"]','["二手","校园","自提"]','南苑',0,'0',33,2,1,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),'0',DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1053,'CM202610291053',13,3,'保温杯 500ml 316不锈钢','保温杯 500ml 316不锈钢','保温杯 500ml 316不锈钢。成色 9 成新，功能完好无维修。原价 99.00 元，现价 35.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',35.00,99.00,9,2,'/upload/demo/item-05.png','["/upload/demo/item-05.png"]','["二手","校园","自提"]','北苑',0,'0',52,4,1,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),'0',DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1054,'CM202610291054',2,4,'斯伯丁篮球 7号 室内外','斯伯丁篮球 7号 室内外','斯伯丁篮球 7号 室内外。成色 8 成新，功能完好无维修。原价 169.00 元，现价 70.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',70.00,169.00,8,0,'/upload/demo/item-06.png','["/upload/demo/item-06.png"]','["二手","校园","自提"]','教学楼',0,'0',68,5,1,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),'0',DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1055,'CM202610201055',3,602,'床上折叠桌 小号','床上折叠桌 小号','床上折叠桌 小号。成色 8 成新，功能完好无维修。原价 99.00 元，现价 40.00 元。交易方式：校内自提。学号认证用户，支持当面验货，非诚勿扰。',40.00,99.00,8,0,'/upload/demo/item-07.png','["/upload/demo/item-07.png"]','["二手","校园","自提"]','东区宿舍',0,'0',84,7,5,'卖家主动下架',DATE_SUB(NOW(), INTERVAL 10 DAY),'0',DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1056,'CM202610211056',11,103,'键鼠套装 有线 办公','键鼠套装 有线 办公','键鼠套装 有线 办公。成色 9 成新，功能完好无维修。原价 129.00 元，现价 55.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',55.00,129.00,9,1,'/upload/demo/item-08.png','["/upload/demo/item-08.png"]','["二手","校园","自提"]','西区宿舍',1,'0',71,5,5,'卖家主动下架',DATE_SUB(NOW(), INTERVAL 9 DAY),'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1057,'CM202610221057',12,201,'计算机组成原理复习资料 手写笔记','计算机组成原理复习资料 手写笔记','计算机组成原理复习资料 手写笔记。成色 8 成新，功能完好无维修。原价 65.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,65.00,8,2,'/upload/demo/item-09.png','["/upload/demo/item-09.png"]','["二手","校园","自提"]','南苑',0,'1',93,7,5,'卖家主动下架',DATE_SUB(NOW(), INTERVAL 8 DAY),'0',DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1058,'CM202610231058',13,203,'英文原版小说 3本 打包','英文原版小说 3本 打包','英文原版小说 3本 打包。成色 9 成新，功能完好无维修。原价 150.00 元，现价 45.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',45.00,150.00,9,1,'/upload/demo/item-10.png','["/upload/demo/item-10.png"]','["二手","校园","自提"]','北苑',1,'0',57,4,6,'标题含违规词，请修改后重新提交',DATE_SUB(NOW(), INTERVAL 7 DAY),'0',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1059,'CM202610241059',2,3,'静音小风扇 USB 充电','静音小风扇 USB 充电','静音小风扇 USB 充电。成色 9 成新，功能完好无维修。原价 69.00 元，现价 25.00 元。交易方式：自提/快递均可。学号认证用户，支持当面验货，非诚勿扰。',25.00,69.00,9,2,'/upload/demo/item-11.png','["/upload/demo/item-11.png"]','["二手","校园","自提"]','教学楼',0,'0',61,5,6,'标题含违规词，请修改后重新提交',DATE_SUB(NOW(), INTERVAL 6 DAY),'0',DATE_SUB(NOW(), INTERVAL 7 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1060,'CM202610301060',3,102,'二手显示器 27寸 2K 待补图','二手显示器 27寸 2K 待补图','二手显示器 27寸 2K 待补图。成色 8 成新，功能完好无维修。原价 1299.00 元，现价 400.00 元。交易方式：可快递。学号认证用户，支持当面验货，非诚勿扰。',400.00,1299.00,8,1,'/upload/demo/item-12.png','["/upload/demo/item-12.png"]','["二手","校园","自提"]','东区宿舍',1,'0',0,0,0,NULL,NULL,'0',DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 收藏数按 biz_favorite 真实行数回写

-- 2. 订单 37 条
INSERT INTO biz_order (order_id, order_no, item_id, buyer_id, seller_id, amount, status, pay_type,
  trade_place, buyer_remark, cancel_reason, create_time, pay_time, finish_time, cancel_time, update_time) VALUES
(2001,'CM202610012001',1001,1,11,2400.00,2,1,'图书馆门口',NULL,NULL,DATE_SUB(NOW(), INTERVAL 26 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 26 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 26 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 26 DAY)),
(2002,'CM202610022002',1002,2,12,2100.00,2,1,'南苑食堂',NULL,NULL,DATE_SUB(NOW(), INTERVAL 25 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 25 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 25 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 25 DAY)),
(2003,'CM202610032003',1003,3,13,4200.00,2,1,'西区快递点',NULL,NULL,DATE_SUB(NOW(), INTERVAL 24 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 24 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 24 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 24 DAY)),
(2004,'CM202610042004',1004,11,2,1500.00,2,1,'教学楼 A 座',NULL,NULL,DATE_SUB(NOW(), INTERVAL 23 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 23 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 23 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 23 DAY)),
(2005,'CM202610052005',1005,12,3,380.00,2,1,'东区 3 号宿舍楼下',NULL,NULL,DATE_SUB(NOW(), INTERVAL 22 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 22 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 22 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 22 DAY)),
(2006,'CM202610062006',1006,13,11,900.00,2,1,'图书馆门口',NULL,NULL,DATE_SUB(NOW(), INTERVAL 21 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 21 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 21 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 21 DAY)),
(2007,'CM202610072007',1007,1,12,70.00,2,1,'南苑食堂',NULL,NULL,DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2008,'CM202610082008',1008,2,13,1600.00,2,1,'西区快递点',NULL,NULL,DATE_SUB(NOW(), INTERVAL 19 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 19 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 19 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 19 DAY)),
(2009,'CM202610092009',1009,3,2,25.00,2,1,'教学楼 A 座',NULL,NULL,DATE_SUB(NOW(), INTERVAL 18 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 18 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 18 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 18 DAY)),
(2010,'CM202610102010',1010,11,3,20.00,2,1,'东区 3 号宿舍楼下',NULL,NULL,DATE_SUB(NOW(), INTERVAL 17 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 17 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 17 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 17 DAY)),
(2011,'CM202610112011',1011,12,11,22.00,2,1,'图书馆门口',NULL,NULL,DATE_SUB(NOW(), INTERVAL 16 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 16 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 16 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(2012,'CM202610122012',1012,13,12,60.00,2,1,'南苑食堂',NULL,NULL,DATE_SUB(NOW(), INTERVAL 15 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 15 DAY)),
(2013,'CM202610132013',1013,1,13,45.00,2,1,'西区快递点',NULL,NULL,DATE_SUB(NOW(), INTERVAL 14 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 14 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 14 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 14 DAY)),
(2014,'CM202610142014',1014,3,2,30.00,2,1,'教学楼 A 座',NULL,NULL,DATE_SUB(NOW(), INTERVAL 13 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 13 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 13 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 13 DAY)),
(2015,'CM202610152015',1015,11,3,25.00,2,1,'东区 3 号宿舍楼下',NULL,NULL,DATE_SUB(NOW(), INTERVAL 12 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 12 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 12 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2016,'CM202610162016',1016,12,11,35.00,2,1,'图书馆门口',NULL,NULL,DATE_SUB(NOW(), INTERVAL 11 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 11 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 11 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 11 DAY)),
(2017,'CM202610172017',1017,13,12,60.00,2,1,'南苑食堂',NULL,NULL,DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2018,'CM202610182018',1018,1,13,55.00,2,1,'西区快递点',NULL,NULL,DATE_SUB(NOW(), INTERVAL 9 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 9 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 9 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 9 DAY)),
(2019,'CM202610192019',1019,1,2,80.00,2,1,'教学楼 A 座',NULL,NULL,DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2020,'CM202610202020',1020,2,3,30.00,2,1,'东区 3 号宿舍楼下',NULL,NULL,DATE_SUB(NOW(), INTERVAL 7 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 7 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 7 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2021,'CM202610212021',1021,3,11,45.00,2,1,'图书馆门口',NULL,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2022,'CM202610222022',1022,11,12,60.00,2,1,'南苑食堂',NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2023,'CM202610232023',1023,12,13,90.00,2,1,'西区快递点',NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2024,'CM202610242024',1024,13,2,320.00,2,1,'教学楼 A 座',NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 2 HOUR),DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2025,'CM202610242025',1025,1,3,80.00,1,1,'东区 3 号宿舍楼下','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 2 HOUR),NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2026,'CM202610242026',1026,2,11,30.00,1,1,'图书馆门口','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 2 HOUR),NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2027,'CM202610242027',1027,3,12,260.00,1,1,'南苑食堂','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 2 HOUR),NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2028,'CM202610262028',1028,11,13,120.00,0,1,'西区快递点','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2029,'CM202610262029',1029,12,2,150.00,0,1,'教学楼 A 座','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2030,'CM202610262030',1030,13,3,70.00,0,1,'东区 3 号宿舍楼下','请尽快联系',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2031,'CM202610192031',1031,1,11,130.00,3,1,'图书馆门口',NULL,'买家临时有事',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),NULL,DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2032,'CM202610192032',1032,2,12,380.00,3,1,'南苑食堂',NULL,'买家临时有事',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),NULL,DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2033,'CM202610192033',1033,3,13,450.00,3,1,'西区快递点',NULL,'买家临时有事',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),NULL,DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2034,'CM202610192034',1034,11,2,180.00,3,1,'教学楼 A 座',NULL,'买家临时有事',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),NULL,DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2035,'CM202610192035',1035,12,3,220.00,3,1,'东区 3 号宿舍楼下',NULL,'买家临时有事',DATE_SUB(NOW(), INTERVAL 8 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 2 HOUR),NULL,DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2036,'CM202610212036',1036,13,11,1400.00,5,1,'图书馆门口',NULL,'商品与描述不符',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 2 HOUR),NULL,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2037,'CM202610212037',1037,1,12,90.00,5,1,'南苑食堂',NULL,'商品与描述不符',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 2 HOUR),NULL,NULL,DATE_SUB(NOW(), INTERVAL 6 DAY));

-- 3. 收藏（真实行数，供收藏榜/个性化推荐使用）
INSERT INTO biz_favorite (favorite_id, user_id, item_id, create_time) VALUES
(4001,11,1050,DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4002,11,1014,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4003,11,1049,DATE_SUB(NOW(), INTERVAL 17 DAY)),
(4004,11,1033,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4005,11,1042,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4006,11,1015,DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4007,11,1039,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4008,11,1024,DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4009,11,1029,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4010,11,1007,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4011,11,1032,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4012,11,1027,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4013,12,1039,DATE_SUB(NOW(), INTERVAL 19 DAY)),
(4014,12,1018,DATE_SUB(NOW(), INTERVAL 17 DAY)),
(4015,12,1041,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4016,12,1008,DATE_SUB(NOW(), INTERVAL 17 DAY)),
(4017,12,1028,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4018,12,1020,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4019,12,1044,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4020,12,1040,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4021,12,1019,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4022,12,1015,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4023,12,1011,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4024,12,1004,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4025,13,1017,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4026,13,1016,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4027,13,1025,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4028,13,1044,DATE_SUB(NOW(), INTERVAL 13 DAY)),
(4029,13,1026,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4030,13,1050,DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4031,13,1034,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4032,13,1009,DATE_SUB(NOW(), INTERVAL 0 DAY)),
(4033,13,1036,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4034,13,1030,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4035,13,1032,DATE_SUB(NOW(), INTERVAL 11 DAY)),
(4036,13,1011,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4037,1,1024,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4038,1,1043,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4039,1,1004,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4040,1,1020,DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4041,1,1037,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4042,1,1032,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4043,1,1019,DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4044,1,1048,DATE_SUB(NOW(), INTERVAL 13 DAY)),
(4045,1,1017,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4046,1,1025,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4047,1,1039,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4048,1,1014,DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4049,2,1012,DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4050,2,1025,DATE_SUB(NOW(), INTERVAL 0 DAY)),
(4051,2,1042,DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4052,2,1017,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4053,2,1016,DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4054,2,1022,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4055,2,1032,DATE_SUB(NOW(), INTERVAL 15 DAY)),
(4056,2,1001,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4057,2,1045,DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4058,2,1015,DATE_SUB(NOW(), INTERVAL 11 DAY)),
(4059,2,1011,DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4060,2,1040,DATE_SUB(NOW(), INTERVAL 9 DAY));

UPDATE biz_item i SET i.favorite_count = (SELECT COUNT(*) FROM biz_favorite f WHERE f.item_id = i.item_id)
WHERE i.item_id BETWEEN 1001 AND 1060;

-- 4. 评价（已完成订单前 18 笔）
INSERT INTO biz_comment (comment_id, order_id, item_id, from_user_id, to_user_id, score, content, images, reply_content, status, create_time) VALUES
(3001,2001,1001,1,11,5,'东西很新，价格也实在，宿舍楼下自提非常方便。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3002,2002,1002,2,12,5,'包装保护得很好，收到就能用，推荐！','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 19 DAY)),
(3003,2003,1003,3,13,4,'有一点点使用痕迹但完全不影响，性价比高。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 18 DAY)),
(3004,2004,1004,11,2,5,'卖家很耐心，还送了配件，五星好评。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 17 DAY)),
(3005,2005,1005,12,3,5,'比想象中新，价格比新买的便宜一半，很值。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 16 DAY)),
(3006,2006,1006,13,11,4,'成色和描述一致，学长人很好，约在图书馆当面验货，交易很快。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 15 DAY)),
(3007,2007,1007,1,12,5,'东西很新，价格也实在，宿舍楼下自提非常方便。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 14 DAY)),
(3008,2008,1008,2,13,5,'包装保护得很好，收到就能用，推荐！','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3009,2009,1009,3,2,4,'有一点点使用痕迹但完全不影响，性价比高。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 12 DAY)),
(3010,2010,1010,11,3,5,'卖家很耐心，还送了配件，五星好评。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 11 DAY)),
(3011,2011,1011,12,11,5,'比想象中新，价格比新买的便宜一半，很值。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3012,2012,1012,13,12,4,'成色和描述一致，学长人很好，约在图书馆当面验货，交易很快。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3013,2013,1013,1,13,5,'东西很新，价格也实在，宿舍楼下自提非常方便。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3014,2014,1014,3,2,5,'包装保护得很好，收到就能用，推荐！','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3015,2015,1015,11,3,4,'有一点点使用痕迹但完全不影响，性价比高。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3016,2016,1016,12,11,5,'卖家很耐心，还送了配件，五星好评。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3017,2017,1017,13,12,5,'比想象中新，价格比新买的便宜一半，很值。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3018,2018,1018,1,13,4,'成色和描述一致，学长人很好，约在图书馆当面验货，交易很快。','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 5. 举报（后台待处理演示）
INSERT INTO biz_report (report_id, item_id, reporter_id, reason_type, content, images, status, handle_user_id, handle_remark, handle_time, create_time) VALUES
(5001,1058,11,'违禁物品','疑似售卖盗版书籍',NULL,0,NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5002,1059,12,'虚假描述','图片与实物不符，标价虚高',NULL,0,NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5003,1036,13,'价格欺诈','同一商品多次改价',NULL,1,2,'已核实，提醒卖家规范改价',DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5004,1041,1,'骚扰信息','联系方式为代购广告',NULL,2,2,'举报不成立，已驳回',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY));

-- 6. 站内通知
INSERT INTO biz_message (message_id, user_id, msg_type, title, content, biz_id, is_read, create_time) VALUES
(6001,11,3,'您发布的商品已通过审核','《iPhone 12 128G 国行 95新》已上架，买家可浏览下单。',1001,'1',DATE_SUB(NOW(), INTERVAL 26 DAY)),
(6002,11,2,'您有一笔新订单','买家已下单并支付，请尽快联系买家约定交付时间。',2001,'0',DATE_SUB(NOW(), INTERVAL 20 DAY)),
(6003,12,3,'商品审核未通过','《英文原版小说 3本 打包》因标题含违规词被下架，请修改后重新提交。',1058,'0',DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6004,13,2,'订单已完成','买家已确认收货，交易金额 90.00 元已入账信用记录。',2013,'1',DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6005,11,1,'信用分提升提醒','您的信用分提升至 98 分，可享受优先推荐曝光。',NULL,'0',DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6006,12,1,'校园易站使用小贴士','发布商品时上传真实图片和清晰成色描述，成交率提升 3 倍。',NULL,'0',DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 7. 公告
INSERT INTO biz_notice (notice_id, title, content, notice_type, status, publish_time, create_by, create_time) VALUES
(1,'校园易站正式上线公告','校园易站 CampusMart 已上线，支持商品发布、AI 文案生成、校内自提交易。首周发布商品免审核（人工抽检）。',1,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2,'交易安全提醒：请勿脱离平台交易','为保障双方权益，请使用平台内置订单流程，勿加微信私下转账。遇可疑情况可在商品页举报，运营会在 24 小时内处理。',2,'0',DATE_SUB(NOW(), INTERVAL 9 DAY),1,DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3,'毕业季闲置清仓活动开始','12 月毕业季清仓专区开启，教材类商品佣金全免，优质商品进入首页推荐位。活动时间：12.01 - 12.31。',1,'0',DATE_SUB(NOW(), INTERVAL 2 DAY),1,DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 8. 轮播图
INSERT INTO biz_banner (banner_id, title, image_url, link_url, sort, status, start_time, end_time, create_time) VALUES
(1,'让闲置流动起来','/upload/demo/banner-01.png','/pages/category/category',1,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_ADD(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2,'AI 一键发布','/upload/demo/banner-02.png','/pages/item/publish',2,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_ADD(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3,'毕业季清仓专区','/upload/demo/banner-03.png','search:教材',3,'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY));

-- 9. AI 调用日志（近 7 天，供 AI 统计面板）
INSERT INTO ai_call_log (id, user_id, scene, template_code, provider, model, prompt_tokens, completion_tokens, total_tokens, cost_ms, status, error_msg, create_time) VALUES
(7001,12,'price','ITEM_PRICE','remote','deepseek-chat',589,309,898,3172,'0',NULL,DATE_SUB(NOW(), INTERVAL 13 HOUR)),
(7002,13,'audit','ITEM_AUDIT','rule','template',213,457,670,755,'0',NULL,DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(7003,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',579,447,1026,926,'0',NULL,DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(7004,2,'audit','ITEM_AUDIT','rule','template',136,165,301,198,'0',NULL,DATE_SUB(NOW(), INTERVAL 7 HOUR)),
(7005,11,'copywrite','ITEM_COPYWRITE','mock','template',223,89,312,1026,'0',NULL,DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(7006,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',354,465,819,514,'0',NULL,DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(7007,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',130,434,564,741,'1','连接超时，已降级',DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(7008,1,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',132,443,575,1620,'1','连接超时，已降级',DATE_SUB(NOW(), INTERVAL 17 HOUR)),
(7009,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',480,273,753,2632,'0',NULL,DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(7010,11,'audit','ITEM_AUDIT','rule','template',396,440,836,579,'0',NULL,DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(7011,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',555,351,906,3127,'0',NULL,DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(7012,13,'price','ITEM_PRICE','remote','deepseek-chat',517,318,835,2468,'0',NULL,DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(7013,1,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',614,200,814,957,'0',NULL,DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(7014,2,'audit','ITEM_AUDIT','rule','template',535,286,821,760,'0',NULL,DATE_SUB(NOW(), INTERVAL 31 HOUR)),
(7015,11,'copywrite','ITEM_COPYWRITE','mock','template',485,130,615,2547,'0',NULL,DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(7016,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',359,105,464,902,'0',NULL,DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(7017,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',427,285,712,3238,'0',NULL,DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(7018,1,'audit','ITEM_AUDIT','rule','template',186,262,448,271,'1','连接超时，已降级',DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(7019,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',468,194,662,1734,'0',NULL,DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(7020,11,'audit','ITEM_AUDIT','rule','template',412,103,515,620,'0',NULL,DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(7021,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',348,344,692,1490,'0',NULL,DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(7022,13,'price','ITEM_PRICE','remote','deepseek-chat',431,176,607,3587,'0',NULL,DATE_SUB(NOW(), INTERVAL 53 HOUR)),
(7023,1,'audit','ITEM_AUDIT','rule','template',200,254,454,740,'0',NULL,DATE_SUB(NOW(), INTERVAL 54 HOUR)),
(7024,2,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',508,111,619,2863,'0',NULL,DATE_SUB(NOW(), INTERVAL 62 HOUR)),
(7025,11,'copywrite','ITEM_COPYWRITE','mock','template',244,362,606,2125,'0',NULL,DATE_SUB(NOW(), INTERVAL 54 HOUR)),
(7026,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',257,352,609,3504,'0',NULL,DATE_SUB(NOW(), INTERVAL 49 HOUR)),
(7027,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',237,351,588,3203,'0',NULL,DATE_SUB(NOW(), INTERVAL 65 HOUR)),
(7028,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',255,210,465,3340,'0',NULL,DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(7029,2,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',513,266,779,2569,'0',NULL,DATE_SUB(NOW(), INTERVAL 54 HOUR)),
(7030,11,'chat','CUSTOMER_SERVICE','mock','template',404,224,628,3095,'0',NULL,DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(7031,12,'audit','ITEM_AUDIT','rule','template',472,358,830,637,'0',NULL,DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(7032,13,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',482,286,768,2419,'0',NULL,DATE_SUB(NOW(), INTERVAL 55 HOUR)),
(7033,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',289,127,416,1126,'0',NULL,DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(7034,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',144,252,396,431,'0',NULL,DATE_SUB(NOW(), INTERVAL 88 HOUR)),
(7035,11,'chat','CUSTOMER_SERVICE','mock','template',123,339,462,1680,'0',NULL,DATE_SUB(NOW(), INTERVAL 91 HOUR)),
(7036,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',547,308,855,3161,'0',NULL,DATE_SUB(NOW(), INTERVAL 80 HOUR)),
(7037,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',610,302,912,1115,'0',NULL,DATE_SUB(NOW(), INTERVAL 76 HOUR)),
(7038,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',306,455,761,1318,'0',NULL,DATE_SUB(NOW(), INTERVAL 74 HOUR)),
(7039,2,'audit','ITEM_AUDIT','rule','template',139,322,461,332,'0',NULL,DATE_SUB(NOW(), INTERVAL 84 HOUR)),
(7040,11,'chat','CUSTOMER_SERVICE','mock','template',540,195,735,3007,'0',NULL,DATE_SUB(NOW(), INTERVAL 86 HOUR)),
(7041,12,'audit','ITEM_AUDIT','rule','template',185,292,477,684,'0',NULL,DATE_SUB(NOW(), INTERVAL 74 HOUR)),
(7042,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',412,147,559,1911,'0',NULL,DATE_SUB(NOW(), INTERVAL 89 HOUR)),
(7043,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',202,97,299,2894,'0',NULL,DATE_SUB(NOW(), INTERVAL 105 HOUR)),
(7044,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',363,425,788,2271,'0',NULL,DATE_SUB(NOW(), INTERVAL 100 HOUR)),
(7045,11,'copywrite','ITEM_COPYWRITE','mock','template',133,179,312,1297,'0',NULL,DATE_SUB(NOW(), INTERVAL 105 HOUR)),
(7046,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',302,175,477,455,'0',NULL,DATE_SUB(NOW(), INTERVAL 114 HOUR)),
(7047,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',506,471,977,820,'0',NULL,DATE_SUB(NOW(), INTERVAL 113 HOUR)),
(7048,1,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',214,374,588,754,'0',NULL,DATE_SUB(NOW(), INTERVAL 115 HOUR)),
(7049,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',139,407,546,1191,'0',NULL,DATE_SUB(NOW(), INTERVAL 109 HOUR)),
(7050,11,'copywrite','ITEM_COPYWRITE','mock','template',545,253,798,455,'0',NULL,DATE_SUB(NOW(), INTERVAL 101 HOUR)),
(7051,12,'price','ITEM_PRICE','remote','deepseek-chat',377,232,609,467,'0',NULL,DATE_SUB(NOW(), INTERVAL 98 HOUR)),
(7052,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',404,194,598,3322,'0',NULL,DATE_SUB(NOW(), INTERVAL 109 HOUR)),
(7053,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',240,152,392,516,'0',NULL,DATE_SUB(NOW(), INTERVAL 113 HOUR)),
(7054,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',336,235,571,2358,'1','连接超时，已降级',DATE_SUB(NOW(), INTERVAL 108 HOUR)),
(7055,11,'chat','CUSTOMER_SERVICE','mock','template',244,157,401,3543,'0',NULL,DATE_SUB(NOW(), INTERVAL 123 HOUR)),
(7056,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',245,480,725,2507,'0',NULL,DATE_SUB(NOW(), INTERVAL 139 HOUR)),
(7057,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',468,402,870,2510,'0',NULL,DATE_SUB(NOW(), INTERVAL 127 HOUR)),
(7058,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',279,431,710,2438,'0',NULL,DATE_SUB(NOW(), INTERVAL 132 HOUR)),
(7059,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',404,295,699,1833,'0',NULL,DATE_SUB(NOW(), INTERVAL 133 HOUR)),
(7060,11,'chat','CUSTOMER_SERVICE','mock','template',234,425,659,648,'0',NULL,DATE_SUB(NOW(), INTERVAL 133 HOUR)),
(7061,12,'audit','ITEM_AUDIT','rule','template',271,287,558,686,'0',NULL,DATE_SUB(NOW(), INTERVAL 125 HOUR)),
(7062,13,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',466,354,820,3052,'0',NULL,DATE_SUB(NOW(), INTERVAL 128 HOUR)),
(7063,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',394,122,516,984,'0',NULL,DATE_SUB(NOW(), INTERVAL 132 HOUR)),
(7064,2,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',597,322,919,3117,'0',NULL,DATE_SUB(NOW(), INTERVAL 145 HOUR)),
(7065,11,'copywrite','ITEM_COPYWRITE','mock','template',388,350,738,1222,'0',NULL,DATE_SUB(NOW(), INTERVAL 148 HOUR)),
(7066,12,'audit','ITEM_AUDIT','rule','template',521,230,751,238,'0',NULL,DATE_SUB(NOW(), INTERVAL 163 HOUR)),
(7067,13,'audit','ITEM_AUDIT','rule','template',449,111,560,498,'0',NULL,DATE_SUB(NOW(), INTERVAL 149 HOUR)),
(7068,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',223,384,607,1275,'0',NULL,DATE_SUB(NOW(), INTERVAL 144 HOUR)),
(7069,2,'audit','ITEM_AUDIT','rule','template',185,364,549,724,'0',NULL,DATE_SUB(NOW(), INTERVAL 154 HOUR)),
(7070,11,'copywrite','ITEM_COPYWRITE','mock','template',299,247,546,2053,'0',NULL,DATE_SUB(NOW(), INTERVAL 160 HOUR)),
(7071,12,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',124,418,542,1651,'0',NULL,DATE_SUB(NOW(), INTERVAL 147 HOUR)),
(7072,13,'chat','CUSTOMER_SERVICE','remote','deepseek-chat',161,166,327,3313,'0',NULL,DATE_SUB(NOW(), INTERVAL 146 HOUR)),
(7073,1,'copywrite','ITEM_COPYWRITE','remote','deepseek-chat',280,338,618,1287,'0',NULL,DATE_SUB(NOW(), INTERVAL 164 HOUR)),
(7074,2,'audit','ITEM_AUDIT','rule','template',139,479,618,498,'0',NULL,DATE_SUB(NOW(), INTERVAL 150 HOUR)),
(7075,11,'copywrite','ITEM_COPYWRITE','mock','template',392,286,678,2278,'0',NULL,DATE_SUB(NOW(), INTERVAL 145 HOUR));

-- 校验
SELECT 'biz_item' t, COUNT(*) c FROM biz_item WHERE item_id BETWEEN 1001 AND 1060
UNION ALL SELECT 'biz_order', COUNT(*) FROM biz_order WHERE order_id BETWEEN 2001 AND 2100
UNION ALL SELECT 'biz_favorite', COUNT(*) FROM biz_favorite WHERE favorite_id BETWEEN 4001 AND 5000
UNION ALL SELECT 'biz_comment', COUNT(*) FROM biz_comment WHERE comment_id BETWEEN 3001 AND 3200
UNION ALL SELECT 'biz_report', COUNT(*) FROM biz_report
UNION ALL SELECT 'biz_message', COUNT(*) FROM biz_message
UNION ALL SELECT 'ai_call_log', COUNT(*) FROM ai_call_log;
