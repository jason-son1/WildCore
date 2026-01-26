package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.util.ItemGroupUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 인챈트 시스템 매니저
 * - 재료/돈 검증
 * - 확률 계산
 * - 인챈트 적용/실패/파괴 처리
 */
public class EnchantManager {

    private final WildCore plugin;
    private final Random random = new Random();

    public EnchantManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 인챈트 시도
     * 
     * @return 결과 (SUCCESS, FAIL, DESTROY)
     */
    public EnchantResult tryEnchant(Player player, String enchantId) {
        EnchantProcess process = prepareEnchant(player, enchantId);
        if (process.result.isError()) {
            return process.result;
        }
        return executeEnchant(player, process);
    }

    /**
     * 인챈트 진행을 위한 사전 준비 및 결과 결정
     */
    public EnchantProcess prepareEnchant(Player player, String enchantId) {
        EnchantConfig enchant = plugin.getConfigManager().getEnchant(enchantId);
        if (enchant == null) {
            return new EnchantProcess(null, EnchantResult.INVALID);
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        // 아이템 검증
        if (!isValidTarget(item, enchant)) {
            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant.invalid_target", replacements));
            return new EnchantProcess(enchant, EnchantResult.INVALID);
        }

        // 돈 검증
        if (!plugin.getEconomy().has(player, enchant.getCostMoney())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", String.format("%,.0f", enchant.getCostMoney()));
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant.no_money", replacements));
            return new EnchantProcess(enchant, EnchantResult.INSUFFICIENT_FUNDS);
        }

        // 재료 검증
        if (!hasRequiredItems(player, enchant)) {
            return new EnchantProcess(enchant, EnchantResult.INSUFFICIENT_ITEMS);
        }

        // 확률 계산
        double roll = random.nextDouble() * 100;
        double successThreshold = enchant.getSuccessRate();
        double failThreshold = successThreshold + enchant.getFailRate();

        EnchantResult result;
        if (roll < successThreshold) {
            result = EnchantResult.SUCCESS;
        } else if (roll < failThreshold) {
            result = EnchantResult.FAIL;
        } else {
            result = EnchantResult.DESTROY;
        }

        return new EnchantProcess(enchant, result);
    }

    /**
     * 결정된 결과를 실제로 적용
     */
    public EnchantResult executeEnchant(Player player, EnchantProcess process) {
        EnchantConfig enchant = process.enchant;
        EnchantResult result = process.result;
        ItemStack item = player.getInventory().getItemInMainHand();

        // 최종 재검증 (아이템 바꿨을 가능성 대비)
        if (!isValidTarget(item, enchant) || !plugin.getEconomy().has(player, enchant.getCostMoney())
                || !hasRequiredItems(player, enchant)) {
            return EnchantResult.INVALID;
        }

        // 비용 차감
        plugin.getEconomy().withdrawPlayer(player, enchant.getCostMoney());
        removeRequiredItems(player, enchant);

        if (result == EnchantResult.SUCCESS) {
            applyEnchantment(player, item, enchant);
        } else if (result == EnchantResult.FAIL) {
            playFailEffect(player);
            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant.fail", replacements));
        } else if (result == EnchantResult.DESTROY) {
            item.setAmount(0);
            playDestroyEffect(player);
            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant.destroy", replacements));
        }

        return result;
    }

    public static class EnchantProcess {
        public final EnchantConfig enchant;
        public final EnchantResult result;

        public EnchantProcess(EnchantConfig enchant, EnchantResult result) {
            this.enchant = enchant;
            this.result = result;
        }
    }

    /**
     * 대상 아이템 검증 (화이트리스트 + 그룹 기반)
     */
    private boolean isValidTarget(ItemStack item, EnchantConfig enchant) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // Unsafe 모드일 경우 모든 아이템 허용
        if (enchant.isUnsafeMode()) {
            return true;
        }

        String itemType = item.getType().name();

        // 개별 화이트리스트 확인
        if (enchant.getTargetWhitelist().contains(itemType)) {
            return true;
        }

        // 그룹 기반 확인
        for (String group : enchant.getTargetGroups()) {
            if (ItemGroupUtil.isInGroup(item.getType(), group)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 필요 재료 보유 확인
     */
    private boolean hasRequiredItems(Player player, EnchantConfig enchant) {
        for (String itemStr : enchant.getCostItems()) {
            String[] parts = itemStr.split(":");
            if (parts.length < 2)
                continue;

            int amount = Integer.parseInt(parts[parts.length - 1]);
            String type = parts[0];

            if (type.equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                String customId = parts[1];
                if (countCustomItemInInventory(player, customId) < amount) {
                    com.myserver.wildcore.config.CustomItemConfig customItem = plugin.getConfigManager()
                            .getCustomItem(customId);
                    String displayName = (customItem != null) ? customItem.getDisplayName() : customId;

                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("item", displayName);
                    replacements.put("amount", String.valueOf(amount));
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("enchant.no_materials", replacements));
                    return false;
                }
            } else {
                Material material = Material.getMaterial(parts[0].toUpperCase());
                if (material == null || !player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("item", parts[0]);
                    replacements.put("amount", String.valueOf(amount));
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("enchant.no_materials", replacements));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 필요 재료 제거
     */
    private void removeRequiredItems(Player player, EnchantConfig enchant) {
        for (String itemStr : enchant.getCostItems()) {
            String[] parts = itemStr.split(":");
            if (parts.length < 2)
                continue;

            int amount = Integer.parseInt(parts[parts.length - 1]);
            String type = parts[0];

            if (type.equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                removeCustomItemFromInventory(player, parts[1], amount);
            } else {
                Material material = Material.getMaterial(parts[0].toUpperCase());
                if (material != null) {
                    player.getInventory().removeItem(new ItemStack(material, amount));
                }
            }
        }
    }

    private int countCustomItemInInventory(Player player, String customId) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && customId.equals(com.myserver.wildcore.util.ItemUtil.getCustomItemId(plugin, stack))) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeCustomItemFromInventory(Player player, String customId, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && customId.equals(com.myserver.wildcore.util.ItemUtil.getCustomItemId(plugin, stack))) {
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
     * 인챈트 적용
     * unsafe_mode가 true일 경우 addUnsafeEnchantment를 사용하여 모든 제한 우회
     */
    private EnchantResult applyEnchantment(Player player, ItemStack item, EnchantConfig enchant) {
        String enchantName = enchant.getResultEnchantment().toLowerCase();
        int level = enchant.getResultLevel();

        // 인챈트 찾기
        Enchantment enchantment = Bukkit.getRegistry(Enchantment.class).get(NamespacedKey.minecraft(enchantName));

        if (enchantment == null) {
            plugin.getLogger().warning("유효하지 않은 인챈트: " + enchantName);
            return EnchantResult.INVALID;
        }

        // Unsafe 모드 분기
        if (enchant.isUnsafeMode()) {
            // 아이템에 직접 비정상 인챈트 적용 (모든 제한 우회)
            // 레벨 제한, 아이템 타입 제한, 상충 인챈트 제한 모두 무시
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            // 기존 방식
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(enchantment, level, true);
                item.setItemMeta(meta);
            }
        }

        playSuccessEffect(player);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("enchant", enchant.getDisplayName());
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("enchant.success", replacements));

        return EnchantResult.SUCCESS;
    }

    /**
     * 성공 이펙트
     */
    private void playSuccessEffect(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * 실패 이펙트
     */
    private void playFailEffect(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05);
    }

    /**
     * 파괴 이펙트
     */
    private void playDestroyEffect(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
        player.spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
    }

    /**
     * 특정 아이템에 적용 가능한 인챈트 목록 반환
     */
    public List<EnchantConfig> getAvailableEnchants(ItemStack item) {
        List<EnchantConfig> available = new ArrayList<>();

        if (item == null || item.getType() == Material.AIR) {
            return available;
        }

        String itemType = item.getType().name();

        for (EnchantConfig enchant : plugin.getConfigManager().getEnchants().values()) {
            if (enchant.getTargetWhitelist().contains(itemType)) {
                available.add(enchant);
            }
        }

        return available;
    }

    /**
     * 리로드
     */
    public void reload() {
        // 설정은 ConfigManager에서 리로드되므로 추가 작업 불필요
        plugin.getLogger().info("인챈트 시스템 리로드 완료");
    }

    /**
     * 인챈트 결과 열거형
     */
    /**
     * 비용 아이템 상태 목록 반환 (GUI 표시용)
     */
    public List<String> getCostItemStatus(Player player, EnchantConfig enchant) {
        List<String> status = new ArrayList<>();

        for (String itemStr : enchant.getCostItems()) {
            String[] parts = itemStr.split(":");
            if (parts.length < 2)
                continue;

            int required = Integer.parseInt(parts[parts.length - 1]);
            String type = parts[0];
            String name = parts[0];
            int current = 0;

            if (type.equalsIgnoreCase("custom")) {
                if (parts.length < 3)
                    continue;
                String customId = parts[1];
                com.myserver.wildcore.config.CustomItemConfig customItem = plugin.getConfigManager()
                        .getCustomItem(customId);
                name = (customItem != null) ? customItem.getDisplayName() : customId;
                current = countCustomItemInInventory(player, customId);
            } else {
                Material material = Material.getMaterial(parts[0].toUpperCase());
                if (material != null) {
                    name = material.name();
                    // 바닐라 아이템 수량 체크
                    for (ItemStack stack : player.getInventory().getContents()) {
                        if (stack != null && stack.getType() == material) {
                            current += stack.getAmount();
                        }
                    }
                }
            }

            String color = (current >= required) ? "§a" : "§c";
            status.add("§7- " + name + ": " + color + current + "§7/" + required);
        }
        return status;
    }

    public enum EnchantResult {
        SUCCESS,
        FAIL,
        DESTROY,
        INVALID,
        INSUFFICIENT_FUNDS,
        INSUFFICIENT_ITEMS;

        public boolean isError() {
            return this == INVALID || this == INSUFFICIENT_FUNDS || this == INSUFFICIENT_ITEMS;
        }
    }
}
