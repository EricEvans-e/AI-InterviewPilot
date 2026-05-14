package com.interviewpilot.common.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.service.AdminPermissionService;
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

    private final AdminPermissionService adminPermissionService;
    private final UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String username = String.valueOf(loginId);
        UserDO user = userService.getByUsername(username);
        if (user != null && "admin".equals(user.getRole())) {
            return List.of("admin");
        }
        if (user != null && "teacher".equals(user.getRole())) {
            return List.of("teacher");
        }
        return List.of("student");
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String username = String.valueOf(loginId);
        // 优先从数据库读取 role 字段
        UserDO user = userService.getByUsername(username);
        if (user != null && user.getRole() != null) {
            return List.of(user.getRole());
        }
        // 兼容旧逻辑
        if (adminPermissionService.isAdmin(username)) {
            return List.of("admin");
        }
        return List.of("student");
    }
}
