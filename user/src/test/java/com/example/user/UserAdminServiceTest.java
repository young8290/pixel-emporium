package com.example.user;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.model.UserRoleCodeRow;
import com.example.user.request.UserListRequest;
import com.example.user.response.UserAdminResponse;
import com.example.user.service.TokenBlacklistService;
import com.example.user.service.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.MybatisConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private UserServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "user-admin-test");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userMapper, tokenBlacklistService);
    }

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void adminListFiltersStableOrderBatchesRolesAndNeverExposesPassword() throws Exception {
        UserListRequest request = new UserListRequest();
        request.setEmail(" example.com ");
        request.setName("用户");
        request.setEnabled(true);
        request.setPage(2);
        request.setSize(10);
        User user = user(256L, "user@example.com", true);
        user.setPassword("hashed-secret");
        Page<User> page = new Page<>(2, 10);
        page.setTotal(1);
        page.setRecords(List.of(user));
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectRoleCodeRowsByUserIds(List.of(256L)))
                .thenReturn(List.of(roleRow(256L, 3L, "USER")));

        PageResponse<UserAdminResponse> result = service.adminList(request);

        assertEquals(1L, result.getTotal());
        UserAdminResponse response = result.getList().getFirst();
        assertEquals("256", String.valueOf(response.getId()));
        assertEquals(List.of("USER"), response.getRoleCodes());
        String json = new ObjectMapper().writeValueAsString(response);
        assertFalse(json.contains("password"));
        verify(userMapper).selectRoleCodeRowsByUserIds(List.of(256L));
        ArgumentCaptor<Wrapper> wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment().toLowerCase();
        assertTrue(sql.contains("email") && sql.contains("name") && sql.contains("enabled"));
        assertTrue(sql.contains("order by created_at desc,id desc"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void emptyPageDoesNotIssueRoleQuery() {
        UserListRequest request = new UserListRequest();
        Page<User> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<UserAdminResponse> result = service.adminList(request);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getList().isEmpty());
        verify(userMapper, never()).selectRoleCodeRowsByUserIds(anyCollection());
    }

    @Test
    void adminListRejectsNullPageOrSizeBeforeQuery() {
        UserListRequest nullPage = new UserListRequest();
        nullPage.setPage(null);
        assertThrows(CustomValidationException.class, () -> service.adminList(nullPage));

        UserListRequest nullSize = new UserListRequest();
        nullSize.setSize(null);
        assertThrows(CustomValidationException.class, () -> service.adminList(nullSize));

        verifyNoInteractions(userMapper);
    }

    @Test
    void detailMapsRolesAndDoesNotContainPasswordField() throws Exception {
        User user = user(178L, "admin@example.com", true);
        user.setPassword("hashed-secret");
        when(userMapper.selectById(178L)).thenReturn(user);
        when(userMapper.selectRoleCodeRowsByUserIds(List.of(178L)))
                .thenReturn(List.of(roleRow(178L, 1L, "SUPER_ADMIN")));

        UserAdminResponse result = service.adminDetail(178L);

        assertEquals(List.of("SUPER_ADMIN"), result.getRoleCodes());
        String json = new ObjectMapper().writeValueAsString(result);
        assertFalse(json.contains("password"));
    }

    @Test
    void disablingTheLastEnabledSuperAdminIsRejectedBeforeUpdate() {
        asSuperAdmin();
        User target = user(178L, "admin@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(178L)).thenReturn(target);
        when(userMapper.selectRoleIdsByUserId(178L)).thenReturn(List.of(1L));
        when(userMapper.countEnabledSuperAdmins()).thenReturn(1L);

        assertThrows(CustomConflictException.class,
                () -> service.updateEnabled(178L, false));
        verify(userMapper, never()).updateEnabledById(anyLong(), anyBoolean());
        var order = inOrder(userMapper);
        order.verify(userMapper).lockRoleById(1L);
        order.verify(userMapper).selectByIdForUpdate(178L);
        order.verify(userMapper).selectRoleIdsByUserId(178L);
        order.verify(userMapper).countEnabledSuperAdmins();
    }

    @Test
    void disablingARegularUserUpdatesOnlyEnabledColumn() {
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(3L));
        when(userMapper.updateEnabledById(256L, false)).thenReturn(1);
        when(userMapper.incrementTokenVersionById(256L)).thenReturn(1);
        when(userMapper.selectTokenVersionById(256L)).thenReturn(1L);
        when(tokenBlacklistService.storeTokenVersion(256L, 1L)).thenReturn(1L);

        assertTrue(service.updateEnabled(256L, false));

        assertFalse(target.getEnabled());
        verify(userMapper).updateEnabledById(256L, false);
        verify(tokenBlacklistService).storeTokenVersion(256L, 1L);
    }

    @Test
    void redisVersionAheadAbortsDatabaseVersionRotation() {
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(3L));
        when(userMapper.updateEnabledById(256L, false)).thenReturn(1);
        when(userMapper.incrementTokenVersionById(256L)).thenReturn(1);
        when(userMapper.selectTokenVersionById(256L)).thenReturn(1L);
        when(tokenBlacklistService.storeTokenVersion(256L, 1L)).thenReturn(2L);

        assertThrows(CustomBusinessException.class,
                () -> service.updateEnabled(256L, false));
    }

    @Test
    void unchangedEnabledStateDoesNotRotateTokenVersion() {
        User target = user(256L, "user@example.com", false);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);

        assertTrue(service.updateEnabled(256L, false));

        verify(userMapper, never()).updateEnabledById(anyLong(), anyBoolean());
        verify(userMapper, never()).incrementTokenVersionById(anyLong());
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void ordinaryAdminCannotDisableSuperAdmin() {
        UserContext.set(99L);
        User target = user(178L, "admin@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(178L)).thenReturn(target);
        when(userMapper.selectRoleIdsByUserId(178L)).thenReturn(List.of(1L));
        when(userMapper.selectRoleCodesByUserId(99L)).thenReturn(List.of("ADMIN"));

        assertThrows(CustomForbiddenException.class,
                () -> service.updateEnabled(178L, false));
        verify(userMapper, never()).updateEnabledById(anyLong(), anyBoolean());
    }

    @Test
    void ordinaryAdminCannotReplaceRolesBeforeAnyWrite() {
        UserContext.set(99L);
        when(userMapper.selectRoleCodesByUserId(99L)).thenReturn(List.of("ADMIN"));

        assertThrows(CustomForbiddenException.class,
                () -> service.replaceRoles(256L, List.of(2L, 3L)));
        verify(userMapper, never()).lockRoleById(anyLong());
        verify(userMapper, never()).deleteRolesByUserId(anyLong());
        verify(userMapper, never()).insertUserRole(anyLong(), anyLong(), anyLong());
    }

    @Test
    void removingTheOnlySuperAdminRoleIsRejectedAndNothingIsDeleted() {
        asSuperAdmin();
        User target = user(178L, "admin@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(178L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(2L))).thenReturn(List.of(2L));
        when(userMapper.selectRoleIdsByUserId(178L)).thenReturn(List.of(1L));
        when(userMapper.countSuperAdmins()).thenReturn(1L);

        assertThrows(CustomConflictException.class,
                () -> service.replaceRoles(178L, List.of(2L)));
        verify(userMapper, never()).deleteRolesByUserId(anyLong());
        verify(userMapper, never()).insertUserRole(anyLong(), anyLong(), anyLong());
    }

    @Test
    void removingRoleFromLastEnabledSuperAdminIsRejected() {
        asSuperAdmin();
        User target = user(178L, "admin@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(178L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(2L))).thenReturn(List.of(2L));
        when(userMapper.selectRoleIdsByUserId(178L)).thenReturn(List.of(1L));
        when(userMapper.countSuperAdmins()).thenReturn(2L);
        when(userMapper.countEnabledSuperAdmins()).thenReturn(1L);

        assertThrows(CustomConflictException.class,
                () -> service.replaceRoles(178L, List.of(2L)));
        verify(userMapper, never()).deleteRolesByUserId(anyLong());
    }

    @Test
    void replacingRolesDeletesThenInsertsEveryUniqueRoleInOneTransaction() {
        asSuperAdmin();
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(1L, 2L))).thenReturn(List.of(1L, 2L));
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(3L));
        when(userMapper.deleteRolesByUserId(256L)).thenReturn(1);
        when(userMapper.insertUserRole(eq(256L), anyLong(), anyLong())).thenReturn(1);
        when(userMapper.incrementTokenVersionById(256L)).thenReturn(1);
        when(userMapper.selectTokenVersionById(256L)).thenReturn(1L);
        when(tokenBlacklistService.storeTokenVersion(256L, 1L)).thenReturn(1L);

        assertTrue(service.replaceRoles(256L, List.of(2L, 1L, 2L)));

        var order = inOrder(userMapper);
        order.verify(userMapper).lockRoleById(1L);
        order.verify(userMapper).selectByIdForUpdate(256L);
        order.verify(userMapper).selectExistingRoleIds(List.of(1L, 2L));
        order.verify(userMapper).selectRoleIdsByUserId(256L);
        order.verify(userMapper).deleteRolesByUserId(256L);
        order.verify(userMapper).insertUserRole(eq(256L), anyLong(), eq(1L));
        order.verify(userMapper).insertUserRole(eq(256L), anyLong(), eq(2L));
        verify(tokenBlacklistService).storeTokenVersion(256L, 1L);
    }

    @Test
    void unchangedRoleSetDoesNotRotateTokenVersion() {
        asSuperAdmin();
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(2L, 3L))).thenReturn(List.of(2L, 3L));
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(2L, 3L));

        assertTrue(service.replaceRoles(256L, List.of(3L, 2L, 3L)));

        verify(userMapper, never()).deleteRolesByUserId(anyLong());
        verify(userMapper, never()).incrementTokenVersionById(anyLong());
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void unknownOrEmptyRolesAreValidationFailuresBeforeWrites() {
        assertThrows(CustomValidationException.class,
                () -> service.replaceRoles(256L, List.of()));
        assertThrows(CustomValidationException.class,
                () -> service.replaceRoles(256L, null));
        asSuperAdmin();
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L))
                .thenReturn(user(256L, "u@example.com", true));
        when(userMapper.selectExistingRoleIds(List.of(999L))).thenReturn(List.of());

        assertThrows(CustomValidationException.class,
                () -> service.replaceRoles(256L, List.of(999L)));
        verify(userMapper, never()).deleteRolesByUserId(anyLong());
    }

    @Test
    void roleWriteCountMismatchesFailTheTransaction() {
        asSuperAdmin();
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(2L))).thenReturn(List.of(2L));
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(3L, 4L));
        when(userMapper.deleteRolesByUserId(256L)).thenReturn(1);

        assertThrows(CustomBusinessException.class,
                () -> service.replaceRoles(256L, List.of(2L)));
        verify(userMapper, never()).insertUserRole(anyLong(), anyLong(), anyLong());
    }

    @Test
    void roleInsertCountMismatchFailsTheTransaction() {
        asSuperAdmin();
        User target = user(256L, "user@example.com", true);
        when(userMapper.lockRoleById(1L)).thenReturn(1L);
        when(userMapper.selectByIdForUpdate(256L)).thenReturn(target);
        when(userMapper.selectExistingRoleIds(List.of(2L))).thenReturn(List.of(2L));
        when(userMapper.selectRoleIdsByUserId(256L)).thenReturn(List.of(3L));
        when(userMapper.deleteRolesByUserId(256L)).thenReturn(1);
        when(userMapper.insertUserRole(eq(256L), anyLong(), eq(2L))).thenReturn(0);

        assertThrows(CustomBusinessException.class,
                () -> service.replaceRoles(256L, List.of(2L)));
    }

    private User user(Long id, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("用户" + id);
        user.setEnabled(enabled);
        return user;
    }

    private void asSuperAdmin() {
        UserContext.set(178L);
        when(userMapper.selectRoleCodesByUserId(178L)).thenReturn(List.of("SUPER_ADMIN"));
    }

    private UserRoleCodeRow roleRow(Long userId, Long roleId, String code) {
        UserRoleCodeRow row = new UserRoleCodeRow();
        row.setUserId(userId);
        row.setRoleId(roleId);
        row.setRoleCode(code);
        return row;
    }
}
