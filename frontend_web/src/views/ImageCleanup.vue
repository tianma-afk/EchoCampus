<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPendingImages, batchDeletePendingImages, type PendingImage } from '../api/cleanup'

const images = ref<PendingImage[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const deleting = ref(false)
const selectedIds = ref<Set<string>>(new Set())

const selectedCount = computed(() => selectedIds.value.size)

async function fetchList() {
  loading.value = true
  try {
    const res = await getPendingImages(page.value, pageSize.value)
    images.value = res.data.records
    total.value = res.data.total
    selectedIds.value = new Set()
  } catch {
    images.value = []
  } finally {
    loading.value = false
  }
}

function toggleSelect(id: string) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

function selectAll() {
  const all = images.value.map(r => r.id)
  if (selectedIds.value.size === all.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(all)
  }
}

async function handleBatchDelete() {
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return
  if (!confirm(`确定删除 Milvus 中选中的 ${ids.length} 条向量吗？`)) return
  deleting.value = true
  try {
    const res = await batchDeletePendingImages(ids)
    if (res.code === '00000') {
      await fetchList()
    }
  } catch {
    alert('删除失败，请重试')
  } finally {
    deleting.value = false
  }
}

function shortId(id: string) {
  return id.substring(0, 8) + '...'
}

function formatTime(t: string) {
  return new Date(t).toLocaleString()
}

onMounted(fetchList)
</script>

<template>
  <div class="cleanup-page">
    <div class="page-header">
      <div class="page-title">
        <h1>图片清理</h1>
        <span class="page-count">共 {{ total }} 条待清理</span>
      </div>
      <div class="page-actions">
        <el-button
          type="danger"
          :disabled="selectedCount === 0"
          :loading="deleting"
          @click="handleBatchDelete"
        >
          {{ deleting ? '删除中...' : `批量删除（${selectedCount}）` }}
        </el-button>
      </div>
    </div>

    <div class="tip-card">
      <el-alert
        title="说明"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          以下图片已被管理员从地标中删除（MinIO 和数据库），但 Milvus 中仍残留向量数据。
          选中后点击「批量删除」同步清理 Milvus，释放搜索资源。
        </template>
      </el-alert>
    </div>

    <div class="table-card">
      <el-table
        :data="images"
        v-loading="loading"
        @selection-change="(rows: any[]) => selectedIds = new Set(rows.map(r => r.id))"
      >
        <template #empty>
          <el-empty description="暂无待清理图片">
            <template #image>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" style="width: 64px; height: 64px; color: #d1d5db;">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </template>
            <el-button type="primary" @click="fetchList">刷新</el-button>
          </el-empty>
        </template>

        <el-table-column type="selection" width="50" />

        <el-table-column label="图片 UUID (Milvus)" min-width="300">
          <template #default="{ row }">
            <span class="mono" :title="row.imageId">{{ row.imageId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="记录 ID" min-width="180">
          <template #default="{ row }">
            <span class="mono muted">{{ shortId(row.id) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="删除时间" min-width="180">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.cleanup-page {
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip-card {
  margin-bottom: 20px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.mono {
  font-family: 'SF Mono', 'Cascadia Code', 'JetBrains Mono', 'Courier New', monospace;
  font-size: 13px;
}

.mono.muted {
  color: #9ca3af;
}

.time-cell {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.table-footer {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-table th.el-table__cell) {
  background: #f8f9fb;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  letter-spacing: 0.02em;
}

:deep(.el-table .el-table__cell) {
  padding: 14px 16px;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f8f9fb;
}

:deep(.el-table__body tr) {
  transition: background 0.15s;
}
</style>
