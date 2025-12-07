package com.group_call.call_backend.controller;

import com.group_call.call_backend.service.RedisWebSocketBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de teste para verificar Redis Pub/Sub cross-server
 */
@RestController
@RequestMapping("/api/test/redis")
public class RedisTestController {

    private static final Logger logger = LoggerFactory.getLogger(RedisTestController.class);
    private final RedisWebSocketBroadcastService redisService;

    public RedisTestController(RedisWebSocketBroadcastService redisService) {
        this.redisService = redisService;
    }

    /**
     * Endpoint de teste para publicar mensagem WebRTC via Redis
     * GET /api/test/redis/publish?targetUserId=123
     */
    @GetMapping("/publish")
    public ResponseEntity<Map<String, Object>> testPublish(@RequestParam Long targetUserId) {
        try {
            String serverId = getServerIdentifier();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "test");
            payload.put("message", "Mensagem de teste do servidor " + serverId);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("serverId", serverId);
            payload.put("targetUserId", targetUserId);
            
            redisService.broadcastWebRTCSignal(targetUserId, payload);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("targetUserId", targetUserId);
            response.put("message", "Mensagem publicada no Redis. Verifique os logs de TODOS os servidores.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao publicar mensagem de teste", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Retorna informações sobre o servidor atual
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        String serverId = getServerIdentifier();
        
        Map<String, Object> info = new HashMap<>();
        info.put("serverId", serverId);
        info.put("flyMachineId", System.getenv("FLY_MACHINE_ID"));
        info.put("hostname", getHostname());
        info.put("redisUrl", System.getenv("REDIS_URL") != null ? "configurado" : "não configurado");
        
        return ResponseEntity.ok(info);
    }
    
    private String getServerIdentifier() {
        String flyMachineId = System.getenv("FLY_MACHINE_ID");
        if (flyMachineId != null && !flyMachineId.isEmpty()) {
            return flyMachineId;
        }
        return getHostname();
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
