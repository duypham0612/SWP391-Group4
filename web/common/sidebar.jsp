<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%@page import="com.mycoffee.model.Table"%>
<%@page import="java.util.List"%>
<%
    // Lấy URL hiện tại để active menu tương ứng
    String currentUri = request.getRequestURI();
    boolean isDashboard = currentUri.contains("manager-dashboard");
    boolean isInventory = currentUri.contains("manager-inventory");
    boolean isAttendance = currentUri.contains("manager-attendance");
    boolean isTables = currentUri.contains("manager-tables"); // Đã kích hoạt biến nhận diện trang bàn
    

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

    // ĐÃ THÊM: Lấy danh sách bàn được gửi từ ManagerTableController để phục vụ cho ô chọn bàn trong Modal
    List<Table> modalTableList = (List<Table>) request.getAttribute("tableList");
%>
<aside class="w-64 bg-slate-50 border-r border-slate-200/60 flex flex-col h-full z-20">
    <div class="h-20 flex items-center gap-3 px-6 border-b border-slate-200/60">
        <div class="w-9 h-9 rounded-xl bg-[#006064] flex items-center justify-center text-white shadow-md shadow-[#006064]/20">
            <i class="fa-solid fa-mug-hot text-base"></i>
        </div>
        <div>
            <h4 class="text-sm font-bold tracking-tight text-slate-800 leading-tight">Coffee POS</h4>
            <p class="text-[10px] text-slate-400 font-medium">Hệ thống quản lý</p>
        </div>
    </div>

    <nav class="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        <a href="manager-dashboard" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isDashboard ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-grid-2-dash text-sm w-4 text-center <%= isDashboard ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Tổng quan</span>
        </a>

        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-cash-register text-sm w-4 text-center text-slate-400"></i>
            <span>Gọi món (POS)</span>
        </a>

        <a href="${pageContext.request.contextPath}/manager-tables" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isTables ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-chair text-sm w-4 text-center <%= isTables ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Quản lý bàn</span>
        </a>

        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-utensils text-sm w-4 text-center text-slate-400"></i>
            <span>Thực đơn</span>
        </a>

        <a href="manager-inventory" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isInventory ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-box-open text-sm w-4 text-center <%= isInventory ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Kho nguyên liệu</span>
        </a>

        <a href="manager-attendance" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 <%= isAttendance ? "bg-sky-50 text-sky-600" : "text-slate-500 hover:bg-slate-100 hover:text-slate-800" %>">
            <i class="fa-solid fa-user-clock text-sm w-4 text-center <%= isAttendance ? "text-sky-600" : "text-slate-400" %>"></i>
            <span>Nhân viên</span>
        </a>

        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-tag text-sm w-4 text-center text-slate-400"></i>
            <span>Khuyến mãi</span>
        </a>

        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-chart-simple text-sm w-4 text-center text-slate-400"></i>
            <span>Báo cáo</span>
        </a>

        <a href="#" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200">
            <i class="fa-solid fa-gear text-sm w-4 text-center text-slate-400"></i>
            <span>Cài đặt</span>
        </a>
    </nav>

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

<div class="flex-1 flex flex-col overflow-hidden">
    <header class="h-20 bg-white border-b border-slate-200/40 flex items-center justify-between px-8 shadow-sm z-10">
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

        <div class="flex items-center gap-6">
            <div class="flex items-center gap-2">
                <button class="relative w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-bell text-sm"></i>
                    <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-red-500 ring-2 ring-white"></span>
                </button>
                <button class="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-user text-sm"></i>
                </button>
                <button class="w-9 h-9 rounded-xl hover:bg-slate-100 flex items-center justify-center text-slate-500 transition-colors">
                    <i class="fa-regular fa-sun text-sm"></i>
                </button>
            </div>

            <div class="w-px h-6 bg-slate-200"></div>

            <a 
                href="login?action=logout" 
                class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100/50 shadow-sm"
                >
                <i class="fa-solid fa-right-from-bracket text-xs"></i>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <main class="flex-1 overflow-y-auto p-8 bg-[#f4f7fc]/40">

        <div class="max-w-xl mx-auto bg-white rounded-3xl border border-slate-200/80 shadow-xl p-7 z-30 relative my-6">
            <div class="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wide">Tạo phiếu đặt bàn trước</h3>
                <span class="w-2.5 h-2.5 rounded-full bg-amber-500 animate-pulse"></span>
            </div>

            <form action="${pageContext.request.contextPath}/manager-tables" method="POST" class="flex flex-col gap-5">
                <input type="hidden" name="action" value="add">

                <div class="flex flex-col gap-1.5">
                    <label class="text-[11px] font-bold text-slate-600 uppercase tracking-wider">Tên khách hàng</label>
                    <input type="text" name="customerName" required placeholder="Nhập tên khách hàng đặt trước..." class="w-full bg-slate-50 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500 transition-all font-medium text-slate-800 placeholder:text-slate-400">
                </div>

                <div class="flex flex-col gap-1.5">
                    <label class="text-[11px] font-bold text-slate-600 uppercase tracking-wider">Số điện thoại</label>
                    <input type="tel" name="phone" required placeholder="Nhập số điện thoại liên hệ..." class="w-full bg-slate-50 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500 transition-all font-medium text-slate-800 placeholder:text-slate-400">
                </div>

                <div class="grid grid-cols-2 gap-4">
                    <div class="flex flex-col gap-1.5">
                        <label class="text-[11px] font-bold text-slate-600 uppercase tracking-wider">Chọn bàn</label>
                        <select name="tableId" required class="w-full bg-slate-50 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500 transition-all font-medium text-slate-700">
                            <option value="" disabled selected>-- Chọn bàn trống --</option>
                            <% 
                                if (modalTableList != null && !modalTableList.isEmpty()) {
                                    boolean hasAvailableTable = false;
                                    for (Table t : modalTableList) {
                                        // Lọc trạng thái hiển thị các bàn trống
                                        if ("Empty".equalsIgnoreCase(t.getStatus()) || "Available".equalsIgnoreCase(t.getStatus()) || "Trống".equalsIgnoreCase(t.getStatus())) {
                                            hasAvailableTable = true;
                            %>
                            <option value="<%= t.getTableID() %>">
                                <%= t.getTableName() %> (Sức chứa: <%= t.getCapacity() %> người)
                            </option>
                            <% 
                                        }
                                    }
                                    if (!hasAvailableTable) {
                            %>
                            <option value="" disabled>Hết bàn trống trên hệ thống!</option>
                            <%
                                    }
                                } else {
                            %>
                            <option value="" disabled>Không tìm thấy dữ liệu bàn nào</option>
                            <% 
                                }
                            %>
                        </select>
                    </div>

                    <div class="flex flex-col gap-1.5">
                        <label class="text-[11px] font-bold text-slate-600 uppercase tracking-wider">Giờ hẹn đến</label>
                        <input type="datetime-local" name="resTime" required class="w-full bg-slate-50 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500 transition-all font-medium text-slate-700">
                    </div>
                </div>

                <div class="flex items-center justify-end gap-3 border-t border-slate-100 pt-5 mt-2">
                    <button type="button" onclick="window.history.back();" class="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-500 text-xs font-bold hover:bg-slate-50 transition-all">
                        Hủy
                    </button>
                    <button type="submit" class="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-bold shadow-md shadow-teal-600/10 transition-all">
                        Xác nhận đặt
                    </button>
                </div>
            </form>
        </div>