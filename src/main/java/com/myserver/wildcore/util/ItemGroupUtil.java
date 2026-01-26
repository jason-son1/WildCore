package com.myserver.wildcore.util;

import org.bukkit.Material;

import java.util.*;

/**
 * 아이템 그룹 분류 유틸리티
 * WEAPON, ARMOR, TOOL 등의 그룹으로 아이템 타입을 분류
 */
public class ItemGroupUtil {

    // 무기류
    private static final Set<Material> WEAPONS = EnumSet.of(
            Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.NETHERITE_SWORD,
            Material.GOLDEN_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD,
            Material.TRIDENT);

    // 갑옷류
    private static final Set<Material> ARMOR = EnumSet.of(
            // 헬멧
            Material.DIAMOND_HELMET, Material.IRON_HELMET, Material.NETHERITE_HELMET,
            Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET,
            Material.TURTLE_HELMET,
            // 갑옷
            Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.LEATHER_CHESTPLATE,
            // 레깅스
            Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS, Material.NETHERITE_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.LEATHER_LEGGINGS,
            // 부츠
            Material.DIAMOND_BOOTS, Material.IRON_BOOTS, Material.NETHERITE_BOOTS,
            Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS, Material.LEATHER_BOOTS);

    // 도구류
    private static final Set<Material> TOOLS = EnumSet.of(
            // 곡괭이
            Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE,
            // 삽
            Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL, Material.NETHERITE_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.STONE_SHOVEL, Material.WOODEN_SHOVEL,
            // 도끼
            Material.DIAMOND_AXE, Material.IRON_AXE, Material.NETHERITE_AXE,
            Material.GOLDEN_AXE, Material.STONE_AXE, Material.WOODEN_AXE,
            // 괭이
            Material.DIAMOND_HOE, Material.IRON_HOE, Material.NETHERITE_HOE,
            Material.GOLDEN_HOE, Material.STONE_HOE, Material.WOODEN_HOE);

    // 활류
    private static final Set<Material> BOWS = EnumSet.of(
            Material.BOW, Material.CROSSBOW);

    // 낚싯대
    private static final Set<Material> FISHING = EnumSet.of(
            Material.FISHING_ROD);

    // 삼지창
    private static final Set<Material> TRIDENT_SET = EnumSet.of(
            Material.TRIDENT);

    /**
     * 해당 아이템이 특정 그룹에 속하는지 확인
     */
    public static boolean isInGroup(Material material, String group) {
        if (material == null || group == null)
            return false;

        return switch (group.toUpperCase()) {
            case "WEAPON" -> WEAPONS.contains(material);
            case "ARMOR" -> ARMOR.contains(material);
            case "TOOL" -> TOOLS.contains(material);
            case "BOW" -> BOWS.contains(material);
            case "FISHING" -> FISHING.contains(material);
            case "TRIDENT" -> TRIDENT_SET.contains(material);
            case "ALL" -> true;
            default -> false;
        };
    }

    /**
     * 그룹에 속하는 모든 아이템 목록 반환
     */
    public static List<Material> getGroupMaterials(String group) {
        if (group == null)
            return Collections.emptyList();

        return switch (group.toUpperCase()) {
            case "WEAPON" -> new ArrayList<>(WEAPONS);
            case "ARMOR" -> new ArrayList<>(ARMOR);
            case "TOOL" -> new ArrayList<>(TOOLS);
            case "BOW" -> new ArrayList<>(BOWS);
            case "FISHING" -> new ArrayList<>(FISHING);
            case "TRIDENT" -> new ArrayList<>(TRIDENT_SET);
            default -> Collections.emptyList();
        };
    }

    /**
     * 모든 그룹 이름 목록 반환
     */
    public static List<String> getAllGroups() {
        return List.of("WEAPON", "ARMOR", "TOOL", "BOW", "FISHING", "TRIDENT", "ALL");
    }

    /**
     * 아이템 타입에 맞는 가장 적합한 그룹 반환
     */
    public static String getGroupForMaterial(Material material) {
        if (WEAPONS.contains(material))
            return "WEAPON";
        if (ARMOR.contains(material))
            return "ARMOR";
        if (TOOLS.contains(material))
            return "TOOL";
        if (BOWS.contains(material))
            return "BOW";
        if (FISHING.contains(material))
            return "FISHING";
        if (TRIDENT_SET.contains(material))
            return "TRIDENT";
        return "OTHER";
    }

    /**
     * 그룹 이름의 한글 표시명 반환
     */
    public static String getGroupDisplayName(String group) {
        if (group == null)
            return "알 수 없음";

        return switch (group.toUpperCase()) {
            case "WEAPON" -> "§c무기";
            case "ARMOR" -> "§9갑옷";
            case "TOOL" -> "§e도구";
            case "BOW" -> "§6활/석궁";
            case "FISHING" -> "§3낚싯대";
            case "TRIDENT" -> "§b삼지창";
            case "ALL" -> "§f전체";
            default -> "§7기타";
        };
    }

    /**
     * 그룹 아이콘 Material 반환 (GUI용)
     */
    public static Material getGroupIcon(String group) {
        if (group == null)
            return Material.BARRIER;

        return switch (group.toUpperCase()) {
            case "WEAPON" -> Material.DIAMOND_SWORD;
            case "ARMOR" -> Material.DIAMOND_CHESTPLATE;
            case "TOOL" -> Material.DIAMOND_PICKAXE;
            case "BOW" -> Material.BOW;
            case "FISHING" -> Material.FISHING_ROD;
            case "TRIDENT" -> Material.TRIDENT;
            case "ALL" -> Material.NETHER_STAR;
            default -> Material.BARRIER;
        };
    }

    /**
     * 인챈트 키를 기반으로 적절한 그룹을 반환합니다.
     */
    public static String getEnchantmentGroup(String enchantmentKey) {
        String key = enchantmentKey.toLowerCase();

        if (key.contains("sharpness") || key.contains("smite") ||
                key.contains("bane_of_arthropods") || key.contains("knockback") ||
                key.contains("fire_aspect") || key.contains("sweeping") ||
                key.contains("looting")) {
            return "WEAPON";
        }

        if (key.contains("protection") || key.contains("thorns") ||
                key.contains("respiration") || key.contains("aqua_affinity") ||
                key.contains("feather_falling") || key.contains("depth_strider") ||
                key.contains("frost_walker") || key.contains("soul_speed") ||
                key.contains("swift_sneak")) {
            return "ARMOR";
        }

        if (key.contains("efficiency") || key.contains("silk_touch") ||
                key.contains("fortune") || key.contains("unbreaking")) {
            return "TOOL";
        }

        if (key.contains("power") || key.contains("punch") ||
                key.contains("flame") || key.contains("infinity") ||
                key.contains("multishot") || key.contains("quick_charge") ||
                key.contains("piercing")) {
            return "BOW";
        }

        if (key.contains("luck_of_the_sea") || key.contains("lure")) {
            return "FISHING";
        }

        if (key.contains("loyalty") || key.contains("riptide") ||
                key.contains("channeling") || key.contains("impaling")) {
            return "TRIDENT";
        }

        return "OTHER"; // mending, vanishing_curse, binding_curse etc
    }
}
