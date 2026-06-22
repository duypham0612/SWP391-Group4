<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="nav" value="${empty nav ? 'dashboard' : nav}"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="onCls" value="bg-sky-50 text-sky-600"/>
<c:set var="offCls" value="text-slate-500 hover:bg-slate-100 hover:text-slate-800"/>
<c:set var="baseCls" value="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200"/>

<c:choose>
    <c:when test="${nav == 'board'}"><c:set var="pageTitle" value="Hàng chờ pha chế"/></c:when>
    <c:when test="${nav == 'products'}"><c:set var="pageTitle" value="Quản lý sản phẩm"/></c:when>
    <c:when test="${nav == 'history'}"><c:set var="pageTitle" value="Lịch sử order"/></c:when>
    <c:when test="${nav == 'report'}"><c:set var="pageTitle" value="Báo cáo thống kê"/></c:when>
    <c:when test="${nav == 'detail'}"><c:set var="pageTitle" value="Chi tiết order"/></c:when>
    <c:otherwise><c:set var="pageTitle" value="Tổng quan pha chế"/></c:otherwise>
</c:choose>

<c:set var="baristaName" value="${empty sessionScope.user ? 'Barista' : sessionScope.user.fullName}"/>
<c:set var="nameParts" value="${fn:split(baristaName, ' ')}"/>
<c:set var="baristaShort" value="${empty nameParts ? 'BA' : fn:toUpperCase(nameParts[fn:length(nameParts) - 1])}"/>

<aside class="w-64 bg-slate-50 border-r border-slate-200/60 flex flex-col h-full z-20">
    <div class="h-20 flex items-center gap-3 px-6 border-b border-slate-200/60">
        <div class="w-9 h-9 rounded-xl bg-[#006064] flex items-center justify-center text-white shadow-md shadow-[#006064]/20">
            <i class="fa-solid fa-mug-hot text-base"></i>
        </div>
        <div>
            <h4 class="text-sm font-bold tracking-tight text-slate-800 leading-tight">Coffee POS</h4>
            <p class="text-[10px] text-slate-400 font-medium">Khu vực pha chế</p>
        </div>
    </div>

    <nav class="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
        <a href="${ctx}/barista" class="${baseCls} ${nav == 'dashboard' ? onCls : offCls}">
            <i class="fa-solid fa-grid-2-dash text-sm w-4 text-center ${nav == 'dashboard' ? 'text-sky-600' : 'text-slate-400'}"></i>
            <span>Tổng quan</span>
        </a>
        <a href="${ctx}/barista-board" class="${baseCls} ${nav == 'board' ? onCls : offCls}">
            <i class="fa-solid fa-blender text-sm w-4 text-center ${nav == 'board' ? 'text-sky-600' : 'text-slate-400'}"></i>
            <span>Hàng chờ pha chế</span>
        </a>
<!--        <a href="${ctx}/barista-display" target="_blank" class="${baseCls} ${offCls}">
            <i class="fa-solid fa-tv text-sm w-4 text-center text-slate-400"></i>
            <span>Màn gọi món</span>
            <i class="fa-solid fa-arrow-up-right-from-square text-[8px] ml-auto text-slate-300"></i>
        </a>-->
        <a href="${ctx}/barista-products" class="${baseCls} ${nav == 'products' ? onCls : offCls}">
            <i class="fa-solid fa-box-open text-sm w-4 text-center ${nav == 'products' ? 'text-sky-600' : 'text-slate-400'}"></i>
            <span>Sản phẩm / Báo hết</span>
        </a>
        <a href="${ctx}/barista-history" class="${baseCls} ${nav == 'history' ? onCls : offCls}">
            <i class="fa-solid fa-clock-rotate-left text-sm w-4 text-center ${nav == 'history' ? 'text-sky-600' : 'text-slate-400'}"></i>
            <span>Lịch sử order</span>
        </a>
        <a href="${ctx}/barista-report" class="${baseCls} ${nav == 'report' ? onCls : offCls}">
            <i class="fa-solid fa-chart-simple text-sm w-4 text-center ${nav == 'report' ? 'text-sky-600' : 'text-slate-400'}"></i>
            <span>Báo cáo thống kê</span>
        </a>
    </nav>

    <div class="p-4 border-t border-slate-200/60 bg-slate-50/50">
        <div class="flex items-center gap-3 bg-white border border-slate-200/50 p-2.5 rounded-2xl shadow-sm">
            <div class="w-9 h-9 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shadow-inner">
                ${baristaShort}
            </div>
            <div class="min-w-0 flex-1">
                <h5 class="text-xs font-bold text-slate-800 truncate">${baristaName}</h5>
                <p class="text-[10px] text-slate-400 font-medium truncate">Nhân viên pha chế</p>
            </div>
        </div>
    </div>
</aside>

<div class="flex-1 flex flex-col overflow-hidden">
    <header class="h-20 bg-white border-b border-slate-200/40 flex items-center justify-between px-8 shadow-sm z-10">
        <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-[#006064]/10 flex items-center justify-center text-[#006064]">
                <i class="fa-solid fa-blender text-base"></i>
            </div>
            <div>
                <h1 class="text-base font-bold text-slate-800 tracking-tight">${pageTitle}</h1>
                <p class="text-[11px] text-slate-400 font-medium" id="barista-clock">--:--:--</p>
            </div>
        </div>

        <div class="flex items-center gap-6">
            <div class="flex items-center gap-2 bg-emerald-50 border border-emerald-100 px-3 py-1.5 rounded-full text-[10px] font-bold text-emerald-600">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping"></span>
                Cập nhật trực tiếp
            </div>

            <div class="w-px h-6 bg-slate-200"></div>

            <a href="${ctx}/login?action=logout"
               class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100/50 shadow-sm">
                <i class="fa-solid fa-right-from-bracket text-xs"></i>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <main class="flex-1 overflow-y-auto p-8 bg-[#f4f7fc]/40">
    <script>
        (function () {
            function tickClock() {
                var el = document.getElementById('barista-clock');
                if (!el) return;
                var d = new Date();
                var p = function (n) { return (n < 10 ? '0' : '') + n; };
                el.textContent = p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
            }
            tickClock();
            setInterval(tickClock, 1000);
        })();
    </script>
