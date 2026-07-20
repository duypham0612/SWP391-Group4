<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Menu: ${branch.name}</h1></div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch-menu">← Chi nhánh khác</a>
</div>

<style>
  .branch-menu-table th,
  .branch-menu-table td{vertical-align:middle}
  .branch-menu-table th:first-child,
  .branch-menu-table td:first-child{padding-left:96px}
  .branch-menu-product{display:grid;grid-template-columns:64px minmax(160px,1fr);align-items:center;column-gap:14px;min-width:320px}
  .branch-menu-product .prod-thumb{width:58px;height:58px;object-fit:cover;justify-self:center;border-radius:8px;display:block}
  .branch-menu-product__name{display:flex;flex-direction:column;gap:4px;justify-content:center;min-height:58px}
  .branch-menu-product__name strong{display:block;line-height:1.25}
  .branch-menu-price{font-weight:700;white-space:nowrap}
  .branch-menu-input{max-width:140px}
  .branch-menu-check{display:inline-flex;align-items:center;justify-content:center;gap:6px;margin:0;white-space:nowrap}
  .branch-menu-actions{text-align:right;white-space:nowrap}
  .branch-menu-muted{font-size:.82rem;color:var(--muted)}
</style>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có sản phẩm đang hoạt động.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table branch-menu-table">
            <thead>
                <tr>
                    <th>Sản phẩm</th>
                    <th style="width:130px">Giá gốc</th>
                    <th style="width:150px">Giá áp dụng</th>
                    <th style="width:100px;text-align:center">Đang bán</th>
                    <th style="width:120px;text-align:center">Tạm hết</th>
                    <th style="width:110px;text-align:right">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                    <tr>
                        <td>
                            <div class="branch-menu-product">
                                <img class="prod-thumb" src="${imgSrc}" alt="${m.productName}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                                <div class="branch-menu-product__name">
                                    <strong>${m.productName}</strong>
                                    <c:if test="${not m.published}">
                                        <span class="badge badge-served">Chưa bán tại chi nhánh</span>
                                    </c:if>
                                </div>
                            </div>
                        </td>
                        <td class="branch-menu-price"><fmt:formatNumber value="${m.basePrice}" maxFractionDigits="0"/> ₫</td>
                        <td>
                            <input form="bm-${m.productId}" type="number" name="localPrice" class="form-control branch-menu-input" step="500"
                                   placeholder="Theo giá gốc" value="${m.localPrice}">
                        </td>
                        <td style="text-align:center">
                            <label class="branch-menu-check">
                                <input form="bm-${m.productId}" type="checkbox" name="available" value="1" <c:if test="${m.available}">checked</c:if>>
                            </label>
                        </td>
                        <td style="text-align:center">
                            <label class="branch-menu-check">
                                <input form="bm-${m.productId}" type="checkbox" name="is86" value="1" <c:if test="${m.is86}">checked</c:if>>
                            </label>
                        </td>
                        <td class="branch-menu-actions">
                            <form id="bm-${m.productId}" action="${ctx}/admin/branch-menu" method="post" style="display:inline">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="save">
                                <input type="hidden" name="branchId" value="${branch.branchId}">
                                <input type="hidden" name="productId" value="${m.productId}">
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
