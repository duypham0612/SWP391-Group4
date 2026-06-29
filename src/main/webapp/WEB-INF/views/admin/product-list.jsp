<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Sản phẩm</h1><p>catalog.Product</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/product?action=new">+ Thêm sản phẩm</a>
</div>

<c:choose>
    <c:when test="${empty products}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có sản phẩm nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th style="width:60px">#</th><th>Tên</th><th>Danh mục</th>
                <th style="width:140px">Giá gốc</th><th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td>${p.productId}</td>
                        <td>${p.name}</td>
                        <td>${p.categoryName}</td>
                        <td><fmt:formatNumber value="${p.basePrice}" type="number" maxFractionDigits="0"/> ₫</td>
                        <td><c:choose><c:when test="${p.active}"><span class="badge badge-ready">Hiển thị</span></c:when><c:otherwise><span class="badge badge-cancelled">Ẩn</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/product?action=edit&id=${p.productId}">Sửa</a>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/recipe?productId=${p.productId}">Công thức</a>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/modifier?view=assign&productId=${p.productId}">Modifier</a>
                            <form action="${ctx}/admin/product" method="post" style="display:inline" onsubmit="return confirm('Đổi trạng thái sản phẩm này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleActive">
                                <input type="hidden" name="id" value="${p.productId}">
                                <button type="submit" class="btn btn-ghost btn-sm">${p.active ? 'Ẩn' : 'Hiện'}</button>
                            </form>
                            <form action="${ctx}/admin/product" method="post" style="display:inline-flex;gap:4px;align-items:center">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="publishToBranch">
                                <input type="hidden" name="id" value="${p.productId}">
                                <select name="branchId" class="form-control" style="width:auto;padding:4px 8px" required>
                                    <option value="">Publish →</option>
                                    <c:forEach var="b" items="${branches}">
                                        <option value="${b.branchId}">${b.code}</option>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="btn btn-ghost btn-sm">Publish</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
