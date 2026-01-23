package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 인챈트 관리자 메인 GUI - 인챈트 목록 및 관리 옵션
 */
public class EnchantAdminGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    public EnchantAdminGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, "§8[ §5인챈트 관리 §8]");

        // 배경
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // 상단 정보
        ItemStack infoItem = createItem(Material.ENCHANTED_BOOK, "§5[ 인챈트 관리 시스템 ]",
                List.of(
                        "",
                        "§7인챈트 옵션을 선택하여",
                        "§7설정을 변경할 수 있습니다.",
                        "",
                        "§e클릭§7: 상세 설정 열기"));
        inventory.setItem(4, infoItem);

        // 인챈트 목록 표시
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int slotIndex = 0;

        for (EnchantConfig enchant : plugin.getConfigManager().getEnchants().values()) {
            if (slotIndex >= slots.length)
                break;

            Material material = Material.getMaterial(enchant.getMaterial());
            if (material == null)
                material = Material.ENCHANTED_BOOK;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7ID: §f" + enchant.getId());
            lore.add("");
            lore.add("§7결과: §f" + enchant.getResultEnchantment() + " " + enchant.getResultLevel());
            lore.add("§7비용: §6" + String.format("%,.0f", enchant.getCostMoney()) + "원");
            lore.add("");
            lore.add("§a성공: §f" + enchant.getSuccessRate() + "%");
            lore.add("§e실패: §f" + enchant.getFailRate() + "%");
            lore.add("§c파괴: §f" + enchant.getDestroyRate() + "%");
            lore.add("");
            lore.add("§e클릭하여 설정 변경");

            ItemStack enchantItem = createItem(material, enchant.getDisplayName(), lore);
            inventory.setItem(slots[slotIndex++], enchantItem);
        }

        // 새 인챈트 추가 버튼
        ItemStack addEnchant = createItem(Material.EMERALD_BLOCK, "§a[ + 새 인챈트 추가 ]",
                List.of("", "§7클릭하여 새 인챈트 옵션을 추가합니다."));
        inventory.setItem(49, addEnchant);

        // 뒤로 가기
        ItemStack back = createItem(Material.ARROW, "§c[ 닫기 ]", null);
        inventory.setItem(53, back);
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
        createInventory();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
