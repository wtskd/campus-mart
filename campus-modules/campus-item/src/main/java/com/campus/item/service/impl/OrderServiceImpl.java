package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizComment;
import com.campus.item.domain.entity.BizItem;
import com.campus.item.domain.entity.BizMessage;
import com.campus.item.domain.entity.BizOrder;
import com.campus.item.dto.OrderCreateDTO;
import com.campus.item.dto.OrderQueryDTO;
import com.campus.item.dto.OrderStatVO;
import com.campus.item.dto.OrderStatusDTO;
import com.campus.item.dto.OrderVO;
import com.campus.item.mapper.BizCommentMapper;
import com.campus.item.mapper.BizItemMapper;
import com.campus.item.mapper.BizMessageMapper;
import com.campus.item.mapper.BizOrderMapper;import com.campus.item.service.MsgType;
import com.campus.item.service.OrderService;
import com.campus.item.service.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * <p>二手单品交易：一个商品同一时间只允许一条进行中的订单；下单通过「商品 2在售→3已预订」的
 * 乐观更新保证并发安全，取消后商品恢复在售、旧订单保留为历史。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 商品状态：2 在售 */
    private static final int ITEM_ON_SALE = 2;

    /** 商品状态：3 已预订 */
    private static final int ITEM_BOOKED = 3;

    /** 商品状态：4 已售出 */
    private static final int ITEM_SOLD = 4;

    /** 消息类型：订单消息（取值见 {@link MsgType}） */
    private static final int MSG_TYPE_ORDER = MsgType.ORDER;

    /** 订单号时间部分 */
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BizOrderMapper orderMapper;
    private final BizItemMapper itemMapper;
    private final BizMessageMapper messageMapper;
    private final BizCommentMapper commentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        Long buyerId = SecurityUtils.requireUserId();
        BizItem item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new ServiceException(404, "商品不存在或已下架");
        }
        if (buyerId.equals(item.getSellerId())) {
            throw new ServiceException(400, "不能购买自己发布的商品");
        }
        if (item.getStatus() == null || item.getStatus() != ITEM_ON_SALE) {
            throw new ServiceException(409, "商品当前不可下单（可能已预订、已售出或已下架）");
        }
        // 二手单品：同一商品已有进行中的订单则不允许再下单（FINISHED 也视为占用，取消/退款后可重新下单）
        Long activeCount = orderMapper.selectCount(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getItemId, dto.getItemId())
                .in(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_DELIVER, OrderStatus.FINISHED));
        if (activeCount != null && activeCount > 0) {
            throw new ServiceException(409, "该商品已被预订或已售出");
        }
        // 乐观并发控制：仅当商品仍在售时才允许预订，影响行数为 0 说明被他人抢先
        int rows = itemMapper.update(null, new LambdaUpdateWrapper<BizItem>()
                .eq(BizItem::getItemId, dto.getItemId())
                .eq(BizItem::getStatus, ITEM_ON_SALE)
                .set(BizItem::getStatus, ITEM_BOOKED)
                .set(BizItem::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "该商品已被预订或已售出");
        }
        BizOrder order = new BizOrder();
        order.setOrderNo(generateOrderNo());
        order.setItemId(item.getItemId());
        order.setBuyerId(buyerId);
        order.setSellerId(item.getSellerId());
        order.setAmount(item.getPrice());
        order.setStatus(OrderStatus.PENDING_CONFIRM);
        order.setPayType(0);
        order.setTradePlace(StringUtils.hasText(dto.getTradePlace()) ? dto.getTradePlace().trim() : item.getLocation());
        order.setBuyerRemark(dto.getBuyerRemark());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            log.warn("下单唯一键冲突，itemId={}，buyerId={}", item.getItemId(), buyerId);
            throw new ServiceException(409, "该商品已被预订或已售出");
        }
        // 通知卖家有新的订单
        insertMessage(item.getSellerId(), MSG_TYPE_ORDER, "您有一笔新订单",
                "你的商品《" + item.getTitle() + "》被下单，订单号 " + order.getOrderNo() + "，请及时确认。",
                order.getOrderId());
        return order.getOrderId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = requireOrder(orderId);
        if (!userId.equals(order.getBuyerId())) {
            throw new ServiceException(403, "只有买家可以付款");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
        // 已处于「待交付」：幂等返回；若卖家先标记交付、买家尚未付款（pay_time 为空），补记付款信息
        if (status == OrderStatus.PENDING_DELIVER) {
            if (order.getPayTime() == null) {
                orderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                        .eq(BizOrder::getOrderId, orderId)
                        .eq(BizOrder::getStatus, OrderStatus.PENDING_DELIVER)
                        .isNull(BizOrder::getPayTime)
                        .set(BizOrder::getPayTime, LocalDateTime.now())
                        .set(BizOrder::getPayType, 1)
                        .set(BizOrder::getUpdateTime, LocalDateTime.now()));
            }
            log.info("订单 {} 处于待交付状态，重复付款请求幂等返回", orderId);
            return;
        }
        if (status != OrderStatus.PENDING_CONFIRM) {
            throw new ServiceException(409, "订单当前状态不可付款，请刷新");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getOrderId, orderId)
                .eq(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM)
                .set(BizOrder::getStatus, OrderStatus.PENDING_DELIVER)
                .set(BizOrder::getPayTime, LocalDateTime.now())
                .set(BizOrder::getPayType, 1)
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = requireOrder(orderId);
        if (!userId.equals(order.getSellerId())) {
            throw new ServiceException(403, "只有卖家可以标记交付");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
        // biz_order 无独立「已交付待确认」态，交付后停留在 1 待交付（幂等返回，靠 pay_time 区分是否已付款）
        if (status == OrderStatus.PENDING_DELIVER) {
            log.info("订单 {} 已处于待交付状态，重复交付标记幂等返回", orderId);
            return;
        }
        if (status != OrderStatus.PENDING_CONFIRM) {
            throw new ServiceException(409, "订单当前状态不可标记交付，请刷新");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getOrderId, orderId)
                .eq(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM)
                .set(BizOrder::getStatus, OrderStatus.PENDING_DELIVER)
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = requireOrder(orderId);
        if (!userId.equals(order.getBuyerId())) {
            throw new ServiceException(403, "只有买家可以确认收货");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_DELIVER) {
            throw new ServiceException(409, "订单当前状态不可确认收货，请刷新");
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getOrderId, orderId)
                .eq(BizOrder::getStatus, OrderStatus.PENDING_DELIVER)
                .set(BizOrder::getStatus, OrderStatus.FINISHED)
                .set(BizOrder::getFinishTime, LocalDateTime.now())
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
        // 商品：已预订 -> 已售出
        itemMapper.update(null, new LambdaUpdateWrapper<BizItem>()
                .eq(BizItem::getItemId, order.getItemId())
                .eq(BizItem::getStatus, ITEM_BOOKED)
                .set(BizItem::getStatus, ITEM_SOLD)
                .set(BizItem::getUpdateTime, LocalDateTime.now()));
        // 卖家信用分 +1（上限 100）
        jdbcTemplate.update("UPDATE sys_user SET credit_score = LEAST(credit_score + 1, 100) "
                + "WHERE user_id = ? AND del_flag = '0'", order.getSellerId());
        insertMessage(order.getSellerId(), MSG_TYPE_ORDER, "订单已完成",
                "您的商品已被买家确认收货，订单号 " + order.getOrderNo() + "，交易完成。", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = requireOrder(orderId);
        boolean buyer = userId.equals(order.getBuyerId());
        boolean seller = userId.equals(order.getSellerId());
        if (!buyer && !seller) {
            throw new ServiceException(403, "无权操作该订单");
        }
        if (order.getStatus() == null
                || (order.getStatus() != OrderStatus.PENDING_CONFIRM && order.getStatus() != OrderStatus.PENDING_DELIVER)) {
            throw new ServiceException(409, "订单当前状态不可取消，请刷新");
        }
        String cancelReason = StringUtils.hasText(reason) ? reason.trim() : "用户取消";
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getOrderId, orderId)
                .in(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_DELIVER)
                .set(BizOrder::getStatus, OrderStatus.CANCELLED)
                .set(BizOrder::getCancelTime, LocalDateTime.now())
                .set(BizOrder::getCancelReason, cancelReason)
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
        // 商品恢复在售，允许其他人再次下单（旧订单保留历史）
        restoreItemOnSale(order.getItemId());
        Long other = buyer ? order.getSellerId() : order.getBuyerId();
        insertMessage(other, MSG_TYPE_ORDER, "订单已取消",
                "订单号 " + order.getOrderNo() + " 已被" + (buyer ? "买家" : "卖家") + "取消。", orderId);
    }

    @Override
    public PageResult<OrderVO> myOrders(String role, Integer status, PageQuery pageQuery) {
        Long userId = SecurityUtils.requireUserId();
        String r = StringUtils.hasText(role) ? role.trim().toLowerCase() : "buy";
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if ("buy".equals(r)) {
            wrapper.eq(BizOrder::getBuyerId, userId);
        } else if ("sell".equals(r)) {
            wrapper.eq(BizOrder::getSellerId, userId);
        } else {
            throw new ServiceException(400, "role 参数只能为 buy 或 sell");
        }
        if (status != null) {
            wrapper.eq(BizOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizOrder::getCreateTime);
        Page<BizOrder> page = orderMapper.selectPage(pageQuery.toPage(), wrapper);
        return toPageResult(page);
    }

    @Override
    public OrderVO detail(Long orderId) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = requireOrder(orderId);
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new ServiceException(403, "无权查看该订单");
        }
        List<OrderVO> list = toVOList(Collections.singletonList(order));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PageResult<OrderVO> adminPage(OrderQueryDTO query) {
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like(BizOrder::getOrderNo, query.getOrderNo().trim());
        }
        if (query.getItemId() != null) {
            wrapper.eq(BizOrder::getItemId, query.getItemId());
        }
        if (query.getBuyerId() != null) {
            wrapper.eq(BizOrder::getBuyerId, query.getBuyerId());
        }
        if (query.getSellerId() != null) {
            wrapper.eq(BizOrder::getSellerId, query.getSellerId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BizOrder::getStatus, query.getStatus());
        }
        if (query.getBeginTime() != null) {
            wrapper.ge(BizOrder::getCreateTime, query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(BizOrder::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(BizOrder::getCreateTime);
        Page<BizOrder> page = orderMapper.selectPage(query.toPage(), wrapper);
        return toPageResult(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateStatus(OrderStatusDTO dto) {
        BizOrder order = requireOrder(dto.getOrderId());
        int from = order.getStatus() == null ? -1 : order.getStatus();
        int to = dto.getStatus();
        if (from == to) {
            throw new ServiceException(409, "订单已处于该状态，无需变更");
        }
        if (!OrderStatus.canTransit(from, to)) {
            throw new ServiceException(409, "订单状态不允许从「" + OrderStatus.textOf(from)
                    + "」变更为「" + OrderStatus.textOf(to) + "」");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BizOrder> wrapper = new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getOrderId, order.getOrderId())
                .eq(BizOrder::getStatus, from)
                .set(BizOrder::getStatus, to)
                .set(BizOrder::getUpdateTime, now);
        switch (to) {
            case OrderStatus.PENDING_DELIVER -> wrapper.set(BizOrder::getStatus, OrderStatus.PENDING_DELIVER);
            case OrderStatus.FINISHED -> wrapper.set(BizOrder::getFinishTime, now);
            case OrderStatus.CANCELLED -> {
                wrapper.set(BizOrder::getCancelTime, now);
                if (StringUtils.hasText(dto.getRemark())) {
                    wrapper.set(BizOrder::getCancelReason, dto.getRemark().trim());
                }
            }
            case OrderStatus.REFUNDING -> {
                if (StringUtils.hasText(dto.getRemark())) {
                    wrapper.set(BizOrder::getCancelReason, dto.getRemark().trim());
                }
            }
            case OrderStatus.REFUNDED -> {
                wrapper.set(BizOrder::getCancelTime, now);
                if (StringUtils.hasText(dto.getRemark())) {
                    wrapper.set(BizOrder::getCancelReason, dto.getRemark().trim());
                }
            }
            default -> throw new ServiceException(409, "订单状态不允许变更为该值");
        }
        int rows = orderMapper.update(null, wrapper);
        if (rows == 0) {
            throw new ServiceException(409, "订单状态已变更，请刷新");
        }
        if (to == OrderStatus.FINISHED) {
            itemMapper.update(null, new LambdaUpdateWrapper<BizItem>()
                    .eq(BizItem::getItemId, order.getItemId())
                    .eq(BizItem::getStatus, ITEM_BOOKED)
                    .set(BizItem::getStatus, ITEM_SOLD)
                    .set(BizItem::getUpdateTime, now));
            jdbcTemplate.update("UPDATE sys_user SET credit_score = LEAST(credit_score + 1, 100) "
                    + "WHERE user_id = ? AND del_flag = '0'", order.getSellerId());
        } else if (to == OrderStatus.CANCELLED || to == OrderStatus.REFUNDED) {
            // 取消/退款完成后商品恢复在售（退款中 4 保持不动，等退款完成再回滚）
            restoreItemOnSale(order.getItemId());
        }
        if (to == OrderStatus.CANCELLED) {
            insertMessage(order.getBuyerId(), MSG_TYPE_ORDER, "订单已取消",
                    "订单号 " + order.getOrderNo() + " 已被平台取消。", order.getOrderId());
        } else if (to == OrderStatus.REFUNDING) {
            insertMessage(order.getBuyerId(), MSG_TYPE_ORDER, "退款处理中",
                    "订单号 " + order.getOrderNo() + " 的退款申请已受理，请耐心等待。", order.getOrderId());
        } else if (to == OrderStatus.REFUNDED) {
            insertMessage(order.getBuyerId(), MSG_TYPE_ORDER, "订单已退款",
                    "订单号 " + order.getOrderNo() + " 已完成退款。", order.getOrderId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefund(Long orderId, String reason) {
        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setOrderId(orderId);
        dto.setStatus(OrderStatus.REFUNDING);
        dto.setRemark(reason);
        // 复用状态机：1→4、2→4 允许，其它流转会抛 409
        adminUpdateStatus(dto);
    }

    @Override
    public OrderStatVO todayStat() {
        LocalDateTime begin = LocalDate.now().atStartOfDay();
        LocalDateTime end = begin.plusDays(1);
        Long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<BizOrder>()
                .ge(BizOrder::getCreateTime, begin)
                .lt(BizOrder::getCreateTime, end));
        BigDecimal todayTradeAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM biz_order WHERE status = ? AND finish_time >= ? AND finish_time < ?",
                BigDecimal.class, OrderStatus.FINISHED, begin, end);
        Long pendingCount = orderMapper.selectCount(new LambdaQueryWrapper<BizOrder>()
                .in(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_DELIVER));
        OrderStatVO vo = new OrderStatVO();
        vo.setTodayOrderCount(todayOrderCount == null ? 0 : todayOrderCount);
        vo.setTodayTradeAmount(todayTradeAmount == null ? BigDecimal.ZERO : todayTradeAmount);
        vo.setPendingCount(pendingCount == null ? 0 : pendingCount);
        return vo;
    }

    // ------------------------------------------------------------------ 内部方法

    private BizOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new ServiceException(400, "订单ID不能为空");
        }
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException(404, "订单不存在");
        }
        return order;
    }

    private String generateOrderNo() {
        return "CM" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private void insertMessage(Long receiverId, int msgType, String title, String content, Long bizId) {
        if (receiverId == null) {
            return;
        }
        BizMessage message = new BizMessage();
        message.setUserId(receiverId);
        message.setMsgType(msgType);
        message.setTitle(title);
        message.setContent(content);
        message.setBizId(bizId);
        message.setIsRead("0");
        messageMapper.insert(message);
    }

    /**
     * 商品恢复在售（取消/退款完成后）。
     *
     * <p>若该商品已有其它进行中的订单（0/1/2），说明已被重新下单，此时不回滚商品状态，避免覆盖新订单。</p>
     */
    private void restoreItemOnSale(Long itemId) {
        if (itemId == null) {
            return;
        }
        Long active = orderMapper.selectCount(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getItemId, itemId)
                .in(BizOrder::getStatus, OrderStatus.PENDING_CONFIRM,
                        OrderStatus.PENDING_DELIVER, OrderStatus.FINISHED));
        if (active != null && active > 0) {
            return;
        }
        itemMapper.update(null, new LambdaUpdateWrapper<BizItem>()
                .eq(BizItem::getItemId, itemId)
                .in(BizItem::getStatus, ITEM_BOOKED, ITEM_SOLD)
                .set(BizItem::getStatus, ITEM_ON_SALE)
                .set(BizItem::getUpdateTime, LocalDateTime.now()));
    }

    /** 分页结果批量补全商品标题/封面与买卖家昵称，避免 N+1 */
    private PageResult<OrderVO> toPageResult(Page<BizOrder> page) {
        PageResult<OrderVO> result = new PageResult<>();
        result.setRecords(toVOList(page.getRecords()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    private List<OrderVO> toVOList(List<BizOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return new ArrayList<>();
        }
        Map<Long, BizItem> items = loadItems(orders.stream()
                .map(BizOrder::getItemId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> userIds = new java.util.HashSet<>();
        for (BizOrder order : orders) {
            if (order.getBuyerId() != null) {
                userIds.add(order.getBuyerId());
            }
            if (order.getSellerId() != null) {
                userIds.add(order.getSellerId());
            }
        }
        Map<Long, String> nicknames = loadNicknames(userIds);
        Map<Long, BizComment> comments = loadComments(orders.stream()
                .map(BizOrder::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<OrderVO> list = new ArrayList<>(orders.size());
        for (BizOrder order : orders) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            BizItem item = items.get(order.getItemId());
            if (item != null) {
                vo.setItemTitle(item.getTitle());
                vo.setItemCover(item.getCoverUrl());
            }
            vo.setBuyerNickname(nicknames.get(order.getBuyerId()));
            vo.setSellerNickname(nicknames.get(order.getSellerId()));
            vo.setStatusText(OrderStatus.textOf(order.getStatus()));
            BizComment comment = comments.get(order.getOrderId());
            vo.setCommented(comment != null);
            if (comment != null) {
                vo.setCommentId(comment.getCommentId());
                vo.setCommentScore(comment.getScore());
                vo.setCommentContent(comment.getContent());
                vo.setCommentTime(comment.getCreateTime());
                vo.setCommentReply(comment.getReplyContent());
            }
            list.add(vo);
        }
        return list;
    }

    /** 批量查询订单评价（一个订单最多一条），用于前端展示「已评价 / 查看评价」 */
    private Map<Long, BizComment> loadComments(Collection<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<BizComment> comments = commentMapper.selectList(new LambdaQueryWrapper<BizComment>()
                .in(BizComment::getOrderId, orderIds));
        Map<Long, BizComment> map = new HashMap<>();
        for (BizComment comment : comments) {
            if (comment.getOrderId() != null) {
                map.putIfAbsent(comment.getOrderId(), comment);
            }
        }
        return map;
    }

    private Map<Long, BizItem> loadItems(Collection<Long> itemIds) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        List<BizItem> items = itemMapper.selectBatchIds(itemIds);
        Map<Long, BizItem> map = new HashMap<>();
        for (BizItem item : items) {
            map.put(item.getItemId(), item);
        }
        return map;
    }

    /** 批量查询 sys_user 昵称（sys_user 不在本模块，使用 JdbcTemplate 避免跨模块改 Mapper） */
    private Map<Long, String> loadNicknames(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT user_id, COALESCE(NULLIF(nickname, ''), username) AS nickname "
                + "FROM sys_user WHERE user_id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ids.toArray());
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("user_id");
            Object nickname = row.get("nickname");
            if (id != null) {
                map.put(((Number) id).longValue(), nickname == null ? null : nickname.toString());
            }
        }
        return map;
    }
}
