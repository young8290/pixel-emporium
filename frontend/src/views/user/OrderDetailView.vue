<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelOrder, confirmOrder, getOrder } from '@/api/order'
import { deleteReview, getOrderReviewEligibility } from '@/api/review'
import OrderItemList from '@/components/business/OrderItemList.vue'
import OrderTimeline from '@/components/business/OrderTimeline.vue'
import ProductReviewDialog from '@/components/business/ProductReviewDialog.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelConfirmDialog from '@/components/ui/PixelConfirmDialog.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import network from '@/assets/pixel/illustrations/error-network.png'

const route=useRoute(),router=useRouter()
const order=ref(null),loading=ref(true),failed=ref(false),action=ref({open:false,type:'',loading:false})
const reviewItems=ref([]),reviewDialog=ref({open:false,eligibility:null}),removingReview=ref(null),deletingReview=ref(false)
const reviewsByItem=computed(()=>new Map(reviewItems.value.map(item=>[String(item.orderItemId),item])))
function reviewState(item){return reviewsByItem.value.get(String(item.id))}
async function loadReviewEligibility(){try{const data=await getOrderReviewEligibility(order.value.id);reviewItems.value=data?.items||[]}catch{reviewItems.value=[]}}
async function load(){loading.value=true;failed.value=false;try{order.value=await getOrder(route.params.id);if(order.value.status==='COMPLETED')await loadReviewEligibility();else reviewItems.value=[]}catch(e){failed.value=true;if(e.response?.status===404)router.replace('/404')}finally{loading.value=false}}
function ask(type){action.value={open:true,type,loading:false}}
async function execute(){action.value.loading=true;try{if(action.value.type==='cancel')await cancelOrder(order.value.id);else await confirmOrder(order.value.id);action.value.open=false;await load()}finally{action.value.loading=false}}
function openReview(eligibility){reviewDialog.value={open:true,eligibility}}
async function afterReviewSaved(){reviewDialog.value.open=false;await loadReviewEligibility()}
async function removeReview(){if(!removingReview.value)return;deletingReview.value=true;try{await deleteReview(removingReview.value.reviewId);removingReview.value=null;await loadReviewEligibility()}finally{deletingReview.value=false}}
onMounted(load)
</script>
<template><div class="detail"><PixelSkeleton v-if="loading" :rows="8"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><template v-else>
  <header><div><small>ORDER DETAIL</small><h1>订单 {{ order.orderNo }}</h1></div><PixelButton type="ghost" @click="router.back()">返回</PixelButton></header>
  <section class="panel"><OrderTimeline :status="order.status"/></section>
  <section class="panel"><h2>收货信息</h2><p>{{ order.receiverName }}　{{ order.receiverPhone }}</p><p>{{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverDistrict }}{{ order.receiverDetail }}</p></section>
  <section v-if="order.shippingCarrier || order.trackingNumber" class="panel shipping"><h2>物流信息</h2><p><span>物流公司</span><b>{{ order.shippingCarrier }}</b></p><p><span>运单号</span><b>{{ order.trackingNumber }}</b></p><p v-if="order.shippingNote"><span>发货备注</span><b>{{ order.shippingNote }}</b></p></section>
  <section class="panel"><h2>商品明细</h2><OrderItemList :items="order.items"><template #actions="{item}"><div v-if="reviewState(item)" class="review-actions"><span v-if="reviewState(item).reviewStatus==='HIDDEN'" class="review-hidden">已隐藏：{{ reviewState(item).moderationReason||'内容不符合规范' }}</span><PixelButton v-if="reviewState(item).eligible" size="small" @click="openReview(reviewState(item))">评价商品</PixelButton><PixelButton v-if="reviewState(item).editable" type="ghost" size="small" @click="openReview(reviewState(item))">修改评价</PixelButton><PixelButton v-if="reviewState(item).deletable" type="brick" size="small" @click="removingReview=reviewState(item)">删除评价</PixelButton></div></template></OrderItemList></section>
  <section class="panel totals"><div><span>商品合计</span><b>¥{{ Number(order.totalAmount||0).toFixed(2) }}</b></div><div><span>运费</span><b>¥{{ Number(order.freightAmount||0).toFixed(2) }}</b></div><div class="pay"><span>应付金额</span><b>¥{{ Number(order.payAmount||0).toFixed(2) }}</b></div><p v-if="order.remark">备注：{{ order.remark }}</p></section>
  <footer class="actions"><PixelButton v-if="order.status==='PENDING_PAYMENT'" type="brick" @click="ask('cancel')">取消订单</PixelButton><PixelButton v-if="order.status==='PENDING_PAYMENT'" @click="router.push({path:'/pay',query:{orderNo:order.orderNo}})">立即付款</PixelButton><PixelButton v-if="order.status==='SHIPPED'" @click="ask('confirm')">确认收货</PixelButton></footer>
  <PixelConfirmDialog v-model="action.open" :danger="action.type==='cancel'" :loading="action.loading" :title="action.type==='cancel'?'取消订单':'确认收货'" @confirm="execute" @cancel="action.open=false"/>
  <ProductReviewDialog v-model="reviewDialog.open" :eligibility="reviewDialog.eligibility" @saved="afterReviewSaved"/>
  <PixelConfirmDialog :model-value="!!removingReview" danger :loading="deletingReview" title="删除评价" content="删除后该订单项可以重新评价，确定继续吗？" @confirm="removeReview" @cancel="removingReview=null" @update:model-value="!$event&&(removingReview=null)"/>
</template></div></template>
<style scoped>.detail{display:grid;gap:var(--sp-4)}header{display:flex;justify-content:space-between;align-items:center}header small{color:var(--c-moss-dark)}h1,h2{font-family:var(--font-pixel);color:var(--c-wood-dark)}h1{font-size:26px}.panel{padding:var(--sp-4);border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.panel h2{margin-bottom:12px;font-size:21px}.shipping{display:grid;gap:8px}.shipping p{display:grid;grid-template-columns:90px minmax(0,1fr);gap:12px}.shipping span{color:var(--c-ink-2)}.totals{display:grid;gap:8px;margin-left:auto;width:min(420px,100%)}.totals>div{display:flex;justify-content:space-between}.totals .pay{padding-top:10px;border-top:2px solid var(--c-line);color:var(--c-amber);font-size:20px}.totals p{color:var(--c-ink-2)}.actions{display:flex;justify-content:flex-end;gap:12px}.review-actions{display:flex;justify-content:flex-end;align-items:center;gap:8px;flex-wrap:wrap}.review-hidden{width:100%;color:var(--c-brick);font-size:12px;text-align:right}@media(max-width:600px){header{align-items:flex-start}.actions{position:sticky;bottom:72px;z-index:10;padding:10px;background:var(--c-surface);border:2px solid var(--c-wood-dark)}.review-actions{justify-content:flex-start}.review-hidden{text-align:left}}
</style>
