package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.EnchantNameUtil;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 인챈트 종류 선택 GUI (1단계)
 * 마인크래프트의 모든 인챈트를 페이지네이션과 필터링으로 표시
 */
public class EnchantTypeSelectorGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    private int currentPage = 0;
    private String currentFilter = "ALL"; // 현재 필터 (ALL, WEAPON, ARMOR, TOOL, BOW, FISHING, TRIDENT, OTHER)
    private List<Enchantment> filteredEnchantments;

    private static final int ITEMS_PER_PAGE = 36; // 9-44 슬롯 사용
    private static final int[] CONTENT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    public EnchantTypeSelectorGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.filteredEnchantments = getAllEnchantments();
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, ItemUtil.parse("§8[ §5인챈트 선택 §8]"));
        updateInventory();
    }

    private void updateInventory() {
        // 배경 초기화
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // === 상단 필터 버튼 (슬롯 0-8) ===
        inventory.setItem(0, createFilterItem("ALL", Material.NETHER_STAR, "§f[ 전체 ]"));
        inventory.setItem(1, createFilterItem("WEAPON", Material.DIAMOND_SWORD, "§c[ 무기 ]"));
        inventory.setItem(2, createFilterItem("ARMOR", Material.DIAMOND_CHESTPLATE, "§9[ 갑옷 ]"));
        inventory.setItem(3, createFilterItem("TOOL", Material.DIAMOND_PICKAXE, "§e[ 도구 ]"));
        inventory.setItem(4, createItem(Material.ENCHANTING_TABLE, "§5[ 인챈트 선택 ]",
                List.of("", "§7적용할 인챈트 종류를", "§7선택하세요.", "", "§7필터: " + getFilterDisplayName(currentFilter))));
        inventory.setItem(5, createFilterItem("BOW", Material.BOW, "§6[ 활 ]"));
        inventory.setItem(6, createFilterItem("FISHING", Material.FISHING_ROD, "§3[ 낚싯대 ]"));
        inventory.setItem(7, createFilterItem("TRIDENT", Material.TRIDENT, "§b[ 삼지창 ]"));
        inventory.setItem(8, createFilterItem("OTHER", Material.BOOK, "§7[ 기타 ]"));

        // === 인챈트 목록 (슬롯 9-44) ===
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredEnchantments.size());

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int enchantIndex = startIndex + i;
            if (enchantIndex < endIndex) {
                Enchantment enchant = filteredEnchantments.get(enchantIndex);
                inventory.setItem(CONTENT_SLOTS[i], createEnchantItem(enchant));
            }
        }

        // === 하단 네비게이션 (슬롯 45-53) ===

        // 이전 페이지
        if (currentPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "§e[ ◀ 이전 페이지 ]",
                    List.of("", "§7페이지 " + currentPage + " / " + getTotalPages())));
        }

        // 페이지 표시
        inventory.setItem(49,
                createItem(Material.PAPER, "§f[ 페이지 " + (currentPage + 1) + " / " + getTotalPages() + " ]",
                        List.of("", "§7총 " + filteredEnchantments.size() + "개의 인챈트")));

        // 다음 페이지
        if (currentPage < getTotalPages() - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "§e[ 다음 페이지 ▶ ]",
                    List.of("", "§7페이지 " + (currentPage + 2) + " / " + getTotalPages())));
        }

        // 뒤로 가기
        inventory.setItem(48, createItem(Material.BARRIER, "§c[ 뒤로 가기 ]",
                List.of("", "§7인챈트 관리 메뉴로 돌아갑니다.")));
    }

    private ItemStack createFilterItem(String filter, Material material, String name) {
        boolean isSelected = filter.equals(currentFilter);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(isSelected ? "§a✔ 선택됨" : "§7클릭하여 필터링");

        ItemStack item = createItem(material, name, lore);
        if (isSelected) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createEnchantItem(Enchantment enchant) {
        String enchantName = formatEnchantmentName(enchant);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7키: §f" + enchant.getKey().getKey());
        lore.add("§7최대 레벨: §e" + enchant.getMaxLevel());
        lore.add("");
        lore.add("§e클릭하여 선택");

        return createItem(Material.ENCHANTED_BOOK, "§b" + enchantName, lore);
    }

    private String formatEnchantmentName(Enchantment enchant) {
        return EnchantNameUtil.getKoreanName(enchant);
    }

    private String getFilterDisplayName(String filter) {
        return switch (filter) {
            case "ALL" -> "§f전체";
            case "WEAPON" -> "§c무기";
            case "ARMOR" -> "§9갑옷";
            case "TOOL" -> "§e도구";
            case "BOW" -> "§6활";
            case "FISHING" -> "§3낚싯대";
            case "TRIDENT" -> "§b삼지창";
            case "OTHER" -> "§7기타";
            default -> "§f전체";
        };
    }

    private List<Enchantment> getAllEnchantments() {
        return StreamSupport.stream(Bukkit.getRegistry(Enchantment.class).spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * 필터 설정 및 목록 갱신
     */
    public void setFilter(String filter) {
        this.currentFilter = filter;
        this.currentPage = 0;

        if ("ALL".equals(filter)) {
            filteredEnchantments = getAllEnchantments();
        } else {
            filteredEnchantments = getAllEnchantments().stream()
                    .filter(e -> matchesCategory(e, filter))
                    .collect(Collectors.toList());
        }

        updateInventory();
    }

    private boolean matchesCategory(Enchantment enchant, String category) {
        // 인챈트의 대상에 따라 카테고리 분류
        String key = enchant.getKey().getKey();

        return switch (category) {
            case "WEAPON" -> key.contains("sharpness") || key.contains("smite") ||
                    key.contains("bane_of_arthropods") || key.contains("knockback") ||
                    key.contains("fire_aspect") || key.contains("sweeping") ||
                    key.contains("looting") || key.contains("impaling");
            case "ARMOR" -> key.contains("protection") || key.contains("thorns") ||
                    key.contains("respiration") || key.contains("aqua_affinity") ||
                    key.contains("feather_falling") || key.contains("depth_strider") ||
                    key.contains("frost_walker") || key.contains("soul_speed") ||
                    key.contains("swift_sneak");
            case "TOOL" -> key.contains("efficiency") || key.contains("silk_touch") ||
                    key.contains("fortune") || key.contains("unbreaking");
            case "BOW" -> key.contains("power") || key.contains("punch") ||
                    key.contains("flame") || key.contains("infinity") ||
                    key.contains("multishot") || key.contains("quick_charge") ||
                    key.contains("piercing");
            case "FISHING" -> key.contains("luck_of_the_sea") || key.contains("lure");
            case "TRIDENT" -> key.contains("loyalty") || key.contains("riptide") ||
                    key.contains("channeling") || key.contains("impaling");
            case "OTHER" -> key.contains("mending") || key.contains("vanishing") ||
                    key.contains("binding");
            default -> true;
        };
    }

    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
            updateInventory();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateInventory();
        }
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredEnchantments.size() / ITEMS_PER_PAGE));
    }

    /**
     * 클릭한 슬롯에서 인챈트 가져오기
     */
    public Enchantment getEnchantmentAtSlot(int slot) {
        int slotIndex = -1;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex == -1)
            return null;

        int enchantIndex = currentPage * ITEMS_PER_PAGE + slotIndex;
        if (enchantIndex < filteredEnchantments.size()) {
            return filteredEnchantments.get(enchantIndex);
        }
        return null;
    }

    /**
     * 슬롯이 인챈트 컨텐츠 영역인지 확인
     */
    public boolean isContentSlot(int slot) {
        for (int s : CONTENT_SLOTS) {
            if (s == slot)
                return true;
        }
        return false;
    }

    public String getCurrentFilter() {
        return currentFilter;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
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
