<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Inventory"%>

<%-- Nhúng Header dùng chung --%>
<jsp:include page="/common/header.jsp" />

<%-- Nhúng Sidebar dùng chung --%>
<jsp:include page="/common/sidebar.jsp" />

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <!-- Breadcrumb / Section Intro -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Tồn Kho Thực Tế</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Quản lý và theo dõi lượng nguyên liệu pha chế tại chi nhánh hiện tại.</p>
        </div>
        <div class="flex items-center gap-3">
            <button onclick="window.location.reload();" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white border border-slate-200 text-xs font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all">
                <i class="fa-solid fa-arrows-rotate text-slate-400"></i>
                Tải lại trang
            </button>
        </div>
    </div>

    <!-- Main Card -->
    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
        <!-- Banner/Header of Card -->
        <div class="px-6 py-5 bg-[#006064] text-white flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div class="flex items-center gap-3">
                <div class="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center border border-white/20 shadow-inner">
                    <i class="fa-solid fa-warehouse text-xl text-white"></i>
                </div>
                <div>
                    <h3 class="text-sm font-bold tracking-wide uppercase">QUẢN LÝ TỒN KHO NGUYÊN LIỆU</h3>
                    <p class="text-[11px] text-teal-100/90 mt-0.5 font-medium">Hệ thống tự động bật cảnh báo đối với nguyên liệu sắp hết.</p>
                </div>
            </div>
            <div>
                <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-bold bg-white/10 border border-white/10 text-white backdrop-blur-sm">
                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                    Hệ thống trực tuyến
                </span>
            </div>
        </div>

        <!-- Table View -->
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-24">Mã NL</th>
                        <th class="px-6 py-3.5">Tên Nguyên Liệu</th>
                        <th class="px-6 py-3.5 text-center w-40">Tồn Kho</th>
                        <th class="px-6 py-3.5 text-center w-32">Đơn Vị</th>
                        <th class="px-6 py-3.5 text-center w-40">Ngưỡng Tối Thiểu</th>
                        <th class="px-6 py-3.5 text-center w-56">Trạng Thế</th>
                        <th class="px-6 py-3.5 text-center w-48">Cập Nhật Cuối</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <%
                        List<Inventory> list = (List<Inventory>) request.getAttribute("khoHang");
                        if (list == null || list.isEmpty()) {
                    %>
                        <tr>
                            <td colspan="7" class="px-6 py-12 text-center">
                                <div class="flex flex-col items-center justify-center gap-3">
                                    <i class="fa-solid fa-box-open text-slate-200 text-5xl"></i>
                                    <p class="text-xs font-bold text-red-500">Không có dữ liệu nguyên liệu nào trong kho!</p>
                                    <p class="text-[10px] text-slate-400">Vui lòng kiểm tra lại dữ liệu trong bảng SQL Server.</p>
                                </div>
                            </td>
                        </tr>
                    <%
                        } else {
                            for (Inventory item : list) {
                                boolean isLowStock = item.getQuantity() < item.getMinRequired();
                    %>
                        <tr class="<%= isLowStock ? "bg-red-50/20 hover:bg-red-50/40 text-red-950" : "hover:bg-slate-50/40 transition-colors" %>">
                            <td class="px-6 py-4 text-center font-bold text-slate-400 <%= isLowStock ? "text-red-500" : "" %>">
                                #<%= item.getIngredientId() %>
                            </td>
                            <td class="px-6 py-4 font-bold text-slate-800 <%= isLowStock ? "text-red-900" : "" %>">
                                <%= item.getIngredientName() %>
                            </td>
                            <td class="px-6 py-4 text-center text-sm font-extrabold <%= isLowStock ? "text-red-650" : "text-slate-800" %>">
                                <%= item.getQuantity() %>
                            </td>
                            <td class="px-6 py-4 text-center">
                                <span class="inline-flex items-center px-2 py-1 rounded-lg text-[10px] font-bold <%= isLowStock ? "bg-red-50 text-red-600 border border-red-100" : "bg-slate-100 text-slate-600 border border-slate-200/40" %>">
                                    <%= item.getUnit() %>
                                </span>
                            </td>
                            <td class="px-6 py-4 text-center text-slate-400 font-semibold">
                                <%= item.getMinRequired() %>
                            </td>
                            <td class="px-6 py-4 text-center">
                                <% if (isLowStock) { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-red-50 text-red-650 border border-red-100 animate-pulse shadow-sm shadow-red-500/5">
                                        <i class="fa-solid fa-triangle-exclamation text-[8px]"></i>
                                        Sắp hết - Cần nhập
                                    </span>
                                <% } else { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100">
                                        <i class="fa-solid fa-circle-check text-[8px]"></i>
                                        An toàn
                                    </span>
                                <% } %>
                            </td>
                            <td class="px-6 py-4 text-center text-[10px] text-slate-400 font-medium">
                                <%= item.getLastUpdated() != null ? item.getLastUpdated().toString() : "N/A" %>
                            </td>
                        </tr>
                    <%
                            }
                        }
                    %>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Nhúng Footer dùng chung --%>
<jsp:include page="/common/footer.jsp" />