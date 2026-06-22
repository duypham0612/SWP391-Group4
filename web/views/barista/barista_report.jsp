<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="nav" scope="request" value="report"/>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/barista_sidebar.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="sumQty" value="0"/>
<c:forEach var="s" items="${statList}"><c:set var="sumQty" value="${sumQty + s.totalQty}"/></c:forEach>

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Báo cáo thống kê pha chế</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Số lượng từng món đã pha và thời gian pha trung bình theo khoảng ngày.</p>
        </div>
        <form method="get" action="${ctx}/barista-report" class="flex flex-wrap items-center gap-2">
            <input type="date" name="from" value="${from}" class="bg-white border border-slate-200 text-xs font-semibold text-slate-700 px-3 py-2.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500/20">
            <span class="text-slate-400 text-xs">→</span>
            <input type="date" name="to" value="${to}" class="bg-white border border-slate-200 text-xs font-semibold text-slate-700 px-3 py-2.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500/20">
            <button type="submit" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold shadow-sm transition-all"><i class="fa-solid fa-magnifying-glass text-[10px]"></i> Lọc</button>
        </form>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Số loại món</p>
            <h3 class="text-2xl font-extrabold text-slate-800 mt-1">${fn:length(statList)}</h3>
        </div>
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng số ly đã pha</p>
            <h3 class="text-2xl font-extrabold text-slate-800 mt-1">${sumQty}</h3>
        </div>
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Khoảng ngày</p>
            <h3 class="text-sm font-bold text-slate-700 mt-2">${empty from ? 'Hôm nay' : from} <c:if test="${not empty to}">→ ${to}</c:if></h3>
        </div>
    </div>

    <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-6">
        <h3 class="text-sm font-bold text-slate-800 mb-4"><i class="fa-solid fa-chart-column text-[#006064] mr-2"></i>Số ly đã pha theo món</h3>
        <c:choose>
            <c:when test="${empty statList}">
                <p class="text-xs text-slate-400 py-8 text-center">Không có dữ liệu trong khoảng ngày đã chọn.</p>
            </c:when>
            <c:otherwise>
                <canvas id="repChart" height="110"></canvas>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-100"><h3 class="text-sm font-bold text-slate-800">Chi tiết theo món</h3></div>
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-16">#</th>
                        <th class="px-6 py-3.5">Món</th>
                        <th class="px-6 py-3.5 text-center w-28">Số ly</th>
                        <th class="px-6 py-3.5 text-center w-28">Số lần</th>
                        <th class="px-6 py-3.5 text-center w-40">Thời gian pha TB</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <c:choose>
                        <c:when test="${empty statList}">
                            <tr><td colspan="5" class="px-6 py-12 text-center text-slate-400 font-bold">Không có dữ liệu.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="s" items="${statList}" varStatus="st">
                                <tr class="hover:bg-slate-50/40 transition-colors">
                                    <td class="px-6 py-4 text-center font-bold text-slate-400">${st.count}</td>
                                    <td class="px-6 py-4 font-bold text-slate-800"><c:out value="${s.productName}"/></td>
                                    <td class="px-6 py-4 text-center"><span class="inline-flex items-center justify-center px-2.5 py-1 rounded-lg text-[10px] font-bold bg-[#006064]/10 text-[#006064]">${s.totalQty}</span></td>
                                    <td class="px-6 py-4 text-center text-slate-500">${s.orderCount}</td>
                                    <td class="px-6 py-4 text-center">
                                        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100"><i class="fa-solid fa-stopwatch text-[9px]"></i> ${s.avgPrepLabel}</span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</div>

<c:if test="${not empty statList}">
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
    var labels = [<c:forEach var="s" items="${statList}" varStatus="st" end="9">${st.index > 0 ? ',' : ''}"${fn:replace(s.productName, '"', "'")}"</c:forEach>];
    var data = [<c:forEach var="s" items="${statList}" varStatus="st" end="9">${st.index > 0 ? ',' : ''}${s.totalQty}</c:forEach>];
    new Chart(document.getElementById('repChart'), {
        type: 'bar',
        data: { labels: labels, datasets: [{ label: 'Số ly', data: data, backgroundColor: '#0ea5e9', borderRadius: 6 }] },
        options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
</script>
</c:if>

<jsp:include page="/common/footer.jsp" />
