<template>
  <footer class="shop-footer">
    <div class="grass" aria-hidden="true"></div>
    <div><strong>像素百货</strong><span>把四季好物，送到你的生活里。</span></div>
    <small>© 2026 Pixel Emporium · 模拟电商项目</small>
  </footer>
</template>
<style scoped>
.shop-footer{margin-top:var(--sp-8);border-top:4px solid var(--c-wood-dark);background:var(--c-wood);color:var(--c-surface);text-align:center}.grass{height:24px;margin-top:-24px;background:url('@/assets/pixel/common/divider-grass.png') center/auto 24px repeat-x}.shop-footer>div:not(.grass){display:flex;justify-content:center;gap:var(--sp-3);padding:var(--sp-5) var(--sp-4) var(--sp-2)}.shop-footer strong{font-family:var(--font-pixel);font-size:20px}.shop-footer small{display:block;padding-bottom:var(--sp-4);opacity:.8}@media(max-width:600px){.shop-footer>div:not(.grass){flex-direction:column}}
</style>
