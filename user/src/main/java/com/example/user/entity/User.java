package com.example.user.entity;

import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class User extends BaseEntity {
    private String email;
    private String password;
    private String name;
    private String mobile;
    private String avatar;
    private Boolean enabled;
    private Long tokenVersion;
}
