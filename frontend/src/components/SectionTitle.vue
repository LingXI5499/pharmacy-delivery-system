
<script setup lang="ts">
defineProps<{ title: string; subtitle?: string; actionText?: string }>()
const emit = defineEmits<{ action: [] }>()
</script>
<template><div class="section-title"><div><h2>{{ title }}</h2><p v-if="subtitle">{{ subtitle }}</p></div><el-button v-if="actionText" text type="primary" @click="emit('action')">{{ actionText }} →</el-button></div></template>
