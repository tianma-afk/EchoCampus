<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

const props = defineProps<{
  landmarkId: string
  landmarkName: string
  imageUrl: string
}>()

const emit = defineEmits<{
  done: []
  back: []
}>()

const API_BASE_URL = 'http://localhost:8080/api/v1'

const feedbackType = ref('INFO_ERROR')
const correctLandmarkName = ref('')
const content = ref('')
const submitting = ref(false)

const feedbackTypes = [
  { value: 'INFO_ERROR', label: '信息错误' },
  { value: 'CONTENT_ILLEGAL', label: '违禁内容' },
  { value: 'INFO_CHANGE', label: '信息变更' },
  { value: 'OTHER', label: '其他' },
]

async function handleSubmit() {
  if (!content.value.trim()) {
    alert('请输入反馈正文')
    return
  }
  submitting.value = true
  try {
    await axios.post(`${API_BASE_URL}/user/feedbacks/`, {
      landmarkId: props.landmarkId,
      feedbackType: feedbackType.value,
      content: content.value.trim(),
      correctLandmarkName: correctLandmarkName.value.trim() || undefined,
      uploadUrl: props.imageUrl,
    })
    alert('感谢您的反馈，我们会尽快处理')
    emit('done')
  } catch (e) {
    alert('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="feedback-page">
    <div class="feedback-header">
      <button class="back-btn" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h2 class="header-title">纠正反馈</h2>
      <div class="header-spacer"></div>
    </div>

    <div class="feedback-form">
      <div class="form-group">
        <label class="form-label">反馈类型</label>
        <select v-model="feedbackType" class="form-select">
          <option v-for="ft in feedbackTypes" :key="ft.value" :value="ft.value">
            {{ ft.label }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">识别结果</label>
        <input type="text" class="form-input readonly" :value="landmarkName" readonly />
      </div>

      <div class="form-group">
        <label class="form-label">实际建筑</label>
        <input
          v-model="correctLandmarkName"
          type="text"
          class="form-input"
          placeholder="请输入真正的地标名称"
        />
      </div>

      <div class="form-group">
        <label class="form-label">反馈正文</label>
        <textarea
          v-model="content"
          class="form-textarea"
          placeholder="请描述您发现的错误信息..."
          rows="4"
          maxlength="2000"
        ></textarea>
      </div>

      <div class="form-group">
        <label class="form-label">反馈图片</label>
        <div class="image-preview">
          <img :src="imageUrl" alt="反馈图片" class="feedback-img" />
        </div>
      </div>

      <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '完成' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.feedback-page {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: #f3f4f6;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.feedback-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: #374151;
  cursor: pointer;
}

.back-btn svg {
  width: 22px;
  height: 22px;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.header-spacer {
  width: 36px;
}

.feedback-form {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.form-select,
.form-input {
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 15px;
  color: #1f2937;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
}

.form-select:focus,
.form-input:focus {
  border-color: #059669;
}

.form-input.readonly {
  background: #f9fafb;
  color: #6b7280;
}

.form-textarea {
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 15px;
  color: #1f2937;
  outline: none;
  transition: border-color 0.2s;
  resize: vertical;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: #059669;
}

.image-preview {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.feedback-img {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  display: block;
}

.submit-btn {
  padding: 14px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: #047857;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
