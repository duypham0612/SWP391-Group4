<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<c:set var="nav" scope="request" value="detail"/>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/barista_sidebar.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="first" value="${empty orderItems ? null : orderItems[0]}"/>

<div class="max-w-5xl mx-auto space-y-8 fade-in">
    <div class="flex items-center gap-3">
        <a href="${ctx}/barista-board" class="w-9 h-9 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-slate-50 transition-all"><i class="fa-solid fa-arrow-left text-xs"></i></a>
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Chi tiết Order #${orderId}</h2>
            <p class="text-xs text-slate-400 font-medium mt-0.5">Toàn bộ món trong đơn hàng này.</p>
        </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Bàn</p>
            <h3 class="text-lg font-extrabold text-slate-800 mt-1"><i class="fa-solid fa-chair text-[#006064] text-sm mr-1.5"></i>${empty first.tableName ? 'Mang đi' : first.tableName}</h3>
        </div>
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Giờ gọi</p>
            <h3 class="text-lg font-extrabold text-slate-800 mt-1"><i class="fa-regular fa-clock text-[#006064] text-sm mr-1.5"></i><c:choose><c:when test="${not empty first}"><fmt:formatDate value="${first.orderDate}" pattern="dd/MM/yyyy HH:mm"/></c:when><c:otherwise>-</c:otherwise></c:choose></h3>
        </div>
        <div class="bg-white p-5 rounded-2xl border border-slate-200/60 shadow-sm">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng số ly</p>
            <h3 class="text-lg font-extrabold text-slate-800 mt-1"><i class="fa-solid fa-mug-hot text-[#006064] text-sm mr-1.5"></i>${totalQty}</h3>
        </div>
    </div>

    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-100"><h3 class="text-sm font-bold text-slate-800">Các món trong order</h3></div>
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5">Món</th>
                        <th class="px-6 py-3.5 text-center w-20">SL</th>
                        <th class="px-6 py-3.5 w-48">Ghi chú</th>
                        <th class="px-6 py-3.5 text-center w-40">Trạng thái</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <c:choose>
                        <c:when test="${empty orderItems}">
                            <tr><td colspan="4" class="px-6 py-12 text-center text-slate-400 font-bold">Order không tồn tại hoặc không có món nào.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="it" items="${orderItems}">
                                <tr class="hover:bg-slate-50/40 transition-colors">
                                    <td class="px-6 py-4 font-bold text-slate-800"><c:out value="${it.productName}"/></td>
                                    <td class="px-6 py-4 text-center"><span class="inline-flex items-center justify-center px-2 py-1 rounded-lg text-[10px] font-bold bg-slate-100 text-slate-600">x${it.quantity}</span></td>
                                    <td class="px-6 py-4 text-amber-700"><c:choose><c:when test="${not empty it.note}"><c:out value="${it.note}"/></c:when><c:otherwise><span class="text-slate-300">—</span></c:otherwise></c:choose></td>
                                    <td class="px-6 py-4 text-center">
                                        <c:choose>
                                            <c:when test="${it.itemStatus == 'Pending'}"><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border bg-amber-50 text-amber-600 border-amber-100"><i class="fa-solid fa-hourglass-half text-[8px]"></i> Chờ pha</span></c:when>
                                            <c:when test="${it.itemStatus == 'Preparing'}"><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border bg-sky-50 text-sky-600 border-sky-100"><i class="fa-solid fa-blender text-[8px]"></i> Đang pha</span></c:when>
                                            <c:when test="${it.itemStatus == 'Completed'}"><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border bg-emerald-50 text-emerald-600 border-emerald-100"><i class="fa-solid fa-circle-check text-[8px]"></i> Hoàn thành</span></c:when>
                                            <c:when test="${it.itemStatus == 'OutOfStock'}"><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border bg-red-50 text-red-600 border-red-100"><i class="fa-solid fa-ban text-[8px]"></i> Tạm hết</span></c:when>
                                            <c:otherwise><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold border bg-slate-100 text-slate-600 border-slate-200"><i class="fa-solid fa-circle text-[8px]"></i> ${it.itemStatus}</span></c:otherwise>
                                        </c:choose>
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
