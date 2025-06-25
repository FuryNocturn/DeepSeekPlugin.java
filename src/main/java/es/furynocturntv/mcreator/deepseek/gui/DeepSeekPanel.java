package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.gui.components.ActivityIndicator;
import es.furynocturntv.mcreator.deepseek.services.DeepSeekClient;
import es.furynocturntv.mcreator.deepseek.utils.ConversationHistory;
import es.furynocturntv.mcreator.deepseek.utils.SettingsManager;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.validation.Validator;
import net.mcreator.ui.validation.component.VTextField;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class DeepSeekPanel extends JPanel {

    private final MCreator mcreator;
    private final DeepSeekClient apiClient;
    private final ConversationHistory historyManager;
    private final SettingsManager settingsManager;

    // Componentes UI
    private JTextArea chatArea;
    private JTextArea codeArea;
    private VTextField inputField;
    private JButton sendButton;
    private JButton cancelButton;
    private JButton clearButton;
    private JButton historyButton;
    private JButton settingsButton;
    private JLabel costLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JSplitPane splitPane;
    private JTabbedPane tabbedPane;
    private ActivityIndicator activityIndicator;
    private JPanel statusPanel;

    public DeepSeekPanel(MCreator mcreator, DeepSeekClient apiClient,
                         ConversationHistory historyManager, SettingsManager settingsManager) {
        this.mcreator = mcreator;
        this.apiClient = apiClient;
        this.historyManager = historyManager;
        this.settingsManager = settingsManager;

        initComponents();
        setupLayout();
        setupListeners();
    }

    private void initComponents() {
        // Área de chat principal
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Área de código (con pestañas)
        codeArea = new JTextArea();
        codeArea.setEditable(false);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de pestañas
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Conversación", new JScrollPane(chatArea));
        tabbedPane.addTab("Código", new JScrollPane(codeArea));

        // Campo de entrada
        inputField = new VTextField();
        inputField.setPlaceholder("Escribe tu pregunta aquí...");
        inputField.setValidator(Validator.createEmptyValidator(
                () -> !inputField.getText().trim().isEmpty(),
                "El mensaje no puede estar vacío"
        ));

        // Botones
        sendButton = new JButton("Enviar", UIRES.get("16px.send"));
        cancelButton = new JButton("Cancelar", UIRES.get("16px.stop"));
        clearButton = new JButton("Limpiar", UIRES.get("16px.clear"));
        historyButton = new JButton("Historial", UIRES.get("16px.history"));
        settingsButton = new JButton("Configuración", UIRES.get("16px.settings"));

        // Etiquetas de estado
        costLabel = new JLabel("Costo: $0.00");
        costLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        costLabel.setForeground(new Color(0, 100, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(100, 100, 100));

        // Barra de progreso
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        // Configurar estilo de botones
        for (JButton button : new JButton[]{sendButton, cancelButton, clearButton, historyButton, settingsButton}) {
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(true);
            button.setBackground(new Color(240, 240, 240));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
        }

        sendButton.setBackground(new Color(65, 131, 215));
        sendButton.setForeground(Color.WHITE);

        // Crear indicador de actividad
        activityIndicator = new ActivityIndicator();
        activityIndicator.setPreferredSize(new Dimension(20, 20));

        // Panel de estado mejorado
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.add(activityIndicator);
        statusPanel.add(statusLabel);
        activityIndicator.setVisible(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel de contenido principal (chat + código)
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        // Panel de entrada
        JPanel inputPanel = new JPanel(new MigLayout("insets 10, fillx, wrap 2",
                "[grow][pref!]",
                "[pref!][pref!]"));

        inputPanel.add(inputField, "growx");
        inputPanel.add(sendButton, "gapleft 10, h 35!, w 100!");
        inputPanel.add(progressBar, "growx, span 2");
        inputPanel.add(statusLabel, "growx, span 2");
        inputPanel.add(statusPanel, "growx, span 2");

        // Panel de controles inferiores
        JPanel controlPanel = new JPanel(new MigLayout("insets 5, fillx",
                "[pref!][pref!][pref!][pref!][grow][pref!]",
                "[]"));

        controlPanel.add(clearButton);
        controlPanel.add(historyButton);
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(costLabel, "align right");
        controlPanel.add(settingsButton, "gapleft 10");

        // Panel sur combinado
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.CENTER);
        southPanel.add(controlPanel, BorderLayout.SOUTH);
        southPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        // Panel principal con división
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, contentPanel, southPanel);
        splitPane.setDividerSize(5);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(0.8);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);
        setBorder(new EmptyBorder(5, 5, 5, 5));


    }

    private void setupListeners() {
        // Enviar mensaje al presionar Enter
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    sendMessage();
                    e.consume();
                }
            }
        });

        // Acción del botón enviar
        sendButton.addActionListener(e -> sendMessage());

        // Acción del botón cancelar
        cancelButton.addActionListener(e -> {
            apiClient.cancelCurrentRequest();
            setStatus("Operación cancelada", false);
        });

        // Acción del botón limpiar
        clearButton.addActionListener(e -> {
            chatArea.setText("");
            codeArea.setText("");
            costLabel.setText("Costo: $0.00");
        });

        // Acción del botón historial
        historyButton.addActionListener(e -> {
            new ConversationHistoryDialog(mcreator, historyManager).setVisible(true);
        });

        // Acción del botón configuración
        settingsButton.addActionListener(e -> {
            new SettingsDialog(settingsManager, apiClient.getApiKeyManager()).setVisible(true);
        });
    }

    private void sendMessage() {
        if (!inputField.getValidationStatus().isValid()) {
            setStatus("Por favor escribe un mensaje válido", true);
            return;
        }

        String message = inputField.getText().trim();
        inputField.setText("");

        // Mostrar mensaje del usuario
        appendToChat("Tú: " + message + "\n\n");

        // Configurar UI para solicitud
        setStatus("Procesando solicitud...", false);
        activityIndicator.start();
        activityIndicator.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        sendButton.setEnabled(false);
        cancelButton.setEnabled(true);

        // Ejecutar solicitud en un hilo separado
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.sendRequest(message, settingsManager.getSelectedModel());
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    processResponse(response);
                } catch (Exception e) {
                    setStatus("Error: " + e.getMessage(), true);
                } finally {
                    activityIndicator.stop();
                    activityIndicator.setVisible(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    sendButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                }
            }
        }.execute();
    }

    private void processResponse(String response) {
        // Procesar la respuesta (separar texto de código)
        // Esto es un ejemplo básico - deberías implementar un parser más sofisticado
        if (response.contains("```")) {
            String[] parts = response.split("```");
            appendToChat("DeepSeek: " + parts[0]);
            if (parts.length > 1) {
                codeArea.setText(parts[1]);
                tabbedPane.setSelectedIndex(1);
            }
        } else {
            appendToChat("DeepSeek: " + response);
        }

        // Actualizar costo
        double cost = apiClient.getCurrentSessionCost();
        costLabel.setText(String.format("Costo: $%.4f", cost));

        setStatus("Listo", false);
    }

    private void appendToChat(String text) {
        chatArea.append(text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : new Color(100, 100, 100));
    }
}