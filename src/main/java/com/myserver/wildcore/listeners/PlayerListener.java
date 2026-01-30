package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;

import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.myserver.wildcore.gui.PlayerInfoGUI;

import java.util.Random;

/**
 * 플레이어 관련 이벤트 리스너
 * - 인벤토리 세이브권
 * - 스폰 워프권
 * - 랜덤 워프권
 * - 경험치 보존권
 */
public class PlayerListener implements Listener {

    private final WildCore plugin;
    private final Random random = new Random();

    public PlayerListener(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 사망 이벤트 - 세이브권 처리
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 인벤토리 세이브권 확인
        for (ItemStack item : player.getInventory().getContents()) {
            if (ItemUtil.hasFunction(plugin, item, "inventory_save")) {
                // 세이브권 사용
                item.setAmount(item.getAmount() - 1);
                event.setKeepInventory(true);
                event.getDrops().clear();

                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("general.save_ticket_used"));
                break;
            }
        }

        // 경험치 보존권 확인
        for (ItemStack item : player.getInventory().getContents()) {
            if (ItemUtil.hasFunction(plugin, item, "exp_save")) {
                // 경험치 보존권 사용
                item.setAmount(item.getAmount() - 1);
                event.setKeepLevel(true);
                event.setDroppedExp(0);

                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§a경험치 보존권이 사용되었습니다! 경험치가 보존됩니다.");
                break;
            }
        }
    }

    /**
     * 상호작용 이벤트 - 워프권 처리
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK"))
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null)
            return;

        // 스폰 워프권 확인 (EssentialsX /spawn 명령어 사용)
        if (ItemUtil.hasFunction(plugin, item, "spawn_warp")) {
            event.setCancelled(true);

            scheduleWarp(player, item, () -> {
                // mv spawn 명령어 실행
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "mv spawn " + player.getName());

                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("general.warp_ticket_used"));
            });
            return;
        }

        // 홈 워프권 확인 (EssentialsX /home 명령어 사용)
        if (ItemUtil.hasFunction(plugin, item, "home_warp")) {
            event.setCancelled(true);

            scheduleWarp(player, item, () -> {
                // EssentialsX의 home 명령어 실행 (콘솔에서 실행하여 권한 무시)
                boolean success = player.performCommand("home");

                if (success) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§e홈 워프권이 사용되었습니다! 홈으로 이동합니다.");
                } else {
                    // 실패 시 아이템 복구 (소모 처리는 scheduleWarp에서 하므로 다시 줘야 함)
                    // 하지만 scheduleWarp 로직을 수정하여 runnable 안에서 성공 여부를 반환하도록 하기는 복잡함
                    // 따라서 편의상 성공한 것으로 간주하고 소모하거나
                    // 또는 scheduleWarp를 단순히 실행만 하도록 하고, 내부에서 소모 처리를 하도록 변경 필요

                    // 여기서는 scheduleWarp가 아이템을 소모시키므로, 실패 시 다시 지급
                    player.getInventory().addItem(ItemUtil.createCustomItem(plugin, "home_warp_ticket", 1));
                }
            });
            return;
        }

        // 랜덤 워프권 확인
        if (ItemUtil.hasFunction(plugin, item, "random_warp")) {
            event.setCancelled(true);

            scheduleWarp(player, item, () -> {
                Location randomLoc = getRandomSafeLocation(player.getWorld());
                if (randomLoc != null) {
                    player.teleport(randomLoc);
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§d랜덤 워프권이 사용되었습니다! 랜덤한 위치로 이동했습니다.");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c안전한 위치를 찾지 못했습니다. 다시 시도해주세요.");
                    // 실패 시 아이템 복구
                    player.getInventory().addItem(ItemUtil.createCustomItem(plugin, "random_warp_ticket", 1));
                }
            });
            return;
        }
    }

    /**
     * 워프 스케줄링 (3초 딜레이)
     */
    private void scheduleWarp(Player player, ItemStack item, Runnable callback) {
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§e3초 후에 이동합니다. 움직이지 마세요!");

        Location startLoc = player.getLocation();

        new BukkitRunnable() {
            @Override
            public void run() {
                // 플레이어가 접속 중인지 확인
                if (!player.isOnline())
                    return;

                // 움직였는지 확인 (간단하게 블록 위치로)
                if (startLoc.getBlockX() != player.getLocation().getBlockX() ||
                        startLoc.getBlockY() != player.getLocation().getBlockY() ||
                        startLoc.getBlockZ() != player.getLocation().getBlockZ()) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c움직여서 이동이 취소되었습니다.");
                    return;
                }

                // 아이템 소모
                item.setAmount(item.getAmount() - 1);

                // 콜백 실행
                callback.run();
            }
        }.runTaskLater(plugin, 60L); // 3초 딜레이
    }

    /**
     * 키 입력 이벤트 - 내 정보 GUI (Shift + F)
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            new PlayerInfoGUI(plugin, event.getPlayer()).open();
        }
    }

    /**
     * 안전한 랜덤 위치 찾기
     */
    private Location getRandomSafeLocation(World world) {
        for (int attempts = 0; attempts < 10; attempts++) {
            int x = random.nextInt(10000) - 5000;
            int z = random.nextInt(10000) - 5000;
            int y = world.getHighestBlockYAt(x, z);

            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

            // 안전한 위치인지 확인
            if (isSafeLocation(loc)) {
                return loc;
            }
        }
        return null;
    }

    /**
     * 안전한 위치인지 확인
     */
    private boolean isSafeLocation(Location loc) {
        // 발 아래가 고체 블록인지 확인
        if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            return false;
        }

        // 플레이어 위치가 공기인지 확인
        if (!loc.getBlock().getType().isAir()) {
            return false;
        }

        // 머리 위치가 공기인지 확인
        if (!loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
            return false;
        }

        // 용암, 물이 아닌지 확인
        if (loc.clone().subtract(0, 1, 0).getBlock().isLiquid()) {
            return false;
        }

        return true;
    }
}
