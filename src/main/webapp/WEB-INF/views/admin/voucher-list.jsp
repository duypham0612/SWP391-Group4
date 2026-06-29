<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Voucher</h1><p>payment.Voucher</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/voucher?action=new">+ Thêm voucher</a>
</div>

<c:choose>
    <c:when test="${empty vouchers}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có voucher nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Mã</th><th>Loại</th><th>Giá trị</th><th>Đơn tối thiểu</th><th>Phạm vi</th>
                <th>Đã dùng</th><th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="v" items="${vouchers}">
                    <tr>
                        <td><strong>${v.code}</strong></td>
                        <td>${v.discountType}</td>
                        <td>
                            <c:choose>
                                <c:when test="${v.discountType == 'PERCENT'}">${v.discountValue}%</c:when>
                                <c:otherwise><fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/> ₫</c:otherwise>
                            </c:choose>
                        </td>
                        <td><fmt:formatNumber value="${v.minOrderAmount}" maxFractionDigits="0"/> ₫</td>
                        <td><c:choose><c:when test="${v.scope == 'BRANCH'}">${v.branchName}</c:when><c:otherwise>Toàn chuỗi</c:otherwise></c:choose></td>
                        <td>${v.usedCount}<c:if test="${not empty v.usageLimit}">/${v.usageLimit}</c:if></td>
                        <td><c:choose><c:when test="${v.active}"><span class="badge badge-ready">Bật</span></c:when><c:otherwise><span class="badge badge-cancelled">Tắt</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/voucher?action=edit&id=${v.voucherId}">Sửa</a>
                            <form action="${ctx}/admin/voucher" method="post" style="display:inline" onsubmit="return confirm('Đổi trạng thái voucher này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleActive">
                                <input type="hidden" name="id" value="${v.voucherId}">
                                <button type="submit" class="btn btn-ghost btn-sm">${v.active ? 'Tắt' : 'Bật'}</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
