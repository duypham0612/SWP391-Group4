<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Kho</div><h1>Đối soát tồn</h1><p>inventory.StockAdjustment · điều chỉnh qua sổ cái (ADJUST)</p></div>
    <a class="btn btn-primary" href="${ctx}/manager/reconciliation?action=new">+ Điều chỉnh mới</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty adjustments}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có lần đối soát nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Nguyên liệu</th><th style="width:130px">Tồn hệ thống</th><th style="width:130px">Tồn thực tế</th><th style="width:120px">Chênh lệch</th><th>Lý do</th><th>Người</th></tr></thead>
            <tbody>
                <c:forEach var="a" items="${adjustments}">
                    <tr>
                        <td>${a.stockAdjustmentId}</td>
                        <td>${a.ingredientName}</td>
                        <td>${a.systemQty} ${a.displayUnit}</td>
                        <td>${a.actualQty} ${a.displayUnit}</td>
                        <td style="font-weight:600;color:${a.diffQty.signum() < 0 ? 'var(--st-cancelled)' : 'var(--st-ready)'}"><c:if test="${a.diffQty.signum() > 0}">+</c:if>${a.diffQty}</td>
                        <td>${a.reason}</td>
                        <td>${a.adjustedByName}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
