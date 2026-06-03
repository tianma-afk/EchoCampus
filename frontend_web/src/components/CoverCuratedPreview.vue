<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, Star, PictureFilled } from '@element-plus/icons-vue'
import { getLandmarkDetail, type LandmarkDetailVO } from '../api/landmark'

const props = defineProps<{
  landmarkId: string
}>()

const router = useRouter()

const detail = ref<LandmarkDetailVO | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getLandmarkDetail(props.landmarkId)
    detail.value = res.data
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})

function goManageImages() {
  router.push(`/landmark/${props.landmarkId}/images`)
}

const hasAnyImage = () => {
  return !!(detail.value?.coverImageUrl || (detail.value?.imgs && detail.value.imgs.length > 0))
}
</script>

<template>
  <div class="cover-curated-preview">
    <div class="preview-header">
      <h3 class="section-title">图片管理</h3>
      <button type="button" class="manage-btn" @click="goManageImages">
        <el-icon :size="16"><Picture /></el-icon>
        管理图片
      </button>
    </div>

    <div v-if="loading" class="state-msg">加载中...</div>

    <div v-else-if="!hasAnyImage()" class="empty-state">
      <el-icon :size="48" color="#d1d5db"><PictureFilled /></el-icon>
      <p>暂未设置封面和精选图片</p>
    </div>

    <template v-else>
      <!-- Cover image -->
      <div v-if="detail?.coverImageUrl" class="cover-section">
        <div class="cover-card">
          <div class="cover-thumb">
            <img :src="detail.coverImageUrl" alt="封面图片" />
            <span class="corner-badge cover-badge" title="封面图片">
              <el-icon :size="14"><Picture /></el-icon>
            </span>
          </div>
          <span class="card-label">封面图片</span>
        </div>
      </div>

      <!-- Curated images -->
      <div
        v-if="detail?.imgs && detail.imgs.length > 0"
        class="curated-section"
      >
        <div class="curated-grid">
          <div
            v-for="(imgUrl, idx) in detail.imgs"
            :key="imgUrl"
            class="curated-card"
          >
            <div class="curated-thumb">
              <img :src="imgUrl" :alt="'精选图片 ' + (idx + 1)" />
              <span class="corner-badge curated-badge" title="精选图片">
                <el-icon :size="14"><Star /></el-icon>
              </span>
            </div>
            <span class="card-label">精选图片 {{ idx + 1 }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.cover-curated-preview {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.manage-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.manage-btn:hover {
  background: #047857;
}

.state-msg {
  text-align: center;
  padding: 32px 0;
  color: #9ca3af;
  font-size: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px 0;
}

.empty-state p {
  color: #9ca3af;
  font-size: 14px;
  margin: 0;
}

.cover-section {
  margin-bottom: 16px;
}

.cover-card {
  width: 100%;
}

.cover-thumb {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #10b981;
}

.cover-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.curated-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.curated-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.curated-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.corner-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-badge {
  background: #10b981;
  color: #fff;
}

.curated-badge {
  background: #f59e0b;
  color: #fff;
}

.card-label {
  display: block;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
}
</style>
