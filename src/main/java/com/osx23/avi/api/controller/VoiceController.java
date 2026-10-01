package com.osx23.avi.api.controller;

import com.osx23.avi.api.model.InterpretRequest;
import com.osx23.avi.api.model.InterpretResult;
import com.osx23.avi.api.service.VoiceCommandParser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VoiceController {

    private final VoiceCommandParser parser;

    public VoiceController(VoiceCommandParser parser) {
        this.parser = parser;
    }

    @PostMapping("/interpretar")
    public InterpretResult interpretar(@Valid @RequestBody InterpretRequest request) {
        return parser.parse(request.texto());
    }
}
