package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 관리 Admin GUI
 * 모든 타입의 NPC 생성 및 관리
 */
public class NpcAdminGUI implements InventoryHolder {

        private final WildCore plugin;
        private final Player player;
        private Inventory inventory;

        // 슬롯 상수
        private static final int SLOT_HEADER = 4;

        // NPC 생성 슬롯 (상단)
        private static final int SLOT_CREATE_ENCHANT = 10;
        private static final int SLOT_CREATE_STOCK = 12;
        private static final int SLOT_CREATE_BANK = 14;
        private static final int SLOT_CREATE_WARP = 16;

        // NPC 목록 슬롯 (중단)
        private static final int SLOT_LIST_ALL = 22;

        // 타입별 목록 슬롯 (하단)
        private static final int SLOT_LIST_ENCHANT = 28;
        private static final int SLOT_LIST_STOCK = 30;
        private static final int SLOT_LIST_BANK = 32;
        private static final int SLOT_LIST_WARP = 34;

        // 액션 슬롯
        private static final int SLOT_BACK = 45;
        private static final int SLOT_REFRESH = 49;
        private static final int SLOT_DELETE_ALL = 53;

        public NpcAdminGUI(WildCore plugin, Player player) {
                this.plugin = plugin;
                this.player = player;
                createInventory();
        }

        private void createInventory() {
                inventory = Bukkit.createInventory(this, 54, ItemUtil.parse("§8[ §6NPC 관리 §8]"));
                updateInventory();
        }

        private void updateInventory() {
                // 배경 초기화
                ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
                for (int i = 0; i < 54; i++) {
                        inventory.setItem(i, background);
                }

                // 상단 장식
                ItemStack topBorder = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ", null);
                for (int i = 0; i < 9; i++) {
                        inventory.setItem(i, topBorder);
                }

                // === 헤더 ===
                int totalNpcs = plugin.getNpcManager().getNpcCount();
                inventory.setItem(SLOT_HEADER, createItem(Material.VILLAGER_SPAWN_EGG, "§6[ NPC 관리 ]",
                                List.of("",
                                                "§7WildCore NPC를 생성하고",
                                                "§7관리합니다.",
                                                "",
                                                "§7총 NPC 수: §e" + totalNpcs + "개")));

                // === NPC 생성 버튼 ===
                setupCreateButtons();

                // === NPC 목록 버튼 ===
                setupListButtons();

                // === 하단 액션 버튼 ===
                inventory.setItem(SLOT_BACK, createItem(Material.ARROW, "§7[ 뒤로 가기 ]",
                                List.of("", "§7메인 메뉴로 돌아갑니다.")));

                inventory.setItem(SLOT_REFRESH, createItem(Material.SUNFLOWER, "§e[ 새로고침 ]",
                                List.of("", "§7NPC 목록을 새로고침합니다.")));

                inventory.setItem(SLOT_DELETE_ALL, createItem(Material.TNT, "§c[ 전체 NPC 삭제 ]",
                                List.of("",
                                                "§7상점 외 모든 NPC를 삭제합니다.",
                                                "",
                                                "§c주의: 상점 NPC는 제외됩니다.",
                                                "",
                                                "§c우클릭하여 삭제")));
        }

        private void setupCreateButtons() {
                // 강화 NPC 생성
                inventory.setItem(SLOT_CREATE_ENCHANT, createNpcButton(NpcType.ENCHANT, true));

                // 주식 NPC 생성
                inventory.setItem(SLOT_CREATE_STOCK, createNpcButton(NpcType.STOCK, true));

                // 은행 NPC 생성
                inventory.setItem(SLOT_CREATE_BANK, createNpcButton(NpcType.BANK, true));

                // 이동 NPC 생성
                inventory.setItem(SLOT_CREATE_WARP, createNpcButton(NpcType.WARP, true));
        }

        private void setupListButtons() {
                // 전체 NPC 목록 버튼
                int totalCount = plugin.getNpcManager().getNpcCount();
                inventory.setItem(SLOT_LIST_ALL, createItem(Material.CHEST, "§f[ 전체 NPC 목록 ]",
                                List.of("",
                                                "§7모든 NPC를 확인합니다.",
                                                "",
                                                "§7총 NPC: §e" + totalCount + "개",
                                                "",
                                                "§e클릭하여 목록 보기")));

                // 타입별 목록 버튼
                inventory.setItem(SLOT_LIST_ENCHANT, createNpcButton(NpcType.ENCHANT, false));
                inventory.setItem(SLOT_LIST_STOCK, createNpcButton(NpcType.STOCK, false));
                inventory.setItem(SLOT_LIST_BANK, createNpcButton(NpcType.BANK, false));
                inventory.setItem(SLOT_LIST_WARP, createNpcButton(NpcType.WARP, false));
        }

        /**
         * NPC 타입별 버튼 생성
         * 
         * @param isCreate true면 생성 버튼, false면 목록 버튼
         */
        private ItemStack createNpcButton(NpcType type, boolean isCreate) {
                int count = plugin.getNpcManager().getNpcCount(type);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(type.getDescription());
                lore.add("");
                lore.add("§7현재: §e" + count + "개");
                lore.add("");

                if (isCreate) {
                        lore.add("§a클릭: §f현재 위치에 생성");
                        if (type.requiresTargetId()) {
                                lore.add("§7(대상 입력 필요)");
                        }
                } else {
                        lore.add("§e좌클릭: §f목록 보기");
                        lore.add("§c우클릭: §f전체 삭제");
                }

                String name = isCreate
                                ? type.getColorCode() + "[ " + type.getDisplayName() + " NPC 생성 ]"
                                : type.getColorCode() + "[ " + type.getDisplayName() + " 목록 ]";

                return createItem(type.getIcon(), name, lore);
        }

        // =====================
        // NPC 생성 메서드
        // =====================

        /**
         * 강화 NPC 생성
         */
        public void spawnEnchantNpc() {
                plugin.getNpcManager().createNpc(
                                NpcType.ENCHANT,
                                player.getLocation(),
                                NpcType.ENCHANT.getDefaultNpcName(),
                                null,
                                EntityType.VILLAGER,
                                player);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a강화 NPC가 생성되었습니다.");
                updateInventory();
        }

        /**
         * 주식 NPC 생성
         */
        public void spawnStockNpc() {
                plugin.getNpcManager().createNpc(
                                NpcType.STOCK,
                                player.getLocation(),
                                NpcType.STOCK.getDefaultNpcName(),
                                null,
                                EntityType.VILLAGER,
                                player);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a주식 NPC가 생성되었습니다.");
                updateInventory();
        }

        /**
         * 은행 NPC 생성
         */
        public void spawnBankNpc() {
                plugin.getNpcManager().createNpc(
                                NpcType.BANK,
                                player.getLocation(),
                                NpcType.BANK.getDefaultNpcName(),
                                null,
                                EntityType.VILLAGER,
                                player);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a은행 NPC가 생성되었습니다.");
                updateInventory();
        }

        /**
         * 이동 NPC 생성
         */
        public void spawnWarpNpc(String worldName) {
                plugin.getNpcManager().createNpc(
                                NpcType.WARP,
                                player.getLocation(),
                                NpcType.WARP.getDefaultNpcName(),
                                worldName,
                                EntityType.VILLAGER,
                                player);
                player.sendMessage(
                                plugin.getConfigManager().getPrefix() + "§a이동 NPC가 생성되었습니다. (목적지: " + worldName + ")");
                updateInventory();
        }

        /**
         * 특정 타입의 모든 NPC 제거
         */
        public void removeAllNpcs(NpcType type) {
                plugin.getNpcManager().removeAllTaggedNpcs(type);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                                "§c모든 " + type.getDisplayName() + " NPC가 제거되었습니다.");
                updateInventory();
        }

        /**
         * 상점 제외 모든 NPC 제거
         */
        public void removeAllNonShopNpcs() {
                removeAllNpcs(NpcType.ENCHANT);
                removeAllNpcs(NpcType.STOCK);
                removeAllNpcs(NpcType.BANK);
                removeAllNpcs(NpcType.WARP);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                                "§c상점 외 모든 NPC가 제거되었습니다.");
        }

        // =====================
        // 유틸리티 메서드
        // =====================

        private ItemStack createItem(Material material, String name, List<String> lore) {
                return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
        }

        public void open() {
                player.openInventory(inventory);
        }

        public void refresh() {
                updateInventory();
        }

        @Override
        public Inventory getInventory() {
                return inventory;
        }

        public Player getPlayer() {
                return player;
        }

        // =====================
        // 슬롯 확인 메서드
        // =====================

        public boolean isCreateEnchantSlot(int slot) {
                return slot == SLOT_CREATE_ENCHANT;
        }

        public boolean isCreateStockSlot(int slot) {
                return slot == SLOT_CREATE_STOCK;
        }

        public boolean isCreateBankSlot(int slot) {
                return slot == SLOT_CREATE_BANK;
        }

        public boolean isCreateWarpSlot(int slot) {
                return slot == SLOT_CREATE_WARP;
        }

        public boolean isListAllSlot(int slot) {
                return slot == SLOT_LIST_ALL;
        }

        public boolean isListEnchantSlot(int slot) {
                return slot == SLOT_LIST_ENCHANT;
        }

        public boolean isListStockSlot(int slot) {
                return slot == SLOT_LIST_STOCK;
        }

        public boolean isListBankSlot(int slot) {
                return slot == SLOT_LIST_BANK;
        }

        public boolean isListWarpSlot(int slot) {
                return slot == SLOT_LIST_WARP;
        }

        public boolean isBackSlot(int slot) {
                return slot == SLOT_BACK;
        }

        public boolean isRefreshSlot(int slot) {
                return slot == SLOT_REFRESH;
        }

        public boolean isDeleteAllSlot(int slot) {
                return slot == SLOT_DELETE_ALL;
        }
}
