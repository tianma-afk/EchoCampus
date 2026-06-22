<script setup lang="ts">
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
</script>

<template>
  <div class="layout">
    <Header />
    <div class="layout-body">
      <Sidebar />
      <main class="page-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f7f8fa;
}

.layout-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
  min-width: 0;
}
</style>
