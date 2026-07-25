import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const pixelAsset = (path) => resolve(process.cwd(), 'src/assets/pixel', path)
const fontAsset = (name) => resolve(process.cwd(), 'src/assets/fonts', name)

function pngSize(path) {
  const bytes = readFileSync(pixelAsset(path))
  return [bytes.readUInt32BE(16), bytes.readUInt32BE(20)]
}

function expectPng(path, dimensions) {
  const fullPath = pixelAsset(path)
  expect(existsSync(fullPath), `${path} should exist`).toBe(true)
  if (!existsSync(fullPath)) return
  expect(pngSize(path), `${path} should keep its supplied size`).toEqual(dimensions)
}

describe('pixel update assets', () => {
  const exactSizes = {
    'backgrounds/shop-skyline.png': [1600, 160],
    'backgrounds/shop-skyline@2x.png': [3200, 320],
    'common/frame/frame-corner.png': [96, 96],
    'common/frame/frame-corner@2x.png': [192, 192],
    'common/frame/frame-corner-vine.png': [96, 96],
    'common/frame/frame-corner-vine@2x.png': [192, 192],
    'common/frame/frame-garland.png': [256, 48],
    'common/frame/frame-garland@2x.png': [512, 96],
    'common/frame/frame-baseboard.png': [256, 32],
    'common/frame/frame-baseboard@2x.png': [512, 64],
    'interactive/lantern-lit.png': [64, 96],
    'interactive/lantern-lit@2x.png': [128, 192],
    'interactive/lantern-off.png': [64, 96],
    'interactive/lantern-off@2x.png': [128, 192],
    'transition/transition-curtain.png': [128, 128],
    'transition/transition-curtain@2x.png': [256, 256],
    'transition/transition-dissolve-mask.png': [64, 64],
    'transition/transition-dissolve-mask@2x.png': [128, 128],
    'transition/transition-sweep.png': [256, 64],
    'transition/transition-sweep@2x.png': [512, 128],
    'auth/auth-scene-day.png': [960, 1280],
    'auth/auth-scene-day@2x.png': [1920, 2560],
    'auth/auth-scene-night.png': [960, 1280],
    'auth/auth-scene-night@2x.png': [1920, 2560],
    'auth/auth-plaque-9slice.png': [96, 96],
    'auth/auth-plaque-9slice@2x.png': [192, 192],
    'auth/auth-btn-9slice.png': [48, 24],
    'auth/auth-btn-9slice@2x.png': [96, 48],
    'auth/auth-sign-open.png': [64, 48],
    'auth/auth-sign-open@2x.png': [128, 96],
    'auth/auth-cat.png': [48, 48],
    'auth/auth-cat@2x.png': [96, 96],
    'auth/auth-lamp.png': [48, 80],
    'auth/auth-lamp@2x.png': [96, 160],
    'products/product-placeholder.png': [400, 400],
    'products/product-placeholder@2x.png': [800, 800]
  }

  it.each(Object.entries(exactSizes))('keeps %s at its supplied dimensions', (path, dimensions) => {
    expectPng(path, dimensions)
  })

  it.each(['spring', 'summer', 'autumn', 'winter'])('ships the %s wallpaper pair', (season) => {
    expectPng(`backgrounds/wallpaper/wall-${season}.png`, [256, 256])
    expectPng(`backgrounds/wallpaper/wall-${season}@2x.png`, [512, 512])
  })

  it.each(['', '-spring', '-summer', '-autumn', '-winter'])('keeps the banner%s artwork complete', (suffix) => {
    expectPng(`backgrounds/banner-home${suffix}.png`, [1983, 793])
    expectPng(`backgrounds/banner-home${suffix}@2x.png`, [1983, 793])
  })

  it.each(['default', 'pointer', 'text', 'grab', 'grabbing', 'disabled'])('ships the %s cursor pair', (name) => {
    expectPng(`cursors/cursor-${name}.png`, [32, 32])
    expectPng(`cursors/cursor-${name}@2x.png`, [64, 64])
  })

  it.each(['firefly', 'petal', 'leaf', 'snow', 'mote'])('ships the %s particle pair', (name) => {
    expectPng(`interactive/particle-${name}.png`, [16, 16])
    expectPng(`interactive/particle-${name}@2x.png`, [32, 32])
  })

  it('ships every general marketplace category icon', () => {
    const names = [
      'digital', 'appliance', 'apparel', 'home', 'kitchen', 'beauty', 'book', 'toy',
      'sport', 'baby', 'pet', 'grocery', 'health', 'auto', 'craft', 'jewelry',
      'stationery', 'garden', 'other'
    ]
    for (const name of names) {
      expectPng(`categories/cat-${name}.png`, [32, 32])
      expectPng(`categories/cat-${name}@2x.png`, [64, 64])
    }
  })

  it('self-hosts legal pixel fonts without a bundled Zpix derivative', () => {
    expect(existsSync(fontAsset('Silkscreen-Regular.woff2'))).toBe(true)
    expect(existsSync(fontAsset('Silkscreen-Bold.woff2'))).toBe(true)
    expect(existsSync(fontAsset('FusionPixel12px-subset.woff2'))).toBe(true)
    expect(existsSync(fontAsset('Zpix.woff2'))).toBe(false)
  })
})
