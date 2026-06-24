<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Order"%>
<%@page import="com.mycoffee.model.OrderDetail"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Biên lai thanh toán - #<%= ((Order)request.getAttribute("order")).getOrderId() %></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://site-assets.fontawesome.com/releases/v6.5.1/css/all.css">
    <style>
        @media print {
            body { background: white; color: black; padding: 0; margin: 0; }
            .no-print { display: none !important; }
            .print-card { box-shadow: none !important; border: none !important; width: 100% !important; max-width: 100% !important; padding: 0 !important; }
        }
    </style>
</head>
<body class="bg-slate-100 font-sans antialiased min-h-screen flex flex-col items-center justify-center p-4">

<%
    Order order = (Order) request.getAttribute("order");
    List<OrderDetail> details = (List<OrderDetail>) request.getAttribute("orderDetails");
%>

    <div class="print-card bg-white w-full max-w-md rounded-2xl shadow-xl border border-slate-200 p-6 space-y-6">
        <div class="text-center space-y-1">
            <h1 class="text-xl font-black text-slate-800 tracking-tight">MY COFFEE HOUSE</h1>
            <p class="text-xs text-slate-500 font-medium">Chi nhánh Cầu Giấy, Hà Nội</p>
            <div class="border-b-2 border-dashed border-slate-200 pt-3"></div>
        </div>

        <div class="grid grid-cols-2 gap-y-2 text-xs text-slate-600 font-medium">
            <div>Mã hóa đơn: <span class="font-bold text-slate-800">#<%= order.getOrderId() %></span></div>
            <div class="text-right">Bàn: <span class="font-bold text-slate-800"><%= order.getOrderType() %></span></div>
            <div class="text-right col-span-2">Trạng thái: <span class="text-emerald-600 font-bold">Đã thanh toán</span></div>
        </div>

        <div class="border-b border-slate-100"></div>

        <div class="space-y-3">
            <h3 class="text-xs font-black text-slate-400 uppercase tracking-wider">Chi tiết món</h3>
            <div class="space-y-2.5">
                <% if (details != null) { for (OrderDetail od : details) { %>
                    <div class="flex justify-between items-start text-xs">
                        <div class="flex-1 pr-4">
                            <h4 class="font-bold text-slate-800"><%= od.getNote() %></h4>
                            <p class="text-[11px] text-slate-400 font-medium"><%= od.getQuantity() %> x <%= String.format("%,.0f", od.getUnitPrice()) %>đ</p>
                        </div>
                        <span class="font-extrabold text-slate-700 mt-1"><%= String.format("%,.0f", od.getQuantity() * od.getUnitPrice()) %>đ</span>
                    </div>
                <% } } %>
            </div>
        </div>

        <div class="border-b-2 border-dashed border-slate-200"></div>

        <div class="space-y-2 text-xs">
            <div class="flex justify-between font-medium text-slate-500">
                <span>Tạm tính:</span><span><%= String.format("%,.0f", order.getTotalAmount()) %>đ</span>
            </div>
            <div class="flex justify-between font-medium text-emerald-600">
                <span>Chiết khấu:</span><span>-<%= String.format("%,.0f", order.getDiscountAmount()) %>đ</span>
            </div>
            <div class="flex justify-between text-sm font-black text-slate-800 pt-1">
                <span>TỔNG THANH TOÁN:</span><span class="text-base text-red-600"><%= String.format("%,.0f", order.getFinalAmount()) %>đ</span>
            </div>
        </div>

        <div class="border-b border-slate-100"></div>

        <div class="text-center space-y-1">
            <p class="text-xs font-bold text-slate-700">Cảm ơn Quý khách - Hẹn gặp lại!</p>
        </div>
    </div>

    <div class="no-print w-full max-w-md flex gap-3 mt-6">
        <button onclick="window.print();" class="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-2">
            <i class="fa-solid fa-print"></i> In hóa đơn
        </button>
        <a href="${pageContext.request.contextPath}/cashier-dashboard" class="flex-1 py-3 bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-xs rounded-xl shadow-sm transition-all flex items-center justify-center gap-2 text-center">
            <i class="fa-solid fa-house"></i> Quay lại
        </a>
    </div>
</body>
</html>
