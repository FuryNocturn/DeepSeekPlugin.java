package es.furynocturntv.mcreator.deepseek;

    //Carga modulos Plugins
import es.furynocturntv.mcreator.deepseek.utils.*;
import es.furynocturntv.mcreator.deepseek.services.*;
import es.furynocturntv.mcreator.deepseek.api.*;
import es.furynocturntv.mcreator.deepseek.gui.*;
import es.furynocturntv.mcreator.deepseek.utils.ConversationHistory;
import es.furynocturntv.mcreator.deepseek.utils.PluginLoader;
import es.furynocturntv.mcreator.deepseek.config.DeepSeekPreferences;
import es.furynocturntv.mcreator.deepseek.utils.ResponseCache;

    //Carga modulos Mcreator
import net.mcreator.plugin.*;
import net.mcreator.plugin.events.*;
import net.mcreator.ui.*;
import net.mcreator.ui.action.*;
import net.mcreator.ui.init.*;

    //Carga De Extras
import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeepSeekPlugin extends JavaPlugin {
    private static final String VERSION = "2.0.1";
    private static final String AUTHOR = "FuryNocturnTV";
    private static final String DESCRIPTION = "Asistente de IA DeepSeek integrado en MCreator";

    private DeepSeekModule module;
    private ApiKeyManager apiKeyManager;
    private DeepSeekClient apiClient;
    private ConversationHistory historyManager;
    private SettingsManager settingsManager;
    private ResponseCache responseCache;
    private ReportGenerator reportGenerator;
    private PluginLoader pluginLoader;
    private CodeGenerator codeGenerator;
    private ScheduledExecutorService scheduler;

    public DeepSeekPlugin(Plugin plugin) {
        super(plugin);

        PluginLogger.log("Inicializando DeepSeek Plugin v" + VERSION);

        // Inicialización de componentes principales
        initializeCoreComponents();

        // Inicialización de componentes avanzados
        initializeAdvancedComponents();

        // Registrar módulo
        registerModule(module);

        // Configurar eventos
        addListener(PreGeneratorsLoadingEvent.class, event -> onMCreatorLoaded(event.getMCreator()));

        PluginLogger.log("DeepSeek Assistant plugin inicializado correctamente");
    }

    private void initializeCoreComponents() {
        this.apiKeyManager = new ApiKeyManager();
        this.responseCache = new ResponseCache(TimeUnit.HOURS.toMillis(1), 1000);
        this.settingsManager = new SettingsManager(responseCache);
        this.historyManager = new ConversationHistory();
        this.apiClient = new DeepSeekClient(apiKeyManager, settingsManager);
        this.module = new DeepSeekModule(apiClient, historyManager, settingsManager);
    }

    private void initializeAdvancedComponents() {
        // Sistema de reportes
        this.reportGenerator = new ReportGenerator(historyManager, "furynocturntv@gmail.com");

        // Cargador de plugins
        this.pluginLoader = new PluginLoader();
        pluginLoader.loadPlugins();

        // Programar reporte mensual
        scheduleMonthlyReport();

        // Inicializar generador de código
        this.codeGenerator = new CodeGenerator(null); // Se actualizará cuando se cargue el workspace
    }

    private void scheduleMonthlyReport() {
        scheduler = Executors.newScheduledThreadPool(1);

        // Calcular tiempo hasta el próximo día 1 a las 9:00 AM
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReport = now.withDayOfMonth(1).plusMonths(1).withHour(9).withMinute(0);

        long initialDelay = Duration.between(now, nextReport).toMillis();
        long period = TimeUnit.DAYS.toMillis(30); // Aproximadamente un mes

        scheduler.scheduleAtFixedRate(() -> {
            try {
                reportGenerator.sendMonthlyReport();
            } catch (Exception e) {
                PluginLogger.logError("Error generando reporte mensual", e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    private void onMCreatorLoaded(MCreator mcreator) {
        PluginLogger.log("MCreator cargado, inicializando interfaz...");

        // Actualizar generador de código con el workspace
        this.codeGenerator = new CodeGenerator(mcreator.getWorkspace());

        // Verificar si es la primera ejecución
        if (isFirstRun()) {
            SwingUtilities.invokeLater(() -> showApiKeyDialog(mcreator));
        }

        // Registrar acciones en la UI de MCreator
        registerActions(mcreator.getActionRegistry());

        // Inicializar panel de control
        module.initializeControlPanel(mcreator, codeGenerator);

        // Inicializar plugins
        initializePlugins(mcreator);
    }

    private void initializePlugins(MCreator mcreator) {
        pluginLoader.getExtensions().forEach(extension -> {
            try {
                extension.initialize();
                PluginLogger.log("Extensión inicializada: " + extension.getName());
            } catch (Exception e) {
                PluginLogger.logError("Error inicializando extensión: " + extension.getName(), e);
            }
        });
    }

    private boolean isFirstRun() {
        return DeepSeekPreferences.getInstance().isFirstRun();
    }

    private void showApiKeyDialog(MCreator mcreator) {
        ApiKeyDialog dialog = new ApiKeyDialog(mcreator, apiKeyManager);
        dialog.setVisible(true);

        // Marcar que ya no es la primera ejecución
        setFirstRunCompleted();
    }

    private void registerActions(ActionRegistry actionRegistry) {
        // Acción para abrir el panel de DeepSeek
        BasicAction openDeepSeekAction = new BasicAction("Open DeepSeek Assistant",
                e -> module.showControlPanel());

        openDeepSeekAction.setIcon(UIRES.get("deepseek.icon"));
        actionRegistry.registerAction("DeepSeek", openDeepSeekAction);

        // Acción para configuración
        BasicAction settingsAction = new BasicAction("DeepSeek Settings",
                e -> new SettingsDialog(settingsManager, apiKeyManager).setVisible(true));

        actionRegistry.registerAction("DeepSeekSettings", settingsAction);

        // Acción para historial
        BasicAction historyAction = new BasicAction("DeepSeek History",
                e -> module.showConversationHistory());

        actionRegistry.registerAction("DeepSeekHistory", historyAction);
    }

    @Override
    public void onShutdown() {
        PluginLogger.log("Apagando DeepSeek Plugin...");

        // Apagar componentes
        shutdownComponents();

        PluginLogger.log("DeepSeek Plugin apagado correctamente");
    }

    private void shutdownComponents() {
        // Apagar el cliente API
        if (apiClient != null) {
            apiClient.shutdown();
        }

        // Apagar el scheduler de reportes
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Guardar historial
        if (historyManager != null) {
            historyManager.saveConversations();
        }

        // Notificar a los plugins
        if (pluginLoader != null) {
            pluginLoader.getExtensions().forEach(extension -> {
                try {
                    if (extension instanceof AutoCloseable) {
                        ((AutoCloseable) extension).close();
                    }
                } catch (Exception e) {
                    PluginLogger.logError("Error cerrando extensión: " + extension.getName(), e);
                }
            });
        }
    }

    // Métodos de acceso para otros componentes del plugin
    public DeepSeekClient getApiClient() {
        return apiClient;
    }

    public ConversationHistory getHistoryManager() {
        return historyManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public CodeGenerator getCodeGenerator() {
        return codeGenerator;
    }

    public PluginLoader getPluginLoader() {
        return pluginLoader;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getAuthor() {
        return AUTHOR;
    }

    public static String getDescription() {
        return DESCRIPTION;
    }

    private void setFirstRunCompleted() {
        DeepSeekPreferences.getInstance().setFirstRun(false);
    }
}