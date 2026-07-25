<script setup>
import AmbientParticles from '@/components/ambient/AmbientParticles.vue'
import NightOverlay from '@/components/ambient/NightOverlay.vue'
import PageTransition from '@/components/ambient/PageTransition.vue'
import SeasonalDecor from '@/components/ambient/SeasonalDecor.vue'
import ShopLights from '@/components/ambient/ShopLights.vue'
import ViewportFrame from '@/components/ambient/ViewportFrame.vue'
import WallpaperLayer from '@/components/ambient/WallpaperLayer.vue'
</script>

<template>
  <WallpaperLayer />
  <ViewportFrame />
  <AmbientParticles />
  <SeasonalDecor />
  <PageTransition>
    <RouterView />
  </PageTransition>
  <ShopLights />
  <NightOverlay />
</template>
