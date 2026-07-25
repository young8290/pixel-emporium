import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductReviewDialog from '../ProductReviewDialog.vue'

const api = vi.hoisted(() => ({ createReview: vi.fn(), updateReview: vi.fn() }))
vi.mock('@/api/review', () => api)

const dialogStub = {
  props: ['modelValue', 'title'],
  emits: ['update:modelValue', 'cancel'],
  template: '<div v-if="modelValue" class="dialog"><slot/><slot name="footer"/></div>'
}
const textareaStub = {
  props: ['modelValue', 'error'],
  emits: ['update:modelValue'],
  template: '<div><textarea data-test="content" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"/><p v-if="error">{{ error }}</p></div>'
}
const buttonStub = {
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" @click="$emit(\'click\')"><slot/></button>'
}

const mountDialog = (eligibility) => mount(ProductReviewDialog, {
  props: { modelValue: true, eligibility },
  global: { stubs: { PixelDialog: dialogStub, PixelTextarea: textareaStub, PixelButton: buttonStub } }
})

describe('ProductReviewDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.createReview.mockResolvedValue({ id: '1' })
    api.updateReview.mockResolvedValue({ id: '2' })
  })

  it('blocks missing ratings and short content while showing the character count', async () => {
    const wrapper = mountDialog({ orderItemId: '701', productTitle: '像素键盘' })
    expect(wrapper.text()).toContain('0 / 1000')
    await wrapper.get('[data-test="content"]').setValue('短评')
    expect(wrapper.text()).toContain('2 / 1000')
    await wrapper.get('[data-test="submit-review"]').trigger('click')
    expect(api.createReview).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请选择评分')
  })

  it('creates a review without accepting product or order ownership fields', async () => {
    const wrapper = mountDialog({ orderItemId: '701', productId: '301', productTitle: '像素键盘' })
    await wrapper.findAll('[aria-label^="评 "]')[4].trigger('click')
    await wrapper.get('[data-test="content"]').setValue('真实购买后体验很好')
    await wrapper.get('[data-test="submit-review"]').trigger('click')
    await flushPromises()

    expect(api.createReview).toHaveBeenCalledWith({
      orderItemId: '701', rating: 5, content: '真实购买后体验很好'
    })
    expect(wrapper.emitted('saved')).toBeTruthy()
  })

  it('updates an existing published review', async () => {
    const wrapper = mountDialog({
      orderItemId: '701', reviewId: '88', rating: 4,
      content: '原来的有效评价', productTitle: '像素键盘'
    })
    await wrapper.get('[data-test="content"]').setValue('修改后的有效评价')
    await wrapper.get('[data-test="submit-review"]').trigger('click')
    await flushPromises()

    expect(api.updateReview).toHaveBeenCalledWith('88', {
      rating: 4, content: '修改后的有效评价'
    })
  })
})
