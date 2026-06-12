/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.interviewpilot.user.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.bean.BeanUtil;
import jakarta.validation.Valid;
import com.interviewpilot.auth.application.LoginSessionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.user.api.io.req.UserLoginReqDTO;
import com.interviewpilot.user.api.io.req.UserPageReqDTO;
import com.interviewpilot.user.api.io.req.UserPhoneLoginReqDTO;
import com.interviewpilot.user.api.io.req.UserRegisterReqDTO;
import com.interviewpilot.user.api.io.req.UserChangePasswordReqDTO;
import com.interviewpilot.user.api.io.req.UserUpdateReqDTO;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.service.SmsCodeService;
import com.interviewpilot.user.api.io.resp.UserActualRespDTO;
import com.interviewpilot.user.api.io.resp.AdminStatsRespDTO;
import com.interviewpilot.user.api.io.resp.UserPageRespDTO;
import com.interviewpilot.user.api.io.resp.UserRespDTO;
import com.interviewpilot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户注册、登录（账号密码 + 手机验证码）、登出、权限管理等接口
 * 使用 Sa-Token 进行会话管理，token 存储在 Redis 中
 */
@RestController
@RequestMapping("/api/ip/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LoginSessionService loginSessionService;
    private final SmsCodeService smsCodeService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 查询用户详细信息（含敏感字段，仅管理员可用）
     */
    @GetMapping("/actual/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 检查用户名是否已被注册（用于前端实时校验）
     */
    @GetMapping("/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 用户注册（账号密码方式）
     * 使用 BloomFilter + 分布式锁防止重复注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 更新用户信息
     */
    @PutMapping
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam,
                               @CurrentUser String currentUsername) {
        userService.update(requestParam, currentUsername);
        return Results.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody UserChangePasswordReqDTO requestParam,
                                       @CurrentUser String currentUsername) {
        if (requestParam == null) {
            throw new ClientException("request body cannot be empty");
        }
        if (requestParam.getNewPassword() == null || requestParam.getNewPassword().trim().isEmpty()) {
            throw new ClientException("new password cannot be empty");
        }
        if (!requestParam.getNewPassword().equals(requestParam.getConfirmPassword())) {
            throw new ClientException("password confirmation does not match");
        }
        userService.changePassword(currentUsername, requestParam.getOldPassword(), requestParam.getNewPassword());
        return Results.success();
    }

    /**
     * 账号密码登录
     * 登录成功返回 token，后续请求需在 Header 中携带 Authorization: Bearer {token}
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginReqDTO requestParam) {
        userService.login(requestParam);
        loginSessionService.login(requestParam.getUsername());

        UserDO user = userService.getByUsername(requestParam.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", loginSessionService.getCurrentToken());
        result.put("username", requestParam.getUsername());
        result.put("role", resolveRole(user));
        return Results.success(result);
    }

    /**
     * 检查当前是否已登录（前端刷新页面时调用，恢复登录态）
     */
    @GetMapping("/check-login")
    public Result<Map<String, Object>> checkLogin() {
        Map<String, Object> result = new HashMap<>();
        boolean isLogin = loginSessionService.isCurrentLoggedIn();
        result.put("isLogin", isLogin);
        if (isLogin) {
            String username = loginSessionService.getCurrentLoginId();
            result.put("username", username);
            result.put("token", loginSessionService.getCurrentToken());
            UserDO user = userService.getByUsername(username);
            result.put("role", resolveRole(user));
        }
        return Results.success(result);
    }

    /**
     * 登出（清除 Redis 中的会话）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        loginSessionService.logoutCurrent();
        return Results.success();
    }

    /**
     * 发送手机验证码
     *
     * @param phone   手机号
     * @param bizType 业务类型（login=登录, register=注册）
     */
    @PostMapping("/send-sms-code")
    public Result<Void> sendSmsCode(@RequestParam String phone,
                                    @RequestParam(defaultValue = "login") String bizType) {
        smsCodeService.sendCode(phone, bizType);
        return Results.success();
    }

    /**
     * 手机验证码登录（未注册用户自动创建账号）
     */
    @PostMapping("/phone-login")
    public Result<Map<String, Object>> phoneLogin(@RequestBody @Valid UserPhoneLoginReqDTO req) {
        if (!smsCodeService.verifyCode(req.getPhone(), req.getCode(), "login")) {
            throw new ClientException("验证码错误或已过期");
        }
        // 查找或创建用户
        UserDO user = userService.getByPhone(req.getPhone());
        if (user == null) {
            user = new UserDO();
            user.setUsername("phone_" + req.getPhone());
            user.setPhone(req.getPhone());
            user.setRole("student");
            user.setPassword(""); // 手机号登录无密码
            userService.save(user);
        }
        // Sa-Token 登录
        loginSessionService.login(user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", loginSessionService.getCurrentToken());
        result.put("username", user.getUsername());
        result.put("role", resolveRole(user));
        return Results.success(result);
    }

    /**
     * 设置用户为管理员（仅管理员可操作）
     */
    @PostMapping("/admin")
    @SaCheckRole("admin")
    public Result<Void> addAdmin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        UserDO user = userService.getByUsername(username);
        if (user == null) {
            throw new ClientException("用户不存在");
        }
        user.setRole("admin");
        userService.updateById(user);
        return Results.success();
    }

    /**
     * 分页查询用户列表（仅管理员）
     */
    @GetMapping("/page")
    @SaCheckRole("admin")
    public Result<IPage<UserPageRespDTO>> pageUsers(UserPageReqDTO requestParam) {
        return Results.success(userService.pageUsers(requestParam));
    }

    /**
     * 管理后台统计数据（仅管理员）
     */
    @GetMapping("/stats")
    @SaCheckRole("admin")
    public Result<AdminStatsRespDTO> getStats() {
        return Results.success(userService.getStats());
    }

    private String resolveRole(UserDO user) {
        return (user != null && user.getRole() != null) ? user.getRole() : "student";
    }
}
