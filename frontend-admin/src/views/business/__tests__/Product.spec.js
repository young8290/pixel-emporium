import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Product from '../Product.vue'

const api = vi.hoisted(() => ({
  listProducts: vi.fn(),
  saveProduct: vi.fn(),
  deleteProduct: vi.fn()
}))
const categoryApi = vi.hoisted(() => ({ categoryTree: vi.fn() }))

vi.mock('@/api/business/product', () => api)
vi.mock('@/api/business/category', () => categoryApi)

describe('Product administration write boundary', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listProducts.mockResolvedValue({ list: [], total: 0 })
    api.saveProduct.mockResolvedValue(true)
    categoryApi.categoryTree.mockResolvedValue([])
  })

  it('never submits rating, sales or review aggregates from the editor', async () => {
    const wrapper = mountProduct()
    await flushPromises()
    const row = {
      id: '7', title: '像素果酱', description: '草莓味', image: 'jam.png', onSale: true,
      price: 12.8, categoryId: '3', rating: 4.9, soldCount: 88, reviewCount: 20
    }

    wrapper.vm.edit(row)
    await wrapper.vm.save()

    expect(api.saveProduct).toHaveBeenCalledWith({
      id: '7', title: '像素果酱', description: '草莓味', image: 'jam.png', onSale: true,
      price: 12.8, categoryId: '3'
    })
  })
})

function mountProduct() {
  return mount(Product, {
    global: {
      stubs: {
        FilterBar: { template: '<div><slot/><slot name="actions"/></div>' },
        ProductForm: true,
        ProductImageManager: true,
        'el-input': true,
        'el-tree-select': true,
        'el-select': { template: '<select><slot/></select>' },
        'el-option': true,
        'el-button': { template: '<button><slot/></button>' },
        'el-table': { template: '<div><slot/></div>' },
        'el-table-column': true,
        'el-switch': true,
        'el-pagination': true,
        'el-dialog': { template: '<div><slot/><slot name="footer"/></div>' }
      }
    }
  })
}
