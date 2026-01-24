package com.myserver.wildcore.util;

import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * 인챈트 이름을 한국어로 변환하는 유틸리티 클래스
 */
public class EnchantNameUtil {

    private static final Map<String, String> KOREAN_NAMES = new HashMap<>();

    static {
        // === 무기 인챈트 ===
        KOREAN_NAMES.put("sharpness", "날카로움");
        KOREAN_NAMES.put("smite", "강타");
        KOREAN_NAMES.put("bane_of_arthropods", "살충");
        KOREAN_NAMES.put("knockback", "밀치기");
        KOREAN_NAMES.put("fire_aspect", "발화");
        KOREAN_NAMES.put("looting", "약탈");
        KOREAN_NAMES.put("sweeping_edge", "휩쓸기");
        KOREAN_NAMES.put("sweeping", "휩쓸기"); // 구버전 호환

        // === 갑옷 인챈트 ===
        KOREAN_NAMES.put("protection", "보호");
        KOREAN_NAMES.put("fire_protection", "화염으로부터 보호");
        KOREAN_NAMES.put("feather_falling", "가벼운 착지");
        KOREAN_NAMES.put("blast_protection", "폭발로부터 보호");
        KOREAN_NAMES.put("projectile_protection", "발사체로부터 보호");
        KOREAN_NAMES.put("respiration", "호흡");
        KOREAN_NAMES.put("aqua_affinity", "친수성");
        KOREAN_NAMES.put("thorns", "가시");
        KOREAN_NAMES.put("depth_strider", "물갈퀴");
        KOREAN_NAMES.put("frost_walker", "차가운 걸음");
        KOREAN_NAMES.put("soul_speed", "영혼 가속");
        KOREAN_NAMES.put("swift_sneak", "빠른 잠행");

        // === 도구 인챈트 ===
        KOREAN_NAMES.put("efficiency", "효율");
        KOREAN_NAMES.put("silk_touch", "섬세한 손길");
        KOREAN_NAMES.put("unbreaking", "내구성");
        KOREAN_NAMES.put("fortune", "행운");

        // === 활 인챈트 ===
        KOREAN_NAMES.put("power", "힘");
        KOREAN_NAMES.put("punch", "밀어내기");
        KOREAN_NAMES.put("flame", "화염");
        KOREAN_NAMES.put("infinity", "무한");

        // === 석궁 인챈트 ===
        KOREAN_NAMES.put("multishot", "다중 발사");
        KOREAN_NAMES.put("quick_charge", "빠른 장전");
        KOREAN_NAMES.put("piercing", "관통");

        // === 낚싯대 인챈트 ===
        KOREAN_NAMES.put("luck_of_the_sea", "바다의 행운");
        KOREAN_NAMES.put("lure", "미끼");

        // === 삼지창 인챈트 ===
        KOREAN_NAMES.put("impaling", "찌르기");
        KOREAN_NAMES.put("riptide", "급류");
        KOREAN_NAMES.put("loyalty", "충성");
        KOREAN_NAMES.put("channeling", "집전");

        // === 공통/기타 인챈트 ===
        KOREAN_NAMES.put("mending", "수선");
        KOREAN_NAMES.put("vanishing_curse", "소실의 저주");
        KOREAN_NAMES.put("binding_curse", "귀속의 저주");

        // === 1.21+ 신규 인챈트 ===
        KOREAN_NAMES.put("density", "밀도");
        KOREAN_NAMES.put("breach", "관통력");
        KOREAN_NAMES.put("wind_burst", "바람 폭발");
    }

    /**
     * 인챈트의 한국어 이름을 반환합니다.
     * 
     * @param enchant 마인크래프트 인챈트
     * @return 한국어 이름 (없을 경우 영어 이름을 Title Case로 반환)
     */
    public static String getKoreanName(Enchantment enchant) {
        if (enchant == null) {
            return "알 수 없음";
        }
        return getKoreanName(enchant.getKey().getKey());
    }

    /**
     * 인챈트 키의 한국어 이름을 반환합니다.
     * 
     * @param enchantKey 인챈트 키 (예: "sharpness", "protection")
     * @return 한국어 이름 (없을 경우 영어 이름을 Title Case로 반환)
     */
    public static String getKoreanName(String enchantKey) {
        if (enchantKey == null || enchantKey.isEmpty()) {
            return "알 수 없음";
        }

        String key = enchantKey.toLowerCase();
        if (KOREAN_NAMES.containsKey(key)) {
            return KOREAN_NAMES.get(key);
        }

        // 한국어 이름이 없는 경우 Title Case로 변환
        return toTitleCase(key);
    }

    /**
     * 스네이크 케이스를 타이틀 케이스로 변환
     */
    private static String toTitleCase(String key) {
        StringBuilder result = new StringBuilder();
        for (String word : key.split("_")) {
            if (result.length() > 0) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /**
     * 주어진 키에 대한 한국어 이름이 존재하는지 확인
     */
    public static boolean hasKoreanName(String enchantKey) {
        return KOREAN_NAMES.containsKey(enchantKey.toLowerCase());
    }
}
