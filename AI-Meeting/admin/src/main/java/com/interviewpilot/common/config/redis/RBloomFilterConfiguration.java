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

package com.interviewpilot.common.config.redis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.dao.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 布隆过滤器配置

 */
@Slf4j
@Configuration(value = "rBloomFilterConfigurationByAdmin")
public class RBloomFilterConfiguration {

    /**
     * 防止用户注册查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("userRegisterCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(100000000L, 0.001);
        return cachePenetrationBloomFilter;
    }

    /**
     * 应用启动时将已有用户名预加载到布隆过滤器，避免误判
     */
    @Bean
    public ApplicationRunner bloomFilterPreloader(
            RBloomFilter<String> userRegisterCachePenetrationBloomFilter,
            UserMapper userMapper) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                List<UserDO> users = userMapper.selectList(
                        Wrappers.lambdaQuery(UserDO.class)
                                .select(UserDO::getUsername)
                                .eq(UserDO::getDelFlag, 0)
                );
                if (users == null || users.isEmpty()) {
                    log.info("Bloom filter preloader: no existing usernames found, skipping");
                    return;
                }
                int count = 0;
                for (UserDO user : users) {
                    if (user.getUsername() != null) {
                        userRegisterCachePenetrationBloomFilter.add(user.getUsername());
                        count++;
                    }
                }
                log.info("Bloom filter preloader: loaded {} usernames into filter", count);
            }
        };
    }

    /**
     * 防止分组标识注册查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> gidRegisterCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("gidRegisterCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(200000000L, 0.001);
        return cachePenetrationBloomFilter;
    }
}