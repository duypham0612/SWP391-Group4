<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Product"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="com.mycoffee.model.OrderDetail"%>
<%@page import="com.mycoffee.model.Voucher"%>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<%
    Order order = (Order) request.getAttribute("order");
    List<OrderDetail> details = (List<OrderDetail>) request.getAttribute("orderDetails");
    List<Product> products = (List<Product>) request.getAttribute("products");
    List<Voucher> availableVouchers = (List<Voucher>) request.getAttribute("availableVouchers");
%>

<div class="max-w-[1400px] mx-auto p-4 flex flex-col lg:flex-row gap-6 h-[calc(100vh-80px)] fade-in">

    <div class="w-full lg:w-[65%] flex flex-col space-y-4">
        <div class="flex items-center justify-between bg-white p-4 rounded-2xl shadow-sm border border-slate-200">
            <h2 class="text-lg font-bold text-slate-800">Chọn món (POS)</h2>
            <div class="flex gap-2">
                <a href="pos?action=cancel_order&orderId=<%= order.getOrderId() %>&tableId=<%= order.getTableId() %>"
                   onclick="return confirm('Bạn có chắc chắn muốn hủy bàn này không? Đơn hàng sẽ bị hủy bỏ!');"
                   class="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold rounded-xl transition-all shadow-sm">
                    <i class="fa-solid fa-trash mr-1"></i> Hủy bàn
                </a>
                <a href="pos-tables" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all shadow-sm">
                    <i class="fa-solid fa-arrow-left mr-1"></i> Quay lại sơ đồ
                </a>
            </div>
        </div>

        <div class="flex-1 overflow-y-auto pr-2 pb-4">
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-4">
                <% if (products != null) { for (Product p : products) { %>
                    <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= p.getProductId() %>&quantity=1&price=<%= p.getBasePrice() %>"
                       class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm hover:border-[#006064] hover:shadow-md transition-all flex flex-col items-center justify-between text-center cursor-pointer group min-h-[160px] h-full">
                        <div class="w-14 h-14 shrink-0 rounded-full bg-[#006064]/10 text-[#006064] flex items-center justify-center text-2xl mb-3 group-hover:scale-110 transition-transform">
                            <i class="fa-solid fa-mug-hot"></i>
                        </div>
                        <div class="flex-1 flex flex-col justify-center w-full">
                            <h3 class="text-xs font-bold text-slate-800 line-clamp-2 leading-snug"><%= p.getProductName() %></h3>
                        </div>
                        <p class="text-[13px] font-extrabold text-orange-600 mt-2 shrink-0"><%= String.format("%,.0f", p.getBasePrice()) %>đ</p>
                    </a>
                <% } } %>
            </div>
        </div>
    </div>

    <div class="w-full lg:w-[35%] bg-white rounded-3xl shadow-lg border border-slate-200 flex flex-col overflow-hidden h-full">
        <div class="bg-[#006064] p-5 text-white flex justify-between items-center shrink-0">
            <div>
                <p class="text-[10px] font-medium text-teal-100 uppercase tracking-widest">Đơn hàng #<%= order.getOrderId() %></p>
                <h3 class="text-lg font-bold mt-1"><%= order.getOrderType() %></h3>
            </div>
            <div class="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-xl shrink-0">
                <i class="fa-solid fa-receipt"></i>
            </div>
        </div>

        <div class="flex-1 overflow-y-auto p-4 space-y-3">
            <% if (details == null || details.isEmpty()) { %>
                <div class="h-full flex flex-col items-center justify-center text-slate-400">
                    <i class="fa-solid fa-cart-shopping text-4xl mb-3 opacity-50"></i>
                    <p class="text-xs font-bold">Chưa có món nào được chọn</p>
                </div>
            <% } else { for (OrderDetail od : details) { %>
                <div class="flex items-center justify-between p-3 rounded-xl border border-slate-100 bg-slate-50">
                    <div class="flex-1 pr-2">
                        <h4 class="text-xs font-bold text-slate-800"><%= od.getNote() %></h4>
                        <p class="text-[10px] text-slate-500 font-medium mt-0.5"><%= String.format("%,.0f", od.getUnitPrice()) %>đ</p>
                    </div>
                    <div class="flex items-center gap-2 bg-white rounded-lg border border-slate-200 p-1 shrink-0">
                        <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= od.getProductId() %>&quantity=-1&price=<%= od.getUnitPrice() %>" class="w-6 h-6 flex items-center justify-center rounded bg-slate-100 hover:bg-red-100 hover:text-red-600 text-xs font-bold transition-colors">-</a>
                        <span class="text-xs font-extrabold w-5 text-center"><%= od.getQuantity() %></span>
                        <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= od.getProductId() %>&quantity=1&price=<%= od.getUnitPrice() %>" class="w-6 h-6 flex items-center justify-center rounded bg-slate-100 hover:bg-[#006064] hover:text-white text-xs font-bold transition-colors">+</a>
                    </div>
                </div>
            <% } } %>
        </div>

        <div class="p-5 bg-slate-50 border-t border-slate-200 shrink-0">
            <% if (availableVouchers != null && !availableVouchers.isEmpty()) { %>
            <form action="pos" method="GET" class="flex gap-2 mb-4 items-center relative" id="voucherDropdownContainer">
                <input type="hidden" name="action" value="apply_voucher">
                <input type="hidden" name="orderId" value="<%= order.getOrderId() %>">
                <input type="hidden" name="voucherCode" id="hiddenVoucherCode" value="">

                <div class="relative flex-1 min-w-0">
                    <button type="button" onclick="toggleVoucherDropdown()" class="w-full flex items-center justify-between bg-white border border-slate-200 px-3 py-2.5 rounded-xl text-[11px] font-bold text-slate-600 shadow-sm focus:outline-none hover:border-emerald-500 transition-all">
                        <div class="flex items-center gap-2 overflow-hidden flex-1">
                            <i class="fa-solid fa-ticket text-emerald-500 shrink-0"></i>
                            <span id="voucher-text" class="truncate">-- Mời chọn Voucher --</span>
                        </div>
                        <i class="fa-solid fa-chevron-down text-[10px] ml-2 shrink-0 transition-transform duration-200" id="voucher-icon"></i>
                    </button>

                    <div id="voucher-menu" class="hidden absolute bottom-full mb-2 left-0 w-full bg-white rounded-xl shadow-lg border border-slate-100 z-50 overflow-hidden transition-all duration-200 opacity-0 transform scale-95 origin-bottom">
                        <ul class="max-h-48 overflow-y-auto py-1 text-xs text-slate-600 font-medium">
                            <% for(Voucher v : availableVouchers) {
                                String maxDesc = (v.getMaxDiscount() != null && v.getMaxDiscount() > 0) ? " (tối đa " + String.format("%,.0f", v.getMaxDiscount()) + "k)" : "";
                                String displayDiscount = v.isIsPercentage() ?
                                        String.format("%,.0f", v.getDiscountValue()) + "%" + maxDesc
                                        : String.format("%,.0f", v.getDiscountValue()) + "đ";
                            %>
                            <li>
                                <a href="#" onclick="selectVoucher('<%= v.getVoucherCode() %>', 'Mã: <%= v.getVoucherCode() %> (-<%= displayDiscount %>)', event)" class="block px-3 py-2.5 hover:bg-emerald-50 hover:text-emerald-600 transition-colors border-b border-slate-50 last:border-0 truncate">
                                    <strong><%= v.getVoucherCode() %></strong>
                                    <span class="text-slate-400 font-normal ml-1">(-<%= displayDiscount %>)</span>
                                </a>
                            </li>
                            <% } %>
                        </ul>
                    </div>
                </div>

                <button type="submit" class="shrink-0 bg-emerald-50 hover:bg-emerald-100 text-emerald-600 px-4 py-2.5 rounded-xl text-xs font-bold transition-colors border border-emerald-100 shadow-sm">
                    Áp dụng
                </button>
            </form>
            <% } %>

            <div class="space-y-2 mb-4">
                <div class="flex justify-between text-[11px] font-bold text-slate-400">
                    <span>Tạm tính:</span>
                    <span><%= String.format("%,.0f", order.getTotalAmount()) %>đ</span>
                </div>
                <div class="flex justify-between text-[11px] font-bold text-emerald-500 border-b border-slate-200/60 pb-2">
                    <span>Chiết khấu Voucher:</span>
                    <span>- <%= String.format("%,.0f", order.getDiscountAmount()) %>đ</span>
                </div>
                <div class="flex justify-between text-sm font-extrabold text-slate-800 pt-1">
                    <span>Tổng thanh toán:</span>
                    <span class="text-xl text-red-600"><%= String.format("%,.0f", order.getFinalAmount()) %>đ</span>
                </div>
            </div>

            <button class="w-full py-4 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white font-bold text-sm shadow-md transition-all flex items-center justify-center gap-2">
                <i class="fa-solid fa-money-bill-wave"></i> Tiến hành Thanh toán
            </button>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp" />

<script>
    function toggleVoucherDropdown() {
        const menu = document.getElementById('voucher-menu');
        const icon = document.getElementById('voucher-icon');

        if (menu.classList.contains('hidden')) {
            menu.classList.remove('hidden');
            icon.classList.add('rotate-180');
            setTimeout(() => {
                menu.classList.remove('opacity-0', 'scale-95');
                menu.classList.add('opacity-100', 'scale-100');
            }, 10);
        } else {
            closeVoucherDropdown();
        }
    }

    function closeVoucherDropdown() {
        const menu = document.getElementById('voucher-menu');
        const icon = document.getElementById('voucher-icon');

        if(menu) {
            menu.classList.remove('opacity-100', 'scale-100');
            menu.classList.add('opacity-0', 'scale-95');
            icon.classList.remove('rotate-180');

            setTimeout(() => {
                menu.classList.add('hidden');
            }, 200);
        }
    }

    function selectVoucher(code, displayText, event) {
        event.preventDefault();

        // Cập nhật text hiển thị trên UI
        document.getElementById('voucher-text').innerText = displayText;

        // Gắn mã code vào thẻ input hidden để gửi xuống Controller
        document.getElementById('hiddenVoucherCode').value = code;

        closeVoucherDropdown();
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function(e) {
        const container = document.getElementById('voucherDropdownContainer');
        if (container && !container.contains(e.target)) {
            closeVoucherDropdown();
        }
    });
</script>