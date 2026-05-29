<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    placeholder?: string
  }>(),
  {
    placeholder: '输入后点击添加',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const inputText = ref('')

function add() {
  const val = inputText.value.trim()
  if (!val) return
  if (props.modelValue.includes(val)) {
    inputText.value = ''
    return
  }
  emit('update:modelValue', [...props.modelValue, val])
  inputText.value = ''
}

function remove(index: number) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  emit('update:modelValue', next)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    add()
  }
}
</script>

<template>
  <div class="tag-list-input">
    <div class="input-row">
      <input
        v-model="inputText"
        type="text"
        :placeholder="placeholder"
        @keydown="onKeydown"
      />
      <button type="button" class="add-btn" @click="add">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </div>
    <div v-if="modelValue.length > 0" class="tag-row">
      <span v-for="(tag, index) in modelValue" :key="index" class="tag">
        {{ tag }}
        <button type="button" class="tag-remove" @click="remove(index)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </span>
    </div>
  </div>
</template>

<style scoped>
.tag-list-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-row {
  display: flex;
  gap: 8px;
}

.input-row input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.input-row input:focus {
  border-color: #10b981;
}

.input-row input::placeholder {
  color: #d1d5db;
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: none;
  background: #10b981;
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}

.add-btn:hover {
  background: #059669;
}

.add-btn svg {
  width: 18px;
  height: 18px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
}

.tag-remove:hover {
  color: #ef4444;
}

.tag-remove svg {
  width: 12px;
  height: 12px;
}
</style>
