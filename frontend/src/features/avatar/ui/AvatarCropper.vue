<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useI18n } from "vue-i18n";

const props = defineProps<{
  file: File;
}>();

const emit = defineEmits<{
  cancel: [];
  apply: [file: File, previewUrl: string];
}>();

const { t } = useI18n();
const objectUrl = ref("");
const imageElement = ref<HTMLImageElement | null>(null);
const naturalSize = ref({ width: 1, height: 1 });
const offset = ref({ x: 0, y: 0 });
const zoom = ref(1);
const rotation = ref(0);
const dragStart = ref<{ pointerX: number; pointerY: number; offsetX: number; offsetY: number } | null>(null);
const cropSize = 300;
const outputSize = 512;

const baseSize = computed(() => {
  const rotated = rotation.value % 180 !== 0;
  const width = rotated ? naturalSize.value.height : naturalSize.value.width;
  const height = rotated ? naturalSize.value.width : naturalSize.value.height;
  const scale = Math.max(cropSize / width, cropSize / height);
  return {
    width: naturalSize.value.width * scale,
    height: naturalSize.value.height * scale,
  };
});

const imageStyle = computed(() => ({
  width: `${baseSize.value.width}px`,
  height: `${baseSize.value.height}px`,
  transform: `translate(calc(-50% + ${offset.value.x}px), calc(-50% + ${offset.value.y}px)) scale(${zoom.value}) rotate(${rotation.value}deg)`,
}));

const loadObjectUrl = () => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value);
  objectUrl.value = URL.createObjectURL(props.file);
  offset.value = { x: 0, y: 0 };
  zoom.value = 1;
  rotation.value = 0;
};

watch(() => props.file, loadObjectUrl, { immediate: true });

onBeforeUnmount(() => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value);
  removeDragListeners();
});

const onImageLoad = (event: Event) => {
  const image = event.target as HTMLImageElement;
  naturalSize.value = {
    width: image.naturalWidth || 1,
    height: image.naturalHeight || 1,
  };
  imageElement.value = image;
};

const onPointerDown = (event: PointerEvent) => {
  dragStart.value = {
    pointerX: event.clientX,
    pointerY: event.clientY,
    offsetX: offset.value.x,
    offsetY: offset.value.y,
  };
  window.addEventListener("pointermove", onPointerMove);
  window.addEventListener("pointerup", onPointerUp, { once: true });
};

const onPointerMove = (event: PointerEvent) => {
  if (!dragStart.value) return;
  offset.value = {
    x: dragStart.value.offsetX + event.clientX - dragStart.value.pointerX,
    y: dragStart.value.offsetY + event.clientY - dragStart.value.pointerY,
  };
};

const onPointerUp = () => {
  dragStart.value = null;
  removeDragListeners();
};

function removeDragListeners() {
  window.removeEventListener("pointermove", onPointerMove);
  window.removeEventListener("pointerup", onPointerUp);
}

const rotate = () => {
  rotation.value = (rotation.value + 90) % 360;
};

const applyCrop = async () => {
  const image = imageElement.value;
  if (!image) return;

  const canvas = document.createElement("canvas");
  canvas.width = outputSize;
  canvas.height = outputSize;
  const context = canvas.getContext("2d");
  if (!context) return;

  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, outputSize, outputSize);
  context.translate(outputSize / 2 + (offset.value.x / cropSize) * outputSize, outputSize / 2 + (offset.value.y / cropSize) * outputSize);
  context.rotate((rotation.value * Math.PI) / 180);
  context.scale(zoom.value, zoom.value);

  const drawWidth = (baseSize.value.width / cropSize) * outputSize;
  const drawHeight = (baseSize.value.height / cropSize) * outputSize;
  context.drawImage(image, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight);

  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, props.file.type || "image/jpeg", 0.92));
  if (!blob) return;

  const croppedFile = new File([blob], props.file.name, { type: blob.type || props.file.type });
  emit("apply", croppedFile, URL.createObjectURL(blob));
};
</script>

<template>
  <div class="modal-backdrop minimal" @click.self="emit('cancel')">
    <section class="crop-modal" role="dialog" aria-modal="true" :aria-label="t('profile.cropPhoto')">
      <header class="modal-head">
        <h2>{{ t("profile.cropPhoto") }}</h2>
        <button class="icon-button quiet" type="button" :aria-label="t('common.cancel')" @click="emit('cancel')">
          <i class="pi pi-times"></i>
        </button>
      </header>

      <div class="crop-area">
        <div class="crop-box" @pointerdown="onPointerDown">
          <img :src="objectUrl" alt="" :style="imageStyle" draggable="false" @load="onImageLoad" />
          <span class="crop-mask" aria-hidden="true"></span>
        </div>
        <p class="muted small">{{ t("profile.cropInstructions") }}</p>
      </div>

      <div class="crop-controls">
        <label class="field compact">
          <span>{{ t("profile.zoom") }}</span>
          <input v-model.number="zoom" type="range" min="1" max="3" step="0.01" />
        </label>
        <button class="icon-button" type="button" :aria-label="t('profile.rotate')" @click="rotate">
          <i class="pi pi-refresh"></i>
        </button>
      </div>

      <footer class="modal-foot split">
        <button class="btn btn-ghost" type="button" @click="emit('cancel')">{{ t("common.cancel") }}</button>
        <button class="btn btn-primary" type="button" @click="applyCrop">{{ t("common.apply") }}</button>
      </footer>
    </section>
  </div>
</template>
