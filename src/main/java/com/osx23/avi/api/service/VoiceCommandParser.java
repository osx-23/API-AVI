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

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static final Map<String, String> PHONETIC = Map.ofEntries(
            Map.entry("alfa", "A"), Map.entry("alpha", "A"), Map.entry("bravo", "B"),
            Map.entry("charlie", "C"), Map.entry("delta", "D"), Map.entry("echo", "E"),
            Map.entry("foxtrot", "F"), Map.entry("golf", "G"), Map.entry("hotel", "H"),
            Map.entry("india", "I"), Map.entry("juliet", "J"), Map.entry("juliett", "J"),
            Map.entry("kilo", "K"), Map.entry("lima", "L"), Map.entry("mike", "M"),
            Map.entry("november", "N"), Map.entry("oscar", "O"), Map.entry("papa", "P"),
            Map.entry("quebec", "Q"), Map.entry("romeo", "R"), Map.entry("sierra", "S"),
            Map.entry("tango", "T"), Map.entry("uniform", "U"), Map.entry("victor", "V"),
            Map.entry("whiskey", "W"), Map.entry("whisky", "W"), Map.entry("xray", "X"),
            Map.entry("yankee", "Y"), Map.entry("zulu", "Z")
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
            Map.entry("veinte", 20), Map.entry("veintiuno", 21), Map.entry("veintidos", 22),
            Map.entry("veintitres", 23), Map.entry("veinticuatro", 24), Map.entry("veinticinco", 25),
            Map.entry("veintiseis", 26), Map.entry("veintisiete", 27), Map.entry("veintiocho", 28),
            Map.entry("veintinueve", 29)
    );

    private static final Map<String, Integer> UNITS = Map.ofEntries(
            Map.entry("cero", 0), Map.entry("uno", 1), Map.entry("una", 1), Map.entry("dos", 2),
            Map.entry("tres", 3), Map.entry("cuatro", 4), Map.entry("cinco", 5),
            Map.entry("seis", 6), Map.entry("siete", 7), Map.entry("ocho", 8),
            Map.entry("nueve", 9), Map.entry("diez", 10)
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
                .replace("x-ray", "xray");

        String tipo = text.contains("fuga") ? "FUGA" : null;

        String viaSegment = "";
        int viaIndex = text.indexOf("via");
        if (viaIndex >= 0) {
            viaSegment = text.substring(viaIndex + 3);
            int placaInVia = viaSegment.indexOf("placa");
            if (placaInVia >= 0) {
                viaSegment = viaSegment.substring(0, placaInVia);
            }
            viaSegment = viaSegment.trim();
        }

        String plateSegment = "";
        int placaIndex = text.indexOf("placa");
        if (placaIndex >= 0) {
            plateSegment = text.substring(placaIndex + 5).trim();
        }

        Integer via = parseSpanishNumber(viaSegment);
        String placa = parsePlate(plateSegment);

        List<String> errores = new ArrayList<>();
        if (tipo == null) errores.add("No se detectó el tipo FUGA.");
        if (via == null || via <= 0) errores.add("No se detectó una vía válida.");
        if (placaIndex >= 0 && (placa == null || placa.isBlank())) {
            errores.add("Se indicó una placa, pero no pudo interpretarse.");
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
        if (segment == null || segment.isBlank()) return null;

        StringBuilder out = new StringBuilder();
        for (String token : segment.split("\\s+")) {
            if (PHONETIC.containsKey(token)) {
                out.append(PHONETIC.get(token));
            } else if (DIGIT_WORDS.containsKey(token)) {
                out.append(DIGIT_WORDS.get(token));
            } else if (token.chars().allMatch(Character::isDigit)) {
                out.append(token);
            } else if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
                out.append(token.toUpperCase(Locale.ROOT));
            }
        }

        String value = out.toString().replaceAll("[^A-Z0-9]", "");
        return value.isBlank() ? null : value;
    }

    Integer parseSpanishNumber(String segment) {
        if (segment == null || segment.isBlank()) return null;

        Matcher matcher = DIGITS.matcher(segment);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group());
        }

        String joined = segment.trim();
        if (SPECIAL_NUMBERS.containsKey(joined)) {
            return SPECIAL_NUMBERS.get(joined);
        }

        String[] tokens = joined.split("\\s+");
        boolean allSingleDigits = tokens.length > 1;
        StringBuilder digitSequence = new StringBuilder();
        if (allSingleDigits) {
            for (String token : tokens) {
                String digit = DIGIT_WORDS.get(token);
                if (digit == null) {
                    allSingleDigits = false;
                    break;
                }
                digitSequence.append(digit);
            }
            if (allSingleDigits) {
                return Integer.valueOf(digitSequence.toString());
            }
        }

        int total = 0;
        boolean recognized = false;

        for (String token : tokens) {
            if ("y".equals(token)) continue;

            Integer value = SPECIAL_NUMBERS.get(token);
            if (value == null) value = HUNDREDS.get(token);
            if (value == null) value = TENS.get(token);
            if (value == null) value = UNITS.get(token);

            if (value != null) {
                total += value;
                recognized = true;
            }
        }

        return recognized ? total : null;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(
                value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
