package com.myserver.wildcore.gui.shop;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 상점 목록 GUI
 * 등록된 모든 상점을 보여주고 관리(이동, 삭제, 설정 등)할 수 있는 GUI
 */
public class ShopListGUI extends PaginatedGui<ShopConfig> {

    private final WildCore plugin;

    public ShopListGUI(WildCore plugin, Player player) {
        super(plugin, player);
        this.plugin = plugin;
    }

    @Override
    protected List<ShopConfig> getItems() {
        return new ArrayList<>(plugin.getConfigManager().getShops().values());
    }

    @Override
    protected ItemStack createItemDisplay(ShopConfig shop) {
        Material icon = shop.isVillager() ? Material.VILLAGER_SPAWN_EGG : Material.ARMOR_STAND;

        return ItemUtil.createItem(
                icon,
                "§e" + shop.getDisplayName(),
                List.of(
                        "",
                        "§7ID: §f" + shop.getId(),
                        "§7NPC 타입: §f" + shop.getNpcType(),
                        "§7위치: §f" + shop.getLocationString(),
                        "§7아이템: §e" + shop.getItemCount() + "개",
                        "",
                        "§e좌클릭 §7- 상점 관리",
                        "§e우클릭 §7- 상점 열기",
                        "§eShift+좌클릭 §7- 상점으로 이동"),
                1, null, 0, true, null);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        return "§8[ §c상점 목록 §8] (" + page + "/" + totalPages + ")";
    }

    /**
     * 상점 ID 추출 (아이템 클릭 처리용 - 이제 getItemAtSlot을 사용하므로 불필요할 수 있지만 호환성을 위해 유지하거나 제거)
     * PaginatedGui의 getItemAtSlot을 사용하면 ShopConfig 객체를 직접 얻을 수 있으므로 이 메서드는 제거해도 됨.
     */

    /**
     * 네비게이션 바 설정 (새 상점 생성 버튼 추가)
     */
    @Override
    protected void setupNavigationBar(int page, int totalPages, int totalItems) {
        super.setupNavigationBar(page, totalPages, totalItems);

        // 새 상점 생성 버튼 (49번 슬롯 - 중앙, 정보 아이콘 덮어쓰기)
        getInventory().setItem(49, ItemUtil.createItem(
                Material.EMERALD,
                "§a새 상점 생성",
                List.of(
                        "",
                        "§7새로운 상점을 생성합니다.",
                        "§a클릭하여 생성"),
                1, null, 0, false, null));
    }
}
