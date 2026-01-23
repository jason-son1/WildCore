package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemGroupUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 인챈트 빌더 GUI (2단계)
 * 선택한 인챈트의 레벨, 옵션, 타겟 등을 설정
 */
public class EnchantBuilderGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    // 빌더 상태
    private final Enchantment selectedEnchant;
    private int level = 1;
    private boolean unsafeMode = false;
    private boolean ignoreConflicts = false;
    private Set<String> targetGroups = new HashSet<>();
    private Set<String> targetWhitelist = new HashSet<>();
    private double successRate = 50.0;
    private double failRate = 40.0;
    private double destroyRate = 10.0;
    private double costMoney = 1000.0;
    private List<String> costItems = new ArrayList<>();

    public EnchantBuilderGUI(WildCore plugin, Player player, Enchantment enchant) {
        this.plugin = plugin;
        this.player = player;
        this.selectedEnchant = enchant;
        this.targetGroups.add("WEAPON"); // 기본값
        this.costItems.add("DIAMOND:1"); // 기본 재료
        createInventory();
    }

    private void createInventory() {
        String enchantName = formatEnchantmentName(selectedEnchant);
        inventory = Bukkit.createInventory(this, 54, "§8[ §5" + enchantName + " 설정 §8]");
        updateInventory();
    }

    private void updateInventory() {
        // 배경 초기화
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // === 헤더 (슬롯 4) ===
        inventory.setItem(4, createItem(Material.ENCHANTED_BOOK, "§5[ " + formatEnchantmentName(selectedEnchant) + " ]",
                List.of("",
                        "§7키: §f" + selectedEnchant.getKey().getKey(),
                        "§7기본 최대 레벨: §e" + selectedEnchant.getMaxLevel(),
                        "",
                        "§7설정 레벨: §b" + level)));

        // === 레벨 설정 (슬롯 10-16) ===
        inventory.setItem(10, createItem(Material.RED_DYE, "§c[ -100 ]", List.of("", "§7레벨 100 감소")));
        inventory.setItem(11, createItem(Material.RED_DYE, "§c[ -10 ]", List.of("", "§7레벨 10 감소")));
        inventory.setItem(12, createItem(Material.RED_DYE, "§c[ -1 ]", List.of("", "§7레벨 1 감소")));
        inventory.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, "§b[ 레벨: " + level + " ]",
                List.of("", "§7현재 레벨: §b" + level, "", "§e클릭하여 직접 입력")));
        inventory.setItem(14, createItem(Material.LIME_DYE, "§a[ +1 ]", List.of("", "§7레벨 1 증가")));
        inventory.setItem(15, createItem(Material.LIME_DYE, "§a[ +10 ]", List.of("", "§7레벨 10 증가")));
        inventory.setItem(16, createItem(Material.LIME_DYE, "§a[ +100 ]", List.of("", "§7레벨 100 증가")));

        // === 옵션 토글 (슬롯 20-24) ===
        inventory.setItem(20, createToggleItem("§6[ 바닐라 제한 무시 ]", unsafeMode,
                List.of("", "§7활성화 시:", "§7- 레벨 제한 무시 (Lv.∞ 가능)",
                        "§7- 아이템 타입 제한 무시", "§7- 상충 인챈트 적용 가능")));
        inventory.setItem(22, createToggleItem("§e[ 충돌 인챈트 무시 ]", ignoreConflicts,
                List.of("", "§7활성화 시:", "§7- 보호/화피 동시 적용 가능",
                        "§7- 행운/섬세한손길 동시 적용 가능")));
        inventory.setItem(24, createItem(Material.CHEST, "§a[ 적용 대상 설정 ]",
                List.of("", "§7현재 대상:", formatTargets(), "", "§e클릭하여 설정")));

        // === 확률 설정 (슬롯 28-34) ===
        inventory.setItem(28, createRateItem(Material.LIME_CONCRETE, "§a[ 성공 확률 ]", successRate, "성공"));
        inventory.setItem(29, createItem(Material.LIME_DYE, "§a[ 성공 +5% ]", null));
        inventory.setItem(30, createItem(Material.RED_DYE, "§c[ 성공 -5% ]", null));

        inventory.setItem(31, createRateItem(Material.YELLOW_CONCRETE, "§e[ 실패 확률 ]", failRate, "실패"));
        inventory.setItem(32, createItem(Material.YELLOW_DYE, "§e[ 실패 +5% ]", null));
        inventory.setItem(33, createItem(Material.ORANGE_DYE, "§6[ 실패 -5% ]", null));

        inventory.setItem(34, createRateItem(Material.RED_CONCRETE, "§c[ 파괴 확률 ]", destroyRate, "파괴"));

        // 확률 합계 표시
        double total = successRate + failRate + destroyRate;
        String totalColor = Math.abs(total - 100.0) < 0.01 ? "§a" : "§c";
        inventory.setItem(22 + 9, createItem(Material.PAPER, "§f[ 확률 합계 ]",
                List.of("", totalColor + "합계: " + String.format("%.1f", total) + "%",
                        "", "§7확률 합계는 100%여야 합니다.")));

        // === 비용 설정 (슬롯 37-41) ===
        inventory.setItem(38, createItem(Material.GOLD_INGOT, "§6[ 비용: " + String.format("%,.0f", costMoney) + "원 ]",
                List.of("", "§7클릭하여 금액 설정")));
        inventory.setItem(40, createItem(Material.DIAMOND, "§b[ 재료 아이템 ]",
                List.of("", "§7현재 재료:", formatCostItems(), "", "§e클릭하여 설정")));

        // === 액션 버튼 (슬롯 45-53) ===
        inventory.setItem(45, createItem(Material.ARROW, "§7[ 뒤로 가기 ]",
                List.of("", "§7인챈트 선택으로 돌아갑니다.")));
        inventory.setItem(49, createItem(Material.EMERALD_BLOCK, "§a[ 생성 및 저장 ]",
                List.of("", "§7이 설정으로 인챈트를 생성합니다.")));
        inventory.setItem(53, createItem(Material.BARRIER, "§c[ 취소 ]",
                List.of("", "§7변경사항을 버리고 닫습니다.")));
    }

    private ItemStack createToggleItem(String name, boolean enabled, List<String> baseLore) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        List<String> lore = new ArrayList<>(baseLore);
        lore.add("");
        lore.add(enabled ? "§a✔ 활성화됨" : "§7✗ 비활성화됨");
        lore.add("§e클릭하여 전환");
        return createItem(material, name, lore);
    }

    private ItemStack createRateItem(Material material, String name, double rate, String type) {
        return createItem(material, name, List.of("", "§7현재: §f" + String.format("%.1f", rate) + "%"));
    }

    private String formatTargets() {
        StringBuilder sb = new StringBuilder();
        for (String group : targetGroups) {
            sb.append("§7- ").append(ItemGroupUtil.getGroupDisplayName(group)).append("\n");
        }
        if (sb.length() == 0) {
            return "§7- 없음";
        }
        return sb.toString().trim();
    }

    private String formatCostItems() {
        StringBuilder sb = new StringBuilder();
        for (String item : costItems) {
            sb.append("§7- §f").append(item).append("\n");
        }
        if (sb.length() == 0) {
            return "§7- 없음";
        }
        return sb.toString().trim();
    }

    private String formatEnchantmentName(Enchantment enchant) {
        String key = enchant.getKey().getKey();
        StringBuilder result = new StringBuilder();
        for (String word : key.split("_")) {
            if (result.length() > 0)
                result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    // === 조작 메서드들 ===

    public void adjustLevel(int delta) {
        level = Math.max(1, level + delta);
        updateInventory();
    }

    public void setLevel(int newLevel) {
        this.level = Math.max(1, newLevel);
        updateInventory();
    }

    public void toggleUnsafeMode() {
        unsafeMode = !unsafeMode;
        updateInventory();
    }

    public void toggleIgnoreConflicts() {
        ignoreConflicts = !ignoreConflicts;
        updateInventory();
    }

    public void adjustSuccessRate(double delta) {
        successRate = Math.max(0, Math.min(100, successRate + delta));
        updateInventory();
    }

    public void adjustFailRate(double delta) {
        failRate = Math.max(0, Math.min(100, failRate + delta));
        updateInventory();
    }

    public void adjustDestroyRate(double delta) {
        destroyRate = Math.max(0, Math.min(100, destroyRate + delta));
        updateInventory();
    }

    public void setCostMoney(double cost) {
        this.costMoney = Math.max(0, cost);
        updateInventory();
    }

    public void setCostItems(String itemsStr) {
        this.costItems.clear();
        for (String item : itemsStr.split(",")) {
            this.costItems.add(item.trim());
        }
        updateInventory();
    }

    public void addTargetGroup(String group) {
        targetGroups.add(group);
        updateInventory();
    }

    public void removeTargetGroup(String group) {
        targetGroups.remove(group);
        updateInventory();
    }

    public void setTargetGroups(Set<String> groups) {
        this.targetGroups = groups;
        updateInventory();
    }

    public void setTargetWhitelist(Set<String> whitelist) {
        this.targetWhitelist = whitelist;
        updateInventory();
    }

    /**
     * 현재 설정으로 인챈트 생성
     */
    public boolean saveEnchant() {
        String enchantId = selectedEnchant.getKey().getKey() + "_" + level;

        // ConfigManager를 통해 새 인챈트 생성
        if (!plugin.getConfigManager().createNewEnchant(enchantId)) {
            return false;
        }

        // 추가 설정 적용 (ConfigManager에 메서드 추가 필요)
        String path = "tiers." + enchantId;
        plugin.getConfigManager().getEnchantsConfig().set(path + ".display_name",
                "&b[ " + formatEnchantmentName(selectedEnchant) + " " + level + " ]");
        plugin.getConfigManager().getEnchantsConfig().set(path + ".result.enchantment",
                selectedEnchant.getKey().getKey());
        plugin.getConfigManager().getEnchantsConfig().set(path + ".result.level", level);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".unsafe_mode", unsafeMode);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".ignore_conflicts", ignoreConflicts);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".target_groups", new ArrayList<>(targetGroups));
        plugin.getConfigManager().getEnchantsConfig().set(path + ".probability.success", successRate);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".probability.fail", failRate);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".probability.destroy", destroyRate);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".cost.money", costMoney);
        plugin.getConfigManager().getEnchantsConfig().set(path + ".cost.items", costItems);

        // 저장
        plugin.getConfigManager().saveEnchantConfig(enchantId);

        return true;
    }

    // === Getter ===

    public Enchantment getSelectedEnchant() {
        return selectedEnchant;
    }

    public int getLevel() {
        return level;
    }

    public boolean isUnsafeMode() {
        return unsafeMode;
    }

    public boolean isIgnoreConflicts() {
        return ignoreConflicts;
    }

    public Set<String> getTargetGroups() {
        return targetGroups;
    }

    public Set<String> getTargetWhitelist() {
        return targetWhitelist;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void refresh() {
        updateInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
