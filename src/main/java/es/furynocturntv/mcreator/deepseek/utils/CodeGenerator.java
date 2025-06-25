package es.furynocturntv.mcreator.deepseek.utils;

import es.furynocturntv.mcreator.deepseek.services.MCreatorWorkspace;
import net.mcreator.element.*;
import net.mcreator.element.types.*;
import net.mcreator.workspace.*;

public class CodeGenerator {
    private final MCreatorWorkspace workspace;

    public CodeGenerator(MCreatorWorkspace workspace) {
        this.workspace = workspace;
    }

    public void implementCode(String code, String elementName, CodeType codeType) throws Exception {
        // Extraer el código relevante entre los marcadores ```
        String cleanCode = extractCodeBlock(code);

        switch (codeType) {
            case PROCEDURE:
                implementProcedure(cleanCode, elementName);
                break;
            case CLASS:
                implementClass(cleanCode, elementName);
                break;
            case METHOD:
                implementMethod(cleanCode, elementName);
                break;
            default:
                throw new IllegalArgumentException("Tipo de código no soportado: " + codeType);
        }

        workspace.markDirty();
    }

    private String extractCodeBlock(String code) {
        if (code.contains("```")) {
            String[] parts = code.split("```");
            if (parts.length >= 2) {
                return parts[1].replaceFirst("^[a-zA-Z]*\n", ""); // Eliminar lenguaje si existe
            }
        }
        return code;
    }

    private void implementProcedure(String code, String procedureName) throws Exception {
        Procedure procedure = workspace.getModElementManager().getModElementByName(procedureName, Procedure.class)
                .orElseThrow(() -> new IllegalArgumentException("Procedimiento no encontrado: " + procedureName));

        // Actualizar el código del procedimiento
        procedure.code = code;
        workspace.getModElementManager().storeModElement(procedure);
    }

    private void implementClass(String code, String className) throws Exception {
        // Verificar si es una clase personalizada
        if (workspace.getModElementManager().getModElementByName(className) != null) {
            throw new IllegalArgumentException("Ya existe un elemento con ese nombre: " + className);
        }

        // Crear nueva clase Java
        CustomElement element = new CustomElement(workspace);
        element.setName(className);
        element.code = code;

        workspace.getModElementManager().storeModElement(element);
        workspace.getGenerator().generateElement(element);
    }

    private void implementMethod(String code, String methodSignature) throws Exception {
        String className = methodSignature.substring(0, methodSignature.lastIndexOf('.'));
        String methodName = methodSignature.substring(methodSignature.lastIndexOf('.') + 1);

        // Buscar la clase contenedora
        CustomElement element = workspace.getModElementManager().getModElementByName(className, CustomElement.class)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada: " + className));

        // Insertar el método en la clase
        element.code = insertMethod(element.code, code, methodName);
        workspace.getModElementManager().storeModElement(element);
        workspace.getGenerator().generateElement(element);
    }

    private String insertMethod(String classCode, String methodCode, String methodName) {
        // Implementación simplificada - en producción usaría un parser de código real
        int lastBrace = classCode.lastIndexOf('}');
        if (lastBrace == -1) {
            throw new IllegalArgumentException("Código de clase inválido");
        }

        return classCode.substring(0, lastBrace) +
                "\n\n" + methodCode + "\n" +
                classCode.substring(lastBrace);
    }

    public enum CodeType {
        PROCEDURE,
        CLASS,
        METHOD
    }
}