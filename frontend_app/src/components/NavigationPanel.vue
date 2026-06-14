<script setup lang="ts">
defineProps<{
  destinationName: string
  currentInstruction: string
  nextInstruction: string | null
  remainingDistance: number
  remainingTime: number
  isRouting: boolean
  hasArrived: boolean
  routeDeviation: boolean
  navError: string
}>()

const emit = defineEmits<{
  exit: []
  recenter: []
  replan: []
}>()

function fmtDist(m: number): string {
  if (m >= 1000) return `${(m / 1000).toFixed(1)}公里`
  return `${Math.round(m)}米`
}
function fmtTime(s: number): string {
  if (s < 60) return `${Math.round(s)}秒`
  const m = Math.round(s / 60)
  return `${m}分钟`
}
</script>

<template>
  <div class="nav-panel">
    <div class="nav-top">
      <button class="nav-exit" @click="emit('exit')">&#8592; 退出</button>
      <span class="nav-dest">{{ destinationName }}</span>
      <span class="nav-eta">{{ fmtTime(remainingTime) }}</span>
    </div>

    <div class="nav-body">
      <div class="nav-distance">{{ fmtDist(remainingDistance) }}</div>

      <div v-if="navError" class="nav-error">
        {{ navError }}
        <button class="nav-replan-btn" @click="emit('exit')">关闭</button>
      </div>

      <div v-else-if="isRouting" class="nav-loading">路线计算中...</div>

      <div v-else-if="hasArrived" class="nav-arrived">已到达目的地</div>

      <template v-else>
        <div class="nav-instruction">{{ currentInstruction }}</div>
        <div v-if="nextInstruction" class="nav-next">
          下一步：{{ nextInstruction }}
        </div>

        <div v-if="routeDeviation" class="nav-deviation">
          已偏离路线
          <button class="nav-replan-btn" @click="emit('replan')">重新规划</button>
        </div>
      </template>
    </div>

    <div class="nav-bottom">
      <button class="nav-btn" @click="emit('recenter')">&#x1F4CD; 我的位置</button>
    </div>
  </div>
</template>

<style scoped>
.nav-panel {
  position: fixed;
  bottom: 84px;
  left: 12px;
  right: 12px;
  background: var(--color-bg-card);
  border-radius: 16px;
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.12);
  z-index: 3000;
  overflow: hidden;
}
.nav-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-primary);
  color: #fff;
}
.nav-exit {
  background: none;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.nav-dest {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-eta {
  font-size: 13px;
  opacity: 0.9;
}
.nav-body {
  padding: 16px;
}
.nav-distance {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-heading);
  margin-bottom: 8px;
}
.nav-error {
  font-size: 14px;
  color: #e74c3c;
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-loading,
.nav-arrived {
  font-size: 15px;
  color: var(--color-text-secondary);
}
.nav-arrived {
  color: var(--color-primary);
  font-weight: 600;
}
.nav-instruction {
  font-size: 16px;
  color: var(--color-text-heading);
  font-weight: 500;
  line-height: 1.4;
}
.nav-next {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.nav-deviation {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e74c3c;
  font-size: 14px;
}
.nav-replan-btn {
  background: var(--color-primary);
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
}
.nav-bottom {
  display: flex;
  gap: 8px;
  padding: 0 16px 14px;
}
.nav-btn {
  flex: 1;
  padding: 10px 0;
  border: 1.5px solid var(--color-primary);
  border-radius: 24px;
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
</style>
