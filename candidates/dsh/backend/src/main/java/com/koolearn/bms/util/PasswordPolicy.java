package com.koolearn.bms.util;

/**
 * 密码策略校验（注册/修改密码共用）：
 * 长度 8~20 位，且包含大写字母、小写字母、数字、特殊符号中的至少 3 类。
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /** 校验通过返回 null，否则返回错误信息。 */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < 8 || password.length() > 20) {
            return "密码长度须为8~20位";
        }
        int kinds = 0;
        if (password.matches(".*[A-Z].*")) kinds++;
        if (password.matches(".*[a-z].*")) kinds++;
        if (password.matches(".*[0-9].*")) kinds++;
        if (password.matches(".*[^a-zA-Z0-9].*")) kinds++;
        if (kinds < 3) {
            return "密码需包含大写字母、小写字母、数字、特殊符号中的至少3类";
        }
        return null;
    }
}
