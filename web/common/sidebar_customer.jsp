<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Customer"%>
<%
    String uri = request.getRequestURI();

    boolean isMenu = uri.contains("customer-menu") || uri.contains("menu") || uri.contains("product-detail");
    boolean isQrOrder = uri.contains("customer-qr-order");
    boolean isOrderStatus = uri.contains("customer-order-status");
    boolean isReview = uri.contains("customer-review");
    boolean isHistory = uri.contains("customer-purchase-history");
    boolean isPoints = uri.contains("customer-points") || uri.contains("customer-track-points");
    boolean isMyVouchers = uri.contains("customer-my-vouchers");
    boolean isRedeem = uri.contains("customer-redeem-voucher");
    boolean isOffers = uri.contains("customer-offers");

    User loggedInUser = (User) session.getAttribute("user");
    Customer customerInfo = (Customer) request.getAttribute("customerInfo");

    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null && !loggedInUser.getFullName().isEmpty())
            ? loggedInUser.getFullName() : "Khách hàng";
    String memberRank = (customerInfo != null && customerInfo.getMemberRank() != null)
            ? customerInfo.getMemberRank() : "Thành viên Vàng";
%>

<style>
    :root {
        --customer-primary: #0052CC;
        --customer-secondary: #00B8D9;
        --customer-tertiary: #FFAB00;
        --customer-neutral: #42526E;
        --customer-ink: #0B1F45;
        --customer-bg: #F4F6FF;
        --customer-panel: #FFFFFF;
        --customer-line: #DFE6F8;
    }

    .customer-shell-sidebar {
        width: 292px;
        min-width: 292px;
        background: #fff;
        border-right: 1px solid var(--customer-line);
        box-shadow: 18px 0 36px rgba(66, 82, 110, .08);
    }

    .customer-brand {
        height: 78px;
        display: flex;
        align-items: center;
        padding: 0 38px;
        color: var(--customer-primary);
        font-weight: 900;
        letter-spacing: .01em;
        font-size: 20px;
    }

    .customer-member-card {
        padding: 18px 40px 24px;
    }

    .customer-rank-icon {
        width: 46px;
        height: 46px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #8B5E00;
        background: linear-gradient(145deg, #fff8dd, #e6edf9);
        box-shadow: inset 0 1px 0 rgba(255,255,255,.75), 0 12px 22px rgba(66,82,110,.12);
    }

    .customer-upgrade {
        width: 100%;
        height: 46px;
        margin-top: 18px;
        border: 0;
        border-radius: 10px;
        background: var(--customer-primary);
        color: #fff;
        font-size: 13px;
        font-weight: 800;
        cursor: pointer;
        box-shadow: 0 10px 22px rgba(0,82,204,.22);
    }

    .customer-logout-top {
        margin: 0 24px 18px;
        height: 42px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        background: #FFEBE6;
        color: #DE350B;
        border: 1px solid #FFBDAD;
        text-decoration: none;
        font-size: 13px;
        font-weight: 900;
        transition: background .18s ease, transform .18s ease;
    }

    .customer-logout-top:hover {
        background: #FFDDD6;
        transform: translateY(-1px);
    }

    .customer-nav {
        padding: 8px 24px 28px;
    }

    .customer-nav-item {
        position: relative;
        display: flex;
        align-items: center;
        gap: 18px;
        height: 54px;
        padding: 0 16px;
        color: #172B4D;
        text-decoration: none;
        font-size: 15px;
        font-weight: 500;
        border-radius: 12px;
        transition: background .18s ease, color .18s ease, transform .18s ease;
    }

    .customer-nav-item:hover {
        background: #F0F5FF;
        color: var(--customer-primary);
    }

    .customer-nav-item.active {
        color: var(--customer-primary);
        background: #EAF0FF;
        font-weight: 800;
    }

    .customer-nav-item.active::after {
        content: "";
        position: absolute;
        right: -12px;
        top: 9px;
        width: 4px;
        height: 36px;
        border-radius: 99px;
        background: var(--customer-primary);
    }

    .customer-nav-icon {
        width: 18px;
        text-align: center;
        font-size: 16px;
    }
</style>

<aside class="customer-shell-sidebar flex flex-col h-full z-20">
    <div class="customer-brand">Modern Cafe</div>

    <div class="customer-member-card">
        <div class="flex items-center gap-3">
            <div class="customer-rank-icon">
                <i class="fa-solid fa-medal"></i>
            </div>
            <div class="min-w-0">
                <div class="text-[24px] leading-tight font-black text-[#002B7F]">Xin chào!</div>
                <div class="text-[13px] font-medium text-[#172B4D] mt-1"><%= memberRank %></div>
            </div>
        </div>
        <button type="button" class="customer-upgrade">Nâng cấp hạng</button>
    </div>

    <a href="${pageContext.request.contextPath}/login?action=logout" class="customer-logout-top">
        <i class="fa-solid fa-right-from-bracket"></i>
        <span>Đăng xuất</span>
    </a>

    <nav class="customer-nav flex-1">
        <a href="${pageContext.request.contextPath}/customer-menu" class="customer-nav-item <%= isMenu ? "active" : "" %>">
            <i class="fa-solid fa-utensils customer-nav-icon"></i>
            <span>Menu online</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-qr-order" class="customer-nav-item <%= isQrOrder ? "active" : "" %>">
            <i class="fa-solid fa-qrcode customer-nav-icon"></i>
            <span>Đặt món QR</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-order-status" class="customer-nav-item <%= isOrderStatus ? "active" : "" %>">
            <i class="fa-solid fa-truck-fast customer-nav-icon"></i>
            <span>Theo dõi order</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-review" class="customer-nav-item <%= isReview ? "active" : "" %>">
            <i class="fa-regular fa-star customer-nav-icon"></i>
            <span>Đánh giá</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-purchase-history" class="customer-nav-item <%= isHistory ? "active" : "" %>">
            <i class="fa-regular fa-rectangle-list customer-nav-icon"></i>
            <span>Lịch sử</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-points" class="customer-nav-item <%= isPoints ? "active" : "" %>">
            <i class="fa-solid fa-tags customer-nav-icon"></i>
            <span>Tích điểm</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-my-vouchers" class="customer-nav-item <%= isMyVouchers ? "active" : "" %>">
            <i class="fa-solid fa-ticket customer-nav-icon"></i>
            <span>Voucher</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-redeem-voucher" class="customer-nav-item <%= isRedeem ? "active" : "" %>">
            <i class="fa-solid fa-gift customer-nav-icon"></i>
            <span>Đổi voucher</span>
        </a>
        <a href="${pageContext.request.contextPath}/customer-offers" class="customer-nav-item <%= isOffers ? "active" : "" %>">
            <i class="fa-regular fa-calendar-check customer-nav-icon"></i>
            <span>Ưu đãi</span>
        </a>
    </nav>

    <div class="px-10 pb-8">
        <div class="text-[11px] font-bold text-[#6B778C] truncate"><%= fullName %></div>
    </div>
</aside>
