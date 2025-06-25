package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.utils.PluginLogger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DebugToolsPanel extends JPanel {
    private final JTextArea logArea;

    public DebugToolsPanel() {
        setLayout(new BorderLayout());

        // Área de logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshButton = new JButton("Actualizar Logs");
        refreshButton.addActionListener(e -> refreshLogs());

        JButton clearButton = new JButton("Limpiar Logs");
        clearButton.addActionListener(e -> {
            PluginLogger.clearLogs();
            refreshLogs();
        });

        JButton exportButton = new JButton("Exportar Logs");
        exportButton.addActionListener(e -> exportLogs());

        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exportButton);

        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshLogs();
    }

    private void refreshLogs() {
        logArea.setText(PluginLogger.getLogContent());
        logArea.setCaretPosition(0);
    }

    private void exportLogs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Logs");
        fileChooser.setSelectedFile(new File("deepseek_plugin_logs.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(
                        fileChooser.getSelectedFile().toPath(),
                        PluginLogger.getLogContent()
                );
                JOptionPane.showMessageDialog(this,
                        "Logs exportados exitosamente",
                        "Exportación completada",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error exportando logs: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}