<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getFeedbackList,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_STATUS_MAP,
  type FeedbackAdminVO,
} from '../../api/feedback'

const router = useRouter()

const feedbacks = ref<FeedbackAdminVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filterStatus = ref('')
const filterType = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await getFeedbackList(
      page.value,
      pageSize.value,
      filterStatus.value || undefined,
      filterType.value || undefined,
    )
    feedbacks.value = res.data.records
    total.value = res.data.total
  } catch {
    feedbacks.value = []
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  fetchList()
}

function goToDetail(id: string) {
  router.push(`/feedback/${id}`)
}

function truncateContent(text: string, maxLen: number): string {
  return text.length > maxLen ? text.slice(0, maxLen) + '…' : text
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="feedback-list">
    <div class="page-header">
      <div class="page-title">
        <h1>反馈审核</h1>
        <span class="page-count">共 {{ total }} 条反馈</span>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label>状态</label>
        <select v-model="filterStatus" @change="applyFilters">
          <option value="">全部</option>
          <option value="PENDING">待处理</option>
          <option value="RESOLVED">已解决</option>
          <option value="REJECTED">已驳回</option>
        </select>
      </div>
      <div class="filter-group">
        <label>类型</label>
        <select v-model="filterType" @change="applyFilters">
          <option value="">全部</option>
          <option value="INFO_ERROR">信息错误</option>
          <option value="CONTENT_ILLEGAL">违禁内容</option>
          <option value="INFO_CHANGE">信息变更</option>
          <option value="OTHER">其他</option>
        </select>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table" v-if="!loading && feedbacks.length > 0">
        <thead>
          <tr>
            <th>地标名称</th>
            <th>反馈类型</th>
            <th>内容摘要</th>
            <th>状态</th>
            <th>提交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="fb in feedbacks" :key="fb.id" @click="goToDetail(fb.id)" class="clickable-row">
            <td class="landmark-name">{{ fb.landmarkName }}</td>
            <td>
              <span class="type-tag">{{ FEEDBACK_TYPE_MAP[fb.feedbackType] || fb.feedbackType }}</span>
            </td>
            <td class="content-cell">{{ truncateContent(fb.content, 30) }}</td>
            <td>
              <span class="status-tag" :class="fb.status.toLowerCase()">
                {{ FEEDBACK_STATUS_MAP[fb.status] || fb.status }}
              </span>
            </td>
            <td class="time-cell">{{ new Date(fb.createdAt).toLocaleDateString('zh-CN') }}</td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="loading" class="empty-state">加载中...</div>
      <div v-else class="empty-state">暂无反馈数据</div>

      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="page <= 1" @click="page--; fetchList()">上一页</button>
        <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; fetchList()">下一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feedback-list {
  max-width: 1000px;
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
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.filter-group select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.filter-group select:focus {
  border-color: #10b981;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
  color: #374151;
}

.clickable-row {
  cursor: pointer;
  transition: background 0.15s;
}

.clickable-row:hover {
  background: #f9fafb;
}

.landmark-name {
  font-weight: 500;
  color: #1f2937;
}

.content-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  white-space: nowrap;
  color: #6b7280;
  font-size: 13px;
}

.type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: #f3f4f6;
  color: #374151;
}

.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: #fef3c7;
  color: #d97706;
}

.status-tag.resolved {
  background: #d1fae5;
  color: #059669;
}

.status-tag.rejected {
  background: #fee2e2;
  color: #dc2626;
}

.empty-state {
  padding: 48px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #10b981;
  color: #10b981;
}

.page-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}
</style>
