<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Vận hành</div>
        <h1>Két thu ngân</h1>
        <p>Kiểm tra và xử lý ca thu ngân bị bỏ quên tại chi nhánh.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/manager/attendance">Chấm công</a>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<c:if test="${openShifts.size() gt 1}">
    <div class="alert alert-error">
        Chi nhánh đang có ${openShifts.size()} ca thu ngân cùng mở. Hãy kiểm đếm tiền thực tế,
        đối chiếu từng ca và kết các ca cũ trước khi cho ca tiếp theo hoạt động.
    </div>
</c:if>

<div class="alert alert-info">
    Kết ca hộ chỉ đóng két và ghi đầy đủ tiền dự kiến, tiền thực đếm, chênh lệch, người xử lý và lý do.
    Bản chấm công không tự thay đổi; hãy chỉnh giờ ra ở màn Chấm công nếu ca bị bỏ quên.
</div>

<c:choose>
    <c:when test="${empty openShifts}">
        <div class="card empty-state">
            <div class="icon">✓</div>
            <p>Không có két thu ngân nào đang mở.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div style="overflow-x:auto">
            <table class="table">
                <thead>
                <tr>
                    <th style="width:70px">Ca</th>
                    <th>Thu ngân</th>
                    <th style="width:155px">Mở lúc</th>
                    <th style="width:135px">Quỹ đầu ca</th>
                    <th style="width:145px">Tiền mặt đã thu</th>
                    <th style="width:145px">Cần đối chiếu</th>
                    <th style="min-width:390px">Kết ca hộ</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="shift" items="${openShifts}">
                    <tr>
                        <td><strong>#${shift.cashierShiftId}</strong></td>
                        <td>
                            <strong><c:out value="${shift.cashierName}" /></strong>
                            <div class="muted" style="font-size:.85rem">${shift.billCount} hóa đơn đã thu</div>
                        </td>
                        <td>${shift.openedAtDisplay}</td>
                        <td><fmt:formatNumber value="${shift.openingCash}" maxFractionDigits="0" /> đ</td>
                        <td><fmt:formatNumber value="${shift.cashCollected}" maxFractionDigits="0" /> đ</td>
                        <td>
                            <strong><fmt:formatNumber value="${shift.expectedClosingCash}" maxFractionDigits="0" /> đ</strong>
                        </td>
                        <td>
                            <form action="${ctx}/manager/cashier-shift" method="post"
                                  style="display:grid;grid-template-columns:145px 1fr auto;gap:8px;align-items:end"
                                  onsubmit="return confirm('Kết ca #${shift.cashierShiftId} bằng số tiền thực đếm đã nhập? Thao tác sẽ được ghi audit.');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="forceClose">
                                <input type="hidden" name="shiftId" value="${shift.cashierShiftId}">
                                <div class="form-group" style="margin:0">
                                    <label>Tiền thực đếm</label>
                                    <input type="number" name="actualCash" class="form-control"
                                           min="0" step="1000" required placeholder="0">
                                </div>
                                <div class="form-group" style="margin:0">
                                    <label>Lý do</label>
                                    <input type="text" name="reason" class="form-control" maxlength="255"
                                           required placeholder="VD: Thu ngân quên kết ca">
                                </div>
                                <button type="submit" class="btn btn-primary">Kết ca hộ</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
