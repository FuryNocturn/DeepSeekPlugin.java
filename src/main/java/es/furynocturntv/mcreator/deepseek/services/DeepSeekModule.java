package es.furynocturntv.mcreator.deepseek.services;

//Carga modulos Plugins
import es.furynocturntv.mcreator.deepseek.utils.*;
import es.furynocturntv.mcreator.deepseek.gui.*;

//Carga modulos mcreator
import es.furynocturntv.mcreator.deepseek.utils.ConversationHistory;
import net.mcreator.ui.MCreator;

public class DeepSeekModule {
    private final DeepSeekClient apiClient;
    private final ConversationHistory historyManager;
    private final SettingsManager settingsManager;
    private DeepSeekPanel controlPanel;
    private CodeGenerator codeGenerator;

    public DeepSeekModule(DeepSeekClient apiClient, ConversationHistory historyManager,
                          SettingsManager settingsManager) {
        this.apiClient = apiClient;
        this.historyManager = historyManager;
        this.settingsManager = settingsManager;
    }

    public void initializeControlPanel(MCreator mcreator, CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
        this.controlPanel = new DeepSeekPanel(mcreator, apiClient, historyManager,
                settingsManager, codeGenerator);
    }

    public void showControlPanel() {
        if (controlPanel != null) {
            controlPanel.setVisible(true);
        }
    }

    public void showConversationHistory() {
        if (controlPanel != null) {
            controlPanel.showHistoryDialog();
        }
    }
}