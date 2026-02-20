package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 페이지네이션을 지원하는 GUI 추상 클래스
 * 모든 페이지네이션 GUI의 기본이 되는 클래스입니다.
 *
 * @param <T> GUI에 표시할 아이템 타입
 */
public abstract class PaginatedGui<T> implements InventoryHolder {

    public static final int ITEMS_PER_PAGE = 45; // 0~44 슬롯
    public static final int GUI_SIZE = 54; // 6줄 인벤토리

    // 네비게이션 슬롯
    public static final int SLOT_PREV_PAGE = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT_PAGE = 53;

    protected final WildCore plugin;
    protected final Player player;
    protected Inventory inventory;
    protected int currentPage = 0;

    public PaginatedGui(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * 표시할 아이템 리스트를 반환합니다.
     * 하위 클래스에서 구현해야 합니다.
     */
    protected abstract List<T> getItems();

    /**
     * 개별 아이템을 ItemStack으로 변환합니다.
     * 하위 클래스에서 구현해야 합니다.
     */
    protected abstract ItemStack createItemDisplay(T item);

    /**
     * GUI 제목을 반환합니다. 페이지 번호를 포함할 수 있습니다.
     */
    protected abstract String getTitle(int page, int totalPages);

    /**
     * 네비게이션 바 배경 유리판 색상을 반환합니다.
     */
    protected Material getBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    /**
     * 메인 영역(0~44) 배경 유리판 색상을 반환합니다.
     */
    protected Material getMainBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    /**
     * 인벤토리를 생성하고 아이템을 배치합니다.
     */
    protected void createInventory(int page) {
        List<T> items = getItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));

        // 페이지 범위 검증
        if (page < 0)
            page = 0;
        if (page >= totalPages)
            page = totalPages - 1;
        this.currentPage = page;

        String title = getTitle(page + 1, totalPages);
        inventory = Bukkit.createInventory(this, GUI_SIZE, ItemUtil.parse(title));

        // 배경 채우기
        ItemStack background = createItem(getMainBackgroundMaterial(), " ", null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, background);
        }

        // 아이템 배치 (0~44 슬롯)
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            T item = items.get(i);
            int slot = i - startIndex; // 0~44 슬롯에 순서대로 배치
            ItemStack display = createItemDisplay(item);
            display = createFinalItemDisplay(item, display);
            inventory.setItem(slot, display);
        }

        // 네비게이션 바 (45~53 슬롯)
        setupNavigationBar(page, totalPages, items.size());
    }

    /**
     * 네비게이션 바를 설정합니다.
     */
    protected void setupNavigationBar(int page, int totalPages, int totalItems) {
        // 네비게이션 바 배경
        ItemStack navBackground = createItem(getBackgroundMaterial(), " ", null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, navBackground);
        }

        // 이전 페이지 버튼
        if (page > 0) {
            inventory.setItem(SLOT_PREV_PAGE, createItem(Material.ARROW, "§a[ ◀ 이전 페이지 ]",
                    List.of("", "§7클릭하여 이전 페이지로 이동", "", "§e현재: " + (page + 1) + "/" + totalPages + " 페이지")));
        } else {
            inventory.setItem(SLOT_PREV_PAGE, createItem(Material.BARRIER, "§7[ 첫 페이지 ]",
                    List.of("", "§7이전 페이지가 없습니다.")));
        }

        // 정보 아이콘
        inventory.setItem(SLOT_INFO, createInfoItem(page, totalPages, totalItems));

        // 다음 페이지 버튼
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT_PAGE, createItem(Material.ARROW, "§a[ 다음 페이지 ▶ ]",
                    List.of("", "§7클릭하여 다음 페이지로 이동", "", "§e현재: " + (page + 1) + "/" + totalPages + " 페이지")));
        } else {
            inventory.setItem(SLOT_NEXT_PAGE, createItem(Material.BARRIER, "§7[ 마지막 페이지 ]",
                    List.of("", "§7다음 페이지가 없습니다.")));
        }
    }

    /**
     * 정보 아이콘을 생성합니다.
     * 하위 클래스에서 오버라이드하여 커스터마이즈 가능합니다.
     */
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
        return createItem(Material.BOOK, "§f[ 페이지 정보 ]",
                List.of("",
                        "§7현재 페이지: §e" + (page + 1) + " / " + totalPages,
                        "§7총 아이템: §e" + totalItems + "개"));
    }

    /**
     * 아이템 생성 헬퍼 메서드
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
    }

    /**
     * createItemDisplay 후 추가 가공을 위한 훅 메소드.
     * 하위 클래스에서 오버라이드하여 아이템별 메타데이터(예: PotionMeta)를 적용할 수 있습니다.
     */
    protected ItemStack createFinalItemDisplay(T item, ItemStack baseDisplay) {
        return baseDisplay;
    }

    /**
     * GUI를 열고 첫 페이지를 표시합니다.
     */
    public void open() {
        open(0);
    }

    /**
     * GUI를 열고 지정된 페이지를 표시합니다.
     */
    public void open(int page) {
        createInventory(page);
        player.openInventory(inventory);
    }

    /**
     * 현재 페이지를 새로고침합니다.
     */
    public void refresh() {
        open(currentPage);
    }

    /**
     * 다음 페이지로 이동합니다.
     */
    public void nextPage() {
        open(currentPage + 1);
    }

    /**
     * 이전 페이지로 이동합니다.
     */
    public void previousPage() {
        open(currentPage - 1);
    }

    /**
     * 현재 페이지 번호를 반환합니다.
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 전체 페이지 수를 반환합니다.
     */
    public int getTotalPages() {
        List<T> items = getItems();
        return Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
    }

    /**
     * 클릭된 슬롯에 해당하는 아이템을 반환합니다.
     * 슬롯이 아이템 영역(0~44)이 아니거나 해당 아이템이 없으면 null을 반환합니다.
     */
    public T getItemAtSlot(int slot) {
        if (slot < 0 || slot >= ITEMS_PER_PAGE) {
            return null;
        }

        List<T> items = getItems();
        int index = currentPage * ITEMS_PER_PAGE + slot;

        if (index >= items.size()) {
            return null;
        }

        return items.get(index);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public WildCore getPlugin() {
        return plugin;
    }
}
