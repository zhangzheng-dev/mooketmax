package com.mooket.social.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL JdbcTemplate 配置
 */
@Configuration
public class PostgreSqlConfig {

    @Bean("pgJdbcTemplate")
    @Primary
    public JdbcTemplate pgJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
