<script setup>
import { useRoute } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'

const route = useRoute()
</script>

<template>
  <AppLayout>
    <h1 class="page-title">{{ route.meta.title }}</h1>
    <el-empty description="后续功能开发时在这里实现列表、查询、表单和操作按钮" />
  </AppLayout>
</template>
