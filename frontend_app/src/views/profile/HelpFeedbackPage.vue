<script setup lang="ts">
import { ref } from 'vue'
import FeedbackPage from '../feedback/FeedbackPage.vue'

defineEmits<{ back: [] }>()

const showFeedback = ref(false)
</script>

<template>
  <div class="help-page">
    <div class="help-header">
      <button class="help-back" @click="$emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="help-title">帮助与反馈</span>
      <div style="width: 40px" />
    </div>

    <div class="help-body">
      <div class="section">
        <h3 class="section-title">项目介绍</h3>
        <div class="content-card">
          <p class="content-text">
            EchoCampus 是一款面向高校师生的校园地标识别与信息服务平台。
            通过拍照识别校园建筑、雕塑等地标，获取详细介绍、历史背景和校园文化资讯。
            支持地标收藏、评分打卡、信息纠错等功能，旨在帮助师生和访客更好地了解和探索校园。
          </p>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">用户隐私声明</h3>
        <div class="content-card">
          <p class="content-text">
            我们重视您的隐私。本应用仅在您主动使用拍照识别功能时采集图片数据，
            图片仅用于地标识别匹配，不会用于其他用途或与第三方共享。
            您在使用打卡、收藏、评分等功能时产生的数据仅用于提供个性化服务体验。
            您的账号信息（邮箱、昵称）仅用于身份识别和登录验证，不会对外公开。
            如您需要删除账号及所有相关数据，请联系管理员处理。
          </p>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">联系与反馈</h3>
        <div class="content-card">
          <p class="content-text">
            如您在使用过程中遇到任何问题，或有功能建议、地标纠错需求，欢迎提交反馈。
          </p>
          <span class="feedback-text-link" @click="showFeedback = true">有问题？去反馈</span>
        </div>
      </div>
    </div>

    <FeedbackPage
      v-if="showFeedback"
      default-type="OTHER"
      @back="showFeedback = false"
      @done="showFeedback = false"
    />
  </div>
</template>

<style scoped>
.help-page {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.help-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  flex-shrink: 0;
}

.help-back {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.help-back svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-heading);
}

.help-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.help-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.help-body::-webkit-scrollbar { display: none; }

.section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0;
  padding-left: 4px;
}

.content-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-heading);
  margin: 0;
}

.feedback-text-link {
  display: inline-block;
  font-size: 14px;
  color: var(--color-primary);
  cursor: pointer;
}

.feedback-text-link:active {
  opacity: 0.7;
}
</style>
