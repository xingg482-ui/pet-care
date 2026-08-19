<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { canvasToAvatarFile } from '../utils/avatarImage'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  file: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const cropSize = 320
const outputSize = 512
const imageUrl = ref('')
const imageRef = ref()
const zoom = ref(1)
const dragging = ref(false)
const imageInfo = reactive({
  width: 0,
  height: 0,
  naturalWidth: 0,
  naturalHeight: 0,
  offsetX: 0,
  offsetY: 0,
})
const dragStart = reactive({
  x: 0,
  y: 0,
  offsetX: 0,
  offsetY: 0,
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const imageStyle = computed(() => ({
  width: `${displayWidth()}px`,
  height: `${displayHeight()}px`,
  transform: `translate(calc(-50% + ${imageInfo.offsetX}px), calc(-50% + ${imageInfo.offsetY}px))`,
}))

watch(() => props.file, async (file) => {
  resetCropper()
  if (!file) {
    return
  }
  imageUrl.value = URL.createObjectURL(file)
  await nextTick()
}, { immediate: true })

function resetCropper() {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
  }
  imageUrl.value = ''
  zoom.value = 1
  dragging.value = false
  Object.assign(imageInfo, {
    width: 0,
    height: 0,
    naturalWidth: 0,
    naturalHeight: 0,
    offsetX: 0,
    offsetY: 0,
  })
}

function onImageLoad(event) {
  const image = event.target
  imageInfo.naturalWidth = image.naturalWidth
  imageInfo.naturalHeight = image.naturalHeight
  const aspect = image.naturalWidth / image.naturalHeight
  if (aspect >= 1) {
    imageInfo.height = cropSize
    imageInfo.width = cropSize * aspect
  } else {
    imageInfo.width = cropSize
    imageInfo.height = cropSize / aspect
  }
  clampOffset()
}

function displayWidth() {
  return imageInfo.width * zoom.value
}

function displayHeight() {
  return imageInfo.height * zoom.value
}

function clampOffset() {
  const maxX = Math.max(0, (displayWidth() - cropSize) / 2)
  const maxY = Math.max(0, (displayHeight() - cropSize) / 2)
  imageInfo.offsetX = Math.min(maxX, Math.max(-maxX, imageInfo.offsetX))
  imageInfo.offsetY = Math.min(maxY, Math.max(-maxY, imageInfo.offsetY))
}

function startDrag(event) {
  if (!imageUrl.value) {
    return
  }
  dragging.value = true
  const point = pointerPoint(event)
  dragStart.x = point.x
  dragStart.y = point.y
  dragStart.offsetX = imageInfo.offsetX
  dragStart.offsetY = imageInfo.offsetY
  window.addEventListener('pointermove', onDrag)
  window.addEventListener('pointerup', stopDrag)
}

function onDrag(event) {
  if (!dragging.value) {
    return
  }
  const point = pointerPoint(event)
  imageInfo.offsetX = dragStart.offsetX + point.x - dragStart.x
  imageInfo.offsetY = dragStart.offsetY + point.y - dragStart.y
  clampOffset()
}

function stopDrag() {
  dragging.value = false
  window.removeEventListener('pointermove', onDrag)
  window.removeEventListener('pointerup', stopDrag)
}

function pointerPoint(event) {
  return {
    x: event.clientX,
    y: event.clientY,
  }
}

function closeDialog() {
  emit('cancel')
  dialogVisible.value = false
}

async function confirmCrop() {
  if (!props.file || !imageRef.value || !imageInfo.naturalWidth || !imageInfo.naturalHeight) {
    return
  }
  try {
    const canvas = document.createElement('canvas')
    canvas.width = outputSize
    canvas.height = outputSize
    const context = canvas.getContext('2d')
    const imgLeft = (cropSize - displayWidth()) / 2 + imageInfo.offsetX
    const imgTop = (cropSize - displayHeight()) / 2 + imageInfo.offsetY
    const sourceX = Math.max(0, (-imgLeft / displayWidth()) * imageInfo.naturalWidth)
    const sourceY = Math.max(0, (-imgTop / displayHeight()) * imageInfo.naturalHeight)
    const sourceWidth = Math.min(imageInfo.naturalWidth - sourceX, (cropSize / displayWidth()) * imageInfo.naturalWidth)
    const sourceHeight = Math.min(imageInfo.naturalHeight - sourceY, (cropSize / displayHeight()) * imageInfo.naturalHeight)
    context.drawImage(imageRef.value, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, outputSize, outputSize)
    const file = await canvasToAvatarFile(canvas, props.file.name)
    emit('confirm', {
      file,
      previewUrl: URL.createObjectURL(file),
    })
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '头像裁剪失败，请重新选择')
  }
}

watch(zoom, clampOffset)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="裁剪头像"
    width="520px"
    append-to-body
    destroy-on-close
    @closed="resetCropper"
  >
    <div class="cropper-shell">
      <div class="cropper-stage" @pointerdown.prevent="startDrag">
        <img
          v-if="imageUrl"
          ref="imageRef"
          class="cropper-image"
          :class="{ 'is-dragging': dragging }"
          :src="imageUrl"
          :style="imageStyle"
          alt="待裁剪头像"
          draggable="false"
          @load="onImageLoad"
        />
        <div class="cropper-mask" aria-hidden="true"></div>
      </div>
      <div class="cropper-tools">
        <span>缩放</span>
        <el-slider v-model="zoom" :min="1" :max="3" :step="0.01" />
      </div>
      <p class="cropper-tip">拖动图片调整圆形头像范围，确定后会回到编辑表单预览，保存档案后正式生效。</p>
    </div>
    <template #footer>
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" @click="confirmCrop">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.cropper-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.cropper-stage {
  position: relative;
  width: 320px;
  height: 320px;
  overflow: hidden;
  border-radius: 8px;
  background: #f3f4f6;
  cursor: grab;
  touch-action: none;
}

.cropper-stage:active {
  cursor: grabbing;
}

.cropper-image {
  position: absolute;
  left: 50%;
  top: 50%;
  max-width: none;
  user-select: none;
}

.cropper-image.is-dragging {
  cursor: grabbing;
}

.cropper-mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.cropper-mask::before {
  content: '';
  position: absolute;
  inset: 0;
  border: 2px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 0 999px rgba(17, 24, 39, 0.48);
}

.cropper-tools {
  width: min(100%, 360px);
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  color: #344054;
  font-size: 14px;
}

.cropper-tip {
  width: min(100%, 390px);
  margin: 0;
  color: #667085;
  font-size: 13px;
  line-height: 20px;
  text-align: center;
}

</style>
