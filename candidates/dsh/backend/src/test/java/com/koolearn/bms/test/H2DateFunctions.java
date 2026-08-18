package com.koolearn.bms.test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * H2 兼容 MySQL 函数的别名实现（仅测试用）：
 * DATE_FORMAT(ts, fmt)、DATEDIFF(a, b) 与 MySQL 语义一致。
 */
public final class H2DateFunctions {

    private H2DateFunctions() {
    }

    public static String dateFormat(Timestamp ts, String fmt) {
        if (ts == null || fmt == null) return null;
        String javaFmt = fmt
                .replace("%Y", "yyyy")
                .replace("%m", "MM")
                .replace("%d", "dd")
                .replace("%H", "HH")
                .replace("%u", "w")
                .replace("%", "");
        try {
            return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern(javaFmt));
        } catch (Exception e) {
            return ts.toLocalDateTime().toString();
        }
    }

    /** MySQL DATEDIFF(expr1, expr2) = expr1 - expr2（天数）。 */
    public static int datediff(Timestamp a, Timestamp b) {
        if (a == null || b == null) return 0;
        LocalDateTime la = a.toLocalDateTime();
        LocalDateTime lb = b.toLocalDateTime();
        return (int) java.time.Duration.between(lb, la).toDays();
    }
}
