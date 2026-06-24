<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="nav" scope="request" value="dashboard"/>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/barista_sidebar.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="space-y-8 fade-in">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Tổng quan pha chế hôm nay</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi nhanh tình hình quầy pha chế của bạn.</p>
        </div>
        <a href="${ctx}/barista-board" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold shadow-md shadow-[#006064]/10 transition-all">
            <i class="fa-solid fa-blender text-[10px]"></i> Vào bảng pha chế
        </a>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center"><i class="fa-solid fa-hourglass-half text-lg"></i></div>
            <div>
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Chờ pha</p>
                <h3 class="text-2xl font-extrabold text-slate-800 mt-0.5">${countPending}</h3>
            </div>
        </div>
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-sky-100 text-sky-600 flex items-center justify-center"><i class="fa-solid fa-blender text-lg"></i></div>
            <div>
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Đang pha</p>
                <h3 class="text-2xl font-extrabold text-slate-800 mt-0.5">${countPreparing}</h3>
            </div>
        </div>
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center"><i class="fa-solid fa-circle-check text-lg"></i></div>
            <div>
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Hoàn thành (hôm nay)</p>
                <h3 class="text-2xl font-extrabold text-slate-800 mt-0.5">${countCompleted}</h3>
            </div>
        </div>
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-[#006064]/10 text-[#006064] flex items-center justify-center"><i class="fa-solid fa-stopwatch text-lg"></i></div>
            <div>
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Thời gian pha TB</p>
                <h3 class="text-2xl font-extrabold text-slate-800 mt-0.5">${avgPrepLabel}</h3>
            </div>
        </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-white rounded-3xl border border-slate-200/60 shadow-sm p-6">
            <h3 class="text-sm font-bold text-slate-800 mb-4"><i class="fa-solid fa-ranking-star text-[#006064] mr-2"></i>Top món pha nhiều nhất hôm nay</h3>
            <c:choose>
                <c:when test="${empty topProducts}">
                    <p class="text-xs text-slate-400 py-8 text-center">Chưa có món nào hoàn thành hôm nay.</p>
                </c:when>
                <c:otherwise>
                    <canvas id="topChart" height="120"></canvas>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-6">
            <h3 class="text-sm font-bold text-slate-800 mb-4"><i class="fa-solid fa-bolt text-amber-500 mr-2"></i>Truy cập nhanh</h3>
            <div class="space-y-2.5">
                <a href="${ctx}/barista-board" class="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all"><i class="fa-solid fa-blender text-sky-500 w-4"></i> Hàng chờ pha chế</a>
                <!--<a href="${ctx}/barista-display" target="_blank" class="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all"><i class="fa-solid fa-tv text-[#006064] w-4"></i> Màn gọi món (khách xem)</a>-->
                <a href="${ctx}/barista-products" class="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all"><i class="fa-solid fa-box-open text-red-500 w-4"></i> Quản lý sản phẩm</a>
                <a href="${ctx}/barista-report" class="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all"><i class="fa-solid fa-chart-simple text-emerald-500 w-4"></i> Báo cáo thống kê</a>
            </div>
        </div>
    </div>
</div>

<c:if test="${not empty topProducts}">
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
    var labels = [<c:forEach var="s" items="${topProducts}" varStatus="st" end="7">${st.index > 0 ? ',' : ''}"${fn:replace(s.productName, '"', "'")}"</c:forEach>];
    var data = [<c:forEach var="s" items="${topProducts}" varStatus="st" end="7">${st.index > 0 ? ',' : ''}${s.totalQty}</c:forEach>];
    new Chart(document.getElementById('topChart'), {
        type: 'bar',
        data: { labels: labels, datasets: [{ label: 'Số ly đã pha', data: data, backgroundColor: '#006064', borderRadius: 6 }] },
        options: { indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
</script>
</c:if>

<jsp:include page="/common/footer.jsp" />
