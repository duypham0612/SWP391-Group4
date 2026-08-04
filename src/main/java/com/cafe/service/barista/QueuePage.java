package com.cafe.service.barista;

import com.cafe.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of the barista counter queue — a pure list slice, no re-querying the DB.
 *
 * <p>Built via {@link KdsService#paginate(List, int, int)} over a list that is ALREADY ordered
 * for making and ALREADY numbered, so the number shown on each row is its real position in the
 * whole queue rather than being renumbered per page.
 */
public class QueuePage {

    private final List<OrderItem> items;
    private final int total;
    private final int page;
    private final int pageSize;
    private final int totalPages;
    private final int startRow;

    /** A page number outside range is clamped to the boundary: after any action shortens the queue, the page being viewed may no longer exist. */
    QueuePage(List<OrderItem> all, int page, int pageSize) {
        this.total = all.size();
        this.pageSize = Math.max(1, pageSize);
        List<List<OrderItem>> pages = splitPages(all, this.pageSize);
        this.totalPages = Math.max(1, pages.size());
        this.page = Math.min(Math.max(1, page), this.totalPages);
        int before = 0;
        for (int i = 0; i < this.page - 1 && i < pages.size(); i++) before += pages.get(i).size();
        this.startRow = total == 0 ? 0 : before + 1;
        this.items = pages.isEmpty() ? new ArrayList<>() : pages.get(this.page - 1);
    }

    /**
     * Splits into pages by ORDER BLOCK: consecutive rows belonging to the same order are never
     * split across two pages — finishing page 1 while the order still has two cups on page 2 is
     * the surest way to under-deliver an order.
     *
     * <p>A page keeps taking whole blocks as long as it hasn't reached {@code pageSize}, so a
     * page can end up longer than the standard size by exactly the overflow of its last block
     * (the queue panel already scrolls anyway). An empty page always takes a block, even one
     * bigger than the page itself — otherwise the loop would never advance.
     */
    private static List<List<OrderItem>> splitPages(List<OrderItem> all, int pageSize) {
        List<List<OrderItem>> pages = new ArrayList<>();
        List<OrderItem> current = new ArrayList<>();
        int i = 0;
        while (i < all.size()) {
            int end = i + 1;
            while (end < all.size() && all.get(end).getOrderId() == all.get(i).getOrderId()) end++;
            if (current.size() >= pageSize) {
                pages.add(current);
                current = new ArrayList<>();
            }
            current.addAll(all.subList(i, end));
            i = end;
        }
        if (!current.isEmpty()) pages.add(current);
        return pages;
    }

    public List<OrderItem> getItems() { return items; }
    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < totalPages; }
    public int getStartRow() { return startRow; }
    public int getEndRow() { return total == 0 ? 0 : startRow + items.size() - 1; }

    /** At most 5 page numbers around the current page so the pager doesn't balloon when the queue is long. */
    public List<Integer> getVisiblePages() {
        List<Integer> pages = new ArrayList<>();
        int start = Math.max(1, page - 2);
        int end = Math.min(totalPages, start + 4);
        start = Math.max(1, end - 4);
        for (int value = start; value <= end; value++) pages.add(value);
        return pages;
    }
}
