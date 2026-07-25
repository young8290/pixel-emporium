import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { resolveMode, resolveSeason, useThemeStore } from '../theme'

describe('theme store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.documentElement.removeAttribute('data-mode')
    document.documentElement.removeAttribute('data-season')
  })

  it('resolves month and clock defaults', () => {
    expect(resolveSeason(new Date(2026, 2, 1))).toBe('spring')
    expect(resolveSeason(new Date(2026, 6, 1))).toBe('summer')
    expect(resolveSeason(new Date(2026, 9, 1))).toBe('autumn')
    expect(resolveSeason(new Date(2026, 0, 1))).toBe('winter')
    expect(resolveMode(new Date(2026, 0, 1, 19))).toBe('night')
    expect(resolveMode(new Date(2026, 0, 1, 12))).toBe('day')
  })

  it('persists manual values and applies html attributes', () => {
    const store = useThemeStore()
    store.setSeason('winter')
    store.setMode('night')

    expect(document.documentElement.dataset.season).toBe('winter')
    expect(document.documentElement.dataset.mode).toBe('night')
    expect(JSON.parse(localStorage.getItem('theme'))).toMatchObject({
      season: 'winter',
      mode: 'night'
    })
  })

  it('hydrates safely, validates values and restores automatic values', () => {
    localStorage.setItem('theme', '{broken')
    const store = useThemeStore()
    expect(() => store.hydrate()).not.toThrow()

    localStorage.setItem('theme', JSON.stringify({ season: 'monsoon', mode: 'neon' }))
    store.hydrate()
    expect(store.season).toBe('')
    expect(store.mode).toBe('')

    store.setSeason('spring')
    store.setMode('night')
    store.useAutomatic()
    expect(store.season).toBe('')
    expect(store.mode).toBe('')
  })

  it('persists a clamped lantern state separately from automatic theme values', () => {
    const store = useThemeStore()
    store.setLantern({ x: 120, y: 240, on: false })
    const saved = JSON.parse(localStorage.getItem('theme'))

    expect(saved.lantern).toEqual({ x: 120, y: 240, on: false })
    expect(store.resolvedSeason).toMatch(/spring|summer|autumn|winter/)
    expect(store.resolvedMode).toMatch(/day|night/)
  })
})
