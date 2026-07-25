package com.example.generator.custom;

import cn.hutool.json.JSONUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomDbUtil {
    public static String url = "";
    public static String username = "";
    public static String password = "";
    public static String database = "";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = CustomDbUtil.url;
            String user = CustomDbUtil.username;
            String password = CustomDbUtil.password;
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNotFoundException:" + e);
        } catch (SQLException e) {
            System.err.println("SQLException:" + e);
        }
        return conn;
    }

    public static List<Field> getColumnByTableName(String tableName) throws Exception {
        List<Field> fieldList = new ArrayList<>();
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("show full columns from `" + database + "`.`" + tableName + "`");
        if (rs != null) {
            while (rs.next()) {
                String columnName = rs.getString("Field");
                String type = rs.getString("Type");
                String comment = rs.getString("Comment");
                String nullAble = rs.getString("Null"); // YES or NO
                Field field = new Field();
                field.setName(columnName);
                field.setNameLowerCamelCase(underLineToLowerCameCase(columnName));
                field.setNameUpperCameCase(underLineToUpperCameCase(columnName));
                field.setType(type);
                field.setJavaType(CustomDbUtil.sqlTypeToJavaType(rs.getString("Type")));
                field.setComment(comment);
                if (comment.contains("｜")) {
                    field.setNameCn(comment.substring(0, comment.indexOf("｜")));
                } else {
                    field.setNameCn(comment);
                }
                field.setNullAble("YES".equals(nullAble));
                field.setDefaultValue(rs.getString("Default"));
                if (type.toUpperCase().contains("varchar".toUpperCase())) {
                    String lengthStr = type.substring(type.indexOf("(") + 1, type.length() - 1);
                    field.setLength(Integer.valueOf(lengthStr));
                } else {
                    field.setLength(0);
                }
                field.setSearchable(comment.contains("searchable"));

                field.setHasOption(comment.contains("【") && comment.contains("、") && comment.contains("】"));

                field.setImageColumn(comment.contains("imageColumn"));

                field.setMarkdownColumn(comment.contains("markdownColumn"));

                field.setVideoColumn(comment.contains("videoColumn"));

                if (comment.contains("ref:")) {
                    int refStart = comment.indexOf("ref:") + 4;
                    String refPart = comment.substring(refStart).split("｜")[0].trim();
                    String[] refParts = refPart.split(":");
                    if (refParts.length >= 1) {
                        field.setRefTable(refParts[0].trim());
                    }
                    if (refParts.length >= 2) {
                        field.setRefLabelField(refParts[1].trim());
                    } else {
                        field.setRefLabelField("id");
                    }
                }

                fieldList.add(field);
            }
        }
        assert rs != null;
        rs.close();
        stmt.close();
        conn.close();
        return fieldList;
    }

    public static String getTableComment(String tableName) throws Exception {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select table_comment from information_schema.tables where table_schema = '" + database + "' and table_name = '" + tableName + "'");
        String tableNameCH = "";
        if (rs != null) {
            while (rs.next()) {
                tableNameCH = rs.getString("table_comment");
            }
        }
        assert rs != null;
        rs.close();
        stmt.close();
        conn.close();
        System.out.println("表名（" + tableName + "）的中文注释是（" + tableNameCH + "）");
        return tableNameCH;
    }

    /**
     * 下划线转小驼峰。例：user_id 转成 userId
     */
    public static String underLineToLowerCameCase(String str) {
        Pattern linePattern = Pattern.compile("_(\\w)");
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 下划线转大驼峰：user_id 转成 UserId
     */
    public static String underLineToUpperCameCase(String str) {
        String s = underLineToLowerCameCase(str);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * 数据库类型转为 Java 类型
     */
    public static String sqlTypeToJavaType(String sqlType) {
        if (sqlType.toUpperCase().contains("varchar".toUpperCase())
                || sqlType.toUpperCase().contains("char".toUpperCase())
                || sqlType.toUpperCase().contains("text".toUpperCase())) {
            return "String";
        } else if (sqlType.toUpperCase().contains("datetime".toUpperCase())) {
            return "LocalDateTime";
        } else if (sqlType.toUpperCase().contains("time".toUpperCase())) {
            return "LocalTime";
        } else if (sqlType.toUpperCase().contains("date".toUpperCase())) {
            return "LocalDate";
        } else if (sqlType.toUpperCase().contains("bigint".toUpperCase())) {
            return "Long";
        } else if (sqlType.toUpperCase().contains("tinyint".toUpperCase())) {
            return "Integer";
        } else if (sqlType.toUpperCase().contains("int".toUpperCase())) {
            return "Integer";
        } else if (sqlType.toUpperCase().contains("long".toUpperCase())) {
            return "Long";
        } else if (sqlType.toUpperCase().contains("decimal".toUpperCase())) {
            return "BigDecimal";
        } else if (sqlType.toUpperCase().contains("double".toUpperCase())
                || sqlType.toUpperCase().contains("float".toUpperCase())) {
            return "Double";
        } else if (sqlType.toUpperCase().contains("boolean".toUpperCase())) {
            return "Boolean";
        } else {
            return "String";
        }
    }
}