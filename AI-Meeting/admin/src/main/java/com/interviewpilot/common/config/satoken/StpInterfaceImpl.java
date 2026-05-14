package com.interviewpilot.common.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     * 统一从 t_user.role 字段读取，默认为 student
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of(resolveRole(loginId));
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     * 统一从 t_user.role 字段读取，默认为 student
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of(resolveRole(loginId));
    }

    private String resolveRole(Object loginId) {
        UserDO user = userService.getByUsername(String.valueOf(loginId));
        return (user != null && user.getRole() != null) ? user.getRole() : "student";
    }
}
