package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.npc.NpcData;
import com.myserver.wildcore.npc.NpcType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NPC 목록 GUI (페이지네이션 지원)
 * 모든 NPC를 표시하고 개별 관리 가능
 */
public class NpcListGUI extends PaginatedGui<NpcData> {

    private final NpcType filterType; // null이면 전체 타입
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * 전체 NPC 목록 GUI
     */
    public NpcListGUI(WildCore plugin, Player player) {
        super(plugin, player);
        this.filterType = null;
    }

    /**
     * 특정 타입 NPC 목록 GUI
     */
    public NpcListGUI(WildCore plugin, Player player, NpcType filterType) {
        super(plugin, player);
        this.filterType = filterType;
    }

    @Override
    protected List<NpcData> getItems() {
        if (filterType != null) {
            return plugin.getNpcManager().getNpcsByType(filterType);
        }
        return plugin.getNpcManager().getAllNpcsList();
    }

    @Override
    protected ItemStack createItemDisplay(NpcData npc) {
        NpcType type = npc.getType();
        Material material = type.getIcon();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7ID: §f" + npc.getId());
        lore.add("§7타입: " + type.getColoredName());

        if (npc.getTargetId() != null && !npc.getTargetId().isEmpty()) {
            lore.add("§7대상: §f" + npc.getTargetId());
        }

        lore.add("");
        lore.add("§7위치: §f" + npc.getLocationString());
        lore.add("§7엔티티: §f" + (npc.getEntityType() != null ? npc.getEntityType().name() : "VILLAGER"));
        lore.add("");
        lore.add("§7생성일: §f" + dateFormat.format(new Date(npc.getCreatedTime())));

        lore.add("");
        lore.add("§a좌클릭: §f텔레포트");
        lore.add("§e우클릭: §f이름 변경");
        lore.add("§cShift+우클릭: §f삭제");

        String displayName = npc.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = type.getDefaultNpcName();
        }

        return createItem(material, displayName, lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        String typePrefix = filterType != null ? filterType.getColoredName() + " " : "";
        if (totalPages <= 1) {
            return "§8[ " + typePrefix + "§6NPC 목록 §8]";
        }
        return "§8[ " + typePrefix + "§6NPC 목록 §8] §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    @Override
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (filterType == null) {
            // 전체 목록: 타입별 카운트
            for (NpcType type : NpcType.values()) {
                int count = plugin.getNpcManager().getNpcCount(type);
                if (count > 0) {
                    lore.add(type.getColorCode() + type.getDisplayName() + ": §f" + count + "개");
                }
            }
        } else {
            lore.add(filterType.getDescription());
        }

        lore.add("");
        lore.add("§7현재 페이지: §e" + page + " / " + totalPages);
        lore.add("§7총 NPC: §e" + totalItems + "개");
        lore.add("");
        lore.add("§7클릭하여 필터 초기화");

        return createItem(Material.BOOK, "§f[ NPC 정보 ]", lore);
    }

    /**
     * 특정 슬롯에 해당하는 NPC ID를 반환합니다.
     */
    public String getNpcIdAtSlot(int slot) {
        NpcData npc = getItemAtSlot(slot);
        return npc != null ? npc.getId() : null;
    }

    /**
     * 특정 슬롯에 해당하는 NPC 데이터를 반환합니다.
     */
    public NpcData getNpcDataAtSlot(int slot) {
        return getItemAtSlot(slot);
    }

    public NpcType getFilterType() {
        return filterType;
    }
}
