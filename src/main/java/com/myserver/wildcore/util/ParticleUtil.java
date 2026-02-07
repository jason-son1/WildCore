package com.myserver.wildcore.util;

import com.myserver.wildcore.WildCore;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 사유지 미리보기 파티클 및 효과를 담당하는 유틸리티 클래스입니다.
 */
public class ParticleUtil {

    private final WildCore plugin;

    public ParticleUtil(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 사유지 테두리에 파티클을 표시합니다.
     * 지정된 시간 동안 반복하여 표시합니다.
     *
     * @param player          파티클을 볼 플레이어
     * @param center          중심 좌표
     * @param radius          반지름
     * @param durationSeconds 지속 시간 (초)
     * @return 파티클 태스크 ID (취소용)
     */
    public int showClaimBorderParticles(Player player, Location center, int radius, int durationSeconds) {
        return new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                spawnBorderParticles(player, center, radius);
                ticks += 10; // 0.5초마다 실행
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L).getTaskId();
    }

    /**
     * 한 번의 테두리 파티클 스폰
     */
    private void spawnBorderParticles(Player player, Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        double y = center.getY() + 1.2;

        Particle particle = Particle.HAPPY_VILLAGER;

        // 북쪽 & 남쪽 라인
        for (int x = minX; x <= maxX; x++) {
            spawnParticle(player, particle, x + 0.5, y, minZ + 0.5);
            spawnParticle(player, particle, x + 0.5, y, maxZ + 0.5);
        }

        // 서쪽 & 동쪽 라인
        for (int z = minZ + 1; z < maxZ; z++) {
            spawnParticle(player, particle, minX + 0.5, y, z + 0.5);
            spawnParticle(player, particle, maxX + 0.5, y, z + 0.5);
        }
    }

    /**
     * 플레이어에게만 보이는 파티클 스폰
     */
    private void spawnParticle(Player player, Particle particle, double x, double y, double z) {
        player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
    }

    /**
     * 성공 효과 (폭죽)
     */
    public void playSuccessEffect(Location location) {
        if (location.getWorld() == null)
            return;

        // 메인 스레드에서 실행해야 함
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Firework firework = location.getWorld().spawn(location.clone().add(0, 1, 0), Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();

            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(Color.LIME, Color.GREEN)
                    .withFade(Color.WHITE)
                    .with(FireworkEffect.Type.BALL)
                    .trail(true)
                    .build();

            meta.addEffect(effect);
            meta.setPower(0); // 즉시 폭발
            firework.setFireworkMeta(meta);

            // 즉시 폭발시키기
            plugin.getServer().getScheduler().runTaskLater(plugin, firework::detonate, 1L);
        });
    }

    /**
     * 파티클 태스크 취소
     */
    public void cancelParticleTask(int taskId) {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }
}
