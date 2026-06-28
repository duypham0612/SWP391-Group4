<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1>Danh mục sản phẩm</h1>
        <p>Quản lý các nhóm món (catalog.Category)</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/admin/category?action=new">+ Thêm danh mục</a>
</div>

<c:choose>
    <c:when test="${empty categories}">
        <div class="card empty-state">
            <div class="icon">📭</div>
            <p>Chưa có danh mục nào.</p>
        </div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead>
                <tr>
                    <th style="width:70px">#</th>
                    <th>Tên</th>
                    <th style="width:120px">Thứ tự</th>
                    <th style="width:120px">Trạng thái</th>
                    <th style="width:170px">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="cat" items="${categories}">
                    <tr>
                        <td>${cat.categoryId}</td>
                        <td>${cat.name}</td>
                        <td>${cat.sortOrder}</td>
                        <td>
                            <c:choose>
                                <c:when test="${cat.active}"><span class="badge badge-ready">Hiển thị</span></c:when>
                                <c:otherwise><span class="badge badge-cancelled">Ẩn</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/category?action=edit&id=${cat.categoryId}">Sửa</a>
                            <c:if test="${cat.active}">
                                <form action="${ctx}/admin/category" method="post" style="display:inline"
                                      onsubmit="return confirm('Ẩn danh mục này?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${cat.categoryId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Ẩn</button>
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
