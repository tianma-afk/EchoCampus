<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

export interface SelectOption {
  id: string
  name: string
}

const props = withDefaults(
  defineProps<{
    modelValue: SelectOption | null
    options: SelectOption[]
    placeholder?: string
    disabled?: boolean
    loading?: boolean
  }>(),
  {
    placeholder: '请输入关键词搜索',
    disabled: false,
    loading: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: SelectOption | null]
  search: [keyword: string]
}>()

const inputText = ref('')
const isOpen = ref(false)
const highlightIndex = ref(-1)
const wrapperRef = ref<HTMLElement | null>(null)

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      inputText.value = val.name
    }
  },
)

function onInput(e: Event) {
  const value = (e.target as HTMLInputElement).value
  inputText.value = value
  highlightIndex.value = -1
  isOpen.value = true
  emit('search', value)
}

function onFocus() {
  if (!props.disabled && props.options.length > 0) {
    isOpen.value = true
  }
}

function selectOption(option: SelectOption) {
  emit('update:modelValue', option)
  inputText.value = option.name
  isOpen.value = false
  highlightIndex.value = -1
}

function clearSelection() {
  emit('update:modelValue', null)
  inputText.value = ''
  isOpen.value = false
  emit('search', '')
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    highlightIndex.value = Math.min(highlightIndex.value + 1, props.options.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    highlightIndex.value = Math.max(highlightIndex.value - 1, 0)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (highlightIndex.value >= 0 && highlightIndex.value < props.options.length) {
      const opt = props.options[highlightIndex.value]
      if (opt) selectOption(opt)
    }
  } else if (e.key === 'Escape') {
    isOpen.value = false
    highlightIndex.value = -1
  }
}

function onClickOutside(e: MouseEvent) {
  if (wrapperRef.value && !wrapperRef.value.contains(e.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
})
</script>

<template>
  <div
    ref="wrapperRef"
    class="searchable-select"
    :class="{ disabled, open: isOpen }"
  >
    <div class="input-wrapper">
      <input
        type="text"
        :value="inputText"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="onInput"
        @focus="onFocus"
        @keydown="onKeydown"
      />
      <span v-if="loading" class="spinner"></span>
      <button
        v-if="modelValue && !disabled"
        type="button"
        class="clear-btn"
        @click.stop="clearSelection"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
    <ul v-if="isOpen && options.length > 0" class="dropdown">
      <li
        v-for="(option, index) in options"
        :key="option.id"
        class="dropdown-item"
        :class="{ highlighted: index === highlightIndex }"
        @mousedown.prevent="selectOption(option)"
      >
        {{ option.name }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
.searchable-select {
  position: relative;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper input {
  width: 100%;
  padding: 10px 32px 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.input-wrapper input:focus {
  border-color: #10b981;
}

.searchable-select.disabled .input-wrapper input {
  background: #f9fafb;
  color: #d1d5db;
  cursor: not-allowed;
}

.searchable-select.open .input-wrapper input {
  border-color: #10b981;
}

.input-wrapper input::placeholder {
  color: #d1d5db;
}

.spinner {
  position: absolute;
  right: 10px;
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.clear-btn {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
}

.clear-btn:hover {
  color: #ef4444;
}

.clear-btn svg {
  width: 14px;
  height: 14px;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  max-height: 200px;
  overflow-y: auto;
  z-index: 50;
  list-style: none;
  padding: 4px 0;
}

.dropdown-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-item:hover,
.dropdown-item.highlighted {
  background: #ecfdf5;
  color: #059669;
}
</style>
