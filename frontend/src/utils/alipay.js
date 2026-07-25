const ALIPAY_HOST_SUFFIXES = ['.alipay.com', '.alipaydev.com']

export function submitAlipayForm(formHtml) {
  const documentNode = new DOMParser().parseFromString(formHtml || '', 'text/html')
  const source = documentNode.querySelector('form')
  if (!source) throw new Error('支付宝支付表单无效')

  const action = new URL(source.getAttribute('action') || '', window.location.origin)
  if (action.protocol !== 'https:' || !isAlipayHost(action.hostname)) {
    throw new Error('支付宝支付地址无效')
  }

  const form = document.createElement('form')
  form.action = action.href
  form.method = source.method?.toLowerCase() === 'get' ? 'get' : 'post'
  form.hidden = true
  for (const field of source.querySelectorAll('input[name]')) {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = field.name
    input.value = field.value
    form.append(input)
  }
  document.body.append(form)
  form.submit()
}

function isAlipayHost(hostname) {
  return hostname === 'alipay.com'
    || hostname === 'alipaydev.com'
    || ALIPAY_HOST_SUFFIXES.some(suffix => hostname.endsWith(suffix))
}
