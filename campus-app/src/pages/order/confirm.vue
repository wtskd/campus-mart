<!-- 确认下单：商品信息 + 交易地点 + 买家留言 -->
<template>
  <view class="page">
    <view v-if="loading" class="skeleton-wrap">
      <skeleton-box :rows="2" mode="row" />
    </view>

    <block v-else-if="item">
      <!-- 商品信息 -->
      <view class="card goods">
        <image class="cover" :src="mediaUrl(coverOf(item))" mode="aspectFill" />
        <view class="goods-info">
          <text class="title">{{ item.title }}</text>
          <text class="cond">{{ conditionText(item.itemCondition) }} · {{ tradeTypeText(item.tradeType) }}</text>
          <text class="price">{{ formatPrice(item.price) }}</text>
        </view>
      </view>

      <!-- 交易信息 -->
      <view class="card">
        <view class="form-item">
          <text class="label">交易地点<text class="required">*</text></text>
          <input v-model="tradePlace" class="input" maxlength="40" placeholder="如：东区宿舍楼下" />
        </view>
        <view class="form-item">
          <text class="label">联系电话</text>
          <input v-model="contactPhone" class="input" type="number" maxlength="11" placeholder="选填，便于卖家联系" />
        </view>
        <view class="form-item remark-item">
          <text class="label">买家留言</text>
          <textarea v-model="buyerRemark" class="textarea" maxlength="100" placeholder="如：周末下午方便交易" />
        </view>
      </view>

      <!-- 金额 -->
      <view class="card">
        <view class="amount-row">
          <text>商品金额</text>
          <text class="amount">{{ formatPrice(item.price) }}</text>
        </view>
        <view class="amount-row sub">
          <text>交易方式</text>
          <text>{{ tradeTypeText(item.tradeType) }}</text>
        </view>
        <view class="tip">下单后请与卖家确认交易时间；确认收货后订单完成，可对卖家进行评价。</view>
      </view>

      <view class="bottom-space"></view>
      <view class="bottom-bar">
        <view class="total">
          <text class="total-label">合计</text>
          <text class="total-price">{{ formatPrice(item.price) }}</text>
        </view>
        <view class="submit-btn" @tap="submit">提交订单</view>
      </view>
    </block>

    <empty-state v-else icon="😕" text="商品信息加载失败" action-text="返回" @action="goBack" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import skeletonBox from '@/components/skeleton-box.vue'
import emptyState from '@/components/empty-state.vue'
import { getItemDetail } from '@/api/item'
import { createOrder } from '@/api/order'
import { formatPrice, conditionText, tradeTypeText, coverOf, mediaUrl } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const itemId = ref('')
const item = ref(null)
const loading = ref(true)
const tradePlace = ref('')
const contactPhone = ref('')
const buyerRemark = ref('')
const submitting = ref(false)

async function loadItem() {
  loading.value = true
  try {
    const data = await getItemDetail(itemId.value)
    item.value = data
    if (data && data.location) tradePlace.value = data.location
  } catch (e) {
    item.value = null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value) return
  if (!userStore.requireLogin()) return
  if (!item.value) return
  if (Number(item.value.status) !== 2) {
    uni.showToast({ title: '商品当前不可下单', icon: 'none' })
    return
  }
  if (!tradePlace.value.trim()) {
    uni.showToast({ title: '请填写交易地点', icon: 'none' })
    return
  }
  if (contactPhone.value && !/^1[3-9]\d{9}$/.test(contactPhone.value)) {
    uni.showToast({ title: '联系电话格式不正确', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await createOrder({
      itemId: itemId.value,
      tradePlace: tradePlace.value.trim(),
      buyerRemark: [buyerRemark.value.trim(), contactPhone.value ? `联系电话：${contactPhone.value}` : '']
        .filter(Boolean)
        .join('；')
    })
    const orderId = (res && (res.orderId || (res.order && res.order.orderId))) || ''
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => {
      if (orderId) uni.redirectTo({ url: `/pages/order/detail?orderId=${orderId}` })
      else uni.redirectTo({ url: '/pages/order/list?role=buy' })
    }, 700)
  } catch (e) {
    /* 已统一 toast（409 会提示已被预订等） */
  } finally {
    submitting.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

onLoad((options) => {
  userStore.initFromStorage()
  itemId.value = (options && options.itemId) || ''
  if (!itemId.value) {
    loading.value = false
    return
  }
  loadItem()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $cm-page-bg;
}
.skeleton-wrap {
  padding: 24rpx;
}
.card {
  margin: 20rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: $cm-radius;
}
.goods {
  display: flex;
  .cover {
    width: 180rpx;
    height: 180rpx;
    border-radius: $cm-radius-sm;
    background: #eef0f3;
    flex: none;
  }
  .goods-info {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
  }
  .title {
    font-size: 28rpx;
    color: $cm-text;
    line-height: 40rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .cond {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $cm-text-3;
  }
  .price {
    margin-top: auto;
    color: $cm-danger;
    font-size: 30rpx;
    font-weight: 600;
  }
}
.form-item {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  border-bottom: 1rpx solid $cm-border;
  &:last-child {
    border-bottom: none;
  }
  .label {
    width: 160rpx;
    flex: none;
    font-size: 26rpx;
    color: $cm-text;
  }
  .required {
    color: $cm-danger;
  }
  .input {
    flex: 1;
    font-size: 26rpx;
  }
  &.remark-item {
    align-items: flex-start;
    padding: 20rpx 0;
  }
  .textarea {
    flex: 1;
    height: 120rpx;
    font-size: 26rpx;
  }
}
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  color: $cm-text-2;
  padding: 10rpx 0;
  .amount {
    color: $cm-danger;
    font-size: 30rpx;
    font-weight: 600;
  }
  &.sub {
    font-size: 24rpx;
    color: $cm-text-3;
  }
}
.tip {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $cm-border;
  font-size: 21rpx;
  color: $cm-text-4;
  line-height: 34rpx;
}
.bottom-space {
  height: 140rpx;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid $cm-border;
  .total {
    flex: 1;
  }
  .total-label {
    font-size: 24rpx;
    color: $cm-text-2;
  }
  .total-price {
    margin-left: 10rpx;
    color: $cm-danger;
    font-size: 34rpx;
    font-weight: 600;
  }
  .submit-btn {
    width: 300rpx;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    background: $cm-primary;
    color: #fff;
    border-radius: 999rpx;
    font-size: 30rpx;
    font-weight: 600;
  }
}
</style>
