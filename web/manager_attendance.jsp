<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Attendance"%>
<%@page import="java.text.SimpleDateFormat"%>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<%
    SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, dd/MM/yyyy");
    String formattedDate = sdfDate.format(new java.util.Date());
%>

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Chấm Công Hôm Nay</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi hoạt động check-in/check-out của nhân viên tại chi nhánh ngày hôm nay.</p>
        </div>
        <div class="flex items-center gap-3">
            <button onclick="openCheckInModal()" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006064] text-xs font-bold text-white shadow-sm hover:bg-[#004d40] transition-all">
                <i class="fa-solid fa-plus"></i> Thêm Chấm Công
            </button>
            <button onclick="window.location.reload();" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white border border-slate-200 text-xs font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all">
                <i class="fa-solid fa-arrows-rotate text-slate-400"></i> Tải lại trang
            </button>
        </div>
    </div>

    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
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
                    <i class="fa-regular fa-calendar-days text-teal-200"></i> <%= formattedDate %>
                </span>
            </div>
        </div>

        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-24">Mã CC</th>
                        <th class="px-6 py-3.5 text-center w-28">Mã NV</th>
                        <th class="px-6 py-3.5">Tên Nhân Viên</th>
                        <th class="px-6 py-3.5 text-center w-36">Ca Làm</th>
                        <th class="px-6 py-3.5 text-center w-36">Ngày</th>
                        <th class="px-6 py-3.5 text-center w-40">Giờ Vào (In)</th>
                        <th class="px-6 py-3.5 text-center w-40">Giờ Ra (Out)</th>
                        <th class="px-6 py-3.5 text-center w-36">Trạng Thái</th>
                        <th class="px-6 py-3.5 text-center w-36">Hành Động</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <%
                        List<Attendance> list = (List<Attendance>) request.getAttribute("danhSachChamCong");
                        if (list == null || list.isEmpty()) {
                    %>
                        <tr>
                            <td colspan="9" class="px-6 py-12 text-center">
                                <div class="flex flex-col items-center justify-center gap-3">
                                    <i class="fa-solid fa-clipboard-user text-slate-200 text-5xl"></i>
                                    <p class="text-xs font-bold text-slate-500">Chưa có nhân viên nào thực hiện chấm công tại chi nhánh ngày hôm nay.</p>
                                    <p class="text-[10px] text-slate-400 font-medium">Hệ thống sẽ tự động cập nhật khi có hoạt động chấm công.</p>
                                </div>
                            </td>
                        </tr>
                    <%
                        } else {
                            for (Attendance item : list) {
                                String status = item.getStatus() != null ? item.getStatus() : "Đang làm việc";
                    %>
                        <tr class="hover:bg-slate-50/40 transition-colors">
                            <td class="px-6 py-4 text-center font-bold text-slate-400">#<%= item.getAttendanceId() %></td>
                            <td class="px-6 py-4 text-center font-bold text-sky-700">NV-<%= item.getEmployeeId() %></td>
                            <td class="px-6 py-4 font-bold text-slate-800"><%= item.getEmployeeName() %></td>
                            <td class="px-6 py-4 text-center">
                                <span class="inline-flex items-center px-2.5 py-1 rounded-lg text-[10px] font-bold bg-sky-50 text-sky-600 border border-sky-100">
                                    <i class="fa-solid fa-clock-rotate-left mr-1 text-[8px]"></i> <%= item.getShiftName() %>
                                </span>
                            </td>
                            <td class="px-6 py-4 text-center text-slate-400 font-medium"><%= item.getFormattedDate() %></td>
                            <td class="px-6 py-4 text-center text-sm font-extrabold text-emerald-600"><%= item.getFormattedCheckInTime() %></td>
                            <td class="px-6 py-4 text-center text-sm font-extrabold text-rose-500"><%= item.getFormattedCheckOutTime() %></td>
                            <td class="px-6 py-4 text-center">
                                <% if ("Đã hoàn thành".equals(status)) { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100">
                                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span><%= status %>
                                    </span>
                                <% } else if ("Đi muộn".equals(status)) { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-amber-50 text-amber-600 border border-amber-100">
                                        <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span><%= status %>
                                    </span>
                                <% } else if ("Vắng mặt".equals(status)) { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-rose-50 text-rose-600 border border-rose-100">
                                        <span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span><%= status %>
                                    </span>
                                <% } else { %>
                                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-bold bg-blue-50 text-blue-600 border border-blue-100">
                                        <span class="w-1.5 h-1.5 rounded-full bg-blue-500"></span><%= status %>
                                    </span>
                                <% } %>
                            </td>
                            <td class="px-6 py-4 text-center">
                                <div class="flex items-center justify-center gap-1.5">
                                    <%-- Nút bấm kết thúc ca làm việc (Check-out nhanh) - Chỉ xuất hiện khi chưa hoàn thành --%>
                                    <% if (!"Đã hoàn thành".equals(status) && !"Vắng mặt".equals(status)) { %>
                                        <button onclick="triggerCheckOut('<%= item.getAttendanceId() %>')" class="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 hover:bg-emerald-500 hover:text-white transition-colors flex items-center justify-center shadow-sm" title="Check-out nhanh">
                                            <i class="fa-solid fa-door-open text-[11px]"></i>
                                        </button>
                                    <% } %>
                                    <button onclick="openUpdateModal('<%= item.getAttendanceId() %>', '<%= status %>')" class="w-7 h-7 rounded-lg bg-slate-100 text-slate-600 hover:bg-amber-50 hover:text-amber-600 transition-colors flex items-center justify-center shadow-sm" title="Sửa trạng thái">
                                        <i class="fa-solid fa-pen text-[11px]"></i>
                                    </button>
                                    <button onclick="triggerDelete('<%= item.getAttendanceId() %>')" class="w-7 h-7 rounded-lg bg-slate-100 text-slate-600 hover:bg-rose-50 hover:text-rose-600 transition-colors flex items-center justify-center shadow-sm" title="Xóa bản ghi">
                                        <i class="fa-solid fa-trash text-[11px]"></i>
                                    </button>
                                </div>
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

<%-- Modals --%>
<div id="checkInModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center hidden opacity-0 transition-opacity duration-300">
    <div class="bg-white rounded-3xl shadow-xl border border-slate-100 max-w-md w-full mx-4 overflow-hidden transform scale-95 transition-transform duration-300">
        <div class="px-6 py-4 bg-[#006064] text-white flex items-center justify-between">
            <h3 class="text-sm font-bold tracking-wide uppercase"><i class="fa-solid fa-user-plus mr-2"></i>Thêm Lượt Chấm Công</h3>
            <button onclick="closeCheckInModal()" class="text-white/70 hover:text-white"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <form action="manager-attendance" method="POST" class="p-6 space-y-4">
            <input type="hidden" name="action" value="checkin">
            <div>
                <label class="block text-[11px] font-bold text-slate-400 uppercase mb-1.5">Mã số nhân viên (Employee ID)</label>
                <input type="number" name="employeeId" required min="1" placeholder="Nhập số ID ví dụ: 3" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:border-[#006064]">
            </div>
            <div>
                <label class="block text-[11px] font-bold text-slate-400 uppercase mb-1.5">Chọn Ca Làm Việc</label>
                <select name="shiftId" required class="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:border-[#006064]">
                    <option value="1">Ca Sáng (06:00 - 12:00)</option>
                    <option value="2">Ca Chiều (12:00 - 18:00)</option>
                    <option value="3">Ca Tối (18:00 - 23:00)</option>
                </select>
            </div>
            <div class="flex items-center justify-end gap-2 pt-2">
                <button type="button" onclick="closeCheckInModal()" class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold text-slate-600 transition-colors">Hủy</button>
                <button type="submit" class="px-4 py-2 rounded-xl bg-[#006064] hover:bg-[#004d40] text-xs font-bold text-white transition-colors">Xác nhận Check-in</button>
            </div>
        </form>
    </div>
</div>

<div id="updateModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center hidden opacity-0 transition-opacity duration-300">
    <div class="bg-white rounded-3xl shadow-xl border border-slate-100 max-w-md w-full mx-4 overflow-hidden transform scale-95 transition-transform duration-300">
        <div class="px-6 py-4 bg-slate-800 text-white flex items-center justify-between">
            <h3 class="text-sm font-bold tracking-wide uppercase"><i class="fa-solid fa-pen-to-square mr-2"></i>Cập Nhật Trạng Thái</h3>
            <button onclick="closeUpdateModal()" class="text-white/70 hover:text-white"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <form action="manager-attendance" method="POST" class="p-6 space-y-4">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="attendanceId" id="updateAttId">
            <div>
                <label class="block text-[11px] font-bold text-slate-400 uppercase mb-1.5">Trạng thái làm việc mới</label>
                <select name="status" id="updateStatusSelect" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:border-slate-800">
                    <option value="Đang làm việc">Đang làm việc</option>
                    <option value="Đã hoàn thành">Đã hoàn thành</option>
                    <option value="Đi muộn">Đi muộn</option>
                    <option value="Vắng mặt">Vắng mặt</option>
                </select>
            </div>
            <div class="flex items-center justify-end gap-2 pt-2">
                <button type="button" onclick="closeUpdateModal()" class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold text-slate-600 transition-colors">Hủy</button>
                <button type="submit" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-xs font-bold text-white transition-colors">Lưu Thay Đổi</button>
            </div>
        </form>
    </div>
</div>

<%-- Các Form ẩn gửi Request POST lên Controller --%>
<form id="hiddenDeleteForm" action="manager-attendance" method="POST" class="hidden">
    <input type="hidden" name="action" value="delete">
    <input type="hidden" name="attendanceId" id="deleteAttId">
</form>

<form id="hiddenCheckOutForm" action="manager-attendance" method="POST" class="hidden">
    <input type="hidden" name="action" value="checkout">
    <input type="hidden" name="attendanceId" id="checkoutAttId">
</form>

<script>
    function openCheckInModal() {
        const modal = document.getElementById('checkInModal');
        modal.classList.remove('hidden');
        setTimeout(() => {
            modal.classList.add('opacity-100');
            modal.querySelector('.transform').classList.remove('scale-95');
        }, 10);
    }

    function closeCheckInModal() {
        const modal = document.getElementById('checkInModal');
        modal.classList.add('opacity-100');
        modal.querySelector('.transform').classList.add('scale-95');
        setTimeout(() => modal.classList.add('hidden'), 300);
    }

    function openUpdateModal(attId, currentStatus) {
        document.getElementById('updateAttId').value = attId;
        document.getElementById('updateStatusSelect').value = currentStatus;
        const modal = document.getElementById('updateModal');
        modal.classList.remove('hidden');
        setTimeout(() => {
            modal.classList.add('opacity-100');
            modal.querySelector('.transform').classList.remove('scale-95');
        }, 10);
    }

    function closeUpdateModal() {
        const modal = document.getElementById('updateModal');
        modal.classList.add('opacity-100');
        modal.querySelector('.transform').classList.add('scale-95');
        setTimeout(() => modal.classList.add('hidden'), 300);
    }

    function triggerDelete(attId) {
        if (confirm('Bạn có chắc chắn muốn xóa bản ghi chấm công #' + attId + ' này không?')) {
            document.getElementById('deleteAttId').value = attId;
            document.getElementById('hiddenDeleteForm').submit();
        }
    }

    // JS Xử lý gửi lệnh Check-out nhanh lên Servlet
    function triggerCheckOut(attId) {
        if (confirm('Xác nhận thực hiện Check-out kết thúc ca làm việc cho bản ghi #' + attId + '?')) {
            document.getElementById('checkoutAttId').value = attId;
            document.getElementById('hiddenCheckOutForm').submit();
        }
    }
</script>

<jsp:include page="common/footer.jsp" />