package com.myserver.wildcore.gui.shop;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 관리자용 상점 설정 GUI
 * 상점 이름 변경, 위치 이동, NPC 타입 변경, 삭제 등
 */
public class ShopAdminGUI implements InventoryHolder {

    private static final int GUI_SIZE = 54;

    // 슬롯 정의
    private static final int SLOT_NAME = 11;
    private static final int SLOT_LOCATION = 13;
    private static final int SLOT_NPC_TYPE = 15;
    private static final int SLOT_EDIT_ITEMS = 31;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_DELETE = 49;
    private static final int SLOT_CLOSE = 53;

    private final WildCore plugin;
    private final Player player;
    private final ShopConfig shop;
    private Inventory inventory;

    public ShopAdminGUI(WildCore plugin, Player player, ShopConfig shop) {
        this.plugin = plugin;
        this.player = player;
        this.shop = shop;
    }

    public void open() {
        String title = "§8[ §c상점 관리 §8] §7" + shop.getId();
        inventory = Bukkit.createInventory(this, GUI_SIZE, ItemUtil.parse(title));

        setupItems();
        player.openInventory(inventory);
    }

    private void setupItems() {
        // 배경 채우기
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, background);
        }

        // 상점 정보 표시 (상단)
        inventory.setItem(4, createInfoIcon());

        // 상점 이름 변경
        inventory.setItem(SLOT_NAME, ItemUtil.createItem(
                Material.NAME_TAG,
                "§e상점 이름 변경",
                List.of(
                        "",
                        "§7현재 이름: " + shop.getDisplayName(),
                        "",
                        "§a클릭하여 이름 변경"),
                1, null, 0, false, null));

        // 위치 이동
        inventory.setItem(SLOT_LOCATION, ItemUtil.createItem(
                Material.COMPASS,
                "§e위치 이동",
                List.of(
                        "",
                        "§7현재 위치: " + shop.getLocationString(),
                        "",
                        "§a클릭하여 현재 위치로 이동"),
                1, null, 0, false, null));

        // NPC 타입 변경
        Material npcIcon = shop.isVillager() ? Material.VILLAGER_SPAWN_EGG : Material.ARMOR_STAND;
        inventory.setItem(SLOT_NPC_TYPE, ItemUtil.createItem(
                npcIcon,
                "§eNPC 타입 변경",
                List.of(
                        "",
                        "§7현재 타입: §f" + shop.getNpcType(),
                        "",
                        "§a클릭하여 타입 전환"),
                1, null, 0, false, null));

        // 아이템 편집 모드
        inventory.setItem(SLOT_EDIT_ITEMS, ItemUtil.createItem(
                Material.CHEST,
                "§6아이템 편집",
                List.of(
                        "",
                        "§7등록된 아이템: §e" + shop.getItemCount() + "개",
                        "",
                        "§a클릭하여 아이템 편집 모드 진입"),
                1, null, 0, true, null));

        // 뒤로 가기
        inventory.setItem(SLOT_BACK, ItemUtil.createItem(
                Material.ARROW,
                "§7뒤로 가기",
                List.of("", "§7클릭하여 상점 열기"),
                1, null, 0, false, null));

        // 상점 삭제
        inventory.setItem(SLOT_DELETE, ItemUtil.createItem(
                Material.BARRIER,
                "§c상점 삭제",
                List.of(
                        "",
                        "§c⚠ 주의: 되돌릴 수 없습니다!",
                        "",
                        "§cShift+클릭하여 삭제"),
                1, null, 0, false, null));

        // 닫기
        inventory.setItem(SLOT_CLOSE, ItemUtil.createItem(
                Material.DARK_OAK_DOOR,
                "§c닫기",
                null,
                1, null, 0, false, null));
    }

    private ItemStack createInfoIcon() {
        return ItemUtil.createItem(
                Material.EMERALD,
                "§a" + shop.getDisplayName(),
                List.of(
                        "",
                        "§7상점 ID: §f" + shop.getId(),
                        "§7NPC 타입: §f" + shop.getNpcType(),
                        "§7위치: §f" + shop.getLocationString(),
                        "§7등록 아이템: §e" + shop.getItemCount() + "개"),
                1, null, 0, true, null);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ShopConfig getShop() {
        return shop;
    }

    public Player getPlayer() {
        return player;
    }

    public WildCore getPlugin() {
        return plugin;
    }

    public void refresh() {
        setupItems();
    }

    // 슬롯 Getter들
    public static int getSlotName() {
        return SLOT_NAME;
    }

    public static int getSlotLocation() {
        return SLOT_LOCATION;
    }

    public static int getSlotNpcType() {
        return SLOT_NPC_TYPE;
    }

    public static int getSlotEditItems() {
        return SLOT_EDIT_ITEMS;
    }

    public static int getSlotBack() {
        return SLOT_BACK;
    }

    public static int getSlotDelete() {
        return SLOT_DELETE;
    }

    public static int getSlotClose() {
        return SLOT_CLOSE;
    }
}
