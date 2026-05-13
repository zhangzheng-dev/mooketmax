package com.mooket.social.controller;

import com.mooket.social.gateway.GatewayOAuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 库存数据代理接口
 * Android 不直连 gateway.mujidigital.com，而是通过我们后端代理
 * 这样 gateway 只看到一个固定来源（我们 server IP），避免 token 权限校验差异
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryProxyController {

    @Autowired
    private GatewayOAuthClient gatewayOAuthClient;

    /**
     * 查询用户可下载的数据类型权限
     */
    @GetMapping("/queryQuantificationUserAction/{userId}")
    public Object queryQuantificationUserAction(
            @PathVariable String userId,
            @RequestHeader(value = "X-Gateway-Token", required = false) String gatewayToken) {

        System.out.println("【InventoryProxy】queryQuantificationUserAction userId=" + userId + " token=" + (gatewayToken != null ? gatewayToken.substring(0, Math.min(20, gatewayToken.length())) : "null"));

        if (gatewayToken == null || gatewayToken.isBlank()) {
            return Map.of("code", 401, "message", "库存功能未授权");
        }

        try {
            String url = "https://gateway.mujidigital.com/uac/quantification/queryQuantificationUserAction/" + userId;

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Authorization", "Bearer " + gatewayToken);

            int responseCode = conn.getResponseCode();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            System.out.println("【InventoryProxy】queryQuantificationUserAction response: " + responseCode);
            if (responseCode == 200 && response.length() > 0) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.toString(), Object.class);
            }
            return Map.of("code", responseCode, "message", "查询失败");
        } catch (Exception e) {
            System.out.println("【InventoryProxy】权限查询失败: " + e.getMessage());
            return Map.of("code", 500, "message", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 拉取数据快照
     */
    @PostMapping("/pullSnapshots")
    public Object pullSnapshots(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Gateway-Token", required = false) String gatewayToken) {

        System.out.println("【InventoryProxy】pullSnapshots token=" + (gatewayToken != null ? gatewayToken.substring(0, Math.min(20, gatewayToken.length())) : "null") + " body=" + request);
        if (gatewayToken == null || gatewayToken.isBlank()) {
            return Map.of("code", 401, "message", "库存功能未授权");
        }

        try {
            String url = "https://gateway.mujidigital.com/info/server/quantification/infoQuantificationGroupSnapshots/sync/pull";
            String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + gatewayToken);

            conn.getOutputStream().write(body.getBytes("UTF-8"));
            int responseCode = conn.getResponseCode();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            System.out.println("【InventoryProxy】pullSnapshots response: " + responseCode + " body: " + response);
            if (responseCode == 200 && response.length() > 0) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.toString(), Object.class);
            }
            return Map.of("code", responseCode, "message", "拉取快照失败");
        } catch (Exception e) {
            System.out.println("【InventoryProxy】拉取快照异常: " + e.getMessage());
            return Map.of("code", 500, "message", "拉取快照失败: " + e.getMessage());
        }
    }

    /**
     * 查询Spot Market列表
     */
    @PostMapping("/queryServerSpotMarketListWithPage")
    public Object queryServerSpotMarketListWithPage(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Gateway-Token", required = false) String gatewayToken) {

        if (gatewayToken == null || gatewayToken.isBlank()) {
            return Map.of("code", 401, "message", "库存功能未授权");
        }

        try {
            String url = "https://gateway.mujidigital.com/info/serverMtm/queryServerSpotMarketListWithPage";
            String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + gatewayToken);

            conn.getOutputStream().write(body.getBytes("UTF-8"));
            int responseCode = conn.getResponseCode();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            System.out.println("【InventoryProxy】queryServerSpotMarketListWithPage response: " + responseCode);
            if (responseCode == 200 && response.length() > 0) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.toString(), Object.class);
            }
            return Map.of("code", responseCode, "message", "查询失败");
        } catch (Exception e) {
            System.out.println("【InventoryProxy】查询Market列表失败: " + e.getMessage());
            return Map.of("code", 500, "message", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 按市场ID查询Spot Market明细
     */
    @PostMapping("/queryServerSpotMarketDetailListByMarketIdWithPage")
    public Object queryServerSpotMarketDetailListByMarketIdWithPage(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Gateway-Token", required = false) String gatewayToken) {

        if (gatewayToken == null || gatewayToken.isBlank()) {
            return Map.of("code", 401, "message", "库存功能未授权");
        }

        try {
            String url = "https://gateway.mujidigital.com/info/serverMtm/queryServerSpotMarketDetailListByMarketIdWithPage";
            String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + gatewayToken);

            conn.getOutputStream().write(body.getBytes("UTF-8"));
            int responseCode = conn.getResponseCode();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            System.out.println("【InventoryProxy】queryServerSpotMarketDetailListByMarketIdWithPage response: " + responseCode);
            if (responseCode == 200 && response.length() > 0) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.toString(), Object.class);
            }
            return Map.of("code", responseCode, "message", "查询失败");
        } catch (Exception e) {
            System.out.println("【InventoryProxy】查询Market明细失败: " + e.getMessage());
            return Map.of("code", 500, "message", "查询失败: " + e.getMessage());
        }
    }
}
