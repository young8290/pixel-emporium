package com.example.user;

import com.example.user.config.MailCenterProperties;
import com.example.user.model.OrderCreatedMailEvent;
import com.example.user.service.MailTemplateService;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailTemplateServiceTest {
    @Test
    void rendersEscapedOrderDetailsWithAbsoluteContainedImagesAndPlainText() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        MailTemplateService service = new MailTemplateService(new MailCenterProperties(null, null, null,
                new MailCenterProperties.Links("https://shop.example", "https://assets.example")), engine);
        OrderCreatedMailEvent event = new OrderCreatedMailEvent();
        event.setOrderId(1L); event.setOrderNo("A-1"); event.setTotalAmount(BigDecimal.ONE); event.setFreightAmount(BigDecimal.ZERO); event.setPayAmount(BigDecimal.ONE);
        event.setCreatedAt(java.time.OffsetDateTime.parse("2026-07-24T10:15:30+08:00"));
        OrderCreatedMailEvent.Item item = new OrderCreatedMailEvent.Item(); item.setProductTitle("<script>alert(1)</script>"); item.setSkuTitle("Blue"); item.setImage("images/a.png"); item.setPrice(BigDecimal.ONE); item.setQuantity(1); item.setSubtotal(BigDecimal.ONE); event.setItems(List.of(item));
        OrderCreatedMailEvent.Receiver receiver = new OrderCreatedMailEvent.Receiver(); receiver.setName("Owner"); receiver.setPhone("1"); receiver.setProvince("P"); receiver.setCity("C"); receiver.setDistrict("D"); receiver.setDetail("Street"); event.setReceiver(receiver);
        MailTemplateService.RenderedMail mail = service.orderCreated(event);
        assertTrue(mail.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(mail.html().contains("<script>alert(1)</script>"));
        assertTrue(mail.html().contains("https://assets.example/images/a.png"));
        assertTrue(mail.html().contains("object-fit:contain"));
        assertFalse(mail.html().contains("object-fit:cover"));
        assertTrue(mail.text().contains("A-1"));
        assertTrue(mail.html().contains("2026-07-24T10:15:30+08:00"));
        assertTrue(mail.html().contains("小计"));
        assertTrue(mail.html().contains("总额"));
        assertTrue(mail.html().contains("收货信息"));
        assertTrue(mail.html().contains("align=\"center\""));
        assertTrue(mail.html().contains("valign=\"middle\""));
        assertTrue(mail.text().contains("收货信息"));
        assertEquals("images/a.png", item.getImage());
        assertTrue(mail.text().contains("<script>alert(1)</script>"));
        assertTrue(mail.text().contains("Blue"));
        assertTrue(mail.text().contains("单价：1.00"));
        assertTrue(mail.text().contains("数量：1"));
        assertTrue(mail.text().contains("小计：1.00"));
        assertTrue(mail.text().contains("Owner"));
        assertTrue(mail.text().contains("1"));
        assertTrue(mail.text().contains("P C D Street"));
        assertTrue(mail.html().matches("(?s).*单价：\\s*<span[^>]*>1\\.00</span>.*"));
        assertTrue(mail.html().matches("(?s).*小计：\\s*<span[^>]*>1\\.00</span>.*"));
        assertTrue(mail.html().matches("(?s).*总额：\\s*<span[^>]*>1\\.00</span>.*"));
        assertTrue(mail.html().matches("(?s).*运费：\\s*<span[^>]*>0\\.00</span>.*"));
        assertTrue(mail.html().matches("(?s).*应付：\\s*<span[^>]*>1\\.00</span>.*"));

        MailTemplateService.RenderedMail verification = service.verification("123456");
        assertTrue(verification.html().contains("像素百货 · PIXEL SHOP"));
        assertTrue(verification.html().contains("这是一封系统通知邮件，请勿直接回复。"));
        assertFalse(verification.html().contains("<script"));
        assertFalse(verification.html().contains("position:"));
        assertFalse(verification.html().contains("display:grid"));
        assertFalse(verification.html().contains("display:flex"));
    }

    @Test
    void omitsUnsupportedImagesWithoutDroppingItemText() {
        MailTemplateService service = service();
        OrderCreatedMailEvent event = event("javascript:alert(1)");

        MailTemplateService.RenderedMail mail = service.orderCreated(event);

        assertFalse(mail.html().contains("<img"));
        assertTrue(mail.html().contains("Keyboard"));
        assertTrue(mail.html().contains("Green / 87"));
        assertEquals("javascript:alert(1)", event.getItems().getFirst().getImage());
    }

    private MailTemplateService service() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        return new MailTemplateService(new MailCenterProperties(null, null, null,
                new MailCenterProperties.Links("https://shop.example", "https://assets.example")), engine);
    }

    private OrderCreatedMailEvent event(String image) {
        OrderCreatedMailEvent event = new OrderCreatedMailEvent();
        event.setOrderId(1L); event.setOrderNo("A-1"); event.setTotalAmount(BigDecimal.ONE); event.setFreightAmount(BigDecimal.ZERO); event.setPayAmount(BigDecimal.ONE);
        event.setCreatedAt(java.time.OffsetDateTime.parse("2026-07-24T10:15:30+08:00"));
        OrderCreatedMailEvent.Item item = new OrderCreatedMailEvent.Item(); item.setProductTitle("Keyboard"); item.setSkuTitle("Green / 87"); item.setImage(image); item.setPrice(BigDecimal.ONE); item.setQuantity(1); item.setSubtotal(BigDecimal.ONE); event.setItems(List.of(item));
        OrderCreatedMailEvent.Receiver receiver = new OrderCreatedMailEvent.Receiver(); receiver.setName("Owner"); receiver.setPhone("1"); receiver.setProvince("P"); receiver.setCity("C"); receiver.setDistrict("D"); receiver.setDetail("Street"); event.setReceiver(receiver);
        return event;
    }
}
