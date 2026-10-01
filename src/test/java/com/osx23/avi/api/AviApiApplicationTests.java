package com.osx23.avi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "avi.whisper.command=__avi_whisper_missing__"
})
@AutoConfigureMockMvc
class AviApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusExponeWhisperLocal() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api").value("ok"))
                .andExpect(jsonPath("$.version").value("0.4.0-v1"))
                .andExpect(jsonPath("$.parser").value("avi-voice-v1.1"))
                .andExpect(jsonPath("$.supabaseConfigurado").isBoolean())
                .andExpect(jsonPath("$.transcripcionConfigurada").value(false))
                .andExpect(jsonPath("$.motorTranscripcion").value("whisper-local"))
                .andExpect(jsonPath("$.modeloTranscripcion").value("whisper-local:small"));
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
    void transcripcionIndicaQueWhisperNoEstaInstalado() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "comando.webm",
                "audio/webm",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/transcribir").file(audio))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Whisper local no está disponible. Instálalo y asegúrate de que el comando '__avi_whisper_missing__' esté en PATH."
                ));
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
