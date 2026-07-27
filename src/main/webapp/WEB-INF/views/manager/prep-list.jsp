<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Kho chi nhánh</div>
        <h1>Quản lý mẻ pha sẵn</h1>
        <p>Đính chính mẻ Barista nhập sai trước khi nguyên liệu phát sinh tiêu thụ.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/manager/inventory">← Tồn kho</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>

<div class="alert alert-warn">
    Nếu nguyên liệu đã được dùng sau khi tạo mẻ, hệ thống sẽ chặn hủy. Khi đó hãy
    <a href="${ctx}/manager/reconciliation?action=new">kiểm kê và điều chỉnh tồn thực tế</a>.
</div>

<table class="table">
    <thead><tr><th>#</th><th>Nguyên liệu</th><th>Sản lượng</th><th>Người pha</th><th>Lúc</th><th>Hạn dùng</th><th>Trạng thái</th><th>Đính chính</th></tr></thead>
    <tbody>
        <c:choose>
            <c:when test="${empty batches}">
                <tr class="tt-empty"><td colspan="8">Chưa có mẻ pha nào.</td></tr>
            </c:when>
            <c:otherwise>
                <c:forEach var="batch" items="${batches}">
                    <tr class="${batch.status == 'CANCELLED' ? 'row-muted' : ''}">
                        <td>${batch.prepBatchId}</td>
                        <td>${batch.preppedIngredientName}</td>
                        <td><strong>${batch.quantityProduced}</strong> ${batch.preppedIngredientUnit}</td>
                        <td>${batch.madeByName}</td>
                        <td>${batch.madeAtDisplay}</td>
                        <td>${empty batch.expiresAtDisplay ? '—' : batch.expiresAtDisplay}</td>
                        <td>
                            <c:choose>
                                <c:when test="${batch.status == 'CANCELLED'}"><span class="badge badge-cancelled">Đã hủy</span></c:when>
                                <c:when test="${batch.writtenOff}"><span class="badge badge-making">Đã loại bỏ</span></c:when>
                                <c:otherwise><span class="badge badge-ready">Đang dùng</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${batch.status == 'ACTIVE' and not batch.writtenOff}">
                                <form action="${ctx}/manager/prep" method="post"
                                      onsubmit="return confirm('Hủy mẻ nhập sai này và hoàn lại tồn kho?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="cancelBatch">
                                    <input type="hidden" name="prepBatchId" value="${batch.prepBatchId}">
                                    <button type="submit" class="btn btn-ghost btn-sm prep-cancel-btn">Hủy mẻ sai</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </tbody>
</table>

<jsp:include page="../layout/footer.jsp" />
