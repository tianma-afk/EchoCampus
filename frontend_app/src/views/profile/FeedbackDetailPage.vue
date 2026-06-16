<script setup lang="ts">
import { computed } from 'vue'
import { type FeedbackRecord, FEEDBACK_TYPE_MAP, FEEDBACK_STATUS_MAP } from '../../api/feedback'

const props = defineProps<{ feedback: FeedbackRecord }>()
defineEmits<{ back: [] }>()

function typeLabel(t: string) {
  return FEEDBACK_TYPE_MAP[t] || t
}

const statusInfo = computed(() => {
  return FEEDBACK_STATUS_MAP[props.feedback.status] || { label: props.feedback.status, color: '#6b7280', bg: '#f3f4f6' }
})

function formatDateTime(d: string | null) {
  if (!d) return ''
  return d.substring(0, 16).replace('T', ' ')
}
</script>

<template>
  <div class="feedback-detail-page">
    <div class="detail-header">
      <button class="back-btn" @click="$emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="header-title">反馈详情</span>
      <div style="width: 40px" />
    </div>

    <div class="detail-body">
      <!-- 状态标签 -->
      <div class="status-row">
        <span
          class="status-badge"
          :style="{ color: statusInfo.color, background: statusInfo.bg }"
        >{{ statusInfo.label }}</span>
      </div>

      <!-- 基本信息 -->
      <div class="info-section">
        <div class="info-row">
          <span class="info-label">反馈类型</span>
          <span class="info-value">{{ typeLabel(feedback.feedbackType) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">关联地标</span>
          <span class="info-value">{{ feedback.landmarkName || '新增地标建议' }}</span>
        </div>
        <div v-if="feedback.correctLandmarkName" class="info-row">
          <span class="info-label">{{ feedback.feedbackType === 'ADD_LANDMARK' ? '建议地标名称' : '实际建筑' }}</span>
          <span class="info-value">{{ feedback.correctLandmarkName }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">提交时间</span>
          <span class="info-value">{{ formatDateTime(feedback.createdAt) }}</span>
        </div>
      </div>

      <!-- 反馈正文 -->
      <div class="content-section">
        <h3 class="section-title">反馈正文</h3>
        <p class="content-text">{{ feedback.content }}</p>
      </div>

      <!-- 反馈图片 -->
      <div v-if="feedback.uploadUrl" class="content-section">
        <h3 class="section-title">反馈图片</h3>
        <div class="image-wrap">
          <img :src="feedback.uploadUrl" alt="反馈图片" class="feedback-img" />
        </div>
      </div>

      <!-- 处理结果 -->
      <div class="content-section">
        <h3 class="section-title">处理结果</h3>
        <div v-if="feedback.status === 'PENDING'" class="pending-hint">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="hint-icon">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span>管理员正在处理中，请耐心等待</span>
        </div>
        <div v-else class="resolve-info">
          <div class="info-row">
            <span class="info-label">处理时间</span>
            <span class="info-value">{{ formatDateTime(feedback.resolveTime) }}</span>
          </div>
          <div v-if="feedback.resolveNote" class="resolve-note">
            <span class="info-label">处理备注</span>
            <p class="note-text">{{ feedback.resolveNote }}</p>
          </div>
          <div v-else class="resolve-note">
            <span class="info-label">处理备注</span>
            <p class="note-text empty-note">管理员未留下备注</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feedback-detail-page {
  position: fixed;
  inset: 0;
  z-index: 130;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.feedback-detail-page::-webkit-scrollbar { display: none; }

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  flex-shrink: 0;
  background: rgba(250, 248, 245, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.back-btn svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-heading);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.detail-body {
  padding: 0 16px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-row {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.status-badge {
  font-size: 14px;
  font-weight: 600;
  padding: 6px 20px;
  border-radius: 20px;
}

.info-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: var(--color-text-heading);
  font-weight: 500;
  text-align: right;
  max-width: 60%;
}

.content-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0 0 10px;
}

.content-text {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.image-wrap {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.feedback-img {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  display: block;
}

.pending-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-size: 14px;
}

.hint-icon {
  width: 20px;
  height: 20px;
  color: var(--color-primary);
  flex-shrink: 0;
}

.resolve-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resolve-note {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.note-text {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.6;
  margin: 0;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  padding: 12px;
}

.note-text.empty-note {
  color: var(--color-text-muted);
  font-style: italic;
}
</style>
