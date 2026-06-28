<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nguyên liệu</h1><p>catalog.Ingredient · RAW (mua về) / PREPPED (pha sẵn)</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/ingredient?action=new">+ Thêm nguyên liệu</a>
</div>

<c:choose>
    <c:when test="${empty ingredients}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có nguyên liệu nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th style="width:60px">#</th><th>Tên</th><th style="width:100px">Đơn vị</th>
                <th style="width:120px">Loại</th><th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="i" items="${ingredients}">
                    <tr>
                        <td>${i.ingredientId}</td>
                        <td>${i.name}</td>
                        <td>${i.unit}</td>
                        <td>
                            <c:choose>
                                <c:when test="${i.ingredientType == 'RAW'}"><span class="badge badge-making">RAW</span></c:when>
                                <c:otherwise><span class="badge badge-ready">PREPPED</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:choose><c:when test="${i.active}"><span class="badge badge-ready">Hiển thị</span></c:when><c:otherwise><span class="badge badge-cancelled">Ẩn</span></c:otherwise></c:choose></td>
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
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
