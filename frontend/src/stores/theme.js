import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'theme'
const SEASONS = ['spring', 'summer', 'autumn', 'winter']
const MODES = ['day', 'night']

export function resolveSeason(date = new Date()) {
  const month = date.getMonth()
  if (month >= 2 && month <= 4) return 'spring'
  if (month >= 5 && month <= 7) return 'summer'
  if (month >= 8 && month <= 10) return 'autumn'
  return 'winter'
}

export function resolveMode(date = new Date()) {
  const hour = date.getHours()
  return hour >= 18 || hour < 6 ? 'night' : 'day'
}

function finiteCoordinate(value) {
  return Number.isFinite(value) ? value : null
}

export const useThemeStore = defineStore('theme', () => {
  const season = ref('')
  const mode = ref('')
  const lantern = ref({ x: null, y: null, on: true })

  const resolvedSeason = computed(() => season.value || resolveSeason(new Date()))
  const resolvedMode = computed(() => mode.value || resolveMode(new Date()))
  const isAutomatic = computed(() => season.value === '' && mode.value === '')

  function apply() {
    document.documentElement.dataset.season = resolvedSeason.value
    document.documentElement.dataset.mode = resolvedMode.value
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      season: season.value,
      mode: mode.value,
      lantern: lantern.value
    }))
  }

  function hydrate() {
    let saved
    try {
      saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    } catch {
      saved = null
    }

    season.value = SEASONS.includes(saved?.season) ? saved.season : ''
    mode.value = MODES.includes(saved?.mode) ? saved.mode : ''
    lantern.value = {
      x: finiteCoordinate(saved?.lantern?.x),
      y: finiteCoordinate(saved?.lantern?.y),
      on: typeof saved?.lantern?.on === 'boolean' ? saved.lantern.on : true
    }
    apply()
  }

  function setSeason(value) {
    season.value = SEASONS.includes(value) ? value : ''
    persist()
    apply()
  }

  function setMode(value) {
    mode.value = MODES.includes(value) ? value : ''
    persist()
    apply()
  }

  function useAutomatic() {
    season.value = ''
    mode.value = ''
    persist()
    apply()
  }

  function setLantern(value = {}) {
    lantern.value = {
      x: value.x === undefined ? lantern.value.x : finiteCoordinate(value.x),
      y: value.y === undefined ? lantern.value.y : finiteCoordinate(value.y),
      on: typeof value.on === 'boolean' ? value.on : lantern.value.on
    }
    persist()
  }

  return {
    season,
    mode,
    lantern,
    resolvedSeason,
    resolvedMode,
    isAutomatic,
    apply,
    persist,
    hydrate,
    setSeason,
    setMode,
    useAutomatic,
    setLantern
  }
})
