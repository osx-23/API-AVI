package com.osx23.avi.api.controller;

import com.osx23.avi.api.model.TranscriptionResponse;
import com.osx23.avi.api.service.OpenAiTranscriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class TranscriptionController {

    private final OpenAiTranscriptionService service;

    public TranscriptionController(OpenAiTranscriptionService service) {
        this.service = service;
    }

    @PostMapping(
            value = "/transcribir",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public TranscriptionResponse transcribir(
            @RequestPart("audio") MultipartFile audio
    ) {
        return service.transcribe(audio);
    }
}
