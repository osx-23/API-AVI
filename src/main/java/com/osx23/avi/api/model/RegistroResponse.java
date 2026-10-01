package com.osx23.avi.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroResponse(
        UUID id,
        String tipo,
        Integer via,
        String placa,
        @JsonProperty("texto_reconocido")
        String textoReconocido,
        @JsonProperty("creado_en")
        OffsetDateTime creadoEn
) {}
