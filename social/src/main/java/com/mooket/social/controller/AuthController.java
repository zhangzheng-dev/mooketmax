package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import com.mooket.social.common.JwtUtil;
import com.mooket.social.entity.DictUser;
import com.mooket.social.mapper.DictUserMapper;
import com.mooket.social.entity.uac.UacUser;
import com.mooket.social.uac.mapper.UacUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 登录注册 Controller
 * 开发阶段使用内存存储验证码（生产需换Redis）
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private DictUserMapper dictUserMapper;

    @Autowired
    private UacUserMapper uacUserMapper;

    // 内存存储：手机号 → 验证码
    private final Map<String, String> smsCodeStore = new ConcurrentHashMap<>();
    // 内存存储：手机号 → 冷却截止时间
    private final Map<String, Long> smsCooldownStore = new ConcurrentHashMap<>();
    // 验证码计数器
    private final AtomicLong codeCounter = new AtomicLong(System.currentTimeMillis() / 1000);
    private final Random random = new Random();

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ApiResponse<Map<String, String>> sendCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.length() != 11) {
            return ApiResponse.error(400, "手机号格式错误");
        }

        // 检查60秒冷却
        Long cooldownEnd = smsCooldownStore.get(phone);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return ApiResponse.error(400, "请稍后再试");
        }

        // 写死验证码888888，方便测试
        String code = "888888";

        // 存储验证码（15分钟有效期）
        smsCodeStore.put(phone, code);
        // 设置60秒冷却
        smsCooldownStore.put(phone, System.currentTimeMillis() + 60000);

        // TODO: 实际发送短信（生产环境接入短信网关）
        System.out.println("【Mock SMS】phone=" + phone + ", code=" + code);

        Map<String, String> data = new HashMap<>();
        data.put("message", "验证码已发送");
        return ApiResponse.success(data);
    }

    /**
     * 验证码登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        if (phone == null || code == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        // 验证验证码
        String storedCode = smsCodeStore.get(phone);
        if (storedCode == null || !storedCode.equals(code)) {
            return ApiResponse.error(400, "验证码错误");
        }

        // 删除验证码（一次性）
        smsCodeStore.remove(phone);

        // 查询用户（首次登录自动注册）
        DictUser user = dictUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "active")
        );

        // 如果没有活跃用户但有注销用户（同一手机号），保持旧记录不变，插入新记录
        boolean hasCancelledRecord = dictUserMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DictUser>()
                .eq("phone", phone)
                .eq("cancellation_status", "cancelled")
        ) > 0;

        boolean isNewUser = (user == null);
        if (isNewUser) {
            // 自动注册（仅插入手机号，等待注册接口完善资料）
            user = new DictUser();
            user.setPhone(phone);
            user.setCreateTime(java.time.LocalDateTime.now());
            user.setUpdateTime(java.time.LocalDateTime.now());

            // 从UAC查询mooket_no和mooket_id
            UacUser uacUser = uacUserMapper.selectByPhone(phone);
            if (uacUser != null) {
                user.setMooketNo(uacUser.getUserMujiNo());
                user.setMooketId(String.valueOf(uacUser.getId()));
            }

            dictUserMapper.insert(user);
        }

        // 生成 JWT
        String token = JwtUtil.generateToken(user.getUserId(), phone);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("isNewUser", isNewUser);
        data.put("userId", user.getUserId());
        data.put("phone", user.getPhone());
        data.put("nickname", user.getNickname());
        data.put("mooketId", user.getMooketId());

        return ApiResponse.success(data);
    }

    /**
     * 注册（完善资料）
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.validateToken(token)) {
            return ApiResponse.error(401, "Token无效");
        }

        Long userId = JwtUtil.getUserId(token);
        String nickname = (String) request.get("nickname");
        @SuppressWarnings("unchecked")
        java.util.List<String> identityTags = (java.util.List<String>) request.get("identityTags");

        if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
            return ApiResponse.error(400, "昵称需2-20个字符");
        }
        if (identityTags == null || identityTags.isEmpty()) {
            return ApiResponse.error(400, "请选择至少一个身份");
        }

        DictUser user = dictUserMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        user.setNickname(nickname);
        user.setIdentityTags(String.join(",", identityTags));
        user.setUpdateTime(java.time.LocalDateTime.now());
        dictUserMapper.updateById(user);

        Map<String, String> data = new HashMap<>();
        data.put("message", "注册成功");
        return ApiResponse.success(data);
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

        Long userId = JwtUtil.getUserId(token);
        DictUser user = dictUserMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        return ApiResponse.success(user);
    }
}

