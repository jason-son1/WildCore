package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.claim.ClaimFlags;
import com.myserver.wildcore.managers.ClaimDataManager;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.managers.CropGrowthManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * GriefPrevention 이벤트를 리스닝하여 WildCore 데이터를 동기화합니다.
 * 또한 커스텀 보호 기능(입장 제한 등)을 제공합니다.
 */
public class ClaimProtectionListener implements Listener {

    private final WildCore plugin;
    private final ClaimManager claimManager;
    private final ClaimDataManager claimDataManager;
    private final CropGrowthManager cropGrowthManager;
    private BukkitTask mobEntryTask;

    public ClaimProtectionListener(WildCore plugin, ClaimManager claimManager, ClaimDataManager claimDataManager,
            CropGrowthManager cropGrowthManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.claimDataManager = claimDataManager;
        this.cropGrowthManager = cropGrowthManager;
        startMobEntryTask();
    }

    // =====================
    // GP 연동 이벤트
    // =====================

    /**
     * GP Claim 삭제 시 WildCore 데이터도 동기화 삭제
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaimDeleted(ClaimDeletedEvent event) {
        Claim claim = event.getClaim();
        if (claim != null) {
            Long claimId = claim.getID();
            if (claimId != null) {
                claimDataManager.removeClaimData(claimId);
                plugin.debug("Synced claim deletion: " + claimId);
            }
        }
    }

    // =====================
    // 일반 설정 이벤트
    // =====================

    /**
     * 외부인 입장 제한 기능
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 성능 최적화: 블록 이동이 아니면 무시
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (!claimManager.isEnabled())
            return;

        Player player = event.getPlayer();

        // OP나 관리자 권한 있는 플레이어는 무시
        if (player.isOp() || player.hasPermission("wildcore.claim.bypass")) {
            return;
        }

        // 이동 대상 위치의 Claim 확인
        Claim claim = claimManager.getClaimAt(event.getTo());
        if (claim == null)
            return;

        // 본인 땅이면 무시
        if (claimManager.isClaimOwner(claim, player.getUniqueId())) {
            return;
        }

        // Trust된 플레이어면 무시
        if (claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        // 입장 제한 플래그 확인
        boolean blockEntry = getFlag(claim, ClaimFlags.BLOCK_ENTRY);
        if (blockEntry) {
            event.setCancelled(true);

            // 메시지는 스팸 방지를 위해 가끔만 표시
            if (System.currentTimeMillis() % 2000 < 50) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지에는 입장할 수 없습니다.");
            }
        }
    }

    /**
     * PvP 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntity() instanceof Player victim))
            return;

        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim))
            return;

        Claim claim = claimManager.getClaimAt(victim.getLocation());
        if (claim == null)
            return;

        // PvP 플래그가 꺼져있으면 PvP 차단
        if (!getFlag(claim, ClaimFlags.PVP)) {
            event.setCancelled(true);
            if (System.currentTimeMillis() % 2000 < 50) {
                attacker.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지에서는 PvP가 허용되지 않습니다.");
            }
        }
    }

    /**
     * 몬스터 스폰 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!claimManager.isEnabled())
            return;

        Entity entity = event.getEntity();
        Claim claim = claimManager.getClaimAt(entity.getLocation());
        if (claim == null)
            return;

        // 적대적 몹 (몬스터) 입장/스폰 제한
        if (entity instanceof Monster) {
            // MOB_ENTRY 플래그: 활성화 시 (true) 적대적 몹이 사유지에 스폰되는 것을 차단
            if (getFlag(claim, ClaimFlags.MOB_ENTRY)) {
                event.setCancelled(true);
                return;
            }
            if (!getFlag(claim, ClaimFlags.MOB_SPAWN)) {
                event.setCancelled(true);
            }
        }
        // 동물 스폰 제한
        else if (entity instanceof Animals) {
            if (!getFlag(claim, ClaimFlags.ANIMAL_SPAWN)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 적대적 몹 피해 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHostileDamage(EntityDamageByEntityEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntity() instanceof Player victim))
            return;

        Entity damager = event.getDamager();
        if (!(damager instanceof Monster)
                && !(damager instanceof Projectile projectile && projectile.getShooter() instanceof Monster))
            return;

        Claim claim = claimManager.getClaimAt(victim.getLocation());
        if (claim == null)
            return;

        // 적대적 몹 피해 플래그가 꺼져있으면 피해 차단
        if (!getFlag(claim, ClaimFlags.HOSTILE_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    /**
     * 불 번짐 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        // 불 번짐이 꺼져있고, 플레이어가 직접 놓은 게 아니면 취소
        if (!getFlag(claim, ClaimFlags.FIRE_SPREAD)) {
            if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD ||
                    event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 불 번짐 (블록 타기) 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        if (!getFlag(claim, ClaimFlags.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    /**
     * 폭발 피해 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!claimManager.isEnabled())
            return;

        // 폭발 영향을 받는 블록 중 claim 내 블록 제거
        event.blockList().removeIf(block -> {
            Claim claim = claimManager.getClaimAt(block.getLocation());
            if (claim != null) {
                return !getFlag(claim, ClaimFlags.EXPLOSIONS);
            }
            return false;
        });
    }

    /**
     * 블록 폭발 피해 제한 (침대, 리스폰 앵커 등)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!claimManager.isEnabled())
            return;

        event.blockList().removeIf(block -> {
            Claim claim = claimManager.getClaimAt(block.getLocation());
            if (claim != null) {
                return !getFlag(claim, ClaimFlags.EXPLOSIONS);
            }
            return false;
        });
    }

    /**
     * 엔더맨 그리핑 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEndermanGrief(EntityChangeBlockEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntity() instanceof Enderman))
            return;

        Claim claim = claimManager.getClaimAt(event.getBlock().getLocation());
        if (claim == null)
            return;

        if (!getFlag(claim, ClaimFlags.ENDERMAN_GRIEF)) {
            event.setCancelled(true);
        }
    }

    // =====================
    // 농경 설정 이벤트
    // =====================

    /**
     * 농작물 밟기 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCropTrample(EntityChangeBlockEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND)
            return;

        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        if (!getFlag(claim, ClaimFlags.CROP_TRAMPLE)) {
            event.setCancelled(true);
        }
    }

    /**
     * 농작물/덩굴 성장 제한 + 작물 성장 버프 가속
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        Material type = event.getNewState().getType();

        // 덩굴류 확인
        if (isVine(type)) {
            if (!getFlag(claim, ClaimFlags.VINE_GROWTH)) {
                event.setCancelled(true);
            }
            return;
        }

        // 일반 농작물 성장 플래그 확인
        if (!getFlag(claim, ClaimFlags.CROP_GROWTH)) {
            event.setCancelled(true);
            return;
        }

        // 작물 성장 버프 가속 처리
        if (cropGrowthManager != null && cropGrowthManager.hasActiveBuff(claim.getID())) {
            double multiplier = cropGrowthManager.getBuffMultiplier(claim.getID());
            if (multiplier > 1.0 && block.getBlockData() instanceof Ageable ageable) {
                // 추가 성장 단계 계산 (예: 2.0x → 1번 추가, 3.0x → 2번 추가)
                int additionalGrowth = (int) (multiplier - 1);
                double fractional = (multiplier - 1) - additionalGrowth;
                if (Math.random() < fractional) {
                    additionalGrowth++;
                }

                if (additionalGrowth > 0) {
                    int currentAge = ageable.getAge();
                    int maxAge = ageable.getMaximumAge();
                    int newAge = Math.min(currentAge + additionalGrowth, maxAge);
                    if (newAge > currentAge) {
                        ageable.setAge(newAge);
                        block.setBlockData(ageable);
                    }
                }
            }
        }
    }

    /**
     * 동물 피해 제한 (외부인)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAnimalDamage(EntityDamageByEntityEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntity() instanceof Animals animal))
            return;

        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null)
            return;

        Claim claim = claimManager.getClaimAt(animal.getLocation());
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, attacker.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, attacker.getUniqueId()) != null) {
            return;
        }

        // 동물 피해 플래그가 꺼져있으면 차단
        if (!getFlag(claim, ClaimFlags.ANIMAL_DAMAGE)) {
            event.setCancelled(true);
            if (System.currentTimeMillis() % 2000 < 50) {
                attacker.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지의 동물을 공격할 수 없습니다.");
            }
        }
    }

    /**
     * 낚시 제한 (외부인)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (!claimManager.isEnabled())
            return;

        // 낚시 찌가 물에 닿았을 때만 체크 (불필요한 체크 방지)
        if (event.getState() != PlayerFishEvent.State.FISHING
                && event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }

        Player player = event.getPlayer();

        // 낚시 찌 위치 또는 플레이어 위치의 Claim 확인
        Location hookLocation = event.getHook().getLocation();
        Claim claim = claimManager.getClaimAt(hookLocation);
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, player.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        // OP나 관리자 무시
        if (player.isOp() || player.hasPermission("wildcore.claim.bypass")) {
            return;
        }

        // FISHING 플래그가 꺼져있으면 낚시 차단
        if (!getFlag(claim, ClaimFlags.FISHING)) {
            event.setCancelled(true);
            if (System.currentTimeMillis() % 3000 < 50) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지에서는 낚시가 허용되지 않습니다.");
            }
        }
    }

    // =====================
    // 상호작용 설정 이벤트
    // =====================

    /**
     * 상자/버튼/레버/문 접근 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, player.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        // OP나 관리자는 무시
        if (player.isOp() || player.hasPermission("wildcore.claim.bypass")) {
            return;
        }

        Material type = block.getType();

        // 상자류 접근 확인
        if (isContainer(type)) {
            if (!getFlag(claim, ClaimFlags.CONTAINER_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지의 상자에 접근할 수 없습니다.");
                return;
            }
        }

        // 버튼/레버 사용 확인
        if (isButtonOrLever(type)) {
            if (!getFlag(claim, ClaimFlags.BUTTON_LEVER)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지의 버튼/레버를 사용할 수 없습니다.");
                return;
            }
        }

        // 문 사용 확인
        if (isDoor(type)) {
            if (!getFlag(claim, ClaimFlags.DOOR_ACCESS)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지의 문/게이트를 사용할 수 없습니다.");
            }
        }
    }

    /**
     * 탈것 사용 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntered() instanceof Player player))
            return;

        Vehicle vehicle = event.getVehicle();
        Claim claim = claimManager.getClaimAt(vehicle.getLocation());
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, player.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        if (!getFlag(claim, ClaimFlags.VEHICLE_USE)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c이 사유지에서 탈것을 사용할 수 없습니다.");
        }
    }

    /**
     * 아이템 드롭 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!claimManager.isEnabled())
            return;

        Player player = event.getPlayer();
        Claim claim = claimManager.getClaimAt(player.getLocation());
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, player.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        if (!getFlag(claim, ClaimFlags.ITEM_DROP)) {
            event.setCancelled(true);
            if (System.currentTimeMillis() % 2000 < 50) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c이 사유지에서 아이템을 버릴 수 없습니다.");
            }
        }
    }

    /**
     * 아이템 줍기 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!claimManager.isEnabled())
            return;

        if (!(event.getEntity() instanceof Player player))
            return;

        Claim claim = claimManager.getClaimAt(event.getItem().getLocation());
        if (claim == null)
            return;

        // 본인 땅이거나 Trust된 경우 허용
        if (claimManager.isClaimOwner(claim, player.getUniqueId()) ||
                claimManager.getPlayerTrustLevel(claim, player.getUniqueId()) != null) {
            return;
        }

        if (!getFlag(claim, ClaimFlags.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    // =====================
    // 환경 설정 이벤트
    // =====================

    /**
     * 나뭇잎 분해 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        if (!getFlag(claim, ClaimFlags.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    /**
     * 눈/얼음 생성 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        Material newType = event.getNewState().getType();

        // 눈 생성
        if (newType == Material.SNOW || newType == Material.SNOW_BLOCK) {
            if (!getFlag(claim, ClaimFlags.SNOW_FALL)) {
                event.setCancelled(true);
            }
        }
        // 얼음 생성
        else if (newType == Material.ICE || newType == Material.FROSTED_ICE) {
            if (!getFlag(claim, ClaimFlags.ICE_FORM)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 덩굴 퍼짐 제한
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!claimManager.isEnabled())
            return;

        Block block = event.getBlock();
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        Material sourceType = event.getSource().getType();

        // 덩굴류
        if (isVine(sourceType)) {
            if (!getFlag(claim, ClaimFlags.VINE_GROWTH)) {
                event.setCancelled(true);
            }
        }
        // 불 번짐
        else if (sourceType == Material.FIRE) {
            if (!getFlag(claim, ClaimFlags.FIRE_SPREAD)) {
                event.setCancelled(true);
            }
        }
    }

    // =====================
    // 유틸리티 메소드
    // =====================

    /**
     * 플래그 값을 가져옵니다.
     */
    private boolean getFlag(Claim claim, ClaimFlags flag) {
        return claimDataManager.getClaimFlag(claim.getID(), flag.getKey(), flag.getDefaultValue());
    }

    /**
     * 공격자로부터 플레이어를 추출합니다.
     */
    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    /**
     * 컨테이너 블록인지 확인합니다.
     */
    private boolean isContainer(Material type) {
        return type == Material.CHEST ||
                type == Material.TRAPPED_CHEST ||
                type == Material.BARREL ||
                type == Material.SHULKER_BOX ||
                type.name().endsWith("_SHULKER_BOX") ||
                type == Material.FURNACE ||
                type == Material.BLAST_FURNACE ||
                type == Material.SMOKER ||
                type == Material.HOPPER ||
                type == Material.DISPENSER ||
                type == Material.DROPPER ||
                type == Material.BREWING_STAND;
    }

    /**
     * 버튼/레버인지 확인합니다.
     */
    private boolean isButtonOrLever(Material type) {
        return type == Material.LEVER ||
                type.name().endsWith("_BUTTON") ||
                type == Material.TRIPWIRE_HOOK ||
                type.name().contains("PRESSURE_PLATE");
    }

    /**
     * 문인지 확인합니다.
     */
    private boolean isDoor(Material type) {
        return type.name().endsWith("_DOOR") ||
                type.name().endsWith("_GATE") ||
                type.name().endsWith("_TRAPDOOR");
    }

    /**
     * 덩굴류인지 확인합니다.
     */
    private boolean isVine(Material type) {
        return type == Material.VINE ||
                type == Material.KELP ||
                type == Material.KELP_PLANT ||
                type == Material.TWISTING_VINES ||
                type == Material.WEEPING_VINES ||
                type == Material.CAVE_VINES;
    }

    /**
     * 플레이어가 사유지에 들어왔는지 확인하는 유틸리티 메소드
     */
    public boolean isEnteringClaim(PlayerMoveEvent event) {
        Claim fromClaim = claimManager.getClaimAt(event.getFrom());
        Claim toClaim = claimManager.getClaimAt(event.getTo());
        return fromClaim == null && toClaim != null;
    }

    /**
     * 플레이어가 사유지에서 나갔는지 확인하는 유틸리티 메소드
     */
    public boolean isLeavingClaim(PlayerMoveEvent event) {
        Claim fromClaim = claimManager.getClaimAt(event.getFrom());
        Claim toClaim = claimManager.getClaimAt(event.getTo());
        return fromClaim != null && toClaim == null;
    }

    // =====================
    // MOB_ENTRY 주기적 태스크
    // =====================

    /**
     * MOB_ENTRY 플래그가 활성화된 사유지에서 적대적 몹을 주기적으로 제거합니다.
     * 외부에서 걸어 들어오는 몹을 차단하기 위한 보완 기능입니다.
     * 10초 간격으로 실행됩니다.
     */
    private void startMobEntryTask() {
        mobEntryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!claimManager.isEnabled())
                return;

            for (World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof Monster monster))
                        continue;

                    Claim claim = claimManager.getClaimAt(monster.getLocation());
                    if (claim == null)
                        continue;

                    // MOB_ENTRY 플래그가 활성화(true)면 몹 제거
                    if (getFlag(claim, ClaimFlags.MOB_ENTRY)) {
                        monster.remove();
                    }
                }
            }
        }, 200L, 200L); // 10초 간격 (200 ticks)
    }

    /**
     * MOB_ENTRY 태스크 중지 (플러그인 비활성화 시)
     */
    public void stopMobEntryTask() {
        if (mobEntryTask != null) {
            mobEntryTask.cancel();
            mobEntryTask = null;
        }
    }
}
