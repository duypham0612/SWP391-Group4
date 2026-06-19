<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%
    // Lấy URL hiện tại để active menu tương ứng
    String currentUri = request.getRequestURI();
    boolean isDashboard = currentUri.contains("manager-dashboard");
    boolean isInventory = currentUri.contains("manager-inventory");
    boolean isAttendance = currentUri.contains("manager-attendance");

    // Lấy thông tin user đăng nhập từ Session
    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null) ? loggedInUser.getFullName() : "Admin Manager";
    String shortName = "";
    if (fullName != null && !fullName.trim().isEmpty()) {
        String[] parts = fullName.split(" ");
        shortName = parts[parts.length - 1]; // Lấy tên cuối
    } else {
        shortName = "AD";
    }
%>
<!-- Sidebar Container -->
<aside class="w-64 bg-slate-50 border-r border-slate-200/60 flex flex-col h-full z-20">
    <!-- Branding -->
    <div class="h-20 flex items-center gap-3 px-6 border-b border-slate-200/60">
        <div class="w-9 h-9 rounded-xl bg-[#006064] flex items-center justify-center text-white shadow-md shadow-[#006064]/20">
            <i class="fa-solid fa-mug-hot text-base"></i>
        </div>
        <div>
            <h4 class="text-sm font-bold tracking-tight text-slate-800 leading-tight">Coffee POS</h4>
            <p class="text-[10px] text-slate-400 font-medium">Hệ thống quản lý</p>
        </div>
    </div>

    <!-- Navigation links -->
    <nav class="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        <!-- Item: Tổng quan -->
        <a href="manager-dashboard" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isDashboard ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-grid-2-dash text-sm w-4 text-center <%= isDashboard ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Tổng quan</span>
        </a>

        <!-- Item: Gọi món (POS) -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-cash-register text-sm w-4 text-center text-slate-400"></i>
            <span>Gọi món (POS)</span>
        </a>

        <!-- Item: Quản lý bàn -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-chair text-sm w-4 text-center text-slate-400"></i>
            <span>Quản lý bàn</span>
        </a>

        <!-- Item: Sơ đồ bàn -->
        <a href="pos-tables" class="flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-slate-50 text-slate-600 hover:text-[#006064] transition-colors">
            <i class="fa-solid fa-border-all text-lg"></i>
            <span class="text-sm font-bold">Sơ đồ bàn</span>
        </a>

        <!-- Item: Thực đơn -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-utensils text-sm w-4 text-center text-slate-400"></i>
            <span>Thực đơn</span>
        </a>

        <!-- Item: Kho nguyên liệu -->
        <a href="manager-inventory" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isInventory ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-box-open text-sm w-4 text-center <%= isInventory ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Kho nguyên liệu</span>
        </a>

        <!-- Item: Nhân viên -->
        <a href="manager-attendance" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isAttendance ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-user-clock text-sm w-4 text-center <%= isAttendance ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Nhân viên</span>
        </a>

        <!-- Item: Khuyến mãi -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-tag text-sm w-4 text-center text-slate-400"></i>
            <span>Khuyến mãi</span>
        </a>

        <!-- Item: Báo cáo -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-chart-simple text-sm w-4 text-center text-slate-400"></i>
            <span>Báo cáo</span>
        </a>

        <!-- Item: Cài đặt -->
        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-gear text-sm w-4 text-center text-slate-400"></i>
            <span>Cài đặt</span>
        </a>
    </nav>

    <!-- User Profile Card in Sidebar Footer -->
    <div class="p-4 border-t border-slate-200/60 bg-slate-50/50">
        <div class="flex items-center gap-3 bg-white border border-slate-200/50 p-2.5 rounded-2xl shadow-sm">
            <div class="w-9 h-9 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shadow-inner">
                <%= shortName.toUpperCase() %>
            </div>
            <div class="min-w-0 flex-1">
                <h5 class="text-xs font-bold text-slate-800 truncate"><%= fullName %></h5>
                <p class="text-[10px] text-slate-400 font-medium truncate">Chi nhánh Cầu Giấy</p>
            </div>
        </div>
    </div>
</aside>

<!-- Main content container opens here, it will be closed in footer.jsp -->
<div class="flex-1 flex flex-col overflow-hidden">
    <!-- Top Navbar / Header -->
    <header class="h-20 bg-white border-b border-slate-200/40 flex items-center justify-between px-8 shadow-sm z-10">
        <!-- Left Search bar -->
        <div class="relative w-80">
            <span class="absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
                <i class="fa-solid fa-magnifying-glass text-xs"></i>
            </span>
            <input 
                type="text" 
                placeholder="Tìm kiếm hóa đơn, món ăn, nhân viên..." 
                class="w-full bg-[#f1f5f9] text-xs pl-9 pr-4 py-2.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500/80 focus:bg-white transition-all text-slate-800 placeholder:text-slate-400 font-medium"
            >
        </div>

        <!-- Right User Actions -->
        <div class="flex items-center gap-6">
            <!-- Icon shortcuts -->
            <div class="flex items-center gap-2">
                <!-- Notifications icon -->
                <button class="relative w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-bell text-sm"></i>
                    <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-red-500 ring-2 ring-white"></span>
                </button>
                <!-- Group icon -->
                <button class="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-user text-sm"></i>
                </button>
                <!-- Settings shortcut icon -->
                <button class="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-sun text-sm"></i>
                </button>
            </div>

            <!-- Vertical Separator -->
            <div class="w-px h-6 bg-slate-200"></div>

            <!-- Logout Button -->
            <a 
                href="login?action=logout" 
                class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100/50 shadow-sm"
            >
                <i class="fa-solid fa-right-from-bracket text-xs"></i>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <!-- Scrollable content panel opens here, closed in footer.jsp -->
    <main class="flex-1 overflow-y-auto p-8 bg-[#f4f7fc]/40">
