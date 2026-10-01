package com.osx23.avi.api.service;

import com.osx23.avi.api.model.InterpretResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceCommandParserTest {

    private final VoiceCommandParser parser = new VoiceCommandParser();

    @Test
    void interpretaViaNumerica() {
        InterpretResult result = parser.parse("Fuga vía 151");

        assertThat(result.tipo()).isEqualTo("FUGA");
        assertThat(result.via()).isEqualTo(151);
        assertThat(result.valido()).isTrue();
    }

    @Test
    void interpretaViaEnPalabras() {
        InterpretResult result = parser.parse("Fuga vía ciento cincuenta y uno");

        assertThat(result.via()).isEqualTo(151);
    }

    @Test
    void interpretaPlacaFonetica() {
        InterpretResult result = parser.parse(
                "Fuga vía 151 placa Bravo Tango Lima dos cuatro cinco"
        );

        assertThat(result.placa()).isEqualTo("BTL245");
        assertThat(result.valido()).isTrue();
    }

    @Test
    void interpretaViaConDigitosHabladosPorSeparado() {
        InterpretResult result = parser.parse(
                "Fuga vía uno cinco uno placa Alfa Bravo Charlie uno dos tres"
        );

        assertThat(result.via()).isEqualTo(151);
        assertThat(result.placa()).isEqualTo("ABC123");
    }

    @Test
    void normalizaTildes() {
        InterpretResult result = parser.parse(
                "Fúga vía ciento cincuenta y uno placa Víctor Oscar dos"
        );

        assertThat(result.tipo()).isEqualTo("FUGA");
        assertThat(result.via()).isEqualTo(151);
        assertThat(result.placa()).isEqualTo("VO2");
    }

    @Test
    void reportaViaFaltante() {
        InterpretResult result = parser.parse("Fuga placa Bravo Lima uno");

        assertThat(result.valido()).isFalse();
        assertThat(result.errores()).anyMatch(error -> error.contains("vía"));
    }
}
