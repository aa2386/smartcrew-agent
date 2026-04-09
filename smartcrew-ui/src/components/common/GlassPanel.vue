<template>
  <section class="glass-panel" :class="panelClass">
    <slot />
  </section>
</template>

<script setup lang="ts">
const props = defineProps<{
  panelClass?: string
}>()

const panelClass = props.panelClass ?? ''
</script>
