package com.osx23.avi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AviApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusExponeInformacionDeLaV1() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api").value("ok"))
                .andExpect(jsonPath("$.version").value("0.2.0-v1"))
                .andExpect(jsonPath("$.parser").value("avi-voice-v1.1"))
                .andExpect(jsonPath("$.supabaseConfigurado").isBoolean());
    }

    @Test
    void interpretaComandoCompleto() throws Exception {
        mockMvc.perform(post("/api/v1/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "texto": "Fuga vía ciento cincuenta y uno placa Bravo Tango Lima dos cuatro cinco"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FUGA"))
                .andExpect(jsonPath("$.via").value(151))
                .andExpect(jsonPath("$.placa").value("BTL245"))
                .andExpect(jsonPath("$.valido").value(true));
    }

    @Test
    void rechazaTextoVacio() throws Exception {
        mockMvc.perform(post("/api/v1/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "texto": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rechazaJsonInvalidoConMensajeUtil() throws Exception {
        mockMvc.perform(post("/api/v1/interpretar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{texto:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("JSON inválido"));
    }
}
