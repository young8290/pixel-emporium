package com.example.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomForbiddenException;
import com.example.user.entity.Address;
import com.example.user.mapper.AddressMapper;
import com.example.user.request.AddressListRequest;
import com.example.user.request.AddressRequest;
import com.example.user.response.AddressResponse;
import com.example.user.service.impl.AddressServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressMapper addressMapper;

    private AddressServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "address-test"),
                Address.class);
    }

    @BeforeEach
    void setUp() {
        service = new AddressServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", addressMapper);
        UserContext.set(7L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void createBindsCurrentUserAndComposesLegacyAddress() {
        AddressRequest request = request(true);
        request.setUserId(99L);
        when(addressMapper.lockUserById(7L)).thenReturn(7L);
        when(addressMapper.update(any(), any())).thenReturn(1);
        when(addressMapper.insert(any(Address.class))).thenReturn(1);

        assertTrue(service.saveOrUpdate(request));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        Address saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals("张三", saved.getRecipient());
        assertEquals("广东省深圳市南山区科技南十二路", saved.getAddress());
        assertTrue(saved.getIsDefault());
        verify(addressMapper).lockUserById(7L);
        verify(addressMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void updatePreservesStoredIdentityAndRejectsOtherUsers() {
        Address existing = address(11L, 7L, false);
        when(addressMapper.selectById(11L)).thenReturn(existing);
        when(addressMapper.updateById(any(Address.class))).thenReturn(1);

        AddressRequest request = request(false);
        request.setId(11L);
        request.setUserId(99L);

        assertTrue(service.saveOrUpdate(request));
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateById(captor.capture());
        assertEquals(11L, captor.getValue().getId());
        assertEquals(7L, captor.getValue().getUserId());

        Address other = address(12L, 8L, false);
        when(addressMapper.selectById(12L)).thenReturn(other);
        request.setId(12L);
        assertThrows(CustomForbiddenException.class, () -> service.saveOrUpdate(request));
        verify(addressMapper, never()).updateById(argThat((Address a) -> a.getId() != null && a.getId().equals(12L)));
    }

    @Test
    void defaultSaveClearsOnlyCurrentUsersOtherDefaults() {
        AddressRequest request = request(true);
        when(addressMapper.lockUserById(7L)).thenReturn(7L);
        when(addressMapper.update(any(), any())).thenReturn(1);
        when(addressMapper.insert(any(Address.class))).thenReturn(0);

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveOrUpdate(request));
        assertNotNull(error);
        verify(addressMapper).update(isNull(), any(Wrapper.class));
        verify(addressMapper).insert(any(Address.class));
    }

    @Test
    void setDefaultLocksUserClearsOthersAndUpdatesTarget() {
        Address target = address(21L, 7L, false);
        when(addressMapper.selectById(21L)).thenReturn(target);
        when(addressMapper.lockUserById(7L)).thenReturn(7L);
        when(addressMapper.update(any(), any())).thenReturn(1);
        when(addressMapper.updateById(any(Address.class))).thenReturn(1);

        assertTrue(service.setDefault(21L));

        assertTrue(target.getIsDefault());
        verify(addressMapper).lockUserById(7L);
        verify(addressMapper).update(isNull(), any(Wrapper.class));
        verify(addressMapper).updateById(target);
    }

    @Test
    void setDefaultRejectsOtherUsersAndMissingAddress() {
        when(addressMapper.selectById(31L)).thenReturn(address(31L, 8L, false));
        assertThrows(CustomForbiddenException.class, () -> service.setDefault(31L));

        when(addressMapper.selectById(32L)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> service.setDefault(32L));
    }

    @Test
    void listAndPageAreBoundToCurrentUserAndSearchStructuredFields() {
        when(addressMapper.selectList(any())).thenReturn(List.of(address(41L, 7L, true)));
        assertEquals(1, service.getAddress().size());
        ArgumentCaptor<Wrapper<Address>> listWrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(addressMapper).selectList(listWrapper.capture());
        String listSql = listWrapper.getValue().getSqlSegment();
        assertTrue(listSql.contains("user_id"));
        assertTrue(listSql.contains("is_default"));
        assertTrue(listSql.contains("created_at"));

        when(addressMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<Address> page = invocation.getArgument(0);
            page.setRecords(List.of(address(42L, 7L, false)));
            page.setTotal(1);
            return page;
        });
        AddressListRequest request = new AddressListRequest();
        request.setPage(1);
        request.setSize(10);
        request.setKeyword("深圳");
        assertEquals(1, service.getAddressPage(request).getTotal());
        ArgumentCaptor<Wrapper<Address>> pageWrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(addressMapper).selectPage(any(Page.class), pageWrapper.capture());
        String pageSql = pageWrapper.getValue().getSqlSegment();
        assertTrue(pageSql.contains("user_id"));
        assertTrue(pageSql.contains("province"));
        assertTrue(pageSql.contains("city"));
        assertTrue(pageSql.contains("district"));
        assertTrue(pageSql.contains("detail"));
        assertTrue(pageSql.contains("address"));
    }

    @Test
    void deleteFiltersOwnershipAndThrowsForMissingOrOtherUser() {
        when(addressMapper.selectById(51L)).thenReturn(address(51L, 7L, false));
        when(addressMapper.deleteById(51L)).thenReturn(1);
        assertTrue(service.deleteAddress(51L));

        when(addressMapper.selectById(52L)).thenReturn(address(52L, 8L, false));
        assertThrows(CustomForbiddenException.class, () -> service.deleteAddress(52L));

        when(addressMapper.selectById(53L)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> service.deleteAddress(53L));
    }

    private static AddressRequest request(boolean defaultAddress) {
        AddressRequest request = new AddressRequest();
        request.setRecipient("张三");
        request.setPhone("13800138000");
        request.setProvince("广东省");
        request.setCity("深圳市");
        request.setDistrict("南山区");
        request.setDetail("科技南十二路");
        request.setIsDefault(defaultAddress);
        return request;
    }

    private static Address address(long id, long userId, boolean isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setRecipient("收件人");
        address.setPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技南十二路");
        address.setAddress("广东省深圳市南山区科技南十二路");
        address.setIsDefault(isDefault);
        address.setCreatedAt(LocalDateTime.now());
        return address;
    }
}
