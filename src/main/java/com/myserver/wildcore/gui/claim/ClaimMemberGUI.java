package com.myserver.wildcore.gui.claim;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.managers.ClaimManager.TrustType;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * 사유지 멤버 관리 GUI
 * - 현재 Trust된 플레이어 목록 표시
 * - 멤버 추가/제거 기능
 */
public class ClaimMemberGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Claim claim;
    private Inventory inventory;
    private final Map<Integer, UUID> slotToMember = new HashMap<>();

    // GUI 타이틀
    private static final String TITLE = "§8[ §b👥 멤버 관리 §8]";

    // 특수 슬롯
    private static final int SLOT_ADD_MEMBER = 49;
    private static final int SLOT_BACK = 45;

    // 멤버 표시 시작 슬롯
    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ClaimMemberGUI(WildCore plugin, Player player, Claim claim) {
        this.plugin = plugin;
        this.player = player;
        this.claim = claim;
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, TITLE);
        slotToMember.clear();

        // 테두리 채우기
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // 멤버 내용 영역 비우기
        for (int slot : MEMBER_SLOTS) {
            inventory.setItem(slot, null);
        }

        // 멤버 목록 표시
        ClaimManager claimManager = plugin.getClaimManager();
        Map<UUID, TrustType> members = claimManager.getTrustedPlayers(claim);

        int slotIndex = 0;
        for (Map.Entry<UUID, TrustType> entry : members.entrySet()) {
            if (slotIndex >= MEMBER_SLOTS.length)
                break;

            UUID memberUUID = entry.getKey();
            TrustType trustType = entry.getValue();
            int slot = MEMBER_SLOTS[slotIndex];

            inventory.setItem(slot, createMemberItem(memberUUID, trustType));
            slotToMember.put(slot, memberUUID);
            slotIndex++;
        }

        // 멤버 추가 버튼
        inventory.setItem(SLOT_ADD_MEMBER, createAddMemberButton());

        // 뒤로가기 버튼
        inventory.setItem(SLOT_BACK, createBackButton());
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemberItem(UUID uuid, TrustType trustType) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString().substring(0, 8);

        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§f" + playerName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7신뢰 레벨: " + getTrustTypeDisplay(trustType));
        lore.add("");
        lore.add("§e좌클릭: §7등급 변경");
        lore.add("§c쉬프트 + 우클릭: §7멤버 제거");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getTrustTypeDisplay(TrustType type) {
        return switch (type) {
            case ACCESS -> "§a출입";
            case CONTAINER -> "§e창고";
            case BUILD -> "§6건축";
            case MANAGER -> "§c관리자";
        };
    }

    private ItemStack createAddMemberButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l➕ 멤버 추가");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7새로운 플레이어를 사유지에");
        lore.add("§7멤버로 추가합니다.");
        lore.add("");
        lore.add("§e클릭하여 추가");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f◀ 뒤로가기");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7메인 관리 화면으로 돌아갑니다.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 멤버 등급 순환 변경
     */
    public void cycleMemberTrust(int slot) {
        UUID memberUUID = slotToMember.get(slot);
        if (memberUUID == null)
            return;

        ClaimManager claimManager = plugin.getClaimManager();
        TrustType currentType = claimManager.getPlayerTrustLevel(claim, memberUUID);

        if (currentType == null) {
            currentType = TrustType.ACCESS;
        }

        // 다음 등급으로 변경
        TrustType nextType = switch (currentType) {
            case ACCESS -> TrustType.CONTAINER;
            case CONTAINER -> TrustType.BUILD;
            case BUILD -> TrustType.MANAGER;
            case MANAGER -> TrustType.ACCESS;
        };

        claimManager.addTrust(claim, memberUUID, nextType);

        // GUI 새로고침
        createInventory();
        player.openInventory(inventory);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName()
                : memberUUID.toString().substring(0, 8);

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§f" + playerName + "§7님의 등급이 " + getTrustTypeDisplay(nextType) + "§7(으)로 변경되었습니다.");
    }

    /**
     * 멤버 제거
     */
    public void removeMember(int slot) {
        UUID memberUUID = slotToMember.get(slot);
        if (memberUUID == null)
            return;

        plugin.getClaimManager().removeTrust(claim, memberUUID);

        // GUI 새로고침
        createInventory();
        player.openInventory(inventory);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName()
                : memberUUID.toString().substring(0, 8);

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§f" + playerName + "§7님을 멤버에서 제거했습니다.");
    }

    public void open() {
        player.openInventory(inventory);
    }

    public boolean isBackSlot(int slot) {
        return slot == SLOT_BACK;
    }

    public boolean isAddMemberSlot(int slot) {
        return slot == SLOT_ADD_MEMBER;
    }

    public boolean isMemberSlot(int slot) {
        return slotToMember.containsKey(slot);
    }

    public Claim getClaim() {
        return claim;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
