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

package com.interviewpilot.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.convention.exception.ServiceException;
import com.interviewpilot.common.enums.UserErrorCodeEnum;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.dao.mapper.UserMapper;
import com.interviewpilot.user.api.io.req.UserLoginReqDTO;
import com.interviewpilot.user.api.io.req.UserPageReqDTO;
import com.interviewpilot.user.api.io.req.UserRegisterReqDTO;
import com.interviewpilot.user.api.io.req.UserUpdateReqDTO;
import com.interviewpilot.user.api.io.resp.UserLoginRespDTO;
import com.interviewpilot.user.api.io.resp.UserPageRespDTO;
import com.interviewpilot.user.api.io.resp.UserRespDTO;
import com.interviewpilot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.interviewpilot.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.interviewpilot.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.interviewpilot.common.enums.UserErrorCodeEnum.USER_EXIST;
import static com.interviewpilot.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.interviewpilot.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * User service implementation.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public UserDO getByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public UserDO getByPhone(String phone) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getPhone, phone);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public Boolean hasUsername(String username) {
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        // Bloom filter 说"已存在"时，再查数据库确认（避免误判）
        if (!hasUsername(requestParam.getUsername())) {
            UserDO existing = baseMapper.selectOne(
                    Wrappers.lambdaQuery(UserDO.class)
                            .eq(UserDO::getUsername, requestParam.getUsername())
                            .eq(UserDO::getDelFlag, 0));
            if (existing != null) {
                throw new ClientException(USER_NAME_EXIST);
            }
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        if (!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (inserted < 1) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam, String currentUsername) {
        if (requestParam == null) {
            throw new ClientException("request body cannot be empty");
        }
        if (StrUtil.isBlank(currentUsername)) {
            throw new ClientException("current user is not logged in");
        }
        if (StrUtil.isNotBlank(requestParam.getUsername()) && !currentUsername.equals(requestParam.getUsername())) {
            throw new ClientException("no permission to update other users");
        }

        requestParam.setUsername(currentUsername);
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, currentUsername);
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        UserDO userDO = baseMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getUsername, requestParam.getUsername())
                        .eq(UserDO::getDelFlag, 0));
        if (userDO == null) {
            throw new ClientException("user does not exist");
        }
        if (!userDO.getPassword().equals(requestParam.getPassword())) {
            throw new ClientException("incorrect password");
        }

        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + requestParam.getUsername());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("user login state is invalid"));
            return new UserLoginRespDTO(token);
        }

        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token) != null;
    }

    @Override
    public void logout(String username, String token) {
        if (checkLogin(username, token)) {
            stringRedisTemplate.delete(USER_LOGIN_KEY + username);
            return;
        }
        throw new ClientException("user token does not exist or user is not logged in");
    }

    @Override
    public IPage<UserPageRespDTO> pageUsers(UserPageReqDTO requestParam) {
        Page<UserDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getDelFlag, 0);

        if (requestParam.getKeyword() != null && !requestParam.getKeyword().trim().isEmpty()) {
            String keyword = requestParam.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(UserDO::getUsername, keyword)
                    .or().like(UserDO::getRealName, keyword)
                    .or().like(UserDO::getMail, keyword)
            );
        }

        if (requestParam.getCreateTimeSort() != null) {
            if ("asc".equalsIgnoreCase(requestParam.getCreateTimeSort())) {
                queryWrapper.orderByAsc(UserDO::getCreateTime);
            } else {
                queryWrapper.orderByDesc(UserDO::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(UserDO::getCreateTime);
        }

        IPage<UserDO> userPage = baseMapper.selectPage(page, queryWrapper);

        Page<UserPageRespDTO> resultPage = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        resultPage.setTotal(userPage.getTotal());

        if (CollUtil.isNotEmpty(userPage.getRecords())) {
            resultPage.setRecords(userPage.getRecords().stream()
                    .map(userDO -> {
                        UserPageRespDTO respDTO = new UserPageRespDTO();
                        BeanUtils.copyProperties(userDO, respDTO);
                        respDTO.setIsAdmin("admin".equals(userDO.getRole()));
                        return respDTO;
                    })
                    .toList());
        }

        return resultPage;
    }
}
