package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * GriefPrevention 플러그인과 연동하여 사유지 생성 및 검증을 담당하는 매니저 클래스입니다.
 * Trust 관리, Claim 조회 등의 심화 기능을 제공합니다.
 */
public class ClaimManager {

    private final WildCore plugin;
    private GriefPrevention gpInstance;
    private DataStore dataStore;
    private boolean enabled;

    /**
     * Trust 타입 정의
     */
    public enum TrustType {
        ACCESS, // 출입만 가능
        CONTAINER, // 창고 사용 가능
        BUILD, // 건축 가능
        MANAGER // 관리자 권한
    }

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

    // =====================
    // Claim 조회 API
    // =====================

    /**
     * 해당 위치의 Claim을 조회합니다.
     *
     * @param location 조회할 위치
     * @return Claim 객체 (없으면 null)
     */
    public Claim getClaimAt(Location location) {
        if (!isEnabled())
            return null;
        return dataStore.getClaimAt(location, true, null);
    }

    /**
     * Claim ID로 Claim을 조회합니다.
     *
     * @param claimId Claim ID
     * @return Claim 객체 (없으면 null)
     */
    public Claim getClaimById(Long claimId) {
        if (!isEnabled() || claimId == null)
            return null;
        return dataStore.getClaim(claimId);
    }

    /**
     * 플레이어가 소유한 모든 Claim 목록을 조회합니다.
     *
     * @param playerUUID 플레이어 UUID
     * @return Claim 목록
     */
    public List<Claim> getPlayerClaims(UUID playerUUID) {
        if (!isEnabled())
            return Collections.emptyList();
        PlayerData playerData = dataStore.getPlayerData(playerUUID);
        return playerData.getClaims();
    }

    /**
     * 플레이어가 해당 Claim의 주인인지 확인합니다.
     *
     * @param claim      Claim 객체
     * @param playerUUID 플레이어 UUID
     * @return 주인 여부
     */
    public boolean isClaimOwner(Claim claim, UUID playerUUID) {
        if (claim == null || playerUUID == null)
            return false;
        return playerUUID.equals(claim.ownerID);
    }

    /**
     * 플레이어가 해당 Claim에서 권한이 있는지 확인합니다.
     *
     * @param claim      Claim 객체
     * @param player     플레이어
     * @param permission 확인할 권한
     * @return 권한 유무 (null이면 권한 있음, 문자열이면 거부 사유)
     */
    public String checkPermission(Claim claim, Player player, ClaimPermission permission) {
        if (claim == null)
            return null;
        // GP API 버전에 따라 반환 타입이 다를 수 있음
        var result = claim.checkPermission(player, permission, null);
        if (result == null) {
            return null;
        }
        // Supplier<String> 타입인 경우 get() 호출
        if (result instanceof java.util.function.Supplier) {
            return ((java.util.function.Supplier<String>) result).get();
        }
        return result.toString();
    }

    // =====================
    // Trust 관리 API
    // =====================

    /**
     * Claim에 플레이어를 Trust로 추가합니다.
     *
     * @param claim      Claim 객체
     * @param playerUUID 추가할 플레이어 UUID
     * @param type       Trust 타입
     * @return 성공 여부
     */
    public boolean addTrust(Claim claim, UUID playerUUID, TrustType type) {
        if (!isEnabled() || claim == null || playerUUID == null)
            return false;

        try {
            String playerName = playerUUID.toString();

            switch (type) {
                case ACCESS:
                    claim.setPermission(playerName, ClaimPermission.Access);
                    break;
                case CONTAINER:
                    claim.setPermission(playerName, ClaimPermission.Inventory);
                    break;
                case BUILD:
                    claim.setPermission(playerName, ClaimPermission.Build);
                    break;
                case MANAGER:
                    if (!claim.managers.contains(playerName)) {
                        claim.managers.add(playerName);
                    }
                    // 매니저는 Build 권한도 필요
                    claim.setPermission(playerName, ClaimPermission.Build);
                    break;
            }

            dataStore.saveClaim(claim);
            plugin.debug("Added trust " + type + " for " + playerUUID + " in claim " + claim.getID());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add trust: " + e.getMessage());
            return false;
        }
    }

    /**
     * Claim에서 플레이어의 Trust를 제거합니다.
     *
     * @param claim      Claim 객체
     * @param playerUUID 제거할 플레이어 UUID
     * @return 성공 여부
     */
    public boolean removeTrust(Claim claim, UUID playerUUID) {
        if (!isEnabled() || claim == null || playerUUID == null)
            return false;

        try {
            String playerName = playerUUID.toString();
            claim.dropPermission(playerName);
            claim.managers.remove(playerName);
            dataStore.saveClaim(claim);
            plugin.debug("Removed trust for " + playerUUID + " in claim " + claim.getID());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove trust: " + e.getMessage());
            return false;
        }
    }

    /**
     * Claim에 Trust된 모든 플레이어와 그들의 권한을 조회합니다.
     *
     * @param claim Claim 객체
     * @return UUID와 TrustType의 맵
     */
    public Map<UUID, TrustType> getTrustedPlayers(Claim claim) {
        Map<UUID, TrustType> result = new HashMap<>();
        if (!isEnabled() || claim == null)
            return result;

        try {
            // Builder 권한 조회
            ArrayList<String> builders = new ArrayList<>();
            ArrayList<String> containers = new ArrayList<>();
            ArrayList<String> accessors = new ArrayList<>();
            ArrayList<String> managers = new ArrayList<>(claim.managers);

            claim.getPermissions(builders, containers, accessors, managers);

            // 매니저 처리
            for (String name : managers) {
                UUID uuid = parseUUID(name);
                if (uuid != null) {
                    result.put(uuid, TrustType.MANAGER);
                }
            }

            // 빌더 처리 (매니저가 아닌 경우만)
            for (String name : builders) {
                UUID uuid = parseUUID(name);
                if (uuid != null && !result.containsKey(uuid)) {
                    result.put(uuid, TrustType.BUILD);
                }
            }

            // 컨테이너 처리
            for (String name : containers) {
                UUID uuid = parseUUID(name);
                if (uuid != null && !result.containsKey(uuid)) {
                    result.put(uuid, TrustType.CONTAINER);
                }
            }

            // 접근자 처리
            for (String name : accessors) {
                UUID uuid = parseUUID(name);
                if (uuid != null && !result.containsKey(uuid)) {
                    result.put(uuid, TrustType.ACCESS);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get trusted players: " + e.getMessage());
        }

        return result;
    }

    /**
     * 문자열을 UUID로 파싱합니다.
     */
    private UUID parseUUID(String name) {
        if (name == null || name.isEmpty())
            return null;

        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            // UUID 형식이 아닌 경우 플레이어 이름으로 시도
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer.getUniqueId();
            }
            return null;
        }
    }

    /**
     * 플레이어가 해당 Claim에서 가진 Trust 레벨을 조회합니다.
     *
     * @param claim      Claim 객체
     * @param playerUUID 플레이어 UUID
     * @return Trust 타입 (없으면 null)
     */
    public TrustType getPlayerTrustLevel(Claim claim, UUID playerUUID) {
        if (!isEnabled() || claim == null || playerUUID == null)
            return null;

        Map<UUID, TrustType> trustedPlayers = getTrustedPlayers(claim);
        return trustedPlayers.get(playerUUID);
    }

    // =====================
    // Claim 삭제 API
    // =====================

    /**
     * Claim을 삭제합니다.
     *
     * @param claim Claim 객체
     * @return 성공 여부
     */
    public boolean deleteClaim(Claim claim) {
        if (!isEnabled() || claim == null)
            return false;

        try {
            dataStore.deleteClaim(claim);
            plugin.debug("Deleted claim " + claim.getID());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete claim: " + e.getMessage());
            return false;
        }
    }

    // =====================
    // 유틸리티 메소드
    // =====================

    /**
     * Claim의 면적을 계산합니다.
     *
     * @param claim Claim 객체
     * @return 면적 (블록 수)
     */
    public int getClaimArea(Claim claim) {
        if (claim == null)
            return 0;
        return claim.getArea();
    }

    /**
     * Claim의 중심점을 계산합니다.
     *
     * @param claim Claim 객체
     * @return 중심 위치
     */
    public Location getClaimCenter(Claim claim) {
        if (claim == null)
            return null;

        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();

        if (lesser == null || greater == null)
            return null;

        double centerX = (lesser.getBlockX() + greater.getBlockX()) / 2.0;
        double centerZ = (lesser.getBlockZ() + greater.getBlockZ()) / 2.0;
        double centerY = lesser.getWorld().getHighestBlockYAt((int) centerX, (int) centerZ) + 1;

        return new Location(lesser.getWorld(), centerX, centerY, centerZ);
    }

    /**
     * Claim의 크기 정보를 가져옵니다.
     *
     * @param claim Claim 객체
     * @return "WxL" 형식의 문자열 (예: "11x11")
     */
    public String getClaimSize(Claim claim) {
        if (claim == null)
            return "0x0";

        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();

        if (lesser == null || greater == null)
            return "0x0";

        int width = greater.getBlockX() - lesser.getBlockX() + 1;
        int length = greater.getBlockZ() - lesser.getBlockZ() + 1;

        return width + "x" + length;
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
