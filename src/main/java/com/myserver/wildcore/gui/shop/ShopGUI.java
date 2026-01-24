package com.myserver.wildcore.gui.shop;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.config.ShopItemConfig;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.KoreanMaterialUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 유저용 상점 GUI
 * 아이템 구매/판매 인터페이스
 */
public class ShopGUI extends PaginatedGui<ShopItemConfig> {

    private final ShopConfig shop;
    private final List<ShopItemConfig> sortedItems;

    public ShopGUI(WildCore plugin, Player player, ShopConfig shop) {
        super(plugin, player);
        this.shop = shop;

        // 아이템들을 슬롯 순서대로 정렬
        this.sortedItems = new ArrayList<>(shop.getItems().values());
        this.sortedItems.sort(Comparator.comparingInt(ShopItemConfig::getSlot));
    }

    @Override
    protected List<ShopItemConfig> getItems() {
        return sortedItems;
    }

    @Override
    protected ItemStack createItemDisplay(ShopItemConfig item) {
        // 아이템 기본 정보
        Material material = Material.STONE;
        String displayName = "§f알 수 없는 아이템";

        if (item.isVanilla()) {
            try {
                material = Material.valueOf(item.getId().toUpperCase());
                displayName = "§f" + KoreanMaterialUtil.getName(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("잘못된 Material: " + item.getId());
            }
        } else {
            // 커스텀 아이템
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

        // Lore 생성
        List<String> lore = new ArrayList<>();
        lore.add("");

        // 구매/판매 가격
        if (item.canBuy()) {
            lore.add("§7구매가: §6" + String.format("%,.0f", item.getBuyPrice()) + "원");
        } else {
            lore.add("§7구매가: §c구매 불가");
        }

        if (item.canSell()) {
            lore.add("§7판매가: §6" + String.format("%,.0f", item.getSellPrice()) + "원");
        } else {
            lore.add("§7판매가: §c판매 불가");
        }

        // 현재 잔액
        lore.add("");
        double balance = plugin.getEconomy().getBalance(player);
        lore.add("§7보유금: §e" + String.format("%,.0f", balance) + "원");

        // 보유 수량
        int owned = countOwnedItems(item);
        lore.add("§7보유량: §e" + owned + "개");

        // 조작 안내
        lore.add("");
        if (item.canBuy()) {
            lore.add("§e좌클릭: §f1개 구매");
            lore.add("§eShift+좌클릭: §f64개 구매");
        }
        if (item.canSell()) {
            lore.add("§e우클릭: §f1개 판매");
            lore.add("§eShift+우클릭: §f전량 판매");
        }

        return ItemUtil.createItem(material, displayName, lore, 1, null, 0, false, null);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        String shopName = shop.getDisplayName();
        if (totalPages > 1) {
            return shopName + " §7(" + page + "/" + totalPages + ")";
        }
        return shopName;
    }

    @Override
    protected Material getBackgroundMaterial() {
        return Material.LIME_STAINED_GLASS_PANE;
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    @Override
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7페이지: §e" + page + " / " + totalPages);
        lore.add("§7상품 수: §e" + totalItems + "개");
        lore.add("");
        double balance = plugin.getEconomy().getBalance(player);
        lore.add("§7보유금: §6" + String.format("%,.0f", balance) + "원");

        return createItem(Material.BOOK, "§f[ 상점 정보 ]", lore);
    }

    /**
     * 슬롯에 해당하는 아이템 가져오기
     */
    public ShopItemConfig getShopItemAtSlot(int slot) {
        return getItemAtSlot(slot);
    }

    /**
     * 상점 설정 가져오기
     */
    public ShopConfig getShop() {
        return shop;
    }

    /**
     * 보유 아이템 수량 세기
     */
    private int countOwnedItems(ShopItemConfig item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && matchesItem(stack, item)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    /**
     * 아이템 매칭 확인
     */
    private boolean matchesItem(ItemStack stack, ShopItemConfig item) {
        if (item.isCustom()) {
            String customId = ItemUtil.getCustomItemId(plugin, stack);
            return item.getId().equals(customId);
        } else {
            try {
                Material material = Material.valueOf(item.getId().toUpperCase());
                return stack.getType() == material;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

}
