<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Lịch sử tồn kho</div><h1>${ingredient.name}</h1><p>Các lần nhập, xuất, hao hụt và điều chỉnh số lượng nguyên liệu.</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/inventory">← Quay lại tồn kho</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty ledger}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có thay đổi tồn kho nào cho nguyên liệu này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:80px">#</th><th style="width:170px">Thời gian</th><th style="width:130px">Loại</th><th style="width:140px">Thay đổi</th><th>Nguồn</th><th>Người tạo</th></tr></thead>
            <tbody>
                <c:forEach var="t" items="${ledger}">
                    <tr>
                        <td>${t.inventoryTxnId}</td>
                        <td>${t.createdAt}</td>
                        <td><span class="badge badge-served"><c:choose>
                            <c:when test="${t.txnType == 'RECEIPT'}">Nhập kho</c:when>
                            <c:when test="${t.txnType == 'DEDUCT'}">Xuất pha chế</c:when>
                            <c:when test="${t.txnType == 'WASTE'}">Hao hụt</c:when>
                            <c:when test="${t.txnType == 'PREP_IN'}">Nhập từ sơ chế</c:when>
                            <c:when test="${t.txnType == 'PREP_OUT'}">Xuất để sơ chế</c:when>
                            <c:when test="${t.txnType == 'ADJUST'}">Điều chỉnh kiểm kê</c:when>
                            <c:otherwise>Thay đổi khác</c:otherwise>
                        </c:choose></span></td>
                        <td style="font-weight:600;color:${t.changeQty.signum() < 0 ? 'var(--st-cancelled)' : 'var(--st-ready)'}">
                            <c:if test="${t.changeQty.signum() > 0}">+</c:if>${view.grouped(t.changeQty)} ${t.ingredientUnit}
                        </td>
                        <td><c:choose>
                                <c:when test="${t.referenceType == 'STOCK_RECEIPT_LINE'}">Phiếu nhập</c:when>
                            <c:when test="${t.referenceType == 'ORDER_ITEM'}">Món đã pha</c:when>
                            <c:when test="${t.referenceType == 'PREP_BATCH'}">Mẻ sơ chế</c:when>
                                <c:when test="${t.referenceType == 'WASTE_ENTRY'}">Ghi nhận hao hụt</c:when>
                            <c:when test="${t.referenceType == 'STOCK_ADJUSTMENT'}">Lần kiểm kê</c:when>
                            <c:otherwise>${t.referenceType}</c:otherwise>
                        </c:choose><c:if test="${not empty t.referenceId}"> #${t.referenceId}</c:if></td>
                        <td>${t.createdByName}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
