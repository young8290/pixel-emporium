package com.example.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentEndpointRemovalTest {
    @Test
    void legacyStudentModuleIsNotPartOfRuntime() {
        String[] removedClasses = {
                "com.example.business.controller.StudentController",
                "com.example.business.service.StudentService",
                "com.example.business.service.impl.StudentServiceImpl",
                "com.example.business.mapper.StudentMapper",
                "com.example.business.entity.Student",
                "com.example.business.request.StudentRequest",
                "com.example.business.request.StudentListRequest",
                "com.example.business.response.StudentResponse"
        };

        for (String className : removedClasses) {
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName(className), className + " 不应被编译进运行时");
        }

        assertNull(getClass().getClassLoader().getResource("db/migration/V3__student.sql"),
                "Student 迁移不应继续进入业务运行时");
        assertNull(getClass().getClassLoader().getResource("mapper/StudentMapper.xml"),
                "Student Mapper 不应继续进入业务运行时");
    }
}
