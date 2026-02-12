package com.myserver.wildcore.gui.claim;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.claim.ClaimFlags;
import com.myserver.wildcore.claim.ClaimFlags.Category;
import com.myserver.wildcore.managers.ClaimDataManager;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * 사유지 설정 GUI
 * 카테고리별 페이지 시스템으로 다양한 플래그를 관리합니다.
 */
public class ClaimSettingsGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Claim claim;
    private Inventory inventory;
    private Category currentCategory;

    // GUI 타이틀
    private static final String TITLE_PREFIX = "§8[ §6⚙️ 사유지 설정 - ";

    // 슬롯 배치
    private static final int[] CATEGORY_SLOTS = { 10, 12, 14, 16 }; // 카테고리 탭 위치
    private static final int[] FLAG_SLOTS = {
            19, 20, 21, 22, 23, 24, 25, // 3번째 줄
            28, 29, 30, 31, 32, 33, 34, // 4번째 줄
            37, 38, 39, 40, 41, 42, 43 // 5번째 줄 (최대 21개 플래그 지원)
    };

    private static final int SLOT_BACK = 45; // 뒤로가기
    private static final int SLOT_TOGGLE_ALL = 53; // 모두 켜기/끄기

    public ClaimSettingsGUI(WildCore plugin, Player player, Claim claim) {
        this.plugin = plugin;
        this.player = player;
        this.claim = claim;
        this.currentCategory = Category.GENERAL; // 기본 카테고리
        createInventory();
    }

    public ClaimSettingsGUI(WildCore plugin, Player player, Claim claim, Category category) {
        this.plugin = plugin;
        this.player = player;
        this.claim = claim;
        this.currentCategory = category;
        createInventory();
    }

    private void createInventory() {
        String title = TITLE_PREFIX + currentCategory.getDisplayName() + " §8]";
        inventory = Bukkit.createInventory(this, 54, title);

        // 테두리 채우기
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // 상단 구분선
        ItemStack divider = createDividerItem();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, divider);
        }

        // 카테고리 탭 배치
        setupCategoryTabs();

        // 플래그 아이템 배치
        setupFlagItems();

        // 하단 버튼
        inventory.setItem(SLOT_BACK, createBackButton());
        inventory.setItem(SLOT_TOGGLE_ALL, createToggleAllButton());
    }

    /**
     * 카테고리 탭 설정
     */
    private void setupCategoryTabs() {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            Category cat = categories[i];
            boolean isSelected = cat == currentCategory;
            inventory.setItem(CATEGORY_SLOTS[i], createCategoryTab(cat, isSelected));
        }
    }

    /**
     * 플래그 아이템 설정
     */
    private void setupFlagItems() {
        List<ClaimFlags> flags = ClaimFlags.getByCategory(currentCategory);
        ClaimDataManager dataManager = plugin.getClaimDataManager();

        for (int i = 0; i < flags.size() && i < FLAG_SLOTS.length; i++) {
            ClaimFlags flag = flags.get(i);
            boolean enabled = dataManager.getClaimFlag(claim.getID(), flag.getKey(), flag.getDefaultValue());
            inventory.setItem(FLAG_SLOTS[i], createFlagItem(flag, enabled));
        }
    }

    /**
     * 카테고리 탭 아이템 생성
     */
    private ItemStack createCategoryTab(Category category, boolean isSelected) {
        // 선택 여부와 관계없이 카테고리 아이콘 사용
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();

        String selectedIndicator = isSelected ? " §a▶" : "";
        meta.setDisplayName(category.getPrefix() + " " + category.getDisplayName() + selectedIndicator);

        List<String> lore = new ArrayList<>();
        lore.add("");

        // 카테고리별 플래그 개수 표시
        List<ClaimFlags> flags = ClaimFlags.getByCategory(category);
        ClaimDataManager dataManager = plugin.getClaimDataManager();
        int enabledCount = 0;
        for (ClaimFlags flag : flags) {
            if (dataManager.getClaimFlag(claim.getID(), flag.getKey(), flag.getDefaultValue())) {
                enabledCount++;
            }
        }

        lore.add("§7설정: §f" + enabledCount + "/" + flags.size() + " §a활성화됨");
        lore.add("");

        if (isSelected) {
            lore.add("§e현재 선택됨");
            // 선택된 탭은 인챈트 효과로 강조
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add("§e클릭하여 이동");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 플래그 아이템 생성
     */
    private ItemStack createFlagItem(ClaimFlags flag, boolean enabled) {
        // 활성화 상태에 따라 아이콘 변경
        Material material = enabled ? flag.getIcon() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String statusColor = enabled ? "§a" : "§c";
        String statusText = enabled ? "[켜짐]" : "[꺼짐]";
        meta.setDisplayName(currentCategory.getPrefix() + " " + flag.getDisplayName() + " " + statusColor + statusText);

        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : flag.getDescription()) {
            lore.add("§7" + line);
        }
        lore.add("");
        lore.add("§7상태: " + statusColor + (enabled ? "활성화" : "비활성화"));
        lore.add("");
        lore.add("§e클릭하여 토글");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 채움 아이템 생성
     */
    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 구분선 아이템 생성
     */
    private ItemStack createDividerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 뒤로가기 버튼 생성
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f◀ 뒤로가기");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7메인 관리 화면으로 돌아갑니다.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 모두 켜기/끄기 버튼 생성
     */
    private ItemStack createToggleAllButton() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6⚡ 전체 토글");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7현재 카테고리의 모든 설정을");
        lore.add("§7한 번에 변경합니다.");
        lore.add("");
        lore.add("§e좌클릭: 모두 켜기");
        lore.add("§e우클릭: 모두 끄기");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 카테고리 탭 클릭 처리
     */
    public boolean handleCategoryClick(int slot) {
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) {
                Category[] categories = Category.values();
                if (i < categories.length) {
                    Category newCategory = categories[i];

                    // 현재 카테고리와 다른 경우에만 이동
                    if (newCategory != currentCategory) {
                        // 새 카테고리로 GUI 열기
                        new ClaimSettingsGUI(plugin, player, claim, newCategory).open();
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 플래그 슬롯에서 플래그를 가져옵니다
     */
    public ClaimFlags getFlagAtSlot(int slot) {
        List<ClaimFlags> flags = ClaimFlags.getByCategory(currentCategory);
        for (int i = 0; i < FLAG_SLOTS.length && i < flags.size(); i++) {
            if (FLAG_SLOTS[i] == slot) {
                return flags.get(i);
            }
        }
        return null;
    }

    /**
     * 플래그 토글 처리
     */
    public void toggleFlag(int slot) {
        ClaimFlags flag = getFlagAtSlot(slot);
        if (flag == null)
            return;

        ClaimDataManager dataManager = plugin.getClaimDataManager();
        boolean current = dataManager.getClaimFlag(claim.getID(), flag.getKey(), flag.getDefaultValue());
        dataManager.setClaimFlag(claim.getID(), flag.getKey(), !current);

        // GUI 새로고침
        refresh();

        // 알림 메시지
        boolean newValue = !current;
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7" + flag.getDisplayName() + " 설정이 " + (newValue ? "§a켜짐" : "§c꺼짐") + "§7으로 변경되었습니다.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, newValue ? 1.2f : 0.8f);
    }

    /**
     * 모든 플래그 토글 (켜기/끄기)
     */
    public void toggleAllFlags(boolean enable) {
        List<ClaimFlags> flags = ClaimFlags.getByCategory(currentCategory);
        ClaimDataManager dataManager = plugin.getClaimDataManager();

        for (ClaimFlags flag : flags) {
            dataManager.setClaimFlag(claim.getID(), flag.getKey(), enable);
        }

        refresh();

        String action = enable ? "§a켜짐" : "§c꺼짐";
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7" + currentCategory.getDisplayName() + "의 모든 설정이 " + action + "§7으로 변경되었습니다.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, enable ? 1.5f : 0.5f);
    }

    /**
     * GUI 새로고침
     */
    public void refresh() {
        setupCategoryTabs();
        setupFlagItems();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public boolean isBackSlot(int slot) {
        return slot == SLOT_BACK;
    }

    public boolean isToggleAllSlot(int slot) {
        return slot == SLOT_TOGGLE_ALL;
    }

    public boolean isCategorySlot(int slot) {
        for (int categorySlot : CATEGORY_SLOTS) {
            if (categorySlot == slot)
                return true;
        }
        return false;
    }

    public boolean isFlagSlot(int slot) {
        return getFlagAtSlot(slot) != null;
    }

    public Claim getClaim() {
        return claim;
    }

    public Category getCurrentCategory() {
        return currentCategory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
