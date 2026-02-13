package com.myserver.wildcore.claim;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사유지 설정 플래그 정의
 * GriefPrevention/GPFlags 스타일의 상세한 보호 설정을 제공합니다.
 */
public enum ClaimFlags {

        // =====================
        // 일반 설정 (GENERAL)
        // =====================
        BLOCK_ENTRY("block_entry", "외부인 입장 차단", Category.GENERAL,
                        Material.IRON_DOOR, false,
                        "신뢰하지 않은 플레이어가", "사유지에 들어오는 것을 막습니다."),

        PVP("pvp", "PvP 허용", Category.GENERAL,
                        Material.DIAMOND_SWORD, false,
                        "사유지 내에서 플레이어 간", "전투를 허용합니다."),

        MOB_SPAWN("mob_spawn", "몬스터 스폰", Category.GENERAL,
                        Material.ZOMBIE_HEAD, true,
                        "사유지 내에서 몬스터가", "스폰되는 것을 허용합니다."),

        HOSTILE_DAMAGE("hostile_damage", "적대적 몹 피해", Category.GENERAL,
                        Material.SKELETON_SKULL, true,
                        "사유지 내에서 적대적 몹이", "플레이어에게 피해를 줄 수 있습니다."),

        FIRE_SPREAD("fire_spread", "불 번짐", Category.GENERAL,
                        Material.FLINT_AND_STEEL, false,
                        "사유지 내에서 불이", "다른 블록으로 번지는 것을 허용합니다."),

        EXPLOSIONS("explosions", "폭발 피해", Category.GENERAL,
                        Material.TNT, false,
                        "사유지 내에서 폭발(TNT, 크리퍼 등)이", "블록을 파괴하는 것을 허용합니다."),

        ENDERMAN_GRIEF("enderman_grief", "엔더맨 그리핑", Category.GENERAL,
                        Material.ENDER_PEARL, false,
                        "엔더맨이 사유지 내 블록을", "집거나 놓는 것을 허용합니다."),

        MOB_ENTRY("mob_entry", "몬스터 입장 차단", Category.GENERAL,
                        Material.SHIELD, false,
                        "적대적 몬스터가 사유지 안으로", "들어오는 것을 차단합니다."),

        // =====================
        // 농경 설정 (FARMING)
        // =====================
        CROP_TRAMPLE("crop_trample", "농작물 밟기", Category.FARMING,
                        Material.FARMLAND, false,
                        "플레이어나 몹이 농작물을", "밟아서 파괴하는 것을 허용합니다."),

        CROP_GROWTH("crop_growth", "농작물 성장", Category.FARMING,
                        Material.WHEAT, true,
                        "사유지 내에서 농작물이", "자라는 것을 허용합니다."),

        ANIMAL_SPAWN("animal_spawn", "동물 스폰", Category.FARMING,
                        Material.PIG_SPAWN_EGG, true,
                        "사유지 내에서 동물이", "자연적으로 스폰되는 것을 허용합니다."),

        ANIMAL_DAMAGE("animal_damage", "동물 피해", Category.FARMING,
                        Material.BEEF, false,
                        "외부인이 사유지 내 동물을", "공격하는 것을 허용합니다."),

        FISHING("fishing", "낚시", Category.FARMING,
                        Material.FISHING_ROD, true,
                        "사유지 내에서 낚시를", "할 수 있도록 허용합니다."),

        // =====================
        // 상호작용 설정 (INTERACTION)
        // =====================
        CONTAINER_ACCESS("container_access", "상자 접근", Category.INTERACTION,
                        Material.CHEST, false,
                        "외부인이 상자, 화로 등", "컨테이너에 접근하는 것을 허용합니다."),

        BUTTON_LEVER("button_lever", "버튼/레버 사용", Category.INTERACTION,
                        Material.LEVER, false,
                        "외부인이 버튼, 레버 등", "레드스톤 장치를 사용하는 것을 허용합니다."),

        DOOR_ACCESS("door_access", "문 사용", Category.INTERACTION,
                        Material.OAK_DOOR, false,
                        "외부인이 문, 울타리 문 등을", "열고 닫는 것을 허용합니다."),

        VEHICLE_USE("vehicle_use", "탈것 사용", Category.INTERACTION,
                        Material.MINECART, true,
                        "외부인이 마인카트, 보트 등", "탈것을 사용하는 것을 허용합니다."),

        ITEM_DROP("item_drop", "아이템 드롭", Category.INTERACTION,
                        Material.DROPPER, true,
                        "사유지 내에서 아이템을", "드롭할 수 있도록 허용합니다."),

        ITEM_PICKUP("item_pickup", "아이템 줍기", Category.INTERACTION,
                        Material.HOPPER, true,
                        "사유지 내에서 아이템을", "주울 수 있도록 허용합니다."),

        // =====================
        // 환경 설정 (ENVIRONMENT)
        // =====================
        LEAF_DECAY("leaf_decay", "나뭇잎 분해", Category.ENVIRONMENT,
                        Material.OAK_LEAVES, true,
                        "사유지 내에서 나뭇잎이", "자연적으로 분해되는 것을 허용합니다."),

        SNOW_FALL("snow_fall", "눈 쌓임", Category.ENVIRONMENT,
                        Material.SNOW_BLOCK, true,
                        "사유지 내에서 눈이", "쌓이는 것을 허용합니다."),

        ICE_FORM("ice_form", "얼음 생성", Category.ENVIRONMENT,
                        Material.ICE, true,
                        "사유지 내에서 물이 얼어", "얼음이 생성되는 것을 허용합니다."),

        VINE_GROWTH("vine_growth", "덩굴 성장", Category.ENVIRONMENT,
                        Material.VINE, true,
                        "사유지 내에서 덩굴, 켈프 등이", "자라는 것을 허용합니다.");

        // =====================
        // Fields
        // =====================
        private final String key;
        private final String displayName;
        private final Category category;
        private final Material icon;
        private final boolean defaultValue;
        private final String[] description;

        ClaimFlags(String key, String displayName, Category category,
                        Material icon, boolean defaultValue, String... description) {
                this.key = key;
                this.displayName = displayName;
                this.category = category;
                this.icon = icon;
                this.defaultValue = defaultValue;
                this.description = description;
        }

        public String getKey() {
                return key;
        }

        public String getDisplayName() {
                return displayName;
        }

        public Category getCategory() {
                return category;
        }

        public Material getIcon() {
                return icon;
        }

        public boolean getDefaultValue() {
                return defaultValue;
        }

        public String[] getDescription() {
                return description;
        }

        /**
         * 키 값으로 플래그를 찾습니다.
         */
        public static ClaimFlags fromKey(String key) {
                for (ClaimFlags flag : values()) {
                        if (flag.key.equals(key)) {
                                return flag;
                        }
                }
                return null;
        }

        /**
         * 카테고리에 속한 모든 플래그를 반환합니다.
         */
        public static List<ClaimFlags> getByCategory(Category category) {
                return Arrays.stream(values())
                                .filter(flag -> flag.category == category)
                                .collect(Collectors.toList());
        }

        /**
         * 플래그 카테고리 정의
         */
        public enum Category {
                GENERAL("일반 설정", Material.REDSTONE, "§c🔧"),
                FARMING("농경 설정", Material.WHEAT, "§e🌾"),
                INTERACTION("상호작용 설정", Material.CHEST, "§b🎮"),
                ENVIRONMENT("환경 설정", Material.GRASS_BLOCK, "§a🌍");

                private final String displayName;
                private final Material icon;
                private final String prefix;

                Category(String displayName, Material icon, String prefix) {
                        this.displayName = displayName;
                        this.icon = icon;
                        this.prefix = prefix;
                }

                public String getDisplayName() {
                        return displayName;
                }

                public Material getIcon() {
                        return icon;
                }

                public String getPrefix() {
                        return prefix;
                }
        }
}
