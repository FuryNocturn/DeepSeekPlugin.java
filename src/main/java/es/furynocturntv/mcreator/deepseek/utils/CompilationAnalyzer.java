package es.furynocturntv.mcreator.deepseek.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilationAnalyzer {
    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "ERROR in (.*)\\((\\d+),(\\d+)\\): (.*)"
    );

    public AnalysisResult analyzeErrors(String errorLog) {
        AnalysisResult result = new AnalysisResult();
        Matcher matcher = ERROR_PATTERN.matcher(errorLog);

        while (matcher.find()) {
            ErrorDetail error = new ErrorDetail();
            error.filePath = matcher.group(1);
            error.line = Integer.parseInt(matcher.group(2));
            error.column = Integer.parseInt(matcher.group(3));
            error.message = matcher.group(4);

            error.suggestion = generateSuggestion(error);
            result.errors.add(error);
        }

        return result;
    }

    private String generateSuggestion(ErrorDetail error) {
        // Este es un ejemplo básico - en producción usaría un sistema más sofisticado
        if (error.message.contains("cannot find symbol")) {
            return "Parece que falta una declaración o importación. Verifica que todos los nombres estén escritos correctamente y que las clases necesarias estén importadas.";
        } else if (error.message.contains("expected")) {
            return "Hay un problema de sintaxis en esta línea. Revisa que todos los paréntesis, llaves y puntos y coma estén correctamente cerrados.";
        } else if (error.message.contains("incompatible types")) {
            return "Estás intentando asignar un valor de un tipo a una variable de otro tipo incompatible. Revisa los tipos de datos involucrados.";
        }

        return "Revisa la documentación relacionada con: " + error.message;
    }

    public static class AnalysisResult {
        public List<ErrorDetail> errors = new ArrayList<>();

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public static class ErrorDetail {
        public String filePath;
        public int line;
        public int column;
        public String message;
        public String suggestion;

        @Override
        public String toString() {
            return String.format("%s (%d:%d): %s\nSugerencia: %s",
                    filePath, line, column, message, suggestion);
        }
    }
}