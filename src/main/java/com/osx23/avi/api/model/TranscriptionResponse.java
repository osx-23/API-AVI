package com.osx23.avi.api.model;

public record TranscriptionResponse(
        String texto,
        String modelo,
        InterpretResult interpretacion
) {}
