package com.mooket.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：头像等静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String AVATAR_DIR = "/tmp/mooket/avatar/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /avatar/* 请求映射到服务器本地目录 /tmp/mooket/avatar/
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + AVATAR_DIR);
    }
}
