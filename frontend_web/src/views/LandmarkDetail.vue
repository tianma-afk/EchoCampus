<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLandmarkDetail, type LandmarkDetailVO } from '../api/landmark'
import MapPicker from '../components/MapPicker.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string

const landmark = ref<LandmarkDetailVO | null>(null)
const loading = ref(true)
const error = ref('')

const mapCoords = computed(() => ({
  lat: landmark.value?.latitude ?? null,
  lng: landmark.value?.longitude ?? null,
}))

onMounted(async () => {
  try {
    const res = await getLandmarkDetail(id)
    landmark.value = res.data
  } catch {
    error.value = '加载地标信息失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="landmark-detail">
    <div class="page-header">
      <div class="page-title">
        <button class="back-btn" @click="router.push('/landmark')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <h1>{{ landmark?.name ?? '地标详情' }}</h1>
      </div>
      <button class="edit-btn" @click="router.push('/landmark/' + id + '/edit')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
        编辑
      </button>
    </div>

    <div v-if="loading" class="state-msg">加载中...</div>
    <div v-else-if="error" class="state-msg error">{{ error }}</div>

    <template v-else-if="landmark">
      <div class="form-card">
        <h2 class="form-section-title">基本信息</h2>

        <div class="form-row">
          <div class="form-group">
            <label>地标名称</label>
            <div class="field-value">{{ landmark.name }}</div>
          </div>
        </div>

        <div class="form-row two-col">
          <div class="form-group">
            <label>分类</label>
            <div class="field-value">{{ landmark.category ?? '-' }}</div>
          </div>
          <div class="form-group">
            <label>大学</label>
            <div class="field-value">{{ landmark.universityName ?? '-' }}</div>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>校区</label>
            <div class="field-value">{{ landmark.campusName ?? '-' }}</div>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>地标描述</label>
            <div class="field-value">{{ landmark.description || '-' }}</div>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>标签</label>
            <div class="field-value">
              <span v-if="landmark.tags?.length">
                <span v-for="tag in landmark.tags" :key="tag" class="tag-chip">{{ tag }}</span>
              </span>
              <span v-else>-</span>
            </div>
          </div>
        </div>
      </div>

      <div class="form-card">
        <h2 class="form-section-title">位置信息</h2>

        <div class="form-row">
          <div class="form-group">
            <label>位置描述</label>
            <div class="field-value">{{ landmark.location || '-' }}</div>
          </div>
        </div>

        <div class="form-row" v-if="mapCoords.lat != null && mapCoords.lng != null">
          <div class="form-group">
            <label>地图位置</label>
            <MapPicker :model-value="mapCoords" readonly />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>楼层</label>
            <div class="field-value" v-if="landmark.floorList?.length">
              <div v-for="f in landmark.floorList" :key="f.id" class="floor-item">
                {{ f.floorName }}（{{ f.floorNumber }}楼）
                <span v-if="f.tags?.length" class="floor-tags">
                  — {{ f.tags.join('、') }}
                </span>
              </div>
            </div>
            <div class="field-value" v-else>-</div>
          </div>
        </div>
      </div>

      <div class="form-card">
        <h2 class="form-section-title">其他信息</h2>

        <div class="form-row two-col">
          <div class="form-group">
            <label>开放时间</label>
            <div class="field-value">{{ landmark.openTime || '-' }}</div>
          </div>
          <div class="form-group">
            <label>开放时间详情</label>
            <div class="field-value">{{ landmark.openTimeDetail || '-' }}</div>
          </div>
        </div>

        <div class="form-row two-col">
          <div class="form-group">
            <label>建成年份</label>
            <div class="field-value">{{ landmark.buildYear || '-' }}</div>
          </div>
          <div class="form-group">
            <label>总楼层数</label>
            <div class="field-value">{{ landmark.totalFloors ?? '-' }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.landmark-detail {
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.02em;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
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

.edit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.edit-btn:hover {
  background: #047857;
}

.edit-btn svg {
  width: 16px;
  height: 16px;
}

.state-msg {
  text-align: center;
  padding: 64px 0;
  color: #9ca3af;
  font-size: 14px;
}

.state-msg.error {
  color: #ef4444;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.form-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecfdf5;
}

.form-row {
  margin-bottom: 18px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-row.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.field-value {
  font-size: 14px;
  color: #1a1a1a;
  padding: 10px 14px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  min-height: 42px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  word-break: break-word;
}

.tag-chip {
  display: inline-block;
  padding: 3px 10px;
  background: #ecfdf5;
  color: #059669;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.floor-item {
  width: 100%;
  font-size: 13px;
}

.floor-tags {
  color: #9ca3af;
  font-size: 12px;
}
</style>
