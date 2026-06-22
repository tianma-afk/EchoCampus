<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getFeedbackDetail,
  resolveFeedback,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_STATUS_MAP,
  type FeedbackAdminVO,
} from '../../api/feedback'

const route = useRoute()
const router = useRouter()
const feedbackId = route.params.id as string

const feedback = ref<FeedbackAdminVO | null>(null)
const loading = ref(false)

const resolveStatus = ref('RESOLVED')
const resolveNote = ref('')
const submitting = ref(false)
const error = ref('')

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getFeedbackDetail(feedbackId)
    feedback.value = res.data
  } catch {
    feedback.value = null
  } finally {
    loading.value = false
  }
}

async function handleResolve() {
  submitting.value = true
  error.value = ''
  try {
    const res = await resolveFeedback(feedbackId, {
      status: resolveStatus.value,
      resolveNote: resolveNote.value.trim() || undefined,
    })
    if (res.code !== '00000') {
      error.value = res.message
      submitting.value = false
      return
    }
    await fetchDetail()
    resolveStatus.value = 'RESOLVED'
    resolveNote.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="feedback-detail">
    <div class="page-header">
      <div class="page-title">
        <button class="back-btn" @click="router.push('/feedback')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="breadcrumb">
          <span class="breadcrumb-link" @click="router.push('/feedback')">反馈审核</span>
          <span class="breadcrumb-sep">/</span>
          <span class="breadcrumb-current">反馈详情</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <template v-if="feedback">
      <div class="info-card">
        <h2 class="card-title">反馈信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">地标名称</span>
            <span class="info-value">{{ feedback.landmarkName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">反馈类型</span>
            <span class="info-value">{{ FEEDBACK_TYPE_MAP[feedback.feedbackType] || feedback.feedbackType }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">状态</span>
            <span class="status-tag" :class="feedback.status.toLowerCase()">
              {{ FEEDBACK_STATUS_MAP[feedback.status] || feedback.status }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">提交时间</span>
            <span class="info-value">{{ new Date(feedback.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
          <div class="info-item" v-if="feedback.correctLandmarkName">
            <span class="info-label">校正地标</span>
            <span class="info-value">{{ feedback.correctLandmarkName }}</span>
          </div>
          <div class="info-item" v-if="feedback.adminId">
            <span class="info-label">处理人</span>
            <span class="info-value mono-text">{{ feedback.adminId }}</span>
          </div>
          <div class="info-item" v-if="feedback.resolveTime">
            <span class="info-label">处理时间</span>
            <span class="info-value">{{ new Date(feedback.resolveTime).toLocaleString('zh-CN') }}</span>
          </div>
        </div>
        <div class="info-content" v-if="feedback.uploadUrl">
          <span class="info-label">反馈图片</span>
          <img :src="feedback.uploadUrl" class="feedback-image" />
        </div>
        <div class="info-content">
          <span class="info-label">反馈正文</span>
          <p class="content-text">{{ feedback.content }}</p>
        </div>
        <div class="info-content" v-if="feedback.resolveNote">
          <span class="info-label">处理备注</span>
          <p class="content-text resolve-note">{{ feedback.resolveNote }}</p>
        </div>
      </div>

      <div class="info-card" v-if="feedback.status === 'PENDING'">
        <h2 class="card-title">处理反馈</h2>
        <div class="form-group">
          <label>处理结果</label>
          <select v-model="resolveStatus" class="form-select">
            <option value="RESOLVED">已解决</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </div>
        <div class="form-group">
          <label>处理备注</label>
          <textarea
            v-model="resolveNote"
            class="form-textarea"
            placeholder="可选，填写处理说明或驳回原因"
            rows="3"
            maxlength="500"
          ></textarea>
        </div>
        <div v-if="error" class="form-error">{{ error }}</div>
        <div class="form-actions">
          <button class="submit-btn" :disabled="submitting" @click="handleResolve">
            {{ submitting ? '提交中...' : '提交处理' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.feedback-detail {
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #059669;
  color: #059669;
  background: #ecfdf5;
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-link {
  color: #6b7280;
  cursor: pointer;
}

.breadcrumb-link:hover {
  color: #059669;
}

.breadcrumb-sep {
  color: #d1d5db;
}

.breadcrumb-current {
  color: #1a1a1a;
  font-weight: 600;
}

.loading-state {
  padding: 64px 16px;
  text-align: center;
  color: #9ca3af;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecfdf5;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.info-label {
  font-size: 13px;
  font-weight: 500;
  color: #9ca3af;
}

.info-value {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

.mono-text {
  font-family: monospace;
  font-size: 13px;
}

.content-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.resolve-note {
  background: #f9fafb;
  padding: 12px 16px;
  border-radius: 8px;
}

.feedback-image {
  max-width: 240px;
  max-height: 180px;
  border-radius: 8px;
  object-fit: cover;
  margin-top: 4px;
}

.status-tag {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  width: fit-content;
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

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.form-select,
.form-input {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
}

.form-select:focus,
.form-input:focus {
  border-color: #10b981;
}

.form-textarea {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
  resize: vertical;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #10b981;
}

.form-error {
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  padding: 10px 24px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #047857;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
