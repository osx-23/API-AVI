package com.osx23.avi.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.osx23.avi.api.config.OpenAiProperties;
import com.osx23.avi.api.model.InterpretResult;
import com.osx23.avi.api.model.TranscriptionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@Service
public class OpenAiTranscriptionService {

    private static final long MAX_AUDIO_BYTES = 25L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "webm", "wav", "mp3", "mp4", "mpeg", "mpga", "m4a"
    );

    private final OpenAiProperties properties;
    private final VoiceCommandParser parser;

    public OpenAiTranscriptionService(
            OpenAiProperties properties,
            VoiceCommandParser parser
    ) {
        this.properties = properties;
        this.parser = parser;
    }

    public boolean isConfigured() {
        return properties.configured();
    }

    public String model() {
        return properties.model();
    }

    public TranscriptionResponse transcribe(MultipartFile audio) {
        if (!properties.configured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar OPENAI_API_KEY en el backend."
            );
        }

        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El audio está vacío.");
        }

        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "El audio supera el límite de 25 MB."
            );
        }

        String filename = normalizeFilename(audio.getOriginalFilename(), audio.getContentType());
        String extension = extension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato de audio no soportado: " + extension
            );
        }

        try {
            byte[] bytes = audio.getBytes();
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", resource)
                    .filename(filename)
                    .contentType(mediaType(audio.getContentType(), extension));
            builder.part("model", properties.model());
            builder.part("language", "es");
            builder.part(
                    "prompt",
                    "Comando operativo vial en español de Perú. " +
                    "Palabras esperadas: fuga, vía, placa. " +
                    "Las vías pueden dictarse como 151, ciento cincuenta y uno o uno cinco uno. " +
                    "Las placas pueden dictarse usando alfabeto fonético: " +
                    "Alfa Bravo Charlie Delta Echo Foxtrot Golf Hotel India Juliet Kilo Lima Mike " +
                    "November Oscar Papa Quebec Romeo Sierra Tango Uniform Victor Whiskey Xray Yankee Zulu. " +
                    "Transcribe letras y números con máxima precisión."
            );

            MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();

            OpenAiResponse response = RestClient.create()
                    .post()
                    .uri(properties.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El servicio de transcripción no devolvió texto."
                );
            }

            String text = response.text().trim();
            InterpretResult interpretation = parser.parse(text);

            return new TranscriptionResponse(text, properties.model(), interpretation);
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (detail == null || detail.isBlank()) {
                detail = ex.getStatusText();
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falló la transcripción de audio: " + detail,
                    ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo leer el audio recibido.",
                    ex
            );
        }
    }

    private String normalizeFilename(String original, String contentType) {
        if (original != null && original.matches("(?i).+\\.(webm|wav|mp3|mp4|mpeg|mpga|m4a)$")) {
            return original;
        }

        if (contentType != null) {
            String lower = contentType.toLowerCase();
            if (lower.contains("wav")) return "comando.wav";
            if (lower.contains("mpeg")) return "comando.mp3";
            if (lower.contains("mp4")) return "comando.mp4";
        }

        return "comando.webm";
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "webm";
    }

    private MediaType mediaType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
            }
        }

        return switch (extension) {
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "mp3", "mpeg", "mpga" -> MediaType.parseMediaType("audio/mpeg");
            case "m4a", "mp4" -> MediaType.parseMediaType("audio/mp4");
            default -> MediaType.parseMediaType("audio/webm");
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponse(String text) {}
}
