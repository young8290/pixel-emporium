package com.example.generator.custom;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.example.generator.util.CustomFreemarkerUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CustomCodeGenerator {
    static String basePath = "[module]/src/main/java/com/example/[module]/";
    static String frontendType = "frontend";
    static String apiJsPath = frontendType + "/src/api/[module]/";
    static String vuePath = frontendType + "/src/views/[module]/";
    static boolean readOnly = false;

    public static void main(String[] args) throws Exception {
        String module = "business";

        basePath = basePath.replace("[module]", module);
        new File(basePath).mkdirs();

        String tableName = args.length > 0 ? args[0] : "student";
        String Domain = StrUtil.upperFirst(StrUtil.toCamelCase(tableName));
        System.out.println("表名：" + tableName + "，对应的实体名：" + Domain);
        String domain = Domain.substring(0, 1).toLowerCase() + Domain.substring(1);
        System.out.println("表名的小驼峰形式：" + domain);
        String doHyphenMain = tableName.replaceAll("_", "-");
        System.out.println("表名的中划线格式：" + doHyphenMain);

        initDb();

        List<Field> fieldList = CustomDbUtil.getColumnByTableName(tableName);

        String tableNameCn = CustomDbUtil.getTableComment(tableName);

        Set<String> typeSet = getJavaType(fieldList);

        Map<String, Object> data = new HashMap<>();
        data.put("module", module);
        data.put("tableName", tableName);
        data.put("tableNameCn", tableNameCn);
        data.put("Domain", Domain);
        data.put("domain", domain);
        data.put("doHyphenMain", doHyphenMain);
        data.put("fieldList", fieldList);
        data.put("typeSet", typeSet);
        data.put("readOnly", readOnly);
        data.put("frontendType", frontendType);
        data.put("hasImageColumn", fieldList.stream().anyMatch(Field::getImageColumn));
        data.put("hasMarkdownColumn", fieldList.stream().anyMatch(Field::getMarkdownColumn));
        data.put("hasVideoColumn", fieldList.stream().anyMatch(Field::getVideoColumn));

        List<Field> refFields = fieldList.stream()
                .filter(f -> f.getRefTable() != null && !f.getRefTable().isEmpty())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                Field::getRefTable,
                                f -> f,
                                (a, b) -> a),
                        m -> new ArrayList<>(m.values())));
        data.put("refFields", refFields);
        data.put("hasRefColumn", !refFields.isEmpty());

        generate(Domain, data, "entity", "entity");

        generate(Domain, data, "mapper", "mapper");

        generate(Domain, data, "request", "request");

        generate(Domain, data, "request", "listRequest");

        generate(Domain, data, "response", "response");

        generate(Domain, data, "service", "service");

        generate(Domain, data, "service/impl", "serviceImpl");

        if (frontendType.endsWith("admin")) {
            generate(Domain, data, "controller/admin", "adminController");

            generateApiJs(domain, data, "adminApiJs");
        } else {
            generate(Domain, data, "controller", "controller");

            generateApiJs(domain, data, "apiJs");
        }

        generateVue(Domain, data);

        for (Field refField : refFields) {
            generateSelect(refField, module, data);
        }

        String menuEntry = "  {\n    path: '/%s',\n    title: '%s',\n    icon: CoffeeCup // <= 替换图标\n  },\n"
                .formatted(doHyphenMain, tableNameCn + "管理");
        writeToGeneratedSection(frontendType + "/src/layout/menuConfig.js",
                "path: '/" + doHyphenMain + "'", menuEntry);
        System.out.println("自动修改 menuConfig.js 完成");
        String routeEntry = "      {\n        path: '%s',\n        name: '%s',\n        component: () => import('@/views/%s/%s.vue'),\n        meta: { title: '%s' }\n      },\n"
                .formatted(doHyphenMain, Domain, module, Domain, tableNameCn + "管理");
        writeToGeneratedSection(frontendType + "/src/router/routes.js",
                "path: '" + doHyphenMain + "'", routeEntry);
        System.out.println("自动修改 routes.js 完成");
    }

    private static void writeToGeneratedSection(String filePath, String uniqueKey, String newContent) throws IOException {
        String startMarker = "// [gen-start]";
        String endMarker = "// [gen-end]";

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return;
        }

        String content = Files.readString(path, StandardCharsets.UTF_8);
        int startIdx = content.indexOf(startMarker);
        int endIdx = content.indexOf(endMarker);

        if (startIdx == -1 || endIdx == -1) {
            return;
        }

        String section = content.substring(startIdx + startMarker.length(), endIdx);
        if (section.contains(uniqueKey)) {
            return;
        }

        int lineStart = content.lastIndexOf('\n', endIdx) + 1;
        String updated = content.substring(0, lineStart) + newContent + content.substring(lineStart);
        Files.writeString(path, updated, StandardCharsets.UTF_8);
    }

    private static void generate(String Domain, Map<String, Object> data, String packageName, String templateName) throws Exception {
        CustomFreemarkerUtil.getTemplate(templateName + ".ftl");
        String Target = templateName.substring(0, 1).toUpperCase() + templateName.substring(1);
        String targetPath = basePath + packageName + "/";
        new File(targetPath).mkdirs();
        String typeClass = targetPath + Domain + ("entity".equals(templateName) ? "" : Target) + ".java";
        CustomFreemarkerUtil.generate(typeClass, data);
        System.out.println("自动生成 " + Target + " 完成，" + typeClass);
    }

    private static void generateApiJs(String domain, Map<String, Object> data, String templateName) throws Exception {
        CustomFreemarkerUtil.getTemplate(templateName + ".ftl");
        apiJsPath = apiJsPath.replace("[module]", String.valueOf(data.get("module")));
        new File(apiJsPath).mkdirs();
        String fileName = apiJsPath + domain + ".js";
        System.out.println("自动生成 " + fileName + " 完成");
        CustomFreemarkerUtil.generate(fileName, data);
    }

    private static void generateVue(String Domain, Map<String, Object> data) throws Exception {
        CustomFreemarkerUtil.getTemplate("vue.ftl");
        vuePath = vuePath.replace("[module]", String.valueOf(data.get("module")));
        new File(vuePath).mkdirs();
        String fileName = vuePath + Domain + ".vue";
        System.out.println("自动生成 " + fileName + " 完成");
        CustomFreemarkerUtil.generate(fileName, data);
    }

    private static void generateSelect(Field refField, String module, Map<String, Object> data) throws Exception {
        String refTable = refField.getRefTable();
        String refDomain = StrUtil.upperFirst(StrUtil.toCamelCase(refTable));
        String refDomainLower = StrUtil.toCamelCase(refTable);

        Map<String, Object> selectData = new HashMap<>(data);
        selectData.put("refTable", refTable);
        selectData.put("RefDomain", refDomain);
        selectData.put("refDomain", refDomainLower);
        selectData.put("refLabelField", refField.getRefLabelField());
        selectData.put("module", module);
        selectData.put("refTableNameCn", refDomain);

        CustomFreemarkerUtil.getTemplate("select.ftl");
        String componentsPath = frontendType + "/src/components/";
        new File(componentsPath).mkdirs();
        String fileName = componentsPath + refDomain + "Select.vue";

        if (new File(fileName).exists()) {
            return;
        }

        CustomFreemarkerUtil.generate(fileName, selectData);
    }

    private static void initDb() {
        Properties dbProperties = new Properties();
        File file = getFile("db.properties");
        try {
            dbProperties.load(new FileReader(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String url = dbProperties.getProperty("url");
        String username = dbProperties.getProperty("username");
        String password = dbProperties.getProperty("password");

        CustomDbUtil.url = url;
        CustomDbUtil.username = username;
        CustomDbUtil.password = password;
        CustomDbUtil.database = ReUtil.getGroup1(".*:\\d+/([\\w\\d-_]+)\\?*.*", url);
    }

    private static File getFile(String fileName) {
        ClassLoader classLoader = CodeGenerator.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);

        if (resource == null) {
            throw new IllegalArgumentException("file is not found: [" + fileName + "]");
        } else {
            try {
                return new File(resource.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static Set<String> getJavaType(List<Field> fieldList) {
        Set<String> set = new HashSet<>();
        fieldList.forEach(field -> set.add(field.getJavaType()));
        return set;
    }
}
