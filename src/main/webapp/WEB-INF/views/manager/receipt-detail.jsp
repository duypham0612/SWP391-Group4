<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="draft" value="${receipt.status == 'DRAFT'}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Phiếu nhập #${receipt.stockReceiptId}</div>
        <h1>
            <c:choose>
                <c:when test="${receipt.status == 'DRAFT'}"><span class="badge badge-waiting">Nháp</span></c:when>
                <c:when test="${receipt.status == 'CONFIRMED'}"><span class="badge badge-ready">Đã nhập kho</span></c:when>
                <c:otherwise><span class="badge badge-cancelled">Đã huỷ</span></c:otherwise>
            </c:choose>
        </h1>
        <p><c:if test="${not empty receipt.supplierName}">NCC: ${receipt.supplierName} · </c:if>Người nhập: ${receipt.receivedByName}</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/manager/receipt">← Danh sách phiếu</a>
</div>

<c:if test="${draft}">
    <div class="card" style="margin-bottom:18px">
        <h3 style="margin-top:0">Thêm dòng nguyên liệu</h3>
        <form action="${ctx}/manager/receipt" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="addLine">
            <input type="hidden" name="receiptId" value="${receipt.stockReceiptId}">
            <div class="form-group" style="margin:0;flex:1;min-width:220px"><label>Nguyên liệu</label>
                <select name="ingredientId" class="form-control" required>
                    <option value="">-- Chọn --</option>
                    <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}">${i.name} (${i.unit})</option></c:forEach>
                </select></div>
            <div class="form-group" style="margin:0;width:140px"><label>Số lượng</label>
                <input type="number" name="quantity" class="form-control" min="0" step="0.001" required></div>
            <div class="form-group" style="margin:0;width:150px"><label>Đơn giá (₫)</label>
                <input type="number" name="unitCost" class="form-control" min="0" step="100" value="0"></div>
            <button type="submit" class="btn btn-primary">+ Thêm</button>
        </form>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty details}">
        <div class="card empty-state"><div class="icon">∅</div><p>Phiếu chưa có dòng nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:150px">Số lượng</th><th style="width:150px">Đơn giá</th><th style="width:160px">Thành tiền</th><c:if test="${draft}"><th style="width:90px"></th></c:if></tr></thead>
            <tbody>
                <c:forEach var="d" items="${details}">
                    <tr>
                        <td>${d.ingredientName}</td>
                        <td>${d.quantity} ${d.ingredientUnit}</td>
                        <td><fmt:formatNumber value="${d.unitCost}" maxFractionDigits="0"/> ₫</td>
                        <td><fmt:formatNumber value="${d.lineCost}" maxFractionDigits="0"/> ₫</td>
                        <c:if test="${draft}">
                            <td>
                                <form action="${ctx}/manager/receipt" method="post" style="display:inline" onsubmit="return confirm('Xoá dòng?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="removeLine">
                                    <input type="hidden" name="receiptId" value="${receipt.stockReceiptId}">
                                    <input type="hidden" name="detailId" value="${d.stockReceiptDetailId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                                </form>
                            </td>
                        </c:if>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<c:if test="${receipt.status == 'CONFIRMED'}">
    <div class="alert alert-success" style="margin-top:18px">Đã nhập kho · Tổng tiền: <strong><fmt:formatNumber value="${receipt.totalCost}" maxFractionDigits="0"/> ₫</strong>. Tồn đã được cộng qua sổ cái.</div>
</c:if>

<c:if test="${draft}">
    <div style="display:flex;gap:10px;margin-top:18px">
        <form action="${ctx}/manager/receipt" method="post" onsubmit="return confirm('Xác nhận nhập kho? Tồn sẽ được cộng và không thể sửa.');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="confirm">
            <input type="hidden" name="receiptId" value="${receipt.stockReceiptId}">
            <button type="submit" class="btn btn-primary btn-lg" <c:if test="${empty details}">disabled</c:if>>Xác nhận nhập kho</button>
        </form>
        <form action="${ctx}/manager/receipt" method="post" onsubmit="return confirm('Huỷ phiếu này?');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="cancel">
            <input type="hidden" name="receiptId" value="${receipt.stockReceiptId}">
            <button type="submit" class="btn btn-ghost btn-lg">Huỷ phiếu</button>
        </form>
    </div>
</c:if>

<jsp:include page="../layout/footer.jsp" />
