package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.managers.EnchantManager.EnchantProcess;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * 인챈트 진행 연출 GUI
 */
public class EnchantProcessGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Inventory inventory;
    private final EnchantConfig enchant;
    private final EnchantProcess process;
    private final Random random = new Random();

    public EnchantProcessGUI(WildCore plugin, Player player, EnchantProcess process) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = process.enchant;
        this.process = process;
        this.inventory = Bukkit.createInventory(this, 27, ItemUtil.parse("§8[ §5강화 진행 중... §8]"));
    }

    public void open() {
        player.openInventory(inventory);
        startAnimation();
    }

    private void startAnimation() {
        // 배경 설정
        fill(Material.BLACK_STAINED_GLASS_PANE);

        // 중앙에 인챈트북 또는 재료 표시
        Material centerMat = Material.getMaterial(enchant.getMaterial());
        if (centerMat == null)
            centerMat = Material.ENCHANTED_BOOK;
        inventory.setItem(13, createItem(centerMat, "§5강화 진행 중...", List.of("§7잠시만 기다려주세요...")));

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof EnchantProcessGUI)) {
                    this.cancel();
                    return;
                }

                if (step >= 30) {
                    this.cancel();
                    finishEnchant();
                    return;
                }

                // 애니메이션 효과: 후반부에는 결과에 따른 색상 강조
                if (step > 20) {
                    updateAtmosphere(step);
                } else {
                    updateGlassPanes();
                }

                // 사운드 효과 (점점 빨라지거나 고조됨)
                float pitch = 1.0f + (step * 0.03f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, pitch);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void updateGlassPanes() {
        Material[] glasses = {
                Material.WHITE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE
        };

        int slot = random.nextInt(27);
        if (slot != 13) {
            inventory.setItem(slot, createItem(glasses[random.nextInt(glasses.length)], " ", null));
        }
    }

    private void updateAtmosphere(int step) {
        Material resultMat;
        switch (process.result) {
            case SUCCESS -> resultMat = Material.LIME_STAINED_GLASS_PANE;
            case FAIL -> resultMat = Material.YELLOW_STAINED_GLASS_PANE;
            case DESTROY -> resultMat = Material.RED_STAINED_GLASS_PANE;
            default -> resultMat = Material.GRAY_STAINED_GLASS_PANE;
        }

        // 테두리부터 채워나가는 효과
        int slotsPerStep = (step - 20) * 2;
        for (int i = 0; i < slotsPerStep; i++) {
            int slot = random.nextInt(27);
            if (slot != 13) {
                inventory.setItem(slot, createItem(resultMat, " ", null));
            }
        }

        if (step % 2 == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
        }
    }

    private void finishEnchant() {
        // 실제 인챈트 결과 적용
        player.closeInventory();

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getEnchantManager().executeEnchant(player, process);
            }
        }.runTaskLater(plugin, 2L);
    }

    private void fill(Material material) {
        ItemStack item = createItem(material, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == 13)
                continue;
            inventory.setItem(i, item);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
