<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="com.mycoffee.model.OrderDetail"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="java.text.NumberFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Map"%>
<%!
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String normalizedStatus(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return "completed";
        }
        if ("Cancelled".equalsIgnoreCase(status)) {
            return "cancelled";
        }
        return "processing";
    }

    private String statusText(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return "HOÀN THÀNH";
        }
        if ("Cancelled".equalsIgnoreCase(status)) {
            return "ĐÃ HỦY";
        }
        return "ĐANG XỬ LÝ";
    }

    private String statusClass(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return "status-done";
        }
        if ("Cancelled".equalsIgnoreCase(status)) {
            return "status-cancelled";
        }
        return "status-processing";
    }

    private String orderSummary(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return "Đơn hàng Modern Cafe";
        }
        String name = details.get(0).getNote();
        if (details.size() > 1) {
            return name + " và " + (details.size() - 1) + " món khác";
        }
        return name;
    }

    private String productImage(int index) {
        String[] images = {
            "https://images.unsplash.com/photo-1517701604599-bb29b565090c?auto=format&fit=crop&w=280&q=80",
            "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=280&q=80",
            "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=280&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=280&q=80",
            "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=280&q=80"
        };
        return images[Math.abs(index) % images.length];
    }
%>
<%
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    if (orders == null) {
        orders = new ArrayList<>();
    }

    Map<Integer, List<OrderDetail>> orderDetailsMap = (Map<Integer, List<OrderDetail>>) request.getAttribute("orderDetailsMap");
    String activeFilter = (String) request.getAttribute("activeFilter");
    if (activeFilter == null) {
        activeFilter = "all";
    }

    User loggedInUser = (User) session.getAttribute("user");
    String avatarText = "TV";
    if (loggedInUser != null && loggedInUser.getFullName() != null && !loggedInUser.getFullName().trim().isEmpty()) {
        String[] parts = loggedInUser.getFullName().trim().split("\\s+");
        avatarText = parts[parts.length - 1].substring(0, 1).toUpperCase();
    }

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'Th'MM, HH:mm", new Locale("vi", "VN"));
    SimpleDateFormat todayFormat = new SimpleDateFormat("'Hôm nay,' HH:mm", new Locale("vi", "VN"));
    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    String todayKey = dayFormat.format(new java.util.Date());
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử mua hàng - Modern Cafe</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Roboto', 'sans-serif']
                    }
                }
            }
        }
    </script>
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #0747A6;
            --primary-bright: #0052CC;
            --ink: #091E42;
            --muted: #5E6C84;
            --line: #DFE6F8;
            --soft: #F4F6FF;
        }

        body {
            margin: 0;
            background: var(--soft);
            color: #172B4D;
        }

        .history-sidebar {
            width: 292px;
            min-width: 292px;
            background: #fff;
            border-right: 1px solid var(--line);
            box-shadow: 18px 0 36px rgba(66, 82, 110, .08);
        }

        .history-brand {
            height: 78px;
            display: flex;
            align-items: center;
            padding: 0 38px;
            color: var(--primary);
            font-size: 22px;
            font-weight: 900;
        }

        .history-nav {
            padding: 40px 24px 24px;
        }

        .history-nav-item {
            position: relative;
            height: 54px;
            display: flex;
            align-items: center;
            gap: 18px;
            padding: 0 18px;
            border-radius: 12px;
            color: #172B4D;
            text-decoration: none;
            font-size: 17px;
            font-weight: 500;
            transition: all .18s ease;
        }

        .history-nav-item:hover,
        .history-nav-item.active {
            background: #DDE8FF;
            color: var(--primary);
            font-weight: 900;
        }

        .history-nav-item.active::after {
            content: "";
            position: absolute;
            right: -12px;
            top: 0;
            width: 4px;
            height: 54px;
            border-radius: 999px;
            background: var(--primary-bright);
        }

        .history-nav-icon {
            width: 20px;
            text-align: center;
            font-size: 18px;
        }

        .rank-card {
            margin: 0 24px 26px;
            padding: 18px;
            border-radius: 16px;
            border: 1px solid #C8D3EA;
            background: #DDE8FF;
        }

        .history-topbar {
            height: 78px;
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 28px;
            padding: 0 42px;
            background: rgba(244, 246, 255, .94);
            border-bottom: 1px solid rgba(223, 230, 248, .78);
            backdrop-filter: blur(18px);
            position: sticky;
            top: 0;
            z-index: 25;
        }

        .filter-pill {
            height: 46px;
            min-width: 116px;
            padding: 0 26px;
            border-radius: 999px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: 1px solid #BAC7E1;
            background: #fff;
            color: #172B4D;
            font-weight: 700;
            text-decoration: none;
            transition: all .18s ease;
        }

        .filter-pill.active,
        .filter-pill:hover {
            background: var(--primary-bright);
            border-color: var(--primary-bright);
            color: #fff;
            box-shadow: 0 10px 22px rgba(0, 82, 204, .2);
        }

        .order-card {
            min-height: 260px;
            border-radius: 20px;
            background: #fff;
            border: 1px solid rgba(223, 230, 248, .82);
            box-shadow: 0 14px 32px rgba(66, 82, 110, .06);
            padding: 28px 30px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        .status-badge {
            height: 28px;
            padding: 0 16px;
            border-radius: 999px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 900;
            white-space: nowrap;
        }

        .status-done {
            background: #D7F8E2;
            color: #008542;
        }

        .status-processing {
            background: #FFF0B8;
            color: #B35400;
        }

        .status-cancelled {
            background: #FFE0E3;
            color: #BF2600;
        }

        .order-thumb {
            width: 72px;
            height: 72px;
            border-radius: 14px;
            object-fit: cover;
            flex: 0 0 auto;
        }

        .detail-btn {
            height: 48px;
            padding: 0 24px;
            border-radius: 14px;
            border: 1.5px solid var(--primary-bright);
            color: var(--primary);
            font-weight: 900;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            text-decoration: none;
            transition: all .18s ease;
            white-space: nowrap;
        }

        .detail-btn:hover {
            background: var(--primary-bright);
            color: #fff;
        }

        @media (max-width: 1024px) {
            .history-topbar {
                height: 70px;
                padding: 0 18px;
            }
        }
    </style>
</head>
<body class="font-sans">
<div class="min-h-screen flex">
    <aside class="history-sidebar flex flex-col min-h-screen">
        <div class="history-brand">Modern Cafe</div>

        <nav class="history-nav flex-1 space-y-3">
            <a href="${pageContext.request.contextPath}/customer-menu" class="history-nav-item">
                <i class="fa-solid fa-utensils history-nav-icon"></i>
                <span>Menu online</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-qr-order" class="history-nav-item">
                <i class="fa-solid fa-qrcode history-nav-icon"></i>
                <span>Đặt món QR</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-order-status" class="history-nav-item">
                <i class="fa-solid fa-truck-fast history-nav-icon"></i>
                <span>Theo dõi order</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-review" class="history-nav-item">
                <i class="fa-regular fa-star history-nav-icon"></i>
                <span>Đánh giá</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-purchase-history" class="history-nav-item active">
                <i class="fa-regular fa-rectangle-list history-nav-icon"></i>
                <span>Lịch sử</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-points" class="history-nav-item">
                <i class="fa-solid fa-tags history-nav-icon"></i>
                <span>Tích điểm</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-my-vouchers" class="history-nav-item">
                <i class="fa-solid fa-ticket history-nav-icon"></i>
                <span>Voucher</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-redeem-voucher" class="history-nav-item">
                <i class="fa-solid fa-gift history-nav-icon"></i>
                <span>Đổi voucher</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-offers" class="history-nav-item">
                <i class="fa-regular fa-calendar-check history-nav-icon"></i>
                <span>Ưu đãi</span>
            </a>
        </nav>

        <div class="rank-card">
            <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-full bg-[#FFDFA8] text-black flex items-center justify-center font-black">V</div>
                <div>
                    <div class="font-bold text-[#172B4D]">Thành viên Vàng</div>
                    <div class="text-sm text-[#172B4D]">2,450 điểm</div>
                </div>
            </div>
            <button type="button" class="mt-4 h-11 w-full rounded-lg bg-[#0052CC] text-white font-black">Nâng cấp hạng</button>
        </div>
    </aside>

    <main class="flex-1 min-w-0">
        <header class="history-topbar">
            <button type="button" class="relative text-xl" aria-label="Thông báo">
                <i class="fa-regular fa-bell"></i>
            </button>
            <a href="${pageContext.request.contextPath}/customer-qr-order" class="text-xl text-[#172B4D]" aria-label="Giỏ hàng">
                <i class="fa-solid fa-cart-shopping"></i>
            </a>
            <div class="w-11 h-11 rounded-full bg-[#0052CC] text-white flex items-center justify-center font-black ring-4 ring-white shadow-md"><%= avatarText %></div>
        </header>

        <section class="px-8 lg:px-12 xl:px-20 py-9">
            <div class="max-w-[1180px]">
                <h1 class="text-[32px] leading-tight font-black text-[#091E42]">Lịch sử mua hàng</h1>
                <p class="mt-3 text-lg text-[#172B4D] font-medium">Xem lại tất cả các đơn hàng bạn đã thực hiện tại Modern Cafe.</p>

                <div class="mt-8 flex flex-wrap gap-4">
                    <a class="filter-pill <%= "all".equals(activeFilter) ? "active" : "" %>" href="${pageContext.request.contextPath}/customer-purchase-history?filter=all">Tất cả</a>
                    <a class="filter-pill <%= "processing".equals(activeFilter) ? "active" : "" %>" href="${pageContext.request.contextPath}/customer-purchase-history?filter=processing">Đang xử lý</a>
                    <a class="filter-pill <%= "completed".equals(activeFilter) ? "active" : "" %>" href="${pageContext.request.contextPath}/customer-purchase-history?filter=completed">Hoàn thành</a>
                    <a class="filter-pill <%= "cancelled".equals(activeFilter) ? "active" : "" %>" href="${pageContext.request.contextPath}/customer-purchase-history?filter=cancelled">Đã hủy</a>
                </div>

                <% if (orders.isEmpty()) { %>
                    <div class="mt-12 max-w-xl rounded-[22px] bg-white border border-[#DFE6F8] p-10 text-center shadow-sm">
                        <div class="w-16 h-16 rounded-full bg-[#E6EEFF] text-[#0052CC] flex items-center justify-center mx-auto text-2xl">
                            <i class="fa-regular fa-rectangle-list"></i>
                        </div>
                        <h2 class="mt-6 text-2xl font-black text-[#091E42]">Chưa có lịch sử mua hàng</h2>
                        <p class="mt-3 text-[#5E6C84] font-medium">Các đơn hàng bạn đã đặt sẽ xuất hiện tại đây.</p>
                        <a href="${pageContext.request.contextPath}/customer-qr-order" class="mt-7 inline-flex items-center justify-center h-12 px-6 rounded-xl bg-[#0052CC] text-white font-black no-underline">Đặt món ngay</a>
                    </div>
                <% } else { %>
                    <div class="mt-12 grid md:grid-cols-2 xl:grid-cols-3 gap-6">
                        <%
                            int visibleCount = 0;
                            int index = 0;
                            for (Order order : orders) {
                                String normalized = normalizedStatus(order.getOrderStatus());
                                if (!"all".equals(activeFilter) && !activeFilter.equals(normalized)) {
                                    index++;
                                    continue;
                                }
                                visibleCount++;
                                List<OrderDetail> details = orderDetailsMap != null ? orderDetailsMap.get(order.getOrderId()) : null;
                                if (details == null) {
                                    details = new ArrayList<>();
                                }
                                double amount = order.getFinalAmount() > 0 ? order.getFinalAmount() : order.getTotalAmount();
                                String orderDateText = "--:--";
                                if (order.getOrderDate() != null) {
                                    String orderDay = dayFormat.format(order.getOrderDate());
                                    orderDateText = todayKey.equals(orderDay) ? todayFormat.format(order.getOrderDate()) : dateFormat.format(order.getOrderDate());
                                }
                        %>
                            <article class="order-card">
                                <div>
                                    <div class="flex items-start justify-between gap-4">
                                        <div>
                                            <div class="text-[#0747A6] text-[14px] font-black tracking-wide">MÃ ĐƠN: #MC-<%= order.getOrderId() %></div>
                                            <div class="mt-2 text-[#172B4D] font-semibold"><%= orderDateText %></div>
                                        </div>
                                        <span class="status-badge <%= statusClass(order.getOrderStatus()) %>"><%= statusText(order.getOrderStatus()) %></span>
                                    </div>

                                    <div class="mt-7 flex items-center gap-5 min-h-[76px]">
                                        <img class="order-thumb" src="<%= productImage(index) %>" alt="Order item">
                                        <div class="min-w-0">
                                            <div class="text-xl leading-snug font-medium text-[#172B4D] line-clamp-2"><%= safe(orderSummary(details)) %></div>
                                            <div class="mt-1 text-[#5E6C84] font-medium"><%= details.size() %> món</div>
                                        </div>
                                    </div>
                                </div>

                                <div class="mt-8 pt-5 border-t border-[#E7ECF8] flex items-end justify-between gap-4">
                                    <div>
                                        <div class="text-[#172B4D] font-medium">Tổng tiền</div>
                                        <div class="mt-1 text-[26px] leading-none font-black text-[#0747A6]"><%= currency.format(amount) %>đ</div>
                                    </div>
                                    <a class="detail-btn" href="${pageContext.request.contextPath}/customer-order-status?orderId=<%= order.getOrderId() %>">Xem chi tiết</a>
                                </div>
                            </article>
                        <%
                                index++;
                            }
                            if (visibleCount == 0) {
                        %>
                            <div class="md:col-span-2 xl:col-span-3 rounded-[22px] bg-white border border-[#DFE6F8] p-10 text-center shadow-sm">
                                <h2 class="text-2xl font-black text-[#091E42]">Không có đơn phù hợp</h2>
                                <p class="mt-3 text-[#5E6C84] font-medium">Thử chọn bộ lọc khác để xem thêm đơn hàng.</p>
                            </div>
                        <% } %>
                    </div>
                <% } %>
            </div>
        </section>
    </main>
</div>
</body>
</html>
