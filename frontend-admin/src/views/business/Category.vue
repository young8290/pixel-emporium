<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { categoryTree, deleteCategory, listCategories, saveCategory } from '@/api/business/category'
const tree=ref([]),rows=ref([]),selected=ref(null),loading=ref(true),saving=ref(false)
const form=reactive({id:null,name:'',parentId:0,sort:99,icon:'',enabled:true})
const flat=computed(()=>tree.value.flatMap(a=>[a,...(a.children||[])]))
async function load(){loading.value=true;try{const [t,p]=await Promise.all([categoryTree(),listCategories()]);tree.value=t||[];rows.value=p?.records||p?.list||[]}finally{loading.value=false}}
function choose(node){selected.value=node;Object.assign(form,{id:null,name:'',parentId:0,sort:99,icon:'',enabled:true,...node})}
function createChild(){Object.assign(form,{id:null,name:'',parentId:selected.value?.id||0,sort:99,icon:'',enabled:true})}
async function save(){saving.value=true;try{await saveCategory(form);await load()}finally{saving.value=false}}
async function remove(){await ElMessageBox.confirm('有子分类或商品引用时后端会拒绝删除。确定继续？','删除分类',{type:'warning'});await deleteCategory(form.id);Object.assign(form,{id:null,name:'',parentId:0,sort:99,icon:'',enabled:true});load()}
onMounted(load)
</script>
<template><div class="page"><header><small>CATEGORY TREE</small><h1>分类管理</h1></header><section class="category-layout"><article class="panel"><div class="panel-title"><h2>分类树</h2><el-button size="small" @click="createChild">新增</el-button></div><el-tree v-loading="loading" :data="tree" node-key="id" :props="{label:'name',children:'children'}" default-expand-all highlight-current @node-click="choose"/></article><article class="panel"><h2>{{ form.id?'编辑分类':'新增分类' }}</h2><el-form label-width="80px"><el-form-item label="名称"><el-input v-model="form.name"/></el-form-item><el-form-item label="父级"><el-select v-model="form.parentId"><el-option label="顶级分类" :value="0"/><el-option v-for="item in flat" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0"/></el-form-item><el-form-item label="图标"><el-input v-model="form.icon"/></el-form-item><el-form-item label="启用"><el-switch v-model="form.enabled"/></el-form-item></el-form><div class="actions"><el-button v-if="form.id" type="danger" @click="remove">删除</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></div></article></section></div></template>
<style scoped>.page{display:grid;gap:18px}header small{color:var(--c-moss-dark)}h1,h2{font-family:var(--font-pixel);color:var(--c-wood-dark)}.category-layout{display:grid;grid-template-columns:360px minmax(0,1fr);gap:18px}.panel{padding:18px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.panel-title,.actions{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.actions{justify-content:flex-end}@media(max-width:800px){.category-layout{grid-template-columns:1fr}}</style>
