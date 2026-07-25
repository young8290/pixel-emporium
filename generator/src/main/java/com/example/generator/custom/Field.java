package com.example.generator.custom;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Field {
    private String name;

    private String nameLowerCamelCase;

    private String nameUpperCameCase;

    private String nameCn;

    private String type;

    private String javaType;

    private String comment;

    private Boolean nullAble;

    private Integer length;

    private Boolean searchable;

    private Boolean hasOption;

    private Boolean imageColumn;

    private Boolean markdownColumn;

    private Boolean videoColumn;

    private String refTable;

    private String refLabelField;

    private String defaultValue;
}