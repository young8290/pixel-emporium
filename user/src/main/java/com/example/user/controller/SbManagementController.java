package com.example.user.controller;

import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import com.example.user.response.SbResponse;
import com.example.user.service.SbService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sb")
public class SbManagementController {
    @Autowired
    private SbService sbService;

    @GetMapping("list")
    public R<PageResponse<SbResponse>> list(
            @Valid PageRequest request
    ) {
        PageResponse<SbResponse> sbList =
                sbService.getSbPage(request);

        return R.ok(sbList);
    }
}