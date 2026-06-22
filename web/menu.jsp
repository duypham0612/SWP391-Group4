<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Product"%>
<%@page import="com.mycoffee.model.CartItem"%>
<%@page import="java.text.NumberFormat"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.LinkedHashMap"%>
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

    private String shortText(String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return "Thức uống được chuẩn bị tươi mới với hương vị cân bằng, phù hợp để thưởng thức mỗi ngày.";
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
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
                return "https://source.unsplash.com/900x700/?latte,coffee";
            case 2:
                return "https://source.unsplash.com/900x700/?frappuccino,drink";
            case 3:
                return "https://source.unsplash.com/900x700/?fruit-tea,iced-tea";
            case 4:
                return "https://source.unsplash.com/900x700/?hot-chocolate,cocoa";
            default:
                return "https://source.unsplash.com/900x700/?croissant,pastry";
        }
    }
%>
<%
    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null)
            ? loggedInUser.getFullName() : "Khách hàng";

    List<Product> products = (List<Product>) request.getAttribute("products");
    if (products == null) {
        products = new ArrayList<>();
    }

    List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
    if (cart == null) {
        cart = new ArrayList<>();
    }

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    int cartCount = 0;
    double cartTotal = 0;
    for (CartItem item : cart) {
        cartCount += item.getQuantity();
        cartTotal += item.getLineTotal();
    }

    Map<Integer, String> categories = new LinkedHashMap<>();
    for (Product product : products) {
        String categoryName = product.getCategoryName();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = categoryFallbackName(product.getCategoryId());
        }
        categories.put(product.getCategoryId(), categoryName);
    }

    String cartMessage = (String) session.getAttribute("cartMessage");
    String cartError = (String) session.getAttribute("cartError");
    session.removeAttribute("cartMessage");
    session.removeAttribute("cartError");

    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Menu online - Modern Cafe</title>
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

        .menu-topbar {
            height: 78px;
            display: grid;
            grid-template-columns: 1fr minmax(280px, 460px) 1fr;
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

        .search-pill {
            height: 54px;
            border-radius: 999px;
            border: 1px solid transparent;
            background: #EAF0FF;
            color: #42526E;
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 0 22px;
        }

        .search-pill input {
            width: 100%;
            border: 0;
            outline: 0;
            background: transparent;
            font-size: 14px;
            color: #172B4D;
        }

        .hero-card {
            min-height: 382px;
            border-radius: 24px;
            overflow: hidden;
            background:
                linear-gradient(90deg, rgba(0,82,204,.95) 0%, rgba(0,82,204,.78) 36%, rgba(0,0,0,.08) 64%),
                url("https://source.unsplash.com/1400x640/?iced-tea,berries") center/cover;
            box-shadow: 0 22px 44px rgba(66,82,110,.14);
        }

        .category-pill {
            height: 44px;
            padding: 0 28px;
            border-radius: 999px;
            border: 1px solid #C8D3EA;
            background: #fff;
            color: #172B4D;
            font-size: 14px;
            font-weight: 600;
            transition: all .18s ease;
            white-space: nowrap;
        }

        .category-pill.active,
        .category-pill:hover {
            color: #fff;
            border-color: var(--primary);
            background: var(--primary);
            box-shadow: 0 10px 22px rgba(0,82,204,.2);
        }

        .product-card {
            background: #fff;
            border-radius: 26px;
            overflow: hidden;
            box-shadow: 0 16px 34px rgba(66,82,110,.08);
            transition: transform .18s ease, box-shadow .18s ease;
        }

        .product-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 24px 42px rgba(66,82,110,.14);
        }

        .product-visual {
            height: 420px;
            position: relative;
            background: linear-gradient(135deg, #EBF2FF, #D8FCFF);
        }

        .product-visual img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }

        .stock-badge {
            position: absolute;
            left: 18px;
            top: 18px;
            height: 26px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 0 12px;
            border-radius: 999px;
            background: #fff;
            color: var(--primary);
            font-size: 11px;
            font-weight: 800;
            box-shadow: 0 8px 18px rgba(66,82,110,.12);
        }

        .favorite-btn {
            position: absolute;
            right: 18px;
            top: 18px;
            width: 42px;
            height: 42px;
            border-radius: 50%;
            border: 2px solid #fff;
            background: #fff;
            color: #DE350B;
            box-shadow: 0 10px 20px rgba(66,82,110,.16);
        }

        .detail-btn {
            height: 48px;
            border-radius: 10px;
            border: 1.5px solid var(--primary);
            color: var(--primary);
            font-weight: 800;
            font-size: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            text-decoration: none;
            transition: all .18s ease;
        }

        .detail-btn:hover {
            color: #fff;
            background: var(--primary);
        }

        .cart-square {
            width: 48px;
            height: 48px;
            border-radius: 10px;
            border: 0;
            background: var(--primary);
            color: #fff;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 10px 22px rgba(0,82,204,.22);
        }

        .notice {
            border-radius: 16px;
            padding: 14px 18px;
            font-size: 13px;
            font-weight: 800;
        }

        @media (max-width: 1100px) {
            .menu-topbar {
                grid-template-columns: 1fr;
                height: auto;
                padding: 18px 24px;
            }
            .product-visual {
                height: 320px;
            }
        }
    </style>
</head>
<body class="h-screen flex font-sans overflow-hidden">
    <jsp:include page="common/sidebar_customer.jsp" />

    <div class="flex-1 h-screen overflow-hidden">
        <header class="menu-topbar">
            <div></div>
            <label class="search-pill">
                <i class="fa-solid fa-magnifying-glass text-[#42526E]"></i>
                <input id="menuSearch" type="search" placeholder="Tìm món ngon ngay..." oninput="filterProducts()">
            </label>
            <div class="flex items-center justify-end gap-6 text-[#002B7F]">
                <button class="relative text-[20px]" type="button" title="Thông báo">
                    <i class="fa-regular fa-bell"></i>
                </button>
                <button class="relative text-[20px]" type="button" title="Giỏ hàng" onclick="scrollToCartSummary()">
                    <i class="fa-solid fa-cart-shopping"></i>
                    <% if (cartCount > 0) { %>
                        <span class="absolute -top-3 -right-3 min-w-[19px] h-[19px] px-1 rounded-full bg-[#00B8D9] text-white text-[10px] font-black flex items-center justify-center"><%= cartCount %></span>
                    <% } %>
                </button>
                <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#00B8D9] to-[#0052CC] p-[2px]">
                    <div class="w-full h-full rounded-full bg-white flex items-center justify-center text-[#0052CC] font-black text-xs">
                        <%= safe(fullName.substring(0, Math.min(1, fullName.length()))).toUpperCase() %>
                    </div>
                </div>
            </div>
        </header>

        <main class="h-[calc(100vh-78px)] overflow-y-auto px-10 pb-16">
            <div class="max-w-[1120px] mx-auto pt-8">
                <section class="hero-card flex items-center px-16">
                    <div class="max-w-[560px] text-white">
                        <div class="inline-flex items-center px-5 py-2 rounded-full bg-[#4BE3FF] text-[#003B87] text-[12px] font-black uppercase tracking-wide">
                            Ưu đãi tháng 10
                        </div>
                        <h1 class="mt-6 text-[48px] leading-[1.08] font-black tracking-tight drop-shadow-sm">
                            Mua 1 Tặng 1 Toàn<br>Menu Trà
                        </h1>
                        <p class="mt-6 text-[18px] leading-8 font-medium text-white/95">
                            Thưởng thức hương vị thiên nhiên với ưu đãi độc quyền dành cho Thành viên Vàng.
                        </p>
                        <a href="#menu-grid" class="mt-8 inline-flex h-[48px] px-9 items-center justify-center rounded-full bg-white text-[#002B7F] text-[14px] font-black no-underline shadow-lg">
                            Nhận mã ngay
                        </a>
                    </div>
                </section>

                <% if (cartMessage != null) { %>
                    <div class="notice mt-7 bg-[#E3FCEF] text-[#006644] border border-[#ABF5D1]">
                        <i class="fa-solid fa-circle-check mr-2"></i><%= safe(cartMessage) %>
                    </div>
                <% } %>
                <% if (cartError != null) { %>
                    <div class="notice mt-7 bg-[#FFEBE6] text-[#DE350B] border border-[#FFBDAD]">
                        <i class="fa-solid fa-circle-exclamation mr-2"></i><%= safe(cartError) %>
                    </div>
                <% } %>

                <div class="flex items-center gap-3 mt-12 overflow-x-auto pb-2">
                    <button type="button" class="category-pill active" data-category="all" onclick="setCategory('all')">Tất cả</button>
                    <% for (Map.Entry<Integer, String> entry : categories.entrySet()) { %>
                        <button type="button" class="category-pill" data-category="<%= entry.getKey() %>" onclick="setCategory('<%= entry.getKey() %>')">
                            <%= safe(entry.getValue()) %>
                        </button>
                    <% } %>
                </div>

                <% if (products.isEmpty()) { %>
                    <div class="mt-10 bg-white rounded-[24px] border border-[#DFE6F8] p-12 text-center">
                        <i class="fa-solid fa-mug-saucer text-6xl text-[#C8D3EA]"></i>
                        <p class="mt-5 text-[16px] font-black text-[#172B4D]">Chưa có sản phẩm đang bán.</p>
                    </div>
                <% } else { %>
                    <div id="menu-grid" class="grid grid-cols-1 xl:grid-cols-2 gap-7 mt-10">
                        <% for (Product product : products) {
                            String categoryName = product.getCategoryName();
                            if (categoryName == null || categoryName.trim().isEmpty()) {
                                categoryName = categoryFallbackName(product.getCategoryId());
                            }
                            String description = shortText(product.getDescription(), 106);
                            double oldPrice = Math.round(product.getBasePrice() * 1.18 / 1000) * 1000;
                        %>
                            <article class="product-card"
                                     data-category="<%= product.getCategoryId() %>"
                                     data-name="<%= safe(product.getProductName()).toLowerCase() %>"
                                     data-description="<%= safe(description).toLowerCase() %>">
                                <div class="product-visual">
                                    <img src="<%= safe(productImage(product)) %>" alt="<%= safe(product.getProductName()) %>">
                                    <span class="stock-badge"><i class="fa-regular fa-circle-check"></i>Còn hàng</span>
                                    <button type="button" class="favorite-btn" title="Yêu thích">
                                        <i class="fa-regular fa-heart"></i>
                                    </button>
                                </div>
                                <div class="p-6">
                                    <div class="text-[12px] font-black uppercase tracking-wide text-[#00A3BF]"><%= safe(categoryName) %></div>
                                    <h2 class="mt-3 text-[22px] leading-tight font-black text-[#172B4D]"><%= safe(product.getProductName()) %></h2>
                                    <p class="mt-4 min-h-[48px] text-[14px] leading-6 font-medium text-[#42526E]"><%= safe(description) %></p>
                                    <div class="mt-5 flex items-end gap-4">
                                        <span class="text-[26px] leading-none font-black text-[#002B7F]"><%= currency.format(product.getBasePrice()) %>đ</span>
                                        <span class="text-[13px] font-bold text-[#A5ADBA] line-through"><%= currency.format(oldPrice) %>đ</span>
                                    </div>
                                    <div class="mt-7 grid grid-cols-[1fr_48px] gap-3">
                                        <a class="detail-btn" href="<%= contextPath %>/product-detail?id=<%= product.getProductId() %>">Chi tiết</a>
                                        <form action="<%= contextPath %>/customer-cart" method="post" class="m-0">
                                            <input type="hidden" name="action" value="add">
                                            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                                            <input type="hidden" name="quantity" value="1">
                                            <input type="hidden" name="redirect" value="/menu">
                                            <button type="submit" class="cart-square" title="Thêm vào giỏ">
                                                <i class="fa-solid fa-cart-plus"></i>
                                            </button>
                                        </form>
                                    </div>
                                </div>
                            </article>
                        <% } %>
                    </div>
                <% } %>

                <section id="cartSummary" class="mt-10 bg-white border border-[#DFE6F8] rounded-[24px] p-7 shadow-[0_16px_34px_rgba(66,82,110,.08)]">
                    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                        <div>
                            <h2 class="text-[22px] font-black text-[#172B4D]">Giỏ hàng của bạn</h2>
                            <p class="mt-1 text-[14px] font-medium text-[#42526E]"><%= cartCount %> món đã chọn</p>
                        </div>
                        <div class="text-left md:text-right">
                            <p class="text-[13px] font-bold text-[#6B778C]">Tổng tiền tạm tính</p>
                            <p class="text-[28px] leading-tight font-black text-[#002B7F]"><%= currency.format(cartTotal) %>đ</p>
                        </div>
                    </div>

                    <% if (!cart.isEmpty()) { %>
                        <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-3">
                            <% for (CartItem item : cart) {
                                Product cartProduct = item.getProduct();
                                if (cartProduct == null) {
                                    continue;
                                }
                            %>
                                <div class="flex items-center justify-between gap-4 rounded-2xl bg-[#F4F6FF] border border-[#DFE6F8] px-4 py-3">
                                    <div class="min-w-0">
                                        <div class="truncate text-[14px] font-black text-[#172B4D]"><%= safe(cartProduct.getProductName()) %></div>
                                        <div class="text-[12px] font-bold text-[#6B778C]"><%= item.getQuantity() %> x <%= currency.format(cartProduct.getBasePrice()) %>đ</div>
                                    </div>
                                    <div class="flex items-center gap-2">
                                        <form action="<%= contextPath %>/customer-cart" method="post">
                                            <input type="hidden" name="action" value="decrease">
                                            <input type="hidden" name="productId" value="<%= cartProduct.getProductId() %>">
                                            <input type="hidden" name="redirect" value="/menu#cartSummary">
                                            <button type="submit" class="w-8 h-8 rounded-lg bg-white text-[#0052CC] font-black">-</button>
                                        </form>
                                        <form action="<%= contextPath %>/customer-cart" method="post">
                                            <input type="hidden" name="action" value="increase">
                                            <input type="hidden" name="productId" value="<%= cartProduct.getProductId() %>">
                                            <input type="hidden" name="redirect" value="/menu#cartSummary">
                                            <button type="submit" class="w-8 h-8 rounded-lg bg-white text-[#0052CC] font-black">+</button>
                                        </form>
                                    </div>
                                </div>
                            <% } %>
                        </div>
                    <% } %>

                    <div class="mt-7 grid grid-cols-1 md:grid-cols-[1fr_180px] gap-4">
                        <a href="<%= contextPath %>/customer-qr-order"
                           class="h-[54px] rounded-2xl bg-[#0052CC] text-white text-[15px] font-black flex items-center justify-center gap-3 no-underline shadow-[0_14px_28px_rgba(0,82,204,.2)]">
                            <span>Đặt món QR</span>
                            <i class="fa-regular fa-paper-plane"></i>
                        </a>
                        <form action="<%= contextPath %>/customer-cart" method="post">
                            <input type="hidden" name="action" value="clear">
                            <input type="hidden" name="redirect" value="/menu#cartSummary">
                            <button type="submit" class="w-full h-[54px] rounded-2xl border-2 border-[#0052CC] bg-white text-[#0052CC] text-[15px] font-black">
                                Huỷ giỏ
                            </button>
                        </form>
                    </div>
                </section>
            </div>
        </main>
    </div>

    <script>
        let activeCategory = 'all';

        function setCategory(category) {
            activeCategory = category;
            document.querySelectorAll('.category-pill').forEach(function (button) {
                button.classList.toggle('active', button.dataset.category === category);
            });
            filterProducts();
        }

        function filterProducts() {
            const keyword = (document.getElementById('menuSearch').value || '').trim().toLowerCase();
            document.querySelectorAll('.product-card').forEach(function (card) {
                const matchesCategory = activeCategory === 'all' || card.dataset.category === activeCategory;
                const text = (card.dataset.name || '') + ' ' + (card.dataset.description || '');
                const matchesKeyword = keyword.length === 0 || text.indexOf(keyword) >= 0;
                card.classList.toggle('hidden', !matchesCategory || !matchesKeyword);
            });
        }

        function scrollToCartSummary() {
            const cart = document.getElementById('cartSummary');
            if (cart) {
                cart.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
    </script>
</body>
</html>
