package com.example.business.service;

import com.example.common.exception.CustomValidationException;

public interface FileUploadPolicy {
    FileUploadDescriptor validate(String filename, String contentType, byte[] bytes) throws CustomValidationException;

    record FileUploadDescriptor(String suffix, String category, String contentType, byte[] bytes) {
    }
}
