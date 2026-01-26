package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.util.EnchantNameUtil;
import com.myserver.wildcore.gui.PaginatedGui; // Import added
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 인챈트 관리자 메인 GUI - 인챈트 목록 및 관리 옵션
 */
public class EnchantAdminGUI extends PaginatedGui<EnchantConfig> {

    public EnchantAdminGUI(WildCore plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected List<EnchantConfig> getItems() {
        // ID 순으로 정렬하여 반환
        List<EnchantConfig> list = new ArrayList<>(plugin.getConfigManager().getEnchants().values());
        list.sort((a, b) -> a.getId().compareTo(b.getId()));
        return list;
    }

    @Override
    protected ItemStack createItemDisplay(EnchantConfig enchant) {
        Material material = Material.getMaterial(enchant.getMaterial());
        if (material == null)
            material = Material.ENCHANTED_BOOK;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7ID: §f" + enchant.getId());
        lore.add("");
        lore.add("§7결과: §f" + EnchantNameUtil.getKoreanName(enchant.getResultEnchantment()) + " "
                + enchant.getResultLevel());
        lore.add("§7비용: §6" + String.format("%,.0f", enchant.getCostMoney()) + "원");
        lore.add("");
        lore.add("§a성공: §f" + enchant.getSuccessRate() + "%");
        lore.add("§e실패: §f" + enchant.getFailRate() + "%");
        lore.add("§c파괴: §f" + enchant.getDestroyRate() + "%");
        lore.add("");
        lore.add("§e클릭하여 설정 변경");

        return createItem(material, enchant.getDisplayName(), lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        return "§8[ §5인챈트 관리 §8] §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected void createInventory(int page) {
        super.createInventory(page);

        // 상단 정보 (슬롯 4 -> PaginatedGui에서는 보통 사용 안하지만 여기선 유지 가능, 하지만 PaginatedGui는 0~44를
        // 아이템 영역으로 씀)
        // PaginatedGui 구조상 0~44는 리스트 아이템이 차지함. 슬롯 4에 정보 아이템을 넣으려면
        // 1. 첫 페이지에만 넣고 나머지 밀기? (복잡)
        // 2. 네비게이션 바(45~53) 활용?
        // 3. 별도 공간 할당?
        // 기존 PaginatedGui 구조를 따르기 위해 슬롯 4 정보 아이템은 제거하고, 대신 타이틀이나 도움말 아이템을 네비게이션 바에 추가.

        // 새 인챈트 추가 버튼 (슬롯 49가 기본 INFO 아이템이므로 50에 배치)
        ItemStack addEnchant = createItem(Material.EMERALD_BLOCK, "§a[ + 새 인챈트 추가 ]",
                List.of("", "§7클릭하여 새 인챈트 옵션을 추가합니다."));
        inventory.setItem(50, addEnchant);

        // 뒤로 가기 (슬롯 53은 Next Page, 45는 Prev Page. 53이 비었을 때 Back? 아니면 45-53 사이 빈 곳?)
        // PaginatedGui에서 45, 49, 53은 예약됨.
        // 46, 47, 48, 50, 51, 52 사용 가능.

        // 닫기/뒤로가기 버튼 -> 슬롯 46에 배치
        ItemStack back = createItem(Material.BARRIER, "§c[ 닫기 ]", null);
        inventory.setItem(46, back);
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.BLACK_STAINED_GLASS_PANE;
    }
}
