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

    private void handleMainGUIClick(Player player, ClaimMainGUI gui, int slot, ClickType click) {
        Claim claim = gui.getClaim();

        // 정보 (별명 변경)
        if (gui.isInfoSlot(slot)) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§e채팅에 새로운 별명을 입력하세요. (취소: cancel)");
            // TODO: ChatListener 연동으로 별명 입력 처리
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

        // 워프
        if (gui.isWarpSlot(slot)) {
            Location home = gui.getMetadata().getHome();
            if (home == null) {
                // 홈이 설정되지 않았으면 중심점으로 이동
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
            // 플레이어가 사유지 안에 있는지 확인
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

        // 삭제
        if (gui.isDeleteSlot(slot)) {
            if (click.isShiftClick()) {
                player.closeInventory();
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
        // 먼저 카테고리 슬롯인지 확인하고 처리
        if (gui.isCategorySlot(slot)) {
            gui.handleCategoryClick(slot);
            return;
        }

        // 전체 토글 버튼
        if (gui.isToggleAllSlot(slot)) {
            if (click.isLeftClick()) {
                gui.toggleAllFlags(true); // 모두 켜기
            } else if (click.isRightClick()) {
                gui.toggleAllFlags(false); // 모두 끄기
            }
            return;
        }

        // 플래그 토글
        // 마지막으로 플래그 슬롯인지 확인
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
            // TODO: ChatListener 연동으로 멤버 이름 입력 처리
            return;
        }

        // 멤버 슬롯
        if (gui.isMemberSlot(slot)) {
            if (click == ClickType.SHIFT_RIGHT) {
                // 쉬프트 + 우클릭: 멤버 제거
                gui.removeMember(slot);
            } else if (click == ClickType.LEFT) {
                // 좌클릭: 등급 변경
                gui.cycleMemberTrust(slot);
            }
            return;
        }
    }
}
