package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GriefPrevention 플러그인과 연동하여 사유지 생성 및 검증을 담당하는 매니저 클래스입니다.
 */
public class ClaimManager {

    private final WildCore plugin;
    private GriefPrevention gpInstance;
    private DataStore dataStore;
    private boolean enabled;

    public ClaimManager(WildCore plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().isClaimSystemEnabled();
        if (enabled) {
            setupGriefPrevention();
        }
    }

    /**
     * GriefPrevention 플러그인이 로드되었는지 확인하고 인스턴스를 가져옵니다.
     */
    private void setupGriefPrevention() {
        if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            this.gpInstance = GriefPrevention.instance;
            this.dataStore = this.gpInstance.dataStore;
            plugin.getLogger().info("GriefPrevention hook established successfully.");
        } else {
            plugin.getLogger().warning("GriefPrevention plugin not found! Claim features will be disabled.");
            this.enabled = false;
        }
    }

    /**
     * 클레임 시스템이 활성화되었는지 확인합니다.
     */
    public boolean isEnabled() {
        return enabled && gpInstance != null && dataStore != null;
    }

    /**
     * 해당 월드에서 사유지 생성이 허용되는지 확인합니다.
     */
    public boolean isWorldAllowed(World world) {
        List<String> allowedWorlds = plugin.getConfigManager().getClaimAllowedWorlds();
        if (allowedWorlds == null || allowedWorlds.isEmpty()) {
            return true; // 빈 리스트면 모든 월드 허용
        }
        return allowedWorlds.contains(world.getName());
    }

    /**
     * 해당 위치에 사유지를 생성할 수 있는지 검사합니다 (중복 검사 등).
     *
     * @param center 중심 좌표
     * @param radius 반지름
     * @param player 플레이어 (권한 체크용)
     * @return 생성 가능 여부
     */
    public boolean canCreateClaim(Location center, int radius, Player player) {
        if (!isEnabled())
            return false;

        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        World world = center.getWorld();

        // 네 꼭짓점과 중앙에 기존 클레임이 있는지 확인
        Location[] checkPoints = {
                new Location(world, minX, center.getBlockY(), minZ),
                new Location(world, maxX, center.getBlockY(), maxZ),
                new Location(world, minX, center.getBlockY(), maxZ),
                new Location(world, maxX, center.getBlockY(), minZ),
                center
        };

        for (Location loc : checkPoints) {
            Claim existingClaim = dataStore.getClaimAt(loc, true, null);
            if (existingClaim != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * 플레이어가 부족한 클레임 블록을 계산합니다.
     *
     * @param player 플레이어
     * @param area   필요한 면적
     * @return 부족한 블록 수 (0이면 충분함)
     */
    public int getNeededBlocks(Player player, int area) {
        if (!isEnabled())
            return 0;

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        int remaining = playerData.getRemainingClaimBlocks();

        if (remaining >= area) {
            return 0;
        }
        return area - remaining;
    }

    /**
     * 플레이어에게 보너스 클레임 블록을 지급합니다.
     *
     * @param player 플레이어
     * @param amount 지급할 블록 수
     */
    public void grantBonusBlocks(Player player, int amount) {
        if (!isEnabled())
            return;

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + amount);
        plugin.debug("Granted " + amount + " bonus claim blocks to " + player.getName());
    }

    /**
     * 실제로 사유지를 생성합니다.
     *
     * @param player 생성할 플레이어
     * @param center 중심 좌표
     * @param radius 반지름
     * @return 생성 결과 (CreateClaimResult)
     */
    public CreateClaimResult createClaim(Player player, Location center, int radius) {
        if (!isEnabled())
            return null;

        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        World world = center.getWorld();

        int area = (maxX - minX + 1) * (maxZ - minZ + 1);

        // 부족한 클레임 블록 자동 지급 (설정에 따라)
        if (plugin.getConfigManager().isClaimAutoGrantBlocks()) {
            int needed = getNeededBlocks(player, area);
            if (needed > 0) {
                grantBonusBlocks(player, needed);
            }
        }

        // GP API를 통한 사유지 생성
        // GP 16.x 버전의 DataStore.createClaim 메서드 시그니처:
        // createClaim(World, x1, x2, y1, y2, z1, z2, ownerID, parent, id,
        // creatingPlayer)
        // y1, y2는 현재 GP에서 실제로 사용되지 않으므로 0, 256으로 설정
        int y1 = world.getMinHeight();
        int y2 = world.getMaxHeight();

        return dataStore.createClaim(
                world,
                minX, maxX,
                y1, y2,
                minZ, maxZ,
                player.getUniqueId(),
                null, // parent claim
                null, // claim id (auto-generated)
                player // creating player
        );
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        this.enabled = plugin.getConfigManager().isClaimSystemEnabled();
        if (enabled && gpInstance == null) {
            setupGriefPrevention();
        }
    }
}
