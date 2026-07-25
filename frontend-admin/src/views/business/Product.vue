<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { deleteProduct, listProducts, saveProduct } from '@/api/business/product'
import { categoryTree } from '@/api/business/category'
import FilterBar from '@/components/business/FilterBar.vue'
import ProductForm from '@/components/business/ProductForm.vue'
import ProductImageManager from '@/components/business/ProductImageManager.vue'
import placeholder from '@/assets/pixel/products/product-placeholder.png'
const rows=ref([]),categories=ref([]),total=ref(0),loading=ref(false),dialog=ref(false),saving=ref(false),imageManager=ref(null)
const query=reactive({page:1,size:10,keyword:'',categoryId:null,onSale:null})
const form=reactive({id:null,title:'',description:'',image:'',onSale:true,price:0.01,categoryId:null})
async function load(){loading.value=true;try{const data=await listProducts(query);rows.value=data?.records||data?.list||[];total.value=Number(data?.total||rows.value.length)}finally{loading.value=false}}
function editableProduct(source){return {id:source.id,title:source.title,description:source.description,image:source.image,onSale:source.onSale,price:source.price,categoryId:source.categoryId}}
function edit(row={}){Object.assign(form,editableProduct({id:null,title:'',description:'',image:'',onSale:true,price:0.01,categoryId:null,...row}));dialog.value=true}
async function save(){saving.value=true;try{await saveProduct(editableProduct(form));dialog.value=false;await load()}finally{saving.value=false}}
async function remove(row){await ElMessageBox.confirm(`确定删除商品“${row.title}”？`,'危险操作',{type:'warning'});await deleteProduct(row.id);load()}
async function toggle(row){await saveProduct({...editableProduct(row),onSale:!row.onSale});load()}
function openImages(row){imageManager.value?.show(row)}
onMounted(async()=>{try{categories.value=await categoryTree()}finally{load()}})
</script>
<template><div class="page"><header><small>PRODUCTS</small><h1>商品管理</h1></header><FilterBar><el-input v-model="query.keyword" placeholder="商品关键词" clearable/><el-tree-select v-model="query.categoryId" :data="categories" node-key="id" :props="{label:'name',children:'children'}" clearable placeholder="分类"/><el-select v-model="query.onSale" clearable placeholder="上下架"><el-option label="上架" :value="true"/><el-option label="下架" :value="false"/></el-select><template #actions><el-button @click="load">筛选</el-button><el-button type="primary" @click="edit()">新增商品</el-button></template></FilterBar><section class="table-card"><el-table v-loading="loading" :data="rows"><el-table-column label="封面" width="84"><template #default="{row}"><img class="cover" :src="row.image||placeholder" :alt="row.title" loading="lazy"></template></el-table-column><el-table-column prop="title" label="标题" min-width="180"/><el-table-column prop="categoryName" label="分类" width="130"/><el-table-column label="价格" width="100"><template #default="{row}">¥{{ Number(row.price).toFixed(2) }}</template></el-table-column><el-table-column prop="soldCount" label="销量" width="80"/><el-table-column label="上架" width="90"><template #default="{row}"><el-switch :model-value="row.onSale" @change="toggle(row)"/></template></el-table-column><el-table-column label="操作" width="230" fixed="right"><template #default="{row}"><el-button link @click="edit(row)">编辑</el-button><el-button link @click="openImages(row)">图片</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" @current-change="load"/></section>
<el-dialog v-model="dialog" :title="form.id?'编辑商品':'新增商品'" width="760px"><ProductForm :form="form" :categories="categories"/><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
<ProductImageManager ref="imageManager"/></div></template>
<style scoped>.page{display:grid;gap:18px}header small{color:var(--c-moss-dark)}h1{font-family:var(--font-pixel);color:var(--c-wood-dark)}.table-card{padding:14px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel);overflow:auto}.cover{width:56px;height:56px;border:2px solid var(--c-wood);object-fit:cover}</style>
