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

<div class="space-y-8 fade-in w-full px-4">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mt-2">
        <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Quản Lý Sơ Đồ & Đặt Bàn</h1>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi trạng thái bàn trực tiếp và điều phối lịch hẹn của khách hàng.</p>
        </div>
        
        <button onclick="openReservationModal()" class="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold transition-all shadow-md self-start md:self-auto">
            <i class="fa-solid fa-calendar-plus text-xs"></i>
            <span>Đặt bàn trước</span>
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
                            
                            // Cấu hình màu mặc định: BÀN TRỐNG (Màu Xanh Lá)
                            String bgClass = "bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100/70";
                            String iconColor = "text-emerald-500";
                            String textStatusDisplay = "Trống";
                            
                            // Kiểm tra trạng thái ĐÃ ĐẶT TRƯỚC (Màu Vàng)
                            if("Reserved".equalsIgnoreCase(status) || "Đã đặt".equalsIgnoreCase(status)) {
                                bgClass = "bg-amber-50 border-amber-200 text-amber-700 hover:bg-amber-100/70";
                                iconColor = "text-amber-500";
                                textStatusDisplay = "Đã đặt trước";
                            } 
                            // 🛠️ ĐÃ SỬA: Chuyển điều kiện so sánh thành "Occupied" theo dữ liệu Database thực tế (Màu Đỏ)
                            else if ("Occupied".equalsIgnoreCase(status) || "Serving".equalsIgnoreCase(status) || "Đang phục vụ".equalsIgnoreCase(status) || "Đang ngồi".equalsIgnoreCase(status)) {
                                bgClass = "bg-rose-50 border-rose-200 text-rose-700 hover:bg-rose-100/70";
                                iconColor = "text-rose-500";
                                textStatusDisplay = "Đang phục vụ";
                            }
                %>
                <div onclick="handleTableClick(<%= t.getTableID() %>, '<%= status %>')" class="<%= bgClass %> p-4 rounded-2xl border transition-all cursor-pointer flex flex-col justify-between h-28 shadow-sm">
                    <div class="flex justify-between items-start">
                        <span class="text-xs font-extrabold tracking-tight"><%= t.getTableName() %></span>
                        <i class="fa-solid fa-couch <%= iconColor %> text-sm"></i>
                    </div>
                    <div class="mt-2">
                        <p class="text-[10px] font-medium opacity-60">Sức chứa: <%= t.getCapacity() %> người</p>
                        <p class="text-[11px] font-bold mt-1 uppercase tracking-wide">
                            <%= textStatusDisplay %>
                        </p>
                    </div>
                </div>
                <% 
                        }
                    } else { 
                %>
                    <div class="col-span-full text-center text-xs py-8 text-slate-400">Không tìm thấy dữ liệu bàn trên hệ thống.</div>
                <% } %>
            </div>
        </div>

        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between">
            <div class="space-y-5">
                <div class="flex items-center justify-between border-b border-slate-100 pb-4">
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Lịch hẹn hôm nay</h3>
                    <span class="px-2 py-0.5 rounded-full bg-[#006064]/10 text-[#006064] text-[10px] font-bold">
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
                            <div class="w-9 h-9 rounded-xl bg-[#006064]/5 text-[#006064] flex items-center justify-center font-bold text-xs">
                                <i class="fa-regular fa-clock"></i>
                            </div>
                            <div>
                                <h4 class="text-xs font-bold text-slate-800"><%= res.getCustomerName() %></h4>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5"><%= res.getPhone() %> • Bàn <%= res.getTableID() %></p>
                            </div>
                        </div>
                        <div class="text-right">
                            <span class="text-xs font-black text-slate-700 block"><%= (res.getReservationTime() != null) ? sdfTime.format(res.getReservationTime()) : "--:--" %></span>
                            <form action="${pageContext.request.contextPath}/manager-tables" method="POST" class="inline mt-1">
                                <input type="hidden" name="action" value="checkin">
                                <input type="hidden" name="resId" value="<%= res.getReservationID() %>">
                                <input type="hidden" name="tableId" value="<%= res.getTableID() %>">
                                <button type="submit" class="text-[9px] font-bold text-[#006064] hover:underline">Nhận bàn</button>
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

<div id="reservationModal" class="hidden fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-100">
        <div class="flex justify-between items-center border-b pb-3">
            <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Tạo phiếu đặt bàn trước</h3>
            <button onclick="closeReservationModal()" class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <form action="${pageContext.request.contextPath}/manager-tables" method="POST" class="space-y-3 text-xs">
            <input type="hidden" name="action" value="add">
            
            <div>
                <label class="block font-bold text-slate-700 mb-1">Tên khách hàng</label>
                <input type="text" name="customerName" required class="w-full border p-2 rounded-xl focus:outline-none focus:border-[#006064]">
            </div>
            <div>
                <label class="block font-bold text-slate-700 mb-1">Số điện thoại</label>
                <input type="tel" name="phone" required class="w-full border p-2 rounded-xl focus:outline-none focus:border-[#006064]">
            </div>
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="block font-bold text-slate-700 mb-1">Chọn bàn</label>
                    <select name="tableId" class="w-full border p-2 rounded-xl focus:outline-none">
                        <% if (tableList != null) {
                            for(Table t : tableList) { 
                                String statusT = t.getStatus();
                                if("Empty".equalsIgnoreCase(statusT) || "Trống".equalsIgnoreCase(statusT)) { %>
                                    <option value="<%= t.getTableID() %>"><%= t.getTableName() %> (Sức chứa: <%= t.getCapacity() %>)</option>
                        <%      }
                            }
                           } %>
                    </select>
                </div>
                <div>
                    <label class="block font-bold text-slate-700 mb-1">Giờ hẹn đến</label>
                    <input type="datetime-local" name="resTime" required class="w-full border p-2 rounded-xl focus:outline-none">
                </div>
            </div>
            <div class="pt-2 flex justify-end gap-2">
                <button type="button" onclick="closeReservationModal()" class="px-4 py-2 border rounded-xl font-bold text-slate-500">Hủy</button>
                <button type="submit" class="px-4 py-2 bg-[#006064] text-white rounded-xl font-bold">Xác nhận đặt</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openReservationModal() {
        document.getElementById('reservationModal').classList.remove('hidden');
    }
    
    function closeReservationModal() {
        document.getElementById('reservationModal').classList.add('hidden');
    }

    function handleTableClick(tableId, currentStatus) {
        var statusNorm = currentStatus.toLowerCase().trim();
        if (statusNorm === 'empty' || statusNorm === 'trống') {
            if(confirm("Bàn đang trống. Bạn muốn chuyển trạng thái sang 'Đang ngồi phục vụ' trực tiếp không?")) {
                window.location.href = "${pageContext.request.contextPath}/manager-tables?action=quickOpen&tableId=" + tableId;
            }
        } else if (statusNorm === 'occupied' || statusNorm === 'serving' || statusNorm === 'đang phục vụ' || statusNorm === 'đang ngồi') {
            alert("Bàn này đang có khách ngồi. Hệ thống sẽ điều hướng qua trang quản lý Order của bàn!");
            // window.location.href = "${pageContext.request.contextPath}/manager-orders?tableId=" + tableId;
        }
    }
</script>

<jsp:include page="/common/footer.jsp" />