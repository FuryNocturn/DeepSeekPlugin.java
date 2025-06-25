package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.api.ApiKeyManager;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.validation.Validator;
import net.mcreator.ui.validation.component.VTextField;
import net.mcreator.util.DesktopUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ApiKeyDialog extends JDialog {

    private final ApiKeyManager apiKeyManager;
    private final MCreator mcreator;
    private final VTextField apiKeyField;

    public ApiKeyDialog(MCreator mcreator, ApiKeyManager apiKeyManager) {
        super(mcreator, "Configuración de API Key de DeepSeek", true);
        this.mcreator = mcreator;
        this.apiKeyManager = apiKeyManager;

        setLayout(new BorderLayout(10, 10));
        setSize(500, 250);
        setLocationRelativeTo(mcreator);

        // Panel principal
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("<html><div style='text-align:center;'>"
                + "<h3>Configuración de DeepSeek API</h3>"
                + "<p>Para usar el asistente de IA, necesitas una API Key válida de DeepSeek</p>"
                + "</div></html>"), BorderLayout.CENTER);

        // Panel de entrada
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        apiKeyField = new VTextField();
        apiKeyField.setPreferredSize(new Dimension(300, 30));
        apiKeyField.setValidator(Validator.createEmptyValidator(() ->
                apiKeyField.getText().length() >= 32, "La API Key debe tener al menos 32 caracteres"));

        // Mostrar la key actual enmascarada (si existe)
        String currentKey = apiKeyManager.getApiKey();
        if (!currentKey.isEmpty()) {
            apiKeyField.setText(maskApiKey(currentKey));
            apiKeyField.setEnabled(false);
        } else {
            apiKeyField.setPlaceholder("Ingresa tu API Key de DeepSeek aquí...");
        }

        inputPanel.add(new JLabel("API Key:"), BorderLayout.WEST);
        inputPanel.add(apiKeyField, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Botón para obtener key
        JButton getKeyButton = new JButton("Obtener Key");
        getKeyButton.setIcon(UIRES.get("16px.url"));
        getKeyButton.addActionListener(e -> DesktopUtils.browseSafe("https://platform.deepseek.com/api_keys"));
        getKeyButton.setToolTipText("Abre el navegador para obtener una API Key");

        // Botón para modificar key
        JButton modifyButton = new JButton("Modificar");
        modifyButton.setIcon(UIRES.get("16px.edit"));
        modifyButton.addActionListener(this::modifyApiKey);
        modifyButton.setToolTipText("Permite modificar la API Key actual");

        // Botón para borrar key
        JButton deleteButton = new JButton("Borrar");
        deleteButton.setIcon(UIRES.get("16px.delete"));
        deleteButton.addActionListener(this::deleteApiKey);
        deleteButton.setToolTipText("Elimina la API Key almacenada");

        // Botón para guardar
        JButton saveButton = new JButton("Guardar");
        saveButton.setIcon(UIRES.get("16px.ok"));
        saveButton.addActionListener(this::saveApiKey);
        saveButton.setToolTipText("Guarda la API Key introducida");

        buttonPanel.add(getKeyButton);
        buttonPanel.add(modifyButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        // Panel de estado
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(100, 100, 100));

        // Ensamblar componentes
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(PanelUtils.totalCenterInPanel(panel), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private String maskApiKey(String key) {
        if (key.length() <= 8) return "••••••••";
        return key.substring(0, 4) + "••••••••" + key.substring(key.length() - 4);
    }

    private void modifyApiKey(ActionEvent e) {
        apiKeyField.setText("");
        apiKeyField.setEnabled(true);
        apiKeyField.setPlaceholder("Ingresa tu nueva API Key aquí...");
        apiKeyField.requestFocusInWindow();
    }

    private void deleteApiKey(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres eliminar la API Key almacenada?\n" +
                        "El asistente no funcionará hasta que ingreses una nueva key.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            apiKeyManager.saveApiKey("");
            apiKeyField.setText("");
            apiKeyField.setEnabled(true);
            apiKeyField.setPlaceholder("Ingresa tu API Key aquí...");
            JOptionPane.showMessageDialog(this,
                    "API Key eliminada correctamente.\n" +
                            "Se te pedirá una nueva key al reiniciar MCreator.",
                    "Key eliminada",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveApiKey(ActionEvent e) {
        if (!apiKeyField.getText().isEmpty() && apiKeyField.getValidationStatus().isValid()) {
            apiKeyManager.saveApiKey(apiKeyField.getText());
            apiKeyField.setText(maskApiKey(apiKeyField.getText()));
            apiKeyField.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "API Key guardada correctamente.",
                    "Key guardada",
                    JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa una API Key válida (mínimo 32 caracteres).",
                    "Key inválida",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}