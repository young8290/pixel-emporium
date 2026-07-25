package com.example.business;

import com.aliyun.oss.OSS;
import com.example.business.config.OSSConfig;
import com.example.business.service.FileUploadPolicy;
import com.example.business.service.ValidatedOssStorage;
import com.example.common.exception.CustomBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class OssOptionalConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OptionalOssTestConfig.class)
            .withPropertyValues(
                    "alibaba.cloud.oss.enabled=false",
                    "alibaba.cloud.oss.endpoint=oss-cn-shenzhen.aliyuncs.com",
                    "alibaba.cloud.oss.access-key=",
                    "alibaba.cloud.oss.secret-key=",
                    "alibaba.cloud.oss.bucket=");

    @Test
    void businessStorageStartsWithoutOptionalOssCredentials() {
        contextRunner.run(context -> {
            org.assertj.core.api.Assertions.assertThat(context).hasNotFailed();
            org.assertj.core.api.Assertions.assertThat(context).doesNotHaveBean(OSS.class);
            org.assertj.core.api.Assertions.assertThat(context).hasSingleBean(ValidatedOssStorage.class);
        });
    }

    @Test
    void disabledStorageReportsAConfigurationErrorWhenUploadIsRequested() {
        contextRunner.run(context -> {
            ValidatedOssStorage storage = context.getBean(ValidatedOssStorage.class);

            CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                    () -> storage.storeDefault("picture.png", "image/png", new byte[]{
                            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0
                    }));

            assertEquals("文件存储未配置，请先启用 OSS", exception.getMessage());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({OSSConfig.class, ValidatedOssStorage.class})
    static class OptionalOssTestConfig {
        @Bean
        FileUploadPolicy fileUploadPolicy() {
            return mock(FileUploadPolicy.class);
        }
    }
}
