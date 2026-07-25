<script setup>
import { useThemeStore } from '@/stores/theme'
import PixelTooltip from '@/components/ui/PixelTooltip.vue'

defineProps({ compact: Boolean })

const theme = useThemeStore()
const seasons = [
  { value: 'spring', short: '春', label: '春季' },
  { value: 'summer', short: '夏', label: '夏季' },
  { value: 'autumn', short: '秋', label: '秋季' },
  { value: 'winter', short: '冬', label: '冬季' }
]

function toggleMode() {
  theme.setMode(theme.resolvedMode === 'day' ? 'night' : 'day')
}
</script>

<template>
  <div class="theme-switcher" :class="{ 'theme-switcher--compact': compact }" aria-label="主题设置">
    <div class="theme-switcher__seasons" role="group" aria-label="四季主题">
      <PixelTooltip
        v-for="item in seasons"
        :key="item.value"
        :content="item.label"
        placement="bottom"
      >
        <button
          type="button"
          :aria-label="`切换到${item.label}`"
          :aria-pressed="theme.resolvedSeason === item.value"
          @click="theme.setSeason(item.value)"
        >{{ item.short }}</button>
      </PixelTooltip>
    </div>
    <PixelTooltip :content="theme.resolvedMode === 'day' ? '切换到夜间' : '切换到日间'" placement="bottom">
      <button
        class="theme-switcher__mode"
        type="button"
        :aria-label="theme.resolvedMode === 'day' ? '切换到夜间' : '切换到日间'"
        :aria-pressed="theme.resolvedMode === 'night'"
        @click="toggleMode"
      >{{ theme.resolvedMode === 'day' ? '夜' : '昼' }}</button>
    </PixelTooltip>
    <PixelTooltip content="自动（跟随季节与时间）" placement="bottom">
      <button
        class="theme-switcher__auto"
        type="button"
        aria-label="使用自动主题"
        :aria-pressed="theme.isAutomatic"
        @click="theme.useAutomatic"
      >自</button>
    </PixelTooltip>
  </div>
</template>

<style scoped>
.theme-switcher { display: inline-flex; align-items: center; gap: 4px; color: var(--c-ink); }
.theme-switcher__seasons { display: inline-flex; gap: 2px; }
.theme-switcher button {
  display: grid;
  place-items: center;
  width: 30px;
  min-width: 30px;
  height: 30px;
  min-height: 30px;
  padding: 0;
  border: 2px solid var(--c-wood-dark);
  border-radius: 0;
  background: var(--c-surface);
  color: var(--c-ink);
  font: 12px var(--font-sans);
  box-shadow: 1px 1px 0 var(--c-wood-dark);
}
.theme-switcher button[aria-pressed="true"] { background: var(--c-season); color: var(--c-ink); box-shadow: inset 1px 1px 0 var(--c-wood-dark); }
.theme-switcher__mode[aria-pressed="true"] { background: var(--c-sky); }
.theme-switcher__auto[aria-pressed="true"] { background: var(--c-moss); color: var(--c-surface); }
.theme-switcher--compact { gap: 2px; }
.theme-switcher--compact button { width: 28px; min-width: 28px; height: 28px; min-height: 28px; }
@media (max-width: 420px) {
  .theme-switcher { max-width: 100%; overflow-x: auto; }
}
</style>
