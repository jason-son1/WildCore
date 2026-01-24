package com.myserver.wildcore;

import com.myserver.wildcore.commands.MainCommand;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.gui.admin.AdminGuiListener;
import com.myserver.wildcore.listeners.BlockListener;
import com.myserver.wildcore.listeners.ChatListener;
import com.myserver.wildcore.listeners.CustomItemProtectListener;
import com.myserver.wildcore.listeners.GuiListener;
import com.myserver.wildcore.listeners.PlayerListener;
import com.myserver.wildcore.listeners.ShopInteractListener;
import com.myserver.wildcore.managers.EnchantManager;
import com.myserver.wildcore.managers.ShopManager;
import com.myserver.wildcore.managers.StockManager;
import com.myserver.wildcore.placeholder.WildCorePlaceholder;
import com.myserver.wildcore.tasks.ActionBarMoneyTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * WildCore 플러그인 메인 클래스
 * 마인크래프트 야생 서버용 핵심 플러그인 - 주식, 인챈트, 유틸리티
 */
public class WildCore extends JavaPlugin {

    private static WildCore instance;
    private Economy economy;

    private ConfigManager configManager;
    private StockManager stockManager;
    private EnchantManager enchantManager;
    private ShopManager shopManager;
    private AdminGuiListener adminGuiListener;
    private ActionBarMoneyTask actionBarMoneyTask;

    @Override
    public void onEnable() {
        instance = this;

        // 설정 파일 초기화
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        // Vault 경제 시스템 연결
        if (!setupEconomy()) {
            getLogger().severe("Vault 경제 시스템을 찾을 수 없습니다! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 매니저 초기화
        stockManager = new StockManager(this);
        enchantManager = new EnchantManager(this);
        shopManager = new ShopManager(this);
        adminGuiListener = new AdminGuiListener(this);

        // 주식 스케줄러 시작
        stockManager.startScheduler();

        // 상점 NPC 로드
        shopManager.loadAllShops();

        // 이벤트 리스너 등록
        registerListeners();

        // 명령어 등록
        registerCommands();

        // ActionBar 돈 표시 태스크 시작
        actionBarMoneyTask = new ActionBarMoneyTask(this);
        actionBarMoneyTask.start();
        getLogger().info("ActionBar 잔액 표시 기능이 활성화되었습니다.");

        // PlaceholderAPI 연동
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WildCorePlaceholder(this).register();
            getLogger().info("PlaceholderAPI가 연동되었습니다.");
        } else {
            getLogger().warning("PlaceholderAPI를 찾을 수 없습니다. 플레이스홀더 기능이 비활성화됩니다.");
        }

        getLogger().info("WildCore 플러그인이 활성화되었습니다!");
        getLogger().info("버전: " + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        // ActionBar 태스크 중지
        if (actionBarMoneyTask != null) {
            actionBarMoneyTask.stop();
        }

        // 주식 스케줄러 중지
        if (stockManager != null) {
            stockManager.stopScheduler();
            stockManager.saveAllData();
        }

        // 상점 데이터 저장
        if (shopManager != null) {
            shopManager.saveAllShops();
        }

        getLogger().info("WildCore 플러그인이 비활성화되었습니다.");
    }

    /**
     * Vault 경제 시스템 설정
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * 이벤트 리스너 등록
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInteractListener(this), this);
        getServer().getPluginManager().registerEvents(adminGuiListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, adminGuiListener), this);
    }

    /**
     * 명령어 등록
     */
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("wildcore").setExecutor(mainCommand);
        getCommand("wildcore").setTabCompleter(mainCommand);
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        configManager.loadAllConfigs();
        stockManager.reload();
        enchantManager.reload();
        shopManager.reload();
    }

    /**
     * 디버그 로그 출력
     */
    public void debug(String message) {
        if (configManager.isDebugEnabled()) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }

    // Getter 메서드들
    public static WildCore getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StockManager getStockManager() {
        return stockManager;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
