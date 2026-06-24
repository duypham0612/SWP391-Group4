<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Table"%>
<%@page import="java.util.List"%>
<%
    String currentUri = request.getRequestURI();
    boolean isManagerDashboard = currentUri.contains("manager-dashboard");
    boolean isInventory = currentUri.contains("manager-inventory");
    boolean isAttendance = currentUri.contains("manager-attendance");
    boolean isManagerTables = currentUri.contains("manager-tables");

    boolean isCashierDashboard = currentUri.contains("cashier-dashboard");
    boolean isCashierTables = currentUri.contains("cashier-tables");
    boolean isCashierOrders = currentUri.contains("cashier-orders");
    boolean isPosScreen = currentUri.contains("pos") && !currentUri.contains("pos-tables");

    User loggedInUser = (User) session.getAttribute("user");
    int roleId = (loggedInUser != null) ? loggedInUser.getRoleId() : 0;
    String fullName = (loggedInUser != null) ? loggedInUser.getFullName() : "Admin Manager";
    String shortName = "";
    if (fullName != null && !fullName.trim().isEmpty()) {
        String[] parts = fullName.split(" ");
        shortName = parts[parts.length - 1];
    } else {
        shortName = "AD";
    }
%>
<aside class="w-64 bg-slate-50 border-r border-slate-200/60 flex flex-col h-full z-20 shrink-0">
    <div class="h-20 flex items-center gap-3 px-6 border-b border-slate-200/60">
        <div class="w-9 h-9 rounded-xl bg-[#006064] flex items-center justify-center text-white shadow-md shadow-[#006064]/20">
            <i class="fa-solid fa-mug-hot text-base"></i>
        </div>
        <div>
            <h4 class="text-sm font-bold tracking-tight text-slate-800 leading-tight">Coffee POS</h4>
            <p class="text-[10px] text-slate-400 font-medium"><%= (roleId == 3) ? "Bán hàng & Thu ngân" : "Hệ thống quản lý" %></p>
        </div>
    </div>

    <nav class="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        <%-- MENU DÀNH CHO MANAGER / ADMIN --%>
        <% if (roleId <= 2) { %>
            <a href="${pageContext.request.contextPath}/manager-dashboard" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isManagerDashboard ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-grid-2-dash text-sm w-4 text-center <%= isManagerDashboard ? "text-sky-600" : "text-slate-400" %>"></i>
                <span>Tổng quan</span>
            </a>

            <a href="${pageContext.request.contextPath}/manager-tables" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isManagerTables ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-chair text-sm w-4 text-center <%= isManagerTables ? "text-sky-600" : "text-slate-400" %>"></i>
                <span>Quản lý bàn</span>
            </a>

            <a href="${pageContext.request.contextPath}/manager-inventory" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isInventory ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-box-open text-sm w-4 text-center <%= isInventory ? "text-sky-600" : "text-slate-400" %>"></i>
                <span>Kho nguyên liệu</span>
            </a>

            <a href="${pageContext.request.contextPath}/manager-attendance" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isAttendance ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-user-clock text-sm w-4 text-center <%= isAttendance ? "text-sky-600" : "text-slate-400" %>"></i>
                <span>Nhân viên</span>
            </a>
        <% } %>

        <%-- MENU DÀNH CHO THU NGÂN (Role = 3) --%>
        <% if (roleId == 3) { %>
            <a href="${pageContext.request.contextPath}/cashier-dashboard" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isCashierDashboard ? "bg-emerald-50 text-emerald-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-grid-2-dash text-sm w-4 text-center <%= isCashierDashboard ? "text-emerald-600" : "text-slate-400" %>"></i>
                <span>Tổng quan Thu ngân</span>
            </a>

            <!-- GỌI MÓN TRỰC TIẾP TẠI QUẦY (Mặc định Table 1 là Bán mang về) -->
            <a href="${pageContext.request.contextPath}/pos?action=open_table&tableId=1" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isPosScreen ? "bg-emerald-50 text-emerald-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-cash-register text-sm w-4 text-center <%= isPosScreen ? "text-emerald-600" : "text-slate-400" %>"></i>
                <span>Gọi món (POS)</span>
            </a>

            <!-- QUẢN LÝ BÀN SẢNH (Dùng Controller Cashier riêng) -->
            <a href="${pageContext.request.contextPath}/cashier-tables" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isCashierTables ? "bg-emerald-50 text-emerald-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-chair text-sm w-4 text-center <%= isCashierTables ? "text-emerald-600" : "text-slate-400" %>"></i>
                <span>Quản lý Sảnh & Đặt bàn</span>
            </a>

            <!-- QUẢN LÝ ĐƠN HÀNG TỔNG HỢP -->
            <a href="${pageContext.request.contextPath}/cashier-orders" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isCashierOrders ? "bg-emerald-50 text-emerald-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
                <i class="fa-solid fa-receipt text-sm w-4 text-center <%= isCashierOrders ? "text-emerald-600" : "text-slate-400" %>"></i>
                <span>Quản lý Đơn hàng</span>
            </a>
        <% } %>
    </nav>

    <div class="p-4 border-t border-slate-200/60 bg-slate-50/50">
        <div class="flex items-center gap-3 bg-white border border-slate-200/50 p-2.5 rounded-2xl shadow-sm">
            <div class="w-9 h-9 rounded-xl <%= (roleId==3) ? "bg-emerald-100 text-emerald-700" : "bg-sky-100 text-sky-700" %> flex items-center justify-center font-bold text-xs shadow-inner">
                <%= shortName.substring(0, Math.min(2, shortName.length())).toUpperCase() %>
            </div>
            <div class="min-w-0 flex-1">
                <h5 class="text-xs font-bold text-slate-800 truncate"><%= fullName %></h5>
                <p class="text-[10px] <%= (roleId==3) ? "text-emerald-500 font-bold" : "text-slate-400 font-medium" %> truncate">
                    <%= (roleId==3) ? "Đang trong ca làm" : "Chi nhánh Cầu Giấy" %>
                </p>
            </div>
        </div>
    </div>
</aside>

<div class="flex-1 flex flex-col overflow-hidden">
    <header class="h-20 bg-white border-b border-slate-200/40 flex items-center justify-between px-8 shadow-sm z-10">
        <div class="relative w-80">
            <span class="absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
                <i class="fa-solid fa-magnifying-glass text-xs"></i>
            </span>
            <input type="text" placeholder="Tìm kiếm hóa đơn, món ăn..." class="w-full bg-[#f1f5f9] text-xs pl-9 pr-4 py-2.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500/80 transition-all text-slate-800">
        </div>
        <div class="flex items-center gap-6">
            <a href="${pageContext.request.contextPath}/login?action=logout" class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100/50 shadow-sm">
                <i class="fa-solid fa-right-from-bracket text-xs"></i><span>Đăng xuất</span>
            </a>
        </div>
    </header>
    <main class="flex-1 overflow-y-auto p-8 bg-[#f4f7fc]/40">