package com.github.dimitryivaniuta.gateway.stepupauth.service.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Builds JSON payloads for monitoring events. */
@Component
public class MonitoringEventFactory {
    private final ObjectMapper mapper;
    public MonitoringEventFactory(ObjectMapper mapper) { this.mapper = mapper; }

    public Map<String,Object> base(String type, UUID userId, UUID aggregateId) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("ts", Instant.now().toString());
        m.put("userId", userId.toString());
        m.put("aggregateId", aggregateId.toString());
        return m;
    }

    public String toJson(Map<String,Object> map) {
        try { return mapper.writeValueAsString(map); }
        catch (Exception e) { throw new IllegalStateException("Serialize failed", e); }
    }
}
