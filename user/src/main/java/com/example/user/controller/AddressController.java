package com.example.user.controller;

import com.example.common.response.PageResponse;
import com.example.common.response.R;
import com.example.user.entity.Address;
import com.example.user.request.AddressListRequest;
import com.example.user.request.AddressRequest;
import com.example.user.response.AddressResponse;
import com.example.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("address")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @GetMapping("list")
    public R<PageResponse<AddressResponse>> list(@Valid AddressListRequest request) {
        PageResponse<AddressResponse> addressList = addressService.getAddressPage(request);
        return R.ok(addressList);
    }

    @PostMapping("/save")
    public R<Boolean> save(@RequestBody @Valid AddressRequest request) {
        return R.ok(addressService.saveOrUpdate(request));
    }

    @Operation(summary = "设为默认收货地址")
    @PutMapping("/default/{id}")
    public R<Boolean> setDefault(@PathVariable Long id) {
        return R.ok(addressService.setDefault(id));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(addressService.deleteAddress(id));
    }
}
