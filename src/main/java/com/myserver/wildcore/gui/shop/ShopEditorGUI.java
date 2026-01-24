package com.myserver.wildcore.gui.shop;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.config.ShopItemConfig;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 관리자용 상점 아이템 편집 GUI
 * 드래그 앤 드롭으로 아이템 등록, 가격 설정
 */
public class ShopEditorGUI implements InventoryHolder {

    private static final int GUI_SIZE = 54;
    private static final int ITEMS_AREA_END = 44; // 0~44 슬롯이 아이템 영역

    // 네비게이션 슬롯
    private static final int SLOT_BACK = 45;
    private static final int SLOT_HELP = 49;
    private static final int SLOT_SAVE = 53;

    private final WildCore plugin;
    private final Player player;
    private final ShopConfig shop;
    private Inventory inventory;

    // 현재 편집 중인 슬롯 (가격 입력 대기용)
    private int pendingPriceSlot = -1;
    private boolean waitingForBuyPrice = false;
    private boolean waitingForSellPrice = false;

    public ShopEditorGUI(WildCore plugin, Player player, ShopConfig shop) {
        this.plugin = plugin;
        this.player = player;
        this.shop = shop;
    }

    public void open() {
        String title = "§8[ §e아이템 편집 §8] §7" + shop.getId();
        inventory = Bukkit.createInventory(this, GUI_SIZE, ItemUtil.parse(title));

        setupItems();
        player.openInventory(inventory);
    }

    private void setupItems() {
        // 아이템 영역 초기화 (0~44)
        for (int i = 0; i <= ITEMS_AREA_END; i++) {
            ShopItemConfig item = shop.getItem(i);
            if (item != null) {
                inventory.setItem(i, createShopItemDisplay(item, i));
            } else {
                // 빈 슬롯 표시
                inventory.setItem(i, createEmptySlotItem(i));
            }
        }

        // 네비게이션 바 (45~53)
        ItemStack navBackground = ItemUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, navBackground);
        }

        // 뒤로 가기
        inventory.setItem(SLOT_BACK, ItemUtil.createItem(
                Material.ARROW,
                "§7뒤로 가기",
                List.of("", "§7클릭하여 관리 메뉴로"),
                1, null, 0, false, null));

        // 도움말
        inventory.setItem(SLOT_HELP, ItemUtil.createItem(
                Material.BOOK,
                "§e사용법",
                List.of(
                        "",
                        "§a아이템 등록:",
                        "§7인벤토리에서 아이템을 이 GUI로",
                        "§7드래그하여 원하는 슬롯에 놓기",
                        "",
                        "§a가격 설정:",
                        "§7등록된 아이템을 §e좌클릭§7하여",
                        "§7구매가/판매가 설정",
                        "",
                        "§c아이템 제거:",
                        "§7등록된 아이템을 §cShift+우클릭"),
                1, null, 0, true, null));

        // 저장 및 나가기
        inventory.setItem(SLOT_SAVE, ItemUtil.createItem(
                Material.EMERALD_BLOCK,
                "§a저장 및 나가기",
                List.of("", "§7변경사항은 자동 저장됩니다"),
                1, null, 0, true, null));
    }

    /**
     * 상점 아이템 표시 생성
     */
    private ItemStack createShopItemDisplay(ShopItemConfig item, int slot) {
        Material material = Material.STONE;
        String displayName = "§f알 수 없는 아이템";

        if (item.isVanilla()) {
            try {
                material = Material.valueOf(item.getId().toUpperCase());
                displayName = "§f" + material.name();
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            var customConfig = plugin.getConfigManager().getCustomItem(item.getId());
            if (customConfig != null) {
                try {
                    material = Material.valueOf(customConfig.getMaterial().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
                displayName = customConfig.getDisplayName();
            } else {
                displayName = "§c[커스텀] " + item.getId();
            }
        }

        List<String> lore = List.of(
                "",
                "§7슬롯: §e" + slot,
                "§7타입: §f" + item.getType(),
                "§7ID: §f" + item.getId(),
                "",
                "§7구매가: " + (item.canBuy() ? "§6" + String.format("%,.0f", item.getBuyPrice()) + "원" : "§c구매 불가"),
                "§7판매가: " + (item.canSell() ? "§6" + String.format("%,.0f", item.getSellPrice()) + "원" : "§c판매 불가"),
                "",
                "§e좌클릭: §f가격 설정",
                "§cShift+우클릭: §f제거");

        return ItemUtil.createItem(material, displayName, lore, 1, null, 0, false, null);
    }

    /**
     * 빈 슬롯 표시 생성
     */
    private ItemStack createEmptySlotItem(int slot) {
        return ItemUtil.createItem(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "§7빈 슬롯 §8#" + slot,
                List.of(
                        "",
                        "§7아이템을 드래그하여 등록"),
                1, null, 0, false, null);
    }

    /**
     * 아이템 등록
     */
    public void registerItem(int slot, ItemStack item) {
        if (slot < 0 || slot > ITEMS_AREA_END)
            return;

        String type;
        String id;

        // 커스텀 아이템인지 확인
        String customId = ItemUtil.getCustomItemId(plugin, item);
        if (customId != null) {
            type = "CUSTOM";
            id = customId;
        } else {
            type = "VANILLA";
            id = item.getType().name();
        }

        // 기본 가격으로 등록 (나중에 수정)
        ShopItemConfig shopItem = new ShopItemConfig(slot, type, id, 100.0, 50.0);
        shop.setItem(slot, shopItem);

        // 저장
        plugin.getConfigManager().saveShop(shop);

        // GUI 갱신
        inventory.setItem(slot, createShopItemDisplay(shopItem, slot));

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§a아이템이 등록되었습니다. 클릭하여 가격을 설정하세요.");
    }

    /**
     * 아이템 제거
     */
    public void removeItem(int slot) {
        if (slot < 0 || slot > ITEMS_AREA_END)
            return;

        shop.removeItem(slot);
        plugin.getConfigManager().saveShop(shop);

        inventory.setItem(slot, createEmptySlotItem(slot));

        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c아이템이 제거되었습니다.");
    }

    /**
     * 가격 설정 모드 시작
     */
    public void startPriceSetting(int slot) {
        pendingPriceSlot = slot;
        waitingForBuyPrice = true;
        waitingForSellPrice = false;

        player.closeInventory();
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§e구매 가격을 입력하세요. §7(구매 불가: -1, 취소: cancel)");
    }

    /**
     * 구매가 설정
     */
    public void setBuyPrice(double price) {
        if (pendingPriceSlot < 0)
            return;

        ShopItemConfig oldItem = shop.getItem(pendingPriceSlot);
        if (oldItem == null)
            return;

        ShopItemConfig newItem = new ShopItemConfig(
                pendingPriceSlot,
                oldItem.getType(),
                oldItem.getId(),
                price,
                oldItem.getSellPrice());
        shop.setItem(pendingPriceSlot, newItem);

        waitingForBuyPrice = false;
        waitingForSellPrice = true;

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§e판매 가격을 입력하세요. §7(판매 불가: -1, 취소: cancel)");
    }

    /**
     * 판매가 설정
     */
    public void setSellPrice(double price) {
        if (pendingPriceSlot < 0)
            return;

        ShopItemConfig oldItem = shop.getItem(pendingPriceSlot);
        if (oldItem == null)
            return;

        ShopItemConfig newItem = new ShopItemConfig(
                pendingPriceSlot,
                oldItem.getType(),
                oldItem.getId(),
                oldItem.getBuyPrice(),
                price);
        shop.setItem(pendingPriceSlot, newItem);
        plugin.getConfigManager().saveShop(shop);

        waitingForBuyPrice = false;
        waitingForSellPrice = false;
        pendingPriceSlot = -1;

        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a가격이 설정되었습니다.");

        // GUI 다시 열기
        open();
    }

    /**
     * 가격 설정 취소
     */
    public void cancelPriceSetting() {
        pendingPriceSlot = -1;
        waitingForBuyPrice = false;
        waitingForSellPrice = false;

        // GUI 다시 열기
        open();
    }

    // === Getters ===

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

    public int getPendingPriceSlot() {
        return pendingPriceSlot;
    }

    public boolean isWaitingForBuyPrice() {
        return waitingForBuyPrice;
    }

    public boolean isWaitingForSellPrice() {
        return waitingForSellPrice;
    }

    public boolean isItemSlot(int slot) {
        return slot >= 0 && slot <= ITEMS_AREA_END;
    }

    public void refresh() {
        setupItems();
    }

    public static int getSlotBack() {
        return SLOT_BACK;
    }

    public static int getSlotHelp() {
        return SLOT_HELP;
    }

    public static int getSlotSave() {
        return SLOT_SAVE;
    }

    public static int getItemsAreaEnd() {
        return ITEMS_AREA_END;
    }
}
