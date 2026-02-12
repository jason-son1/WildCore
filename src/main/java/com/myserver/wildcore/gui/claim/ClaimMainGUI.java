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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 사유지 메인 관리 GUI
 * - 사유지 정보 표시
 * - 멤버 관리, 설정, 워프, 삭제 버튼
 */
public class ClaimMainGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Claim claim;
    private final ClaimMetadata metadata;
    private Inventory inventory;

    // GUI 타이틀
    private static final String TITLE = "§8[ §a🏡 사유지 관리 §8]";

    // 슬롯 배치 (3줄 = 27슬롯)
    private static final int SLOT_INFO = 11;
    private static final int SLOT_MEMBERS = 13;
    private static final int SLOT_SETTINGS = 15;
    private static final int SLOT_WARP = 20;
    private static final int SLOT_SET_HOME = 22;
    private static final int SLOT_DELETE = 24;

    public ClaimMainGUI(WildCore plugin, Player player, Claim claim) {
        this.plugin = plugin;
        this.player = player;
        this.claim = claim;
        this.metadata = plugin.getClaimDataManager().getClaimData(claim.getID());
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 27, TITLE);

        // 테두리 채우기
        ItemStack filler = createFillerItem();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // 정보 아이템
        inventory.setItem(SLOT_INFO, createInfoItem());

        // 멤버 관리 버튼
        inventory.setItem(SLOT_MEMBERS, createMembersButton());

        // 설정 버튼
        inventory.setItem(SLOT_SETTINGS, createSettingsButton());

        // 워프 버튼
        inventory.setItem(SLOT_WARP, createWarpButton());

        // 홈 설정 버튼
        inventory.setItem(SLOT_SET_HOME, createSetHomeButton());

        // 삭제 버튼
        inventory.setItem(SLOT_DELETE, createDeleteButton());
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();

        String nickname = metadata.getNickname();
        if (nickname == null || nickname.isEmpty()) {
            nickname = "나의 농장";
        }
        meta.setDisplayName("§a§l📋 " + nickname);

        List<String> lore = new ArrayList<>();
        lore.add("");

        // 위치 정보
        Location center = plugin.getClaimManager().getClaimCenter(claim);
        if (center != null) {
            lore.add("§7위치: §f" + center.getWorld().getName());
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

        lore.add("");
        lore.add("§e클릭하여 별명 변경");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMembersButton() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l👥 멤버 관리");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7사유지에 접근할 수 있는");
        lore.add("§7플레이어를 관리합니다.");
        lore.add("");

        // 현재 멤버 수
        int memberCount = plugin.getClaimManager().getTrustedPlayers(claim).size();
        lore.add("§7현재 멤버: §f" + memberCount + "명");

        lore.add("");
        lore.add("§e클릭하여 멤버 관리");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsButton() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l⚙️ 사유지 설정");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7사유지의 다양한 설정을");
        lore.add("§7변경할 수 있습니다.");
        lore.add("");

        // 현재 설정 상태 표시
        boolean blockEntry = plugin.getClaimDataManager().getClaimFlag(claim.getID(), "block_entry", false);
        lore.add("§7외부인 입장 차단: " + (blockEntry ? "§a켜짐" : "§c꺼짐"));

        lore.add("");
        lore.add("§e클릭하여 설정 변경");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createWarpButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l🚪 사유지 워프");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7설정된 홈 위치로");
        lore.add("§7텔레포트합니다.");
        lore.add("");

        Location home = metadata.getHome();
        if (home != null) {
            lore.add("§7홈 위치: §f" + home.getBlockX() + ", " + home.getBlockY() + ", " + home.getBlockZ());
        } else {
            lore.add("§c홈이 설정되지 않았습니다.");
        }

        lore.add("");
        lore.add("§e클릭하여 워프");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSetHomeButton() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l🏠 홈 설정");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7현재 위치를 사유지 홈으로");
        lore.add("§7설정합니다.");
        lore.add("");
        lore.add("§e클릭하여 현재 위치 저장");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDeleteButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l🗑️ 사유지 포기");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§c경고! 사유지를 완전히 삭제합니다.");
        lore.add("§c이 작업은 되돌릴 수 없습니다.");
        lore.add("");
        lore.add("§c쉬프트 + 클릭으로 삭제");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void refresh() {
        createInventory();
        player.openInventory(inventory);
    }

    // 슬롯 확인 메소드들
    public boolean isInfoSlot(int slot) {
        return slot == SLOT_INFO;
    }

    public boolean isMembersSlot(int slot) {
        return slot == SLOT_MEMBERS;
    }

    public boolean isSettingsSlot(int slot) {
        return slot == SLOT_SETTINGS;
    }

    public boolean isWarpSlot(int slot) {
        return slot == SLOT_WARP;
    }

    public boolean isSetHomeSlot(int slot) {
        return slot == SLOT_SET_HOME;
    }

    public boolean isDeleteSlot(int slot) {
        return slot == SLOT_DELETE;
    }

    public Claim getClaim() {
        return claim;
    }

    public ClaimMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
