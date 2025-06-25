package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.api.ApiKeyManager;
import es.furynocturntv.mcreator.deepseek.utils.SettingsManager;
import net.mcreator.ui.MCreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SettingsDialog extends JDialog {

    private final SettingsManager settingsManager;
    private final ApiKeyManager apiKeyManager;
    private final MCreator mcreator;

    // Componentes de configuración
    private JCheckBox internetSearchCheckbox;
    private JCheckBox autoImplementCheckbox;
    private JCheckBox analyzeErrorsCheckbox;
    private JCheckBox offlineModeCheckbox;
    private JComboBox<String> modelSelector;
    private JSpinner temperatureSpinner;
    private JSpinner topPSpinner;
    private JSpinner rateLimitSpinner;
    private JButton apiKeyButton;
    private JButton clearCacheButton;
    private JButton manageModelsButton;

    public SettingsDialog(MCreator mcreator, SettingsManager settingsManager, ApiKeyManager apiKeyManager) {
        super((Frame)null, "Configuración de DeepSeek Assistant", true);
        this.mcreator = mcreator;
        this.settingsManager = settingsManager;
        this.apiKeyManager = apiKeyManager;

        initComponents();
        setupLayout();
        loadCurrentSettings();

        setSize(600, 500);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initComponents() {
        // Modo de búsqueda
        internetSearchCheckbox = new JCheckBox("Habilitar búsqueda por internet");
        internetSearchCheckbox.setToolTipText("Permite al asistente realizar búsquedas en internet cuando sea necesario");

        // Implementación automática
        autoImplementCheckbox = new JCheckBox("Implementar código sugerido automáticamente");
        autoImplementCheckbox.setToolTipText("Implementa directamente el código generado en tu proyecto");

        // Análisis de errores
        analyzeErrorsCheckbox = new JCheckBox("Analizar errores de compilación automáticamente");
        analyzeErrorsCheckbox.setToolTipText("Analiza los errores de compilación y sugiere soluciones");

        // Modo offline
        offlineModeCheckbox = new JCheckBox("Usar modo offline con modelos locales");
        offlineModeCheckbox.setToolTipText("Usa modelos de IA instalados localmente en lugar de la API online");
        offlineModeCheckbox.addActionListener(this::toggleOfflineMode);

        // Selector de modelo
        modelSelector = new JComboBox<>();
        modelSelector.setToolTipText("Selecciona el modelo de IA a utilizar");
        updateModelSelector();

        // Configuración de generación
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 1.0, 0.1));
        temperatureSpinner.setToolTipText("Controla la creatividad de las respuestas (valores más altos = más creativo)");

        topPSpinner = new JSpinner(new SpinnerNumberModel(0.9, 0.1, 1.0, 0.05));
        topPSpinner.setToolTipText("Controla la diversidad de las respuestas");

        // Límite de tasa
        rateLimitSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 60, 1));
        rateLimitSpinner.setToolTipText("Segundos de espera entre solicitudes para evitar límites de tasa");

        // Botones de acción
        apiKeyButton = new JButton("Configurar API Key");
        // Cambia el constructor de ApiKeyDialog para aceptar JFrame
        apiKeyButton.addActionListener(e -> new ApiKeyDialog(mcreator, apiKeyManager).setVisible(true));

        clearCacheButton = new JButton("Limpiar Caché");
        clearCacheButton.addActionListener(e -> {
            settingsManager.getResponseCache().clear();
            JOptionPane.showMessageDialog(this, "Caché limpiado exitosamente",
                    "Caché limpiado", JOptionPane.INFORMATION_MESSAGE);
        });

        manageModelsButton = new JButton("Gestionar Modelos Locales");
        manageModelsButton.addActionListener(e ->
                new ModelManagementDialog((JFrame)SwingUtilities.getWindowAncestor(this)).setVisible(true));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Panel principal con margen
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Panel de configuración general
        JPanel generalPanel = createSectionPanel("Configuración General");
        generalPanel.add(createLabeledComponent(internetSearchCheckbox, "Búsqueda por internet:"));
        generalPanel.add(createLabeledComponent(autoImplementCheckbox, "Implementación automática:"));
        generalPanel.add(createLabeledComponent(analyzeErrorsCheckbox, "Análisis de errores:"));
        generalPanel.add(createLabeledComponent(offlineModeCheckbox, "Modo offline:"));

        // Panel de configuración del modelo
        JPanel modelPanel = createSectionPanel("Configuración del Modelo");
        modelPanel.add(createLabeledComponent(modelSelector, "Modelo de IA:"));
        modelPanel.add(createLabeledComponent(temperatureSpinner, "Temperatura (creatividad):"));
        modelPanel.add(createLabeledComponent(topPSpinner, "Top P (diversidad):"));
        modelPanel.add(createLabeledComponent(rateLimitSpinner, "Límite de tasa (segundos):"));

        // Panel de acciones
        JPanel actionPanel = createSectionPanel("Acciones");
        actionPanel.add(createLabeledComponent(apiKeyButton, "API Key:"));
        actionPanel.add(createLabeledComponent(clearCacheButton, "Caché:"));
        actionPanel.add(createLabeledComponent(manageModelsButton, "Modelos locales:"));

        // Panel de botones inferior
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> saveSettings());
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        // Ensamblar componentes
        mainPanel.add(generalPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(modelPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(actionPanel);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel createLabeledComponent(JComponent component, String labelText) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setMaximumSize(new Dimension(Short.MAX_VALUE, component.getPreferredSize().height));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(150, label.getPreferredSize().height));

        panel.add(label, BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);

        return panel;
    }

    private void loadCurrentSettings() {
        internetSearchCheckbox.setSelected(settingsManager.isInternetSearchEnabled());
        autoImplementCheckbox.setSelected(settingsManager.isAutoImplementEnabled());
        analyzeErrorsCheckbox.setSelected(settingsManager.isAnalyzeErrorsEnabled());
        offlineModeCheckbox.setSelected(settingsManager.isOfflineModeEnabled());

        temperatureSpinner.setValue(settingsManager.getTemperature());
        topPSpinner.setValue(settingsManager.getTopP());
        rateLimitSpinner.setValue(settingsManager.getRateLimitDelay() / 1000);

        updateModelSelector();
        modelSelector.setSelectedItem(settingsManager.getSelectedModel());
    }

    private void saveSettings() {
        settingsManager.setInternetSearchEnabled(internetSearchCheckbox.isSelected());
        settingsManager.setAutoImplementEnabled(autoImplementCheckbox.isSelected());
        settingsManager.setAnalyzeErrorsEnabled(analyzeErrorsCheckbox.isSelected());
        settingsManager.setOfflineModeEnabled(offlineModeCheckbox.isSelected());

        settingsManager.setTemperature((Double) temperatureSpinner.getValue());
        settingsManager.setTopP((Double) topPSpinner.getValue());
        settingsManager.setRateLimitDelay((Integer) rateLimitSpinner.getValue() * 1000);

        settingsManager.setSelectedModel((String) modelSelector.getSelectedItem());
        settingsManager.saveSettings();

        JOptionPane.showMessageDialog(this, "Configuración guardada exitosamente",
                "Configuración guardada", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void toggleOfflineMode(ActionEvent e) {
        boolean offlineMode = offlineModeCheckbox.isSelected();
        modelSelector.removeAllItems();

        if (offlineMode) {
            settingsManager.getAvailableLocalModels().forEach(modelSelector::addItem);
        } else {
            settingsManager.getAvailableOnlineModels().forEach(modelSelector::addItem);
        }
    }

    private void updateModelSelector() {
        modelSelector.removeAllItems();

        if (settingsManager.isOfflineModeEnabled()) {
            settingsManager.getAvailableLocalModels().forEach(modelSelector::addItem);
        } else {
            settingsManager.getAvailableOnlineModels().forEach(modelSelector::addItem);
        }
    }
}