package com.myserver.wildcore.gui.claim;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Claim GUI 이벤트 리스너
 */
public class ClaimGUIListener implements Listener {

    private final WildCore plugin;

    public ClaimGUIListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // ClaimListGUI 처리
        if (event.getInventory().getHolder() instanceof ClaimListGUI gui) {
            event.setCancelled(true);
            handleListGUIClick(player, gui, event.getSlot());
            return;
        }

        // ClaimMainGUI 처리
        if (event.getInventory().getHolder() instanceof ClaimMainGUI gui) {
            event.setCancelled(true);
            handleMainGUIClick(player, gui, event.getSlot(), event.getClick());
            return;
        }

        // ClaimSettingsGUI 처리
        if (event.getInventory().getHolder() instanceof ClaimSettingsGUI gui) {
            event.setCancelled(true);
            handleSettingsGUIClick(player, gui, event.getSlot(), event.getClick());
            return;
        }

        // ClaimMemberGUI 처리
        if (event.getInventory().getHolder() instanceof ClaimMemberGUI gui) {
            event.setCancelled(true);
            handleMemberGUIClick(player, gui, event.getSlot(), event.getClick());
            return;
        }
    }

    private void handleListGUIClick(Player player, ClaimListGUI gui, int slot) {
        if (!gui.isClaimSlot(slot))
            return;

        Claim claim = gui.getClaimAtSlot(slot);
        if (claim == null)
            return;

        new ClaimMainGUI(plugin, player, claim).open();
    }

    private void handleMainGUIClick(Player player, ClaimMainGUI gui, int slot, ClickType click) {
        Claim claim = gui.getClaim();

        // 뒤로가기
        if (gui.isBackSlot(slot)) {
            new ClaimListGUI(plugin, player).open();
            return;
        }

        // 정보 (별명 변경)
        if (gui.isInfoSlot(slot)) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§e채팅에 새로운 별명을 입력하세요. (취소: cancel)");
            plugin.getChatListener().setPendingNicknameInput(player.getUniqueId(), claim.getID());
            return;
        }

        // 멤버 관리
        if (gui.isMembersSlot(slot)) {
            new ClaimMemberGUI(plugin, player, claim).open();
            return;
        }

        // 설정
        if (gui.isSettingsSlot(slot)) {
            new ClaimSettingsGUI(plugin, player, claim).open();
            return;
        }

        // 청크 로드 토글
        if (gui.isChunkLoadSlot(slot)) {
            boolean currentState = gui.getMetadata().isChunkLoaded();
            plugin.getClaimChunkLoader().toggleChunkLoading(claim.getID(), !currentState);
            if (!currentState) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§a청크 자동 로드가 활성화되었습니다!");
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c청크 자동 로드가 비활성화되었습니다.");
            }
            gui.refresh();
            return;
        }

        // 워프
        if (gui.isWarpSlot(slot)) {
            Location home = gui.getMetadata().getHome();
            if (home == null) {
                home = plugin.getClaimManager().getClaimCenter(claim);
            }

            if (home != null) {
                player.closeInventory();
                player.teleport(home);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§a사유지로 이동했습니다!");
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c워프 위치를 찾을 수 없습니다.");
            }
            return;
        }

        // 홈 설정
        if (gui.isSetHomeSlot(slot)) {
            Claim currentClaim = plugin.getClaimManager().getClaimAt(player.getLocation());
            if (currentClaim == null || !currentClaim.getID().equals(claim.getID())) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c사유지 안에서만 홈을 설정할 수 있습니다.");
                return;
            }

            plugin.getClaimDataManager().setClaimHome(claim.getID(), player.getLocation());
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§a현재 위치가 사유지 홈으로 설정되었습니다!");
            gui.refresh();
            return;
        }

        // 작물 버프 상태 (정보 표시 전용)
        if (gui.isBuffStatusSlot(slot)) {
            // 정보 표시 전용
            return;
        }

        // 삭제
        if (gui.isDeleteSlot(slot)) {
            if (click.isShiftClick()) {
                player.closeInventory();

                // 청크 로드 해제
                if (gui.getMetadata().isChunkLoaded()) {
                    plugin.getClaimChunkLoader().toggleChunkLoading(claim.getID(), false);
                }

                // 작물 버프 해제
                if (plugin.getCropGrowthManager().hasActiveBuff(claim.getID())) {
                    plugin.getCropGrowthManager().deactivateBuff(claim.getID());
                }

                boolean success = plugin.getClaimManager().deleteClaim(claim);
                if (success) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§a사유지가 성공적으로 삭제되었습니다.");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c사유지 삭제에 실패했습니다.");
                }
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c삭제하려면 쉬프트 + 클릭하세요.");
            }
            return;
        }
    }

    private void handleSettingsGUIClick(Player player, ClaimSettingsGUI gui, int slot, ClickType click) {
        // 뒤로가기
        if (gui.isBackSlot(slot)) {
            new ClaimMainGUI(plugin, player, gui.getClaim()).open();
            return;
        }

        // 카테고리 탭 클릭
        if (gui.isCategorySlot(slot)) {
            gui.handleCategoryClick(slot);
            return;
        }

        // 전체 토글 버튼
        if (gui.isToggleAllSlot(slot)) {
            if (click.isLeftClick()) {
                gui.toggleAllFlags(true);
            } else if (click.isRightClick()) {
                gui.toggleAllFlags(false);
            }
            return;
        }

        // 플래그 토글
        if (gui.isFlagSlot(slot)) {
            gui.toggleFlag(slot);
            return;
        }
    }

    private void handleMemberGUIClick(Player player, ClaimMemberGUI gui, int slot, ClickType click) {
        // 뒤로가기
        if (gui.isBackSlot(slot)) {
            new ClaimMainGUI(plugin, player, gui.getClaim()).open();
            return;
        }

        // 멤버 추가
        if (gui.isAddMemberSlot(slot)) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§e추가할 플레이어 이름을 채팅에 입력하세요. (취소: cancel)");
            plugin.getChatListener().setPendingMemberInput(player.getUniqueId(), gui.getClaim().getID());
            return;
        }

        // 멤버 슬롯
        if (gui.isMemberSlot(slot)) {
            if (click == ClickType.SHIFT_RIGHT) {
                gui.removeMember(slot);
            } else if (click == ClickType.LEFT) {
                gui.cycleMemberTrust(slot);
            }
            return;
        }
    }
}
