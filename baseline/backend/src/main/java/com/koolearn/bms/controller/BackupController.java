package com.koolearn.bms.controller;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequireRole("admin")
@RestController
@RequestMapping("/backup")
public class BackupController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private static String extractDbName(String url) {
        if (url == null) return "bms_db";
        int idx = url.lastIndexOf("/");
        if (idx < 0) return "bms_db";
        String tail = url.substring(idx + 1);
        int q = tail.indexOf("?");
        return q > 0 ? tail.substring(0, q) : tail;
    }

    @PostMapping("/db")
    public Result<String> backup() {
        try {
            String dbName = extractDbName(datasourceUrl);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dir = System.getProperty("user.home") + "/bms_backup";
            new java.io.File(dir).mkdirs();
            String file = dir + "/" + dbName + "_" + time + ".sql";

            List<String> cmd = new ArrayList<>();
            cmd.add("mysqldump");
            cmd.add("-u" + dbUser);
            cmd.add("-p" + dbPassword);
            cmd.add("--databases");
            cmd.add(dbName);
            cmd.add("--result-file=" + file);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) output.append(line);
            }
            if (p.exitValue() == 0) {
                return Result.success("备份成功: " + file);
            }
            return Result.fail("备份失败: " + output);
        } catch (Exception e) {
            return Result.fail("备份异常: " + e.getMessage());
        }
    }
}
