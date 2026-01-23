package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemGroupUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
 * 인챈트 적용 대상 설정 GUI (3단계)
 * 그룹 기반 및 개별 아이템 타겟 설정
 */
public class EnchantTargetGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final EnchantBuilderGUI parentBuilder;
    private Inventory inventory;

    // 선택된 그룹과 아이템
    private Set<String> selectedGroups;
    private Set<String> selectedItems;

    public EnchantTargetGUI(WildCore plugin, Player player, EnchantBuilderGUI parentBuilder) {
        this.plugin = plugin;
        this.player = player;
        this.parentBuilder = parentBuilder;
        this.selectedGroups = new HashSet<>(parentBuilder.getTargetGroups());
        this.selectedItems = new HashSet<>(parentBuilder.getTargetWhitelist());
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, "§8[ §5적용 대상 설정 §8]");
        updateInventory();
    }

    private void updateInventory() {
        // 배경 초기화
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // === 헤더 (슬롯 4) ===
        inventory.setItem(4, createItem(Material.TARGET, "§5[ 적용 대상 설정 ]",
                List.of("", "§7인챈트가 적용될 아이템을", "§7설정합니다.")));

        // === 그룹 추가 버튼 (슬롯 10-16) ===
        inventory.setItem(10, createGroupItem("WEAPON"));
        inventory.setItem(11, createGroupItem("ARMOR"));
        inventory.setItem(12, createGroupItem("TOOL"));
        inventory.setItem(13, createGroupItem("BOW"));
        inventory.setItem(14, createGroupItem("FISHING"));
        inventory.setItem(15, createGroupItem("TRIDENT"));
        inventory.setItem(16, createGroupItem("ALL"));

        // === 손에 든 아이템 추가 (슬롯 22) ===
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && handItem.getType() != Material.AIR) {
            String itemType = handItem.getType().name();
            boolean isSelected = selectedItems.contains(itemType);
            inventory.setItem(22, createItem(handItem.getType(),
                    (isSelected ? "§c" : "§a") + "[ 손에 든 아이템 ]",
                    List.of("",
                            "§7아이템: §f" + itemType,
                            "",
                            isSelected ? "§c✗ 클릭하여 제거" : "§a✔ 클릭하여 추가")));
        } else {
            inventory.setItem(22, createItem(Material.BARRIER, "§7[ 손에 든 아이템 없음 ]",
                    List.of("", "§7손에 아이템을 들고", "§7클릭하여 추가하세요.")));
        }

        // === 현재 선택 상태 표시 (슬롯 28-34) ===
        inventory.setItem(28, createItem(Material.PAPER, "§f[ 선택된 그룹 ]",
                List.of("", formatSelectedGroups())));
        inventory.setItem(30, createItem(Material.PAPER, "§f[ 선택된 아이템 ]",
                List.of("", formatSelectedItems())));

        // === 현재 화이트리스트 아이콘 표시 (슬롯 36-44) ===
        int slot = 36;
        for (String group : selectedGroups) {
            if (slot > 44)
                break;
            inventory.setItem(slot++, createItem(ItemGroupUtil.getGroupIcon(group),
                    ItemGroupUtil.getGroupDisplayName(group) + " §7(그룹)",
                    List.of("", "§c클릭하여 제거")));
        }

        // === 액션 버튼 (슬롯 45-53) ===
        inventory.setItem(45, createItem(Material.ARROW, "§7[ 뒤로 가기 ]",
                List.of("", "§7인챈트 설정으로 돌아갑니다.")));
        inventory.setItem(49, createItem(Material.TNT, "§c[ 전체 초기화 ]",
                List.of("", "§7모든 선택을 초기화합니다.")));
        inventory.setItem(53, createItem(Material.EMERALD, "§a[ 완료 ]",
                List.of("", "§7설정을 저장하고 돌아갑니다.")));
    }

    private ItemStack createGroupItem(String group) {
        boolean isSelected = selectedGroups.contains(group);
        Material icon = ItemGroupUtil.getGroupIcon(group);
        String displayName = ItemGroupUtil.getGroupDisplayName(group);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7포함 아이템:");

        // 그룹에 포함된 아이템 일부 표시
        List<Material> materials = ItemGroupUtil.getGroupMaterials(group);
        int count = 0;
        for (Material m : materials) {
            if (count >= 3) {
                lore.add("§7  ... 외 " + (materials.size() - 3) + "개");
                break;
            }
            lore.add("§7  - §f" + m.name());
            count++;
        }
        if ("ALL".equals(group)) {
            lore.clear();
            lore.add("");
            lore.add("§7모든 아이템에 적용됩니다.");
        }

        lore.add("");
        lore.add(isSelected ? "§a✔ 선택됨 - 클릭하여 제거" : "§7클릭하여 추가");

        ItemStack item = createItem(icon, displayName, lore);
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

    private String formatSelectedGroups() {
        if (selectedGroups.isEmpty()) {
            return "§7없음";
        }
        StringBuilder sb = new StringBuilder();
        for (String group : selectedGroups) {
            sb.append("§7- ").append(ItemGroupUtil.getGroupDisplayName(group)).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatSelectedItems() {
        if (selectedItems.isEmpty()) {
            return "§7없음";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String item : selectedItems) {
            if (count >= 5) {
                sb.append("§7... 외 ").append(selectedItems.size() - 5).append("개");
                break;
            }
            sb.append("§7- §f").append(item).append("\n");
            count++;
        }
        return sb.toString().trim();
    }

    /**
     * 그룹 슬롯인지 확인하고 그룹 이름 반환
     */
    public String getGroupAtSlot(int slot) {
        return switch (slot) {
            case 10 -> "WEAPON";
            case 11 -> "ARMOR";
            case 12 -> "TOOL";
            case 13 -> "BOW";
            case 14 -> "FISHING";
            case 15 -> "TRIDENT";
            case 16 -> "ALL";
            default -> null;
        };
    }

    /**
     * 그룹 토글
     */
    public void toggleGroup(String group) {
        if (selectedGroups.contains(group)) {
            selectedGroups.remove(group);
        } else {
            selectedGroups.add(group);
        }
        updateInventory();
    }

    /**
     * 손에 든 아이템 토글
     */
    public void toggleHandItem() {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            return;
        }

        String itemType = handItem.getType().name();
        if (selectedItems.contains(itemType)) {
            selectedItems.remove(itemType);
        } else {
            selectedItems.add(itemType);
        }
        updateInventory();
    }

    /**
     * 전체 초기화
     */
    public void clearAll() {
        selectedGroups.clear();
        selectedItems.clear();
        updateInventory();
    }

    /**
     * 설정을 부모 빌더에 적용
     */
    public void applyToBuilder() {
        parentBuilder.setTargetGroups(selectedGroups);
        parentBuilder.setTargetWhitelist(selectedItems);
    }

    public EnchantBuilderGUI getParentBuilder() {
        return parentBuilder;
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
