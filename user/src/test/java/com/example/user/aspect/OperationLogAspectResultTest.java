package com.example.user.aspect;

import com.example.common.response.R;
import com.example.user.annotation.OperationLog;
import com.example.user.service.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogAspectResultTest {
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    private OperationLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new OperationLogAspect(operationLogService);
    }

    @Test
    void businessFailureResponseIsRecordedAsFailure() throws Throwable {
        Method method = Fixture.class.getMethod("write");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(R.fail("rejected"));

        aspect.around(joinPoint);

        ArgumentCaptor<com.example.user.entity.OperationLog> captor =
                ArgumentCaptor.forClass(com.example.user.entity.OperationLog.class);
        verify(operationLogService).record(captor.capture());
        assertEquals("FAIL", captor.getValue().getResult());
    }

    static class Fixture {
        @OperationLog(action = "WRITE")
        public R<Boolean> write() {
            return R.ok(true);
        }
    }
}
