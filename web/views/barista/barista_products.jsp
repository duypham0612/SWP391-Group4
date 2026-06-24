<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="nav" scope="request" value="products"/>

<jsp:include page="/common/header.jsp" />
<jsp:include page="/common/barista_sidebar.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="max-w-7xl mx-auto space-y-8 fade-in">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Quản lý sản phẩm</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Báo tạm hết hoặc mở bán lại từng món. Món "tạm hết" sẽ bị ẩn khỏi menu order.</p>
        </div>
        <div class="flex items-center gap-2">
            <span class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-[11px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100"><i class="fa-solid fa-circle-check"></i> Còn bán: ${availCount}</span>
            <span class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-[11px] font-bold bg-red-50 text-red-600 border border-red-100"><i class="fa-solid fa-ban"></i> Tạm hết: ${outCount}</span>
        </div>
    </div>

    <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 overflow-hidden">
        <div class="px-6 py-5 bg-[#006064] text-white flex items-center gap-3">
            <div class="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center border border-white/20 shadow-inner"><i class="fa-solid fa-box-open text-xl"></i></div>
            <div>
                <h3 class="text-sm font-bold tracking-wide uppercase">DANH SÁCH SẢN PHẨM</h3>
                <p class="text-[11px] text-teal-100/90 mt-0.5 font-medium">Tổng: ${fn:length(productList)} món</p>
            </div>
        </div>

        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="border-b border-slate-100 bg-slate-50/50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th class="px-6 py-3.5 text-center w-20">Mã</th>
                        <th class="px-6 py-3.5">Tên Món</th>
                        <th class="px-6 py-3.5 w-56">Danh Mục</th>
                        <th class="px-6 py-3.5 text-right w-32">Giá</th>
                        <th class="px-6 py-3.5 text-center w-36">Trạng Thái</th>
                        <th class="px-6 py-3.5 text-center w-40">Hành Động</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                    <c:choose>
                        <c:when test="${empty productList}">
                            <tr><td colspan="6" class="px-6 py-12 text-center text-slate-400 font-bold">Chưa có sản phẩm nào.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="p" items="${productList}">
                                <tr class="hover:bg-slate-50/40 transition-colors ${p.available ? '' : 'bg-red-50/20'}">
                                    <td class="px-6 py-4 text-center font-bold text-slate-400">#${p.productId}</td>
                                    <td class="px-6 py-4 font-bold text-slate-800"><c:out value="${p.productName}"/></td>
                                    <td class="px-6 py-4 text-slate-500">${empty p.categoryName ? '-' : p.categoryName}</td>
                                    <td class="px-6 py-4 text-right font-bold text-slate-700"><fmt:formatNumber value="${p.basePrice}" pattern="#,##0"/>đ</td>
                                    <td class="px-6 py-4 text-center">
                                        <c:choose>
                                            <c:when test="${p.available}">
                                                <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100"><i class="fa-solid fa-circle-check text-[8px]"></i> Còn bán</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold bg-red-50 text-red-600 border border-red-100"><i class="fa-solid fa-ban text-[8px]"></i> Tạm hết</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="px-6 py-4 text-center">
                                        <form method="post" action="${ctx}/barista-products" class="inline">
                                            <input type="hidden" name="productId" value="${p.productId}">
                                            <input type="hidden" name="available" value="${p.available ? '0' : '1'}">
                                            <c:choose>
                                                <c:when test="${p.available}">
                                                    <button type="submit" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 text-[11px] font-bold transition-all border border-red-100"><i class="fa-solid fa-ban text-[9px]"></i> Báo hết</button>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="submit" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-bold transition-all"><i class="fa-solid fa-rotate-left text-[9px]"></i> Mở bán lại</button>
                                                </c:otherwise>
                                            </c:choose>
                                        </form>
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
