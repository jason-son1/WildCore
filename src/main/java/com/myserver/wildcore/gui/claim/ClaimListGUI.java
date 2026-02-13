package com.myserver.wildcore.gui.claim;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.managers.ClaimDataManager.ClaimMetadata;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 다중 사유지 목록 GUI
 * 플레이어가 소유한 모든 사유지를 목록으로 보여주고 선택하여 관리할 수 있습니다.
 */
public class ClaimListGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final List<Claim> claims;
    private Inventory inventory;

    private static final String TITLE = "§8[ §a🏡 내 사유지 목록 §8]";

    // 사유지 아이콘이 배치될 슬롯들 (최대 28개)
    private static final int[] CLAIM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    // 사유지 ID와 슬롯 매핑
    private final Map<Integer, Claim> slotClaimMap = new HashMap<>();

    public ClaimListGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.claims = plugin.getClaimManager().getPlayerClaims(player.getUniqueId());
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, TITLE);

        // 테두리 채우기
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // 정보 헤더
        inventory.setItem(4, createHeaderItem());

        // 사유지 아이콘 배치
        slotClaimMap.clear();
        for (int i = 0; i < claims.size() && i < CLAIM_SLOTS.length; i++) {
            Claim claim = claims.get(i);
            int slot = CLAIM_SLOTS[i];
            inventory.setItem(slot, createClaimIcon(claim, i + 1));
            slotClaimMap.put(slot, claim);
        }
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHeaderItem() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l🏡 내 사유지 목록");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7보유 사유지: §f" + claims.size() + "개");
        lore.add("");
        lore.add("§e사유지를 클릭하여 관리하세요.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClaimIcon(Claim claim, int index) {
        ClaimMetadata metadata = plugin.getClaimDataManager().getClaimData(claim.getID());

        // 아이콘 아이템
        Material iconMaterial = Material.GRASS_BLOCK;
        try {
            iconMaterial = Material.valueOf(metadata.getIcon());
        } catch (IllegalArgumentException ignored) {
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();

        // 이름
        String nickname = metadata.getNickname();
        if (nickname == null || nickname.isEmpty()) {
            nickname = "사유지 #" + index;
        }
        meta.setDisplayName("§a§l" + index + ". §f" + nickname);

        List<String> lore = new ArrayList<>();
        lore.add("");

        // 위치 정보
        Location center = plugin.getClaimManager().getClaimCenter(claim);
        if (center != null && center.getWorld() != null) {
            lore.add("§7월드: §f" + center.getWorld().getName());
            lore.add("§7좌표: §f" + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());
        }

        // 크기 정보
        String size = plugin.getClaimManager().getClaimSize(claim);
        lore.add("§7크기: §f" + size);

        // 면적 정보
        int area = plugin.getClaimManager().getClaimArea(claim);
        lore.add("§7면적: §f" + area + " 블록");

        // 생성일
        long createdAt = metadata.getCreatedAt();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        lore.add("§7생성일: §f" + sdf.format(new Date(createdAt)));

        // 청크 로드 상태
        boolean chunkLoaded = metadata.isChunkLoaded();
        lore.add("§7청크 로드: " + (chunkLoaded ? "§a활성" : "§c비활성"));

        // 작물 버프 상태
        if (plugin.getCropGrowthManager() != null && plugin.getCropGrowthManager().hasActiveBuff(claim.getID())) {
            long remaining = plugin.getCropGrowthManager().getRemainingBuffTime(claim.getID());
            double multiplier = plugin.getCropGrowthManager().getBuffMultiplier(claim.getID());
            lore.add("§7작물 버프: §a" + multiplier + "x §7(남은 시간: §f" + formatTime(remaining) + "§7)");
        } else {
            lore.add("§7작물 버프: §c비활성");
        }

        lore.add("");
        lore.add("§e클릭하여 관리");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(long seconds) {
        if (seconds <= 0)
            return "0:00";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + ":" + String.format("%02d", secs);
    }

    public void open() {
        player.openInventory(inventory);
    }

    /**
     * 클릭된 슬롯에 대한 Claim을 반환합니다.
     */
    public Claim getClaimAtSlot(int slot) {
        return slotClaimMap.get(slot);
    }

    /**
     * 사유지 슬롯인지 확인합니다.
     */
    public boolean isClaimSlot(int slot) {
        return slotClaimMap.containsKey(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
