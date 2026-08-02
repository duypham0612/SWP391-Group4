package com.cafe.service.barista;

import com.cafe.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Một trang của hàng chờ quầy pha — thuần phép cắt danh sách, không truy vấn lại DB.
 *
 * <p>Dựng qua {@link KdsService#paginate(List, int, int)} trên danh sách ĐÃ sắp thứ tự pha và ĐÃ
 * đánh số thứ tự, nên số hiện trên mỗi dòng là vị trí thật trong cả hàng chờ chứ không bị đánh lại
 * theo từng trang.
 */
public class QueuePage {

    private final List<OrderItem> items;
    private final int total;
    private final int page;
    private final int pageSize;
    private final int totalPages;
    private final int startRow;

    /** Số trang ngoài phạm vi được kéo về biên: sau mỗi thao tác hàng chờ ngắn đi, trang đang xem có thể không còn. */
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
     * Cắt trang theo KHỐI ĐƠN: các dòng liền nhau cùng một đơn không bao giờ bị tách sang hai
     * trang — pha hết trang 1 mà đơn còn hai ly ở trang 2 là cách chắc chắn nhất để giao thiếu.
     *
     * <p>Trang nhận trọn khối chừng nào chưa đạt {@code pageSize}, nên trang có thể dài hơn
     * mức chuẩn đúng bằng phần dôi của khối cuối (khung hàng chờ vốn đã cuộn được). Trang rỗng
     * luôn nhận khối, kể cả khối lớn hơn cả trang — nếu không vòng lặp không bao giờ tiến.
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

    /** Tối đa 5 số trang quanh trang hiện tại để pager không phình khi hàng chờ dài. */
    public List<Integer> getVisiblePages() {
        List<Integer> pages = new ArrayList<>();
        int start = Math.max(1, page - 2);
        int end = Math.min(totalPages, start + 4);
        start = Math.max(1, end - 4);
        for (int value = start; value <= end; value++) pages.add(value);
        return pages;
    }
}
