<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Table"%>
<%@page import="com.mycoffee.model.Reservation"%>
<%@page import="java.text.SimpleDateFormat"%>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/sidebar.jsp" />

<%
    List<Table> tableList = (List<Table>) request.getAttribute("tableList");
    List<Reservation> activeReservations = (List<Reservation>) request.getAttribute("activeReservations");
    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
%>

<div class="space-y-8 fade-in w-full px-4 mt-2">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Quản Lý Bàn & Khách Đặt (Sảnh)</h1>
            <p class="text-xs text-slate-400 font-medium mt-1">Cập nhật trạng thái sảnh. Bấm "Gọi món (POS)" ở Sidebar để lên hóa đơn.</p>
        </div>

        <button onclick="openReservationModal()" class="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-md self-start md:self-auto">
            <i class="fa-solid fa-calendar-plus text-xs"></i>
            <span>Tạo Phiếu Đặt Trước</span>
        </button>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm lg:col-span-2 space-y-6">
            <div class="flex items-center justify-between border-b border-slate-100 pb-4">
                <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Sơ đồ phòng máy / Bàn</h3>
                <div class="flex items-center gap-4 text-[10px] font-bold">
                    <span class="flex items-center gap-1.5 text-emerald-600"><span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>Trống</span>
                    <span class="flex items-center gap-1.5 text-amber-600"><span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>Đã đặt</span>
                    <span class="flex items-center gap-1.5 text-rose-600"><span class="w-2.5 h-2.5 rounded-full bg-rose-500"></span>Đang ngồi</span>
                </div>
            </div>

            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                <%
                    if (tableList != null) {
                        for(Table t : tableList) {
                            String status = (t.getStatus() != null) ? t.getStatus().trim() : "Empty";
                            String bgClass = "bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100/70";
                            String iconColor = "text-emerald-500";
                            String textStatusDisplay = "Trống";

                            if("Reserved".equalsIgnoreCase(status) || "Đã đặt".equalsIgnoreCase(status)) {
                                bgClass = "bg-amber-50 border-amber-200 text-amber-700 hover:bg-amber-100/70";
                                iconColor = "text-amber-500";
                                textStatusDisplay = "Đã đặt trước";
                            }
                            else if ("Occupied".equalsIgnoreCase(status) || "Serving".equalsIgnoreCase(status) || "Đang phục vụ".equalsIgnoreCase(status) || "Đang ngồi".equalsIgnoreCase(status)) {
                                bgClass = "bg-rose-50 border-rose-200 text-rose-700 hover:bg-rose-100/70";
                                iconColor = "text-rose-500";
                                textStatusDisplay = "Đang phục vụ";
                            }
                %>
                <!-- CLICK MỞ MODAL XÁC NHẬN -->
                <div onclick="handleTableClick(<%= t.getTableID() %>, '<%= status %>')" class="<%= bgClass %> p-4 rounded-2xl border transition-all cursor-pointer flex flex-col justify-between h-28 shadow-sm">
                    <div class="flex justify-between items-start">
                        <span class="text-xs font-extrabold tracking-tight"><%= t.getTableName() %></span>
                        <i class="fa-solid fa-couch <%= iconColor %> text-sm"></i>
                    </div>
                    <div class="mt-2">
                        <p class="text-[10px] font-medium opacity-60">Sức chứa: <%= t.getCapacity() %> người</p>
                        <p class="text-[11px] font-bold mt-1 uppercase tracking-wide"><%= textStatusDisplay %></p>
                    </div>
                </div>
                <%
                        }
                    } else {
                %>
                    <div class="col-span-full text-center text-xs py-8 text-slate-400">Không có dữ liệu bàn.</div>
                <% } %>
            </div>
        </div>

        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between">
            <div class="space-y-5">
                <div class="flex items-center justify-between border-b border-slate-100 pb-4">
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Lịch hẹn hôm nay</h3>
                    <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[10px] font-bold">
                        <%= activeReservations != null ? activeReservations.size() : 0 %> cuộc hẹn
                    </span>
                </div>

                <div class="space-y-3.5 max-h-[420px] overflow-y-auto pr-1">
                    <%
                        if (activeReservations != null && !activeReservations.isEmpty()) {
                            for(Reservation res : activeReservations) {
                    %>
                    <div class="p-3 rounded-2xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition-colors flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold text-xs">
                                <i class="fa-regular fa-clock"></i>
                            </div>
                            <div>
                                <h4 class="text-xs font-bold text-slate-800"><%= res.getCustomerName() %></h4>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5"><%= res.getPhone() %> • Bàn <%= res.getTableID() %></p>
                            </div>
                        </div>
                        <div class="text-right">
                            <span class="text-xs font-black text-slate-700 block"><%= (res.getReservationTime() != null) ? sdfTime.format(res.getReservationTime()) : "--:--" %></span>
                            <form action="${pageContext.request.contextPath}/cashier-tables" method="POST" class="inline mt-1">
                                <input type="hidden" name="action" value="checkin">
                                <input type="hidden" name="resId" value="<%= res.getReservationID() %>">
                                <input type="hidden" name="tableId" value="<%= res.getTableID() %>">
                                <button type="submit" class="text-[9px] font-bold text-emerald-600 hover:underline">Nhận bàn</button>
                            </form>
                        </div>
                    </div>
                    <%
                            }
                        } else {
                    %>
                        <div class="text-center text-xs py-12 text-slate-400 font-medium">Không có lịch đặt bàn trước nào hôm nay.</div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Modal Đặt Bàn -->
<div id="reservationModal" class="hidden fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-100">
        <div class="flex justify-between items-center border-b pb-3">
            <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Tạo phiếu đặt bàn trước</h3>
            <button type="button" onclick="closeReservationModal()" class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <form action="${pageContext.request.contextPath}/cashier-tables" method="POST" class="space-y-3 text-xs">
            <input type="hidden" name="action" value="add">

            <div>
                <label class="block font-bold text-slate-700 mb-1">Tên khách hàng</label>
                <input type="text" name="customerName" required class="w-full border border-slate-200 p-2.5 rounded-xl focus:outline-none focus:border-emerald-500">
            </div>
            <div>
                <label class="block font-bold text-slate-700 mb-1">Số điện thoại</label>
                <input type="tel" name="phone" required class="w-full border border-slate-200 p-2.5 rounded-xl focus:outline-none focus:border-emerald-500">
            </div>
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="block font-bold text-slate-700 mb-1">Chọn bàn trống</label>
                    <select name="tableId" class="w-full border border-slate-200 p-2.5 rounded-xl focus:outline-none bg-white">
                        <% if (tableList != null) {
                            for(Table t : tableList) {
                                String statusT = t.getStatus();
                                if("Empty".equalsIgnoreCase(statusT) || "Trống".equalsIgnoreCase(statusT)) { %>
                                    <option value="<%= t.getTableID() %>"><%= t.getTableName() %> (Chứa: <%= t.getCapacity() %>)</option>
                        <%      }
                            }
                           } %>
                    </select>
                </div>
                <div>
                    <label class="block font-bold text-slate-700 mb-1">Giờ khách đến</label>
                    <input type="datetime-local" name="resTime" required class="w-full border border-slate-200 p-2.5 rounded-xl focus:outline-none focus:border-emerald-500">
                </div>
            </div>
            <div class="pt-4 flex justify-end gap-2">
                <button type="button" onclick="closeReservationModal()" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-600 transition-colors">Hủy</button>
                <button type="submit" class="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold transition-colors">Xác nhận đặt</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal Xác Nhận Giao Diện Đẹp Thay Vì Alert/Confirm -->
<div id="customConfirmModal" class="hidden fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-sm w-full p-6 space-y-4 shadow-xl border border-slate-100 text-center">
        <div class="text-amber-500 mb-2">
            <i class="fa-solid fa-circle-exclamation text-4xl"></i>
        </div>
        <h3 class="text-lg font-bold text-slate-800" id="confirmTitle">Xác nhận</h3>
        <p class="text-sm text-slate-600 font-medium" id="confirmMessage">Bạn có chắc chắn?</p>
        <div class="pt-4 flex justify-center gap-3">
            <button type="button" onclick="closeConfirmModal()" class="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 rounded-xl font-bold text-slate-600 transition-colors">Đóng</button>
            <a href="#" id="confirmActionBtn" class="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold transition-colors">Xác nhận</a>
        </div>
    </div>
</div>

<script>
    // Logic Đặt bàn
    function openReservationModal() {
        document.getElementById('reservationModal').classList.remove('hidden');
    }

    function closeReservationModal() {
        document.getElementById('reservationModal').classList.add('hidden');
    }

    // Logic Modal Xác nhận (Thay alert/confirm)
    function showConfirm(title, message, actionUrl) {
        document.getElementById('confirmTitle').innerText = title;
        document.getElementById('confirmMessage').innerText = message;

        let actionBtn = document.getElementById('confirmActionBtn');
        if(actionUrl) {
            actionBtn.href = actionUrl;
            actionBtn.style.display = 'inline-block';
            actionBtn.innerText = 'Xác nhận';
        } else {
            // Trường hợp chỉ thông báo (như alert)
            actionBtn.style.display = 'none';
        }

        document.getElementById('customConfirmModal').classList.remove('hidden');
    }

    function closeConfirmModal() {
        document.getElementById('customConfirmModal').classList.add('hidden');
    }

    function handleTableClick(tableId, currentStatus) {
        var statusNorm = currentStatus.toLowerCase().trim();
        if (statusNorm === 'empty' || statusNorm === 'trống') {
            showConfirm("Mở Bàn", "Khách vào ngồi thẳng không gọi món ngay? Chuyển sang 'Đang phục vụ'?", "${pageContext.request.contextPath}/cashier-tables?action=quickOpen&tableId=" + tableId);
        } else if (statusNorm === 'occupied' || statusNorm === 'serving' || statusNorm === 'đang phục vụ' || statusNorm === 'đang ngồi') {
            showConfirm("Dọn Bàn", "Khách đã rời đi. Dọn bàn và đặt trạng thái về 'Trống'?", "${pageContext.request.contextPath}/cashier-tables?action=quickFree&tableId=" + tableId);
        } else if (statusNorm === 'reserved' || statusNorm === 'đã đặt') {
            showConfirm("Bàn Đã Đặt", "Bàn này đã được khách đặt trước. Vui lòng nhận bàn ở danh sách bên cạnh!", null);
        }
    }
</script>

<jsp:include page="/common/footer.jsp" />