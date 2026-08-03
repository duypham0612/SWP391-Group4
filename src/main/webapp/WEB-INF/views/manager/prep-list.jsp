<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="cssBundles" value="list-controls" scope="request" />
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

<c:if test="${not empty pendingBatches}">
<section class="alert alert-warn" style="display:block">
    <h2 style="margin-top:0">Mẻ cần duyệt (${fn:length(pendingBatches)})</h2>
    <p class="muted">Sản lượng vượt mức mục tiêu — nguyên liệu thô đã bị trừ, nhưng chưa tính vào tồn bán được cho tới khi duyệt.</p>
    <table class="table">
        <thead><tr><th>#</th><th>Nguyên liệu</th><th>Sản lượng</th><th>Người pha</th><th>Lúc</th><th>Hạn dùng</th><th>Duyệt</th></tr></thead>
        <tbody>
        <c:forEach var="batch" items="${pendingBatches}">
            <tr>
                <td>${batch.prepBatchId}</td>
                <td>${batch.preppedIngredientName}</td>
                <td><strong>${view.plain(batch.quantityProduced)}</strong> ${batch.preppedIngredientUnit}</td>
                <td>${batch.madeByName}</td>
                <td>${view.shortUtc(batch.madeAt)}</td>
                <td>${empty batch.expiresAt ? '—' : view.shortUtc(batch.expiresAt)}
                    <c:if test="${batch.expiredWhilePending}"><br><span class="badge badge-cancelled">Đã hết hạn — chỉ có thể Từ chối</span></c:if>
                </td>
                <td>
                    <form action="${ctx}/manager/prep" method="post" style="display:inline">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="prepBatchId" value="${batch.prepBatchId}">
                        <button type="submit" name="action" value="approveBatch" class="btn btn-primary btn-sm"
                                ${batch.expiredWhilePending ? 'disabled' : ''}>Duyệt</button>
                        <button type="submit" name="action" value="rejectBatch" class="btn btn-ghost btn-sm"
                                onclick="return confirm('Từ chối mẻ này? Nguyên liệu thô sẽ được hoàn lại.');">Từ chối</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</section>
</c:if>

<c:if test="${not empty unreviewedBatches}">
<section>
    <h2>Mẻ chưa xem (hậu kiểm, ${fn:length(unreviewedBatches)})</h2>
    <p class="muted">Mẻ bình thường — đã có hiệu lực, không cần chặn gì. Xem lại khi rảnh để đóng dấu đã kiểm tra.</p>
    <table class="table">
        <thead><tr><th>#</th><th>Nguyên liệu</th><th>Sản lượng</th><th>Người pha</th><th>Lúc</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="batch" items="${unreviewedBatches}">
            <tr>
                <td>${batch.prepBatchId}</td>
                <td>${batch.preppedIngredientName}</td>
                <td><strong>${view.plain(batch.quantityProduced)}</strong> ${batch.preppedIngredientUnit}</td>
                <td>${batch.madeByName}</td>
                <td>${view.shortUtc(batch.madeAt)}</td>
                <td>
                    <form action="${ctx}/manager/prep" method="post" style="display:inline">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="markReviewed">
                        <input type="hidden" name="prepBatchId" value="${batch.prepBatchId}">
                        <button type="submit" class="btn btn-ghost btn-sm">Đã xem</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</section>
</c:if>

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
                        <td><strong>${view.plain(batch.quantityProduced)}</strong> ${batch.preppedIngredientUnit}</td>
                        <td>${batch.madeByName}</td>
                        <td>${view.shortUtc(batch.madeAt)}</td>
                        <td>${empty batch.expiresAt ? '—' : view.shortUtc(batch.expiresAt)}</td>
                        <td>
                            <c:choose>
                                <c:when test="${batch.status == 'CANCELLED'}"><span class="badge badge-cancelled">Đã hủy</span></c:when>
                                <c:when test="${batch.status == 'PENDING'}"><span class="badge badge-waiting">Chờ duyệt</span></c:when>
                                <c:when test="${batch.status == 'REJECTED'}"><span class="badge badge-cancelled">Đã từ chối</span></c:when>
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
