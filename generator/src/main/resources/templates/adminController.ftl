package com.example.${module}.controller.admin;

import com.example.common.annotation.CustomRequireRole;
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
@RequestMapping("admin/${doHyphenMain}")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ${Domain}AdminController {
    @Autowired
    private ${Domain}Service ${domain}Service;

    @Operation(summary = "${tableNameCn!}列表")
    @GetMapping("/list")
    public R<PageResponse<${Domain}Response>> list(@Valid ${Domain}ListRequest request) {
        return R.ok(${domain}Service.getAdmin${Domain}Page(request));
    }

    @Operation(summary = "新增/修改${tableNameCn!}")
    @PostMapping("/save")
    public R<Boolean> save(@RequestBody @Valid ${Domain}Request request) {
        if (request.getId() == null) {
            return R.ok(${domain}Service.adminAdd(request));
        } else {
            return R.ok(${domain}Service.adminUpdate(request));
        }
    }

    @Operation(summary = "删除${tableNameCn!}")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(${domain}Service.adminDelete(id));
    }

    @Operation(summary = "批量删除${tableNameCn!}")
    @DeleteMapping
    public R<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        return R.ok(${domain}Service.adminDeleteBatch(ids));
    }
}
