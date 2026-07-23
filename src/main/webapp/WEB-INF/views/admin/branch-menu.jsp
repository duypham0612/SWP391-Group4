<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Menu: ${branch.name}</h1><p>Bật món bán tại chi nhánh, đặt giá riêng và đánh dấu tạm hết.</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch-menu">← Chọn chi nhánh khác</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>

<div class="alert alert-info">
    Bật <strong>Bán</strong> để món xuất hiện trên POS và QR. Giá riêng để trống sẽ dùng giá gốc.
    <strong>Tạm hết</strong> dùng khi chi nhánh hết món trong thời gian ngắn.
</div>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">📦</div><p>Chưa có sản phẩm đang hoạt động để thêm vào menu.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table admin-branch-menu-table">
            <thead><tr>
                <th>Sản phẩm</th>
                <th style="width:130px">Giá gốc</th>
                <th style="width:100px">Bán</th>
                <th style="width:180px">Giá riêng</th>
                <th style="width:140px">Tạm hết</th>
                <th style="width:100px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                    <tr>
                        <td>
                            <div class="menu-product-cell">
                            <img class="prod-thumb" src="${imgSrc}" alt="${m.productName}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                            <span>${m.productName}
                            <c:if test="${not m.published}"><span class="badge badge-served" style="margin-left:6px">chưa bán</span></c:if></span>
                            </div>
                            <form id="menuForm${m.productId}" action="${ctx}/admin/branch-menu" method="post" style="display:none">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="save">
                                <input type="hidden" name="branchId" value="${branch.branchId}">
                                <input type="hidden" name="productId" value="${m.productId}">
                            </form>
                        </td>
                        <td><fmt:formatNumber value="${m.basePrice}" maxFractionDigits="0"/> ₫</td>
                        <td><label class="check-cell"><input form="menuForm${m.productId}" type="checkbox" name="available" value="1" <c:if test="${m.available}">checked</c:if>> Bán</label></td>
                        <td><input form="menuForm${m.productId}" type="number" name="localPrice" class="form-control" min="0" step="500" placeholder="Giá gốc" value="${m.localPrice}"></td>
                        <td><label class="check-cell"><input form="menuForm${m.productId}" type="checkbox" name="is86" value="1" <c:if test="${m.is86}">checked</c:if>> Tạm hết</label></td>
                        <td><button type="submit" form="menuForm${m.productId}" class="btn btn-primary btn-sm">Lưu</button></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
