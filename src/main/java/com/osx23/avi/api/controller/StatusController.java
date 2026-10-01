package com.osx23.avi.api.controller;

import com.osx23.avi.api.service.OpenAiTranscriptionService;
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

    private final SupabaseRegistroService supabase;
    private final OpenAiTranscriptionService transcription;

    public StatusController(
            SupabaseRegistroService supabase,
            OpenAiTranscriptionService transcription
    ) {
        this.supabase = supabase;
        this.transcription = transcription;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api", "ok");
        result.put("version", "0.3.0-v1");
        result.put("parser", "avi-voice-v1.1");
        result.put("supabaseConfigurado", supabase.isConfigured());
        result.put("transcripcionConfigurada", transcription.isConfigured());
        result.put("modeloTranscripcion", transcription.model());
        result.put("timestamp", OffsetDateTime.now().toString());
        return result;
    }
}
