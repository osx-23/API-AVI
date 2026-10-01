package com.osx23.avi.api.service;

import com.osx23.avi.api.model.InterpretResult;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceCommandParser {

    private static final Pattern DIGITS = Pattern.compile("\\b\\d{1,4}\\b");
    private static final Pattern COMPACT_PLATE = Pattern.compile("\\b([a-z]{1,4})[- ]?(\\d{2,4})\\b");

    private static final Map<String, String> PHONETIC = Map.ofEntries(
            Map.entry("alfa", "A"), Map.entry("alpha", "A"),
            Map.entry("bravo", "B"),
            Map.entry("charlie", "C"), Map.entry("charly", "C"), Map.entry("charly", "C"),
            Map.entry("delta", "D"),
            Map.entry("echo", "E"), Map.entry("eco", "E"),
            Map.entry("foxtrot", "F"), Map.entry("foxtrot", "F"),
            Map.entry("golf", "G"),
            Map.entry("hotel", "H"),
            Map.entry("india", "I"),
            Map.entry("juliet", "J"), Map.entry("juliett", "J"), Map.entry("julieta", "J"),
            Map.entry("kilo", "K"),
            Map.entry("lima", "L"),
            Map.entry("mike", "M"), Map.entry("maik", "M"),
            Map.entry("november", "N"), Map.entry("noviembre", "N"),
            Map.entry("oscar", "O"),
            Map.entry("papa", "P"),
            Map.entry("quebec", "Q"),
            Map.entry("romeo", "R"),
            Map.entry("sierra", "S"),
            Map.entry("tango", "T"),
            Map.entry("uniform", "U"), Map.entry("uniforme", "U"),
            Map.entry("victor", "V"),
            Map.entry("whiskey", "W"), Map.entry("whisky", "W"), Map.entry("dobleu", "W"),
            Map.entry("xray", "X"),
            Map.entry("yankee", "Y"), Map.entry("yanki", "Y"),
            Map.entry("zulu", "Z")
    );

    private static final Map<String, String> LETTER_NAMES = Map.ofEntries(
            Map.entry("a", "A"),
            Map.entry("be", "B"), Map.entry("b", "B"),
            Map.entry("ce", "C"), Map.entry("c", "C"),
            Map.entry("de", "D"), Map.entry("d", "D"),
            Map.entry("e", "E"),
            Map.entry("efe", "F"), Map.entry("f", "F"),
            Map.entry("ge", "G"), Map.entry("g", "G"),
            Map.entry("hache", "H"), Map.entry("h", "H"),
            Map.entry("i", "I"),
            Map.entry("jota", "J"), Map.entry("j", "J"),
            Map.entry("ka", "K"), Map.entry("k", "K"),
            Map.entry("ele", "L"), Map.entry("l", "L"),
            Map.entry("eme", "M"), Map.entry("m", "M"),
            Map.entry("ene", "N"), Map.entry("n", "N"),
            Map.entry("o", "O"),
            Map.entry("pe", "P"), Map.entry("p", "P"),
            Map.entry("cu", "Q"), Map.entry("q", "Q"),
            Map.entry("erre", "R"), Map.entry("r", "R"),
            Map.entry("ese", "S"), Map.entry("s", "S"),
            Map.entry("te", "T"), Map.entry("t", "T"),
            Map.entry("u", "U"),
            Map.entry("uve", "V"), Map.entry("v", "V"),
            Map.entry("equis", "X"), Map.entry("x", "X"),
            Map.entry("ye", "Y"), Map.entry("y", "Y"),
            Map.entry("zeta", "Z"), Map.entry("z", "Z")
    );

    private static final Map<String, String> DIGIT_WORDS = Map.ofEntries(
            Map.entry("cero", "0"), Map.entry("uno", "1"), Map.entry("una", "1"),
            Map.entry("dos", "2"), Map.entry("tres", "3"), Map.entry("cuatro", "4"),
            Map.entry("cinco", "5"), Map.entry("seis", "6"), Map.entry("siete", "7"),
            Map.entry("ocho", "8"), Map.entry("nueve", "9")
    );

    private static final Map<String, Integer> SPECIAL_NUMBERS = Map.ofEntries(
            Map.entry("once", 11), Map.entry("doce", 12), Map.entry("trece", 13),
            Map.entry("catorce", 14), Map.entry("quince", 15), Map.entry("dieciseis", 16),
            Map.entry("diecisiete", 17), Map.entry("dieciocho", 18), Map.entry("diecinueve", 19),
            Map.entry("veinte", 20), Map.entry("veintiuno", 21), Map.entry("veintiun", 21),
            Map.entry("veintidos", 22), Map.entry("veintitres", 23), Map.entry("veinticuatro", 24),
            Map.entry("veinticinco", 25), Map.entry("veintiseis", 26), Map.entry("veintisiete", 27),
            Map.entry("veintiocho", 28), Map.entry("veintinueve", 29)
    );

    private static final Map<String, Integer> UNITS = Map.ofEntries(
            Map.entry("cero", 0), Map.entry("uno", 1), Map.entry("un", 1), Map.entry("una", 1),
            Map.entry("dos", 2), Map.entry("tres", 3), Map.entry("cuatro", 4),
            Map.entry("cinco", 5), Map.entry("seis", 6), Map.entry("siete", 7),
            Map.entry("ocho", 8), Map.entry("nueve", 9), Map.entry("diez", 10)
    );

    private static final Map<String, Integer> TENS = Map.of(
            "veinte", 20, "treinta", 30, "cuarenta", 40, "cincuenta", 50,
            "sesenta", 60, "setenta", 70, "ochenta", 80, "noventa", 90
    );

    private static final Map<String, Integer> HUNDREDS = Map.ofEntries(
            Map.entry("cien", 100), Map.entry("ciento", 100), Map.entry("doscientos", 200),
            Map.entry("trescientos", 300), Map.entry("cuatrocientos", 400),
            Map.entry("quinientos", 500), Map.entry("seiscientos", 600),
            Map.entry("setecientos", 700), Map.entry("ochocientos", 800),
            Map.entry("novecientos", 900)
    );

    public InterpretResult parse(String raw) {
        String original = raw == null ? "" : raw.trim();
        String text = normalize(original)
                .replace("x ray", "xray")
                .replace("x-ray", "xray")
                .replace("fox trot", "foxtrot")
                .replace("doble u", "dobleu");

        String tipo = Pattern.compile("\\bfuga\\b").matcher(text).find() ? "FUGA" : null;

        int viaIndex = findMarker(text, "via");
        int placaIndex = findMarker(text, "placa");

        String viaSegment = null;
        if (viaIndex >= 0) {
            int start = viaIndex + 3;
            int end = placaIndex > start ? placaIndex : text.length();
            viaSegment = cleanupViaSegment(text.substring(start, end));
        }

        String plateSegment = null;
        if (placaIndex >= 0) {
            plateSegment = text.substring(placaIndex + 5).trim();
        }

        Integer via = parseSpanishNumber(viaSegment);
        String placa = parsePlate(plateSegment);

        List<String> errores = new ArrayList<>();
        if (original.isBlank()) {
            errores.add("El comando está vacío.");
        }
        if (tipo == null) {
            errores.add("No se detectó el tipo FUGA.");
        }
        if (viaIndex < 0) {
            errores.add("No se detectó la palabra vía.");
        } else if (via == null || via <= 0 || via > 9999) {
            errores.add("No se detectó una vía válida entre 1 y 9999.");
        }
        if (placaIndex >= 0 && (placa == null || placa.isBlank())) {
            errores.add("Se indicó una placa, pero no pudo interpretarse.");
        }
        if (placa != null && placa.length() > 10) {
            errores.add("La placa interpretada supera 10 caracteres.");
        }

        return new InterpretResult(
                tipo,
                via,
                placa,
                original,
                errores.isEmpty(),
                List.copyOf(errores)
        );
    }

    String parsePlate(String segment) {
        if (segment == null || segment.isBlank()) {
            return null;
        }

        String normalized = normalize(segment)
                .replace("x ray", "xray")
                .replace("x-ray", "xray")
                .replace("fox trot", "foxtrot")
                .replace("doble u", "dobleu");

        Matcher compact = COMPACT_PLATE.matcher(normalized);
        if (compact.find()) {
            return (compact.group(1) + compact.group(2))
                    .toUpperCase(Locale.ROOT)
                    .replaceAll("[^A-Z0-9]", "");
        }

        StringBuilder out = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }

            String letter = PHONETIC.get(token);
            if (letter == null) {
                letter = LETTER_NAMES.get(token);
            }

            if (letter != null) {
                out.append(letter);
                continue;
            }

            String digit = DIGIT_WORDS.get(token);
            if (digit != null) {
                out.append(digit);
                continue;
            }

            if (token.chars().allMatch(Character::isDigit)) {
                out.append(token);
                continue;
            }

            String cleanToken = token.replaceAll("[^a-z0-9]", "");
            if (cleanToken.matches("[a-z]{1,4}\\d{2,4}")) {
                out.append(cleanToken.toUpperCase(Locale.ROOT));
            }
        }

        String value = out.toString().replaceAll("[^A-Z0-9]", "");
        return value.isBlank() ? null : value;
    }

    Integer parseSpanishNumber(String segment) {
        if (segment == null || segment.isBlank()) {
            return null;
        }

        String cleaned = cleanupViaSegment(normalize(segment));

        Matcher matcher = DIGITS.matcher(cleaned);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group());
        }

        String[] rawTokens = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isBlank() && !"y".equals(token)) {
                tokens.add(token);
            }
        }

        if (tokens.isEmpty()) {
            return null;
        }

        boolean allSingleDigits = tokens.size() > 1 && tokens.stream().allMatch(DIGIT_WORDS::containsKey);
        if (allSingleDigits) {
            StringBuilder digits = new StringBuilder();
            for (String token : tokens) {
                digits.append(DIGIT_WORDS.get(token));
            }
            try {
                return Integer.valueOf(digits.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        int total = 0;
        int current = 0;
        boolean recognized = false;

        for (String token : tokens) {
            if ("mil".equals(token)) {
                if (current == 0) {
                    current = 1;
                }
                total += current * 1000;
                current = 0;
                recognized = true;
                continue;
            }

            Integer value = SPECIAL_NUMBERS.get(token);
            if (value == null) value = HUNDREDS.get(token);
            if (value == null) value = TENS.get(token);
            if (value == null) value = UNITS.get(token);

            if (value != null) {
                current += value;
                recognized = true;
            }
        }

        if (!recognized) {
            return null;
        }

        int result = total + current;
        return result <= 9999 ? result : null;
    }

    private String cleanupViaSegment(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replaceAll("\\b(numero|nro|num|número)\\b", " ")
                .replaceAll("\\b(de|la|el)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int findMarker(String text, String marker) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(marker) + "\\b").matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(
                value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
