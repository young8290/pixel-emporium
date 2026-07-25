package com.example.user.advice;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomConflictException;
import com.example.common.response.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new TestBodyValidator())
                .build();
    }

    @Test
    void invalidRequestBodyReturnsValidationFailedWithFieldName() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.msg", containsString("name")));
    }

    @Test
    void unhandledRuntimeExceptionReturnsGenericInternalError() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("服务器内部错误"))
                .andExpect(jsonPath("$.msg", not(containsString("internal-path"))))
                .andExpect(jsonPath("$.msg", not(containsString("C:\\secret"))));
    }

    @Test
    void customForbiddenExceptionReturnsForbidden() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("禁止访问"));
    }

    @Test
    void customConflictExceptionReturnsConflict() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("重复资源"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/valid")
        R<Void> valid(@RequestBody @Valid TestBody body) {
            return R.ok();
        }

        @GetMapping("/runtime")
        R<Void> runtime() {
            throw new RuntimeException("internal-path C:\\secret");
        }

        @GetMapping("/forbidden")
        R<Void> forbidden() {
            throw new CustomForbiddenException("禁止访问");
        }

        @GetMapping("/conflict")
        R<Void> conflict() {
            throw new CustomConflictException("重复资源");
        }
    }

    record TestBody(@NotBlank(message = "不能为空") String name) {
    }

    static class TestBodyValidator implements Validator {

        @Override
        public boolean supports(Class<?> clazz) {
            return TestBody.class.equals(clazz);
        }

        @Override
        public void validate(Object target, Errors errors) {
            TestBody body = (TestBody) target;
            if (body.name() == null || body.name().isBlank()) {
                errors.rejectValue("name", "NotBlank", "不能为空");
            }
        }
    }
}
