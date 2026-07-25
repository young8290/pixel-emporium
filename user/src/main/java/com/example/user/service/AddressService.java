package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.response.PageResponse;
import com.example.user.entity.Address;
import com.example.user.request.AddressListRequest;
import com.example.user.request.AddressRequest;
import com.example.user.response.AddressResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface AddressService extends IService<Address> {
    List<Address> getAddress();

    PageResponse<AddressResponse> getAddressPage(@Valid AddressListRequest request);

    boolean saveOrUpdate(AddressRequest request);

    boolean deleteAddress(Long id);

    boolean setDefault(Long id);
}
