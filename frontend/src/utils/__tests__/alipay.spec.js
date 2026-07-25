import { afterEach, describe, expect, it, vi } from 'vitest'
import { submitAlipayForm } from '../alipay'

describe('submitAlipayForm', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.restoreAllMocks()
  })

  it('rebuilds and submits only a sandbox Alipay form', () => {
    const submit = vi.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => {})

    submitAlipayForm(`
      <form action="https://openapi-sandbox.dl.alipaydev.com/gateway.do" method="post">
        <input type="hidden" name="sign" value="signed-value">
        <script>window.hacked=true</script>
      </form>
    `)

    const form = document.body.querySelector('form')
    expect(form.action).toContain('alipaydev.com/gateway.do')
    expect(form.querySelector('[name="sign"]').value).toBe('signed-value')
    expect(form.querySelector('script')).toBeNull()
    expect(submit).toHaveBeenCalledOnce()
  })

  it('rejects forms that target a non-Alipay host', () => {
    expect(() => submitAlipayForm(
      '<form action="https://evil.example/pay"><input name="sign" value="x"></form>'
    )).toThrow('支付宝支付地址无效')
    expect(document.body.querySelector('form')).toBeNull()
  })
})
