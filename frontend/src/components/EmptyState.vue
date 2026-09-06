
<script setup lang="ts">
defineProps<{ title?: string; description?: string; actionText?: string }>()
const emit=defineEmits<{action:[]}>()
</script>
<template><div class="empty-state"><div class="empty-orb">+</div><h3>{{ title || '暂无数据' }}</h3><p>{{ description || '换个条件试试看吧' }}</p><el-button v-if="actionText" type="primary" @click="emit('action')">{{ actionText }}</el-button></div></template>
