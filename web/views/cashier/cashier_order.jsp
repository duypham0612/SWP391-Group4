<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="java.text.SimpleDateFormat"%>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/sidebar.jsp" />

<%
    List<Order> orderList = (List<Order>) request.getAttribute("orderList");
    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
%>

<div class="space-y-8 fade-in w-full px-4 mt-2">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Quản Lý Đơn Hàng Hôm Nay</h1>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi các đơn đang Order, Chưa thanh toán hoặc Đã hoàn thành.</p>
        </div>

        <a href="${pageContext.request.contextPath}/pos?action=open_table&tableId=1" class="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-md self-start md:self-auto">
            <i class="fa-solid fa-plus text-xs"></i>
            <span>Tạo Order mới</span>
        </a>
    </div>

    <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm overflow-hidden">
        <div class="overflow-x-auto p-2">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider bg-slate-50 rounded-t-2xl">
                        <th class="py-4 px-6 rounded-tl-xl">Mã Đơn</th>
                        <th class="py-4 px-4">Thời Gian</th>
                        <th class="py-4 px-4">Loại / Tên Bàn</th>
                        <th class="py-4 px-4 text-right">Tổng Tiền</th>
                        <th class="py-4 px-6 text-center">Trạng Thái</th>
                        <th class="py-4 px-6 text-center rounded-tr-xl">Thao Tác</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <%
                        if (orderList != null && !orderList.isEmpty()) {
                            for(Order o : orderList) {
                                String status = o.getOrderStatus();
                                String badgeClass = "bg-slate-100 text-slate-600";
                                String textStatus = status;

                                if("Pending".equalsIgnoreCase(status)) {
                                    badgeClass = "bg-amber-50 text-amber-600 border border-amber-200";
                                    textStatus = "Chưa thanh toán (Đang Order)";
                                } else if("Completed".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                                    badgeClass = "bg-emerald-50 text-emerald-600 border border-emerald-200";
                                    textStatus = "Đã thanh toán (Hoàn thành)";
                                } else if("Cancelled".equalsIgnoreCase(status)) {
                                    badgeClass = "bg-rose-50 text-rose-600 border border-rose-200";
                                    textStatus = "Đã hủy";
                                }
                    %>
                    <tr class="hover:bg-slate-50/60 transition-colors">
                        <td class="py-4 px-6 font-bold text-slate-800">#<%= o.getOrderId() %></td>
                        <td class="py-4 px-4 text-slate-400"><%= (o.getOrderDate() != null) ? sdfTime.format(o.getOrderDate()) : "" %></td>
                        <td class="py-4 px-4 font-bold text-[#006064]"><%= o.getOrderType() %></td>
                        <td class="py-4 px-4 text-right font-extrabold text-slate-800"><%= String.format("%,.0f", o.getFinalAmount()) %>đ</td>
                        <td class="py-4 px-6 text-center">
                            <span class="px-3 py-1 rounded-full text-[10px] font-bold <%= badgeClass %>"><%= textStatus %></span>
                        </td>
                        <td class="py-4 px-6 text-center flex items-center justify-center gap-2">
                            <% if("Pending".equalsIgnoreCase(status)) { %>
                                <a href="${pageContext.request.contextPath}/pos?action=view&orderId=<%= o.getOrderId() %>" class="px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-600 text-emerald-600 hover:text-white font-bold transition-colors shadow-sm">
                                    Thanh toán
                                </a>
                            <% } else { %>
                                <a href="${pageContext.request.contextPath}/pos?action=receipt&orderId=<%= o.getOrderId() %>" class="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold transition-colors shadow-sm">
                                    Biên lai
                                </a>
                            <% } %>
                        </td>
                    </tr>
                    <%      }
                        } else {
                    %>
                        <tr><td colspan="6" class="py-12 text-center text-slate-400 font-medium">Chưa có đơn hàng nào hôm nay.</td></tr>
                    <%  } %>
                </tbody>
            </table>
        </div>
    </div>
</div>

<jsp:include page="/common/footer.jsp" />