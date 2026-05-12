package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * APP级别 Controller
 */
@RestController
@RequestMapping("/api/v1/app")
public class AppController {

    // 当前线上最新版本（修改此处可触发App端更新提示）
    private static final String CURRENT_VERSION = "1.0.0";
    private static final int CURRENT_VERSION_CODE = 2;  // 必须大于Android包的versionCode=1
    private static final String UPDATE_URL = "http://43.139.56.124:8080/api/v1/app/download/apk";  // APK下载链接
    private static final String UPDATE_CONTENT = "1. 优化性能，提升加载速度\n2. 修复已知问题\n3. 体验优化";

    /**
     * 获取APP版本
     * 客户端versionCode=1 < 服务端versionCode=2 时，hasUpdate=true
     */
    @GetMapping("/version")
    public ApiResponse<Map<String, Object>> getAppVersion() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", CURRENT_VERSION);
        data.put("versionCode", CURRENT_VERSION_CODE);
        data.put("hasUpdate", true);  // 演示用始终返回true，上线前改为动态比较
        data.put("updateUrl", UPDATE_URL);
        data.put("updateContent", UPDATE_CONTENT);
        return ApiResponse.success(data);
    }

    /**
     * 下载 APK（用于应用内更新测试）
     */
    @GetMapping("/download/apk")
    public ResponseEntity<Resource> downloadApk() {
        File file = new File("/tmp/mooket.apk");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mooket.apk")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}