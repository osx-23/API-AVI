package com.osx23.avi.api.service;

import com.osx23.avi.api.config.WhisperProperties;
import com.osx23.avi.api.model.InterpretResult;
import com.osx23.avi.api.model.TranscriptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LocalWhisperTranscriptionService {

    private static final long MAX_AUDIO_BYTES = 25L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "webm", "wav", "mp3", "mp4", "mpeg", "mpga", "m4a"
    );

    private static final String DOMAIN_PROMPT =
            "Comando operativo vial en español de Perú. " +
            "Palabras esperadas: fuga, vía, placa. " +
            "Las vías pueden dictarse como 151, ciento cincuenta y uno o uno cinco uno. " +
            "Las placas pueden dictarse con alfabeto fonético: " +
            "Alfa Bravo Charlie Delta Echo Foxtrot Golf Hotel India Juliet Kilo Lima Mike " +
            "November Oscar Papa Quebec Romeo Sierra Tango Uniform Victor Whiskey Xray Yankee Zulu. " +
            "Conserva con precisión letras y números.";

    private final WhisperProperties properties;
    private final VoiceCommandParser parser;

    public LocalWhisperTranscriptionService(
            WhisperProperties properties,
            VoiceCommandParser parser
    ) {
        this.properties = properties;
        this.parser = parser;
    }

    public boolean isAvailable() {
        Process process = null;
        try {
            process = new ProcessBuilder(properties.effectiveCommand(), "--help")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        }
    }

    public String modelLabel() {
        return "whisper-local:" + properties.effectiveModel();
    }

    public TranscriptionResponse transcribe(MultipartFile audio) {
        if (!isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Whisper local no está disponible. Instálalo y asegúrate de que el comando '" +
                            properties.effectiveCommand() + "' esté en PATH."
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

        String extension = inferExtension(audio);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato de audio no soportado: " + extension
            );
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("avi-whisper-");
            Path audioPath = workDir.resolve("comando." + extension);
            Path logPath = workDir.resolve("whisper.log");
            Files.write(audioPath, audio.getBytes());

            List<String> command = new ArrayList<>();
            command.add(properties.effectiveCommand());
            command.add(audioPath.toAbsolutePath().toString());
            command.add("--model");
            command.add(properties.effectiveModel());
            command.add("--language");
            command.add(properties.effectiveLanguage());
            command.add("--task");
            command.add("transcribe");
            command.add("--output_dir");
            command.add(workDir.toAbsolutePath().toString());
            command.add("--output_format");
            command.add("txt");
            command.add("--fp16");
            command.add("False");
            command.add("--initial_prompt");
            command.add(DOMAIN_PROMPT);

            if (!properties.effectiveDevice().isBlank()) {
                command.add("--device");
                command.add(properties.effectiveDevice());
            }

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logPath.toFile())
                    .start();

            boolean finished = process.waitFor(
                    properties.effectiveTimeoutSeconds(),
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Whisper tardó demasiado. La primera ejecución puede descargar el modelo; " +
                                "vuelve a intentar cuando termine la descarga."
                );
            }

            String log = Files.exists(logPath)
                    ? Files.readString(logPath, StandardCharsets.UTF_8)
                    : "";

            if (process.exitValue() != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Whisper terminó con error: " + tail(log, 1800)
                );
            }

            Path transcriptionPath = workDir.resolve("comando.txt");
            if (!Files.exists(transcriptionPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Whisper terminó sin generar el archivo de transcripción. " + tail(log, 800)
                );
            }

            String text = Files.readString(transcriptionPath, StandardCharsets.UTF_8).trim();
            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Whisper no detectó voz reconocible en el audio."
                );
            }

            InterpretResult interpretation = parser.parse(text);
            return new TranscriptionResponse(text, modelLabel(), interpretation);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar el archivo de audio localmente.",
                    ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La transcripción local fue interrumpida.",
                    ex
            );
        } finally {
            deleteRecursively(workDir);
        }
    }

    private String inferExtension(MultipartFile audio) {
        String original = audio.getOriginalFilename();
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < original.length()) {
                String extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXTENSIONS.contains(extension)) {
                    return extension;
                }
            }
        }

        String contentType = audio.getContentType();
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("wav")) return "wav";
            if (lower.contains("mpeg")) return "mp3";
            if (lower.contains("mp4")) return "mp4";
        }
        return "webm";
    }

    private String tail(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return "sin detalle adicional";
        }
        String compact = value.trim();
        return compact.length() <= maxChars
                ? compact
                : compact.substring(compact.length() - maxChars);
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
