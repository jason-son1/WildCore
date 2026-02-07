package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemUtil;
import net.kyori.adventure.text.Component;
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
 * 인챈트/수리 선택 GUI
 * NPC 클릭 또는 명령어 사용 시 표시되는 중간 선택 화면
 */
public class EnchantSelectGUI implements InventoryHolder {

    private static final int GUI_SIZE = 27;
    private static final int SLOT_ENCHANT = 11;
    private static final int SLOT_REPAIR = 15;
    private static final int SLOT_HELD_ITEM = 22;

    private final WildCore plugin;
    private final Player player;
    private final ItemStack heldItem;
    private Inventory inventory;

    public EnchantSelectGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.heldItem = player.getInventory().getItemInMainHand();
    }

    public void open() {
        createInventory();
        player.openInventory(inventory);
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, GUI_SIZE, ItemUtil.parse("§8[ §d강화소 §8]"));

        // 배경 채우기
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, background);
        }

        // 인챈트 버튼
        ItemStack enchantButton = createEnchantButton();
        inventory.setItem(SLOT_ENCHANT, enchantButton);

        // 수리 버튼
        ItemStack repairButton = createRepairButton();
        inventory.setItem(SLOT_REPAIR, repairButton);

        // 현재 아이템 표시
        ItemStack heldItemDisplay = createHeldItemDisplay();
        inventory.setItem(SLOT_HELD_ITEM, heldItemDisplay);
    }

    private ItemStack createEnchantButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7손에 든 아이템에");
        lore.add("§7인챈트를 부여합니다.");
        lore.add("");

        int enchantCount = plugin.getEnchantManager().getAvailableEnchants(heldItem).size();
        if (enchantCount > 0) {
            lore.add("§a▶ 적용 가능한 인챈트: §e" + enchantCount + "개");
        } else {
            lore.add("§c▶ 적용 가능한 인챈트 없음");
        }
        lore.add("");
        lore.add("§e클릭하여 인챈트 GUI 열기");

        return createItem(Material.ENCHANTED_BOOK, "§d[ 인챈트 ]", lore);
    }

    private ItemStack createRepairButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7손에 든 아이템의");
        lore.add("§7내구도를 수리합니다.");
        lore.add("");

        if (heldItem != null && heldItem.getType() != Material.AIR) {
            if (plugin.getRepairManager().hasDurability(heldItem)) {
                double ratio = plugin.getRepairManager().getDurabilityRatio(heldItem);
                int percent = (int) (ratio * 100);
                String durabilityColor = percent > 50 ? "§a" : (percent > 25 ? "§e" : "§c");
                lore.add("§7현재 내구도: " + durabilityColor + percent + "%");

                int repairCount = plugin.getRepairManager().getAvailableRepairs(heldItem).size();
                lore.add("§a▶ 수리 옵션: §e" + repairCount + "개");
            } else {
                lore.add("§c▶ 수리할 수 없는 아이템");
            }
        } else {
            lore.add("§c▶ 손에 아이템이 없습니다");
        }
        lore.add("");
        lore.add("§e클릭하여 수리 GUI 열기");

        return createItem(Material.ANVIL, "§e[ 수리 ]", lore);
    }

    private ItemStack createHeldItemDisplay() {
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return createItem(Material.BARRIER, "§c[ 아이템 없음 ]",
                    List.of("", "§7손에 아이템을 들고", "§7다시 시도해주세요."));
        }

        ItemStack displayItem = heldItem.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ItemUtil.parse("§a▶ 대상 아이템"));

            if (plugin.getRepairManager().hasDurability(displayItem)) {
                double ratio = plugin.getRepairManager().getDurabilityRatio(heldItem);
                int percent = (int) (ratio * 100);
                lore.add(ItemUtil.parse("§7내구도: §f" + percent + "%"));
            }

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    // 슬롯 확인 메서드들
    public boolean isEnchantSlot(int slot) {
        return slot == SLOT_ENCHANT;
    }

    public boolean isRepairSlot(int slot) {
        return slot == SLOT_REPAIR;
    }
}
