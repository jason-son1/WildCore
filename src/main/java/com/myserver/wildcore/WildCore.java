package com.myserver.wildcore;

import com.myserver.wildcore.commands.MainCommand;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.gui.admin.AdminGuiListener;
import com.myserver.wildcore.listeners.BlockListener;
import com.myserver.wildcore.listeners.ChatListener;
import com.myserver.wildcore.listeners.CustomItemProtectListener;
import com.myserver.wildcore.listeners.GuiListener;
import com.myserver.wildcore.listeners.NpcDamageListener;
import com.myserver.wildcore.listeners.NpcInteractListener;
import com.myserver.wildcore.listeners.PlayerListener;
import com.myserver.wildcore.listeners.BuffBlockListener;
import com.myserver.wildcore.listeners.FarmClaimListener;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.managers.ClaimDataManager;
import com.myserver.wildcore.managers.ClaimChunkLoader;
import com.myserver.wildcore.managers.CropGrowthManager;
import com.myserver.wildcore.managers.ClaimScoreboardManager;
import com.myserver.wildcore.listeners.ClaimProtectionListener;
import com.myserver.wildcore.listeners.CropGrowthBuffListener;
import com.myserver.wildcore.gui.claim.ClaimGUIListener;
import com.myserver.wildcore.managers.EnchantManager;
import com.myserver.wildcore.managers.NpcManager;
import com.myserver.wildcore.managers.ShopManager;
import com.myserver.wildcore.managers.StockManager;
import com.myserver.wildcore.managers.BankManager;
import com.myserver.wildcore.managers.MiningDropManager;
import com.myserver.wildcore.managers.RepairManager;
import com.myserver.wildcore.placeholder.WildCorePlaceholder;
import com.myserver.wildcore.tasks.ActionBarMoneyTask;
import com.myserver.wildcore.gui.AutoRefreshGUI;
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
    private NpcManager npcManager;
    private ShopManager shopManager;
    private BankManager bankManager;
    private MiningDropManager miningDropManager;
    private ClaimManager claimManager;
    private ClaimDataManager claimDataManager;
    private RepairManager repairManager;
    private AdminGuiListener adminGuiListener;
    private ChatListener chatListener;
    private ActionBarMoneyTask actionBarMoneyTask;
    private CropGrowthManager cropGrowthManager;
    private ClaimChunkLoader claimChunkLoader;
    private ClaimScoreboardManager claimScoreboardManager;

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
        npcManager = new NpcManager(this);
        shopManager = new ShopManager(this);
        shopManager = new ShopManager(this);
        bankManager = new BankManager(this);
        miningDropManager = new MiningDropManager(this);
        claimManager = new ClaimManager(this);
        claimDataManager = new ClaimDataManager(this);
        repairManager = new RepairManager(this);

        // 작물 성장 버프 매니저 초기화
        cropGrowthManager = new CropGrowthManager(this);
        cropGrowthManager.init();

        // 청크 로더 초기화 및 저장된 청크 로드
        if (claimManager.isEnabled()) {
            claimChunkLoader = new ClaimChunkLoader(this);
            claimChunkLoader.loadAllChunks();

            // 스코어보드 매니저 초기화
            claimScoreboardManager = new ClaimScoreboardManager(this);
        }
        adminGuiListener = new AdminGuiListener(this);

        // 주식 스케줄러 시작
        stockManager.startScheduler();

        // 기존 NPC 엔티티 제거 후 재소환
        npcManager.removeAllTaggedNpcs();
        npcManager.respawnAllNpcs();

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

        // 은행 데이터 저장
        if (bankManager != null) {
            bankManager.saveAllData();
        }

        // 청크 로더 정리
        if (claimChunkLoader != null) {
            claimChunkLoader.unloadAllChunks();
        }

        // 작물 성장 버프 매니저 정리
        if (cropGrowthManager != null) {
            cropGrowthManager.shutdown();
        }

        // 스코어보드 매니저 정리
        if (claimScoreboardManager != null) {
            claimScoreboardManager.shutdown();
        }

        // GUI 자동 새로고침 태스크 모두 중지
        AutoRefreshGUI.stopAll();

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
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcDamageListener(this), this);
        getServer().getPluginManager().registerEvents(adminGuiListener, this);
        chatListener = new ChatListener(this, adminGuiListener);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(new BuffBlockListener(this), this);

        // 클레임 시스템 리스너 등록 (GP가 있을 때만)
        if (claimManager.isEnabled()) {
            getServer().getPluginManager().registerEvents(new FarmClaimListener(this, claimManager), this);
            getServer().getPluginManager()
                    .registerEvents(
                            new ClaimProtectionListener(this, claimManager, claimDataManager, cropGrowthManager), this);
            getServer().getPluginManager().registerEvents(new ClaimGUIListener(this), this);
            getServer().getPluginManager().registerEvents(new CropGrowthBuffListener(this), this);
            getLogger().info("농장 허가증 시스템이 활성화되었습니다.");
        }
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
        npcManager.reload();
        shopManager.reload();
        bankManager.reload();
        claimManager.reload();
        claimDataManager.reload();
        repairManager.reload();

        if (cropGrowthManager != null) {
            cropGrowthManager.reload();
        }
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

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public MiningDropManager getMiningDropManager() {
        return miningDropManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public ClaimDataManager getClaimDataManager() {
        return claimDataManager;
    }

    public RepairManager getRepairManager() {
        return repairManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public CropGrowthManager getCropGrowthManager() {
        return cropGrowthManager;
    }

    public ClaimChunkLoader getClaimChunkLoader() {
        return claimChunkLoader;
    }

    public ClaimScoreboardManager getClaimScoreboardManager() {
        return claimScoreboardManager;
    }
}
