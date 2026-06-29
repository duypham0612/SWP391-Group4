<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Hao hụt / Làm lại</h1><p>Ghi hao hụt — trừ tồn qua sổ cái (txn WASTE)</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Ghi hao hụt</h3>
    <form action="${ctx}/barista/waste" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group" style="margin:0;flex:1;min-width:200px"><label>Nguyên liệu</label>
            <select name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}">${i.name} (${i.unit})</option></c:forEach>
            </select></div>
        <div class="form-group" style="margin:0;width:130px"><label>Số lượng</label>
            <input type="number" name="quantity" class="form-control" min="0" step="0.001" required></div>
        <div class="form-group" style="margin:0;width:150px"><label>Loại</label>
            <select name="wasteType" class="form-control" required>
                <option value="SPILL">Đổ/rơi</option>
                <option value="EXPIRED">Hết hạn</option>
                <option value="REMAKE">Làm lại</option>
                <option value="OTHER">Khác</option>
            </select></div>
        <div class="form-group" style="margin:0;flex:1;min-width:160px"><label>Lý do</label>
            <input type="text" name="reason" class="form-control" maxlength="255"></div>
        <button type="submit" class="btn btn-primary">Ghi</button>
    </form>
</div>

<h3>Nhật ký hao hụt</h3>
<c:choose>
    <c:when test="${empty logs}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ghi nhận hao hụt nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:120px">Số lượng</th><th style="width:110px">Loại</th><th>Lý do</th><th>Người ghi</th><th style="width:100px">Trạng thái</th><th style="width:340px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="w" items="${logs}">
                    <tr<c:if test="${w.status == 'VOIDED'}"> style="opacity:.55"</c:if>>
                        <td>${w.ingredientName}</td>
                        <td><strong>${w.quantity}</strong> ${w.ingredientUnit}</td>
                        <td>
                            <c:choose>
                                <c:when test="${w.wasteType == 'SPILL'}">Đổ/rơi</c:when>
                                <c:when test="${w.wasteType == 'EXPIRED'}">Hết hạn</c:when>
                                <c:when test="${w.wasteType == 'REMAKE'}">Làm lại</c:when>
                                <c:otherwise>Khác</c:otherwise>
                            </c:choose>
                        </td>
                        <td>${w.reason}</td>
                        <td>${w.loggedByName}</td>
                        <td>
                            <c:choose>
                                <c:when test="${w.status == 'VOIDED'}"><span class="badge" style="background:var(--st-cancelled);color:#fff">Đã huỷ</span></c:when>
                                <c:otherwise><span class="badge" style="background:var(--st-ready);color:#fff">Hiệu lực</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${w.status == 'ACTIVE'}">
                                <form action="${ctx}/barista/waste" method="post" style="display:inline-flex;gap:4px;align-items:center">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="wasteLogId" value="${w.wasteLogId}">
                                    <input type="number" name="quantity" class="form-control" style="width:80px" min="0.001" step="0.001" value="${w.quantity}" required>
                                    <select name="wasteType" class="form-control" style="width:100px">
                                        <option value="SPILL"   ${w.wasteType=='SPILL'?'selected':''}>Đổ/rơi</option>
                                        <option value="EXPIRED" ${w.wasteType=='EXPIRED'?'selected':''}>Hết hạn</option>
                                        <option value="REMAKE"  ${w.wasteType=='REMAKE'?'selected':''}>Làm lại</option>
                                        <option value="OTHER"   ${w.wasteType=='OTHER'?'selected':''}>Khác</option>
                                    </select>
                                    <input type="text" name="reason" class="form-control" style="width:90px" value="${w.reason}" maxlength="255">
                                    <button type="submit" class="btn btn-ghost btn-sm">Sửa</button>
                                </form>
                                <form action="${ctx}/barista/waste" method="post" style="display:inline" onsubmit="return confirm('Huỷ bản ghi hao hụt này? Tồn kho sẽ được hoàn lại.');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="void">
                                    <input type="hidden" name="wasteLogId" value="${w.wasteLogId}">
                                    <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
