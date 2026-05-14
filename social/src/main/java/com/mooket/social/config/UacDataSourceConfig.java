package com.mooket.social.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * UAC 数据库数据源配置 (mallee_muji_uac)
 */
@Configuration
@MapperScan(basePackages = "com.mooket.social.uac.mapper", sqlSessionFactoryRef = "uacSqlSessionFactory")
public class UacDataSourceConfig {

    @Bean(name = "uacDataSource")
    public DataSource uacDataSource(
            @Value("${mooket.datasource.uac.jdbc-url}") String jdbcUrl,
            @Value("${mooket.datasource.uac.username}") String username,
            @Value("${mooket.datasource.uac.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        return new HikariDataSource(config);
    }

    @Bean(name = "uacSqlSessionFactory")
    public SqlSessionFactory uacSqlSessionFactory(
            @Qualifier("uacDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage("com.mooket.social.entity.uac");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }
}
