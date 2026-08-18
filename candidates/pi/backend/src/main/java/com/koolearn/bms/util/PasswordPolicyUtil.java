package com.koolearn.bms.util;

/**
 * 密码策略统一校验：8~20 位，且包含大写字母、小写字母、数字、特殊符号中的至少 3 类。
 * 注册与修改密码共用，保证策略一致。
 */
public final class PasswordPolicyUtil {

    private PasswordPolicyUtil() {
    }

    /** 校验通过返回 null，否则返回错误信息 */
    public static String validate(String pwd) {
        if (pwd == null || pwd.length() < 8 || pwd.length() > 20) {
            return "密码长度须为8~20位";
        }
        int kinds = 0;
        if (pwd.matches(".*[A-Z].*")) kinds++;
        if (pwd.matches(".*[a-z].*")) kinds++;
        if (pwd.matches(".*[0-9].*")) kinds++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) kinds++;
        if (kinds < 3) {
            return "密码需包含大写字母、小写字母、数字、特殊符号中的至少3类";
        }
        return null;
    }
}
