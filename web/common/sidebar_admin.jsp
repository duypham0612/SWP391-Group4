<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%
    String uri = request.getRequestURI();

    // Active states
    boolean isDashboard   = uri.contains("admin-dashboard");
    boolean isBranches    = uri.contains("admin-branches");
    boolean isAccounts    = uri.contains("admin-accounts");
    boolean isRoles       = uri.contains("admin-roles");
    boolean isMenu        = uri.contains("admin-menu");
    boolean isSyncMenu    = uri.contains("admin-sync-menu");
    boolean isStaff       = uri.contains("admin-staff");
    boolean isRevenue     = uri.contains("admin-revenue");
    boolean isReport      = uri.contains("admin-report");
    boolean isOrders      = uri.contains("admin-orders");
    boolean isVoucher     = uri.contains("admin-voucher");
    boolean isCustomers   = uri.contains("admin-customers");
    boolean isExport      = uri.contains("admin-export");
    boolean isActivityLog = uri.contains("admin-activity-log");
    boolean isSettings    = uri.contains("admin-settings");

    // Group active states
    boolean isGroupBranch  = isBranches;
    boolean isGroupUser    = isAccounts || isRoles || isStaff || isCustomers;
    boolean isGroupMenu    = isMenu || isSyncMenu;
    boolean isGroupRevenue = isDashboard || isRevenue || isReport || isOrders || isExport;
    boolean isGroupSystem  = isVoucher || isSettings || isActivityLog;

    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null) ? loggedInUser.getFullName() : "System Admin";
    String[] parts = fullName.trim().split(" ");
    String initials = parts.length >= 2
        ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))
        : fullName.substring(0, Math.min(2, fullName.length()));
%>

<style>
    .sidebar-admin { width: 260px; min-width: 260px; }
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
    .nav-item.active { background: linear-gradient(135deg,#e0f2fe,#f0fdf4); color: #0369a1; font-weight: 700; }
    .nav-item.active .nav-icon { color: #0284c7; }
    .nav-item .nav-icon { width: 16px; text-align: center; font-size: 13px; color: #94a3b8; flex-shrink: 0; }
    .nav-item .nav-badge {
        margin-left: auto; font-size: 9px; font-weight: 800;
        padding: 1px 6px; border-radius: 99px;
        background: #fee2e2; color: #dc2626;
    }
    .sidebar-divider { height: 1px; background: #e2e8f0; margin: 8px 4px; }
    .admin-badge {
        font-size: 9px; font-weight: 800; padding: 2px 8px;
        border-radius: 99px; background: linear-gradient(135deg,#7c3aed,#4f46e5);
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
        background: linear-gradient(135deg,#7c3aed,#4f46e5);
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

<aside class="sidebar-admin bg-slate-50 border-r border-slate-200 flex flex-col h-full z-20" id="adminSidebar">

    <%-- Logo --%>
    <div style="height:72px;display:flex;align-items:center;gap:12px;padding:0 20px;border-bottom:1px solid #e2e8f0">
        <div style="width:38px;height:38px;border-radius:12px;background:linear-gradient(135deg,#7c3aed,#4f46e5);display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(124,58,237,.3)">
            <i class="fa-solid fa-mug-hot" style="color:#fff;font-size:15px"></i>
        </div>
        <div>
            <div style="font-size:13px;font-weight:800;color:#1e293b;line-height:1.2">Coffee Admin</div>
            <div style="display:flex;align-items:center;gap:5px;margin-top:3px">
                <span class="admin-badge">System Admin</span>
            </div>
        </div>
    </div>

    <%-- Nav --%>
    <nav style="flex:1;overflow-y:auto;padding:12px 12px 0">

        <%-- Dashboard riêng --%>
        <a href="${pageContext.request.contextPath}/admin-dashboard"
           class="nav-item <%= isDashboard ? "active" : "" %>" id="nav-dashboard">
            <i class="fa-solid fa-gauge-high nav-icon"></i>
            <span>Dashboard Tổng hệ thống</span>
        </a>

        <div class="sidebar-divider"></div>

        <%-- 1. Chi nhánh --%>
        <div class="nav-group-header <%= isGroupBranch ? "" : "" %>" onclick="toggleGroup('grp-branch',this)" id="hdr-branch">
            <span><i class="fa-solid fa-store" style="margin-right:6px"></i>Chi nhánh</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-branch" style="max-height:200px">
            <a href="${pageContext.request.contextPath}/admin-branches"
               class="nav-item <%= isBranches ? "active" : "" %>">
                <i class="fa-solid fa-code-branch nav-icon"></i>
                <span>Quản lý chi nhánh</span>
            </a>
        </div>

        <%-- 2. Người dùng & Phân quyền --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-user',this)" id="hdr-user">
            <span><i class="fa-solid fa-users-gear" style="margin-right:6px"></i>Người dùng</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-user" style="max-height:300px">
            <a href="${pageContext.request.contextPath}/admin-accounts"
               class="nav-item <%= isAccounts ? "active" : "" %>">
                <i class="fa-solid fa-user-pen nav-icon"></i>
                <span>Tạo / Sửa / Xóa tài khoản</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-roles"
               class="nav-item <%= isRoles ? "active" : "" %>">
                <i class="fa-solid fa-shield-halved nav-icon"></i>
                <span>Phân quyền Role</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-staff"
               class="nav-item <%= isStaff ? "active" : "" %>">
                <i class="fa-solid fa-id-badge nav-icon"></i>
                <span>Quản lý nhân viên</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-customers"
               class="nav-item <%= isCustomers ? "active" : "" %>">
                <i class="fa-solid fa-users nav-icon"></i>
                <span>Quản lý khách hàng</span>
            </a>
        </div>

        <%-- 3. Menu --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-menu',this)" id="hdr-menu">
            <span><i class="fa-solid fa-utensils" style="margin-right:6px"></i>Menu</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-menu" style="max-height:200px">
            <a href="${pageContext.request.contextPath}/admin-menu"
               class="nav-item <%= isMenu ? "active" : "" %>">
                <i class="fa-solid fa-book-open nav-icon"></i>
                <span>Quản lý menu toàn hệ thống</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-sync-menu"
               class="nav-item <%= isSyncMenu ? "active" : "" %>">
                <i class="fa-solid fa-arrows-rotate nav-icon"></i>
                <span>Đồng bộ menu chi nhánh</span>
            </a>
        </div>

        <%-- 4. Doanh thu & Báo cáo --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-revenue',this)" id="hdr-revenue">
            <span><i class="fa-solid fa-chart-line" style="margin-right:6px"></i>Doanh thu & Báo cáo</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-revenue" style="max-height:400px">
            <a href="${pageContext.request.contextPath}/admin-revenue"
               class="nav-item <%= isRevenue ? "active" : "" %>">
                <i class="fa-solid fa-sack-dollar nav-icon"></i>
                <span>Xem doanh thu toàn hệ thống</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-report"
               class="nav-item <%= isReport ? "active" : "" %>">
                <i class="fa-solid fa-chart-column nav-icon"></i>
                <span>Xem báo cáo chi nhánh</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-orders"
               class="nav-item <%= isOrders ? "active" : "" %>">
                <i class="fa-solid fa-receipt nav-icon"></i>
                <span>Theo dõi order realtime</span>
                <span class="nav-badge">Live</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-export"
               class="nav-item <%= isExport ? "active" : "" %>">
                <i class="fa-solid fa-file-export nav-icon"></i>
                <span>Export báo cáo</span>
            </a>
        </div>

        <%-- 5. Hệ thống --%>
        <div class="nav-group-header" onclick="toggleGroup('grp-system',this)" id="hdr-system">
            <span><i class="fa-solid fa-sliders" style="margin-right:6px"></i>Hệ thống</span>
            <i class="fa-solid fa-chevron-down chevron"></i>
        </div>
        <div class="nav-group-body" id="grp-system" style="max-height:300px">
            <a href="${pageContext.request.contextPath}/admin-voucher"
               class="nav-item <%= isVoucher ? "active" : "" %>">
                <i class="fa-solid fa-ticket nav-icon"></i>
                <span>Quản lý voucher</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-settings"
               class="nav-item <%= isSettings ? "active" : "" %>">
                <i class="fa-solid fa-gear nav-icon"></i>
                <span>Cấu hình hệ thống</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin-activity-log"
               class="nav-item <%= isActivityLog ? "active" : "" %>">
                <i class="fa-solid fa-clock-rotate-left nav-icon"></i>
                <span>Lịch sử hoạt động</span>
            </a>
        </div>

    </nav>

    <%-- User card & logout --%>
    <div style="padding:12px 12px 16px;border-top:1px solid #e2e8f0">
        <div class="user-card">
            <div class="user-avatar"><%= initials.toUpperCase() %></div>
            <div style="min-width:0;flex:1">
                <div style="font-size:12px;font-weight:700;color:#1e293b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"><%= fullName %></div>
                <div style="font-size:10px;color:#94a3b8;font-weight:600;margin-top:1px">System Admin</div>
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
        const groups = ['grp-branch','grp-user','grp-menu','grp-revenue','grp-system'];
        const activeLinks = document.querySelectorAll('#adminSidebar .nav-item.active');
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
