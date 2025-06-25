package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.models.ModelDownloader;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModelManagementDialog extends JDialog {

    public ModelManagementDialog(JFrame parent) {
        super(parent, "Gestión de Modelos Locales", true);

        initUI();
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de información
        JTextArea infoArea = new JTextArea(
                "Modelos locales disponibles:\n\n" +
                        "• deepseek-coder-33b-instruct (recomendado para programación)\n" +
                        "  - Tamaño: ~20GB (cuantizado Q4_K_M)\n" +
                        "  - Requisitos: 16GB+ RAM, GPU recomendada\n\n" +
                        "Puedes importar otros modelos GGUF compatibles con llama.cpp"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());

        // Panel de acciones
        JPanel actionPanel = new JPanel(new GridLayout(0, 1, 5, 5));

        JButton downloadButton = new JButton("Descargar Modelo Predeterminado");
        downloadButton.addActionListener(e -> downloadDefaultModel());

        JButton importButton = new JButton("Importar Modelo GGUF");
        importButton.addActionListener(e -> importModel());

        JButton openFolderButton = new JButton("Abrir Carpeta de Modelos");
        openFolderButton.addActionListener(e -> openModelsFolder());

        actionPanel.add(downloadButton);
        actionPanel.add(importButton);
        actionPanel.add(openFolderButton);

        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void downloadDefaultModel() {
        new Thread(() -> {
            try {
                Path path = Paths.get(System.getProperty("user.home"), ".deepseek-mcreator", "models");
                ModelDownloader.downloadDefaultModel(path);

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Modelo descargado exitosamente",
                                "Descarga completada",
                                JOptionPane.INFORMATION_MESSAGE)
                );
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Error descargando modelo: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void importModel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Seleccionar archivo GGUF");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Aquí implementarías la lógica de importación
            JOptionPane.showMessageDialog(this,
                    "Modelo importado exitosamente (simulado)",
                    "Importación completada",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openModelsFolder() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".deepseek-mcreator", "models");
            Desktop.getDesktop().open(path.toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir la carpeta: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}