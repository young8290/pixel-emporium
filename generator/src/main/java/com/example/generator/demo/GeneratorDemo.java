package com.example.generator.demo;

import com.example.generator.util.CustomFreemarkerUtil;

import java.io.File;
import java.util.HashMap;

public class GeneratorDemo {

    static String targetPath = "target/generated-sources/demo/";

    static {
        new File(targetPath).mkdirs();
    }

    public static void main(String[] args) throws Exception {
        //模板 Template
        CustomFreemarkerUtil.getTemplate("demo.ftl");

        //数据 Java objects
        HashMap<String, Object> data = new HashMap<>();
        String className = "Demo1";
        data.put("className", className);

        //Output
        CustomFreemarkerUtil.generate(targetPath + className + ".java", data);
    }
}
