package com.osx23.avi.api.model;

import jakarta.validation.constraints.NotBlank;

public record InterpretRequest(
        @NotBlank(message = "El texto no puede estar vacío")
        String texto
) {}
