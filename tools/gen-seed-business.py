# -*- coding: utf-8 -*-
"""生成 campus-mart/sql/02-seed-business.sql（业务演示数据，时间相对 NOW()，可重复执行）"""
import os
import random

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "sql", "02-seed-business.sql")
random.seed(20261027)

# item_id, title, category_id, price, original_price, condition(1-10), trade_type(0自提/1快递/2均可), days_ago, views
ITEMS = [
    (1001, "iPhone 12 128G 国行 95新 无拆修", 101, 2400, 4999, 9, 2, 26, 862),
    (1002, "小米 13 8+256 骁龙版 带原装充电器", 101, 2100, 4299, 9, 2, 24, 640),
    (1003, "联想拯救者 Y7000 2021 游戏本 i7/16G/512G", 102, 4200, 7299, 8, 0, 22, 1180),
    (1004, "华为 MatePad 11 平板 6+128 附原装键盘", 102, 1500, 2799, 8, 1, 21, 502),
    (1005, "罗技 MX Keys 无线键盘 全键位", 103, 380, 799, 9, 2, 20, 431),
    (1006, "索尼 WH-1000XM4 头戴降噪耳机", 103, 900, 1999, 9, 1, 19, 733),
    (1007, "Anker 65W 氮化镓充电器 三口", 103, 70, 149, 9, 2, 18, 268),
    (1008, "iPad 2021 64G WiFi 版 送保护壳", 102, 1600, 2499, 8, 1, 17, 594),
    (1009, "《高等数学》同济第七版 上下册 笔记齐全", 201, 25, 89, 7, 2, 16, 312),
    (1010, "《数据结构》严蔚敏 第2版 无划线", 201, 20, 45, 8, 2, 15, 190),
    (1011, "《计算机网络》谢希仁 第8版", 201, 22, 49, 8, 2, 15, 168),
    (1012, "《算法导论》第三版 中文版 硬壳", 201, 60, 128, 9, 1, 14, 246),
    (1013, "考研英语一历年真题 2015-2025 全新未拆", 202, 45, 120, 9, 2, 14, 388),
    (1014, "张宇考研数学基础30讲 附笔记本", 202, 30, 89, 8, 2, 13, 275),
    (1015, "肖秀荣考研政治1000题 未做题", 202, 25, 69, 9, 2, 12, 241),
    (1016, "《平凡的世界》全三册 经典版", 203, 35, 108, 9, 1, 12, 156),
    (1017, "村上春树作品集 5本打包", 203, 60, 200, 9, 1, 11, 203),
    (1018, "小米 LED 护眼台灯 无极调光", 3, 55, 149, 9, 2, 11, 322),
    (1019, "宿舍折叠桌 可升降 免安装", 601, 80, 199, 8, 0, 10, 410),
    (1020, "靠墙免打孔收纳架 四层", 602, 30, 79, 9, 0, 10, 187),
    (1021, "电煮锅 1.5L 学生宿舍款 已擦净", 3, 45, 119, 9, 2, 9, 356),
    (1022, "加湿器 静音 3L 大容量", 3, 60, 139, 9, 1, 9, 228),
    (1023, "迪卡侬羽毛球拍套装 双拍", 4, 90, 199, 8, 2, 8, 264),
    (1024, "山地自行车 26寸 21速 刚做过保养", 4, 320, 899, 7, 0, 8, 512),
    # 已预订 / 待确认
    (1025, "捷安特骑行头盔 L码", 4, 80, 199, 9, 1, 13, 143),
    (1026, "瑜伽垫 加厚防滑 8mm", 4, 30, 79, 9, 2, 12, 96),
    (1027, "兰蔻小黑瓶精华 30ml 专柜正品", 5, 260, 760, 9, 1, 11, 336),
    (1028, "飞利浦电动牙刷 附3个刷头", 5, 120, 399, 9, 1, 10, 208),
    (1029, "实木书桌 1.2m 桌面无划痕", 601, 150, 499, 8, 0, 10, 254),
    (1030, "四层收纳柜 白色 九成新", 602, 70, 199, 8, 0, 9, 171),
    # 已取消（商品恢复在售）
    (1031, "尤克里里 23寸 初学者套装", 7, 130, 399, 9, 1, 15, 289),
    (1032, "民谣吉他 41寸 单板 送背包", 7, 380, 899, 8, 0, 14, 497),
    (1033, "卡西欧电子琴 61键 带琴架", 7, 450, 1299, 8, 0, 13, 341),
    (1034, "机械键盘 84键 青轴 热插拔", 103, 180, 459, 8, 1, 12, 463),
    (1035, "人体工学椅 网布透气 可调腰托", 601, 220, 699, 8, 0, 11, 385),
    # 已退款（商品恢复在售）
    (1036, "Switch OLED 主机 港版 含底座", 102, 1400, 2499, 8, 1, 13, 728),
    (1037, "AirPods Pro 2 充电盒 单卖", 103, 90, 379, 9, 1, 12, 217),
    # 在售无订单
    (1038, "显示器 24寸 144Hz 电竞屏", 102, 520, 1099, 8, 0, 9, 604),
    (1039, "台灯 + 书架组合 宿舍适用", 602, 60, 169, 9, 0, 8, 132),
    (1040, "四六级真题全套 近十年", 202, 20, 60, 9, 2, 8, 214),
    (1041, "冬季帽子围巾套装 全新", 3, 25, 89, 10, 2, 7, 88),
    (1042, "文学名著套装 6本", 203, 30, 90, 9, 1, 7, 119),
    (1043, "洗衣液 4kg 未开封", 3, 30, 59, 10, 2, 6, 152),
    (1044, "自动折叠雨伞 加固防风", 3, 20, 59, 9, 2, 6, 76),
    (1045, "U盘 64G USB3.0", 103, 25, 69, 9, 2, 5, 143),
    (1046, "充电宝 20000mAh 双向快充", 103, 60, 149, 9, 2, 5, 331),
    (1047, "桌面吸尘器 静音", 3, 35, 99, 9, 1, 4, 97),
    (1048, "磁吸手机支架 车载/桌面两用", 103, 15, 39, 9, 1, 4, 64),
    (1049, "编程书籍三本打包（Java/算法/MySQL）", 201, 45, 150, 8, 2, 3, 178),
    (1050, "显示器支架 单屏 气压式", 103, 70, 159, 9, 1, 3, 126),
    # 待审核
    (1051, "蓝牙音箱 便携 防水", 103, 80, 199, 9, 1, 2, 41),
    (1052, "装饰挂画 三幅 宿舍改造", 8, 12, 39, 9, 2, 2, 33),
    (1053, "保温杯 500ml 316不锈钢", 3, 35, 99, 9, 2, 1, 52),
    (1054, "斯伯丁篮球 7号 室内外", 4, 70, 169, 8, 0, 1, 68),
    # 已下架
    (1055, "床上折叠桌 小号", 602, 40, 99, 8, 0, 10, 84),
    (1056, "键鼠套装 有线 办公", 103, 55, 129, 9, 1, 9, 71),
    (1057, "计算机组成原理复习资料 手写笔记", 201, 25, 65, 8, 2, 8, 93),
    # 违规下架
    (1058, "英文原版小说 3本 打包", 203, 45, 150, 9, 1, 7, 57),
    (1059, "静音小风扇 USB 充电", 3, 25, 69, 9, 2, 6, 61),
    # 草稿
    (1060, "二手显示器 27寸 2K 待补图", 102, 400, 1299, 8, 1, 0, 0),
]

SELLERS = [11, 12, 13, 2, 3]
BUYERS = [11, 12, 13, 1, 2, 3]

FINISHED_IDS = list(range(1001, 1025))          # 订单已完成
DELIVER_IDS = [1025, 1026, 1027]                 # 待交付
CONFIRM_IDS = [1028, 1029, 1030]                 # 待确认
CANCEL_IDS = [1031, 1032, 1033, 1034, 1035]      # 已取消
REFUND_IDS = [1036, 1037]                        # 已退款

ITEM_STATUS = {}
for i in FINISHED_IDS:
    ITEM_STATUS[i] = 4
for i in DELIVER_IDS + CONFIRM_IDS:
    ITEM_STATUS[i] = 3
for i in CANCEL_IDS + REFUND_IDS:
    ITEM_STATUS[i] = 2
for i in range(1038, 1051):
    ITEM_STATUS[i] = 2
for i in range(1051, 1055):
    ITEM_STATUS[i] = 1
for i in range(1055, 1058):
    ITEM_STATUS[i] = 5
for i in range(1058, 1060):
    ITEM_STATUS[i] = 6
ITEM_STATUS[1060] = 0

SELLER_OF = {it[0]: SELLERS[i % len(SELLERS)] for i, it in enumerate(ITEMS)}


def cover(item_id):
    return "/upload/demo/item-%02d.png" % (((item_id - 1001) % 12) + 1)


def seller_name(uid):
    return {11: "小明同学", 12: "小红同学", 13: "小刚同学", 2: "运营小王", 3: "审核小李"}[uid]


lines = []
w = lines.append

w("-- =====================================================================")
w("-- CampusMart 业务演示数据（商品/订单/评价/收藏/举报/消息/公告/轮播/AI 调用日志）")
w("-- 执行顺序：01-schema.sql -> 02-seed-business.sql -> 03-perms-extra.sql")
w("-- 特点：时间全部相对 NOW() 生成，保证趋势图/漏斗/RFM/大屏任何时候都有近 30 天数据")
w("-- 幂等：先删除本次生成范围内的数据再插入，可重复执行")
w("-- =====================================================================")
w("")
w("USE campus_mart;")
w("SET NAMES utf8mb4;")
w("")
w("-- 清理")
w("DELETE FROM biz_comment  WHERE comment_id BETWEEN 3001 AND 3200;")
w("DELETE FROM biz_favorite WHERE favorite_id BETWEEN 4001 AND 5000;")
w("DELETE FROM biz_order    WHERE order_id BETWEEN 2001 AND 2100;")
w("DELETE FROM biz_item     WHERE item_id BETWEEN 1001 AND 1060;")
w("DELETE FROM biz_report   WHERE report_id BETWEEN 5001 AND 5100;")
w("DELETE FROM biz_message  WHERE message_id BETWEEN 6001 AND 6200;")
w("DELETE FROM ai_call_log  WHERE id BETWEEN 7001 AND 7200;")
w("DELETE FROM biz_notice   WHERE notice_id BETWEEN 1 AND 9;")
w("DELETE FROM biz_banner   WHERE banner_id BETWEEN 1 AND 9;")
w("")

# ---------------- 商品 ----------------
w("-- 1. 商品 60 条（24 已售出 / 6 已预订 / 25 在售 / 4 待审核 / 3 下架 / 2 违规 / 1 草稿）")
w("INSERT INTO biz_item (item_id, item_no, seller_id, category_id, title, summary, description, price,")
w("  original_price, item_condition, trade_type, cover_url, images, tags, location, contact_type,")
w("  ai_generated, view_count, favorite_count, status, audit_remark, publish_time, del_flag, create_time, update_time) VALUES")
rows = []
for (iid, title, cat, price, orig, cond, tt, days, views) in ITEMS:
    st = ITEM_STATUS[iid]
    item_no = "CM%d%04d" % (20261000 + (30 - days), iid)
    images = '["%s"]' % cover(iid)
    tags = '["二手","校园","自提"]'
    summary = title[:60]
    desc = ("%s。成色 %d 成新，%s。原价 %.2f 元，现价 %.2f 元。"
            "交易方式：%s。学号认证用户，支持当面验货，非诚勿扰。") % (
        title, cond, "功能完好无维修" if cond >= 8 else "有正常使用痕迹",
        orig, price, ["校内自提", "可快递", "自提/快递均可"][tt])
    ai_gen = "1" if iid % 7 == 0 else "0"
    audit = "'标题含违规词，请修改后重新提交'" if st == 6 else "NULL"
    if st == 5:
        audit = "'卖家主动下架'"
    publish = "DATE_SUB(NOW(), INTERVAL %d DAY)" % days
    created = "DATE_SUB(NOW(), INTERVAL %d DAY)" % (days + 1)
    if st == 0:
        publish = "NULL"
    loc = ["东区宿舍", "西区宿舍", "南苑", "北苑", "教学楼"][iid % 5]
    rows.append(
        "(%d,'%s',%d,%d,'%s','%s','%s',%.2f,%.2f,%d,%d,'%s','%s','%s','%s',%d,'%s',%d,%d,%d,%s,%s,'0',%s,%s)" % (
            iid, item_no, SELLER_OF[iid], cat, title, summary, desc, price, orig,
            cond, tt, cover(iid), images, tags, loc, (1 if tt == 1 else 0), ai_gen, views,
            max(0, views // 12), st, audit, publish, created, created))
w(",\n".join(rows) + ";")
w("")
w("-- 收藏数按 biz_favorite 真实行数回写")
w("")

# ---------------- 订单 ----------------
w("-- 2. 订单 37 条")
w("INSERT INTO biz_order (order_id, order_no, item_id, buyer_id, seller_id, amount, status, pay_type,")
w("  trade_place, buyer_remark, cancel_reason, create_time, pay_time, finish_time, cancel_time, update_time) VALUES")
order_rows = []
oid = 2001
order_item = {}
place_list = ["东区 3 号宿舍楼下", "图书馆门口", "南苑食堂", "西区快递点", "教学楼 A 座"]


def mk_order(item_id, status, days):
    global oid
    seller = SELLER_OF[item_id]
    buyer = BUYERS[oid % len(BUYERS)]
    if buyer == seller:
        buyer = BUYERS[(oid + 1) % len(BUYERS)]
    price = [i for i in ITEMS if i[0] == item_id][0][3]
    no = "CM%s%04d" % ("202610%02d" % max(1, 27 - days), oid)
    ct = "DATE_SUB(NOW(), INTERVAL %d DAY)" % days
    pay = "DATE_ADD(%s, INTERVAL 2 HOUR)" % ct if status != 0 else "NULL"
    fin = "DATE_ADD(%s, INTERVAL 1 DAY)" % ct if status == 2 else "NULL"
    can = "DATE_ADD(%s, INTERVAL 6 HOUR)" % ct if status == 3 else "NULL"
    remark = "'请尽快联系'" if status in (0, 1) else "NULL"
    reason = "'买家临时有事'" if status == 3 else ("'商品与描述不符'" if status == 5 else "NULL")
    place = place_list[oid % len(place_list)]
    order_item[oid] = (item_id, buyer, seller, price, status)
    oid += 1
    return ("(%d,'%s',%d,%d,%d,%.2f,%d,1,'%s',%s,%s,%s,%s,%s,%s,%s)" % (
        oid - 1, no, item_id, buyer, seller, price, status, place, remark, reason, ct, pay, fin, can, ct))


for i, iid in enumerate(FINISHED_IDS):
    order_rows.append(mk_order(iid, 2, 26 - i))
for iid in DELIVER_IDS:
    order_rows.append(mk_order(iid, 1, 3))
for iid in CONFIRM_IDS:
    order_rows.append(mk_order(iid, 0, 1))
for iid in CANCEL_IDS:
    order_rows.append(mk_order(iid, 3, 8))
for iid in REFUND_IDS:
    order_rows.append(mk_order(iid, 5, 6))
w(",\n".join(order_rows) + ";")
w("")

# ---------------- 收藏 ----------------
w("-- 3. 收藏（真实行数，供收藏榜/个性化推荐使用）")
fav_rows = []
fid = 4001
for uid in [11, 12, 13, 1, 2]:
    cand = [it[0] for it in ITEMS if SELLER_OF[it[0]] != uid and ITEM_STATUS[it[0]] in (2, 3, 4)]
    random.shuffle(cand)
    for iid in cand[:12]:
        days = random.randint(0, 20)
        fav_rows.append("(%d,%d,%d,DATE_SUB(NOW(), INTERVAL %d DAY))" % (fid, uid, iid, days))
        fid += 1
w("INSERT INTO biz_favorite (favorite_id, user_id, item_id, create_time) VALUES")
w(",\n".join(fav_rows) + ";")
w("")
w("UPDATE biz_item i SET i.favorite_count = (SELECT COUNT(*) FROM biz_favorite f WHERE f.item_id = i.item_id)")
w("WHERE i.item_id BETWEEN 1001 AND 1060;")
w("")

# ---------------- 评价 ----------------
w("-- 4. 评价（已完成订单前 18 笔）")
fin_orders = [o for o in order_item.items() if o[1][4] == 2][:18]
cmt_texts = [
    "成色和描述一致，学长人很好，约在图书馆当面验货，交易很快。",
    "东西很新，价格也实在，宿舍楼下自提非常方便。",
    "包装保护得很好，收到就能用，推荐！",
    "有一点点使用痕迹但完全不影响，性价比高。",
    "卖家很耐心，还送了配件，五星好评。",
    "比想象中新，价格比新买的便宜一半，很值。",
]
cmt_rows = []
cid = 3001
for (order_id, (item_id, buyer, seller, price, st)) in fin_orders:
    score = 5 if cid % 3 else 4
    content = cmt_texts[cid % len(cmt_texts)]
    cmt_rows.append("(%d,%d,%d,%d,%d,%d,'%s','[]',NULL,'0',DATE_SUB(NOW(), INTERVAL %d DAY))" % (
        cid, order_id, item_id, buyer, seller, score, content, max(0, 20 - (cid - 3001))))
    cid += 1
w("INSERT INTO biz_comment (comment_id, order_id, item_id, from_user_id, to_user_id, score, content, images, reply_content, status, create_time) VALUES")
w(",\n".join(cmt_rows) + ";")
w("")

# ---------------- 举报 ----------------
w("-- 5. 举报（后台待处理演示）")
w("INSERT INTO biz_report (report_id, item_id, reporter_id, reason_type, content, images, status, handle_user_id, handle_remark, handle_time, create_time) VALUES")
w("(5001,1058,11,'违禁物品','疑似售卖盗版书籍',NULL,0,NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),")
w("(5002,1059,12,'虚假描述','图片与实物不符，标价虚高',NULL,0,NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),")
w("(5003,1036,13,'价格欺诈','同一商品多次改价',NULL,1,2,'已核实，提醒卖家规范改价',DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),")
w("(5004,1041,1,'骚扰信息','联系方式为代购广告',NULL,2,2,'举报不成立，已驳回',DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 7 DAY));")
w("")

# ---------------- 消息 ----------------
w("-- 6. 站内通知")
w("INSERT INTO biz_message (message_id, user_id, msg_type, title, content, biz_id, is_read, create_time) VALUES")
msg = [
    (6001, 11, 3, "您发布的商品已通过审核", "《iPhone 12 128G 国行 95新》已上架，买家可浏览下单。", 1001, "1", 26),
    (6002, 11, 2, "您有一笔新订单", "买家已下单并支付，请尽快联系买家约定交付时间。", 2001, "0", 20),
    (6003, 12, 3, "商品审核未通过", "《英文原版小说 3本 打包》因标题含违规词被下架，请修改后重新提交。", 1058, "0", 7),
    (6004, 13, 2, "订单已完成", "买家已确认收货，交易金额 90.00 元已入账信用记录。", 2013, "1", 5),
    (6005, 11, 1, "信用分提升提醒", "您的信用分提升至 98 分，可享受优先推荐曝光。", None, "0", 3),
    (6006, 12, 1, "校园易站使用小贴士", "发布商品时上传真实图片和清晰成色描述，成交率提升 3 倍。", None, "0", 1),
]
mrows = []
for (mid, uid, mt, title, content, biz, read, days) in msg:
    bizv = "NULL" if biz is None else str(biz)
    mrows.append("(%d,%d,%d,'%s','%s',%s,'%s',DATE_SUB(NOW(), INTERVAL %d DAY))" % (mid, uid, mt, title, content, bizv, read, days))
w(",\n".join(mrows) + ";")
w("")

# ---------------- 公告 ----------------
w("-- 7. 公告")
w("INSERT INTO biz_notice (notice_id, title, content, notice_type, status, publish_time, create_by, create_time) VALUES")
w("(1,'校园易站正式上线公告','校园易站 CampusMart 已上线，支持商品发布、AI 文案生成、校内自提交易。首周发布商品免审核（人工抽检）。',1,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),1,DATE_SUB(NOW(), INTERVAL 20 DAY)),")
w("(2,'交易安全提醒：请勿脱离平台交易','为保障双方权益，请使用平台内置订单流程，勿加微信私下转账。遇可疑情况可在商品页举报，运营会在 24 小时内处理。',2,'0',DATE_SUB(NOW(), INTERVAL 9 DAY),1,DATE_SUB(NOW(), INTERVAL 9 DAY)),")
w("(3,'毕业季闲置清仓活动开始','12 月毕业季清仓专区开启，教材类商品佣金全免，优质商品进入首页推荐位。活动时间：12.01 - 12.31。',1,'0',DATE_SUB(NOW(), INTERVAL 2 DAY),1,DATE_SUB(NOW(), INTERVAL 2 DAY));")
w("")

# ---------------- 轮播 ----------------
w("-- 8. 轮播图")
w("INSERT INTO biz_banner (banner_id, title, image_url, link_url, sort, status, start_time, end_time, create_time) VALUES")
w("(1,'让闲置流动起来','/upload/demo/banner-01.png','/pages/item/list',1,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_ADD(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY)),")
w("(2,'AI 一键发布','/upload/demo/banner-02.png','/pages/publish/index',2,'0',DATE_SUB(NOW(), INTERVAL 20 DAY),DATE_ADD(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY)),")
w("(3,'毕业季清仓专区','/upload/demo/banner-03.png','/pages/item/list?tag=clearance',3,'0',DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 10 DAY));")
w("")

# ---------------- AI 调用日志 ----------------
w("-- 9. AI 调用日志（近 7 天，供 AI 统计面板）")
scenes = [("copywrite", "ITEM_COPYWRITE", 0.55), ("chat", "CUSTOMER_SERVICE", 0.25),
          ("audit", "ITEM_AUDIT", 0.15), ("price", "ITEM_PRICE", 0.05)]
ai_rows = []
aid = 7001
for d in range(7):
    n = 12 if d % 2 == 0 else 9
    for k in range(n):
        r = random.random()
        acc = 0
        for (scene, code, p) in scenes:
            acc += p
            if r <= acc:
                break
        ok = random.random() > 0.06
        pt = random.randint(120, 620)
        ctok = random.randint(80, 480)
        ms = random.randint(420, 3600) if scene != "audit" else random.randint(180, 900)
        provider = "remote" if (aid % 5) else "mock"
        if scene == "audit":
            provider = "rule"
        err = "NULL" if ok else "'连接超时，已降级'"
        ai_rows.append("(%d,%d,'%s','%s','%s','%s',%d,%d,%d,%d,'%s',%s,DATE_SUB(NOW(), INTERVAL %d HOUR))" % (
            aid, [11, 12, 13, 1, 2][aid % 5], scene, code, provider,
            "deepseek-chat" if provider == "remote" else "template", pt, ctok, pt + ctok,
            ms, "0" if ok else "1", err, d * 24 + random.randint(0, 20)))
        aid += 1
w("INSERT INTO ai_call_log (id, user_id, scene, template_code, provider, model, prompt_tokens, completion_tokens, total_tokens, cost_ms, status, error_msg, create_time) VALUES")
w(",\n".join(ai_rows) + ";")
w("")
w("-- 校验")
w("SELECT 'biz_item' t, COUNT(*) c FROM biz_item WHERE item_id BETWEEN 1001 AND 1060")
w("UNION ALL SELECT 'biz_order', COUNT(*) FROM biz_order WHERE order_id BETWEEN 2001 AND 2100")
w("UNION ALL SELECT 'biz_favorite', COUNT(*) FROM biz_favorite WHERE favorite_id BETWEEN 4001 AND 5000")
w("UNION ALL SELECT 'biz_comment', COUNT(*) FROM biz_comment WHERE comment_id BETWEEN 3001 AND 3200")
w("UNION ALL SELECT 'biz_report', COUNT(*) FROM biz_report")
w("UNION ALL SELECT 'biz_message', COUNT(*) FROM biz_message")
w("UNION ALL SELECT 'ai_call_log', COUNT(*) FROM ai_call_log;")

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("written:", OUT, "lines:", len(lines), "items:", len(ITEMS), "orders:", len(order_rows),
      "favs:", len(fav_rows), "comments:", len(cmt_rows), "ai:", len(ai_rows))
