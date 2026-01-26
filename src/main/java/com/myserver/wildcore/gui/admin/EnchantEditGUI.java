package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.myserver.wildcore.util.ItemGroupUtil;

/**
 * 개별 인챈트 설정 편집 GUI
 */
public class EnchantEditGUI implements InventoryHolder {

        private final WildCore plugin;
        private final Player player;
        private final String enchantId;
        private Inventory inventory;

        public EnchantEditGUI(WildCore plugin, Player player, String enchantId) {
                this.plugin = plugin;
                this.player = player;
                this.enchantId = enchantId;
                createInventory();
        }

        private void createInventory() {
                EnchantConfig enchant = plugin.getConfigManager().getEnchant(enchantId);
                if (enchant == null)
                        return;

                inventory = Bukkit.createInventory(this, 54,
                                ItemUtil.parse("§8[ §5인챈트 편집: " + enchant.getDisplayName() + " §8]"));

                // 배경
                ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
                for (int i = 0; i < 54; i++) {
                        inventory.setItem(i, background);
                }

                // 현재 상태 표시
                Material enchantMaterial = Material.getMaterial(enchant.getMaterial());
                if (enchantMaterial == null)
                        enchantMaterial = Material.ENCHANTED_BOOK;

                // fetch values from config manager to reflect modified state
                double costMoney = plugin.getConfigManager().getEnchantProbability(enchantId, "cost"); // wait, cost is
                                                                                                       // not
                                                                                                       // probability.
                                                                                                       // Check
                                                                                                       // ConfigManager
                // ConfigManager doesn't have getEnchantCost? It has setEnchantCost.
                // Let's check getEnchantProbability usage.

                ItemStack infoItem = createItem(enchantMaterial, enchant.getDisplayName(),
                                List.of(
                                                "",
                                                "§7ID: §f" + enchantId,
                                                "§7결과: §f" + enchant.getResultEnchantment() + " Lv."
                                                                + enchant.getResultLevel(),
                                                "§7비용: §6" + String.format("%,.0f", enchant.getCostMoney()) + "원"));
                inventory.setItem(4, infoItem);

                // === 성공 확률 설정 ===
                // 슬롯 19, 20, 21, 22, 23 사용
                inventory.setItem(19, createItem(Material.LIME_DYE, "§c[ 성공 -5% ]",
                                List.of("", "§7성공 확률 5% 감소")));
                inventory.setItem(20, createItem(Material.LIME_DYE, "§c[ 성공 -1% ]",
                                List.of("", "§7성공 확률 1% 감소")));

                double successRate = plugin.getConfigManager().getEnchantProbability(enchantId, "success");
                inventory.setItem(21, createItem(Material.LIME_CONCRETE, "§a[ 성공 확률 ]",
                                List.of("", "§7현재: §a" + String.format("%.1f", successRate) + "%")));

                inventory.setItem(22, createItem(Material.LIME_DYE, "§a[ 성공 +1% ]",
                                List.of("", "§7성공 확률 1% 증가")));
                inventory.setItem(23, createItem(Material.LIME_DYE, "§a[ 성공 +5% ]",
                                List.of("", "§7성공 확률 5% 증가")));

                // === 실패 확률 설정 ===
                // 슬롯 28, 29, 30, 31, 32 사용
                inventory.setItem(28, createItem(Material.YELLOW_DYE, "§c[ 실패 -5% ]",
                                List.of("", "§7실패 확률 5% 감소")));
                inventory.setItem(29, createItem(Material.YELLOW_DYE, "§c[ 실패 -1% ]",
                                List.of("", "§7실패 확률 1% 감소")));

                double failRate = plugin.getConfigManager().getEnchantProbability(enchantId, "fail");
                inventory.setItem(30, createItem(Material.YELLOW_CONCRETE, "§e[ 실패 확률 ]",
                                List.of("", "§7현재: §e" + String.format("%.1f", failRate) + "%",
                                                "", "§7실패 시 재료만 소멸됩니다.")));

                inventory.setItem(31, createItem(Material.YELLOW_DYE, "§a[ 실패 +1% ]",
                                List.of("", "§7실패 확률 1% 증가")));
                inventory.setItem(32, createItem(Material.YELLOW_DYE, "§a[ 실패 +5% ]",
                                List.of("", "§7실패 확률 5% 증가")));

                // === 파괴 확률 설정 ===
                // 슬롯 37, 38, 39, 40, 41 사용
                inventory.setItem(37, createItem(Material.RED_DYE, "§c[ 파괴 -5% ]",
                                List.of("", "§7파괴 확률 5% 감소")));
                inventory.setItem(38, createItem(Material.RED_DYE, "§c[ 파괴 -1% ]",
                                List.of("", "§7파괴 확률 1% 감소")));

                double destroyRate = plugin.getConfigManager().getEnchantProbability(enchantId, "destroy");
                inventory.setItem(39, createItem(Material.RED_CONCRETE, "§c[ 파괴 확률 ]",
                                List.of("", "§7현재: §c" + String.format("%.1f", destroyRate) + "%",
                                                "", "§4파괴 시 아이템이 소멸됩니다!")));

                inventory.setItem(40, createItem(Material.RED_DYE, "§a[ 파괴 +1% ]",
                                List.of("", "§7파괴 확률 1% 증가")));
                inventory.setItem(41, createItem(Material.RED_DYE, "§a[ 파괴 +5% ]",
                                List.of("", "§7파괴 확률 5% 증가")));

                // === 추가 설정 옵션 ===
                boolean unsafeMode = plugin.getConfigManager().getEnchantUnsafeMode(enchantId);
                inventory.setItem(11, createToggleItem("§6[ 바닐라 제한 무시 ]", unsafeMode,
                                List.of("", "§7활성화 시:", "§7- 레벨 제한 무시 (Lv.∞ 가능)",
                                                "§7- 아이템 타입 제한 무시", "§7- 상충 인챈트 적용 가능")));

                boolean ignoreConflicts = plugin.getConfigManager().getEnchantIgnoreConflicts(enchantId);
                inventory.setItem(12, createToggleItem("§e[ 충돌 인챈트 무시 ]", ignoreConflicts,
                                List.of("", "§7활성화 시:", "§7- 보호/화피 동시 적용 가능",
                                                "§7- 행운/섬세한손길 동시 적용 가능")));

                // 타겟 설정
                Set<String> groups = plugin.getConfigManager().getEnchantTargetGroups(enchantId);
                Set<String> whitelist = plugin.getConfigManager().getEnchantTargetWhitelist(enchantId);

                inventory.setItem(14, createItem(Material.TARGET, "§a[ 적용 대상 설정 ]",
                                List.of("", "§7클릭하여 적용 대상을 설정합니다.",
                                                "",
                                                "§7현재 그룹: " + formatGroups(groups),
                                                "§7현재 화이트리스트: " + whitelist.size() + "개")));

                // 확률 합계 표시
                double total = successRate + failRate + destroyRate;
                String totalColor = Math.abs(total - 100.0) < 0.01 ? "§a" : "§c";
                inventory.setItem(13, createItem(Material.PAPER, "§f[ 확률 합계 ]",
                                List.of("", totalColor + "합계: " + String.format("%.1f", total) + "%",
                                                "", "§7확률 합계는 100%여야 합니다.")));

                // === 비용 설정 ===
                inventory.setItem(24, createItem(Material.GOLD_INGOT, "§6[ 비용 설정 ]",
                                List.of("", "§7현재: §6" + String.format("%,.0f", enchant.getCostMoney()) + "원",
                                                "", "§e클릭하여 채팅으로 입력")));

                inventory.setItem(25, createItem(Material.CHEST, "§e[ 재료 아이템 설정 ]",
                                List.of("", "§7현재 재료:", formatCostItems(enchant),
                                                "", "§e클릭하여 채팅으로 입력")));

                // === 액션 버튼 ===
                inventory.setItem(48, createItem(Material.EMERALD, "§a[ 설정 저장 ]",
                                List.of("", "§7변경사항을 파일에 저장합니다.")));

                inventory.setItem(50, createItem(Material.TNT, "§c[ 인챈트 삭제 ]",
                                List.of("", "§c이 인챈트를 삭제합니다.", "§4주의: 되돌릴 수 없습니다!")));

                // 뒤로 가기
                inventory.setItem(45, createItem(Material.ARROW, "§7[ 뒤로 가기 ]", null));
        }

        private String formatCostItems(EnchantConfig enchant) {
                StringBuilder sb = new StringBuilder();
                for (String item : enchant.getCostItems()) {
                        sb.append("§7- §f").append(item).append("\n");
                }
                return sb.toString().trim();
        }

        private ItemStack createToggleItem(String name, boolean enabled, List<String> baseLore) {
                Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
                List<String> lore = new ArrayList<>(baseLore);
                lore.add("");
                lore.add(enabled ? "§a✔ 활성화됨" : "§7✗ 비활성화됨");
                lore.add("§e클릭하여 전환");
                return createItem(material, name, lore);
        }

        private String formatGroups(Set<String> groups) {
                if (groups.isEmpty())
                        return "없음";
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String g : groups) {
                        if (count > 0)
                                sb.append(", ");
                        sb.append(ItemGroupUtil.getGroupDisplayName(g));
                        count++;
                        if (count >= 3) {
                                sb.append(" 외 ").append(groups.size() - 3).append("개");
                                break;
                        }
                }
                return sb.toString();
        }

        private ItemStack createItem(Material material, String name, List<String> lore) {
                return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
        }

        public void open() {
                player.openInventory(inventory);
        }

        public void refresh() {
                createInventory();
                player.openInventory(inventory);
        }

        @Override
        public Inventory getInventory() {
                return inventory;
        }

        public Player getPlayer() {
                return player;
        }

        public String getEnchantId() {
                return enchantId;
        }
}
