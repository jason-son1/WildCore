package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
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
 * 인챈트(강화소) GUI (페이지네이션 지원)
 */
public class EnchantGUI extends PaginatedGui<EnchantConfig> {

    private final ItemStack heldItem;
    private final List<EnchantConfig> availableEnchants;

    public EnchantGUI(WildCore plugin, Player player) {
        super(plugin, player);
        this.heldItem = player.getInventory().getItemInMainHand();
        this.availableEnchants = loadAvailableEnchants();
    }

    /**
     * 현재 손에 든 아이템에 적용 가능한 인챈트 목록을 로드합니다.
     */
    private List<EnchantConfig> loadAvailableEnchants() {
        List<EnchantConfig> enchants = plugin.getEnchantManager().getAvailableEnchants(heldItem);
        // 이름순으로 정렬
        enchants.sort(Comparator.comparing(EnchantConfig::getDisplayName));
        return enchants;
    }

    @Override
    protected List<EnchantConfig> getItems() {
        return availableEnchants;
    }

    @Override
    protected ItemStack createItemDisplay(EnchantConfig enchant) {
        Material material = Material.getMaterial(enchant.getMaterial());
        if (material == null) {
            material = Material.ENCHANTED_BOOK;
        }

        List<String> lore = new ArrayList<>(enchant.getLore());
        lore.add("");
        lore.add("§7결과: §f" + enchant.getResultEnchantment() + " Lv." + enchant.getResultLevel());
        lore.add("§7비용: §6" + String.format("%,.0f", enchant.getCostMoney()) + "원");
        lore.add("");
        if (!enchant.getCostItems().isEmpty()) {
            lore.add("§7재료:");
            lore.addAll(plugin.getEnchantManager().getCostItemStatus(player, enchant));
        }
        lore.add("");
        lore.add(formatProbability("성공", enchant.getSuccessRate(), "§a"));
        lore.add(formatProbability("실패", enchant.getFailRate(), "§e"));
        lore.add(formatProbability("파괴", enchant.getDestroyRate(), "§c"));
        lore.add("");
        lore.add("§e클릭하여 강화 시도!");

        return createItem(material, enchant.getDisplayName(), lore);
    }

    /**
     * 확률을 포맷팅합니다.
     */
    private String formatProbability(String label, double rate, String color) {
        return "§7" + label + ": " + color + String.format("%.1f", rate) + "%";
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        if (totalPages <= 1) {
            return "§8[ §5강화소 §8]";
        }
        return "§8[ §5강화소 §8] §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.PURPLE_STAINED_GLASS_PANE;
    }

    @Override
    protected Material getBackgroundMaterial() {
        return Material.BLACK_STAINED_GLASS_PANE;
    }

    @Override
    protected void createInventory(int page) {
        super.createInventory(page);

        // 상단에 현재 손에 든 아이템 정보 표시 (슬롯 4 -> 47)
        ItemStack heldItemInfo = createHeldItemInfo();
        inventory.setItem(47, heldItemInfo);

        // 적용 가능한 인챈트가 없는 경우 알림 표시 (슬롯 22)
        if (availableEnchants.isEmpty()) {
            ItemStack noEnchant = createItem(Material.BARRIER, "§c적용 가능한 강화가 없습니다",
                    List.of("", "§7손에 강화 가능한 아이템을 들고", "§7다시 열어주세요."));
            inventory.setItem(22, noEnchant);
        }
    }

    /**
     * 손에 든 아이템 정보를 생성합니다.
     */
    private ItemStack createHeldItemInfo() {
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return createItem(Material.IRON_SWORD, "§7[ 대상 아이템 없음 ]",
                    List.of("", "§c손에 아이템을 들고 있지 않습니다.", "§7강화할 아이템을 손에 들고", "§7다시 GUI를 열어주세요."));
        }

        ItemStack displayItem = heldItem.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ItemUtil.parse("§a▶ 강화 대상 아이템"));
            lore.add(ItemUtil.parse("§7아래에서 원하는 강화를 선택하세요."));
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
        lore.add("§7가능한 강화: §e" + totalItems + "개");
        lore.add("");
        lore.add("§e클릭: §f원하는 강화를 선택하세요");

        return createItem(Material.ENCHANTED_BOOK, "§5[ 강화 정보 ]", lore);
    }

    /**
     * 특정 슬롯에 해당하는 인챈트 ID를 반환합니다.
     */
    public String getEnchantIdAtSlot(int slot) {
        EnchantConfig enchant = getItemAtSlot(slot);
        return enchant != null ? enchant.getId() : null;
    }

    /**
     * 현재 손에 든 아이템을 반환합니다.
     */
    public ItemStack getHeldItem() {
        return heldItem;
    }
}
