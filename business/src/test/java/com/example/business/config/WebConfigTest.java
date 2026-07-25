package com.example.business.config;

import com.example.common.interceptor.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebConfigTest {
    @Test
    void categoryTreeIsExcludedFromLoginInterceptor() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "loginInterceptor", mock(LoginInterceptor.class));
        ExposedInterceptorRegistry registry = new ExposedInterceptorRegistry();

        webConfig.addInterceptors(registry);

        MappedInterceptor mapped = (MappedInterceptor) registry.interceptors().getFirst();
        List<String> excludes = List.of(mapped.getExcludePathPatterns());
        assertTrue(excludes.contains("/category/tree"));
        assertTrue(excludes.contains("/product/**"));
        assertTrue(excludes.contains("/pay/alipay/notify"));
        assertFalse(mapped.matches("/category/tree", new AntPathMatcher()));
        assertFalse(mapped.matches("/product/list", new AntPathMatcher()));
        assertFalse(mapped.matches("/product/7", new AntPathMatcher()));
        assertFalse(mapped.matches("/pay/alipay/notify", new AntPathMatcher()));
        assertTrue(mapped.matches("/pay/alipay/create", new AntPathMatcher()));
        assertTrue(mapped.matches("/admin/category/list", new AntPathMatcher()));
        assertTrue(mapped.matches("/admin/product/list", new AntPathMatcher()));
    }

    private static final class ExposedInterceptorRegistry extends InterceptorRegistry {
        private List<Object> interceptors() {
            return getInterceptors();
        }
    }
}
