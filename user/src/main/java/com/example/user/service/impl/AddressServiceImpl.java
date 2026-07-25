package com.example.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.response.PageResponse;
import com.example.user.entity.Address;
import com.example.user.mapper.AddressMapper;
import com.example.user.request.AddressListRequest;
import com.example.user.request.AddressRequest;
import com.example.user.response.AddressResponse;
import com.example.user.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {
    @Override
    public List<Address> getAddress() {
        return this.list(userAddressQuery());
    }

    @Override
    public PageResponse<AddressResponse> getAddressPage(AddressListRequest request) {
        LambdaQueryWrapper<Address> qw = userAddressQuery();

        if (StringUtils.hasText(request.getKeyword())) {
            String kw = request.getKeyword().trim();
            qw.and(search -> search
                    .like(Address::getRecipient, kw)
                    .or().like(Address::getPhone, kw)
                    .or().like(Address::getProvince, kw)
                    .or().like(Address::getCity, kw)
                    .or().like(Address::getDistrict, kw)
                    .or().like(Address::getDetail, kw)
                    .or().like(Address::getAddress, kw));
        }

        Page<Address> page = this.page(Page.of(request.getPage(), request.getSize()), qw);

        List<AddressResponse> list = BeanUtil.copyToList(page.getRecords(), AddressResponse.class);

        PageResponse<AddressResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(list);
        return response;
    }

    @Override
    @Transactional
    public boolean saveOrUpdate(AddressRequest request) {
        Long currentUserId = UserContext.get();
        Address address;

        if (request.getId() != null) {
            address = findAddress(request.getId());
            verifyOwnership(address, currentUserId, "无权操作此收货地址信息");
            BeanUtil.copyProperties(request, address, "id", "userId", "address", "isDefault");
            if (request.getIsDefault() != null) {
                address.setIsDefault(request.getIsDefault());
            }
        } else {
            address = BeanUtil.copyProperties(request, Address.class);
            address.setUserId(currentUserId);
            address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        }
        address.setAddress(composeAddress(request));

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            lockCurrentUser(currentUserId);
            clearOtherDefaults(currentUserId, address.getId());
        }

        boolean saved = address.getId() == null ? this.save(address) : this.updateById(address);
        if (!saved) {
            throw new IllegalStateException("收货地址保存失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteAddress(Long id) {
        Address address = findAddress(id);
        verifyOwnership(address, UserContext.get(), "无权删除此收货地址信息");

        if (!this.removeById(id)) {
            throw new IllegalStateException("收货地址删除失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean setDefault(Long id) {
        Long currentUserId = UserContext.get();
        Address address = findAddress(id);
        verifyOwnership(address, currentUserId, "无权操作此收货地址信息");

        lockCurrentUser(currentUserId);
        clearOtherDefaults(currentUserId, id);
        address.setIsDefault(true);
        if (!this.updateById(address)) {
            throw new IllegalStateException("默认收货地址设置失败");
        }
        return true;
    }

    private Address findAddress(Long id) {
        return this.getOptById(id)
                .orElseThrow(() -> new NoSuchElementException("收货地址不存在"));
    }

    private LambdaQueryWrapper<Address> userAddressQuery() {
        return new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, UserContext.get())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt);
    }

    private void verifyOwnership(Address address, Long currentUserId, String message) {
        if (address.getUserId() == null || !address.getUserId().equals(currentUserId)) {
            throw new CustomForbiddenException(message);
        }
    }

    private void lockCurrentUser(Long currentUserId) {
        Long lockedUserId = getBaseMapper().lockUserById(currentUserId);
        if (!currentUserId.equals(lockedUserId)) {
            throw new IllegalStateException("当前用户不存在，无法更新默认收货地址");
        }
    }

    private void clearOtherDefaults(Long currentUserId, Long excludedAddressId) {
        LambdaUpdateWrapper<Address> update = new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, currentUserId)
                .eq(Address::getDeleted, 0L)
                .eq(Address::getIsDefault, true)
                .ne(excludedAddressId != null, Address::getId, excludedAddressId)
                .set(Address::getIsDefault, false);
        this.update(update);
    }

    private String composeAddress(AddressRequest request) {
        String composed = String.join("",
                valueOrEmpty(request.getProvince()),
                valueOrEmpty(request.getCity()),
                valueOrEmpty(request.getDistrict()),
                valueOrEmpty(request.getDetail()));
        return StringUtils.hasText(composed) ? composed : request.getAddress();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
