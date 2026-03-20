package moscow.mytheria;

import java.awt.image.BufferedImage;
import java.net.URI;
import javax.imageio.ImageIO;
import lombok.Generated;
import moscow.mytheria.framework.shader.GlProgram;
import moscow.mytheria.systems.commands.CommandRegistry;
import moscow.mytheria.systems.config.ConfigDropHandler;
import moscow.mytheria.systems.config.ConfigFile;
import moscow.mytheria.systems.config.ConfigManager;
import moscow.mytheria.systems.discord.DiscordManager;
import moscow.mytheria.systems.event.EventIntegration;
import moscow.mytheria.systems.event.EventManager;
import moscow.mytheria.systems.event.handlers.ServerConnectionHandler;
import moscow.mytheria.systems.file.FileManager;
import moscow.mytheria.systems.friends.FriendManager;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.modules.ModuleManager;
import moscow.mytheria.systems.modules.constructions.swinganim.SwingManager;
import moscow.mytheria.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import moscow.mytheria.systems.modules.listeners.ModuleTickListener;
import moscow.mytheria.systems.modules.listeners.ModuleWidgetRenderer;
import moscow.mytheria.systems.notifications.NotificationManager;
import moscow.mytheria.systems.poshalko.PoshalkoHandler;
import moscow.mytheria.systems.target.TargetManager;
import moscow.mytheria.systems.theme.ThemeManager;
import moscow.mytheria.systems.waypoints.WayPointsManager;
import moscow.mytheria.ui.hud.Hud;
import moscow.mytheria.ui.menu.MenuScreen;
import moscow.mytheria.utility.debug.DebugModeDetector;
import moscow.mytheria.utility.debug.ProcessWatchdog;
import moscow.mytheria.utility.debug.ThreadDebugger;
import moscow.mytheria.utility.game.TitleBarHelper;
import moscow.mytheria.utility.game.WebUtility;
import moscow.mytheria.utility.game.server.TPSHandler;
import moscow.mytheria.utility.interfaces.IMinecraft;
import moscow.mytheria.utility.math.calculator.ChatListener;
import moscow.mytheria.utility.render.DrawUtility;
import moscow.mytheria.utility.rotations.RotationHandler;
import moscow.mytheria.utility.rotations.RotationUpdateListener;
import moscow.mytheria.utility.sounds.MusicTracker;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;
import ru.kotopushka.compiler.sdk.annotations.Initialization;

public enum Mytheria implements IMinecraft {
   INSTANCE;

   public static final String NAME = "Mytheria";
   public static final String BUILD_TYPE = "Beta";
   public static final String VERSION = "INTELIJ IDEA READY";
   public static final String MOD_ID = "Mytheria".toLowerCase();
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private EventManager eventManager;
   private ThemeManager themeManager;
   private ModuleManager moduleManager;
   private CommandRegistry commandManager;
   private FriendManager friendManager;
   private DiscordManager discordManager;
   private RotationHandler rotationHandler;
   private TargetManager targetManager;
   private MusicTracker musicTracker;
   private FileManager fileManager;
   private NotificationManager notificationManager;
   private ConfigManager configManager;
   private SwingManager swingManager;
   private TPSHandler tpsHandler;
   private Hud hud;
   private ServerConnectionHandler serverConnectionHandler;
   private PoshalkoHandler poshalkoHandler;
   private WayPointsManager wayPointsManager;
   private SwingPresetManager swingPresetManager;
   private MenuScreen menuScreen;
   private ChatListener chatListener;
   private boolean panic;

   @Compile
   @Initialization
   public void initialize() {
      LOGGER.info("Initializing {}...", "Mytheria");
      this.musicTracker = new MusicTracker();
      this.wayPointsManager = new WayPointsManager();
      this.eventManager = new EventManager();
      this.friendManager = new FriendManager();
      this.themeManager = new ThemeManager();
      this.discordManager = new DiscordManager();
      this.rotationHandler = new RotationHandler(new RotationUpdateListener());
      this.targetManager = new TargetManager();
      this.fileManager = new FileManager();
      this.moduleManager = new ModuleManager(new ModuleTickListener(), new ModuleWidgetRenderer());
      this.hud = new Hud();
      this.tpsHandler = new TPSHandler();
      this.notificationManager = new NotificationManager();
      this.configManager = new ConfigManager();
      this.fileManager.registerClientFiles();
      this.moduleManager.registerModules();
      this.moduleManager.enableModules();
      this.configManager.handle();
      this.commandManager = new CommandRegistry();
      this.commandManager.initCommands();
      this.swingManager = new SwingManager();
      this.swingPresetManager = new SwingPresetManager();
      this.swingPresetManager.handle();
      this.fileManager.loadClientFiles();
      ConfigFile autosaveConfig = this.configManager.getConfig("autosave", true);
      if (autosaveConfig != null) {
         LOGGER.info("Loading autosave config on startup...");
         autosaveConfig.load();
         this.configManager.setCurrent(autosaveConfig);
         LOGGER.info(
            "Autosave config loaded and set as current. Modules enabled: {}", this.moduleManager.getModules().stream().filter(m -> m.isEnabled()).count()
         );
      } else {
         LOGGER.error("Autosave config not found!");
      }

      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return Mytheria.id("after_shader_load");
         }

         public void reload(ResourceManager manager) {
            try {
               GlProgram.loadAndSetupPrograms();
            } catch (Exception var3) {
               Mytheria.LOGGER.warn("Failed to load shader programs, continuing without them", var3);
            }
         }
      });
      DrawUtility.initializeShaders();
      Localizator.loadTranslations();
      this.chatListener = new ChatListener();
      this.serverConnectionHandler = new ServerConnectionHandler();
      this.poshalkoHandler = new PoshalkoHandler();
      String osName = System.getProperty("os.name");
      String pcName = System.getProperty("user.name");
      if (osName.toLowerCase().contains("windows") && !pcName.equals("nesquik")) {
         this.discordManager.connect();
      }

      ConfigDropHandler.init();
      TitleBarHelper.setDarkTitleBar();
      new EventIntegration();
      this.createAvatar();
      ProcessWatchdog.startWatchdog();
      LOGGER.info("{} initialized", "Mytheria");
   }

   public void shutdown() {
      LOGGER.error("=== MYTHERIA SHUTDOWN STARTED ===");
      System.err.println("=== MYTHERIA SHUTDOWN STARTED ===");
      ProcessWatchdog.triggerShutdown();
      boolean isDebugMode = DebugModeDetector.isDebugMode();
      if (this.moduleManager != null) {
         this.moduleManager.disableAllModules();
      }

      this.fileManager.saveClientFiles();
      if (!this.isPanic()) {
         LOGGER.error("=== SAVING AUTOSAVE CONFIG ===");
         System.err.println("=== SAVING AUTOSAVE CONFIG ===");
         this.swingPresetManager.getAutoSavePreset().save();
         ConfigFile autosaveConfig = this.configManager.getConfig("autosave", false);
         if (autosaveConfig != null) {
            autosaveConfig.save();
         }

         LOGGER.error("=== AUTOSAVE CONFIG SAVED ===");
         System.err.println("=== AUTOSAVE CONFIG SAVED ===");
      } else {
         LOGGER.error("=== PANIC MODE - SKIPPING AUTOSAVE ===");
      }

      if (this.discordManager != null) {
         this.discordManager.disconnect();
      }

      if (this.musicTracker != null) {
         this.musicTracker.shutdown();
      }

      try {
         Class<?> mytheriaClass = Class.forName("ru.mytheria.Mytheria");
         Object mytheriaInstance = mytheriaClass.getMethod("getInstance").invoke(null);
         if (mytheriaInstance != null) {
            mytheriaClass.getMethod("shutdown").invoke(mytheriaInstance);
         }
      } catch (ClassNotFoundException var4) {
         LOGGER.warn("Mytheria Visuals not found, skipping shutdown");
      } catch (Exception var5) {
         LOGGER.error("Error shutting down Mytheria Visuals: {}", var5.getMessage());
      }

      this.setPanic(false);
      Thread shutdownThread = new Thread(() -> {
         try {
            Thread.sleep(300L);
            ThreadDebugger.logAllThreads();
            ThreadDebugger.interruptAllNonDaemonThreads();
            System.gc();
            Thread.sleep(500L);
            LOGGER.info("Force exiting JVM");
            System.exit(0);
            Runtime.getRuntime().halt(0);
         } catch (InterruptedException var1x) {
            Thread.currentThread().interrupt();
            Runtime.getRuntime().halt(0);
         } catch (Exception var2x) {
            LOGGER.error("Error during shutdown: {}", var2x.getMessage());
            Runtime.getRuntime().halt(0);
         }
      });
      shutdownThread.setDaemon(true);
      shutdownThread.start();
   }

   public static Mytheria getInstance() {
      return INSTANCE;
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }

   @CompileBytecode
   private void createAvatar() {
      try {
         BufferedImage bufferedImage = ImageIO.read(URI.create("https://mytheria.pub/api/avatars/ConeTin.jpg?t=1754613855632").toURL());
         if (bufferedImage == null) {
            return;
         }

         Identifier id = id("temp/avatar");
         mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(WebUtility.bufferedImageToNativeImage(bufferedImage, true)));
      } catch (Exception var3) {
      }
   }

   @Generated
   public EventManager getEventManager() {
      return this.eventManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public CommandRegistry getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }

   @Generated
   public RotationHandler getRotationHandler() {
      return this.rotationHandler;
   }

   @Generated
   public TargetManager getTargetManager() {
      return this.targetManager;
   }

   @Generated
   public MusicTracker getMusicTracker() {
      return this.musicTracker;
   }

   @Generated
   public FileManager getFileManager() {
      return this.fileManager;
   }

   @Generated
   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public SwingManager getSwingManager() {
      return this.swingManager;
   }

   @Generated
   public SwingPresetManager getSwingPresetManager() {
      return this.swingPresetManager;
   }

   @Generated
   public TPSHandler getTpsHandler() {
      return this.tpsHandler;
   }

   @Generated
   public Hud getHud() {
      return this.hud;
   }

   @Generated
   public ServerConnectionHandler getServerConnectionHandler() {
      return this.serverConnectionHandler;
   }

   @Generated
   public PoshalkoHandler getPoshalkoHandler() {
      return this.poshalkoHandler;
   }

   @Generated
   public WayPointsManager getWayPointsManager() {
      return this.wayPointsManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }

   @Generated
   public ChatListener getChatListener() {
      return this.chatListener;
   }

   @Generated
   public boolean isPanic() {
      return this.panic;
   }

   @Generated
   public void setMenuScreen(MenuScreen menuScreen) {
      this.menuScreen = menuScreen;
   }

   @Generated
   public void setPanic(boolean panic) {
      this.panic = panic;
   }
}
