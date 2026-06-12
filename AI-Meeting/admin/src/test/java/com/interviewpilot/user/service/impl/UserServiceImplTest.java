package com.interviewpilot.user.service.impl;

import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.interview.dao.mapper.InterviewRecordMapper;
import com.interviewpilot.user.dao.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private InterviewRecordMapper interviewRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void changePassword_ShouldRejectIncorrectOldPassword() {
        UserDO user = new UserDO();
        user.setUsername("student-user");
        user.setPassword("old-pass");
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(user);

        ClientException ex = assertThrows(ClientException.class,
                () -> userService.changePassword("student-user", "wrong-pass", "new-pass-123"));

        assertThat(ex.getMessage()).isEqualTo("incorrect password");
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.<UserDO>any());
    }

    @Test
    void changePassword_ShouldPersistNewPasswordForCurrentUser() {
        UserDO user = new UserDO();
        user.setId(9L);
        user.setUsername("student-user");
        user.setPassword("old-pass");
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(user);

        userService.changePassword("student-user", "old-pass", "new-pass-123");

        ArgumentCaptor<UserDO> captor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(9L);
        assertThat(captor.getValue().getPassword()).isEqualTo("new-pass-123");
    }
}
