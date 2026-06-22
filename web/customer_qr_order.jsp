<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.CartItem"%>
<%@page import="com.mycoffee.model.Product"%>
<%@page import="com.mycoffee.model.Table"%>
<%@page import="com.mycoffee.model.User"%>
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

    private String productImage(Product product) {
        if (product != null && product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            return product.getImageUrl();
        }
        return "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?auto=format&fit=crop&w=300&q=80";
    }
%>
<%
    String contextPath = request.getContextPath();
    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null)
            ? loggedInUser.getFullName() : "Khách hàng";

    List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
    if (cart == null) {
        cart = new ArrayList<>();
    }

    List<Table> tables = (List<Table>) request.getAttribute("tables");
    if (tables == null) {
        tables = new ArrayList<>();
    }

    int selectedTableId = request.getAttribute("selectedTableId") != null
            ? (Integer) request.getAttribute("selectedTableId") : 0;
    String selectedTableName = request.getAttribute("selectedTableName") != null
            ? (String) request.getAttribute("selectedTableName") : "Chưa chọn bàn";
    boolean qrScanned = request.getAttribute("qrScanned") != null
            ? (Boolean) request.getAttribute("qrScanned") : false;

    NumberFormat currency = NumberFormat.getInstance(new Locale("vi", "VN"));
    int cartCount = 0;
    double cartTotal = 0;
    for (CartItem item : cart) {
        cartCount += item.getQuantity();
        cartTotal += item.getLineTotal();
    }

    String cartMessage = (String) session.getAttribute("cartMessage");
    String cartError = (String) session.getAttribute("cartError");
    session.removeAttribute("cartMessage");
    session.removeAttribute("cartError");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt món QR - Modern Cafe</title>
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
            --line: #DFE6F8;
        }

        body {
            background: var(--bg);
            color: #172B4D;
        }

        .qr-topbar {
            height: 78px;
            display: grid;
            grid-template-columns: 1fr minmax(260px, 420px) auto;
            align-items: center;
            gap: 26px;
            padding: 0 42px;
            background: rgba(244, 246, 255, .92);
            border-bottom: 1px solid rgba(223, 230, 248, .75);
            backdrop-filter: blur(18px);
            position: sticky;
            top: 0;
            z-index: 30;
        }

        .top-brand {
            display: inline-flex;
            align-items: center;
            gap: 18px;
            color: #002B7F;
            font-size: 24px;
            font-weight: 900;
            text-decoration: none;
        }

        .search-pill {
            height: 52px;
            border-radius: 999px;
            background: #EAF0FF;
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 0 22px;
            color: #42526E;
        }

        .search-pill input {
            border: 0;
            outline: 0;
            background: transparent;
            width: 100%;
            font-size: 14px;
        }

        .table-chip {
            background: #fff;
            border: 1px solid #D8E1F4;
            border-radius: 28px;
            padding: 18px 26px;
            display: flex;
            align-items: center;
            gap: 18px;
            box-shadow: 0 16px 34px rgba(66, 82, 110, .1);
        }

        .page-title {
            font-size: 56px;
            line-height: 1;
            font-weight: 900;
            color: #002B7F;
            letter-spacing: -.04em;
        }

        .soft-card {
            background: #fff;
            border: 1px solid rgba(223,230,248,.9);
            border-radius: 36px;
            box-shadow: 0 18px 42px rgba(66,82,110,.09);
        }

        .qr-frame {
            width: 285px;
            height: 285px;
            border-radius: 28px;
            background: #fff;
            border: 12px solid #F0F2F7;
            box-shadow: inset 0 0 0 4px #E7E9EF, 0 10px 28px rgba(66,82,110,.08);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #C6D4EC;
            font-size: 84px;
        }

        .blue-cta {
            border-radius: 32px;
            background: var(--primary);
            color: #fff;
            box-shadow: 0 18px 36px rgba(0,82,204,.22);
        }

        .qty-control {
            min-width: 118px;
            height: 42px;
            border-radius: 999px;
            background: #EAF0FF;
            display: grid;
            grid-template-columns: 36px 1fr 36px;
            align-items: center;
            gap: 2px;
            padding: 0 6px;
        }

        .qty-control button {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #fff;
            color: #0052CC;
            font-weight: 900;
            border: 0;
        }

        .outline-action {
            height: 60px;
            border-radius: 16px;
            border: 2px solid var(--primary);
            color: var(--primary);
            background: #fff;
            font-size: 16px;
            font-weight: 900;
        }

        .primary-action {
            height: 60px;
            border-radius: 16px;
            border: 0;
            background: var(--primary);
            color: #fff;
            font-size: 16px;
            font-weight: 900;
            box-shadow: 0 14px 28px rgba(0,82,204,.2);
        }

        @media (max-width: 1180px) {
            .qr-topbar {
                grid-template-columns: 1fr;
                height: auto;
                padding: 18px 24px;
            }
            .page-title {
                font-size: 42px;
            }
        }
    </style>
</head>
<body class="h-screen flex font-sans overflow-hidden">
    <jsp:include page="common/sidebar_customer.jsp" />

    <div class="flex-1 h-screen overflow-hidden">
        <header class="qr-topbar">
            <a class="top-brand" href="<%= contextPath %>/customer-menu">
                <i class="fa-solid fa-qrcode text-[24px]"></i>
                <span>Modern Cafe</span>
            </a>
            <label class="search-pill">
                <i class="fa-solid fa-magnifying-glass text-[18px]"></i>
                <input type="search" placeholder="Tìm món ăn...">
            </label>
            <div class="flex items-center justify-end gap-7 text-[#172B4D]">
                <i class="fa-regular fa-bell text-[22px]"></i>
                <i class="fa-solid fa-cart-shopping text-[22px]"></i>
                <div class="w-11 h-11 rounded-full bg-gradient-to-br from-[#00B8D9] to-[#0052CC] p-[2px]">
                    <div class="w-full h-full rounded-full bg-white flex items-center justify-center text-[#0052CC] font-black text-xs">
                        <%= safe(fullName.substring(0, Math.min(1, fullName.length()))).toUpperCase() %>
                    </div>
                </div>
            </div>
        </header>

        <main class="h-[calc(100vh-78px)] overflow-y-auto px-10 py-12">
            <div class="max-w-[1180px] mx-auto">
                <div class="flex flex-col xl:flex-row xl:items-start xl:justify-between gap-8">
                    <div>
                        <h1 class="page-title">Đặt món tại bàn</h1>
                        <p class="mt-3 text-[20px] font-medium text-[#172B4D]">Vui lòng quét QR để bắt đầu trải nghiệm</p>
                    </div>
                    <% if (qrScanned) { %>
                        <div class="table-chip">
                            <div class="w-14 h-14 rounded-full bg-[#0052CC] text-white flex items-center justify-center text-[24px]">
                                <i class="fa-solid fa-chair"></i>
                            </div>
                            <div>
                                <div class="text-[13px] uppercase tracking-wide font-black text-[#172B4D]">Vị trí hiện tại</div>
                                <div class="mt-1 text-[30px] leading-none font-black text-[#002B7F]"><%= safe(selectedTableName) %></div>
                            </div>
                        </div>
                    <% } else { %>
                        <form action="<%= contextPath %>/customer-qr-order" method="get" class="table-chip">
                            <div class="w-14 h-14 rounded-full bg-[#0052CC] text-white flex items-center justify-center text-[24px]">
                                <i class="fa-solid fa-chair"></i>
                            </div>
                            <div>
                                <div class="text-[13px] uppercase tracking-wide font-black text-[#172B4D]">Chọn bàn để test</div>
                                <select name="tableId" onchange="this.form.submit()" class="mt-1 bg-transparent text-[30px] leading-none font-black text-[#002B7F] outline-none">
                                    <% for (Table table : tables) { %>
                                        <option value="<%= table.getTableID() %>" <%= table.getTableID() == selectedTableId ? "selected" : "" %>>
                                            <%= safe(table.getTableName()) %>
                                        </option>
                                    <% } %>
                                </select>
                            </div>
                        </form>
                    <% } %>
                </div>

                <% if (cartMessage != null) { %>
                    <div class="mt-8 rounded-2xl bg-[#E3FCEF] border border-[#ABF5D1] px-5 py-4 text-[#006644] text-[14px] font-black">
                        <i class="fa-solid fa-circle-check mr-2"></i><%= safe(cartMessage) %>
                    </div>
                <% } %>
                <% if (cartError != null) { %>
                    <div class="mt-8 rounded-2xl bg-[#FFEBE6] border border-[#FFBDAD] px-5 py-4 text-[#DE350B] text-[14px] font-black">
                        <i class="fa-solid fa-circle-exclamation mr-2"></i><%= safe(cartError) %>
                    </div>
                <% } %>

                <div class="grid grid-cols-1 xl:grid-cols-[470px_1fr] gap-8 mt-10">
                    <section>
                        <div class="soft-card min-h-[470px] p-10 flex flex-col items-center justify-center text-center">
                            <div class="qr-frame">
                                <i class="fa-solid fa-qrcode"></i>
                            </div>
                            <div class="mt-9 inline-flex items-center gap-3 rounded-full bg-[#DDF9FF] text-[#006C80] px-6 py-3 text-[15px] font-black">
                                <span class="w-3 h-3 rounded-full bg-[#00A3BF]"></span>
                                Đã nhận diện thành công
                            </div>
                            <p class="mt-6 max-w-[320px] text-[17px] leading-7 font-medium text-[#172B4D]">
                                Mã bàn đã được xác thực. Bạn có thể bắt đầu chọn món ngay.
                            </p>
                        </div>

                        <a href="<%= contextPath %>/customer-menu" class="blue-cta mt-8 min-h-[118px] px-8 flex items-center justify-between no-underline">
                            <div class="flex items-center gap-5">
                                <div class="w-16 h-16 rounded-2xl bg-white/20 flex items-center justify-center text-[32px]">
                                    <i class="fa-regular fa-square-plus"></i>
                                </div>
                                <div>
                                    <div class="text-[25px] leading-tight font-black text-white">Thêm món mới</div>
                                    <div class="mt-2 text-[15px] font-semibold text-white/90">Xem thực đơn đa dạng của quán</div>
                                </div>
                            </div>
                            <i class="fa-solid fa-chevron-right text-[22px] text-white"></i>
                        </a>
                    </section>

                    <section class="soft-card p-10">
                        <div class="flex items-center justify-between gap-5">
                            <h2 class="text-[30px] leading-tight font-black text-[#002B7F] flex items-center gap-4">
                                <i class="fa-solid fa-basket-shopping text-[23px]"></i>
                                Giỏ hàng tạm thời
                            </h2>
                            <span class="rounded-full bg-[#EAF0FF] px-5 py-2 text-[15px] font-black text-[#172B4D]"><%= cartCount %> món</span>
                        </div>

                        <div class="mt-8 space-y-7 min-h-[310px]">
                            <% if (cart.isEmpty()) { %>
                                <div class="h-[300px] flex flex-col items-center justify-center text-center">
                                    <i class="fa-solid fa-cart-shopping text-[64px] text-[#C8D3EA]"></i>
                                    <p class="mt-5 text-[18px] font-black text-[#172B4D]">Giỏ hàng đang trống</p>
                                    <p class="mt-2 text-[14px] text-[#42526E]">Hãy thêm món từ menu để gửi order.</p>
                                </div>
                            <% } else { %>
                                <% for (CartItem item : cart) {
                                    Product product = item.getProduct();
                                    if (product == null) {
                                        continue;
                                    }
                                %>
                                    <div class="grid grid-cols-[100px_1fr_auto] gap-5 items-center border-b border-[#E7ECF8] pb-7">
                                        <img src="<%= safe(productImage(product)) %>" alt="<%= safe(product.getProductName()) %>" class="w-[86px] h-[86px] rounded-2xl object-cover">
                                        <div class="min-w-0">
                                            <h3 class="text-[24px] leading-tight font-black text-[#07142F] truncate"><%= safe(product.getProductName()) %></h3>
                                            <p class="mt-2 text-[15px] font-medium text-[#172B4D]">Ít đá, 50% đường</p>
                                            <p class="mt-5 text-[20px] font-black text-[#002B7F]"><%= currency.format(product.getBasePrice()) %>đ</p>
                                        </div>
                                        <div class="qty-control">
                                            <form action="<%= contextPath %>/customer-cart" method="post">
                                                <input type="hidden" name="action" value="decrease">
                                                <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                                                <input type="hidden" name="redirect" value="/customer-qr-order?tableId=<%= selectedTableId %>">
                                                <button type="submit">-</button>
                                            </form>
                                            <span class="text-center text-[16px] font-black text-[#07142F]"><%= item.getQuantity() %></span>
                                            <form action="<%= contextPath %>/customer-cart" method="post">
                                                <input type="hidden" name="action" value="increase">
                                                <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                                                <input type="hidden" name="redirect" value="/customer-qr-order?tableId=<%= selectedTableId %>">
                                                <button type="submit">+</button>
                                            </form>
                                        </div>
                                    </div>
                                <% } %>
                            <% } %>
                        </div>

                        <form action="<%= contextPath %>/customer-checkout" method="post" class="mt-8">
                            <input type="hidden" name="tableId" value="<%= selectedTableId %>">
                            <label class="block mb-6">
                                <span class="block text-[13px] font-black text-[#42526E] mb-2">Ghi chú cho quán</span>
                                <input name="note" type="text" placeholder="Ví dụ: ít đá, không ống hút..." class="w-full h-12 rounded-2xl border border-[#DFE6F8] bg-[#F8FAFF] px-4 outline-none text-[14px] font-medium">
                            </label>

                            <div class="flex items-center justify-between gap-5">
                                <span class="text-[20px] font-medium text-[#172B4D]">Tổng cộng</span>
                                <span class="text-[38px] leading-none font-black text-[#002B7F]"><%= currency.format(cartTotal) %>đ</span>
                            </div>

                            <div class="grid grid-cols-1 md:grid-cols-[210px_1fr] gap-5 mt-7">
                                <button type="submit" form="clearCartForm" class="outline-action">Hủy giỏ</button>
                                <button type="submit" class="primary-action" <%= cart.isEmpty() ? "disabled" : "" %>>
                                    Gửi Order
                                    <i class="fa-regular fa-paper-plane ml-3"></i>
                                </button>
                            </div>
                        </form>

                        <form id="clearCartForm" action="<%= contextPath %>/customer-cart" method="post">
                            <input type="hidden" name="action" value="clear">
                            <input type="hidden" name="redirect" value="/customer-qr-order?tableId=<%= selectedTableId %>">
                        </form>
                    </section>
                </div>
            </div>
        </main>
    </div>
</body>
</html>
