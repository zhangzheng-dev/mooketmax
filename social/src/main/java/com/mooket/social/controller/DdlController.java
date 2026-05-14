package com.mooket.social.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 临时 DDL/DML 执行控制器（建表后删除）
 */
@RestController
@RequestMapping("/internal")
@ConditionalOnProperty(name = "mooket.internal.sql.enabled", havingValue = "true")
public class DdlController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${mooket.internal.sql.allow-writes:false}")
    private boolean allowWrites;

    @PostMapping("/ddl/execute")
    public Map<String, Object> executeDdl(@RequestBody String sql) {
        Map<String, Object> result = new HashMap<>();
        try {
            String trimmed = sql.trim();
            if (trimmed.toUpperCase().startsWith("SELECT")) {
                var list = jdbcTemplate.queryForList(sql);
                result.put("code", 200);
                result.put("message", "查询成功");
                result.put("data", list);
            } else {
                if (!allowWrites) {
                    result.put("code", 403);
                    result.put("message", "SQL writes are disabled");
                    return result;
                }
                int rows = jdbcTemplate.update(sql);
                result.put("code", 200);
                result.put("message", "执行成功");
                result.put("affectedRows", rows);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "执行失败: " + e.getMessage());
        }
        return result;
    }
}
