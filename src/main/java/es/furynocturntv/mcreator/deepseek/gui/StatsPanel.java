package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.utils.ConversationHistory;


import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsPanel extends JPanel {
    private final ConversationHistory history;

    public StatsPanel(ConversationHistory history) {
        this.history = history;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Crear pestañas
        JTabbedPane tabbedPane = new JTabbedPane();

        // Pestaña de resumen
        tabbedPane.addTab("Resumen", createSummaryPanel());

        // Pestaña de gráficos
        tabbedPane.addTab("Gráficos", createChartsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Estadísticas básicas
        int totalConversations = history.getRecentConversations(Integer.MAX_VALUE).size();
        double totalCost = history.getTotalCost();

        panel.add(new JLabel("Total de conversaciones: " + totalConversations));
        panel.add(new JLabel("Costo total acumulado: $" + String.format("%.4f", totalCost)));

        // Últimas interacciones
        panel.add(new JLabel(" "));
        panel.add(new JLabel("Últimas interacciones:"));

        for (ConversationHistory.Conversation conv : history.getRecentConversations(5)) {
            panel.add(new JLabel(formatConversation(conv)));
        }

        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Gráfico de costos por día
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Agrupar conversaciones por día
        Map<LocalDate, Double> dailyCosts = history.getRecentConversations(Integer.MAX_VALUE).stream()
                .collect(Collectors.groupingBy(
                        conv -> conv.timestamp.toLocalDate(),
                        Collectors.summingDouble(conv -> conv.cost)
                ));

        // Ordenar por fecha y agregar al dataset
        dailyCosts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> dataset.addValue(entry.getValue(), "Costo", entry.getKey()));

        JFreeChart chart = ChartFactory.createBarChart(
                "Costo por día",
                "Fecha",
                "Costo ($)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private String formatConversation(ConversationHistory.Conversation conv) {
        return String.format("[%s] %s - Costo: $%.4f",
                conv.timestamp.toLocalTime().toString().substring(0, 5),
                StringUtils.abbreviate(conv.prompt, 30),
                conv.cost);
    }
}