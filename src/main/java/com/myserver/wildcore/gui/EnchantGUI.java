package com.myserver.wildcore.gui;

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
 * 인챈트(강화소) GUI
 */
public class EnchantGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    public EnchantGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    /**
     * 인벤토리 생성
     */
    private void createInventory() {
        String title = plugin.getConfigManager().getEnchantGuiTitle();
        int size = plugin.getConfigManager().getEnchantGuiSize();

        inventory = Bukkit.createInventory(this, size, title);

        // 배경 유리판 채우기
        ItemStack background = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, background);
        }

        // 현재 손에 든 아이템 정보 표시
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack heldItemInfo = createHeldItemInfo(heldItem);
        inventory.setItem(4, heldItemInfo);

        // 인챈트 옵션 배치
        List<EnchantConfig> availableEnchants = plugin.getEnchantManager().getAvailableEnchants(heldItem);

        if (availableEnchants.isEmpty()) {
            // 적용 가능한 인챈트가 없음
            ItemStack noEnchant = createItem(Material.BARRIER, "§c적용 가능한 강화가 없습니다",
                    List.of("", "§7손에 강화 가능한 아이템을 들고", "§7다시 열어주세요."));
            inventory.setItem(13, noEnchant);
        } else {
            // 가능한 인챈트 표시
            for (EnchantConfig enchant : availableEnchants) {
                ItemStack enchantItem = createEnchantItem(enchant);
                inventory.setItem(enchant.getSlot(), enchantItem);
            }
        }
    }

    /**
     * 손에 든 아이템 정보 생성
     */
    private ItemStack createHeldItemInfo(ItemStack heldItem) {
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return createItem(Material.IRON_SWORD, "§7[ 대상 아이템 없음 ]",
                    List.of("", "§c손에 아이템을 들고 있지 않습니다.", "§7강화할 아이템을 손에 들고", "§7다시 GUI를 열어주세요."));
        }

        ItemStack displayItem = heldItem.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§a▶ 강화 대상 아이템");
            lore.add("§7아래에서 원하는 강화를 선택하세요.");
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    /**
     * 인챈트 아이템 생성
     */
    private ItemStack createEnchantItem(EnchantConfig enchant) {
        Material material = Material.getMaterial(enchant.getMaterial());
        if (material == null)
            material = Material.ENCHANTED_BOOK;

        List<String> lore = new ArrayList<>(enchant.getLore());
        lore.add("");
        lore.add("§e클릭하여 강화 시도!");

        return createItem(material, enchant.getDisplayName(), lore);
    }

    /**
     * 아이템 생성 헬퍼
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * GUI 열기
     */
    public void open() {
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
