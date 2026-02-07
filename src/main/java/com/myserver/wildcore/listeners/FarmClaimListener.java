package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ClaimItemConfig;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.ParticleUtil;
import com.myserver.wildcore.util.StructureBuilder;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 농장 허가증 아이템 사용 시 사유지 생성을 처리하는 리스너입니다.
 */
public class FarmClaimListener implements Listener {

    private final WildCore plugin;
    private final ClaimManager claimManager;
    private final ParticleUtil particleUtil;
    private final StructureBuilder structureBuilder;

    // 확정 대기 중인 플레이어 정보
    private final Map<UUID, Long> confirmTimeMap = new HashMap<>();
    private final Map<UUID, Location> confirmLocMap = new HashMap<>();
    private final Map<UUID, String> confirmItemMap = new HashMap<>();
    private final Map<UUID, Integer> particleTaskMap = new HashMap<>();

    public FarmClaimListener(WildCore plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.particleUtil = new ParticleUtil(plugin);
        this.structureBuilder = new StructureBuilder(plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 우클릭 블록만 처리
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        if (event.getClickedBlock() == null)
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 클레임 생성 기능이 있는 아이템인지 확인
        if (!ItemUtil.hasFunction(plugin, item, "claim_create"))
            return;

        // 클레임 시스템이 활성화되어 있는지 확인
        if (!claimManager.isEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getClaimMessage("gp_not_found"));
            return;
        }

        // 권한 확인
        if (!player.hasPermission("wildcore.claim.use")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("general.no_permission"));
            return;
        }

        event.setCancelled(true);

        String itemId = ItemUtil.getCustomItemId(plugin, item);
        ClaimItemConfig claimConfig = plugin.getConfigManager().getClaimItemConfig(itemId);

        if (claimConfig == null) {
            plugin.getLogger().warning("Claim config not found for item: " + itemId);
            return;
        }

        Location center = event.getClickedBlock().getLocation().add(0, 1, 0);
        int radius = claimConfig.getRadius();

        // 월드 검증
        if (!claimManager.isWorldAllowed(center.getWorld())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getClaimMessage("create_fail_world"));
            playSound(player, "fail");
            return;
        }

        // 더블 클릭(확정) 로직 확인
        if (isConfirming(player, center, itemId)) {
            handleClaimCreation(player, center, radius, item, claimConfig);
        } else {
            startPreview(player, center, radius, itemId);
        }
    }

    /**
     * 플레이어가 확정 대기 상태인지 확인합니다.
     */
    private boolean isConfirming(Player player, Location clickedLoc, String itemId) {
        UUID uuid = player.getUniqueId();

        if (!confirmTimeMap.containsKey(uuid))
            return false;

        long timestamp = confirmTimeMap.get(uuid);
        int previewDuration = plugin.getConfigManager().getClaimPreviewDuration();

        // 제한 시간 초과 확인
        if (System.currentTimeMillis() - timestamp > previewDuration * 1000L) {
            clearConfirmState(uuid);
            return false;
        }

        // 같은 위치인지 확인
        Location savedLoc = confirmLocMap.get(uuid);
        if (savedLoc.distanceSquared(clickedLoc) > 4.0) { // 2블록 이내
            clearConfirmState(uuid);
            return false;
        }

        // 같은 아이템인지 확인
        String savedItemId = confirmItemMap.get(uuid);
        if (!itemId.equals(savedItemId)) {
            clearConfirmState(uuid);
            return false;
        }

        return true;
    }

    /**
     * 미리보기를 시작합니다.
     */
    private void startPreview(Player player, Location center, int radius, String itemId) {
        UUID uuid = player.getUniqueId();

        // 이전 상태 정리
        clearConfirmState(uuid);

        // 유효성 검사
        if (!claimManager.canCreateClaim(center, radius, player)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getClaimMessage("create_fail_overlap"));
            playSound(player, "fail");
            return;
        }

        int previewDuration = plugin.getConfigManager().getClaimPreviewDuration();

        // 상태 저장
        confirmTimeMap.put(uuid, System.currentTimeMillis());
        confirmLocMap.put(uuid, center);
        confirmItemMap.put(uuid, itemId);

        // 메시지 및 사운드
        String message = plugin.getConfigManager().getClaimMessage("preview_start")
                .replace("%seconds%", String.valueOf(previewDuration));
        player.sendMessage(plugin.getConfigManager().getPrefix() + message);
        playSound(player, "preview");

        // 파티클 표시
        int taskId = particleUtil.showClaimBorderParticles(player, center, radius, previewDuration);
        particleTaskMap.put(uuid, taskId);
    }

    /**
     * 실제로 사유지를 생성합니다.
     */
    private void handleClaimCreation(Player player, Location center, int radius,
            ItemStack handItem, ClaimItemConfig claimConfig) {
        UUID uuid = player.getUniqueId();

        // 확정 상태 정리
        clearConfirmState(uuid);

        // 아이템이 여전히 있는지 확인
        if (handItem.getAmount() < 1) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c아이템이 없습니다.");
            return;
        }

        // 자동 블록 지급이 비활성화 상태에서 블록 부족 확인
        if (!plugin.getConfigManager().isClaimAutoGrantBlocks()) {
            int area = (radius * 2 + 1) * (radius * 2 + 1);
            int needed = claimManager.getNeededBlocks(player, area);
            if (needed > 0) {
                String message = plugin.getConfigManager().getClaimMessage("create_fail_blocks")
                        .replace("%needed%", String.valueOf(area))
                        .replace("%have%", String.valueOf(area - needed));
                player.sendMessage(plugin.getConfigManager().getPrefix() + message);
                playSound(player, "fail");
                return;
            }
        }

        // 사유지 생성
        CreateClaimResult result = claimManager.createClaim(player, center, radius);

        if (result == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사유지 생성에 실패했습니다. (시스템 오류)");
            playSound(player, "fail");
            return;
        }

        if (result.succeeded) {
            // 성공 처리
            handItem.setAmount(handItem.getAmount() - 1);

            // 울타리 설치 (설정에 따라)
            if (plugin.getConfigManager().isClaimAutoFence()) {
                structureBuilder.buildFences(center, radius,
                        claimConfig.getFenceMaterial(),
                        claimConfig.getGateMaterial(),
                        claimConfig.getFloorMaterial(),
                        player);
            }

            // 성공 메시지
            int size = claimConfig.getDiameter();
            String message = plugin.getConfigManager().getClaimMessage("create_success")
                    .replace("%size%", String.valueOf(size));
            player.sendMessage(plugin.getConfigManager().getPrefix() + message);
            playSound(player, "success");

            // 폭죽 효과
            particleUtil.playSuccessEffect(center);

        } else {
            // 실패 처리 (GP 내부 이유)
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사유지 생성 실패: " + (result.claim != null ? "알 수 없는 오류" : "영역 충돌"));
            playSound(player, "fail");
        }
    }

    /**
     * 플레이어의 확정 대기 상태를 정리합니다.
     */
    private void clearConfirmState(UUID uuid) {
        confirmTimeMap.remove(uuid);
        confirmLocMap.remove(uuid);
        confirmItemMap.remove(uuid);

        // 파티클 태스크 취소
        if (particleTaskMap.containsKey(uuid)) {
            particleUtil.cancelParticleTask(particleTaskMap.remove(uuid));
        }
    }

    /**
     * 효과음 재생
     */
    private void playSound(Player player, String type) {
        String soundName = plugin.getConfigManager().getClaimSound(type);
        if (soundName == null || soundName.isEmpty())
            return;

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException e) {
            plugin.debug("Invalid sound: " + soundName);
        }
    }
}
