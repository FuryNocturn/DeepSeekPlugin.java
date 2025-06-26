package es.furynocturntv.mcreator.deepseek.gui;

import es.furynocturntv.mcreator.deepseek.utils.ConversationHistory;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.validation.Validator;
import net.mcreator.ui.validation.component.VTextField;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * Diálogo para visualizar y gestionar el historial de conversaciones
 * Incluye funcionalidades de búsqueda y filtrado
 */
public class ConversationHistoryDialog extends JDialog {

    private final ConversationHistory history;
    private final JTable conversationsTable;
    private final ConversationTableModel tableModel;
    private final JLabel statsLabel;

    public ConversationHistoryDialog(Frame parent, ConversationHistory history) {
        super(parent, "Historial de Conversaciones", true);
        this.history = history;

        setSize(800, 600);
        setLocationRelativeTo(parent);

        initComponents();
        setupLayout();
        loadConversations();
    }

    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        // Modelo de tabla personalizado
        tableModel = new ConversationTableModel();
        conversationsTable = new JTable(tableModel);
        conversationsTable.setRowHeight(30);
        conversationsTable.setAutoCreateRowSearcher(true);
        conversationsTable.setDefaultRenderer(Object.class, new ConversationCellRenderer());

        // Configurar selección de una sola fila
        conversationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Etiqueta de estadísticas
        statsLabel = new JLabel();
        updateStatsLabel();

        // Botones de acción
        JButton refreshButton = new JButton("Actualizar");
        refreshButton.setIcon(UIRES.get("16px.refresh"));
        refreshButton.addActionListener(e -> refreshConversations());

        JButton deleteButton = new JButton("Eliminar");
        deleteButton.setIcon(UIRES.get("16px.delete"));
        deleteButton.addActionListener(this::deleteSelectedConversation);

        JButton clearAllButton = new JButton("Limpiar Todo");
        clearAllButton.setIcon(UIRES.get("16px.clear"));
        clearAllButton.addActionListener(this::clearAllConversations);

        // Campo de búsqueda
        VTextField searchField = new VTextField();
        searchField.setPlaceholder("Buscar en conversaciones...");
        searchField.setValidator(Validator.createEmptyValidator(
                () -> searchField.getText().length() >= 3 || searchField.getText().isEmpty(),
                "Mínimo 3 caracteres"
        ));
        searchField.addActionListener(e -> searchConversations(searchField.getText()));
    }

    /**
     * Organiza el layout de los componentes
     */
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de búsqueda
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        VTextField searchField = new VTextField();
        searchField.setPlaceholder("Buscar en conversaciones...");
        searchField.addActionListener(e -> searchConversations(searchField.getText()));

        JButton searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> searchConversations(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Panel de tabla con scroll
        JScrollPane scrollPane = new JScrollPane(conversationsTable);
        scrollPane.setPreferredSize(new Dimension(750, 400));

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearAllButton);

        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.add(statsLabel);

        // Ensamblar componentes
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadConversations() {
        tableModel.setConversations(history.getRecentConversations(100)); // Últimas 100 conversaciones
        updateStatsLabel();
    }

    private void refreshConversations() {
        loadConversations();
        JOptionPane.showMessageDialog(this,
                "Historial actualizado",
                "Actualización",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedConversation(ActionEvent e) {
        int selectedRow = conversationsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = conversationsTable.convertRowIndexToModel(selectedRow);
            ConversationHistory.Conversation toDelete = tableModel.getConversationAt(modelRow);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar esta conversación?\n" + toDelete.prompt,
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                history.deleteConversation(toDelete);
                loadConversations();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Selecciona una conversación primero",
                    "Nada seleccionado",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearAllConversations(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar TODAS las conversaciones? Esta acción no se puede deshacer.",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            history.clearHistory();
            loadConversations();
        }
    }

    private void searchConversations(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadConversations();
        } else {
            tableModel.setConversations(history.searchConversations(query));
        }
        updateStatsLabel();
    }

    private void updateStatsLabel() {
        double totalCost = history.getTotalCost();
        int totalConversations = tableModel.getRowCount();

        statsLabel.setText(String.format(
                "Mostrando %d conversaciones | Costo total: $%.4f",
                totalConversations, totalCost
        ));
    }

    /**
     * Modelo de tabla personalizado para mostrar conversaciones
     */
    private class ConversationTableModel extends AbstractTableModel {
        private List<ConversationHistory.Conversation> conversations;
        private final DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

        /**
         * Actualiza las conversaciones mostradas en la tabla
         * @param conversations Nueva lista de conversaciones
         */
        public void setConversations(List<ConversationHistory.Conversation> conversations) {
            this.conversations = conversations;
            fireTableDataChanged();
        }

        public ConversationHistory.Conversation getConversationAt(int row) {
            return conversations.get(row);
        }

        @Override
        public int getRowCount() {
            return conversations != null ? conversations.size() : 0;
        }

        @Override
        public int getColumnCount() {
            return 4; // Fecha, Prompt, Respuesta (abreviada), Costo
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Fecha";
                case 1 -> "Prompt";
                case 2 -> "Respuesta";
                case 3 -> "Costo";
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            ConversationHistory.Conversation conv = conversations.get(row);
            return switch (column) {
                case 0 -> conv.timestamp.format(dateFormatter);
                case 1 -> StringUtils.abbreviate(conv.prompt, 50);
                case 2 -> StringUtils.abbreviate(conv.response.replace("\n", " "), 70);
                case 3 -> String.format("$%.4f", conv.cost);
                default -> "";
            };
        }
    }

    // Renderer personalizado para celdas
    private static class ConversationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Resaltar filas con mayor costo
            ConversationTableModel model = (ConversationTableModel) table.getModel();
            ConversationHistory.Conversation conv = model.getConversationAt(row);

            if (conv.cost > 0.01) { // Umbral para resaltar
                c.setBackground(new Color(255, 245, 230)); // Naranja claro
            } else if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(table.getBackground());
            }

            // Alinear columnas
            if (column == 3) { // Columna de costo
                setHorizontalAlignment(RIGHT);
            } else {
                setHorizontalAlignment(LEFT);
            }

            return c;
        }
    }
}