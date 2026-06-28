<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Kho</div><h1>Nhà cung cấp</h1><p>inventory.Supplier</p></div>
    <a class="btn btn-primary" href="${ctx}/manager/supplier?action=new">+ Thêm nhà cung cấp</a>
</div>

<c:choose>
    <c:when test="${empty suppliers}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có nhà cung cấp nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Tên</th><th style="width:140px">SĐT</th><th>Địa chỉ</th><th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="s" items="${suppliers}">
                    <tr>
                        <td>${s.supplierId}</td>
                        <td>${s.name}</td>
                        <td>${s.phone}</td>
                        <td>${s.address}</td>
                        <td><c:choose><c:when test="${s.active}"><span class="badge badge-ready">Hoạt động</span></c:when><c:otherwise><span class="badge badge-cancelled">Ngừng</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/manager/supplier?action=edit&id=${s.supplierId}">Sửa</a>
                            <c:if test="${s.active}">
                                <form action="${ctx}/manager/supplier" method="post" style="display:inline" onsubmit="return confirm('Ngừng nhà cung cấp này?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="toggleActive">
                                    <input type="hidden" name="id" value="${s.supplierId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Ngừng</button>
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
