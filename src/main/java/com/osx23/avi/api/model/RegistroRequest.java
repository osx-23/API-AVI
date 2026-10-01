package com.osx23.avi.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "(?i)FUGA", message = "Por ahora el tipo permitido es FUGA")
        String tipo,

        @NotNull(message = "La vía es obligatoria")
        @Min(value = 1, message = "La vía debe ser mayor que cero")
        @Max(value = 9999, message = "La vía no puede superar 9999")
        Integer via,

        @Size(max = 10, message = "La placa no puede superar 10 caracteres")
        @Pattern(regexp = "^$|[A-Za-z0-9]+$", message = "La placa solo puede contener letras y números")
        String placa,

        String textoReconocido
) {}
