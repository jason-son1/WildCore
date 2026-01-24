package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.config.ShopItemConfig;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.NpcTagUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 상점 시스템 매니저
 * NPC 생성/관리, 구매/판매 트랜잭션 처리
 */
public class ShopManager {

    private final WildCore plugin;

    // 엔티티 UUID -> 상점 ID 매핑 (빠른 조회용)
    private final Map<UUID, String> entityToShopMap = new HashMap<>();

    public ShopManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 상점 로드 및 NPC 스폰 (Wipe & Respawn)
     * 서버 리로드 시 기존 상점 NPC를 모두 제거하고 새로 생성합니다.
     */
    public void loadAllShops() {
        entityToShopMap.clear();

        // Wipe: 기존 상점 NPC 제거 (PDC 태그 기반)
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().removeAllTaggedNpcs(NpcType.SHOP);
        }

        // Respawn: 설정된 모든 상점 NPC 새로 생성
        for (ShopConfig shop : plugin.getConfigManager().getShops().values()) {
            if (shop.getLocation() != null) {
                spawnNPC(shop);
            }
        }

        plugin.getLogger().info("상점 NPC " + entityToShopMap.size() + "개 로드됨 (Wipe & Respawn)");
    }

    /**
     * UUID로 엔티티 찾기 (최적화: Bukkit.getEntity 직접 호출)
     */
    private Entity findEntityByUUID(UUID uuid) {
        if (uuid == null)
            return null;
        return Bukkit.getEntity(uuid);
    }

    /**
     * 상점 NPC 스폰
     */
    public Entity spawnNPC(ShopConfig shop) {
        Location loc = shop.getLocation();
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning("상점 위치가 유효하지 않음: " + shop.getId());
            return null;
        }

        Entity npc;

        if (shop.isArmorStand()) {
            // ArmorStand NPC
            npc = loc.getWorld().spawn(loc, ArmorStand.class, armorStand -> {
                armorStand.setVisible(true);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(plugin.getConfigManager().isShopNpcInvulnerable());
                armorStand.setSilent(plugin.getConfigManager().isShopNpcSilent());
                armorStand.setCustomNameVisible(true);
                armorStand.customName(ItemUtil.parse(shop.getDisplayName()));
                armorStand.setMarker(false); // 클릭 가능하게
                armorStand.setBasePlate(false);
                armorStand.setArms(true);
            });
        } else {
            // Villager NPC (기본값)
            npc = loc.getWorld().spawn(loc, Villager.class, villager -> {
                villager.setAI(!plugin.getConfigManager().isShopNpcNoAi());
                villager.setInvulnerable(plugin.getConfigManager().isShopNpcInvulnerable());
                villager.setSilent(plugin.getConfigManager().isShopNpcSilent());
                villager.setCollidable(false);
                villager.setCustomNameVisible(true);
                villager.customName(ItemUtil.parse(shop.getDisplayName()));
                villager.setProfession(Villager.Profession.NITWIT);
                villager.setVillagerType(Villager.Type.PLAINS);
            });
        }

        // PDC 태그 설정 (NPC 식별용)
        NpcTagUtil.setNpcTag(npc, NpcType.SHOP, shop.getId());

        // UUID 저장
        shop.setEntityUuid(npc.getUniqueId());
        entityToShopMap.put(npc.getUniqueId(), shop.getId());
        plugin.getConfigManager().saveShop(shop);

        plugin.debug("상점 NPC 스폰됨: " + shop.getId() + " (UUID: " + npc.getUniqueId() + ")");
        return npc;
    }

    /**
     * 상점 NPC 제거
     */
    public void removeNPC(ShopConfig shop) {
        if (shop.getEntityUuid() != null) {
            Entity entity = findEntityByUUID(shop.getEntityUuid());
            if (entity != null) {
                entity.remove();
            }
            entityToShopMap.remove(shop.getEntityUuid());
        }
    }

    /**
     * 새 상점 생성
     */
    public ShopConfig createShop(String shopId, String displayName, Location location, String npcType) {
        if (plugin.getConfigManager().getShop(shopId) != null) {
            return null; // 이미 존재
        }

        ShopConfig shop = new ShopConfig(
                shopId,
                displayName,
                npcType,
                location,
                null,
                new HashMap<>());

        // NPC 스폰
        spawnNPC(shop);

        return shop;
    }

    /**
     * 상점 삭제
     */
    public boolean deleteShop(String shopId) {
        ShopConfig shop = plugin.getConfigManager().getShop(shopId);
        if (shop == null) {
            return false;
        }

        // NPC 제거
        removeNPC(shop);

        // 설정 파일에서 삭제
        return plugin.getConfigManager().deleteShop(shopId);
    }

    /**
     * 엔티티 UUID로 상점 찾기
     */
    public ShopConfig getShopByEntityUUID(UUID uuid) {
        String shopId = entityToShopMap.get(uuid);
        if (shopId != null) {
            return plugin.getConfigManager().getShop(shopId);
        }
        return null;
    }

    /**
     * 아이템 구매
     */
    public boolean buyItem(Player player, ShopConfig shop, int slot, int amount) {
        ShopItemConfig item = shop.getItem(slot);
        if (item == null || !item.canBuy()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c이 아이템은 구매할 수 없습니다.");
            return false;
        }

        double totalCost = item.getBuyPrice() * amount;
        Economy economy = plugin.getEconomy();

        // 잔액 확인
        if (!economy.has(player, totalCost)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c잔액이 부족합니다. (필요: §6" + String.format("%,.0f", totalCost) + "원§c)");
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        // 인벤토리 공간 확인
        ItemStack itemStack = createItemStack(item, amount);
        if (itemStack == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c아이템을 생성할 수 없습니다.");
            return false;
        }

        if (!hasInventorySpace(player, itemStack)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c인벤토리 공간이 부족합니다.");
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        // 거래 실행
        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (response.transactionSuccess()) {
            player.getInventory().addItem(itemStack);
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§a구매 완료! §7(§6-" + String.format("%,.0f", totalCost) + "원§7)");
            plugin.getConfigManager().playSound(player, "buy");

            plugin.debug("상점 구매: " + player.getName() + " - " + item.getId() + " x" + amount +
                    " (" + shop.getId() + ")");
            return true;
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c거래에 실패했습니다.");
            return false;
        }
    }

    /**
     * 아이템 판매
     */
    public boolean sellItem(Player player, ShopConfig shop, int slot, int amount) {
        ShopItemConfig item = shop.getItem(slot);
        if (item == null || !item.canSell()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c이 아이템은 판매할 수 없습니다.");
            return false;
        }

        // 전체 인벤토리에서 아이템 수량 확인
        int totalInInventory = countItemInInventory(player, item);
        if (totalInInventory < amount) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c보유한 아이템이 부족합니다.");
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        double totalPrice = item.getSellPrice() * amount;
        Economy economy = plugin.getEconomy();

        // 아이템 제거
        removeItemFromInventory(player, item, amount);

        // 돈 지급
        EconomyResponse response = economy.depositPlayer(player, totalPrice);
        if (response.transactionSuccess()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§a판매 완료! §7(§6+" + String.format("%,.0f", totalPrice) + "원§7)");
            plugin.getConfigManager().playSound(player, "sell");

            plugin.debug("상점 판매: " + player.getName() + " - " + item.getId() + " x" + amount +
                    " (" + shop.getId() + ")");
            return true;
        } else {
            // 롤백: 아이템 복구
            ItemStack itemStack = createItemStack(item, amount);
            if (itemStack != null) {
                player.getInventory().addItem(itemStack);
            }
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c거래에 실패했습니다.");
            return false;
        }
    }

    /**
     * 전량 판매
     */
    public boolean sellAllItems(Player player, ShopConfig shop, int slot) {
        ShopItemConfig item = shop.getItem(slot);
        if (item == null || !item.canSell()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c이 아이템은 판매할 수 없습니다.");
            return false;
        }

        int totalAmount = countItemInInventory(player, item);
        if (totalAmount == 0) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c보유한 아이템이 없습니다.");
            return false;
        }

        return sellItem(player, shop, slot, totalAmount);
    }

    /**
     * ItemStack 생성
     */
    private ItemStack createItemStack(ShopItemConfig item, int amount) {
        if (item.isCustom()) {
            // 커스텀 아이템
            ItemStack customItem = ItemUtil.createCustomItem(plugin, item.getId(), amount);
            return customItem;
        } else {
            // 바닐라 아이템
            try {
                Material material = Material.valueOf(item.getId().toUpperCase());
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("잘못된 Material: " + item.getId());
                return null;
            }
        }
    }

    /**
     * 인벤토리 공간 확인
     */
    private boolean hasInventorySpace(Player player, ItemStack item) {
        return player.getInventory().firstEmpty() != -1 ||
                player.getInventory().containsAtLeast(item, 1);
    }

    /**
     * 인벤토리에서 아이템 개수 세기
     */
    private int countItemInInventory(Player player, ShopItemConfig item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && matchesItem(stack, item)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    /**
     * 인벤토리에서 아이템 제거
     */
    private void removeItemFromInventory(Player player, ShopItemConfig item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && matchesItem(stack, item)) {
                int toRemove = Math.min(remaining, stack.getAmount());
                if (toRemove >= stack.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - toRemove);
                }
                remaining -= toRemove;
            }
        }
    }

    /**
     * 아이템 매칭 확인
     */
    private boolean matchesItem(ItemStack stack, ShopItemConfig item) {
        if (item.isCustom()) {
            // 커스텀 아이템 ID 비교
            String customId = ItemUtil.getCustomItemId(plugin, stack);
            return item.getId().equals(customId);
        } else {
            // 바닐라 아이템 Material 비교
            try {
                Material material = Material.valueOf(item.getId().toUpperCase());
                return stack.getType() == material;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * 상점 위치 이동
     */
    public void moveShop(ShopConfig shop, Location newLocation) {
        // 기존 NPC 제거
        removeNPC(shop);

        // 새 위치 설정
        shop.setLocation(newLocation);

        // 새 NPC 스폰
        spawnNPC(shop);
    }

    /**
     * 상점 NPC 타입 변경
     */
    public void changeNpcType(ShopConfig shop, String newType) {
        // 기존 NPC 제거
        removeNPC(shop);

        // 타입 변경
        shop.setNpcType(newType);

        // 새 NPC 스폰
        spawnNPC(shop);
    }

    /**
     * 모든 상점 저장 (플러그인 종료 시)
     */
    public void saveAllShops() {
        for (ShopConfig shop : plugin.getConfigManager().getShops().values()) {
            plugin.getConfigManager().saveShop(shop);
        }
        plugin.getLogger().info("모든 상점 데이터가 저장되었습니다.");
    }

    /**
     * 리로드
     */
    public void reload() {
        // 기존 NPC UUID 매핑 클리어
        entityToShopMap.clear();

        // 다시 로드
        loadAllShops();
    }
}
