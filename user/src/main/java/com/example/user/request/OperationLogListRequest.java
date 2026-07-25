package com.example.user.request;

import com.example.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationLogListRequest extends PageRequest {
    private Long userId;
    private String action;
    private String targetType;
    private String targetId;
}
