package com.osx23.avi.api.model;

import java.util.List;

public record InterpretResult(
        String tipo,
        Integer via,
        String placa,
        String textoOriginal,
        boolean valido,
        List<String> errores
) {}
