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
                            
                            String bgClass = "bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100/70";
                            String iconColor = "text-emerald-500";
                            String textStatusDisplay = "Trống";
                            
                            if("Reserved".equalsIgnoreCase(status) || "Đã đặt".equalsIgnoreCase(status) || "Đã đặt trước".equalsIgnoreCase(status)) {
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
                <div onclick="handleTableClick(<%= t.getTableID() %>, '<%= t.getTableName() %>', '<%= status %>', <%= t.getCapacity() %>)" class="<%= bgClass %> p-4 rounded-2xl border transition-all cursor-pointer flex flex-col justify-between h-28 shadow-sm">
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

<div id="tableDetailModal" class="hidden fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-3xl max-w-sm w-full p-6 space-y-5 shadow-2xl border border-slate-100 flex flex-col items-center text-center">
        <div class="w-full flex justify-between items-center border-b pb-2">
            <h3 id="modalTableTitle" class="text-sm font-black text-[#006064] uppercase tracking-wider">Chi Tiết Bàn</h3>
            <button onclick="closeTableDetailModal()" class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-xmark text-sm"></i></button>
        </div>

        <div class="p-4 bg-slate-50 border border-dashed border-slate-200 rounded-2xl flex flex-col items-center justify-center w-48 h-48 shadow-inner">
            <img id="modalQRImage" src="" alt="Mã QR Gọi món" class="w-40 h-40 object-contain transition-all duration-300" />
        </div>

        <div class="text-xs space-y-1.5 w-full text-left bg-slate-50 p-3 rounded-2xl border border-slate-100">
            <p class="flex justify-between">
                <span class="text-slate-400 font-medium">Trạng thái hiện tại:</span> 
                <span id="modalTableStatus" class="font-bold"></span>
            </p>
            <p class="flex justify-between">
                <span class="text-slate-400 font-medium">Sức chứa tối đa:</span> 
                <span id="modalTableCapacity" class="font-bold text-slate-700"></span>
            </p>
            <div class="pt-1">
                <p class="text-[10px] text-slate-400 font-medium mb-1"><i class="fa-solid fa-link"></i> Link quét gọi món của khách:</p>
                <p id="modalTableURL" class="text-[10px] text-slate-600 truncate bg-white p-1.5 rounded-lg border font-mono select-all"></p>
            </div>
        </div>

        <div class="w-full flex flex-col gap-2 pt-1 text-xs">
            <div class="grid grid-cols-2 gap-2">
                <button onclick="printQRCode()" class="flex items-center justify-center gap-1.5 px-4 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-xl font-bold transition-colors">
                    <i class="fa-solid fa-print"></i> In Mã QR
                </button>
                <button id="btnViewOrder" onclick="goToOrderPage()" class="hidden flex items-center justify-center gap-1.5 px-4 py-2.5 bg-sky-50 hover:bg-sky-100 text-sky-700 rounded-xl font-bold transition-all border border-sky-100">
                    <i class="fa-solid fa-receipt"></i> Xem Order
                </button>
            </div>

            <button id="btnQuickOpen" onclick="executeQuickOpen()" class="hidden w-full flex items-center justify-center gap-1.5 px-4 py-2.5 bg-[#006064] hover:bg-[#004d40] text-white rounded-xl font-bold transition-all shadow-md">
                <i class="fa-solid fa-bolt"></i> Mở Bàn Ngay
            </button>

            <button id="btnCancelRes" onclick="executeCancelReservation()" class="hidden w-full flex items-center justify-center gap-1.5 px-4 py-2.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-bold transition-all shadow-md">
                <i class="fa-solid fa-calendar-xmark"></i> Hủy Lịch Đặt Trước
            </button>

            <button id="btnResetTable" onclick="executeResetTable()" class="hidden w-full flex items-center justify-center gap-1.5 px-4 py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl font-bold transition-all shadow-md">
                <i class="fa-solid fa-trash-can"></i> Dọn Bàn & Trả Về Trống
            </button>
        </div>
    </div>
</div>

<form id="hiddenActionForm" action="${pageContext.request.contextPath}/manager-tables" method="POST" style="display:none;">
    <input type="hidden" name="action" id="hiddenActionField" value="">
    <input type="hidden" name="tableId" id="hiddenTableIdField" value="">
</form>

<script>
    let activeSelectedTableId = null;
    let activeSelectedQRUrl = "";

    function openReservationModal() {
        document.getElementById('reservationModal').classList.remove('hidden');
    }
    function closeReservationModal() {
        document.getElementById('reservationModal').classList.add('hidden');
    }
    function closeTableDetailModal() {
        document.getElementById('tableDetailModal').classList.add('hidden');
    }

    // Hàm Click điều phối mở popup chi tiết
    function handleTableClick(tableId, tableName, currentStatus, capacity) {
        activeSelectedTableId = tableId;
        const statusNorm = currentStatus.toLowerCase().trim();

        // 1. Đồng bộ link QR cho Customer
        const currentOrigin = window.location.origin;
        const contextPath = "${pageContext.request.contextPath}";
        const customerMenuLink = currentOrigin + contextPath + "/customer-menu?tableId=" + tableId;
        activeSelectedQRUrl = customerMenuLink;

        // 2. Gọi API hiển thị ảnh QR động
        document.getElementById('modalQRImage').src = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + encodeURIComponent(customerMenuLink);

        // 3. Đổ text mô tả
        document.getElementById('modalTableTitle').innerText = "Quản lý bàn - " + tableName;
        document.getElementById('modalTableCapacity').innerText = capacity;
        document.getElementById('modalTableURL').innerText = "/customer-menu?tableId=" + tableId;

        // 4. Lấy DOM các nút bấm điều khiển
        const statusLabel = document.getElementById('modalTableStatus');
        const btnQuickOpen = document.getElementById('btnQuickOpen');
        const btnCancelRes = document.getElementById('btnCancelRes');
        const btnResetTable = document.getElementById('btnResetTable');
        const btnViewOrder = document.getElementById('btnViewOrder');

        // Ẩn tất cả các nút điều khiển trước khi phân bổ lại
        btnQuickOpen.classList.add('hidden');
        btnCancelRes.classList.add('hidden');
        btnResetTable.classList.add('hidden');
        btnViewOrder.classList.add('hidden');

        // 5. Phân bổ hiển thị nút bấm dựa vào trạng thái bàn
        if (statusNorm === 'empty' || statusNorm === 'trống') {
            statusLabel.innerText = "Trống (Sẵn sàng)";
            statusLabel.className = "font-bold text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full text-[11px]";
            btnQuickOpen.classList.remove('hidden'); 
        } 
        else if (statusNorm === 'reserved' || statusNorm === 'đã đặt' || statusNorm === 'đã đặt trước') {
            statusLabel.innerText = "Đã đặt trước";
            statusLabel.className = "font-bold text-amber-600 bg-amber-50 px-2.5 py-0.5 rounded-full text-[11px]";
            btnCancelRes.classList.remove('hidden'); 
        } 
        else {
            statusLabel.innerText = "Đang ngồi phục vụ";
            statusLabel.className = "font-bold text-rose-600 bg-rose-50 px-2.5 py-0.5 rounded-full text-[11px]";
            btnResetTable.classList.remove('hidden'); 
            btnViewOrder.classList.remove('hidden');  
        }

        // Bật hiển thị Popup
        document.getElementById('tableDetailModal').classList.remove('hidden');
    }

    // Các hàm kích hoạt gửi Form ẩn
    function executeQuickOpen() {
        if(activeSelectedTableId && confirm("Xác nhận mở bàn này cho khách ngồi trực tiếp?")) {
            submitHiddenForm('quickOpen', activeSelectedTableId);
        }
    }

    function executeCancelReservation() {
        if(activeSelectedTableId && confirm("Bạn có chắc chắn muốn HỦY LỊCH HẸN và đưa bàn này quay về trạng thái TRỐNG không?")) {
            submitHiddenForm('cancelReservation', activeSelectedTableId);
        }
    }

    function executeResetTable() {
        if(activeSelectedTableId && confirm("Xác nhận hành động: Khách đã thanh toán rời đi, dọn bàn và ĐƯA BÀN VỀ TRẠNG THÁI TRỐNG?")) {
            submitHiddenForm('resetTable', activeSelectedTableId);
        }
    }

    function submitHiddenForm(actionValue, tableIdValue) {
        document.getElementById('hiddenActionField').value = actionValue;
        document.getElementById('hiddenTableIdField').value = tableIdValue;
        document.getElementById('hiddenActionForm').submit();
    }

    function goToOrderPage() {
        if(activeSelectedTableId) {
            window.location.href = "${pageContext.request.contextPath}/manager-orders?tableId=" + activeSelectedTableId;
        }
    }

    // Hàm in mã QR đầy đủ, sạch lỗi đóng ngoặc nhọn
    function printQRCode() {
        if(!activeSelectedQRUrl) return;
        const printWindow = window.open('', '_blank', 'width=400,height=400');
        printWindow.document.write('<html><body style="text-align:center;padding-top:40px;">');
        printWindow.document.write('<h2>' + document.getElementById('modalTableTitle').innerText + '</h2>');
        printWindow.document.write('<img src="' + document.getElementById('modalQRImage').src + '" style="width:250px;height=250px;"/>');
        printWindow.document.write('<p style="font-family:monospace;font-size:12px;">' + activeSelectedQRUrl + '</p>');
        printWindow.document.write('<script>window.onload = function() { window.print(); window.close(); };<\/script>');
        printWindow.document.write('</body></html>');
        printWindow.document.close();
    }
</script>

<jsp:include page="/common/footer.jsp" />