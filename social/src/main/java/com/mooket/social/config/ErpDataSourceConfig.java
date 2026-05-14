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
 * ERP 数据库数据源配置 (mallee_muji_erp)
 * 用于 dict_factory 等表的同步
 */
@Configuration
@MapperScan(basePackages = "com.mooket.social.erp.mapper", sqlSessionFactoryRef = "erpSqlSessionFactory")
public class ErpDataSourceConfig {

    @Bean(name = "erpDataSource")
    public DataSource erpDataSource(
            @Value("${mooket.datasource.erp.jdbc-url}") String jdbcUrl,
            @Value("${mooket.datasource.erp.username}") String username,
            @Value("${mooket.datasource.erp.password}") String password) {
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

    @Bean(name = "erpSqlSessionFactory")
    public SqlSessionFactory erpSqlSessionFactory(
            @Qualifier("erpDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage("com.mooket.social.entity.erp");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }
}
