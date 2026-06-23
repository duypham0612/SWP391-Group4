<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/sidebar_admin.jsp" />

<%
    User adminUser = (User) session.getAttribute("user");
    String adminName = (adminUser != null) ? adminUser.getFullName() : "Admin";
%>

<%-- Wrapper content (sidebar_admin.jsp không mở thẻ này nên ta mở ở đây) --%>
<div class="flex-1 flex flex-col overflow-hidden">

    <%-- Topbar --%>
    <header class="h-[72px] bg-white border-b border-slate-200 flex items-center justify-between px-8 shadow-sm z-10">
        <div>
            <h1 class="text-base font-bold text-slate-800">Dashboard — Tổng hệ thống</h1>
            <p class="text-[11px] text-slate-400 font-medium mt-0.5">Xem toàn bộ hoạt động của chuỗi My Coffee House</p>
        </div>
        <div class="flex items-center gap-3">
            <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 px-4 py-2 rounded-xl text-xs font-semibold text-slate-600">
                <i class="fa-regular fa-calendar-days text-violet-500"></i>
                <span id="admin-date"></span>
            </div>
            <a href="${pageContext.request.contextPath}/login?action=logout"
               class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100">
                <i class="fa-solid fa-right-from-bracket"></i>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <%-- Main content --%>
    <main class="flex-1 overflow-y-auto p-8 bg-[#f4f7fc]/60">

        <%-- Welcome --%>
        <div class="mb-8">
            <h2 class="text-2xl font-bold text-slate-800">Xin chào, <%= adminName %>! 👋</h2>
            <p class="text-sm text-slate-400 font-medium mt-1">Đây là tổng quan toàn hệ thống chuỗi My Coffee House.</p>
        </div>

        <%-- KPI Cards --%>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-6 mb-8">

            <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
                <div class="flex items-center justify-between mb-3">
                    <span class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tổng chi nhánh</span>
                    <div class="w-8 h-8 rounded-xl bg-violet-100 flex items-center justify-center">
                        <i class="fa-solid fa-store text-violet-600 text-xs"></i>
                    </div>
                </div>
                <p class="text-2xl font-extrabold text-slate-800">2</p>
                <p class="text-[10px] text-slate-400 font-medium mt-1">Chi nhánh đang hoạt động</p>
            </div>

            <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
                <div class="flex items-center justify-between mb-3">
                    <span class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tổng tài khoản</span>
                    <div class="w-8 h-8 rounded-xl bg-sky-100 flex items-center justify-center">
                        <i class="fa-solid fa-users text-sky-600 text-xs"></i>
                    </div>
                </div>
                <p class="text-2xl font-extrabold text-slate-800">—</p>
                <p class="text-[10px] text-slate-400 font-medium mt-1">Nhân viên + Khách hàng</p>
            </div>

            <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
                <div class="flex items-center justify-between mb-3">
                    <span class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Doanh thu hôm nay</span>
                    <div class="w-8 h-8 rounded-xl bg-emerald-100 flex items-center justify-center">
                        <i class="fa-solid fa-sack-dollar text-emerald-600 text-xs"></i>
                    </div>
                </div>
                <p class="text-2xl font-extrabold text-slate-800">—</p>
                <p class="text-[10px] text-emerald-500 font-bold mt-1">Toàn chuỗi</p>
            </div>

            <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
                <div class="flex items-center justify-between mb-3">
                    <span class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Order đang xử lý</span>
                    <div class="w-8 h-8 rounded-xl bg-amber-100 flex items-center justify-center">
                        <i class="fa-solid fa-receipt text-amber-600 text-xs"></i>
                    </div>
                </div>
                <p class="text-2xl font-extrabold text-slate-800">—</p>
                <p class="text-[10px] text-amber-500 font-bold mt-1">Realtime</p>
            </div>
        </div>

        <%-- Quick Actions --%>
        <div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
            <h3 class="text-sm font-bold text-slate-700 uppercase tracking-wide mb-4">Truy cập nhanh</h3>
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">

                <a href="${pageContext.request.contextPath}/admin-branches"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 hover:bg-violet-50 hover:border-violet-200 border border-slate-200 transition-all group">
                    <div class="w-10 h-10 rounded-xl bg-white border border-slate-200 group-hover:bg-violet-100 flex items-center justify-center transition-all">
                        <i class="fa-solid fa-code-branch text-slate-500 group-hover:text-violet-600 text-sm"></i>
                    </div>
                    <span class="text-[11px] font-bold text-slate-600 group-hover:text-violet-700 text-center">Chi nhánh</span>
                </a>

                <a href="${pageContext.request.contextPath}/admin-accounts"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 hover:bg-sky-50 hover:border-sky-200 border border-slate-200 transition-all group">
                    <div class="w-10 h-10 rounded-xl bg-white border border-slate-200 group-hover:bg-sky-100 flex items-center justify-center transition-all">
                        <i class="fa-solid fa-user-pen text-slate-500 group-hover:text-sky-600 text-sm"></i>
                    </div>
                    <span class="text-[11px] font-bold text-slate-600 group-hover:text-sky-700 text-center">Tài khoản</span>
                </a>

                <a href="${pageContext.request.contextPath}/admin-menu"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 hover:bg-orange-50 hover:border-orange-200 border border-slate-200 transition-all group">
                    <div class="w-10 h-10 rounded-xl bg-white border border-slate-200 group-hover:bg-orange-100 flex items-center justify-center transition-all">
                        <i class="fa-solid fa-book-open text-slate-500 group-hover:text-orange-600 text-sm"></i>
                    </div>
                    <span class="text-[11px] font-bold text-slate-600 group-hover:text-orange-700 text-center">Menu</span>
                </a>

                <a href="${pageContext.request.contextPath}/admin-voucher"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 hover:bg-pink-50 hover:border-pink-200 border border-slate-200 transition-all group">
                    <div class="w-10 h-10 rounded-xl bg-white border border-slate-200 group-hover:bg-pink-100 flex items-center justify-center transition-all">
                        <i class="fa-solid fa-ticket text-slate-500 group-hover:text-pink-600 text-sm"></i>
                    </div>
                    <span class="text-[11px] font-bold text-slate-600 group-hover:text-pink-700 text-center">Voucher</span>
                </a>

                <a href="${pageContext.request.contextPath}/admin-report"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl bg-slate-50 hover:bg-emerald-50 hover:border-emerald-200 border border-slate-200 transition-all group">
                    <div class="w-10 h-10 rounded-xl bg-white border border-slate-200 group-hover:bg-emerald-100 flex items-center justify-center transition-all">
                        <i class="fa-solid fa-chart-column text-slate-500 group-hover:text-emerald-600 text-sm"></i>
                    </div>
                    <span class="text-[11px] font-bold text-slate-600 group-hover:text-emerald-700 text-center">Báo cáo</span>
                </a>
            </div>
        </div>

    </main>
</div>

<script>
    const d = new Date();
    document.getElementById('admin-date').innerText = d.toLocaleDateString('vi-VN', {
        weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric'
    });
</script>

<%@include file="/common/footer.jsp" %>
