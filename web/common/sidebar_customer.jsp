<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Customer"%>
<%
    String uri = request.getRequestURI();

    // Active states
    boolean isMenu        = uri.contains("customer-menu") || uri.contains("menu");
    boolean isQrOrder     = uri.contains("customer-qr-order");
    boolean isOrderStatus = uri.contains("customer-order-status");
    boolean isReview      = uri.contains("customer-review");
    
    boolean isHistory     = uri.contains("customer-purchase-history");
    boolean isPoints      = uri.contains("customer-points") || uri.contains("customer-track-points");
    boolean isRedeem      = uri.contains("customer-redeem-voucher");
    boolean isOffers      = uri.contains("customer-offers");
    boolean isMyVouchers  = uri.contains("customer-my-vouchers");

    // Group active states
    boolean isGroupShopping = isMenu || isQrOrder || isOrderStatus || isReview;
    boolean isGroupMember   = isHistory || isPoints || isRedeem || isOffers || isMyVouchers;

    User loggedInUser = (User) session.getAttribute("user");
    Customer customerInfo = (Customer) request.getAttribute("customerInfo");

    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null && !loggedInUser.getFullName().isEmpty()) 
                        ? loggedInUser.getFullName() : "Khách Hàng";
    String memberRank = (customerInfo != null && customerInfo.getMemberRank() != null) ? customerInfo.getMemberRank() : "Hội Viên";
    String[] parts = fullName.trim().split(" ");
    String initials = parts.length >= 2
        ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))
        : fullName.substring(0, Math.min(2, fullName.length()));
%>

<style>
    .sidebar-customer { width: 260px; min-width: 260px; }
    .nav-group-header {
        display: flex; align-items: center; justify-content: space-between;
        padding: 6px 10px; margin: 4px 0 2px;
        font-size: 10px; font-weight: 800; letter-spacing: .08em;
        text-transform: uppercase; color: #94a3b8;
        cursor: pointer; border-radius: 8px;
        transition: background .15s;
        user-select: none;
    }
    .nav-group-header:hover { background: #f1f5f9; color: #64748b; }
    .nav-group-header .chevron { transition: transform .25s; font-size: 9px; }
    .nav-group-header.collapsed .chevron { transform: rotate(-90deg); }
    .nav-group-body { overflow: hidden; transition: max-height .3s ease; }
    .nav-group-body.collapsed { max-height: 0 !important; }
    .nav-item {
        display: flex; align-items: center; gap: 10px;
        padding: 9px 12px; border-radius: 10px;
        font-size: 12px; font-weight: 600;
        text-decoration: none; color: #64748b;
        transition: all .15s; margin-bottom: 2px;
    }
    .nav-item:hover { background: #f1f5f9; color: #1e293b; }
    .nav-item.active { background: linear-gradient(135deg,#fdf4ff,#fce7f3); color: #be185d; font-weight: 700; }
    .nav-item.active .nav-icon { color: #db2777; }
    .nav-item .nav-icon { width: 16px; text-align: center; font-size: 13px; color: #94a3b8; flex-shrink: 0; }
    .nav-item .nav-badge {
        margin-left: auto; font-size: 9px; font-weight: 800;
        padding: 1px 6px; border-radius: 99px;
        background: #fee2e2; color: #dc2626;
    }
    .sidebar-divider { height: 1px; background: #e2e8f0; margin: 8px 4px; }
    .customer-badge {
        font-size: 9px; font-weight: 800; padding: 2px 8px;
        border-radius: 99px; background: linear-gradient(135deg,#ec4899,#f43f5e);
        color: #fff; letter-spacing: .04em;
    }
    .user-card {
        background: #fff; border: 1px solid #e2e8f0;
        border-radius: 14px; padding: 10px 12px;
        display: flex; align-items: center; gap: 10px;
        box-shadow: 0 1px 4px rgba(0,0,0,.06);
    }
    .user-avatar {
        width: 36px; height: 36px; border-radius: 10px;
        background: linear-gradient(135deg,#ec4899,#f43f5e);
        color: #fff; display: flex; align-items: center;
        justify-content: center; font-size: 11px; font-weight: 800;
        flex-shrink: 0;
    }
    .logout-btn {
        display: flex; align-items: center; gap: 6px;
        padding: 7px 12px; border-radius: 10px;
        font-size: 11px; font-weight: 700; color: #dc2626;
        background: #fef2f2; border: 1px solid #fecaca;
        text-decoration: none; transition: all .15s; margin-top: 6px;
    }
    .logout-btn:hover { background: #fee2e2; }
</style>

<aside class="sidebar-customer bg-slate-50 border-r border-slate-200 flex flex-col h-full z-20" id="customerSidebar">

    <%-- Logo --%>
    <div style="height:72px;display:flex;align-items:center;gap:12px;padding:0 20px;border-bottom:1px solid #e2e8f0">
        <div style="width:38px;height:38px;border-radius:12px;background:linear-gradient(135deg,#ec4899,#f43f5e);display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(236,72,153,.3)">
            <i class="fa-solid fa-mug-hot" style="color:#fff;font-size:15px"></i>
        </div>
        <div>
            <div style="font-size:13px;font-weight:800;color:#1e293b;line-height:1.2">Coffee Shop</div>
            <div style="display:flex;align-items:center;gap:5px;margin-top:3px">
                <span class="customer-badge"><%= memberRank %></span>
            </div>
        </div>
    </div>

    <%-- Nav --%>
    <nav style="flex:1;overflow-y:auto;padding:12px 12px 0">

        <%-- 1. Mua sắm & Đặt hàng --%>
        <div class="nav-group-header <%= isGroupShopping ? "" : "" %>" onclick="toggleGroup('grp-shopping',this)" id="hdr-shopping">
            <span><i class="fa-solid fa-bag-shopping" style="margin-right:6px"></i>Mua sắm & Đặt hàng</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-shopping" style="max-height:250px">
            <a href="${pageContext.request.contextPath}/customer-menu"
               class="nav-item <%= isMenu ? "active" : "" %>">
                <i class="fa-solid fa-book-open nav-icon"></i>
                <span>Xem menu online</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-qr-order"
               class="nav-item <%= isQrOrder ? "active" : "" %>">
                <i class="fa-solid fa-qrcode nav-icon"></i>
                <span>Đặt món bằng QR</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-order-status"
               class="nav-item <%= isOrderStatus ? "active" : "" %>">
                <i class="fa-solid fa-motorcycle nav-icon"></i>
                <span>Theo dõi trạng thái order</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-review"
               class="nav-item <%= isReview ? "active" : "" %>">
                <i class="fa-solid fa-star nav-icon"></i>
                <span>Đánh giá sản phẩm</span>
            </a>
        </div>

        <%-- 2. Chức năng hội viên --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-member',this)" id="hdr-member">
            <span><i class="fa-solid fa-gem" style="margin-right:6px"></i>Chức năng hội viên</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-member" style="max-height:300px">
            <a href="${pageContext.request.contextPath}/customer-purchase-history"
               class="nav-item <%= isHistory ? "active" : "" %>">
                <i class="fa-solid fa-clock-rotate-left nav-icon"></i>
                <span>Lịch sử mua hàng</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-points"
               class="nav-item <%= isPoints ? "active" : "" %>">
                <i class="fa-solid fa-coins nav-icon"></i>
                <span>Tích / Theo dõi điểm thưởng</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-my-vouchers"
               class="nav-item <%= isMyVouchers ? "active" : "" %>">
                <i class="fa-solid fa-ticket-simple nav-icon"></i>
                <span>Xem voucher cá nhân</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-redeem-voucher"
               class="nav-item <%= isRedeem ? "active" : "" %>">
                <i class="fa-solid fa-gift nav-icon"></i>
                <span>Đổi voucher</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-offers"
               class="nav-item <%= isOffers ? "active" : "" %>">
                <i class="fa-solid fa-fire nav-icon"></i>
                <span>Nhận ưu đãi thành viên</span>
            </a>
        </div>

    </nav>

    <%-- User card & logout --%>
    <div style="padding:12px 12px 16px;border-top:1px solid #e2e8f0">
        <div class="user-card">
            <div class="user-avatar"><%= initials.toUpperCase() %></div>
            <div style="min-width:0;flex:1">
                <div style="font-size:12px;font-weight:700;color:#1e293b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"><%= fullName %></div>
                <div style="font-size:10px;color:#94a3b8;font-weight:600;margin-top:1px"><%= memberRank %></div>
            </div>
        </div>
        <a href="${pageContext.request.contextPath}/login?action=logout" class="logout-btn">
            <i class="fa-solid fa-right-from-bracket"></i>
            <span>Đăng xuất</span>
        </a>
    </div>
</aside>

<script>
    function toggleGroup(bodyId, header) {
        const body = document.getElementById(bodyId);
        const isCollapsed = body.classList.contains('collapsed');
        if (isCollapsed) {
            body.classList.remove('collapsed');
            header.classList.remove('collapsed');
        } else {
            body.classList.add('collapsed');
            header.classList.add('collapsed');
        }
    }

    // Auto-collapse groups that are not active on page load
    document.addEventListener('DOMContentLoaded', function () {
        const groups = ['grp-shopping','grp-member'];
        const activeLinks = document.querySelectorAll('#customerSidebar .nav-item.active');
        const activeGroupIds = new Set();
        activeLinks.forEach(link => {
            const body = link.closest('.nav-group-body');
            if (body) activeGroupIds.add(body.id);
        });
        groups.forEach(id => {
            if (!activeGroupIds.has(id)) {
                const body = document.getElementById(id);
                const header = document.getElementById('hdr-' + id.replace('grp-',''));
                if (body) body.classList.add('collapsed');
                if (header) header.classList.add('collapsed');
            }
        });
    });
</script>
