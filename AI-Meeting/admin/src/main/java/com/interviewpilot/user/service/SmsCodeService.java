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

package com.interviewpilot.user.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.user.dao.entity.SmsCodeDO;
import com.interviewpilot.user.dao.mapper.SmsCodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

/**
 * 短信验证码服务
 */
@Slf4j
@Service
public class SmsCodeService extends ServiceImpl<SmsCodeMapper, SmsCodeDO> {

    /**
     * 发送验证码 (MVP: 直接打印到日志, 生产环境对接短信 SDK)
     */
    public void sendCode(String phone, String bizType) {
        // 60s 防重复
        LambdaQueryWrapper<SmsCodeDO> wrapper = new LambdaQueryWrapper<SmsCodeDO>()
                .eq(SmsCodeDO::getPhone, phone)
                .eq(SmsCodeDO::getBizType, bizType)
                .eq(SmsCodeDO::getUsed, false)
                .gt(SmsCodeDO::getExpireTime, new Date())
                .orderByDesc(SmsCodeDO::getCreateTime)
                .last("LIMIT 1");
        if (this.getOne(wrapper) != null) {
            throw new ClientException("验证码已发送，请60秒后重试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        SmsCodeDO entity = new SmsCodeDO();
        entity.setPhone(phone);
        entity.setCode(code);
        entity.setBizType(bizType);
        entity.setUsed(false);
        entity.setExpireTime(DateUtil.offsetMinute(new Date(), 5));
        this.save(entity);

        // MVP: 日志输出, 生产替换为阿里云/腾讯云短信 SDK
        log.info("[SMS] phone={}, code={}, bizType={}", phone, code, bizType);
    }

    /**
     * 校验验证码
     */
    public boolean verifyCode(String phone, String code, String bizType) {
        LambdaQueryWrapper<SmsCodeDO> wrapper = new LambdaQueryWrapper<SmsCodeDO>()
                .eq(SmsCodeDO::getPhone, phone)
                .eq(SmsCodeDO::getCode, code)
                .eq(SmsCodeDO::getBizType, bizType)
                .eq(SmsCodeDO::getUsed, false)
                .gt(SmsCodeDO::getExpireTime, new Date())
                .orderByDesc(SmsCodeDO::getCreateTime)
                .last("LIMIT 1");
        SmsCodeDO record = this.getOne(wrapper);
        if (record == null) return false;

        record.setUsed(true);
        this.updateById(record);
        return true;
    }
}
