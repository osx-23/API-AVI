package com.osx23.avi.api.controller;

import com.osx23.avi.api.service.LocalWhisperTranscriptionService;
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
    private final LocalWhisperTranscriptionService whisper;

    public StatusController(
            SupabaseRegistroService supabase,
            LocalWhisperTranscriptionService whisper
    ) {
        this.supabase = supabase;
        this.whisper = whisper;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean whisperAvailable = whisper.isAvailable();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api", "ok");
        result.put("version", "0.4.0-v1");
        result.put("parser", "avi-voice-v1.1");
        result.put("supabaseConfigurado", supabase.isConfigured());
        result.put("transcripcionConfigurada", whisperAvailable);
        result.put("motorTranscripcion", "whisper-local");
        result.put("modeloTranscripcion", whisper.modelLabel());
        result.put("timestamp", OffsetDateTime.now().toString());
        return result;
    }
}
