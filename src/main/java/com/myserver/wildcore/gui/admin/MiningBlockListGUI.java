package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.MiningDropData;
import com.myserver.wildcore.gui.PaginatedGui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MiningBlockListGUI extends PaginatedGui<Material> {

    public MiningBlockListGUI(WildCore plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected List<Material> getItems() {
        return new ArrayList<>(plugin.getConfigManager().getMiningDrops().keySet());
    }

    @Override
    protected ItemStack createItemDisplay(Material material) {
        MiningDropData data = plugin.getConfigManager().getMiningDropData(material);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7등록된 보상: &e" + data.getRewards().size() + "개");
        lore.add("");
        lore.add("&e[클릭] &f보상 설정 편집");
        lore.add("&c[우클릭] &f설정 삭제");

        return createItem(material, "&6&l" + material.name(), lore);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        return "&8[&6채굴 드랍 설정&8] &f블록 목록 (" + page + "/" + totalPages + ")";
    }

    @Override
    protected void setupNavigationBar(int page, int totalPages, int totalItems) {
        super.setupNavigationBar(page, totalPages, totalItems);

        // 블록 추가 버튼 (가운데 슬롯 옆)
        inventory.setItem(47, createItem(Material.EMERALD, "&a&l[+ 블록 추가]",
                List.of("", "&7새로운 드랍 블록을 추가합니다.", "&7클릭 후 채팅창에 블록 이름 입력")));
    }
}
