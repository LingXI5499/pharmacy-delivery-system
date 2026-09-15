<script setup lang="ts">
import { onMounted, ref } from 'vue';import { ElMessage,ElMessageBox } from 'element-plus';import { prescriptionApi } from '@/api'
const rows=ref<any[]>([]),loading=ref(false);const load=async()=>{loading.value=true;try{rows.value=await prescriptionApi.pending()}finally{loading.value=false}};onMounted(load)
const preview=async(id:number)=>{const blob=await prescriptionApi.file(id);window.open(URL.createObjectURL(blob),'_blank','noopener')}
const review=async(row:any,approved:boolean)=>{let reason='';if(!approved){const r=await ElMessageBox.prompt('填写驳回原因','处方审核',{inputValidator:v=>!!v||'原因不能为空'});reason=r.value}await prescriptionApi.review(row.id,approved,reason);ElMessage.success('审核结果已提交');await load()}
</script>
<template><div class="workbench"><h1>药师审方工作台</h1><p>只展示已绑定订单的待审处方；审批通过后才执行 FEFO 库存预占。</p><el-table v-loading="loading" :data="rows"><el-table-column prop="prescriptionNo" label="处方号"/><el-table-column prop="originalFilename" label="原文件名"/><el-table-column prop="createTime" label="上传时间"/><el-table-column label="操作"><template #default="{row}"><el-button @click="preview(row.id)">安全预览</el-button><el-button type="success" @click="review(row,true)">批准</el-button><el-button type="danger" @click="review(row,false)">拒绝</el-button></template></el-table-column></el-table></div></template>
<style scoped>.workbench{max-width:1100px;margin:40px auto;padding:24px;background:#fff;border-radius:16px}.workbench>p{color:#718198}</style>
