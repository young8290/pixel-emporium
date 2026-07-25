package com.example.user.request;

import com.example.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserListRequest extends PageRequest {
    private String email;
    private String name;
    private Boolean enabled;
}
