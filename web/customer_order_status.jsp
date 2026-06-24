<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="com.mycoffee.model.OrderDetail"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="java.text.NumberFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Locale"%>
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

    private String displayStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Đang thực hiện";
        }
        if ("Completed".equalsIgnoreCase(status)) {
            return "Hoàn thành";
        }
        if ("Cancelled".equalsIgnoreCase(status)) {
            return "Đã hủy";
        }
        return "Đang thực hiện";
    }

    private int activeStep(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return 4;
        }
        if ("Cancelled".equalsIgnoreCase(status)) {
            return 1;
        }
        return 2;
    }

    private String firstProductName(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return "Đơn hàng Modern Cafe";
        }
        if (details.size() == 1) {
            return details.get(0).getNote();
        }
        return details.get(0).getNote() + " & " + (details.size() - 1) + " món khác";
    }
%>
<%
    Order order = (Order) request.getAttribute("order");
    List<OrderDetail> details = (List<OrderDetail>) request.getAttribute("orderDetails");
    if (details == null) {
        details = new ArrayList<>();
    }

    User loggedInUser = (User) session.getAttribute("user");
    String avatarText = "TV";
    if (loggedInUser != null && loggedInUser.getFullName() != null && !loggedInUser.getFullName().trim().isEmpty()) {
        String[] parts = loggedInUser.getFullName().trim().split("\\s+");
        avatarText = parts[parts.length - 1].substring(0, 1).toUpperCase();
    }

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    String orderTime = (order != null && order.getOrderDate() != null) ? timeFormat.format(order.getOrderDate()) : "--:--";
    int step = activeStep(order != null ? order.getOrderStatus() : null);
    int rewardPoints = order != null ? Math.max(1, (int) Math.round(order.getFinalAmount() / 10000.0)) : 0;
    double finalAmount = order != null ? order.getFinalAmount() : 0;
    if (order != null && finalAmount <= 0) {
        finalAmount = order.getTotalAmount();
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trạng thái đơn hàng - Modern Cafe</title>
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
            --cyan: #39CDEB;
            --ink: #0B1F45;
            --muted: #6B778C;
            --line: #DFE6F8;
            --soft: #F4F6FF;
        }

        body {
            margin: 0;
            background: var(--soft);
            color: #172B4D;
        }

        .status-topbar {
            height: 78px;
            display: grid;
            grid-template-columns: 1fr minmax(260px, 420px) 1fr;
            align-items: center;
            gap: 22px;
            padding: 0 40px;
            background: rgba(244, 246, 255, .94);
            border-bottom: 1px solid rgba(223, 230, 248, .78);
            backdrop-filter: blur(18px);
            position: sticky;
            top: 0;
            z-index: 25;
        }

        .search-pill {
            height: 46px;
            border-radius: 999px;
            background: #EAF0FF;
            color: #5E6C84;
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 0 18px;
            font-weight: 600;
        }

        .order-hero {
            border-radius: 32px;
            background:
                radial-gradient(circle at 94% 12%, rgba(216, 226, 248, .9), rgba(255,255,255,0) 24%),
                #fff;
            box-shadow: 0 24px 50px rgba(66, 82, 110, .13);
            border: 1px solid rgba(223, 230, 248, .72);
        }

        .order-photo {
            width: 250px;
            height: 190px;
            border-radius: 28px;
            object-fit: cover;
            transform: rotate(2deg);
            box-shadow: 0 24px 48px rgba(9, 30, 66, .18);
        }

        .step-line {
            position: absolute;
            left: 64px;
            right: 64px;
            top: 28px;
            height: 6px;
            border-radius: 99px;
            background: #DDE8FF;
        }

        .step-fill {
            position: absolute;
            left: 64px;
            top: 28px;
            height: 6px;
            border-radius: 99px;
            background: var(--primary-bright);
            width: <%= step >= 4 ? "calc(100% - 128px)" : step >= 3 ? "66%" : step >= 2 ? "36%" : "0" %>;
        }

        .step-dot {
            width: 58px;
            height: 58px;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            position: relative;
            z-index: 2;
            background: #DDE8FF;
            color: #A5ADBA;
        }

        .step-dot.done,
        .step-dot.active {
            color: #fff;
            background: var(--primary-bright);
            box-shadow: 0 10px 22px rgba(0, 82, 204, .24);
        }

        .step-dot.active {
            outline: 6px solid #DDE8FF;
        }

        .info-card {
            background: #fff;
            border-radius: 24px;
            box-shadow: 0 18px 42px rgba(66, 82, 110, .08);
        }

        .reward-card {
            border-radius: 24px;
            background: linear-gradient(160deg, #0D61D6 0%, #0052CC 100%);
            color: #fff;
            box-shadow: 0 18px 34px rgba(0, 82, 204, .22);
        }

        .qty-badge {
            width: 48px;
            height: 48px;
            border-radius: 14px;
            background: #E6EEFF;
            color: var(--primary);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
        }

        @media (max-width: 1024px) {
            .status-topbar {
                grid-template-columns: 1fr;
                height: auto;
                padding: 16px;
            }

            .order-photo {
                width: 100%;
                height: 220px;
                transform: none;
            }
        }
    </style>
</head>
<body class="font-sans">
<div class="min-h-screen flex">
    <jsp:include page="common/sidebar_customer.jsp"/>

    <main class="flex-1 min-w-0">
        <header class="status-topbar">
            <div></div>
            <div class="search-pill">
                <i class="fa-solid fa-magnifying-glass text-lg"></i>
                <span>Tìm kiếm món ăn...</span>
            </div>
            <div class="flex items-center justify-end gap-6 text-[#172B4D]">
                <button type="button" class="relative text-xl" aria-label="Thông báo">
                    <i class="fa-regular fa-bell"></i>
                    <span class="absolute -right-1 -top-1 w-2.5 h-2.5 rounded-full bg-red-600"></span>
                </button>
                <a href="${pageContext.request.contextPath}/customer-qr-order" class="text-xl" aria-label="Giỏ hàng">
                    <i class="fa-solid fa-cart-shopping"></i>
                </a>
                <div class="w-11 h-11 rounded-full bg-[#0052CC] text-white flex items-center justify-center font-black ring-4 ring-white shadow-md"><%= avatarText %></div>
            </div>
        </header>

        <section class="px-8 lg:px-24 py-10">
            <% if (order == null) { %>
                <div class="max-w-3xl mx-auto info-card p-10 text-center">
                    <div class="w-16 h-16 rounded-full bg-[#E6EEFF] text-[#0052CC] flex items-center justify-center mx-auto text-2xl">
                        <i class="fa-regular fa-clipboard"></i>
                    </div>
                    <h1 class="mt-6 text-3xl font-black text-[#0747A6]">Chưa có đơn hàng để theo dõi</h1>
                    <p class="mt-3 text-[#5E6C84] font-medium">Bạn hãy chọn món và gửi order, trạng thái sẽ hiển thị tại đây ngay sau đó.</p>
                    <a href="${pageContext.request.contextPath}/customer-qr-order" class="mt-7 inline-flex items-center gap-2 px-6 h-12 rounded-xl bg-[#0052CC] text-white font-black no-underline">
                        <span>Đặt món ngay</span>
                        <i class="fa-solid fa-arrow-right"></i>
                    </a>
                </div>
            <% } else { %>
                <div class="max-w-[1120px] mx-auto">
                    <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-5">
                        <div>
                            <h1 class="text-[42px] lg:text-[58px] leading-none font-black text-[#0747A6] tracking-normal">Trạng thái đơn hàng</h1>
                            <p class="mt-3 text-lg text-[#172B4D] font-medium">Mã đơn: <span class="font-black">#MC-<%= order.getOrderId() %></span> • Đặt lúc <%= orderTime %></p>
                        </div>
                        <div class="inline-flex items-center gap-3 rounded-full bg-[#DDE8FF] text-[#0747A6] px-6 h-12 font-black whitespace-nowrap">
                            <i class="fa-regular fa-clock"></i>
                            <span>Dự kiến: 12 phút</span>
                        </div>
                    </div>

                    <article class="order-hero mt-10 p-8 lg:p-12">
                        <div class="grid lg:grid-cols-[280px_1fr] gap-8 items-center">
                            <img class="order-photo" src="https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=720&q=80" alt="Coffee and croissant">
                            <div>
                                <div class="inline-flex items-center rounded-full bg-[#40D2EE] text-[#07475C] px-4 h-8 text-[12px] font-black tracking-[.16em] uppercase">
                                    <%= displayStatus(order.getOrderStatus()) %>
                                </div>
                                <h2 class="mt-5 text-3xl font-black text-[#0747A6]"><%= safe(firstProductName(details)) %></h2>
                                <p class="mt-4 text-lg text-[#172B4D] font-medium">Ghi chú: Ít đường, nhiều đá. Hâm nóng bánh.</p>
                                <div class="mt-6 flex flex-wrap items-center gap-6 text-[#172B4D] font-medium">
                                    <span class="inline-flex items-center gap-2"><i class="fa-regular fa-user text-sm"></i> Phục vụ bởi: Tuấn Anh</span>
                                    <span class="inline-flex items-center gap-2"><i class="fa-solid fa-location-dot text-sm"></i> Bàn số: <%= order.getOrderType() != null ? safe(order.getOrderType()) : order.getTableId() %></span>
                                </div>
                            </div>
                        </div>

                        <div class="relative mt-20 max-w-[820px] mx-auto">
                            <div class="step-line"></div>
                            <div class="step-fill"></div>
                            <div class="grid grid-cols-4 text-center">
                                <div>
                                    <div class="step-dot <%= step >= 1 ? "done" : "" %>"><i class="fa-regular fa-circle-check"></i></div>
                                    <div class="mt-5 text-[#0747A6] font-black">Đã nhận đơn</div>
                                    <div class="text-xs text-[#6B778C] mt-1"><%= orderTime %></div>
                                </div>
                                <div>
                                    <div class="step-dot <%= step > 2 ? "done" : step == 2 ? "active" : "" %>"><i class="fa-solid fa-blender"></i></div>
                                    <div class="mt-5 <%= step >= 2 ? "text-[#0747A6]" : "text-[#6B778C]" %> font-black">Đang pha chế</div>
                                    <div class="text-xs <%= step == 2 ? "text-[#0747A6]" : "text-[#A5ADBA]" %> mt-1"><%= step == 2 ? "Đang xử lý..." : "Chờ đợi" %></div>
                                </div>
                                <div>
                                    <div class="step-dot <%= step > 3 ? "done" : step == 3 ? "active" : "" %>"><i class="fa-solid fa-water"></i></div>
                                    <div class="mt-5 <%= step >= 3 ? "text-[#0747A6]" : "text-[#6B778C]" %> font-black">Đang phục vụ</div>
                                    <div class="text-xs text-[#A5ADBA] mt-1">Chờ đợi</div>
                                </div>
                                <div>
                                    <div class="step-dot <%= step == 4 ? "active" : "" %>"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
                                    <div class="mt-5 <%= step >= 4 ? "text-[#0747A6]" : "text-[#6B778C]" %> font-black">Hoàn thành</div>
                                    <div class="text-xs text-[#A5ADBA] mt-1">Chờ đợi</div>
                                </div>
                            </div>
                        </div>
                    </article>

                    <div class="mt-10 grid lg:grid-cols-[1fr_320px] gap-8">
                        <section class="info-card p-8">
                            <div class="flex items-center justify-between gap-4">
                                <h2 class="text-2xl font-black text-[#091E42]">Chi tiết đơn hàng</h2>
                                <a href="${pageContext.request.contextPath}/customer-qr-order?tableId=<%= order.getTableId() %>" class="inline-flex items-center gap-2 text-[#0747A6] font-black no-underline">
                                    <i class="fa-solid fa-pen text-sm"></i>
                                    <span>Thay đổi</span>
                                </a>
                            </div>

                            <div class="mt-6 divide-y divide-[#E7ECF8]">
                                <% for (OrderDetail item : details) { %>
                                    <div class="py-5 grid grid-cols-[58px_1fr_auto] gap-4 items-center">
                                        <div class="qty-badge"><%= item.getQuantity() %>x</div>
                                        <div>
                                            <div class="font-black text-[#091E42] text-lg"><%= safe(item.getNote()) %></div>
                                            <div class="text-sm text-[#6B778C] mt-1"><%= item.getItemStatus() != null ? safe(item.getItemStatus()) : "Đang chuẩn bị" %></div>
                                        </div>
                                        <div class="font-black text-[#091E42] text-lg"><%= currency.format(item.getQuantity() * item.getUnitPrice()) %>đ</div>
                                    </div>
                                <% } %>
                            </div>

                            <div class="pt-6 flex items-center justify-between text-xl">
                                <span class="font-medium text-[#172B4D]">Tổng thanh toán</span>
                                <span class="font-black text-[#0747A6] text-2xl"><%= currency.format(finalAmount) %>đ</span>
                            </div>
                        </section>

                        <aside class="reward-card p-8 flex flex-col justify-between min-h-[280px]">
                            <div>
                                <div class="text-4xl"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
                                <h2 class="mt-7 text-2xl font-black">Đừng quên!</h2>
                                <p class="mt-4 text-lg leading-relaxed text-white/88 font-medium">Bạn sẽ nhận được <span class="font-black text-white"><%= rewardPoints %> điểm</span> cho đơn hàng này sau khi hoàn thành.</p>
                            </div>
                            <a href="${pageContext.request.contextPath}/customer-points" class="mt-8 inline-flex items-center gap-3 text-white font-black text-lg no-underline">
                                <span>Xem tích lũy</span>
                                <i class="fa-solid fa-arrow-right"></i>
                            </a>
                        </aside>
                    </div>

                    <div class="mt-9 rounded-2xl bg-[#FFE2B0] text-[#172B4D] px-6 py-5 flex items-center gap-4 font-medium">
                        <i class="fa-regular fa-lightbulb text-xl"></i>
                        <span><span class="font-black">Tip:</span> Bạn có thể nhấn vào "Đặt thêm" nếu muốn dùng thêm các món khác mà không cần đợi đơn cũ hoàn thành.</span>
                    </div>
                </div>
            <% } %>
        </section>
    </main>
</div>
</body>
</html>
