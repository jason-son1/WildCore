package com.myserver.wildcore.config;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 특정 블록을 밟았을 때 부여되는 버프 설정
 */
public class BuffBlockConfig {

    private final String id;
    private final String worldName;
    private final Material blockType;
    private final List<BuffEffect> effects;

    public BuffBlockConfig(String id, String worldName, String blockType, List<BuffEffect> effects) {
        this.id = id;
        this.worldName = worldName;
        this.blockType = Material.getMaterial(blockType.toUpperCase());
        this.effects = effects;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public Material getBlockType() {
        return blockType;
    }

    public List<BuffEffect> getEffects() {
        return effects;
    }

    public static class BuffEffect {
        private final PotionEffectType type;
        private final int duration; // ticks
        private final int amplifier;

        public BuffEffect(String type, int duration, int amplifier) {
            this.type = PotionEffectType.getByName(type.toUpperCase());
            this.duration = duration;
            this.amplifier = amplifier;
        }

        public PotionEffectType getType() {
            return type;
        }

        public int getDuration() {
            return duration;
        }

        public int getAmplifier() {
            return amplifier;
        }
    }
}
