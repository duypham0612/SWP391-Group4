<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Sổ cái</div><h1>${ingredient.name}</h1><p>inventory.InventoryTransaction · lịch sử thay đổi tồn</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/inventory">← Quay lại tồn kho</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty ledger}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có giao dịch tồn cho nguyên liệu này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:80px">#</th><th style="width:170px">Thời gian</th><th style="width:130px">Loại</th><th style="width:140px">Thay đổi</th><th>Nguồn</th><th>Người tạo</th></tr></thead>
            <tbody>
                <c:forEach var="t" items="${ledger}">
                    <tr>
                        <td>${t.inventoryTxnId}</td>
                        <td>${t.createdAt}</td>
                        <td><span class="badge badge-served">${t.txnType}</span></td>
                        <td style="font-weight:600;color:${t.changeQty.signum() < 0 ? 'var(--st-cancelled)' : 'var(--st-ready)'}">
                            <c:if test="${t.changeQty.signum() > 0}">+</c:if>${t.changeQty} ${t.ingredientUnit}
                        </td>
                        <td>${t.refTable}<c:if test="${not empty t.refId}"> #${t.refId}</c:if></td>
                        <td>${t.createdByName}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
