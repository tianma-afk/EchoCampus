<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import TagListInput from './TagListInput.vue'

export interface FloorEntry {
  floorNumber: number
  floorName: string
  tags: string[]
}

const props = withDefaults(
  defineProps<{
    modelValue: FloorEntry[]
  }>(),
  {},
)

const emit = defineEmits<{
  'update:modelValue': [value: FloorEntry[]]
}>()

const indoor = ref(true)

const indoorFloors = ref<FloorEntry[]>([])
const nextUpFloor = ref(1)
const nextDownFloor = ref(-1)

const sortedFloors = computed(() =>
  [...indoorFloors.value].sort((a, b) => b.floorNumber - a.floorNumber),
)

watch(
  () => props.modelValue,
  (val) => {
    if (val.length === 1 && val[0]?.floorNumber === 0) {
      indoor.value = false
    } else if (val.length > 0) {
      indoor.value = true
      indoorFloors.value = val
      const nums = val.map((f) => f.floorNumber)
      const maxAbove = Math.max(0, ...nums)
      const minBelow = Math.min(0, ...nums)
      nextUpFloor.value = maxAbove + 1
      nextDownFloor.value = minBelow - 1
    }
  },
  { immediate: true },
)

function sync() {
  if (!indoor.value) {
    const entry: FloorEntry = { floorNumber: 0, floorName: '室外', tags: [] }
    emit('update:modelValue', [entry])
  } else {
    emit('update:modelValue', indoorFloors.value)
  }
}

function onIndoorChange(val: boolean) {
  indoor.value = val
  if (val) {
    indoorFloors.value = []
    nextUpFloor.value = 1
    nextDownFloor.value = -1
  }
  sync()
}

function addAboveGround() {
  const n = nextUpFloor.value
  indoorFloors.value.push({
    floorNumber: n,
    floorName: `${n}F`,
    tags: [],
  })
  nextUpFloor.value = n + 1
  sync()
}

function addBelowGround() {
  const n = nextDownFloor.value
  indoorFloors.value.push({
    floorNumber: n,
    floorName: `${n}F`,
    tags: [],
  })
  nextDownFloor.value = n - 1
  sync()
}

function removeFloor(floorNumber: number) {
  indoorFloors.value = indoorFloors.value.filter((f) => f.floorNumber !== floorNumber)
  const nums = indoorFloors.value.map((f) => f.floorNumber)
  nextUpFloor.value = (Math.max(0, ...nums, 0)) + 1
  nextDownFloor.value = (Math.min(0, ...nums, 0)) - 1
  sync()
}

function updateFloorTags(floorNumber: number, tags: string[]) {
  const floor = indoorFloors.value.find((f) => f.floorNumber === floorNumber)
  if (floor) {
    floor.tags = tags
    sync()
  }
}

function updateOutdoorTags(tags: string[]) {
  emit('update:modelValue', [{ floorNumber: 0, floorName: '室外', tags }])
}
</script>

<template>
  <div class="floor-manager">
    <div class="indoor-toggle">
      <label class="toggle-label">
        <input type="radio" :value="true" :checked="indoor" @change="onIndoorChange(true)" />
        <span>室内</span>
      </label>
      <label class="toggle-label">
        <input type="radio" :value="false" :checked="!indoor" @change="onIndoorChange(false)" />
        <span>室外</span>
      </label>
    </div>

    <div v-if="!indoor" class="outdoor-section">
      <div class="floor-label">室外</div>
      <TagListInput
        :model-value="modelValue[0]?.tags ?? []"
        placeholder="添加室外标签"
        @update:model-value="updateOutdoorTags"
      />
    </div>

    <div v-else class="indoor-section">
      <div class="floor-buttons">
        <button type="button" class="floor-btn up-btn" @click="addAboveGround">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <polyline points="19 12 12 5 5 12" />
          </svg>
          向上添加楼层
        </button>
        <button type="button" class="floor-btn down-btn" @click="addBelowGround">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="19" x2="12" y2="5" />
            <polyline points="5 12 12 19 19 12" />
          </svg>
          向下添加楼层
        </button>
      </div>

      <div v-if="sortedFloors.length === 0" class="floor-empty">尚未添加楼层，请点击上方按钮添加</div>

      <div v-for="floor in sortedFloors" :key="floor.floorNumber" class="floor-item">
        <div class="floor-header">
          <span class="floor-name">{{ floor.floorName }}</span>
          <button type="button" class="floor-remove-btn" @click="removeFloor(floor.floorNumber)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
        <TagListInput
          :model-value="floor.tags"
          placeholder="添加楼层标签"
          @update:model-value="(tags: string[]) => updateFloorTags(floor.floorNumber, tags)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.floor-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indoor-toggle {
  display: flex;
  gap: 16px;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.toggle-label input[type='radio'] {
  accent-color: #10b981;
}

.outdoor-section,
.indoor-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.floor-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.floor-buttons {
  display: flex;
  gap: 8px;
}

.floor-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.floor-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.floor-btn svg {
  width: 16px;
  height: 16px;
}

.floor-empty {
  padding: 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
}

.floor-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}

.floor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.floor-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.floor-remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
}

.floor-remove-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

.floor-remove-btn svg {
  width: 14px;
  height: 14px;
}
</style>
