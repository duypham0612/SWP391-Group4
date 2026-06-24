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

    private String productImage(int index) {
        String[] images = {
            "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?auto=format&fit=crop&w=260&q=80",
            "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=260&q=80",
            "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=260&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=260&q=80"
        };
        return images[Math.abs(index) % images.length];
    }
%>
<%
    String contextPath = request.getContextPath();
    Order order = (Order) request.getAttribute("order");
    List<OrderDetail> details = (List<OrderDetail>) request.getAttribute("orderDetails");
    if (details == null) {
        details = new ArrayList<>();
    }

    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null)
            ? loggedInUser.getFullName() : "Khách hàng";
    String avatarText = fullName.trim().isEmpty() ? "K" : fullName.trim().substring(0, 1).toUpperCase();

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    SimpleDateFormat timeFormat = new SimpleDateFormat("'Hôm nay,' HH:mm", new Locale("vi", "VN"));
    String orderTime = (order != null && order.getOrderDate() != null) ? timeFormat.format(order.getOrderDate()) : "--:--";
    double totalAmount = order != null && order.getFinalAmount() > 0 ? order.getFinalAmount() : (order != null ? order.getTotalAmount() : 0);

    String reviewMessage = (String) session.getAttribute("reviewMessage");
    String reviewError = (String) session.getAttribute("reviewError");
    session.removeAttribute("reviewMessage");
    session.removeAttribute("reviewError");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đánh giá đơn hàng - Modern Cafe</title>
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
            --primary: #0052CC;
            --ink: #091E42;
            --muted: #5E6C84;
            --soft: #F4F6FF;
            --line: #DFE6F8;
        }

        body {
            margin: 0;
            background: var(--soft);
            color: #172B4D;
        }

        .review-topbar {
            height: 78px;
            display: grid;
            grid-template-columns: 1fr minmax(280px, 420px) 1fr;
            align-items: center;
            gap: 24px;
            padding: 0 42px;
            background: rgba(244, 246, 255, .94);
            border-bottom: 1px solid rgba(223, 230, 248, .78);
            backdrop-filter: blur(18px);
            position: sticky;
            top: 0;
            z-index: 25;
        }

        .search-pill {
            height: 48px;
            border-radius: 999px;
            background: #EAF0FF;
            color: #5E6C84;
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 0 20px;
            font-weight: 600;
        }

        .panel {
            border-radius: 18px;
            background: #fff;
            border: 1px solid rgba(223, 230, 248, .82);
            box-shadow: 0 14px 32px rgba(66, 82, 110, .06);
        }

        .item-image {
            width: 88px;
            height: 88px;
            border-radius: 12px;
            object-fit: cover;
        }

        .rating {
            display: inline-flex;
            flex-direction: row-reverse;
            gap: 16px;
        }

        .rating input {
            display: none;
        }

        .rating label {
            cursor: pointer;
            color: #BAC7E1;
            font-size: 24px;
            transition: color .15s ease, transform .15s ease;
        }

        .rating label:hover,
        .rating label:hover ~ label,
        .rating input:checked ~ label {
            color: #FFAB00;
            transform: translateY(-1px);
        }

        .review-textarea {
            width: 100%;
            min-height: 92px;
            border: 0;
            outline: 0;
            resize: vertical;
            border-radius: 10px;
            background: #EAF0FF;
            padding: 18px 20px;
            color: #172B4D;
            font-weight: 500;
        }

        .service-option {
            height: 74px;
            border-radius: 10px;
            border: 1px solid #BAC7E1;
            background: #fff;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 8px;
            font-size: 12px;
            font-weight: 800;
            cursor: pointer;
            transition: all .16s ease;
        }

        .service-option input {
            display: none;
        }

        .service-option:has(input:checked) {
            border-color: var(--primary);
            color: var(--primary);
            background: #EAF0FF;
        }

        .submit-btn {
            width: 216px;
            height: 54px;
            border-radius: 12px;
            border: 0;
            background: var(--primary);
            color: #fff;
            font-weight: 900;
            box-shadow: 0 14px 28px rgba(0, 82, 204, .25);
        }

        @media (max-width: 1024px) {
            .review-topbar {
                grid-template-columns: 1fr;
                height: auto;
                padding: 16px;
            }
        }
    </style>
</head>
<body class="h-screen flex font-sans overflow-hidden">
    <jsp:include page="common/sidebar_customer.jsp"/>

    <div class="flex-1 h-screen overflow-hidden">
        <header class="review-topbar">
            <div></div>
            <div class="search-pill">
                <i class="fa-solid fa-magnifying-glass"></i>
                <span>Tìm kiếm món ăn...</span>
            </div>
            <div class="flex items-center justify-end gap-6 text-[#172B4D]">
                <button type="button" class="text-xl" aria-label="Thông báo"><i class="fa-regular fa-bell"></i></button>
                <a href="<%= contextPath %>/customer-qr-order" class="relative text-xl text-[#172B4D]" aria-label="Giỏ hàng">
                    <i class="fa-solid fa-cart-shopping"></i>
                    <span class="absolute -right-2 -top-2 w-5 h-5 rounded-full bg-[#40D2EE] text-[#0747A6] text-[10px] flex items-center justify-center font-black">3</span>
                </a>
                <div class="w-10 h-10 rounded-full bg-[#0052CC] text-white flex items-center justify-center font-black ring-4 ring-white shadow-md"><%= safe(avatarText) %></div>
            </div>
        </header>

        <main class="h-[calc(100vh-78px)] overflow-y-auto px-8 lg:px-20 py-9">
            <div class="max-w-[820px] mx-auto">
                <h1 class="text-[30px] leading-tight font-black text-[#091E42]">Đánh giá đơn hàng</h1>
                <p class="mt-3 text-[#172B4D] font-medium">Cảm ơn bạn đã sử dụng dịch vụ tại <span class="text-[#0052CC] font-black">Modern Cafe</span>. Hãy chia sẻ trải nghiệm của bạn nhé!</p>

                <% if (reviewMessage != null) { %>
                    <div class="mt-6 rounded-xl bg-[#E3FCEF] border border-[#ABF5D1] px-5 py-4 text-[#006644] font-black"><%= safe(reviewMessage) %></div>
                <% } %>
                <% if (reviewError != null) { %>
                    <div class="mt-6 rounded-xl bg-[#FFEBE6] border border-[#FFBDAD] px-5 py-4 text-[#DE350B] font-black"><%= safe(reviewError) %></div>
                <% } %>

                <% if (order == null) { %>
                    <div class="panel mt-8 p-10 text-center">
                        <div class="w-16 h-16 rounded-full bg-[#EAF0FF] text-[#0052CC] flex items-center justify-center mx-auto text-2xl">
                            <i class="fa-regular fa-star"></i>
                        </div>
                        <h2 class="mt-6 text-2xl font-black text-[#091E42]">Chưa có đơn hàng để đánh giá</h2>
                        <p class="mt-3 text-[#5E6C84] font-medium">Sau khi hoàn tất đơn hàng, bạn có thể quay lại đây để gửi đánh giá.</p>
                        <a href="<%= contextPath %>/customer-purchase-history" class="mt-7 inline-flex items-center justify-center h-12 px-6 rounded-xl bg-[#0052CC] text-white font-black no-underline">Xem lịch sử mua hàng</a>
                    </div>
                <% } else { %>
                    <form action="<%= contextPath %>/customer-review" method="post" class="mt-8">
                        <input type="hidden" name="orderId" value="<%= order.getOrderId() %>">
                        <input type="hidden" id="serviceTags" name="serviceTags" value="">

                        <div class="panel p-6 flex items-center justify-between gap-6">
                            <div class="flex items-center gap-4">
                                <div class="w-12 h-12 rounded-full bg-[#EAF0FF] text-[#0052CC] flex items-center justify-center text-xl">
                                    <i class="fa-regular fa-rectangle-list"></i>
                                </div>
                                <div>
                                    <div class="font-black text-[#091E42] text-lg">Mã đơn: #MC-<%= order.getOrderId() %></div>
                                    <div class="text-sm font-medium text-[#172B4D]"><%= orderTime %> • <%= details.size() %> món • <%= currency.format(totalAmount) %>đ</div>
                                </div>
                            </div>
                            <div class="hidden md:flex -space-x-3">
                                <% for (int i = 0; i < Math.min(3, details.size()); i++) { %>
                                    <img src="<%= productImage(i) %>" class="w-10 h-10 rounded-full object-cover border-2 border-white" alt="Món đã đặt">
                                <% } %>
                                <% if (details.size() > 3) { %>
                                    <div class="w-10 h-10 rounded-full bg-[#EAF0FF] border-2 border-white flex items-center justify-center text-xs font-black text-[#0052CC]">+<%= details.size() - 3 %></div>
                                <% } %>
                            </div>
                        </div>

                        <div class="mt-7 space-y-6">
                            <% int index = 0; for (OrderDetail item : details) { %>
                                <section class="panel p-6">
                                    <div class="grid grid-cols-[92px_1fr_auto] gap-5 items-start">
                                        <img class="item-image" src="<%= productImage(index) %>" alt="<%= safe(item.getNote()) %>">
                                        <div>
                                            <h2 class="text-xl font-black text-[#091E42]"><%= safe(item.getNote()) %></h2>
                                            <p class="mt-2 text-sm italic font-semibold text-[#172B4D]">Hãy cho quán biết cảm nhận của bạn về món này</p>
                                            <div class="rating mt-5">
                                                <% for (int star = 5; star >= 1; star--) { %>
                                                    <input id="item-<%= item.getOrderDetailId() %>-<%= star %>" type="radio" name="itemRating_<%= item.getOrderDetailId() %>" value="<%= star %>">
                                                    <label for="item-<%= item.getOrderDetailId() %>-<%= star %>"><i class="fa-regular fa-star"></i></label>
                                                <% } %>
                                            </div>
                                        </div>
                                        <div class="text-[#0052CC] font-black"><%= currency.format(item.getQuantity() * item.getUnitPrice()) %>đ</div>
                                    </div>
                                    <textarea class="review-textarea mt-7" name="itemComment_<%= item.getOrderDetailId() %>" placeholder="Chia sẻ thêm về hương vị món này..."></textarea>
                                </section>
                            <% index++; } %>
                        </div>

                        <section class="panel mt-8 p-6 border-[#BAC7E1]">
                            <h2 class="text-xl font-black text-[#091E42] flex items-center gap-2">
                                <i class="fa-regular fa-face-smile text-[#0052CC]"></i>
                                <span>Đánh giá dịch vụ & không gian</span>
                            </h2>

                            <div class="rating mt-5">
                                <% for (int star = 5; star >= 1; star--) { %>
                                    <input id="service-<%= star %>" type="radio" name="serviceRating" value="<%= star %>">
                                    <label for="service-<%= star %>"><i class="fa-regular fa-star"></i></label>
                                <% } %>
                            </div>

                            <div class="mt-5 grid grid-cols-2 lg:grid-cols-4 gap-4">
                                <label class="service-option">
                                    <input type="checkbox" value="Giao hàng nhanh" onchange="syncServiceTags()">
                                    <i class="fa-solid fa-gauge-high text-lg"></i>
                                    <span>Giao hàng nhanh</span>
                                </label>
                                <label class="service-option">
                                    <input type="checkbox" value="Bao bì thân thiện" onchange="syncServiceTags()">
                                    <i class="fa-solid fa-leaf text-lg"></i>
                                    <span>Bao bì thân thiện</span>
                                </label>
                                <label class="service-option">
                                    <input type="checkbox" value="Nhân viên nhiệt tình" onchange="syncServiceTags()">
                                    <i class="fa-regular fa-face-smile text-lg"></i>
                                    <span>Nhân viên nhiệt tình</span>
                                </label>
                                <label class="service-option">
                                    <input type="checkbox" value="CSKH chu đáo" onchange="syncServiceTags()">
                                    <i class="fa-solid fa-headset text-lg"></i>
                                    <span>CSKH chu đáo</span>
                                </label>
                            </div>

                            <textarea class="review-textarea mt-5" name="serviceComment" placeholder="Góp ý thêm cho cửa hàng để chúng mình hoàn thiện hơn nhé..."></textarea>
                        </section>

                        <div class="mt-9 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
                            <div class="flex items-center gap-4">
                                <div class="w-12 h-12 rounded-full bg-[#FFDFA8] flex items-center justify-center text-[#8B5E00]">
                                    <i class="fa-solid fa-tags"></i>
                                </div>
                                <div class="font-medium text-[#091E42]">Gửi đánh giá để nhận ngay <span class="font-black">+50 điểm</span> tích lũy</div>
                            </div>
                            <button class="submit-btn" type="submit">
                                Gửi đánh giá
                                <i class="fa-regular fa-paper-plane ml-2"></i>
                            </button>
                        </div>
                    </form>
                <% } %>
            </div>
        </main>
    </div>

<script>
    function syncServiceTags() {
        const tags = Array.from(document.querySelectorAll(".service-option input:checked"))
            .map(input => input.value);
        document.getElementById("serviceTags").value = tags.join(", ");
    }
</script>
</body>
</html>
