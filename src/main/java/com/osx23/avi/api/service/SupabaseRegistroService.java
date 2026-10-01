package com.osx23.avi.api.service;

import com.osx23.avi.api.config.SupabaseProperties;
import com.osx23.avi.api.model.RegistroRequest;
import com.osx23.avi.api.model.RegistroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseRegistroService {

    private final SupabaseProperties properties;

    public SupabaseRegistroService(SupabaseProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.configured();
    }

    public RegistroResponse create(RegistroRequest request) {
        ensureConfigured();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipo", request.tipo().trim().toUpperCase());
        body.put("via", request.via());

        if (request.placa() != null && !request.placa().isBlank()) {
            body.put("placa", request.placa().trim().toUpperCase());
        }

        if (request.textoReconocido() != null && !request.textoReconocido().isBlank()) {
            body.put("texto_reconocido", request.textoReconocido().trim());
        }

        try {
            RegistroResponse[] response = client()
                    .post()
                    .uri("/registros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Prefer", "return=representation")
                    .body(body)
                    .retrieve()
                    .body(RegistroResponse[].class);

            if (response == null || response.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Supabase no devolvió el registro creado."
                );
            }

            return response[0];
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Supabase rechazó el registro: " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    public List<RegistroResponse> findRecent() {
        ensureConfigured();

        try {
            RegistroResponse[] response = client()
                    .get()
                    .uri("/registros?select=*&order=creado_en.desc&limit=50")
                    .retrieve()
                    .body(RegistroResponse[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar Supabase: " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private RestClient client() {
        String url = properties.url().replaceAll("/+$", "") + "/rest/v1";

        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("apikey", properties.key())
                .build();
    }

    private void ensureConfigured() {
        if (!properties.configured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar SUPABASE_PUBLISHABLE_KEY."
            );
        }
    }
}
