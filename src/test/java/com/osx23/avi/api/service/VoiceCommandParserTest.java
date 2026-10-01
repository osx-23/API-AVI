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
    void interpretaViaConNumeroComoRelleno() {
        assertThat(parser.parse("Fuga vía número 151").via()).isEqualTo(151);
    }

    @Test
    void interpretaViaEnPalabras() {
        assertThat(parser.parse("Fuga vía ciento cincuenta y uno").via()).isEqualTo(151);
    }

    @Test
    void interpretaViaConDigitosHabladosPorSeparado() {
        assertThat(parser.parse("Fuga vía uno cinco uno").via()).isEqualTo(151);
    }

    @Test
    void interpretaViaDeCuatroCifras() {
        assertThat(parser.parse("Fuga vía mil doscientos treinta y cuatro").via()).isEqualTo(1234);
        assertThat(parser.parse("Fuga vía dos mil ciento cinco").via()).isEqualTo(2105);
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
    void interpretaPlacaCompacta() {
        assertThat(parser.parse("Fuga vía 151 placa BTL245").placa()).isEqualTo("BTL245");
        assertThat(parser.parse("Fuga vía 151 placa BTL-245").placa()).isEqualTo("BTL245");
    }

    @Test
    void interpretaPlacaConNombresDeLetras() {
        assertThat(parser.parse("Fuga vía 151 placa be te ele dos cuatro cinco").placa())
                .isEqualTo("BTL245");
    }

    @Test
    void interpretaVariantesFoneticasComunes() {
        assertThat(parser.parse("Fuga vía 151 placa charly eco julieta uno dos tres").placa())
                .isEqualTo("CEJ123");
    }

    @Test
    void interpretaXRayYFoxTrotSeparados() {
        assertThat(parser.parse("Fuga vía 151 placa x ray fox trot uno dos").placa())
                .isEqualTo("XF12");
    }

    @Test
    void normalizaTildes() {
        InterpretResult result = parser.parse(
                "Fúga vía ciento cincuenta y uno placa Víctor Óscar dos"
        );
        assertThat(result.tipo()).isEqualTo("FUGA");
        assertThat(result.via()).isEqualTo(151);
        assertThat(result.placa()).isEqualTo("VO2");
    }

    @Test
    void placaEsOpcional() {
        InterpretResult result = parser.parse("Fuga vía 151");
        assertThat(result.placa()).isNull();
        assertThat(result.valido()).isTrue();
    }

    @Test
    void reportaPlacaIndicadaPeroIncomprensible() {
        InterpretResult result = parser.parse("Fuga vía 151 placa desconocida");
        assertThat(result.valido()).isFalse();
        assertThat(result.errores()).anyMatch(error -> error.contains("placa"));
    }

    @Test
    void reportaViaFaltante() {
        InterpretResult result = parser.parse("Fuga placa Bravo Lima uno");
        assertThat(result.valido()).isFalse();
        assertThat(result.errores()).anyMatch(error -> error.toLowerCase().contains("vía"));
    }

    @Test
    void reportaTipoFaltante() {
        InterpretResult result = parser.parse("vía 151 placa Bravo Lima uno");
        assertThat(result.valido()).isFalse();
        assertThat(result.errores()).anyMatch(error -> error.contains("FUGA"));
    }

    @Test
    void reportaComandoVacio() {
        InterpretResult result = parser.parse("   ");
        assertThat(result.valido()).isFalse();
        assertThat(result.errores()).anyMatch(error -> error.contains("vacío"));
    }

    @Test
    void rechazaViaFueraDeRango() {
        assertThat(parser.parse("Fuga vía 10000").valido()).isFalse();
    }
}
