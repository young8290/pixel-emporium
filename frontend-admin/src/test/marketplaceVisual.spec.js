import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const asset = (path) => fileURLToPath(new URL(path, import.meta.url))

describe('admin marketplace visual baseline', () => {
  it('mounts the restrained viewport frame without ambient effects', () => {
    expect(source('../App.vue')).toContain('AdminViewportFrame')
    const frame = source('../components/ambient/AdminViewportFrame.vue')
    expect(frame).toContain("['top-left','top-right','bottom-left','bottom-right']")
    expect(frame).toContain('admin-frame__baseboard')
    expect(frame).not.toMatch(/particle|lantern|garland/i)
  })

  it('uses the marketplace brand and keeps the supplied admin banner complete', () => {
    const text = [source('../router/index.js'), source('../components/business/AdminSidebar.vue'), source('../components/business/AdminTopbar.vue'), source('../views/LoginView.vue')].join('\n')
    expect(text).not.toContain('田园商店')
    const login = source('../views/LoginView.vue')
    expect(login).toContain('object-fit:contain')
    expect(login).not.toContain('object-fit:cover')
  })

  it('ships the self-hosted fonts and cursor pairs', () => {
    for (const font of ['Silkscreen-Regular.woff2', 'Silkscreen-Bold.woff2', 'FusionPixel12px-subset.woff2']) {
      expect(existsSync(asset(`../assets/fonts/${font}`))).toBe(true)
    }
    for (const name of ['default', 'pointer', 'text', 'grab', 'grabbing', 'disabled']) {
      expect(existsSync(asset(`../assets/pixel/cursors/cursor-${name}.png`))).toBe(true)
      expect(existsSync(asset(`../assets/pixel/cursors/cursor-${name}@2x.png`))).toBe(true)
    }
  })
})
