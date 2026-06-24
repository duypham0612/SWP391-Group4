<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/sidebar.jsp" />

<%
    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null) ? loggedInUser.getFullName() : "Thu ngân";
%>

<div class="space-y-8 fade-in w-full px-4 mt-4">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Ca làm việc của <%= fullName %></h1>
            <p class="text-xs text-slate-400 font-medium mt-1">Hôm nay là một ngày tuyệt vời để bùng nổ doanh số!</p>
        </div>

        <div class="flex items-center gap-3">
            <a href="${pageContext.request.contextPath}/pos?action=open_table&tableId=1"
               class="flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold transition-all shadow-md shadow-emerald-600/20">
                <i class="fa-solid fa-cash-register"></i>
                <span>Tạo đơn tại quầy ngay</span>
            </a>
        </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-36 cursor-pointer hover:border-blue-300 transition-colors" onclick="window.location.href='${pageContext.request.contextPath}/cashier-orders'">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Đơn hàng trong ca</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1">-- <span class="text-xs font-semibold text-slate-400">đơn</span></h3>
                </div>
                <div class="w-10 h-10 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center text-lg">
                    <i class="fa-solid fa-receipt"></i>
                </div>
            </div>
            <div class="text-[11px] font-bold text-blue-500 flex items-center gap-1">Xem danh sách Đơn hàng <i class="fa-solid fa-arrow-right"></i></div>
        </div>

        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-36">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Doanh thu tạm tính</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1">--<span class="text-xs font-semibold text-slate-400">đ</span></h3>
                </div>
                <div class="w-10 h-10 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center text-lg">
                    <i class="fa-solid fa-money-bill-wave"></i>
                </div>
            </div>
            <div class="text-[11px] font-bold text-emerald-500">Tiền mặt & Chuyển khoản</div>
        </div>

        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-36 cursor-pointer hover:border-amber-300 transition-colors" onclick="window.location.href='${pageContext.request.contextPath}/cashier-tables'">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tình trạng Sảnh</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1">Xem Sơ đồ bàn</h3>
                </div>
                <div class="w-10 h-10 rounded-full bg-amber-50 text-amber-500 flex items-center justify-center text-lg">
                    <i class="fa-solid fa-couch"></i>
                </div>
            </div>
            <div class="text-[11px] font-bold text-amber-500 flex items-center gap-1">
                <span>Quản lý & Nhận bàn khách đặt</span> <i class="fa-solid fa-arrow-right"></i>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/common/footer.jsp" />