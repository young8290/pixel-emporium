package com.example.common.orm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.example.common.core.UserContext;
import com.example.common.orm.config.MyBatisPlusHandler;
import com.example.common.orm.config.MybatisPlusConfig;
import com.example.common.orm.entity.BaseEntity;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommonOrmContractTest {

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void baseEntityRetainsIdFillAndLogicDeleteAnnotations() throws Exception {
        assertNotNull(field("id").getAnnotation(TableId.class));
        assertEquals(FieldFill.INSERT, field("createdAt").getAnnotation(TableField.class).fill());
        assertEquals(FieldFill.INSERT, field("createdBy").getAnnotation(TableField.class).fill());
        assertEquals(FieldFill.UPDATE, field("updatedAt").getAnnotation(TableField.class).fill());
        assertEquals(FieldFill.UPDATE, field("updatedBy").getAnnotation(TableField.class).fill());
        assertNotNull(field("deleted").getAnnotation(TableLogic.class));
    }

    @Test
    void sharedHandlerFillsAuditFieldsFromUserContext() {
        UserContext.set(42L);
        TestEntity entity = new TestEntity();
        MyBatisPlusHandler handler = new MyBatisPlusHandler();

        handler.insertFill(SystemMetaObject.forObject(entity));
        handler.updateFill(SystemMetaObject.forObject(entity));

        assertNotNull(entity.getCreatedAt());
        assertEquals(42L, entity.getCreatedBy());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(42L, entity.getUpdatedBy());
    }

    @Test
    void sharedConfigKeepsOptimisticLockBeforePagination() {
        var interceptors = new MybatisPlusConfig()
                .mybatisPlusInterceptor()
                .getInterceptors();

        assertEquals(2, interceptors.size());
        assertEquals("OptimisticLockerInnerInterceptor", interceptors.get(0).getClass().getSimpleName());
        assertEquals("PaginationInnerInterceptor", interceptors.get(1).getClass().getSimpleName());
    }

    private Field field(String name) throws NoSuchFieldException {
        return BaseEntity.class.getDeclaredField(name);
    }

    private static final class TestEntity extends BaseEntity {
    }
}
