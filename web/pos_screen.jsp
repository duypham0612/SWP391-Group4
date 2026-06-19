<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Product"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="com.mycoffee.model.OrderDetail"%>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<%
    Order order = (Order) request.getAttribute("order");
    List<OrderDetail> details = (List<OrderDetail>) request.getAttribute("orderDetails");
    List<Product> products = (List<Product>) request.getAttribute("products");
%>

<div class="max-w-[1400px] mx-auto p-4 flex flex-col lg:flex-row gap-6 h-[calc(100vh-80px)] fade-in">

    <div class="w-full lg:w-[65%] flex flex-col space-y-4">
        <div class="flex items-center justify-between bg-white p-4 rounded-2xl shadow-sm border border-slate-200">
            <h2 class="text-lg font-bold text-slate-800">Chọn món (POS)</h2>
            <a href="pos-tables" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-all">
                <i class="fa-solid fa-arrow-left mr-1"></i> Quay lại Sơ đồ bàn
            </a>
        </div>

        <div class="flex-1 overflow-y-auto pr-2 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            <% if (products != null) { for (Product p : products) { %>
                <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= p.getProductId() %>&quantity=1&price=<%= p.getBasePrice() %>"
                   class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm hover:border-[#006064] hover:shadow-md transition-all flex flex-col items-center text-center cursor-pointer group">
                    <div class="w-16 h-16 rounded-full bg-[#006064]/10 text-[#006064] flex items-center justify-center text-2xl mb-3 group-hover:scale-110 transition-transform">
                        <i class="fa-solid fa-mug-hot"></i>
                    </div>
                    <h3 class="text-xs font-bold text-slate-800 line-clamp-2"><%= p.getProductName() %></h3>
                    <p class="text-[11px] font-extrabold text-orange-600 mt-2"><%= String.format("%,.0f", p.getBasePrice()) %>đ</p>
                </a>
            <% } } %>
        </div>
    </div>

    <div class="w-full lg:w-[35%] bg-white rounded-3xl shadow-lg border border-slate-200 flex flex-col overflow-hidden h-full">
        <div class="bg-[#006064] p-5 text-white flex justify-between items-center">
            <div>
                <p class="text-[10px] font-medium text-teal-100 uppercase tracking-widest">Đơn hàng #<%= order.getOrderId() %></p>
                <h3 class="text-lg font-bold mt-1"><%= order.getOrderType() %></h3> </div>
            <div class="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-xl">
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
                    <div class="flex-1">
                        <h4 class="text-xs font-bold text-slate-800"><%= od.getNote() %></h4>
                        <p class="text-[10px] text-slate-500 font-medium"><%= String.format("%,.0f", od.getUnitPrice()) %>đ</p>
                    </div>
                    <div class="flex items-center gap-2 bg-white rounded-lg border border-slate-200 p-1">
                        <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= od.getProductId() %>&quantity=-1&price=<%= od.getUnitPrice() %>" class="w-6 h-6 flex items-center justify-center rounded bg-slate-100 hover:bg-red-100 hover:text-red-600 text-xs font-bold transition-colors">-</a>
                        <span class="text-xs font-extrabold w-5 text-center"><%= od.getQuantity() %></span>
                        <a href="pos?action=add_item&orderId=<%= order.getOrderId() %>&productId=<%= od.getProductId() %>&quantity=1&price=<%= od.getUnitPrice() %>" class="w-6 h-6 flex items-center justify-center rounded bg-slate-100 hover:bg-[#006064] hover:text-white text-xs font-bold transition-colors">+</a>
                    </div>
                </div>
            <% } } %>
        </div>

        <div class="p-5 bg-slate-50 border-t border-slate-200">
            <div class="flex justify-between items-center mb-4">
                <span class="text-sm font-bold text-slate-600">Tổng cộng:</span>
                <span class="text-xl font-extrabold text-red-600"><%= String.format("%,.0f", order.getTotalAmount()) %>đ</span>
            </div>
            <button class="w-full py-4 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white font-bold text-sm shadow-md transition-all flex items-center justify-center gap-2">
                <i class="fa-solid fa-money-bill-wave"></i> Tiến hành Thanh toán
            </button>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp" />