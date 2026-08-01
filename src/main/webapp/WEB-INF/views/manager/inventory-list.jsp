<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Kho</div><h1>Tồn kho chi nhánh</h1><p>Theo dõi số lượng hiện có và các nguyên liệu sắp chạm ngưỡng cảnh báo.</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>

<c:if test="${not empty oversoldStock}">
    <div class="alert alert-error">
        <strong>Tồn âm cần kiểm kê:</strong>
        <c:forEach var="o" items="${oversoldStock}" varStatus="st">${o.ingredientName} (${view.grouped(o.quantityOnHand)} ${o.ingredientUnit})<c:if test="${not st.last}">, </c:if></c:forEach>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/reconciliation?action=new" style="margin-left:10px">Điều chỉnh tồn</a>
    </div>
</c:if>

<c:if test="${not empty lowStock}">
    <div class="alert alert-error">
        <strong>Cảnh báo tồn thấp:</strong>
        <c:forEach var="l" items="${lowStock}" varStatus="st">${l.ingredientName} (${view.grouped(l.quantityOnHand)} ${l.ingredientUnit})<c:if test="${not st.last}">, </c:if></c:forEach>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty inventory}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có tồn kho. Hãy tạo phiếu nhập kho và xác nhận.</p></div>
    </c:when>
    <c:otherwise>
        <div style="display:flex;justify-content:flex-end;margin-bottom:var(--s3)">
            <a class="btn btn-ghost" href="${ctx}/manager/prep">Quản lý mẻ pha sẵn →</a>
        </div>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:110px">Loại</th><th style="width:140px">Tồn hiện tại</th><th style="width:320px">Chính sách tồn</th><th style="width:120px">Sổ cái</th></tr></thead>
            <tbody>
                <c:forEach var="bi" items="${inventory}">
                    <tr>
                        <td>${bi.ingredientName}
                            <c:choose>
                                <c:when test="${bi.oversold}"><span class="badge badge-cancelled" style="margin-left:6px">ÂM KHO · cần kiểm kê</span></c:when>
                                <c:when test="${bi.low}"><span class="badge badge-waiting" style="margin-left:6px">Thấp</span></c:when>
                            </c:choose>
                        </td>
                        <td><c:choose><c:when test="${bi.ingredientType == 'RAW'}"><span class="badge badge-making">Thô</span></c:when><c:otherwise><span class="badge badge-ready">Pha sẵn</span></c:otherwise></c:choose></td>
                        <td><strong>${view.grouped(bi.quantityOnHand)}</strong> ${bi.ingredientUnit}</td>
                        <td>
                            <form action="${ctx}/manager/inventory" method="post" style="display:flex;gap:6px;align-items:end;margin:0;flex-wrap:wrap">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="${bi.ingredientType == 'PREPPED' ? 'setPrepPolicy' : 'setThreshold'}">
                                <input type="hidden" name="ingredientId" value="${bi.ingredientId}">
                                <label style="font-size:var(--fs-xs)">Cảnh báo
                                    <input type="text" name="threshold" class="form-control" style="width:115px" value="${view.grouped(bi.minThreshold)}" data-vi-number required>
                                </label>
                                <c:if test="${bi.ingredientType == 'PREPPED'}">
                                    <label style="font-size:var(--fs-xs)">Mục tiêu
                                        <input type="text" name="target" class="form-control" style="width:115px" value="${view.grouped(bi.prepTargetQty)}" data-vi-number required>
                                    </label>
                                </c:if>
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                            </form>
                        </td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/manager/inventory?action=ledger&ingredientId=${bi.ingredientId}">Xem</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
