<template>
  <el-container style="height:100vh">
    <el-aside width="240px" class="layout-aside">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
</script>

<style>
.layout-aside { background:#001529; overflow:hidden; }
.layout-main { background:#f0f2f5; padding:20px; min-height:100vh; }
</style>
