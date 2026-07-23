<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Kho</div><h1>Phiếu nhập kho</h1><p>Tạo, kiểm tra và xác nhận các lần nhập nguyên liệu vào chi nhánh.</p></div>
    <a class="btn btn-primary" href="${ctx}/manager/receipt?action=new">+ Tạo phiếu nhập</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty receipts}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có phiếu nhập nào.</p></div>
    </c:when>
    <c:otherwise>
        <%-- Huỷ nhiều phiếu cùng lúc: chỉ phiếu Nháp (DRAFT) mới có tickbox & bị huỷ --%>
        <form action="${ctx}/manager/receipt" method="post"
              onsubmit="return confirm('Huỷ các phiếu Nháp đã chọn?');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="cancelMany">
            <div style="margin-bottom:10px">
                <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ phiếu đã chọn</button>
            </div>
            <table class="table">
                <thead><tr>
                    <th style="width:40px"><input type="checkbox" onclick="document.querySelectorAll('.rbox').forEach(c=>c.checked=this.checked)"></th>
                    <th style="width:60px">#</th><th>Nhà cung cấp</th><th style="width:130px">Trạng thái</th><th style="width:150px">Tổng tiền</th><th>Người nhập</th><th style="width:100px"></th>
                </tr></thead>
                <tbody>
                    <c:forEach var="r" items="${receipts}">
                        <tr>
                            <td>
                                <c:if test="${r.status == 'DRAFT'}">
                                    <input class="rbox" type="checkbox" name="rid" value="${r.stockReceiptId}">
                                </c:if>
                            </td>
                            <td>${r.stockReceiptId}</td>
                            <td><c:choose><c:when test="${not empty r.supplierName}">${r.supplierName}</c:when><c:otherwise><span class="muted">—</span></c:otherwise></c:choose></td>
                            <td>
                                <c:choose>
                                    <c:when test="${r.status == 'DRAFT'}"><span class="badge badge-waiting">Nháp</span></c:when>
                                    <c:when test="${r.status == 'CONFIRMED'}"><span class="badge badge-ready">Đã nhập</span></c:when>
                                    <c:otherwise><span class="badge badge-cancelled">Đã huỷ</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><fmt:formatNumber value="${r.totalCost}" maxFractionDigits="0"/> ₫</td>
                            <td>${r.receivedByName}</td>
                            <td><a class="btn btn-ghost btn-sm" href="${ctx}/manager/receipt?action=view&id=${r.stockReceiptId}">Xem</a></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </form>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
