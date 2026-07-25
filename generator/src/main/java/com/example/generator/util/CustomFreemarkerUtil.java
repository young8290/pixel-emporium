package com.example.generator.util;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class CustomFreemarkerUtil {

    static Template template;

    public static void getTemplate(String ftlName) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassLoaderForTemplateLoading(
                CustomFreemarkerUtil.class.getClassLoader(), "templates");
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_33));
        template = cfg.getTemplate(ftlName);
    }

    public static void generate(String fileName, Map<String, Object> map) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            template.process(map, writer);
        }
    }

    public static Template getTemplateInstance(String ftlName) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassLoaderForTemplateLoading(
                CustomFreemarkerUtil.class.getClassLoader(), "templates");
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_33));
        return cfg.getTemplate(ftlName);
    }

    public static String generateToString(Template template, Map<String, Object> map) throws Exception {
        StringWriter out = new StringWriter();
        template.process(map, out);
        return out.toString();
    }
}
