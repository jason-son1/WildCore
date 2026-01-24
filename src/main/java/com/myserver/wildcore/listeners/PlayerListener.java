package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.CustomItemConfig;
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
        CustomItemConfig saveTicket = plugin.getConfigManager().getCustomItem("inventory_save_ticket");
        if (saveTicket != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (ItemUtil.isCustomItem(plugin, item, "inventory_save_ticket")) {
                    // 세이브권 사용
                    item.setAmount(item.getAmount() - 1);
                    event.setKeepInventory(true);
                    event.getDrops().clear();

                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("save_ticket_used"));
                    break;
                }
            }
        }

        // 경험치 보존권 확인
        CustomItemConfig expTicket = plugin.getConfigManager().getCustomItem("exp_save_ticket");
        if (expTicket != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (ItemUtil.isCustomItem(plugin, item, "exp_save_ticket")) {
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
        if (ItemUtil.isCustomItem(plugin, item, "spawn_warp_ticket")) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);

            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("warp_ticket_used"));

            // EssentialsX의 spawn 명령어 실행
            plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    "spawn " + player.getName());
            return;
        }

        // 홈 워프권 확인 (EssentialsX /home 명령어 사용)
        if (ItemUtil.isCustomItem(plugin, item, "home_warp_ticket")) {
            event.setCancelled(true);

            // EssentialsX의 home 명령어 실행 (콘솔에서 실행하여 권한 무시)
            // 홈이 설정되어 있지 않으면 EssentialsX가 알아서 메시지를 보냄
            boolean success = player.performCommand("home");

            if (success) {
                item.setAmount(item.getAmount() - 1);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§e홈 워프권이 사용되었습니다! 홈으로 이동합니다.");
            }
            // 홈이 없는 경우에는 아이템을 소모하지 않음 (EssentialsX가 에러 메시지 출력)
            return;
        }

        // 랜덤 워프권 확인
        if (ItemUtil.isCustomItem(plugin, item, "random_warp_ticket")) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);

            Location randomLoc = getRandomSafeLocation(player.getWorld());
            if (randomLoc != null) {
                player.teleport(randomLoc);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§d랜덤 워프권이 사용되었습니다! 랜덤한 위치로 이동했습니다.");
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c안전한 위치를 찾지 못했습니다. 다시 시도해주세요.");
                // 아이템 반환
                player.getInventory().addItem(ItemUtil.createCustomItem(plugin, "random_warp_ticket", 1));
            }
        }
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
