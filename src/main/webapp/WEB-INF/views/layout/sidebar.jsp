<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="u" value="${sessionScope.authUser}" />
<%-- Đường dẫn gốc (trước forward) để tô sáng menu đang mở --%>
<c:set var="curPath" value="${empty requestScope['jakarta.servlet.forward.request_uri'] ? pageContext.request.requestURI : requestScope['jakarta.servlet.forward.request_uri']}" />

<%-- Sprite icon (Lucide-style, stroke) — định nghĩa 1 lần, dùng lại bằng <use> --%>
<svg style="display:none" aria-hidden="true"><defs>
  <symbol id="ic-dash" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></symbol>
  <symbol id="ic-home" viewBox="0 0 24 24"><path d="M4 11l8-7 8 7"/><path d="M6 10v9h12v-9"/><path d="M10 19v-5h4v5"/></symbol>
  <symbol id="ic-users" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.2"/><path d="M3.5 20a5.5 5.5 0 0 1 11 0"/><path d="M16 5.2a3.2 3.2 0 0 1 0 5.6M17 20a5.5 5.5 0 0 0-3-4.9"/></symbol>
  <symbol id="ic-store" viewBox="0 0 24 24"><path d="M4 9.5 5 4h14l1 5.5"/><path d="M4 9.5h16v0a3 3 0 0 1-6 0 3 3 0 0 1-6 0 3 3 0 0 1-4 0z"/><path d="M5.5 11.5V20h13v-8.5"/><path d="M10 20v-4h4v4"/></symbol>
  <symbol id="ic-tag" viewBox="0 0 24 24"><path d="M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9z"/><circle cx="7.5" cy="7.5" r="1.4"/></symbol>
  <symbol id="ic-coffee" viewBox="0 0 24 24"><path d="M5 9h12v4a5 5 0 0 1-5 5H10a5 5 0 0 1-5-5z"/><path d="M17 10h2a2.5 2.5 0 0 1 0 5h-2"/><path d="M8 3c-.5 1 .5 1.5 0 3M12 3c-.5 1 .5 1.5 0 3"/></symbol>
  <symbol id="ic-leaf" viewBox="0 0 24 24"><path d="M5 19C5 10 11 4 20 4c0 9-6 15-15 15z"/><path d="M5 19c3-5 7-7 11-8"/></symbol>
  <symbol id="ic-book" viewBox="0 0 24 24"><path d="M5 4h11a2 2 0 0 1 2 2v14H7a2 2 0 0 0-2 2z"/><path d="M5 4v16"/><path d="M9 8h6M9 12h6"/></symbol>
  <symbol id="ic-sliders" viewBox="0 0 24 24"><path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/></symbol>
  <symbol id="ic-menu" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16"/></symbol>
  <symbol id="ic-ticket" viewBox="0 0 24 24"><path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2 2 2 0 0 0 0 4 2 2 0 0 1-2 2H5a2 2 0 0 1-2-2 2 2 0 0 0 0-4z"/><path d="M14 6v12" stroke-dasharray="2 2"/></symbol>
  <symbol id="ic-chart" viewBox="0 0 24 24"><path d="M4 20V4"/><path d="M4 20h16"/><rect x="7" y="12" width="3" height="5"/><rect x="12" y="8" width="3" height="9"/><rect x="17" y="5" width="3" height="12"/></symbol>
  <symbol id="ic-box" viewBox="0 0 24 24"><path d="M3.5 7 12 3l8.5 4-8.5 4z"/><path d="M3.5 7v10L12 21l8.5-4V7"/><path d="M12 11v10"/></symbol>
  <symbol id="ic-truck" viewBox="0 0 24 24"><rect x="2" y="7" width="12" height="9" rx="1"/><path d="M14 10h4l3 3v3h-7z"/><circle cx="7" cy="18" r="1.6"/><circle cx="17" cy="18" r="1.6"/></symbol>
  <symbol id="ic-scale" viewBox="0 0 24 24"><path d="M12 4v16M7 20h10"/><path d="M5 9 3 14h4zM19 9l-2 5h4z"/><path d="M5 9l7-3 7 3"/></symbol>
  <symbol id="ic-calendar" viewBox="0 0 24 24"><rect x="3.5" y="5" width="17" height="16" rx="2"/><path d="M3.5 9h17M8 3v4M16 3v4"/></symbol>
  <symbol id="ic-clock" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></symbol>
  <symbol id="ic-wallet" viewBox="0 0 24 24"><path d="M4 7a2 2 0 0 1 2-2h11v4"/><rect x="4" y="7" width="17" height="12" rx="2"/><circle cx="16.5" cy="13" r="1.3"/></symbol>
  <symbol id="ic-grid" viewBox="0 0 24 24"><rect x="3.5" y="3.5" width="7" height="7" rx="1"/><rect x="13.5" y="3.5" width="7" height="7" rx="1"/><rect x="3.5" y="13.5" width="7" height="7" rx="1"/><rect x="13.5" y="13.5" width="7" height="7" rx="1"/></symbol>
  <symbol id="ic-cart" viewBox="0 0 24 24"><path d="M3 4h2l2.2 11h10l2-8H6.5"/><circle cx="9" cy="19" r="1.5"/><circle cx="17" cy="19" r="1.5"/></symbol>
  <symbol id="ic-inbox" viewBox="0 0 24 24"><path d="M3.5 13 6 5h12l2.5 8"/><path d="M3.5 13v6h17v-6h-5a3 3 0 0 1-7 0z"/></symbol>
  <symbol id="ic-card" viewBox="0 0 24 24"><rect x="3" y="5.5" width="18" height="13" rx="2"/><path d="M3 9.5h18M6.5 14.5h4"/></symbol>
  <symbol id="ic-receipt" viewBox="0 0 24 24"><path d="M5 3h14v18l-2.5-1.5L14 21l-2-1.5L10 21l-2.5-1.5L5 21z"/><path d="M9 8h6M9 12h6"/></symbol>
  <symbol id="ic-flame" viewBox="0 0 24 24"><path d="M12 3c1 4 5 5 5 9a5 5 0 0 1-10 0c0-2 1-3 2-4 .5 1.5 1.5 2 2 2 0-2-1-4-1-7z"/></symbol>
  <symbol id="ic-bell" viewBox="0 0 24 24"><path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20a2 2 0 0 0 4 0"/></symbol>
  <symbol id="ic-beaker" viewBox="0 0 24 24"><path d="M9 3v6l-4 9a1.5 1.5 0 0 0 1.4 2.1h11.2A1.5 1.5 0 0 0 19 18l-4-9V3"/><path d="M8 3h8M6.5 14h11"/></symbol>
  <symbol id="ic-trash" viewBox="0 0 24 24"><path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"/></symbol>
  <symbol id="ic-ban" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5"/><path d="M6 6l12 12"/></symbol>
  <symbol id="ic-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5"/><path d="M16 16l4 4"/></symbol>
  <symbol id="ic-clipboard" viewBox="0 0 24 24"><rect x="5" y="5" width="14" height="16" rx="2"/><path d="M9 5V3.5h6V5"/><path d="M9 11h6M9 15h4"/></symbol>
</defs></svg>

<aside class="sidebar">
    <div class="sidebar-brand">
        <span class="logo">C</span>
        <span class="brand-text">
            <span class="brand-name">Cà Phê Chain</span>
            <span class="brand-sub">
                <c:choose>
                    <c:when test="${u.roleCode == 'ADMIN'}">Quản trị hệ thống</c:when>
                    <c:when test="${u.roleCode == 'BRANCH_MANAGER'}">Quản lý chi nhánh</c:when>
                    <c:when test="${u.roleCode == 'CASHIER'}">Thu ngân</c:when>
                    <c:when test="${u.roleCode == 'BARISTA'}">Pha chế</c:when>
                    <c:otherwise>Hệ thống</c:otherwise>
                </c:choose>
            </span>
        </span>
    </div>
    <ul class="nav">
        <li class="nav-section">Tổng quan</li>
        <li><a class="${curPath == ctx.concat('/dashboard') ? 'active' : ''}" href="${ctx}/dashboard"><svg class="ic"><use href="#ic-dash"/></svg>Bảng điều khiển</a></li>

        <c:choose>
            <c:when test="${u.roleCode == 'ADMIN'}">
                <li class="nav-section">Tổ chức</li>
                <li><a class="${curPath == ctx.concat('/admin/user') ? 'active' : ''}" href="${ctx}/admin/user"><svg class="ic"><use href="#ic-users"/></svg>Nhân sự</a></li>
                <li><a class="${curPath == ctx.concat('/admin/branch') ? 'active' : ''}" href="${ctx}/admin/branch"><svg class="ic"><use href="#ic-store"/></svg>Chi nhánh</a></li>
                <li class="nav-section">Thực đơn &amp; công thức</li>
                <li><a class="${curPath == ctx.concat('/admin/category') ? 'active' : ''}" href="${ctx}/admin/category"><svg class="ic"><use href="#ic-tag"/></svg>Danh mục</a></li>
                <li><a class="${curPath == ctx.concat('/admin/product') ? 'active' : ''}" href="${ctx}/admin/product"><svg class="ic"><use href="#ic-coffee"/></svg>Sản phẩm</a></li>
                <li><a class="${curPath == ctx.concat('/admin/ingredient') ? 'active' : ''}" href="${ctx}/admin/ingredient"><svg class="ic"><use href="#ic-leaf"/></svg>Nguyên liệu</a></li>
                <li><a class="${curPath == ctx.concat('/admin/recipe') ? 'active' : ''}" href="${ctx}/admin/recipe"><svg class="ic"><use href="#ic-book"/></svg>Công thức</a></li>
                <li><a class="${curPath == ctx.concat('/admin/modifier') ? 'active' : ''}" href="${ctx}/admin/modifier"><svg class="ic"><use href="#ic-sliders"/></svg>Tuỳ chọn (Modifier)</a></li>
                <li><a class="${curPath == ctx.concat('/admin/branch-menu') ? 'active' : ''}" href="${ctx}/admin/branch-menu"><svg class="ic"><use href="#ic-menu"/></svg>Menu chi nhánh</a></li>
                <li class="nav-section">Trang công khai</li>
                <li><a class="${curPath == ctx.concat('/admin/home') ? 'active' : ''}" href="${ctx}/admin/home"><svg class="ic"><use href="#ic-home"/></svg>Trang Home</a></li>
                <li class="nav-section">Khuyến mãi</li>
                <li><a class="${curPath == ctx.concat('/admin/voucher') ? 'active' : ''}" href="${ctx}/admin/voucher"><svg class="ic"><use href="#ic-ticket"/></svg>Voucher</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'BRANCH_MANAGER'}">
                <li class="nav-section">Kho</li>
                <li><a class="${curPath == ctx.concat('/manager/inventory') ? 'active' : ''}" href="${ctx}/manager/inventory"><svg class="ic"><use href="#ic-box"/></svg>Tồn kho &amp; cảnh báo</a></li>
                <li><a class="${curPath == ctx.concat('/manager/receipt') ? 'active' : ''}" href="${ctx}/manager/receipt"><svg class="ic"><use href="#ic-truck"/></svg>Nhập kho</a></li>
                <li><a class="${curPath == ctx.concat('/manager/supplier') ? 'active' : ''}" href="${ctx}/manager/supplier"><svg class="ic"><use href="#ic-store"/></svg>Nhà cung cấp</a></li>
                <li><a class="${curPath == ctx.concat('/manager/reconciliation') ? 'active' : ''}" href="${ctx}/manager/reconciliation"><svg class="ic"><use href="#ic-scale"/></svg>Đối soát tồn</a></li>
                <li class="nav-section">Nhân sự</li>
                <li><a class="${curPath == ctx.concat('/manager/shift') ? 'active' : ''}" href="${ctx}/manager/shift"><svg class="ic"><use href="#ic-calendar"/></svg>Ca làm</a></li>
                <li><a class="${curPath == ctx.concat('/manager/attendance') ? 'active' : ''}" href="${ctx}/manager/attendance"><svg class="ic"><use href="#ic-clock"/></svg>Chấm công</a></li>
                <li><a class="${curPath == ctx.concat('/manager/payroll') ? 'active' : ''}" href="${ctx}/manager/payroll"><svg class="ic"><use href="#ic-wallet"/></svg>Bảng lương</a></li>
                <li class="nav-section">Thực đơn</li>
                <li><a class="${curPath == ctx.concat('/manager/menu') ? 'active' : ''}" href="${ctx}/manager/menu"><svg class="ic"><use href="#ic-menu"/></svg>Menu chi nhánh</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'CASHIER'}">
                <c:set var="cashierReady" value="${requestScope.cashierOnDuty}" />
                <c:set var="cashierLockTitle" value="Cần bắt đầu ca trước khi thao tác" />
                <li class="nav-section">Bán hàng</li>
                <li><a class="${curPath == ctx.concat('/cashier/table') ? 'active' : ''}" href="${ctx}/cashier/table"><svg class="ic"><use href="#ic-grid"/></svg>Sơ đồ bàn</a></li>
                <li><a class="${curPath == ctx.concat('/cashier/pos') ? 'active' : ''} ${cashierReady ? '' : 'nav-disabled'}" href="${cashierReady ? ctx.concat('/cashier/pos') : ctx.concat('/cashier/shift')}" title="${cashierReady ? '' : cashierLockTitle}"><svg class="ic"><use href="#ic-cart"/></svg>POS / Đặt món</a></li>
                <li><a class="${curPath == ctx.concat('/cashier/inbox') ? 'active' : ''}" href="${ctx}/cashier/inbox"><svg class="ic"><use href="#ic-inbox"/></svg>Đơn đến (Inbox)</a></li>
                <li><a class="${curPath == ctx.concat('/cashier/handoff') ? 'active' : ''}" href="${ctx}/cashier/handoff"><svg class="ic"><use href="#ic-bell"/></svg>Sẵn sàng bàn giao</a></li>
                <li class="nav-section">Thu ngân</li>
                <li><a class="${curPath == ctx.concat('/cashier/shift') ? 'active' : ''}" href="${ctx}/cashier/shift"><svg class="ic"><use href="#ic-clock"/></svg>Ca thu ngân</a></li>
                <li><a class="${curPath == ctx.concat('/cashier/checkout') ? 'active' : ''} ${cashierReady ? '' : 'nav-disabled'}" href="${cashierReady ? ctx.concat('/cashier/checkout') : ctx.concat('/cashier/shift')}" title="${cashierReady ? '' : cashierLockTitle}"><svg class="ic"><use href="#ic-card"/></svg>Thanh toán</a></li>
                <li><a class="${curPath == ctx.concat('/cashier/history') ? 'active' : ''}" href="${ctx}/cashier/history"><svg class="ic"><use href="#ic-receipt"/></svg>Lịch sử hoá đơn</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'BARISTA'}">
                <li class="nav-section">Pha chế</li>
                <li><a class="${curPath == ctx.concat('/barista/kds') ? 'active' : ''}" href="${ctx}/barista/kds"><svg class="ic"><use href="#ic-flame"/></svg>Quầy pha chế</a></li>
                <li><a class="${curPath == ctx.concat('/barista/prep') ? 'active' : ''}" href="${ctx}/barista/prep"><svg class="ic"><use href="#ic-beaker"/></svg>Pha sẵn nguyên liệu</a></li>
                <li><a class="${curPath == ctx.concat('/barista/waste') ? 'active' : ''}" href="${ctx}/barista/waste"><svg class="ic"><use href="#ic-trash"/></svg>Hao hụt & Làm lại</a></li>
                <li><a class="${curPath == ctx.concat('/barista/eightysix') ? 'active' : ''}" href="${ctx}/barista/eightysix"><svg class="ic"><use href="#ic-ban"/></svg>Báo hết món</a></li>
                <li><a class="${curPath == ctx.concat('/barista/recipe') ? 'active' : ''}" href="${ctx}/barista/recipe"><svg class="ic"><use href="#ic-search"/></svg>Tra cứu công thức</a></li>
                <li><a class="${curPath == ctx.concat('/barista/handover') ? 'active' : ''}" href="${ctx}/barista/handover"><svg class="ic"><use href="#ic-clipboard"/></svg>Ca làm & Bàn giao</a></li>
            </c:when>
        </c:choose>
    </ul>
    <div class="sidebar-foot">SWP391 · phiên bản 0.3</div>
</aside>
