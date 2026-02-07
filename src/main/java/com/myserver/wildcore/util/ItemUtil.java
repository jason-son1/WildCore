package com.myserver.wildcore.util;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.CustomItemConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 아이템 관련 유틸리티 클래스
 */
public class ItemUtil {

    private static final String WILDCORE_ITEM_KEY = "wildcore_item_id";

    /**
     * 커스텀 아이템 생성
     */
    public static ItemStack createCustomItem(WildCore plugin, String itemId, int amount) {
        CustomItemConfig config = plugin.getConfigManager().getCustomItem(itemId);
        if (config == null) {
            plugin.getLogger().warning("커스텀 아이템을 찾을 수 없음: " + itemId);
            return null;
        }

        Material material = Material.getMaterial(config.getMaterial());
        if (material == null) {
            material = Material.PAPER;
        }

        return createItem(material, config.getDisplayName(), config.getLore(), amount, itemId,
                config.getCustomModelData(), config.isGlow(), plugin);
    }

    /**
     * 기본 아이템 생성 (Adventure API 지원)
     */
    public static ItemStack createItem(Material material, String name, List<String> lore, int amount, String wildcoreId,
            int modelData, boolean glow, WildCore plugin) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.displayName(parse(name));
            }

            if (lore != null && !lore.isEmpty()) {
                meta.lore(parseList(lore));
            }

            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }

            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (wildcoreId != null && plugin != null) {
                NamespacedKey key = new NamespacedKey(plugin, WILDCORE_ITEM_KEY);
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, wildcoreId);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 레거시 문자열을 Component로 변환
     */
    public static Component parse(String legacyText) {
        if (legacyText == null)
            return Component.empty();

        // 색상 코드 변환 (& -> §)
        legacyText = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', legacyText);
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }

    /**
     * 레거시 문자열 리스트를 Component 리스트로 변환
     */
    public static List<Component> parseList(List<String> legacyLore) {
        if (legacyLore == null)
            return new ArrayList<>();
        return legacyLore.stream()
                .map(ItemUtil::parse)
                .toList();
    }

    /**
     * 커스텀 아이템인지 확인
     */
    public static boolean isCustomItem(WildCore plugin, ItemStack item, String expectedId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, WILDCORE_ITEM_KEY);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(key, PersistentDataType.STRING)) {
            return false;
        }

        String storedId = pdc.get(key, PersistentDataType.STRING);
        return expectedId.equals(storedId);
    }

    /**
     * 커스텀 아이템의 ID 가져오기
     */
    public static String getCustomItemId(WildCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, WILDCORE_ITEM_KEY);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(key, PersistentDataType.STRING)) {
            return null;
        }

        return pdc.get(key, PersistentDataType.STRING);
    }

    /**
     * 아이템이 WildCore 커스텀 아이템인지 확인 (ID 비교 없이 PDC 존재 여부만 확인)
     * 상점에서 바닐라 아이템 판매 시 커스텀 아이템을 필터링하는 데 사용
     */
    public static boolean isWildCoreCustomItem(WildCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, WILDCORE_ITEM_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    /**
     * 아이템의 기능 목록 가져오기
     */
    public static List<String> getFunctions(WildCore plugin, ItemStack item) {
        String itemId = getCustomItemId(plugin, item);
        if (itemId == null) {
            return new ArrayList<>();
        }

        CustomItemConfig config = plugin.getConfigManager().getCustomItem(itemId);
        if (config == null) {
            return new ArrayList<>();
        }

        return config.getFunctions();
    }

    /**
     * 아이템이 특정 기능을 가지고 있는지 확인
     */
    public static boolean hasFunction(WildCore plugin, ItemStack item, String function) {
        List<String> functions = getFunctions(plugin, item);
        return functions != null && functions.contains(function);
    }
}
