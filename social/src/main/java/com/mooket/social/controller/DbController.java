package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/db")
@ConditionalOnProperty(name = "mooket.internal.sql.enabled", havingValue = "true")
public class DbController {

    private static final Logger log = LoggerFactory.getLogger(DbController.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean allowWrites;

    public DbController(
            JdbcTemplate jdbcTemplate,
            @Value("${mooket.internal.sql.allow-writes:false}") boolean allowWrites) {
        this.jdbcTemplate = jdbcTemplate;
        this.allowWrites = allowWrites;
    }

    @PostMapping("/exec")
    public ApiResponse<Map<String, Object>> execSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ApiResponse.error("sql is required");
        }
        String trimmed = sql.trim().toLowerCase();
        try {
            if (isReadOnlyStatement(trimmed)) {
                var rows = jdbcTemplate.queryForList(sql);
                return ApiResponse.success(Map.of("rows", rows, "count", rows.size()));
            }
            if (!allowWrites) {
                return ApiResponse.error(403, "SQL writes are disabled");
            }
            for (String singleSql : sql.split(";")) {
                String s = singleSql.trim();
                if (!s.isEmpty()) {
                    log.warn("Executing internal SQL statement: {}", s);
                    jdbcTemplate.execute(s);
                }
            }
            return ApiResponse.success(Map.of("affectedRows", "ok"));
        } catch (Exception e) {
            log.error("SQL error: {}", e.getMessage());
            return ApiResponse.error("SQL error: " + e.getMessage());
        }
    }

    private boolean isReadOnlyStatement(String sql) {
        return sql.startsWith("select") || sql.startsWith("show")
                || sql.startsWith("describe") || sql.startsWith("with");
    }
}
