package com.myserver.wildcore.util;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Material 이름을 한글로 변환하는 유틸리티 클래스
 */
public class KoreanMaterialUtil {

    private static final Map<Material, String> NAME_MAP = new HashMap<>();

    static {
        // 광물 & 원석
        NAME_MAP.put(Material.DIAMOND, "다이아몬드");
        NAME_MAP.put(Material.IRON_INGOT, "철 주괴");
        NAME_MAP.put(Material.GOLD_INGOT, "금 주괴");
        NAME_MAP.put(Material.COPPER_INGOT, "구리 주괴");
        NAME_MAP.put(Material.NETHERITE_INGOT, "네더라이트 주괴");
        NAME_MAP.put(Material.EMERALD, "에메랄드");
        NAME_MAP.put(Material.LAPIS_LAZULI, "청금석");
        NAME_MAP.put(Material.REDSTONE, "레드스톤");
        NAME_MAP.put(Material.COAL, "석탄");
        NAME_MAP.put(Material.CHARCOAL, "목탄");
        NAME_MAP.put(Material.QUARTZ, "네더 석영");
        NAME_MAP.put(Material.AMETHYST_SHARD, "자수정 조각");
        NAME_MAP.put(Material.RAW_IRON, "철 원석");
        NAME_MAP.put(Material.RAW_GOLD, "금 원석");
        NAME_MAP.put(Material.RAW_COPPER, "구리 원석");

        // 도구 - 검
        NAME_MAP.put(Material.WOODEN_SWORD, "나무 검");
        NAME_MAP.put(Material.STONE_SWORD, "돌 검");
        NAME_MAP.put(Material.IRON_SWORD, "철 검");
        NAME_MAP.put(Material.GOLDEN_SWORD, "금 검");
        NAME_MAP.put(Material.DIAMOND_SWORD, "다이아몬드 검");
        NAME_MAP.put(Material.NETHERITE_SWORD, "네더라이트 검");

        // 도구 - 곡괭이
        NAME_MAP.put(Material.WOODEN_PICKAXE, "나무 곡괭이");
        NAME_MAP.put(Material.STONE_PICKAXE, "돌 곡괭이");
        NAME_MAP.put(Material.IRON_PICKAXE, "철 곡괭이");
        NAME_MAP.put(Material.GOLDEN_PICKAXE, "금 곡괭이");
        NAME_MAP.put(Material.DIAMOND_PICKAXE, "다이아몬드 곡괭이");
        NAME_MAP.put(Material.NETHERITE_PICKAXE, "네더라이트 곡괭이");

        // 도구 - 도끼
        NAME_MAP.put(Material.WOODEN_AXE, "나무 도끼");
        NAME_MAP.put(Material.STONE_AXE, "돌 도끼");
        NAME_MAP.put(Material.IRON_AXE, "철 도끼");
        NAME_MAP.put(Material.GOLDEN_AXE, "금 도끼");
        NAME_MAP.put(Material.DIAMOND_AXE, "다이아몬드 도끼");
        NAME_MAP.put(Material.NETHERITE_AXE, "네더라이트 도끼");

        // 도구 - 삽
        NAME_MAP.put(Material.WOODEN_SHOVEL, "나무 삽");
        NAME_MAP.put(Material.STONE_SHOVEL, "돌 삽");
        NAME_MAP.put(Material.IRON_SHOVEL, "철 삽");
        NAME_MAP.put(Material.GOLDEN_SHOVEL, "금 삽");
        NAME_MAP.put(Material.DIAMOND_SHOVEL, "다이아몬드 삽");
        NAME_MAP.put(Material.NETHERITE_SHOVEL, "네더라이트 삽");

        // 도구 - 괭이
        NAME_MAP.put(Material.WOODEN_HOE, "나무 괭이");
        NAME_MAP.put(Material.STONE_HOE, "돌 괭이");
        NAME_MAP.put(Material.IRON_HOE, "철 괭이");
        NAME_MAP.put(Material.GOLDEN_HOE, "금 괭이");
        NAME_MAP.put(Material.DIAMOND_HOE, "다이아몬드 괭이");
        NAME_MAP.put(Material.NETHERITE_HOE, "네더라이트 괭이");

        // 방어구 - 헬멧
        NAME_MAP.put(Material.LEATHER_HELMET, "가죽 모자");
        NAME_MAP.put(Material.CHAINMAIL_HELMET, "사슬 투구");
        NAME_MAP.put(Material.IRON_HELMET, "철 투구");
        NAME_MAP.put(Material.GOLDEN_HELMET, "금 투구");
        NAME_MAP.put(Material.DIAMOND_HELMET, "다이아몬드 투구");
        NAME_MAP.put(Material.NETHERITE_HELMET, "네더라이트 투구");
        NAME_MAP.put(Material.TURTLE_HELMET, "거북 등껍질");

        // 방어구 - 흉갑
        NAME_MAP.put(Material.LEATHER_CHESTPLATE, "가죽 조끼");
        NAME_MAP.put(Material.CHAINMAIL_CHESTPLATE, "사슬 흉갑");
        NAME_MAP.put(Material.IRON_CHESTPLATE, "철 흉갑");
        NAME_MAP.put(Material.GOLDEN_CHESTPLATE, "금 흉갑");
        NAME_MAP.put(Material.DIAMOND_CHESTPLATE, "다이아몬드 흉갑");
        NAME_MAP.put(Material.NETHERITE_CHESTPLATE, "네더라이트 흉갑");

        // 방어구 - 레깅스
        NAME_MAP.put(Material.LEATHER_LEGGINGS, "가죽 바지");
        NAME_MAP.put(Material.CHAINMAIL_LEGGINGS, "사슬 레깅스");
        NAME_MAP.put(Material.IRON_LEGGINGS, "철 레깅스");
        NAME_MAP.put(Material.GOLDEN_LEGGINGS, "금 레깅스");
        NAME_MAP.put(Material.DIAMOND_LEGGINGS, "다이아몬드 레깅스");
        NAME_MAP.put(Material.NETHERITE_LEGGINGS, "네더라이트 레깅스");

        // 방어구 - 부츠
        NAME_MAP.put(Material.LEATHER_BOOTS, "가죽 장화");
        NAME_MAP.put(Material.CHAINMAIL_BOOTS, "사슬 부츠");
        NAME_MAP.put(Material.IRON_BOOTS, "철 부츠");
        NAME_MAP.put(Material.GOLDEN_BOOTS, "금 부츠");
        NAME_MAP.put(Material.DIAMOND_BOOTS, "다이아몬드 부츠");
        NAME_MAP.put(Material.NETHERITE_BOOTS, "네더라이트 부츠");

        // 활 & 기타 무기
        NAME_MAP.put(Material.BOW, "활");
        NAME_MAP.put(Material.CROSSBOW, "쇠뇌");
        NAME_MAP.put(Material.ARROW, "화살");
        NAME_MAP.put(Material.SPECTRAL_ARROW, "분광 화살");
        NAME_MAP.put(Material.TIPPED_ARROW, "효과부여 화살");
        NAME_MAP.put(Material.TRIDENT, "삼지창");
        NAME_MAP.put(Material.SHIELD, "방패");
        NAME_MAP.put(Material.TOTEM_OF_UNDYING, "불사의 토템");

        // 식량
        NAME_MAP.put(Material.APPLE, "사과");
        NAME_MAP.put(Material.GOLDEN_APPLE, "황금 사과");
        NAME_MAP.put(Material.ENCHANTED_GOLDEN_APPLE, "마법이 부여된 황금 사과");
        NAME_MAP.put(Material.BREAD, "빵");
        NAME_MAP.put(Material.COOKED_PORKCHOP, "익힌 돼지고기");
        NAME_MAP.put(Material.PORKCHOP, "익히지 않은 돼지고기");
        NAME_MAP.put(Material.COOKED_BEEF, "스테이크");
        NAME_MAP.put(Material.BEEF, "익히지 않은 소고기");
        NAME_MAP.put(Material.COOKED_CHICKEN, "익힌 닭고기");
        NAME_MAP.put(Material.CHICKEN, "익히지 않은 닭고기");
        NAME_MAP.put(Material.COOKED_MUTTON, "익힌 양고기");
        NAME_MAP.put(Material.MUTTON, "익히지 않은 양고기");
        NAME_MAP.put(Material.COOKED_RABBIT, "익힌 토끼고기");
        NAME_MAP.put(Material.RABBIT, "익히지 않은 토끼고기");
        NAME_MAP.put(Material.COD, "대구");
        NAME_MAP.put(Material.COOKED_COD, "익힌 대구");
        NAME_MAP.put(Material.SALMON, "연어");
        NAME_MAP.put(Material.COOKED_SALMON, "익힌 연어");
        NAME_MAP.put(Material.CARROT, "당근");
        NAME_MAP.put(Material.POTATO, "감자");
        NAME_MAP.put(Material.BAKED_POTATO, "구운 감자");
        NAME_MAP.put(Material.MELON_SLICE, "수박 조각");
        NAME_MAP.put(Material.SWEET_BERRIES, "달콤한 열매");
        NAME_MAP.put(Material.GLOW_BERRIES, "발광 열매");
        NAME_MAP.put(Material.CAKE, "케이크");
        NAME_MAP.put(Material.COOKIE, "쿠키");

        // 블록 - 나무
        NAME_MAP.put(Material.OAK_LOG, "참나무 원목");
        NAME_MAP.put(Material.SPRUCE_LOG, "가문비나무 원목");
        NAME_MAP.put(Material.BIRCH_LOG, "자작나무 원목");
        NAME_MAP.put(Material.JUNGLE_LOG, "정글나무 원목");
        NAME_MAP.put(Material.ACACIA_LOG, "아카시아나무 원목");
        NAME_MAP.put(Material.DARK_OAK_LOG, "짙은 참나무 원목");
        NAME_MAP.put(Material.MANGROVE_LOG, "맹그로브 원목");
        NAME_MAP.put(Material.CHERRY_LOG, "벚나무 원목");

        NAME_MAP.put(Material.OAK_PLANKS, "참나무 판자");
        NAME_MAP.put(Material.SPRUCE_PLANKS, "가문비나무 판자");
        NAME_MAP.put(Material.BIRCH_PLANKS, "자작나무 판자");
        NAME_MAP.put(Material.JUNGLE_PLANKS, "정글나무 판자");
        NAME_MAP.put(Material.ACACIA_PLANKS, "아카시아나무 판자");
        NAME_MAP.put(Material.DARK_OAK_PLANKS, "짙은 참나무 판자");

        // 블록 - 돌/건축
        NAME_MAP.put(Material.STONE, "돌");
        NAME_MAP.put(Material.COBBLESTONE, "조약돌");
        NAME_MAP.put(Material.DEEPSLATE, "심층암");
        NAME_MAP.put(Material.COBBLED_DEEPSLATE, "심층암 조약돌");
        NAME_MAP.put(Material.GRANITE, "화강암");
        NAME_MAP.put(Material.DIORITE, "섬록암");
        NAME_MAP.put(Material.ANDESITE, "안산암");
        NAME_MAP.put(Material.BRICKS, "벽돌");
        NAME_MAP.put(Material.SAND, "모래");
        NAME_MAP.put(Material.RED_SAND, "붉은 모래");
        NAME_MAP.put(Material.GRAVEL, "자갈");
        NAME_MAP.put(Material.GLASS, "유리");
        NAME_MAP.put(Material.OBSIDIAN, "흑曜석");
        NAME_MAP.put(Material.CRYING_OBSIDIAN, "우는 흑요석");
        NAME_MAP.put(Material.DIRT, "흙");
        NAME_MAP.put(Material.GRASS_BLOCK, "잔디 블록");

        // 기타 아이템
        NAME_MAP.put(Material.STICK, "막대기");
        NAME_MAP.put(Material.BONE, "뼈");
        NAME_MAP.put(Material.STRING, "실");
        NAME_MAP.put(Material.FEATHER, "깃털");
        NAME_MAP.put(Material.GUNPOWDER, "화약");
        NAME_MAP.put(Material.LEATHER, "가죽");
        NAME_MAP.put(Material.PAPER, "종이");
        NAME_MAP.put(Material.BOOK, "책");
        NAME_MAP.put(Material.ENCHANTED_BOOK, "마법이 부여된 책");
        NAME_MAP.put(Material.EXPERIENCE_BOTTLE, "경험치 병");
        NAME_MAP.put(Material.ENDER_PEARL, "엔더 진주");
        NAME_MAP.put(Material.ENDER_EYE, "엔더의 눈");
        NAME_MAP.put(Material.FIREWORK_ROCKET, "폭죽 로켓");
        NAME_MAP.put(Material.BUCKET, "양동이");
        NAME_MAP.put(Material.WATER_BUCKET, "물 양동이");
        NAME_MAP.put(Material.LAVA_BUCKET, "용암 양동이");
        NAME_MAP.put(Material.MILK_BUCKET, "우유 양동이");

        // 자연 블록
        NAME_MAP.put(Material.OAK_SAPLING, "참나무 묘목");
        NAME_MAP.put(Material.SPRUCE_SAPLING, "가문비나무 묘목");
        NAME_MAP.put(Material.BIRCH_SAPLING, "자작나무 묘목");
        NAME_MAP.put(Material.JUNGLE_SAPLING, "정글나무 묘목");
        NAME_MAP.put(Material.ACACIA_SAPLING, "아카시아나무 묘목");
        NAME_MAP.put(Material.DARK_OAK_SAPLING, "짙은 참나무 묘목");
        NAME_MAP.put(Material.MANGROVE_PROPAGULE, "맹그로브 주아");
        NAME_MAP.put(Material.CHERRY_SAPLING, "벚나무 묘목");
        NAME_MAP.put(Material.OAK_LEAVES, "참나무 잎");
        NAME_MAP.put(Material.SPRUCE_LEAVES, "가문비나무 잎");
        NAME_MAP.put(Material.BIRCH_LEAVES, "자작나무 잎");
        NAME_MAP.put(Material.JUNGLE_LEAVES, "정글나무 잎");
        NAME_MAP.put(Material.ACACIA_LEAVES, "아카시아나무 잎");
        NAME_MAP.put(Material.DARK_OAK_LEAVES, "짙은 참나무 잎");
        NAME_MAP.put(Material.MANGROVE_LEAVES, "맹그로브 잎");
        NAME_MAP.put(Material.CHERRY_LEAVES, "벚나무 잎");

        NAME_MAP.put(Material.DANDELION, "민들레");
        NAME_MAP.put(Material.POPPY, "양귀비");
        NAME_MAP.put(Material.BLUE_ORCHID, "파란 난초");
        NAME_MAP.put(Material.ALLIUM, "파꽃");
        NAME_MAP.put(Material.AZURE_BLUET, "선애기별꽃");
        NAME_MAP.put(Material.RED_TULIP, "빨간색 튤립");
        NAME_MAP.put(Material.ORANGE_TULIP, "주황색 튤립");
        NAME_MAP.put(Material.WHITE_TULIP, "하얀색 튤립");
        NAME_MAP.put(Material.PINK_TULIP, "분홍색 튤립");
        NAME_MAP.put(Material.OXEYE_DAISY, "데이지");
        NAME_MAP.put(Material.CORNFLOWER, "수레국화");
        NAME_MAP.put(Material.LILY_OF_THE_VALLEY, "은방울꽃");
        NAME_MAP.put(Material.WITHER_ROSE, "위더 장미");
        NAME_MAP.put(Material.SUNFLOWER, "해바라기");
        NAME_MAP.put(Material.LILAC, "라일락");
        NAME_MAP.put(Material.ROSE_BUSH, "장미 덤불");
        NAME_MAP.put(Material.PEONY, "모란");

        // 블록 - 콘크리트/양털/유리
        NAME_MAP.put(Material.WHITE_WOOL, "하얀색 양털");
        NAME_MAP.put(Material.ORANGE_WOOL, "주황색 양털");
        NAME_MAP.put(Material.MAGENTA_WOOL, "자홍색 양털");
        NAME_MAP.put(Material.LIGHT_BLUE_WOOL, "하늘색 양털");
        NAME_MAP.put(Material.YELLOW_WOOL, "노란색 양털");
        NAME_MAP.put(Material.LIME_WOOL, "연두색 양털");
        NAME_MAP.put(Material.PINK_WOOL, "분홍색 양털");
        NAME_MAP.put(Material.GRAY_WOOL, "회색 양털");
        NAME_MAP.put(Material.LIGHT_GRAY_WOOL, "회백색 양털");
        NAME_MAP.put(Material.CYAN_WOOL, "청록색 양털");
        NAME_MAP.put(Material.PURPLE_WOOL, "보라색 양털");
        NAME_MAP.put(Material.BLUE_WOOL, "파란색 양털");
        NAME_MAP.put(Material.BROWN_WOOL, "갈색 양털");
        NAME_MAP.put(Material.GREEN_WOOL, "초록색 양털");
        NAME_MAP.put(Material.RED_WOOL, "빨간색 양털");
        NAME_MAP.put(Material.BLACK_WOOL, "검은색 양털");

        // 레드스톤/운송
        NAME_MAP.put(Material.PISTON, "피스톤");
        NAME_MAP.put(Material.STICKY_PISTON, "끈끈이 피스톤");
        NAME_MAP.put(Material.DISPENSER, "발사기");
        NAME_MAP.put(Material.DROPPER, "공급기");
        NAME_MAP.put(Material.OBSERVER, "관측기");
        NAME_MAP.put(Material.HOPPER, "호퍼");
        NAME_MAP.put(Material.LEVER, "레버");
        NAME_MAP.put(Material.TNT, "TNT");
        NAME_MAP.put(Material.TARGET, "과녁");

        NAME_MAP.put(Material.RAIL, "레일");
        NAME_MAP.put(Material.POWERED_RAIL, "파워 레일");
        NAME_MAP.put(Material.DETECTOR_RAIL, "탐지 레일");
        NAME_MAP.put(Material.ACTIVATOR_RAIL, "활성화 레일");
        NAME_MAP.put(Material.MINECART, "카트");
        NAME_MAP.put(Material.CHEST_MINECART, "상자 카트");
        NAME_MAP.put(Material.FURNACE_MINECART, "화로 카트");
        NAME_MAP.put(Material.HOPPER_MINECART, "호퍼 카트");
        NAME_MAP.put(Material.TNT_MINECART, "TNT 카트");

        NAME_MAP.put(Material.OAK_BOAT, "참나무 보트");
        NAME_MAP.put(Material.OAK_CHEST_BOAT, "상자가 실린 참나무 보트");
        NAME_MAP.put(Material.SPRUCE_BOAT, "가문비나무 보트");
        NAME_MAP.put(Material.BIRCH_BOAT, "자작나무 보트");
        NAME_MAP.put(Material.ELYTRA, "겉날개");
        NAME_MAP.put(Material.SADDLE, "안장");
        NAME_MAP.put(Material.NAME_TAG, "이름표");
        NAME_MAP.put(Material.LEAD, "끈");

        // 네더/엔드
        NAME_MAP.put(Material.NETHERRACK, "네더랙");
        NAME_MAP.put(Material.SOUL_SAND, "영혼 모래");
        NAME_MAP.put(Material.SOUL_SOIL, "영혼 흙");
        NAME_MAP.put(Material.BASALT, "현무암");
        NAME_MAP.put(Material.BLACKSTONE, "흑암");
        NAME_MAP.put(Material.GLOWSTONE, "발광석");
        NAME_MAP.put(Material.GLOWSTONE_DUST, "발광석 가루");
        NAME_MAP.put(Material.QUARTZ_BLOCK, "석영 블록");
        NAME_MAP.put(Material.MAGMA_BLOCK, "마그마 블록");
        NAME_MAP.put(Material.NETHER_WART, "네더 사마귀");
        NAME_MAP.put(Material.BLAZE_ROD, "블레이즈 막대");
        NAME_MAP.put(Material.BLAZE_POWDER, "블레이즈 가루");
        NAME_MAP.put(Material.GHAST_TEAR, "가스트의 눈물");
        NAME_MAP.put(Material.MAGMA_CREAM, "마그마 크림");
        NAME_MAP.put(Material.ENDER_PEARL, "엔더 진주");
        NAME_MAP.put(Material.END_STONE, "엔드 돌");
        NAME_MAP.put(Material.PURPUR_BLOCK, "퍼퍼 블록");
        NAME_MAP.put(Material.CHORUS_FRUIT, "코러스 열매");
        NAME_MAP.put(Material.SHULKER_SHELL, "셜커 껍질");
        NAME_MAP.put(Material.SHULKER_BOX, "셜커 상자");

        // 바다
        NAME_MAP.put(Material.PRISMARINE, "프리즈머린");
        NAME_MAP.put(Material.PRISMARINE_BRICKS, "프리즈머린 벽돌");
        NAME_MAP.put(Material.DARK_PRISMARINE, "짙은 프리즈머린");
        NAME_MAP.put(Material.SEA_LANTERN, "바다 랜턴");
        NAME_MAP.put(Material.SPONGE, "스펀지");
        NAME_MAP.put(Material.WET_SPONGE, "젖은 스펀지");
        NAME_MAP.put(Material.KELP, "켈프");
        NAME_MAP.put(Material.DRIED_KELP, "말린 켈프");
        NAME_MAP.put(Material.HEART_OF_THE_SEA, "바다의 심장");
        NAME_MAP.put(Material.NAUTILUS_SHELL, "앵무조개 껍데기");
        NAME_MAP.put(Material.TRIDENT, "삼지창");

        // 염료
        NAME_MAP.put(Material.WHITE_DYE, "하얀색 염료");
        NAME_MAP.put(Material.ORANGE_DYE, "주황색 염료");
        NAME_MAP.put(Material.MAGENTA_DYE, "자홍색 염료");
        NAME_MAP.put(Material.LIGHT_BLUE_DYE, "하늘색 염료");
        NAME_MAP.put(Material.YELLOW_DYE, "노란색 염료");
        NAME_MAP.put(Material.LIME_DYE, "연두색 염료");
        NAME_MAP.put(Material.PINK_DYE, "분홍색 염료");
        NAME_MAP.put(Material.GRAY_DYE, "회색 염료");
        NAME_MAP.put(Material.LIGHT_GRAY_DYE, "회백색 염료");
        NAME_MAP.put(Material.CYAN_DYE, "청록색 염료");
        NAME_MAP.put(Material.PURPLE_DYE, "보라색 염료");
        NAME_MAP.put(Material.BLUE_DYE, "파란색 염료");
        NAME_MAP.put(Material.BROWN_DYE, "갈색 염료");
        NAME_MAP.put(Material.GREEN_DYE, "초록색 염료");
        NAME_MAP.put(Material.RED_DYE, "빨간색 염료");
        NAME_MAP.put(Material.BLACK_DYE, "검은색 염료");
    }

    /**
     * Material의 한글 이름을 반환합니다.
     * 등록되지 않은 경우 영문 이름을 포맷팅하여 반환합니다.
     */
    public static String getName(Material material) {
        if (material == null)
            return "알 수 없음";

        String koreanName = NAME_MAP.get(material);
        if (koreanName != null) {
            return koreanName;
        }

        // 등록되지 않은 아이템은 기존 방식대로 포맷팅
        return formatEnglishName(material);
    }

    private static String formatEnglishName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
