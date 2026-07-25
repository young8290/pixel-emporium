import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const files = [
  '../components/business/Banner.vue', '../components/business/Footer.vue', '../components/business/PayStatusCard.vue',
  '../components/business/ProductCard.vue', '../components/business/SearchBar.vue', '../components/business/TopNav.vue',
  '../router/index.js', '../views/CheckoutView.vue', '../views/HomeView.vue', '../views/PayResultView.vue',
  '../views/ProductDetailView.vue', '../views/ProductListView.vue', '../views/user/OverviewView.vue'
]

describe('marketplace copy', () => {
  it('does not describe the marketplace as a farm shop', () => {
    const text = files.map(source).join('\n')
    expect(text).not.toMatch(/田园商店|农场牌|FROM THE FARM|新鲜收成|田园好物|确认收成|给农场主留言/)
  })

  it('keeps the supplied banner complete at its native ratio', () => {
    const banner = source('../components/business/Banner.vue')
    expect(banner).toContain('aspect-ratio:1983/793')
    expect(banner).toContain('height:auto')
    expect(banner).toContain('object-fit:contain')
    expect(banner).not.toContain('object-fit:cover')
  })
})
