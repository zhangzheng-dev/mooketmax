package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import com.mooket.social.common.JwtUtil;
import com.mooket.social.entity.DictUser;
import com.mooket.social.gateway.GatewayOAuthClient;
import com.mooket.social.mapper.DictUserMapper;
import com.mooket.social.entity.uac.UacUser;
import com.mooket.social.uac.mapper.UacUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 登录注册 Controller
 * 对标 RN 项目 MooketQuant-mobile/src/api/auth.ts
 * 走 gateway.mujidigital.com/uac 的认证接口
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private DictUserMapper dictUserMapper;

    @Autowired
    private UacUserMapper uacUserMapper;

    @Autowired
    private GatewayOAuthClient gatewayOAuthClient;

    // 内存存储：手机号 → 冷却截止时间
    private final Map<String, Long> smsCooldownStore = new ConcurrentHashMap<>();
    private final AtomicLong codeCounter = new AtomicLong(System.currentTimeMillis() / 1000);

    /**
     * 发送验证码
     * POST /api/v1/auth/send-code
     * 转发到 gateway.mujidigital.com/uac/auth/code/sms
     */
    @PostMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {

        String phone = request.get("phone");
        if (phone == null || phone.length() != 11) {
            return ApiResponse.error(400, "手机号格式错误");
        }

        // 检查60秒冷却
        Long cooldownEnd = smsCooldownStore.get(phone);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return ApiResponse.error(400, "请稍后再试");
        }

        try {
            // 1. 检查账号是否被禁用（优先检查）
            boolean isDisabled = gatewayOAuthClient.isUserDisabled(phone);
            if (isDisabled) {
                return ApiResponse.error(403, "账号已被禁用");
            }

            // 2. 检查手机号注册状态
            GatewayOAuthClient.MobileRegisterCheckResult registerCheck =
                gatewayOAuthClient.checkMobileRegister(phone);

            // 3. 发送短信验证码（用真实设备ID）
            String realDeviceId = deviceId != null ? deviceId : "server";
            gatewayOAuthClient.sendSmsCode(phone, realDeviceId);

            // 设置60秒冷却
            smsCooldownStore.put(phone, System.currentTimeMillis() + 60000);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "验证码已发送");
            data.put("isRegistered", registerCheck.isRegistered);
            data.put("clientId", String.valueOf(System.currentTimeMillis()));
            return ApiResponse.success(data);

        } catch (GatewayOAuthClient.GatewayException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 短信验证码登录
     * POST /api/v1/auth/login
     * 转发到 gateway.mujidigital.com/uac/oauth/token
     * 登录成功后从 uac_user 同步用户信息到 dict_user
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");
        String clientId = request.get("clientId");
        String deviceId = request.get("deviceId");

        if (phone == null || code == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return ApiResponse.error(400, "手机号格式错误");
        }

        if (!code.matches("^\\d{4,6}$")) {
            return ApiResponse.error(400, "请输入正确的验证码");
        }

        try {
            // 1. 检查手机号注册状态（用于判断是否新用户）
            GatewayOAuthClient.MobileRegisterCheckResult registerCheck =
                gatewayOAuthClient.checkMobileRegister(phone);

            // 2. 调用 gateway 短信登录
            String realDeviceId = deviceId != null ? deviceId : "server";
            GatewayOAuthClient.GatewayTokenResult tokenResult =
                gatewayOAuthClient.loginWithSmsCode(phone, code, clientId, realDeviceId);

            String accessToken = tokenResult.accessToken;
            String gatewayUserId = tokenResult.userId;
            String tokenPrefix = accessToken != null && accessToken.length() >= 20 ? accessToken.substring(0, 20) : accessToken;
            System.out.println("【AuthController】手机号=" + phone + " gatewayUserId=" + gatewayUserId + " accessToken前20=" + tokenPrefix);

            // 3. 同步 uac_user → dict_user
            DictUser user = syncUserFromUac(phone, gatewayUserId);

            // 4. 生成 JWT
            String jwtToken = JwtUtil.generateToken(user.getUserId(), phone);

            // 清除冷却
            smsCooldownStore.remove(phone);

            Map<String, Object> data = new HashMap<>();
            data.put("token", jwtToken);
            data.put("isNewUser", !registerCheck.isRegistered); // 根据注册状态判断
            data.put("userId", user.getUserId());
            data.put("phone", user.getPhone());
            data.put("nickname", user.getNickname());
            data.put("mooketId", user.getMooketId());
            data.put("gatewayAccessToken", accessToken);
            data.put("gatewayUserId", gatewayUserId);

            return ApiResponse.success(data);

        } catch (GatewayOAuthClient.GatewayException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    /**
     * 注册（完善资料）
     * POST /api/v1/auth/register
     * 转发到 gateway.mujidigital.com/uac/finishBootstrap
     * 完成后调用 /oauth/token 完成登录
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        Long userId = JwtUtil.getUserId(token);
        String phone = JwtUtil.getPhone(token);
        String nickname = (String) request.get("nickname");
        @SuppressWarnings("unchecked")
        List<String> identityTags = (List<String>) request.get("identityTags");
        String gatewayAccessToken = (String) request.get("gatewayAccessToken");
        String code = (String) request.get("code");
        String clientId = (String) request.get("clientId");
        String deviceId = (String) request.get("deviceId");

        if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
            return ApiResponse.error(400, "昵称需2-20个字符");
        }
        if (identityTags == null || identityTags.isEmpty()) {
            return ApiResponse.error(400, "请选择至少一个身份");
        }
        if (gatewayAccessToken == null || gatewayAccessToken.isEmpty()) {
            return ApiResponse.error(400, "缺失gatewayToken");
        }
        if (code == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        try {
            // 1. 调用 gateway 完善资料
            gatewayOAuthClient.finishBootstrap(gatewayAccessToken, nickname, identityTags);

            // 2. 调用 /oauth/token 完成登录（用相同的手机号和验证码）
            String realDeviceId = deviceId != null ? deviceId : "server";
            GatewayOAuthClient.GatewayTokenResult tokenResult =
                gatewayOAuthClient.loginWithSmsCode(phone, code, clientId, realDeviceId);

            // 3. 更新 dict_user
            DictUser user = dictUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                    .eq("phone", phone)
                    .eq("cancellation_status", "active")
            );
            if (user == null) {
                return ApiResponse.error(404, "用户不存在");
            }
            user.setNickname(nickname);
            user.setIdentityTags(String.join(",", identityTags));
            user.setUpdateTime(java.time.LocalDateTime.now());
            dictUserMapper.updateById(user);

            // 4. 生成新的 JWT
            String jwtToken = JwtUtil.generateToken(user.getUserId(), phone);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "注册成功");
            data.put("token", jwtToken);
            data.put("userId", user.getUserId());
            data.put("phone", user.getPhone());
            data.put("nickname", nickname);
            data.put("mooketId", user.getMooketId());
            data.put("gatewayAccessToken", tokenResult.accessToken);
            data.put("gatewayUserId", tokenResult.userId);
            return ApiResponse.success(data);

        } catch (GatewayOAuthClient.GatewayException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/userinfo")
    public ApiResponse<DictUser> getUserInfo(@RequestHeader("Authorization") String authHeader) {
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

        return ApiResponse.success(user);
    }

    /**
     * 登录成功后，从 uac_user 同步用户信息到 dict_user
     * 对标 RN 项目登录后持久化 userInfo 到 AsyncStorage
     */
    private DictUser syncUserFromUac(String phone, String gatewayUserId) {
        // 查询是否已有活跃用户
        DictUser user = dictUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "active")
        );

        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = new DictUser();
            user.setPhone(phone);
            user.setCreateTime(java.time.LocalDateTime.now());
        }

        // 从 uac_user 查询信息
        UacUser uacUser = uacUserMapper.selectByPhone(phone);
        if (uacUser != null) {
            user.setMooketNo(uacUser.getUserMujiNo());
            user.setMooketId(String.valueOf(uacUser.getId()));
            user.setNickname(uacUser.getNickName());
            // 头像URL
            user.setAvatarUrl(uacUser.getAnonymousFaceUrl());
            // 实名状态：0=未实名(pending), 1=已实名(verified)
            if (uacUser.getIsIdentification() != null) {
                user.setRealNameStatus(uacUser.getIsIdentification() == 1 ? "verified" : "pending");
            }
        }

        user.setUpdateTime(java.time.LocalDateTime.now());

        if (isNewUser) {
            dictUserMapper.insert(user);
        } else {
            dictUserMapper.updateById(user);
        }

        return user;
    }
}