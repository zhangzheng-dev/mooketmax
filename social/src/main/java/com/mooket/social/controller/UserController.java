package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import com.mooket.social.common.JwtUtil;
import com.mooket.social.entity.DictUser;
import com.mooket.social.mapper.DictUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户个人中心 Controller
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private DictUserMapper dictUserMapper;

    @Autowired
    private com.mooket.social.mapper.BizSearchHistoryMapper bizSearchHistoryMapper;

    private static final String AVATAR_DIR = "/tmp/mooket/avatar/";
    private static final String AVATAR_BASE_URL = "http://43.139.56.124:8080";

    /**
     * 获取用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        String phone = JwtUtil.getPhone(token);
        DictUser user = dictUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "active")
        );
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        String avatarUrl = user.getAvatarUrl();
        // 拼接完整URL（如果是相对路径）
        if (avatarUrl != null && avatarUrl.startsWith("/avatar/")) {
            avatarUrl = AVATAR_BASE_URL + avatarUrl;
        }
        data.put("avatarUrl", avatarUrl);
        data.put("nickname", user.getNickname());
        data.put("phone", user.getPhone());
        data.put("mooketId", user.getMooketId());
        data.put("mooketNo", user.getMooketNo());
        data.put("realNameStatus", user.getRealNameStatus());
        data.put("realName", user.getRealName());

        // 返回行业身份标签
        String identityTagsStr = user.getIdentityTags();
        if (identityTagsStr != null && !identityTagsStr.isEmpty()) {
            data.put("identityTags", Arrays.asList(identityTagsStr.split(",")));
        } else {
            data.put("identityTags", Arrays.asList());
        }

        return ApiResponse.success(data);
    }

    /**
     * 更新用户资料
     */
    @PostMapping("/profile/update")
    public ApiResponse<Map<String, String>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        String phone = JwtUtil.getPhone(token);
        DictUser user = dictUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "active")
        );
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        String nickname = (String) request.get("nickname");
        // realName 不允许自行修改，只能通过实名认证
        @SuppressWarnings("unchecked")
        java.util.List<String> identityTags = (java.util.List<String>) request.get("identityTags");

        if (nickname != null) {
            if (nickname.length() < 2 || nickname.length() > 20) {
                return ApiResponse.error(400, "昵称需2-20个字符");
            }
            user.setNickname(nickname);
        }

        // 更新行业身份标签
        if (identityTags != null) {
            user.setIdentityTags(String.join(",", identityTags));
        }

        user.setUpdateTime(java.time.LocalDateTime.now());
        dictUserMapper.updateById(user);

        Map<String, String> data = new HashMap<>();
        data.put("message", "更新成功");
        return ApiResponse.success(data);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar/upload")
    public ApiResponse<Map<String, String>> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {

        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        Long userId = JwtUtil.getUserId(token);

        // 创建目录
        File dir = new File(AVATAR_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成文件名：userId_timestamp.jpg
        String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(AVATAR_DIR, filename);

        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            return ApiResponse.error(500, "上传失败");
        }

        // 更新数据库头像URL（存相对路径）
        String avatarUrlPath = "/avatar/" + filename;
        DictUser user = dictUserMapper.selectById(userId);
        if (user != null) {
            user.setAvatarUrl(avatarUrlPath);
            user.setUpdateTime(java.time.LocalDateTime.now());
            dictUserMapper.updateById(user);
        }

        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrlPath);
        data.put("message", "上传成功");
        return ApiResponse.success(data);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        // JWT 无状态，客户端直接删除 token 即可
        // 这里可以记录退出日志

        Map<String, String> data = new HashMap<>();
        data.put("message", "退出成功");
        return ApiResponse.success(data);
    }

    /**
     * 获取APP版本
     */
    @GetMapping("/app/version")
    public ApiResponse<Map<String, Object>> getAppVersion() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", "1.0");
        data.put("hasUpdate", false);
        return ApiResponse.success(data);
    }

    /**
     * 注销账号
     * 1. 将用户cancellation_status改为cancelled
     * 2. 删除该用户关联的所有搜索历史记录
     */
    @PostMapping("/cancel-account")
    public ApiResponse<Map<String, String>> cancelAccount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        String phone = JwtUtil.getPhone(token);
        DictUser user = dictUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "active")
        );
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        // 1. 更新注销状态
        user.setCancellationStatus("cancelled");
        user.setUpdateTime(java.time.LocalDateTime.now());
        dictUserMapper.updateById(user);

        // 2. 删除该用户关联的所有搜索历史
        if (bizSearchHistoryMapper != null) {
            bizSearchHistoryMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.mooket.social.entity.BizSearchHistory>()
                    .eq("user_id", user.getUserId()));
        }

        Map<String, String> data = new HashMap<>();
        data.put("message", "注销成功");
        return ApiResponse.success(data);
    }
}