package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 커스텀 아이템의 바닐라 동작 차단 리스너
 * 
 * WildCore 커스텀 아이템이 원래 마인크래프트 기능으로 사용되지 않도록 차단합니다.
 * 예: 경험치병으로 만든 커스텀 아이템이 우클릭 시 투척되지 않도록 함
 */
public class CustomItemProtectListener implements Listener {

    private final WildCore plugin;

    public CustomItemProtectListener(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 상호작용 시 커스텀 아이템의 바닐라 동작 차단
     * - 우클릭으로 아이템 사용 (엔더펄, 경험치병, 눈덩이 등)
     * - 블록에 아이템 사용 (양동이, 비료 등)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null)
            return;

        // 커스텀 아이템인지 확인
        String customItemId = ItemUtil.getCustomItemId(plugin, item);
        if (customItemId == null)
            return;

        // 커스텀 아이템이면 기본 동작 차단 (PlayerListener에서 별도로 처리할 아이템 제외)
        Action action = event.getAction();

        // 우클릭 계열 동작 차단
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // 바닐라 동작을 수행하는 위험한 아이템들
            switch (item.getType()) {
                // 투척 아이템
                case ENDER_PEARL:
                case ENDER_EYE:
                case EXPERIENCE_BOTTLE:
                case SPLASH_POTION:
                case LINGERING_POTION:
                case SNOWBALL:
                case EGG:
                case FIRE_CHARGE:
                case TRIDENT:
                    // 설치/사용 아이템
                case WATER_BUCKET:
                case LAVA_BUCKET:
                case BUCKET:
                case FLINT_AND_STEEL:
                case BONE_MEAL:
                case FIREWORK_ROCKET:
                    // 먹을 수 있는 아이템 (특수 효과)
                case CHORUS_FRUIT:
                case GOLDEN_APPLE:
                case ENCHANTED_GOLDEN_APPLE:
                    // 바닐라 동작 차단
                    event.setCancelled(true);

                    // 디버그 로그
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[CustomItemProtect] 바닐라 동작 차단: " +
                                customItemId + " (" + item.getType().name() + ")");
                    }
                    break;
                default:
                    // 다른 아이템들도 기본적으로 차단 (안전을 위해)
                    // 단, 블록 설치/파괴 같은 기본 동작은 허용할 수 있음
                    break;
            }
        }
    }

    /**
     * 발사체 발사 이벤트 - 커스텀 아이템으로 만든 투척물 차단
     * (엔더펄, 눈덩이, 달걀, 경험치병 등)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player))
            return;

        // 플레이어의 손에 있는 아이템 확인
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 메인핸드 또는 오프핸드에 커스텀 아이템이 있으면 발사 차단
        String mainHandId = ItemUtil.getCustomItemId(plugin, mainHand);
        String offHandId = ItemUtil.getCustomItemId(plugin, offHand);

        if (mainHandId != null || offHandId != null) {
            // 특정 워프권 등 의도된 동작이 아닌 경우에만 차단
            // (PlayerListener에서 이미 처리된 경우는 제외)
            String relevantId = mainHandId != null ? mainHandId : offHandId;

            // 의도된 투척 아이템이 아닌 경우 차단
            if (!isIntendedProjectileItem(relevantId)) {
                event.setCancelled(true);

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[CustomItemProtect] 발사체 차단: " + relevantId);
                }
            }
        }
    }

    /**
     * 아이템 소비 이벤트 - 커스텀 아이템 먹기/마시기 차단
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();

        String customItemId = ItemUtil.getCustomItemId(plugin, item);
        if (customItemId == null)
            return;

        // 커스텀 아이템은 먹거나 마실 수 없음
        event.setCancelled(true);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[CustomItemProtect] 아이템 소비 차단: " + customItemId);
        }
    }

    /**
     * 의도된 투척 아이템인지 확인
     * (특정 커스텀 아이템은 투척이 허용될 수 있음)
     */
    private boolean isIntendedProjectileItem(String itemId) {
        // 현재는 모든 커스텀 아이템의 투척을 차단
        // 필요시 여기에 예외 아이템 ID 추가
        return false;
    }
}
