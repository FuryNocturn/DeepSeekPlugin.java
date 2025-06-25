package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.utils.ResponseCache;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class CacheManagementDialog extends JDialog {
    private final ResponseCache cache;

    public CacheManagementDialog(Frame parent, ResponseCache cache) {
        super(parent, "Gestión de Caché Inteligente", true);
        this.cache = cache;

        initUI();
        setSize(400, 300);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de estadísticas
        ResponseCache.CacheStats stats = cache.getStats();
        DecimalFormat df = new DecimalFormat("#.##");

        JPanel statsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        statsPanel.add(new JLabel("Estadísticas del Caché:"));
        statsPanel.add(new JLabel("Entradas totales: " + stats.totalEntries));
        statsPanel.add(new JLabel("Entradas expiradas: " + stats.expiredEntries));
        statsPanel.add(new JLabel("Tamaño máximo: " + stats.maxSize));
        statsPanel.add(new JLabel("Uso: " + df.format(stats.getUsagePercentage()) + "%"));

        // Botones de acción
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton refreshButton = new JButton("Actualizar");
        JButton clearButton = new JButton("Limpiar Caché");

        refreshButton.addActionListener(e -> updateStats());
        clearButton.addActionListener(e -> {
            cache.clear();
            updateStats();
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);

        panel.add(statsPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void updateStats() {
        // Implementar actualización de estadísticas en la UI
    }
}