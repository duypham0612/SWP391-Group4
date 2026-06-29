<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Thực đơn</div><h1>Menu chi nhánh</h1><p>catalog.BranchMenu — bật/tắt bán, giá địa phương, hết tạm thời</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có sản phẩm nào được Admin publish cho chi nhánh này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Sản phẩm</th><th style="width:140px">Giá gốc</th>
                <th style="width:260px">Giá địa phương</th>
                <th style="width:120px">Bán</th><th style="width:140px">Hết tạm thời</th>
            </tr></thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                    <tr>
                        <td style="display:flex;align-items:center;gap:10px">
                            <img class="prod-thumb" src="${imgSrc}" alt="${m.productName}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                            <span>${m.productName}</span>
                        </td>
                        <td><fmt:formatNumber value="${m.basePrice}" maxFractionDigits="0"/> ₫</td>
                        <td>
                            <form action="${ctx}/manager/menu" method="post" style="display:flex;gap:6px;align-items:center;margin:0">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="setLocalPrice">
                                <input type="hidden" name="productId" value="${m.productId}">
                                <input type="number" name="localPrice" class="form-control" style="width:130px" step="100" min="0"
                                       value="${m.localPrice}" placeholder="(giá gốc)">
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                            </form>
                        </td>
                        <td>
                            <form action="${ctx}/manager/menu" method="post" style="margin:0">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleAvailable">
                                <input type="hidden" name="productId" value="${m.productId}">
                                <button type="submit" class="btn btn-sm ${m.available ? 'btn-primary' : 'btn-ghost'}">
                                    ${m.available ? 'Đang bán' : 'Ngừng bán'}
                                </button>
                            </form>
                        </td>
                        <td>
                            <form action="${ctx}/manager/menu" method="post" style="margin:0">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggle86">
                                <input type="hidden" name="productId" value="${m.productId}">
                                <button type="submit" class="btn btn-sm ${m.is86 ? 'btn-ghost' : 'btn-ghost'}">
                                    <c:choose><c:when test="${m.is86}"><span class="badge badge-cancelled">Hết tạm thời</span></c:when><c:otherwise><span class="badge badge-ready">Còn bán</span></c:otherwise></c:choose>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
