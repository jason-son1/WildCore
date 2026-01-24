package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 정보 GUI
 * Shift + F 키를 눌렀을 때 표시되는 내 정보 창
 */
public class PlayerInfoGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Inventory inventory;

    public PlayerInfoGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ItemUtil.parse("§8[ §a나의 정보 §8]"));
        setupInventory();
    }

    private void setupInventory() {
        // 배경 채우기
        ItemStack bg = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // 1. 플레이어 헤드 (슬롯 10)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.displayName(ItemUtil.parse("§a§l" + player.getName()));

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7레벨: §a" + player.getLevel());
            lore.add("§7경험치: §a" + (int) (player.getExp() * 100) + "%");
            lore.add("");
            lore.add("§7체력: §c" + (int) player.getHealth() + " / "
                    + (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            lore.add("§7배고픔: §6" + player.getFoodLevel() + " / 20");

            headMeta.lore(ItemUtil.parseList(lore));
            head.setItemMeta(headMeta);
        }
        inventory.setItem(10, head);

        // 2. 자산 정보 (슬롯 12)
        double balance = plugin.getEconomy().getBalance(player);
        List<String> moneyLore = new ArrayList<>();
        moneyLore.add("");
        moneyLore.add("§7보유 금액: §e" + String.format("%,.0f", balance) + "원");
        inventory.setItem(12, ItemUtil.createItem(Material.EMERALD, "§e§l자산 정보", moneyLore, 1, null, 0, false, plugin));

        // 3. 위치 정보 (슬롯 14)
        Location loc = player.getLocation();
        List<String> locLore = new ArrayList<>();
        locLore.add("");
        locLore.add("§7월드: §f" + loc.getWorld().getName());
        locLore.add("§7좌표: §fX:" + loc.getBlockX() + ", Y:" + loc.getBlockY() + ", Z:" + loc.getBlockZ());
        inventory.setItem(14, ItemUtil.createItem(Material.COMPASS, "§b§l위치 정보", locLore, 1, null, 0, false, plugin));

        // 4. 서버 시간 및 접속 정보 (슬롯 16)
        List<String> timeLore = new ArrayList<>();
        timeLore.add("");
        timeLore.add("§7현재 시간: §f" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        timeLore.add("§7접속자 수: §f" + Bukkit.getOnlinePlayers().size() + "명");
        inventory.setItem(16, ItemUtil.createItem(Material.CLOCK, "§6§l서버 정보", timeLore, 1, null, 0, false, plugin));

        // 5. 내 주식 정보 (슬롯 13)
        List<String> stockLore = new ArrayList<>();
        stockLore.add("");
        stockLore.add("§7보유 중인 주식을 확인합니다.");
        stockLore.add("");
        stockLore.add("§e클릭하여 확인하기");
        inventory.setItem(13,
                ItemUtil.createItem(Material.GOLD_INGOT, "§d§l내 주식 정보", stockLore, 1, null, 0, false, plugin));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
