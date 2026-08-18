package com.koolearn.bms.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册入参 DTO：只接收注册所需的字段，
 * 避免请求体把 id/status/role/dingtalkUnionId 等字段批量赋值（防提权/防伪造）。
 * 密码复杂度在服务层校验（与修改密码共用 PasswordPolicyUtil）。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    private String username;

    private String password;

    /** 确认密码：与 password 一致才算合法 */
    private String confirmPassword;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String dept;

    /** 注册验证码（非业务字段） */
    private String captchaKey;

    private String captchaCode;
}
