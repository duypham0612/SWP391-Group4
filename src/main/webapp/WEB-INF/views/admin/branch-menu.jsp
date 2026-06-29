<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Menu: ${branch.name}</h1><p>catalog.BranchMenu · publish / giá local / hết tạm thời</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch-menu">← Chọn chi nhánh khác</a>
</div>

<div class="alert alert-info">
    Bật <strong>Bán</strong> để publish sản phẩm cho chi nhánh. <strong>Giá local</strong> để trống = dùng giá gốc.
    <strong>Hết tạm thời</strong> = khoá món khỏi POS &amp; QR (Barista cũng bật được). Bấm <strong>Lưu</strong> từng dòng.
</div>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">📦</div><p>Chưa có sản phẩm nào (IsActive=1).</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Sản phẩm</th><th style="width:130px">Giá gốc</th><th style="width:90px">Bán</th>
                <th style="width:170px">Giá local</th><th style="width:130px">Hết tạm thời</th><th style="width:180px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                    <tr>
                        <td style="display:flex;align-items:center;gap:10px">
                            <img class="prod-thumb" src="${imgSrc}" alt="${m.productName}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                            <span>${m.productName}
                            <c:if test="${not m.published}"><span class="badge badge-served" style="margin-left:6px">chưa publish</span></c:if></span>
                        </td>
                        <td><fmt:formatNumber value="${m.basePrice}" maxFractionDigits="0"/> ₫</td>
                        <td colspan="4">
                            <form action="${ctx}/admin/branch-menu" method="post" style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:0">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="save">
                                <input type="hidden" name="branchId" value="${branch.branchId}">
                                <input type="hidden" name="productId" value="${m.productId}">
                                <label style="margin:0"><input type="checkbox" name="available" value="1" <c:if test="${m.available}">checked</c:if>> Bán</label>
                                <input type="number" name="localPrice" class="form-control" style="width:150px" step="500" placeholder="(giá gốc)" value="${m.localPrice}">
                                <label style="margin:0"><input type="checkbox" name="is86" value="1" <c:if test="${m.is86}">checked</c:if>> Hết tạm thời</label>
                                <button type="submit" class="btn btn-primary btn-sm">Lưu</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
