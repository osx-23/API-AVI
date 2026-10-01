package com.osx23.avi.api.controller;

import com.osx23.avi.api.service.SupabaseRegistroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return Map.of(
                "api", "ok",
                "supabaseConfigurado", service.isConfigured()
        );
    }
}
