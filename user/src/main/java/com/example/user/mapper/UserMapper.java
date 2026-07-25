package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.user.entity.User;
import com.example.user.model.UserRoleCodeRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    @Insert("insert into user_role (id, user_id, role_id, created_at) values (#{rowId}, #{userId}, 3, now())")
    int insertDefaultUserRole(@Param("userId") Long userId, @Param("rowId") Long rowId);

    @Select("SELECT r.code FROM role r JOIN user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT DISTINCT p.code FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.permission_id " +
            "JOIN user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(Long userId);

    @Select({"<script>",
            "select ur.user_id, ur.role_id, r.code as role_code",
            "from user_role ur join role r on r.id = ur.role_id",
            "where r.deleted = 0 and ur.user_id in",
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "order by ur.user_id asc, ur.role_id asc",
            "</script>"})
    @Results(id = "userRoleCodeRow", value = {
            @Result(column = "user_id", property = "userId"),
            @Result(column = "role_id", property = "roleId"),
            @Result(column = "role_code", property = "roleCode")
    })
    List<UserRoleCodeRow> selectRoleCodeRowsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Select("select id from role where id = #{roleId} and deleted = 0 for update")
    Long lockRoleById(@Param("roleId") Long roleId);

    @Select("select * from user where id = #{id} and deleted = 0 for update")
    User selectByIdForUpdate(@Param("id") Long id);

    @Select("select count(distinct ur.user_id) from user_role ur "
            + "join user u on u.id = ur.user_id and u.deleted = 0 "
            + "where ur.role_id = 1")
    long countSuperAdmins();

    @Select("select count(distinct ur.user_id) from user_role ur "
            + "join user u on u.id = ur.user_id and u.deleted = 0 "
            + "where ur.role_id = 1 and u.enabled = 1")
    long countEnabledSuperAdmins();

    @Update("update user set enabled = #{enabled}, updated_at = now() "
            + "where id = #{id} and deleted = 0")
    int updateEnabledById(@Param("id") Long id, @Param("enabled") Boolean enabled);

    @Update("update user set token_version = token_version + 1, updated_at = now() "
            + "where id = #{id} and deleted = 0")
    int incrementTokenVersionById(@Param("id") Long id);

    @Select("select token_version from user where id = #{id} and deleted = 0")
    Long selectTokenVersionById(@Param("id") Long id);

    @Select("select count(*) from user where name = #{name} and id <> #{userId} and deleted = 0")
    long countActiveByNameExcludingId(@Param("name") String name, @Param("userId") Long userId);

    @Select("select count(*) from user where mobile = #{mobile} and id <> #{userId} and deleted = 0")
    long countActiveByMobileExcludingId(@Param("mobile") String mobile, @Param("userId") Long userId);

    @Update("update user set name = #{name}, mobile = #{mobile}, avatar = #{avatar}, "
            + "updated_at = now(), updated_by = #{userId} where id = #{userId} and deleted = 0")
    int updateProfileById(@Param("userId") Long userId,
                          @Param("name") String name,
                          @Param("mobile") String mobile,
                          @Param("avatar") String avatar);

    @Update("update user set password = #{password}, token_version = token_version + 1, "
            + "updated_at = now(), updated_by = #{userId} where id = #{userId} and deleted = 0")
    int updatePasswordAndIncrementTokenVersion(@Param("userId") Long userId,
                                               @Param("password") String password);

    @Select("select role_id from user_role where user_id = #{userId} order by role_id")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select({"<script>",
            "select id from role where deleted = 0 and id in",
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "order by id",
            "</script>"})
    List<Long> selectExistingRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Delete("delete from user_role where user_id = #{userId}")
    int deleteRolesByUserId(@Param("userId") Long userId);

    @Insert("insert into user_role (id, user_id, role_id, created_at) "
            + "values (#{rowId}, #{userId}, #{roleId}, now())")
    int insertUserRole(@Param("userId") Long userId,
                       @Param("rowId") Long rowId,
                       @Param("roleId") Long roleId);
}
