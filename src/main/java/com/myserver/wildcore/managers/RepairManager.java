package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.RepairConfig;
import com.myserver.wildcore.util.ItemGroupUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.KoreanMaterialUtil;

/**
 * 수리 시스템 매니저
 * - 재료/돈 검증
 * - 내구도 수리 처리
 */
public class RepairManager {

    private final WildCore plugin;

    public RepairManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 수리 시도
     * 
     * @return true if successful, false otherwise
     */
    public boolean tryRepair(Player player, String repairId) {
        RepairConfig repair = plugin.getConfigManager().getRepairOption(repairId);
        if (repair == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c수리 옵션을 찾을 수 없습니다.");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        // 아이템 검증
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("repair.no_item"));
            return false;
        }

        // 내구도가 있는 아이템인지 확인
        if (!hasDurability(item)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("repair.no_durability"));
            return false;
        }

        // 대상 아이템 검증
        if (!isValidTarget(item, repair)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("repair.invalid_target"));
            return false;
        }

        // 이미 최대 내구도인지 확인
        if (isFullDurability(item)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("repair.already_full"));
            return false;
        }

        // 돈 확인
        double cost = repair.getCostMoney();
        if (!plugin.getEconomy().has(player, cost)) {
            String message = plugin.getConfigManager().getMessage("repair.no_money")
                    .replace("{cost}", String.format("%,.0f", cost));
            player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            return false;
        }

        // 재료 확인
        if (!hasRequiredItems(player, repair)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("repair.no_materials"));
            return false;
        }

        // 비용 차감
        plugin.getEconomy().withdrawPlayer(player, cost);
        removeRequiredItems(player, repair);

        // 수리 적용
        applyRepair(player, item, repair);

        // 성공 메시지 및 이펙트
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("repair.success"));
        playSuccessEffect(player);

        return true;
    }

    /**
     * 아이템에 내구도가 있는지 확인
     */
    public boolean hasDurability(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable;
    }

    /**
     * 아이템이 최대 내구도인지 확인
     */
    public boolean isFullDurability(ItemStack item) {
        if (!hasDurability(item)) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            return damageable.getDamage() <= 0;
        }
        return true;
    }

    /**
     * 현재 내구도 비율 반환 (0.0~1.0)
     */
    public double getDurabilityRatio(ItemStack item) {
        if (!hasDurability(item)) {
            return 1.0;
        }
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return 1.0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int damage = damageable.getDamage();
            return 1.0 - ((double) damage / maxDurability);
        }
        return 1.0;
    }

    /**
     * 대상 아이템 검증 (화이트리스트 + 그룹 기반)
     */
    public boolean isValidTarget(ItemStack item, RepairConfig repair) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        String materialName = item.getType().name();

        // 1. 화이트리스트 확인
        if (repair.hasWhitelist()) {
            if (repair.getTargetWhitelist().contains(materialName)) {
                return true;
            }
        }

        // 2. 그룹 확인
        if (repair.hasTargetGroups()) {
            for (String group : repair.getTargetGroups()) {
                if (ItemGroupUtil.isInGroup(item.getType(), group)) {
                    return true;
                }
            }
        }

        // 화이트리스트도 그룹도 없으면 모든 수리 가능한 아이템 허용
        if (!repair.hasWhitelist() && !repair.hasTargetGroups()) {
            return hasDurability(item);
        }

        return false;
    }

    /**
     * 필요 재료 보유 확인
     */
    public boolean hasRequiredItems(Player player, RepairConfig repair) {
        List<String> costItems = repair.getCostItems();
        if (costItems == null || costItems.isEmpty()) {
            return true;
        }

        for (String costEntry : costItems) {
            String[] parts = costEntry.split(":");

            // 커스텀 아이템: "custom:ID:수량"
            if (parts[0].equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                String customId = parts[1];
                int required = Integer.parseInt(parts[2]);
                int has = countCustomItemInInventory(player, customId);
                if (has < required) {
                    return false;
                }
            }
            // 바닐라 아이템: "MATERIAL:수량"
            else {
                Material mat = Material.matchMaterial(parts[0]);
                if (mat == null)
                    continue;
                int required = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
                if (!player.getInventory().containsAtLeast(new ItemStack(mat), required)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 필요 재료 제거
     */
    public void removeRequiredItems(Player player, RepairConfig repair) {
        List<String> costItems = repair.getCostItems();
        if (costItems == null || costItems.isEmpty()) {
            return;
        }

        for (String costEntry : costItems) {
            String[] parts = costEntry.split(":");

            if (parts[0].equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                String customId = parts[1];
                int amount = Integer.parseInt(parts[2]);
                removeCustomItemFromInventory(player, customId, amount);
            } else {
                Material mat = Material.matchMaterial(parts[0]);
                if (mat == null)
                    continue;
                int amount = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
                player.getInventory().removeItem(new ItemStack(mat, amount));
            }
        }
    }

    private int countCustomItemInInventory(Player player, String customId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (ItemUtil.isCustomItem(plugin, item, customId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeCustomItemFromInventory(Player player, String customId, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0)
                break;
            if (ItemUtil.isCustomItem(plugin, item, customId)) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
    }

    /**
     * 수리 적용
     */
    public void applyRepair(Player player, ItemStack item, RepairConfig repair) {
        if (!hasDurability(item)) {
            return;
        }

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int repairAmount = (int) (maxDurability * repair.getRepairPercentage());
            int newDamage = Math.max(0, currentDamage - repairAmount);

            damageable.setDamage(newDamage);
            item.setItemMeta(meta);

            plugin.debug("수리 적용: " + player.getName() +
                    " - " + item.getType().name() +
                    " [피해: " + currentDamage + " -> " + newDamage + "]");
        }
    }

    /**
     * 특정 아이템에 적용 가능한 수리 옵션 목록 반환
     */
    public List<RepairConfig> getAvailableRepairs(ItemStack item) {
        List<RepairConfig> result = new ArrayList<>();

        if (item == null || item.getType() == Material.AIR || !hasDurability(item)) {
            return result;
        }

        Map<String, RepairConfig> allRepairs = plugin.getConfigManager().getAllRepairOptions();
        for (RepairConfig repair : allRepairs.values()) {
            if (isValidTarget(item, repair)) {
                result.add(repair);
            }
        }

        return result;
    }

    /**
     * 비용 아이템 상태 목록 반환 (GUI 표시용)
     */
    public List<String> getCostItemStatus(Player player, RepairConfig repair) {
        List<String> result = new ArrayList<>();
        List<String> costItems = repair.getCostItems();

        if (costItems == null || costItems.isEmpty()) {
            return result;
        }

        for (String costEntry : costItems) {
            String[] parts = costEntry.split(":");

            if (parts[0].equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                String customId = parts[1];
                int required = Integer.parseInt(parts[2]);
                int has = countCustomItemInInventory(player, customId);

                String displayName = plugin.getConfigManager().getCustomItemDisplayName(customId);
                String color = has >= required ? "§a" : "§c";
                result.add("  " + color + "✦ " + displayName + " x" + required + " §7(" + has + "/" + required + ")");
            } else {
                Material mat = Material.matchMaterial(parts[0]);
                if (mat == null)
                    continue;
                int required = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
                int has = countMaterialInInventory(player, mat);

                String koreanName = KoreanMaterialUtil.getName(mat);
                String color = has >= required ? "§a" : "§c";
                result.add("  " + color + "✦ " + koreanName + " x" + required + " §7(" + has + "/" + required + ")");
            }
        }

        return result;
    }

    private int countMaterialInInventory(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 성공 이펙트
     */
    private void playSuccessEffect(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5,
                0);
    }

    /**
     * 리로드
     */
    public void reload() {
        plugin.debug("RepairManager 리로드 완료");
    }
}
