package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Registry;

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
        EnchantConfig enchant = plugin.getConfigManager().getEnchant(enchantId);
        if (enchant == null) {
            return EnchantResult.INVALID;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        // 아이템 검증
        if (!isValidTarget(item, enchant)) {
            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant_invalid_item", replacements));
            return EnchantResult.INVALID;
        }

        // 돈 검증
        if (!plugin.getEconomy().has(player, enchant.getCostMoney())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", String.format("%,.0f", enchant.getCostMoney()));
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("insufficient_funds", replacements));
            return EnchantResult.INSUFFICIENT_FUNDS;
        }

        // 재료 검증
        if (!hasRequiredItems(player, enchant)) {
            return EnchantResult.INSUFFICIENT_ITEMS;
        }

        // 비용 차감
        plugin.getEconomy().withdrawPlayer(player, enchant.getCostMoney());
        removeRequiredItems(player, enchant);

        // 확률 계산
        double roll = random.nextDouble() * 100;
        double successThreshold = enchant.getSuccessRate();
        double failThreshold = successThreshold + enchant.getFailRate();

        EnchantResult result;

        if (roll < successThreshold) {
            // 성공
            result = applyEnchantment(player, item, enchant);
        } else if (roll < failThreshold) {
            // 실패 (재료만 소멸)
            result = EnchantResult.FAIL;
            playFailEffect(player);

            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant_fail", replacements));
        } else {
            // 파괴
            result = EnchantResult.DESTROY;
            item.setAmount(0);
            playDestroyEffect(player);

            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("enchant_destroy", replacements));
        }

        return result;
    }

    /**
     * 대상 아이템 검증
     */
    private boolean isValidTarget(ItemStack item, EnchantConfig enchant) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        String itemType = item.getType().name();
        return enchant.getTargetWhitelist().contains(itemType);
    }

    /**
     * 필요 재료 보유 확인
     */
    private boolean hasRequiredItems(Player player, EnchantConfig enchant) {
        for (String itemStr : enchant.getCostItems()) {
            String[] parts = itemStr.split(":");
            if (parts.length != 2)
                continue;

            Material material = Material.getMaterial(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);

            if (material == null || !player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("item", parts[0]);
                replacements.put("amount", parts[1]);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("insufficient_items", replacements));
                return false;
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
            if (parts.length != 2)
                continue;

            Material material = Material.getMaterial(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);

            if (material != null) {
                player.getInventory().removeItem(new ItemStack(material, amount));
            }
        }
    }

    /**
     * 인챈트 적용
     */
    private EnchantResult applyEnchantment(Player player, ItemStack item, EnchantConfig enchant) {
        String enchantName = enchant.getResultEnchantment().toLowerCase();
        int level = enchant.getResultLevel();

        // 인챈트 찾기
        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantName));

        if (enchantment == null) {
            plugin.getLogger().warning("유효하지 않은 인챈트: " + enchantName);
            return EnchantResult.INVALID;
        }

        // 인챈트 적용 (레벨 제한 무시)
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }

        playSuccessEffect(player);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("enchant", enchant.getDisplayName());
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("enchant_success", replacements));

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
    public enum EnchantResult {
        SUCCESS,
        FAIL,
        DESTROY,
        INVALID,
        INSUFFICIENT_FUNDS,
        INSUFFICIENT_ITEMS
    }
}
