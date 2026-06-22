<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Product"%>
<%@page import="com.mycoffee.model.CartItem"%>
<%@page import="java.text.NumberFormat"%>
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

    private String defaultDescription(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Món được chuẩn bị tươi mới tại Modern Cafe, cân bằng giữa hương vị, độ ngọt và hậu vị để bạn dễ dàng thưởng thức trong ngày.";
        }
        return value.trim();
    }

    private String categoryFallbackName(int categoryId) {
        switch (categoryId) {
            case 1:
                return "Cafe";
            case 2:
                return "Đá xay";
            case 3:
                return "Trà trái cây";
            case 4:
                return "Socola";
            default:
                return "Đồ ăn nhẹ";
        }
    }

    private String productImage(Product product) {
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            return product.getImageUrl();
        }
        switch (product.getCategoryId()) {
            case 1:
                return "https://source.unsplash.com/1100x900/?latte,coffee";
            case 2:
                return "https://source.unsplash.com/1100x900/?frappuccino,drink";
            case 3:
                return "https://source.unsplash.com/1100x900/?fruit-tea,iced-tea";
            case 4:
                return "https://source.unsplash.com/1100x900/?hot-chocolate,cocoa";
            default:
                return "https://source.unsplash.com/1100x900/?croissant,pastry";
        }
    }
%>
<%
    Product product = (Product) request.getAttribute("product");
    if (product == null) {
        response.sendRedirect(request.getContextPath() + "/menu");
        return;
    }

    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null)
            ? loggedInUser.getFullName() : "Khách hàng";

    List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
    if (cart == null) {
        cart = new ArrayList<>();
    }
    int cartCount = 0;
    for (CartItem item : cart) {
        cartCount += item.getQuantity();
    }

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    String categoryName = product.getCategoryName();
    if (categoryName == null || categoryName.trim().isEmpty()) {
        categoryName = categoryFallbackName(product.getCategoryId());
    }
    String description = defaultDescription(product.getDescription());
    double oldPrice = Math.round(product.getBasePrice() * 1.18 / 1000) * 1000;
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= safe(product.getProductName()) %> - Modern Cafe</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Be Vietnam Pro', 'Outfit', 'sans-serif']
                    }
                }
            }
        }
    </script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800;900&family=Outfit:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #0052CC;
            --secondary: #00B8D9;
            --tertiary: #FFAB00;
            --neutral: #42526E;
            --ink: #0B1F45;
            --bg: #F4F6FF;
            --panel: #FFFFFF;
            --line: #DFE6F8;
        }

        body {
            background: var(--bg);
            color: #172B4D;
        }

        .detail-topbar {
            height: 78px;
            display: grid;
            grid-template-columns: 1fr auto;
            align-items: center;
            gap: 24px;
            padding: 0 42px;
            background: rgba(244, 246, 255, .92);
            border-bottom: 1px solid rgba(223, 230, 248, .75);
            backdrop-filter: blur(18px);
            position: sticky;
            top: 0;
            z-index: 30;
        }

        .detail-panel {
            border-radius: 28px;
            background: #fff;
            border: 1px solid var(--line);
            box-shadow: 0 22px 46px rgba(66,82,110,.1);
            overflow: hidden;
        }

        .product-photo {
            min-height: 620px;
            background: linear-gradient(135deg, #EBF2FF, #D8FCFF);
            position: relative;
        }

        .product-photo img {
            width: 100%;
            height: 100%;
            min-height: 620px;
            object-fit: cover;
            display: block;
        }

        .stock-badge {
            position: absolute;
            left: 28px;
            top: 28px;
            height: 30px;
            display: inline-flex;
            align-items: center;
            gap: 7px;
            padding: 0 14px;
            border-radius: 999px;
            background: #fff;
            color: var(--primary);
            font-size: 12px;
            font-weight: 900;
            box-shadow: 0 8px 18px rgba(66,82,110,.12);
        }

        .quantity-box {
            height: 54px;
            border-radius: 14px;
            border: 1px solid #C8D3EA;
            background: #F8FAFF;
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 0 16px;
        }

        .quantity-box input {
            width: 84px;
            background: transparent;
            border: 0;
            outline: 0;
            color: #172B4D;
            font-weight: 900;
            font-size: 16px;
        }

        .primary-action {
            height: 54px;
            border-radius: 14px;
            border: 0;
            background: var(--primary);
            color: #fff;
            font-size: 15px;
            font-weight: 900;
            box-shadow: 0 14px 26px rgba(0,82,204,.24);
        }

        .secondary-link {
            height: 46px;
            border-radius: 999px;
            border: 1px solid #C8D3EA;
            background: #fff;
            color: var(--primary);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            padding: 0 20px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 900;
        }
    </style>
</head>
<body class="h-screen flex font-sans overflow-hidden">
    <jsp:include page="common/sidebar_customer.jsp" />

    <div class="flex-1 h-screen overflow-hidden">
        <header class="detail-topbar">
            <a href="<%= contextPath %>/menu" class="secondary-link w-fit">
                <i class="fa-solid fa-arrow-left"></i>
                <span>Quay lại menu</span>
            </a>
            <div class="flex items-center justify-end gap-6 text-[#002B7F]">
                <button class="relative text-[20px]" type="button" title="Thông báo">
                    <i class="fa-regular fa-bell"></i>
                </button>
                <a href="<%= contextPath %>/menu#cartSummary" class="relative text-[20px] text-[#002B7F] no-underline" title="Giỏ hàng">
                    <i class="fa-solid fa-cart-shopping"></i>
                    <% if (cartCount > 0) { %>
                        <span class="absolute -top-3 -right-3 min-w-[19px] h-[19px] px-1 rounded-full bg-[#00B8D9] text-white text-[10px] font-black flex items-center justify-center"><%= cartCount %></span>
                    <% } %>
                </a>
                <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#00B8D9] to-[#0052CC] p-[2px]">
                    <div class="w-full h-full rounded-full bg-white flex items-center justify-center text-[#0052CC] font-black text-xs">
                        <%= safe(fullName.substring(0, Math.min(1, fullName.length()))).toUpperCase() %>
                    </div>
                </div>
            </div>
        </header>

        <main class="h-[calc(100vh-78px)] overflow-y-auto px-10 py-8">
            <div class="max-w-[1120px] mx-auto">
                <section class="detail-panel grid grid-cols-1 xl:grid-cols-2">
                    <div class="product-photo">
                        <img src="<%= safe(productImage(product)) %>" alt="<%= safe(product.getProductName()) %>">
                        <span class="stock-badge"><i class="fa-regular fa-circle-check"></i>Còn hàng</span>
                    </div>

                    <div class="p-10 xl:p-12 flex flex-col justify-center">
                        <div class="flex items-center gap-3">
                            <span class="inline-flex h-8 items-center px-4 rounded-full bg-[#E6FCFF] text-[#008DA6] text-[12px] font-black uppercase tracking-wide">
                                <%= safe(categoryName) %>
                            </span>
                            <span class="inline-flex h-8 items-center px-4 rounded-full bg-[#FFF3CD] text-[#8B5E00] text-[12px] font-black uppercase tracking-wide">
                                Best choice
                            </span>
                        </div>

                        <h1 class="mt-7 text-[44px] leading-tight font-black tracking-tight text-[#0B1F45]">
                            <%= safe(product.getProductName()) %>
                        </h1>
                        <p class="mt-5 text-[16px] leading-8 font-medium text-[#42526E]">
                            <%= safe(description) %>
                        </p>

                        <div class="mt-8 rounded-[22px] bg-[#F4F6FF] border border-[#DFE6F8] p-6">
                            <div class="flex items-end gap-4">
                                <span class="text-[36px] leading-none font-black text-[#002B7F]"><%= currency.format(product.getBasePrice()) %>đ</span>
                                <span class="text-[15px] font-bold text-[#A5ADBA] line-through"><%= currency.format(oldPrice) %>đ</span>
                            </div>
                            <div class="mt-4 grid grid-cols-3 gap-3 text-center">
                                <div class="rounded-2xl bg-white border border-[#DFE6F8] p-4">
                                    <div class="text-[18px] font-black text-[#0052CC]">5-7</div>
                                    <div class="text-[11px] font-bold text-[#6B778C] mt-1">phút</div>
                                </div>
                                <div class="rounded-2xl bg-white border border-[#DFE6F8] p-4">
                                    <div class="text-[18px] font-black text-[#00A3BF]">Fresh</div>
                                    <div class="text-[11px] font-bold text-[#6B778C] mt-1">mỗi ngày</div>
                                </div>
                                <div class="rounded-2xl bg-white border border-[#DFE6F8] p-4">
                                    <div class="text-[18px] font-black text-[#8B5E00]">Gold</div>
                                    <div class="text-[11px] font-bold text-[#6B778C] mt-1">ưu đãi</div>
                                </div>
                            </div>
                        </div>

                        <form action="<%= contextPath %>/customer-cart" method="post" class="mt-8 grid grid-cols-1 sm:grid-cols-[190px_1fr] gap-4">
                            <input type="hidden" name="action" value="add">
                            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                            <input type="hidden" name="redirect" value="/product-detail?id=<%= product.getProductId() %>">
                            <label class="quantity-box">
                                <span class="text-[13px] font-black text-[#42526E]">Số lượng</span>
                                <input type="number" name="quantity" value="1" min="1" max="99">
                            </label>
                            <button type="submit" class="primary-action">
                                <i class="fa-solid fa-cart-plus mr-2"></i>
                                Thêm vào giỏ hàng
                            </button>
                        </form>
                    </div>
                </section>
            </div>
        </main>
    </div>
</body>
</html>
