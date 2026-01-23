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

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 이름 설정 (Adventure API 사용)
            Component displayName = LegacyComponentSerializer.legacySection().deserialize(config.getDisplayName());
            meta.displayName(displayName);

            // Lore 설정 (Adventure API 사용)
            if (config.getLore() != null && !config.getLore().isEmpty()) {
                java.util.List<Component> loreComponents = config.getLore().stream()
                        .map(line -> (Component) LegacyComponentSerializer.legacySection().deserialize(line))
                        .toList();
                meta.lore(loreComponents);
            }

            // CustomModelData 설정
            if (config.getCustomModelData() > 0) {
                meta.setCustomModelData(config.getCustomModelData());
            }

            // 발광 효과
            if (config.isGlow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // PDC에 아이템 ID 저장 (악용 방지)
            NamespacedKey key = new NamespacedKey(plugin, WILDCORE_ITEM_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemId);

            item.setItemMeta(meta);
        }

        return item;
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
}
