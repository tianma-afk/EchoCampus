<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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

const dropdownOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

const selectedLabel = computed(() => {
  const found = feedbackTypes.find(ft => ft.value === feedbackType.value)
  return found ? found.label : ''
})

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

function selectOption(value: string) {
  feedbackType.value = value
  dropdownOpen.value = false
}

function handleClickOutside(e: Event) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    dropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

async function handleSubmit() {
  if (!content.value.trim()) {
    alert('请输入反馈正文')
    return
  }
  submitting.value = true
  try {
    const res = await axios.post(`${API_BASE_URL}/user/feedbacks/`, {
      landmarkId: props.landmarkId,
      feedbackType: feedbackType.value,
      content: content.value.trim(),
      correctLandmarkName: correctLandmarkName.value.trim() || undefined,
      uploadUrl: props.imageUrl,
    })
    if (res.data.code === '00000') {
      alert('感谢您的反馈，我们会尽快处理')
      emit('done')
    } else {
      alert(res.data.message || '提交失败，请重试')
    }
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
        <div class="custom-select" ref="dropdownRef">
          <button
            type="button"
            class="custom-select-trigger form-select"
            @click.stop="toggleDropdown"
          >
            <span>{{ selectedLabel }}</span>
            <svg
              class="custom-select-arrow"
              :class="{ rotated: dropdownOpen }"
              viewBox="0 0 12 12"
            >
              <path fill="currentColor" d="M2 4l4 4 4-4" />
            </svg>
          </button>
          <div class="custom-select-dropdown" :class="{ open: dropdownOpen }">
            <div
              v-for="ft in feedbackTypes"
              :key="ft.value"
              class="custom-select-option"
              :class="{ selected: feedbackType === ft.value }"
              @click.stop="selectOption(ft.value)"
            >
              {{ ft.label }}
            </div>
          </div>
        </div>
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
  overflow: auto;
  overflow-x: hidden;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.feedback-header {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
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
  width: 100%;
  box-sizing: border-box;
  padding: 20px 16px calc(120px + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  width: 100%;
  box-sizing: border-box;
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
  width: 100% !important;
  max-width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 15px;
  color: #1f2937;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
  -webkit-box-sizing: border-box;
  display: block;
}

.form-select:focus,
.form-input:focus {
  border-color: #059669;
}

.form-input.readonly {
  background: #f9fafb;
  color: #6b7280;
}

.custom-select {
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

.custom-select-trigger {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  border: none;
  position: relative;
}

.custom-select-trigger.form-select {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px 40px 12px 14px;
  font-size: 15px;
  background-color: #fff;
  color: #1f2937;
  width: 100%;
}

.custom-select-trigger.form-select:focus {
  border-color: #059669;
}

.custom-select-arrow {
  width: 12px;
  height: 12px;
  color: #6b7280;
  transition: transform 0.2s ease;
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  flex-shrink: 0;
}

.custom-select-arrow.rotated {
  transform: translateY(-50%) rotate(180deg);
}

.custom-select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  width: 100%;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 50;
  overflow: hidden;
  max-height: 0;
  opacity: 0;
  transition: max-height 0.25s ease, opacity 0.2s ease;
  pointer-events: none;
}

.custom-select-dropdown.open {
  max-height: 240px;
  opacity: 1;
  pointer-events: auto;
  overflow-y: auto;
}

.custom-select-option {
  padding: 12px 14px;
  font-size: 15px;
  color: #1f2937;
  background-color: #ffffff;
  cursor: pointer;
  font-weight: normal;
}

.custom-select-option:hover {
  background-color: #ffffff;
  color: #1f2937;
}

.custom-select-option.selected {
  background-color: #ecfdf5;
  color: #1f2937;
}

.custom-select-option.selected:hover {
  background-color: #ecfdf5;
  color: #1f2937;
}

.form-textarea {
  width: 100%;
  box-sizing: border-box;
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
  width: 100%;
  box-sizing: border-box;
  max-width: 100%;
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
