package com.interviewpilot.user.api.io.req;

import lombok.Data;

@Data
public class UserChangePasswordReqDTO {

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
