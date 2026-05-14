package com.interviewpilot.auth.infrastructure.satoken;

import com.interviewpilot.auth.application.PermissionService;
import com.interviewpilot.user.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaTokenPermissionService implements PermissionService {

    private final AdminPermissionService adminPermissionService;

    @Override
    public boolean isAdmin(String username) {
        return adminPermissionService.isAdmin(username);
    }
}
