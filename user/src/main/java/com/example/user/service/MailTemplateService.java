package com.example.user.service;

import com.example.user.config.MailCenterProperties;
import com.example.user.enums.MailTemplateKey;
import com.example.user.model.OrderCreatedMailEvent;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;

@Service
public class MailTemplateService {
    private final MailCenterProperties properties; private final SpringTemplateEngine engine;
    public MailTemplateService(MailCenterProperties properties, SpringTemplateEngine engine) { this.properties = properties; this.engine = engine; }
    public RenderedMail verification(String code) { Context ctx=new Context();ctx.setVariable("code",code);return new RenderedMail("【像素百货】注册验证码",engine.process("mail/verification-code",ctx), "像素百货注册验证码：" + code); }
    public RenderedMail orderCreated(OrderCreatedMailEvent event) {
        String link = storefrontBase() + "/user/orders/" + event.getOrderId();
        List<ItemView> items = event.getItems() == null ? List.of() : event.getItems().stream()
                .map(item -> new ItemView(item.getProductTitle(), item.getSkuTitle(), imageUrl(item.getImage()),
                        money(item.getPrice()), item.getQuantity(), money(item.getSubtotal())))
                .toList();
        Context ctx=new Context();ctx.setVariable("event",event);ctx.setVariable("items",items);ctx.setVariable("orderUrl",link);ctx.setVariable("total",money(event.getTotalAmount()));ctx.setVariable("freight",money(event.getFreightAmount()));ctx.setVariable("pay",money(event.getPayAmount()));
        String details = items.stream().map(item -> "商品：" + item.productTitle + "，规格：" + item.skuTitle + "，单价：" + item.price + "，数量：" + item.quantity + "，小计：" + item.subtotal).reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
        OrderCreatedMailEvent.Receiver receiver = event.getReceiver();
        String recipient = receiver == null ? "" : receiver.getName() + " " + receiver.getPhone() + " " + receiver.getProvince() + " " + receiver.getCity() + " " + receiver.getDistrict() + " " + receiver.getDetail();
        String text = "订单 " + event.getOrderNo() + " 已创建，下单时间：" + event.getCreatedAt() + "\n" + details + "\n总额：" + money(event.getTotalAmount()) + "，运费：" + money(event.getFreightAmount()) + "，应付：" + money(event.getPayAmount()) + "\n收货信息：" + recipient;
        return new RenderedMail("【像素百货】订单 " + event.getOrderNo() + " 已创建", engine.process("mail/order-created",ctx), text);
    }
    public String preview(MailTemplateKey key) { return key == MailTemplateKey.VERIFICATION_CODE ? verification("123456").html() : previewOrder(); }
    private String previewOrder(){OrderCreatedMailEvent e=new OrderCreatedMailEvent();e.setOrderId(1L);e.setOrderNo("20260724193842");e.setCreatedAt(java.time.OffsetDateTime.now());e.setTotalAmount(new java.math.BigDecimal("198.00"));e.setFreightAmount(java.math.BigDecimal.ZERO);e.setPayAmount(new java.math.BigDecimal("198.00"));e.setItems(java.util.List.of());OrderCreatedMailEvent.Receiver r=new OrderCreatedMailEvent.Receiver();r.setName("示例用户");r.setPhone("13800000000");r.setProvince("广东省");r.setCity("深圳市");r.setDistrict("南山区");r.setDetail("示例路 1 号");e.setReceiver(r);return orderCreated(e).html();}
    private String storefrontBase() { return validBase(properties.links() == null ? null : properties.links().storefrontBaseUrl()); }
    private String imageUrl(String image) { if (image == null || image.isBlank()) return null; try { URI uri = URI.create(image); if (uri.isAbsolute()) return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) ? uri.toString() : null; return URI.create(validBase(properties.links() == null ? null : properties.links().assetBaseUrl()) + "/").resolve(image).toString(); } catch (Exception e) { return null; } }
    private String money(java.math.BigDecimal amount) { return (amount == null ? java.math.BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private String validBase(String value) { try { URI uri = URI.create(value); if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null) return uri.toString().replaceAll("/$", ""); } catch (Exception ignored) {} throw new IllegalStateException("邮件链接基址无效"); }
    private String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private record ItemView(String productTitle, String skuTitle, String image, String price, Integer quantity, String subtotal) {}
    public record RenderedMail(String subject, String html, String text) {}
}
