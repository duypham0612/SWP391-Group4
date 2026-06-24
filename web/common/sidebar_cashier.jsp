<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%
    String uri = request.getRequestURI();

    // Active states
    // Bán Hàng & Order
    boolean isTableLayout = uri.contains("table-layout");
    boolean isCreateOrder = uri.contains("pos") && !uri.contains("takeaway");
    boolean isTakeaway    = uri.contains("pos") && uri.contains("takeaway");
    boolean isOnlineOrder = uri.contains("online-orders");
    boolean isItemStatus  = uri.contains("kitchen-status");

    // Hóa Đơn & Thanh Toán
    boolean isEditOrder   = uri.contains("pos-edit");
    boolean isMergeOrder  = uri.contains("pos-merge");
    boolean isSplitBill   = uri.contains("pos-split");
    boolean isPayment     = uri.contains("pos-payment");
    boolean isPrintBill   = uri.contains("pos-print");
    boolean isVoucher     = uri.contains("pos-voucher");

    // Khách Hàng
    boolean isCreateCustomer = uri.contains("customer-create");
    boolean isOrderHistory   = uri.contains("customer-history");

    // Quản Lý Kho Cơ Bản
    boolean isImportStock = uri.contains("inventory-import");
    boolean isUpdateStock = uri.contains("inventory-update");
    boolean isLowStock    = uri.contains("inventory-low-stock");

    // Groups
    boolean isGroupSales    = isTableLayout || isCreateOrder || isTakeaway || isOnlineOrder || isItemStatus;
    boolean isGroupBilling  = isEditOrder || isMergeOrder || isSplitBill || isPayment || isPrintBill || isVoucher;
    boolean isGroupCustomer = isCreateCustomer || isOrderHistory;
    boolean isGroupInventory= isImportStock || isUpdateStock || isLowStock;

    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null && loggedInUser.getFullName() != null && !loggedInUser.getFullName().isEmpty()) 
                        ? loggedInUser.getFullName() : "Nhân Viên Thu Ngân";
    String[] parts = fullName.trim().split(" ");
    String initials = parts.length >= 2
        ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))
        : fullName.substring(0, Math.min(2, fullName.length()));
%>

<style>
    .sidebar-cashier { width: 260px; min-width: 260px; }
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
    .nav-item.active { background: linear-gradient(135deg,#e0e7ff,#dbeafe); color: #4338ca; font-weight: 700; }
    .nav-item.active .nav-icon { color: #4f46e5; }
    .nav-item .nav-icon { width: 16px; text-align: center; font-size: 13px; color: #94a3b8; flex-shrink: 0; }
    
    .cashier-badge {
        font-size: 9px; font-weight: 800; padding: 2px 8px;
        border-radius: 99px; background: linear-gradient(135deg,#6366f1,#8b5cf6);
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
        background: linear-gradient(135deg,#6366f1,#8b5cf6);
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

<aside class="sidebar-cashier bg-slate-50 border-r border-slate-200 flex flex-col h-full z-20" id="cashierSidebar">

    <%-- Logo --%>
    <div style="height:72px;display:flex;align-items:center;gap:12px;padding:0 20px;border-bottom:1px solid #e2e8f0">
        <div style="width:38px;height:38px;border-radius:12px;background:linear-gradient(135deg,#6366f1,#8b5cf6);display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(99,102,241,.3)">
            <i class="fa-solid fa-cash-register" style="color:#fff;font-size:15px"></i>
        </div>
        <div>
            <div style="font-size:13px;font-weight:800;color:#1e293b;line-height:1.2">Thu Ngân & Kho</div>
            <div style="display:flex;align-items:center;gap:5px;margin-top:3px">
                <span class="cashier-badge">Staff</span>
            </div>
        </div>
    </div>

    <%-- Nav --%>
    <nav style="flex:1;overflow-y:auto;padding:12px 12px 0">

        <%-- 1. Bán Hàng & Order --%>
        <div class="nav-group-header <%= isGroupSales ? "" : "" %>" onclick="toggleGroup('grp-sales',this)" id="hdr-sales">
            <span><i class="fa-solid fa-store" style="margin-right:6px"></i>Bán Hàng & Order</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-sales" style="max-height:300px">
            <a href="${pageContext.request.contextPath}/pos-tables"
               class="nav-item <%= isTableLayout ? "active" : "" %>">
                <i class="fa-solid fa-chair nav-icon"></i>
                <span>Chọn bàn</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos"
               class="nav-item <%= isCreateOrder ? "active" : "" %>">
                <i class="fa-solid fa-plus-circle nav-icon"></i>
                <span>Tạo order</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos?type=takeaway"
               class="nav-item <%= isTakeaway ? "active" : "" %>">
                <i class="fa-solid fa-bag-shopping nav-icon"></i>
                <span>Order mang về</span>
            </a>
            <a href="${pageContext.request.contextPath}/online-orders"
               class="nav-item <%= isOnlineOrder ? "active" : "" %>">
                <i class="fa-solid fa-globe nav-icon"></i>
                <span>Order online</span>
            </a>
            <a href="${pageContext.request.contextPath}/kitchen-status"
               class="nav-item <%= isItemStatus ? "active" : "" %>">
                <i class="fa-solid fa-bell-concierge nav-icon"></i>
                <span>Theo dõi trạng thái món</span>
            </a>
        </div>

        <%-- 2. Hóa Đơn & Thanh Toán --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-billing',this)" id="hdr-billing">
            <span><i class="fa-solid fa-file-invoice-dollar" style="margin-right:6px"></i>Hóa Đơn & Thanh Toán</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-billing" style="max-height:300px">
            <a href="${pageContext.request.contextPath}/pos-edit"
               class="nav-item <%= isEditOrder ? "active" : "" %>">
                <i class="fa-solid fa-pen-to-square nav-icon"></i>
                <span>Chỉnh sửa order</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos-merge"
               class="nav-item <%= isMergeOrder ? "active" : "" %>">
                <i class="fa-solid fa-object-group nav-icon"></i>
                <span>Merge order (Gộp bàn)</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos-split"
               class="nav-item <%= isSplitBill ? "active" : "" %>">
                <i class="fa-solid fa-arrows-split-up-and-left nav-icon"></i>
                <span>Split bill (Tách bill)</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos-voucher"
               class="nav-item <%= isVoucher ? "active" : "" %>">
                <i class="fa-solid fa-ticket-simple nav-icon"></i>
                <span>Áp voucher</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos-payment"
               class="nav-item <%= isPayment ? "active" : "" %>">
                <i class="fa-solid fa-credit-card nav-icon"></i>
                <span>Thanh toán</span>
            </a>
            <a href="${pageContext.request.contextPath}/pos-print"
               class="nav-item <%= isPrintBill ? "active" : "" %>">
                <i class="fa-solid fa-print nav-icon"></i>
                <span>In hóa đơn</span>
            </a>
        </div>

        <%-- 3. Khách Hàng --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-customer',this)" id="hdr-customer">
            <span><i class="fa-solid fa-users" style="margin-right:6px"></i>Khách Hàng</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-customer" style="max-height:200px">
            <a href="${pageContext.request.contextPath}/customer-create"
               class="nav-item <%= isCreateCustomer ? "active" : "" %>">
                <i class="fa-solid fa-user-plus nav-icon"></i>
                <span>Tạo khách thành viên</span>
            </a>
            <a href="${pageContext.request.contextPath}/customer-history"
               class="nav-item <%= isOrderHistory ? "active" : "" %>">
                <i class="fa-solid fa-clock-rotate-left nav-icon"></i>
                <span>Xem lịch sử order</span>
            </a>
        </div>

        <%-- 4. Quản Lý Kho Cơ Bản --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-inventory',this)" id="hdr-inventory">
            <span><i class="fa-solid fa-boxes-stacked" style="margin-right:6px"></i>Quản Lý Kho Cơ Bản</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-inventory" style="max-height:200px">
            <a href="${pageContext.request.contextPath}/inventory-import"
               class="nav-item <%= isImportStock ? "active" : "" %>">
                <i class="fa-solid fa-box-open nav-icon"></i>
                <span>Nhập nguyên liệu</span>
            </a>
            <a href="${pageContext.request.contextPath}/inventory-update"
               class="nav-item <%= isUpdateStock ? "active" : "" %>">
                <i class="fa-solid fa-rotate nav-icon"></i>
                <span>Cập nhật tồn kho</span>
            </a>
            <a href="${pageContext.request.contextPath}/inventory-low-stock"
               class="nav-item <%= isLowStock ? "active" : "" %>">
                <i class="fa-solid fa-triangle-exclamation nav-icon"></i>
                <span>Theo dõi sắp hết</span>
            </a>
        </div>

    </nav>

    <%-- User card & logout --%>
    <div style="padding:12px 12px 16px;border-top:1px solid #e2e8f0">
        <div class="user-card">
            <div class="user-avatar"><%= initials.toUpperCase() %></div>
            <div style="min-width:0;flex:1">
                <div style="font-size:12px;font-weight:700;color:#1e293b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"><%= fullName %></div>
                <div style="font-size:10px;color:#94a3b8;font-weight:600;margin-top:1px">Thu Ngân</div>
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
        const groups = ['grp-sales','grp-billing','grp-customer','grp-inventory'];
        const activeLinks = document.querySelectorAll('#cashierSidebar .nav-item.active');
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
