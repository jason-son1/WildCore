package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.RepairConfig;
import com.myserver.wildcore.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 수리소 GUI (페이지네이션 지원)
 * 수리 옵션 목록을 표시하고 클릭 시 수리 진행
 */
public class RepairGUI extends PaginatedGui<RepairConfig> {

    private final ItemStack heldItem;
    private final List<RepairConfig> availableRepairs;

    public RepairGUI(WildCore plugin, Player player) {
        super(plugin, player);
        this.heldItem = player.getInventory().getItemInMainHand();
        this.availableRepairs = loadAvailableRepairs();
    }

    /**
     * 현재 손에 든 아이템에 적용 가능한 수리 옵션 목록을 로드합니다.
     */
    private List<RepairConfig> loadAvailableRepairs() {
        List<RepairConfig> repairs = plugin.getRepairManager().getAvailableRepairs(heldItem);
        // 수리 비율 순으로 정렬 (낮은 것부터)
        repairs.sort(Comparator.comparingDouble(RepairConfig::getRepairPercentage));
        return repairs;
    }

    @Override
    protected List<RepairConfig> getItems() {
        return availableRepairs;
    }

    @Override
    protected ItemStack createItemDisplay(RepairConfig repair) {
        Material material = Material.getMaterial(repair.getMaterial());
        if (material == null) {
            material = Material.ANVIL;
        }

        List<String> lore = new ArrayList<>(repair.getLore());
        lore.add("");

        // 수리량 표시
        int repairPercent = (int) (repair.getRepairPercentage() * 100);
        lore.add("§7수리량: §e" + repairPercent + "%");

        // 비용 표시
        lore.add("§7비용: §6" + String.format("%,.0f", repair.getCostMoney()) + "원");

        // 재료 표시
        if (!repair.getCostItems().isEmpty()) {
            lore.add("");
            lore.add("§7재료:");
            lore.addAll(plugin.getRepairManager().getCostItemStatus(player, repair));
        }

        // 현재 내구도 표시
        if (heldItem != null && heldItem.getType() != Material.AIR) {
            lore.add("");
            double currentRatio = plugin.getRepairManager().getDurabilityRatio(heldItem);
            int currentPercent = (int) (currentRatio * 100);
            double afterRatio = Math.min(1.0, currentRatio + repair.getRepairPercentage());
            int afterPercent = (int) (afterRatio * 100);

            String currentColor = currentPercent > 50 ? "§a" : (currentPercent > 25 ? "§e" : "§c");
            lore.add("§7현재: " + currentColor + currentPercent + "% §7→ §a" + afterPercent + "%");
        }

        lore.add("");
        lore.add("§e클릭하여 수리!");

        return createItem(material, repair.getDisplayName(), lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        String baseTitle = plugin.getConfigManager().getRepairGuiTitle();
        if (totalPages <= 1) {
            return baseTitle;
        }
        return baseTitle + " §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.YELLOW_STAINED_GLASS_PANE;
    }

    @Override
    protected Material getBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    @Override
    protected void createInventory(int page) {
        super.createInventory(page);

        // 상단에 현재 손에 든 아이템 정보 표시 (슬롯 47)
        ItemStack heldItemInfo = createHeldItemInfo();
        inventory.setItem(47, heldItemInfo);

        // 뒤로 가기 버튼 (슬롯 48)
        ItemStack backButton = createItem(Material.ARROW, "§c[ ◀ 뒤로 가기 ]",
                List.of("", "§7클릭하여 선택 화면으로 돌아갑니다."));
        inventory.setItem(48, backButton);

        // 적용 가능한 수리가 없는 경우 알림 표시 (슬롯 22)
        if (availableRepairs.isEmpty()) {
            ItemStack noRepair = createItem(Material.BARRIER, "§c적용 가능한 수리가 없습니다",
                    List.of("", "§7손에 수리 가능한 아이템을 들고", "§7다시 열어주세요."));
            inventory.setItem(22, noRepair);
        }
    }

    /**
     * 손에 든 아이템 정보를 생성합니다.
     */
    private ItemStack createHeldItemInfo() {
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return createItem(Material.BARRIER, "§7[ 대상 아이템 없음 ]",
                    List.of("", "§c손에 아이템을 들고 있지 않습니다.", "§7수리할 아이템을 손에 들고", "§7다시 GUI를 열어주세요."));
        }

        ItemStack displayItem = heldItem.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ItemUtil.parse("§e▶ 수리 대상 아이템"));

            if (plugin.getRepairManager().hasDurability(heldItem)) {
                double ratio = plugin.getRepairManager().getDurabilityRatio(heldItem);
                int percent = (int) (ratio * 100);
                String color = percent > 50 ? "§a" : (percent > 25 ? "§e" : "§c");
                lore.add(ItemUtil.parse("§7내구도: " + color + percent + "%"));
            }

            lore.add(ItemUtil.parse("§7아래에서 원하는 수리를 선택하세요."));
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    @Override
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7현재 페이지: §e" + page + " / " + totalPages);
        lore.add("§7가능한 수리: §e" + totalItems + "개");
        lore.add("");
        lore.add("§e클릭: §f원하는 수리를 선택하세요");

        return createItem(Material.ANVIL, "§e[ 수리 정보 ]", lore);
    }

    /**
     * 특정 슬롯에 해당하는 수리 ID를 반환합니다.
     */
    public String getRepairIdAtSlot(int slot) {
        RepairConfig repair = getItemAtSlot(slot);
        return repair != null ? repair.getId() : null;
    }

    /**
     * 현재 손에 든 아이템을 반환합니다.
     */
    public ItemStack getHeldItem() {
        return heldItem;
    }

    /**
     * 뒤로 가기 슬롯인지 확인
     */
    public boolean isBackSlot(int slot) {
        return slot == 48;
    }
}
