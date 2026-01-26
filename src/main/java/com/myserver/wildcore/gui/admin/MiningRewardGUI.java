package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.MiningDropData;
import com.myserver.wildcore.config.MiningReward;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MiningRewardGUI extends PaginatedGui<MiningReward> {

    private final Material targetBlock;

    public MiningRewardGUI(WildCore plugin, Player player, Material targetBlock) {
        super(plugin, player);
        this.targetBlock = targetBlock;
    }

    @Override
    protected List<MiningReward> getItems() {
        MiningDropData data = plugin.getConfigManager().getMiningDropData(targetBlock);
        if (data == null) {
            return new ArrayList<>();
        }
        return data.getRewards();
    }

    @Override
    protected ItemStack createItemDisplay(MiningReward reward) {
        ItemStack item = ItemUtil.createCustomItem(plugin, reward.getItemId(), 1);
        if (item == null) {
            item = new ItemStack(Material.BARRIER);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7확률: &e" + reward.getChance() + "%");
        lore.add("&7최소 수량: &e" + reward.getMinAmount() + "개");
        lore.add("&7최대 수량: &e" + reward.getMaxAmount() + "개");
        lore.add("");
        lore.add("&e[좌클릭] &f확률 수정");
        lore.add("&e[Shift+클릭] &f수량 수정");
        lore.add("&c[우클릭] &f보상 삭제");

        return createItem(item.getType(), "&6" + reward.getItemId(), lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        return "&8[" + targetBlock.name() + "] &f보상 목록";
    }

    @Override
    protected void setupNavigationBar(int page, int totalPages, int totalItems) {
        super.setupNavigationBar(page, totalPages, totalItems);

        // 뒤로가기
        inventory.setItem(48, createItem(Material.OAK_DOOR, "&c[뒤로가기]", List.of("", "&7블록 목록으로 돌아갑니다.")));

        // 보상 추가
        inventory.setItem(50, createItem(Material.DIAMOND, "&a&l[+ 보상 추가]",
                List.of("", "&7새로운 보상 아이템을 추가합니다.")));
    }

    public Material getTargetBlock() {
        return targetBlock;
    }
}
