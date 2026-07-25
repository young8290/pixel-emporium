package com.example.${module}.controller;

import com.example.common.response.R;
import com.example.${module}.request.${Domain}ListRequest;
import com.example.${module}.request.${Domain}Request;
import com.example.common.response.PageResponse;
import com.example.${module}.response.${Domain}Response;
import com.example.${module}.service.${Domain}Service;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${doHyphenMain}")
public class ${Domain}Controller {
    @Autowired
    private ${Domain}Service ${domain}Service;

    @Operation(summary = "新增/修改${tableNameCn!}")
    @PostMapping("/save")
    public R<Boolean> save(@RequestBody @Valid ${Domain}Request request) {
        return R.ok(${domain}Service.saveOrUpdate(request));
    }

    @Operation(summary = "我的${tableNameCn!}")
    @GetMapping("/list")
    public R<PageResponse<${Domain}Response>> list(@Valid ${Domain}ListRequest request) {
        return R.ok(${domain}Service.get${Domain}Page(request));
    }

    @Operation(summary = "删除${tableNameCn!}")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(${domain}Service.delete${Domain}(id));
    }
}
