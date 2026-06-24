<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="nav" scope="request" value="history"/>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/barista_sidebar.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Lịch sử pha chế</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Danh sách các món đã pha xong, kèm thời gian thực hiện.</p>
        </div>
        <form method="get" action="${ctx}/barista-history" class="flex items-center gap-2">
            <input type="date" name="date" value="${selectedDate}"
                   class="bg-white border border-slate-200 text-xs font-semibold text-slate-700 px-3 py-2.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500/20">
            <button type="submit" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold shadow-sm transition-all">
                <i class="fa-solid fa-magnifying-glass text-[10px]"></i> Lọc
            </button>
        </form>
    </div>

    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
        <div class="px-6 py-5 bg-[#006064] text-white flex items-center gap-3">
            <div class="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center border border-white/20 shadow-inner">
                <i class="fa-solid fa-clock-rotate-left text-xl text-white"></i>
            </div>
            <div>
                <h3 class="text-sm font-bold tracking-wide uppercase">CÁC MÓN ĐÃ HOÀN THÀNH</h3>
                <p class="text-[11px] text-teal-100/90 mt-0.5 font-medium">
                    <c:choose><c:when test="${empty selectedDate}">Hôm nay</c:when><c:otherwise>Ngày ${selectedDate}</c:otherwise></c:choose> &middot; Tổng: ${fn:length(historyList)} món
                </p>
            </div>
        </div>

        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-24">Mã Order</th>
                        <th class="px-6 py-3.5 w-32">Bàn</th>
                        <th class="px-6 py-3.5">Món</th>
                        <th class="px-6 py-3.5 text-center w-20">SL</th>
                        <th class="px-6 py-3.5 text-center w-32">Giờ gọi</th>
                        <th class="px-6 py-3.5 text-center w-32">Giờ xong</th>
                        <th class="px-6 py-3.5 text-center w-36">Thời gian pha</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <c:choose>
                        <c:when test="${empty historyList}">
                            <tr>
                                <td colspan="7" class="px-6 py-12 text-center">
                                    <div class="flex flex-col items-center justify-center gap-3">
                                        <i class="fa-solid fa-mug-saucer text-slate-200 text-5xl"></i>
                                        <p class="text-xs font-bold text-slate-500">Chưa có món nào hoàn thành trong ngày này.</p>
                                    </div>
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="it" items="${historyList}">
                                <tr class="hover:bg-slate-50/40 transition-colors">
                                    <td class="px-6 py-4 text-center font-bold text-[#006064]">#${it.orderId}</td>
                                    <td class="px-6 py-4 font-semibold text-slate-700">${empty it.tableName ? 'Mang đi' : it.tableName}</td>
                                    <td class="px-6 py-4 font-bold text-slate-800"><c:out value="${it.productName}"/></td>
                                    <td class="px-6 py-4 text-center">
                                        <span class="inline-flex items-center justify-center px-2 py-1 rounded-lg text-[10px] font-bold bg-slate-100 text-slate-600">x${it.quantity}</span>
                                    </td>
                                    <td class="px-6 py-4 text-center text-slate-500"><fmt:formatDate value="${it.orderDate}" pattern="HH:mm:ss"/></td>
                                    <td class="px-6 py-4 text-center text-slate-500"><fmt:formatDate value="${it.completedAt}" pattern="HH:mm:ss"/></td>
                                    <td class="px-6 py-4 text-center">
                                        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100">
                                            <i class="fa-solid fa-stopwatch text-[9px]"></i> ${it.prepDurationLabel}
                                        </span>
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

<jsp:include page="/common/footer.jsp" />
