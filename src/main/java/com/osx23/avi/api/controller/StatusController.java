package com.osx23.avi.api.controller;

import com.osx23.avi.api.service.SupabaseRegistroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class StatusController {

    private final SupabaseRegistroService service;

    public StatusController(SupabaseRegistroService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api", "ok");
        result.put("version", "0.2.0-v1");
        result.put("parser", "avi-voice-v1.1");
        result.put("supabaseConfigurado", service.isConfigured());
        result.put("timestamp", OffsetDateTime.now().toString());
        return result;
    }
}
