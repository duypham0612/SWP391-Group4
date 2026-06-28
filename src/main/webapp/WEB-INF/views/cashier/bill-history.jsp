<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Lịch sử hoá đơn</h1><p>payment.Bill — 100 hoá đơn gần nhất của chi nhánh</p></div>
</div>

<c:choose>
    <c:when test="${empty bills}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có hoá đơn nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Bàn</th><th style="width:150px">Tổng tiền</th><th style="width:120px">Hình thức</th><th style="width:120px">Trạng thái</th><th>Lúc</th><th style="width:90px"></th></tr></thead>
            <tbody>
                <c:forEach var="b" items="${bills}">
                    <tr>
                        <td>${b.billId}</td>
                        <td><c:choose><c:when test="${not empty b.tableNumber}">${b.tableNumber}</c:when><c:otherwise><span class="muted">Đem về</span></c:otherwise></c:choose></td>
                        <td><strong><fmt:formatNumber value="${b.totalAmount}" maxFractionDigits="0"/> ₫</strong></td>
                        <td>${b.paymentMethod}</td>
                        <td>
                            <c:choose>
                                <c:when test="${b.status == 'PAID'}"><span class="badge badge-ready">Đã thu</span></c:when>
                                <c:when test="${b.status == 'VOID'}"><span class="badge badge-cancelled">Huỷ</span></c:when>
                                <c:otherwise><span class="badge badge-waiting">Chưa thu</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${b.paidAt != null ? b.paidAt : b.createdAt}</td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/cashier/history?action=view&billId=${b.billId}">Xem</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
