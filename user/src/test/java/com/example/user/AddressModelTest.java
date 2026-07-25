package com.example.user;

import com.example.user.entity.Address;
import com.example.user.request.AddressRequest;
import com.example.user.response.AddressResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressModelTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void structuredAddressRequestIsValidWithoutLegacyAddress() {
        AddressRequest request = validRequest();

        assertTrue(invalidFields(request).isEmpty());
    }

    @Test
    void structuredAddressFieldsAreRequired() {
        assertAll(
                () -> assertMissingFieldRejected("recipient", request -> request.setRecipient(null)),
                () -> assertMissingFieldRejected("phone", request -> request.setPhone(null)),
                () -> assertMissingFieldRejected("province", request -> request.setProvince(null)),
                () -> assertMissingFieldRejected("city", request -> request.setCity(null)),
                () -> assertMissingFieldRejected("district", request -> request.setDistrict(null)),
                () -> assertMissingFieldRejected("detail", request -> request.setDetail(null))
        );
    }

    @Test
    void malformedPhoneIsRejected() {
        AddressRequest request = validRequest();
        request.setPhone("12345678901");

        assertEquals(Set.of("phone"), invalidFields(request));
    }

    @Test
    void addressModelsExposeStructuredFields() {
        assertAll(
                () -> assertStructuredFields(Address.class),
                () -> assertStructuredFields(AddressRequest.class),
                () -> assertStructuredFields(AddressResponse.class)
        );
    }

    @Test
    void v9ExtendsAddressSchemaAndDocumentsRollback() throws IOException {
        Path migration = Path.of("src/main/resources/db/migration/V9__address_extend.sql");
        assertTrue(Files.exists(migration), "V9 address migration must exist");

        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");
        assertAll(
                () -> assertTrue(sql.contains("modify column `address` varchar(255) null")),
                () -> assertTrue(sql.contains("`recipient` varchar(50)")),
                () -> assertTrue(sql.contains("`phone` varchar(20)")),
                () -> assertTrue(sql.contains("`province` varchar(50)")),
                () -> assertTrue(sql.contains("`city` varchar(50)")),
                () -> assertTrue(sql.contains("`district` varchar(50)")),
                () -> assertTrue(sql.contains("`detail` varchar(255)")),
                () -> assertTrue(sql.contains("`is_default` tinyint")),
                () -> assertTrue(sql.contains("update `address` set `detail` = `address`")),
                () -> assertTrue(sql.contains("drop index `uk_address_deleted`")),
                () -> assertTrue(sql.contains("add index `idx_address_user_default` (`user_id`, `is_default`, `deleted`)")),
                () -> assertTrue(sql.contains("-- rollback:"))
        );
    }

    private static AddressRequest validRequest() {
        AddressRequest request = new AddressRequest();
        request.setRecipient("张三");
        request.setPhone("13800138000");
        request.setProvince("浙江省");
        request.setCity("杭州市");
        request.setDistrict("西湖区");
        request.setDetail("文三路 100 号");
        request.setIsDefault(Boolean.TRUE);
        return request;
    }

    private static void assertMissingFieldRejected(String field, RequestMutation mutation) {
        AddressRequest request = validRequest();
        mutation.apply(request);

        assertEquals(Set.of(field), invalidFields(request));
    }

    private static Set<String> invalidFields(AddressRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertStructuredFields(Class<?> modelType) {
        assertAll(
                () -> assertNotNull(modelType.getDeclaredField("recipient")),
                () -> assertNotNull(modelType.getDeclaredField("phone")),
                () -> assertNotNull(modelType.getDeclaredField("province")),
                () -> assertNotNull(modelType.getDeclaredField("city")),
                () -> assertNotNull(modelType.getDeclaredField("district")),
                () -> assertNotNull(modelType.getDeclaredField("detail")),
                () -> assertNotNull(modelType.getDeclaredField("isDefault"))
        );
    }

    @FunctionalInterface
    private interface RequestMutation {
        void apply(AddressRequest request);
    }
}
