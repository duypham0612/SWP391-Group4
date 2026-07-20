<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nguyên liệu</h1></div>
    <a class="btn btn-primary" href="${ctx}/admin/ingredient?action=new">+ Thêm nguyên liệu</a>
</div>

<c:choose>
    <c:when test="${empty ingredients}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có nguyên liệu nào.</p></div>
    </c:when>
    <c:otherwise>
        <div data-tabletools>
            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="ingredientSearch">Tìm kiếm</label>
                    <input id="ingredientSearch" class="form-control" type="search" data-tt-search placeholder="Tìm theo tên hoặc đơn vị">
                </div>
                <div class="form-group">
                    <label for="ingredientTypeFilter">Loại</label>
                    <select id="ingredientTypeFilter" class="form-control tt-filter" data-tt-filter data-tt-col="3">
                        <option value="">Tất cả</option>
                        <option value="RAW">Thô</option>
                        <option value="PREPPED">Pha sẵn</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="ingredientStatusFilter">Trạng thái</label>
                    <select id="ingredientStatusFilter" class="form-control tt-filter" data-tt-filter data-tt-col="4">
                        <option value="">Tất cả</option>
                        <option value="true">Hiển thị</option>
                        <option value="false">Ẩn</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="ingredientPageSize">Hiển thị</label>
                    <select id="ingredientPageSize" class="form-control tt-size" data-tt-size>
                        <option value="10">10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                    </select>
                </div>
            </div>
            <table class="table">
                <thead><tr>
                    <th style="width:60px" data-tt-nosearch>#</th><th data-tt-search>Tên</th><th style="width:100px" data-tt-search>Đơn vị</th>
                    <th style="width:120px" data-tt-nosearch>Loại</th><th style="width:110px" data-tt-nosearch>Trạng thái</th><th style="width:170px" data-tt-nosearch>Thao tác</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="i" items="${ingredients}">
                        <tr>
                            <td>${i.ingredientId}</td>
                            <td>${i.name}</td>
                            <td>${i.unit}</td>
                            <td data-tt-val="${i.ingredientType}">
                                <c:choose>
                                    <c:when test="${i.ingredientType == 'RAW'}"><span class="badge badge-making">Thô</span></c:when>
                                    <c:otherwise><span class="badge badge-ready">Pha sẵn</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td data-tt-val="${i.active}"><c:choose><c:when test="${i.active}"><span class="badge badge-ready">Hiển thị</span></c:when><c:otherwise><span class="badge badge-cancelled">Ẩn</span></c:otherwise></c:choose></td>
                            <td>
                                <a class="btn btn-ghost btn-sm" href="${ctx}/admin/ingredient?action=edit&id=${i.ingredientId}">Sửa</a>
                                <c:if test="${i.active}">
                                    <form action="${ctx}/admin/ingredient" method="post" style="display:inline" onsubmit="return confirm('Ẩn nguyên liệu này?');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="${i.ingredientId}">
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
