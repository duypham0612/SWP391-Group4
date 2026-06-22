<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Attendance"%>
<%@page import="java.text.SimpleDateFormat"%>

<%-- Nhúng Header dùng chung --%>
<jsp:include page="/common/header.jsp" />

<%-- Nhúng Sidebar dùng chung --%>
<jsp:include page="/common/sidebar.jsp" />

<%
    SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, dd/MM/yyyy");
    String formattedDate = sdfDate.format(new java.util.Date());
%>

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <!-- Breadcrumb / Section Intro -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Chấm Công Hôm Nay</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi hoạt động check-in/check-out của nhân viên tại chi nhánh ngày hôm nay.</p>
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
                    <i class="fa-solid fa-user-clock text-xl text-white"></i>
                </div>
                <div>
                    <h3 class="text-sm font-bold tracking-wide uppercase">LỊCH SỬ CHẤM CÔNG HÔM NAY</h3>
                    <p class="text-[11px] text-teal-100/90 mt-0.5 font-medium">Danh sách những nhân viên đã thực hiện check-in vào ca làm việc.</p>
                </div>
            </div>
            <div>
                <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-bold bg-white/10 border border-white/10 text-white backdrop-blur-sm">
                    <i class="fa-regular fa-calendar-days text-teal-200"></i>
                    <%= formattedDate %>
                </span>
            </div>
        </div>

        <!-- Table View -->
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-24">Mã CC</th>
                        <th class="px-6 py-3.5 text-center w-28">Mã NV</th>
                        <th class="px-6 py-3.5">Tên Nhân Viên</th>
                        <th class="px-6 py-3.5 text-center w-36">Ca Làm</th>
                        <th class="px-6 py-3.5 text-center w-40">Ngày</th>
                        <th class="px-6 py-3.5 text-center w-48">Giờ Vào (Check-in)</th>
                        <th class="px-6 py-3.5 text-center w-40">Trạng Thái</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <%
                        List<Attendance> list = (List<Attendance>) request.getAttribute("danhSachChamCong");
                        if (list == null || list.isEmpty()) {
                    %>
                        <tr>
                            <td colspan="7" class="px-6 py-12 text-center">
                                <div class="flex flex-col items-center justify-center gap-3">
                                    <i class="fa-solid fa-clipboard-user text-slate-200 text-5xl"></i>
                                    <p class="text-xs font-bold text-slate-500">Chưa có nhân viên nào thực hiện check-in ca làm việc ngày hôm nay.</p>
                                    <p class="text-[10px] text-slate-400 font-medium">Hệ thống sẽ tự động cập nhật khi có hoạt động chấm công.</p>
                                </div>
                            </td>
                        </tr>
                    <%
                        } else {
                            for (Attendance item : list) {
                    %>
                        <tr class="hover:bg-slate-50/40 transition-colors">
                            <td class="px-6 py-4 text-center font-bold text-slate-400">
                                #<%= item.getAttendanceId() %>
                            </td>
                            <td class="px-6 py-4 text-center font-bold text-sky-700">
                                NV-<%= item.getEmployeeId() %>
                            </td>
                            <td class="px-6 py-4 font-bold text-slate-800">
                                <%= item.getEmployeeName() %>
                            </td>
                            <td class="px-6 py-4 text-center">
                                <span class="inline-flex items-center px-2.5 py-1 rounded-lg text-[10px] font-bold bg-sky-50 text-sky-600 border border-sky-100">
                                    <i class="fa-solid fa-clock-rotate-left mr-1 text-[8px]"></i>
                                    <%= item.getShiftName() %>
                                </span>
                            </td>
                            <td class="px-6 py-4 text-center text-slate-400 font-medium">
                                <%= item.getDate() %>
                            </td>
                            <td class="px-6 py-4 text-center text-sm font-extrabold text-emerald-600">
                                <%= item.getCheckInTime() != null ? item.getCheckInTime().toString().substring(11, 19) : "N/A" %>
                            </td>
                            <td class="px-6 py-4 text-center">
                                <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100">
                                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                                    Đang làm việc
                                </span>
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