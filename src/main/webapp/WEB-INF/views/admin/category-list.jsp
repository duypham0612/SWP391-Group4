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
        <div data-tabletools>
            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="categorySearch">Tìm kiếm</label>
                    <input id="categorySearch" class="form-control" type="search" data-tt-search placeholder="Tìm theo tên danh mục">
                </div>
                <div class="form-group">
                    <label for="categoryStatusFilter">Trạng thái</label>
                    <select id="categoryStatusFilter" class="form-control tt-filter" data-tt-filter data-tt-col="3">
                        <option value="">Tất cả</option>
                        <option value="true">Hiển thị</option>
                        <option value="false">Ẩn</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="categoryPageSize">Hiển thị</label>
                    <select id="categoryPageSize" class="form-control tt-size" data-tt-size>
                        <option value="5">5</option>
                        <option value="10">10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                    </select>
                </div>
            </div>
            <table class="table">
                <thead>
                    <tr>
                        <th style="width:70px" data-tt-nosearch>#</th>
                        <th data-tt-search>Tên</th>
                        <th style="width:120px" data-tt-nosearch>Thứ tự</th>
                        <th style="width:120px" data-tt-nosearch>Trạng thái</th>
                        <th style="width:170px" data-tt-nosearch>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="cat" items="${categories}">
                        <tr>
                            <td>${cat.categoryId}</td>
                            <td>${cat.name}</td>
                            <td>${cat.sortOrder}</td>
                            <td data-tt-val="${cat.active}">
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
            <div class="table-tools-foot">
                <span class="tt-summary" data-tt-summary></span>
                <div class="pagination" data-tt-pager></div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
