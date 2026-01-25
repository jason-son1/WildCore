package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 은행 상품 목록 GUI (페이지네이션 지원)
 * - 가입 가능한 예금/적금 상품 표시
 * - 클릭 시 가입 프로세스 시작
 */
public class BankProductListGUI extends PaginatedGui<BankProductConfig> {

    public BankProductListGUI(WildCore plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected List<BankProductConfig> getItems() {
        return plugin.getConfigManager().getAllBankProductsSorted();
    }

    @Override
    protected ItemStack createItemDisplay(BankProductConfig product) {
        Material material = Material.getMaterial(product.getMaterial());
        if (material == null) {
            material = Material.GOLD_INGOT;
        }

        List<String> lore = new ArrayList<>();

        // 상품 정보
        lore.add("");
        if (product.isSavings()) {
            lore.add("§7유형: §a자유 예금");
            lore.add("§7이자율: §6" + String.format("%.2f%%", product.getInterestRate() * 100) + " "
                    + product.getFormattedInterestInterval());
            if (product.isCompoundInterest()) {
                lore.add("§d✦ 복리 적용");
            }
        } else if (product.isTermDeposit()) {
            lore.add("§7유형: §b정기 적금");
            lore.add("§7만기 이자: §6" + String.format("%.1f%%", product.getInterestRate() * 100));
            lore.add("§7기간: §f" + product.getFormattedDuration());
            lore.add("§c중도 해지 페널티: " + String.format("%.1f%%", product.getEarlyWithdrawalPenalty() * 100));
        }

        lore.add("");
        lore.add("§7최소 입금: §6" + String.format("%,.0f", product.getMinDeposit()) + "원");
        lore.add("§7최대 입금: §6" + String.format("%,.0f", product.getMaxDeposit()) + "원");

        // 설정된 lore 추가
        if (product.getLore() != null && !product.getLore().isEmpty()) {
            lore.add("");
            lore.addAll(product.getLore());
        }

        lore.add("");
        lore.add("§e클릭하여 가입하기");

        return createItem(material, product.getDisplayName(), lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        if (totalPages <= 1) {
            return "§8[ §a은행 상품 §8]";
        }
        return "§8[ §a은행 상품 §8] §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    }

    @Override
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7가입 가능한 은행 상품을 확인하세요.");
        lore.add("");
        lore.add("§a• 자유 예금: §f언제든 입출금, 시간당 이자");
        lore.add("§b• 정기 적금: §f만기 시 고수익 이자");
        lore.add("");
        lore.add("§7현재 페이지: §e" + page + " / " + totalPages);
        lore.add("§7총 상품: §e" + totalItems + "개");

        return createItem(Material.BOOK, "§f[ 상품 안내 ]", lore);
    }

    /**
     * 특정 슬롯에 해당하는 상품 ID를 반환합니다.
     */
    public String getProductIdAtSlot(int slot) {
        BankProductConfig product = getItemAtSlot(slot);
        return product != null ? product.getId() : null;
    }
}
