<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />
<script src="${ctx}/assets/js/qrcode.min.js"></script>

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Thanh toán</h1>
        <p><c:choose><c:when test="${not empty session}">${session.tableNumber} · phiên #${sessionId}</c:when><c:otherwise>Chọn phiên bàn để thanh toán</c:otherwise></c:choose></p></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/table">← Sơ đồ bàn</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${empty shift}">
    <div class="alert alert-info">Chưa mở ca thu ngân — <a href="${ctx}/cashier/shift">mở ca</a> để tiền thu vào đúng ca (vẫn có thể thanh toán).</div>
</c:if>

<c:choose>
    <%-- chưa chọn phiên --%>
    <c:when test="${empty sessionId}">
        <c:choose>
            <c:when test="${empty openSessions}">
                <div class="card empty-state"><div class="icon">∅</div><p>Không có phiên bàn nào đang mở.</p></div>
            </c:when>
            <c:otherwise>
                <div class="card">
                    <h3 style="margin-top:0">Phiên đang mở</h3>
                    <table class="table">
                        <thead><tr><th>Bàn</th><th>Phiên</th><th style="width:120px"></th></tr></thead>
                        <tbody>
                            <c:forEach var="s" items="${openSessions}">
                                <tr><td>${s.tableNumber}</td><td>#${s.tableSessionId}</td>
                                    <td><a class="btn btn-primary btn-sm" href="${ctx}/cashier/checkout?sessionId=${s.tableSessionId}">Thanh toán</a></td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </c:when>

    <%-- đã chọn phiên: hiển thị các bill --%>
    <c:otherwise>
        <c:if test="${empty bills}">
            <div class="card empty-state"><div class="icon">∅</div><p>Phiên chưa có món nào để thanh toán.</p></div>
        </c:if>

        <c:forEach var="b" items="${bills}">
            <div class="card" style="margin-bottom:18px;border-top:3px solid ${b.status == 'PAID' ? 'var(--st-ready)' : 'var(--coffee)'}">
                <div style="display:flex;justify-content:space-between;align-items:center">
                    <h3 style="margin:0">Hoá đơn #${b.billId}
                        <c:choose>
                            <c:when test="${b.status == 'PAID'}"><span class="badge badge-ready">Đã thu (${b.paymentMethod})</span></c:when>
                            <c:when test="${b.status == 'VOID'}"><span class="badge badge-cancelled">Huỷ</span></c:when>
                            <c:otherwise><span class="badge badge-waiting">Chưa thu</span></c:otherwise>
                        </c:choose>
                    </h3>
                </div>

                <%-- Form TÁCH: chọn món rồi tách sang bill mới --%>
                <form action="${ctx}/cashier/checkout" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="splitBill">
                    <input type="hidden" name="sessionId" value="${sessionId}">
                    <table class="table">
                        <thead><tr><c:if test="${b.status == 'UNPAID'}"><th style="width:40px"></th></c:if><th>Món</th><th style="width:80px">SL</th><th style="width:140px">Thành tiền</th></tr></thead>
                        <tbody>
                            <c:forEach var="bi" items="${b.items}">
                                <tr>
                                    <c:if test="${b.status == 'UNPAID'}"><td><input type="checkbox" name="billItemId" value="${bi.billItemId}"></td></c:if>
                                    <td>${bi.productName}</td>
                                    <td>${bi.quantity}</td>
                                    <td><fmt:formatNumber value="${bi.amount}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                    <c:if test="${b.status == 'UNPAID' and b.items.size() > 1}">
                        <button type="submit" class="btn btn-ghost btn-sm">Tách món đã chọn → bill mới</button>
                    </c:if>
                </form>

                <%-- Tổng tiền --%>
                <div style="max-width:340px;margin-left:auto;font-size:.95rem">
                    <div style="display:flex;justify-content:space-between"><span>Tạm tính</span><span><fmt:formatNumber value="${b.subtotal}" maxFractionDigits="0"/> ₫</span></div>
                    <c:if test="${b.discountAmount > 0}">
                        <div style="display:flex;justify-content:space-between;color:var(--st-ready)"><span>Giảm (voucher ${b.voucherCode})</span><span>−<fmt:formatNumber value="${b.discountAmount}" maxFractionDigits="0"/> ₫</span></div>
                    </c:if>
                    <div style="display:flex;justify-content:space-between"><span>VAT 8%</span><span><fmt:formatNumber value="${b.vatAmount}" maxFractionDigits="0"/> ₫</span></div>
                    <div style="display:flex;justify-content:space-between;font-weight:700;border-top:1px solid var(--line);padding-top:6px;margin-top:6px"><span>Tổng cộng</span><span><fmt:formatNumber value="${b.totalAmount}" maxFractionDigits="0"/> ₫</span></div>
                </div>

                <c:if test="${b.status == 'UNPAID'}">
                    <div style="display:flex;gap:16px;flex-wrap:wrap;margin-top:14px;align-items:flex-end">
                        <%-- Voucher --%>
                        <form action="${ctx}/cashier/checkout" method="post" style="display:flex;gap:6px;align-items:flex-end">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="applyVoucher">
                            <input type="hidden" name="sessionId" value="${sessionId}">
                            <input type="hidden" name="billId" value="${b.billId}">
                            <div class="form-group" style="margin:0;width:160px"><label>Mã voucher</label>
                                <input type="text" name="code" class="form-control" placeholder="VD: WELCOME10"></div>
                            <button type="submit" class="btn btn-ghost btn-sm">Áp dụng</button>
                        </form>
                        <c:if test="${not empty b.voucherId}">
                            <form action="${ctx}/cashier/checkout" method="post">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="removeVoucher">
                                <input type="hidden" name="sessionId" value="${sessionId}">
                                <input type="hidden" name="billId" value="${b.billId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Bỏ voucher</button>
                            </form>
                        </c:if>
                        <%-- Thanh toán --%>
                        <form class="pay-form" action="${ctx}/cashier/checkout" method="post" style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap" onsubmit="return confirm(this.querySelector('[name=method]').value === 'QR_BANK' ? 'Xác nhận đã nhận tiền QR?' : 'Xác nhận thu tiền hoá đơn này?');">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="pay">
                            <input type="hidden" name="sessionId" value="${sessionId}">
                            <input type="hidden" name="billId" value="${b.billId}">
                            <div class="form-group" style="margin:0;width:150px"><label>Hình thức</label>
                                <select name="method" class="form-control pay-method" data-bill-id="${b.billId}">
                                    <option value="CASH">Tiền mặt</option>
                                    <option value="TRANSFER">Chuyển khoản</option>
                                    <option value="QR_BANK">QR ngân hàng</option>
                                </select></div>
                            <div class="qr-pay-panel" id="qr-panel-${b.billId}" data-payload="<c:out value='${qrPayloads[b.billId]}'/>" style="display:none">
                                <div class="qr-code" id="qr-code-${b.billId}"></div>
                                <div class="muted" style="font-size:.85rem;margin-top:6px">
                                    ${vietQrAccountName} · ${vietQrAccountNo}<br>
                                    Nội dung: CAFE BILL ${b.billId}
                                </div>
                            </div>
                            <button type="submit" class="btn btn-primary pay-submit">Thu tiền</button>
                        </form>
                    </div>
                </c:if>
            </div>
        </c:forEach>

        <%-- Gộp bill nếu có >1 bill chưa thu --%>
        <c:set var="unpaidCount" value="0" />
        <c:forEach var="b" items="${bills}"><c:if test="${b.status == 'UNPAID'}"><c:set var="unpaidCount" value="${unpaidCount + 1}" /></c:if></c:forEach>
        <c:if test="${unpaidCount > 1}">
            <div class="card">
                <h3 style="margin-top:0">Gộp hoá đơn</h3>
                <form action="${ctx}/cashier/checkout" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="mergeBill">
                    <input type="hidden" name="sessionId" value="${sessionId}">
                    <p class="muted">Chọn các hoá đơn cần gộp (dồn vào hoá đơn đầu tiên được chọn):</p>
                    <c:forEach var="b" items="${bills}">
                        <c:if test="${b.status == 'UNPAID'}">
                            <label style="display:inline-flex;gap:6px;margin-right:14px"><input type="checkbox" name="billId" value="${b.billId}"> #${b.billId} (<fmt:formatNumber value="${b.totalAmount}" maxFractionDigits="0"/> ₫)</label>
                        </c:if>
                    </c:forEach>
                    <div style="margin-top:10px"><button type="submit" class="btn btn-ghost">Gộp hoá đơn đã chọn</button></div>
                </form>
            </div>
        </c:if>
    </c:otherwise>
</c:choose>

<script>
document.querySelectorAll('.pay-form').forEach(form => {
  const method = form.querySelector('.pay-method');
  const submit = form.querySelector('.pay-submit');
  const panel = form.querySelector('.qr-pay-panel');
  const codeBox = panel ? panel.querySelector('.qr-code') : null;
  let rendered = false;
  function syncPaymentUi(){
    const isQr = method.value === 'QR_BANK';
    if (panel) panel.style.display = isQr ? 'block' : 'none';
    if (submit) submit.textContent = isQr ? 'Đã nhận tiền' : 'Thu tiền';
    if (isQr && panel && codeBox && !rendered) {
      const payload = panel.dataset.payload || '';
      codeBox.innerHTML = '';
      if (window.QRCode && payload) {
        new QRCode(codeBox, {text: payload, width: 180, height: 180, correctLevel: QRCode.CorrectLevel.M});
      } else {
        codeBox.textContent = payload || 'Không tạo được QR.';
      }
      rendered = true;
    }
  }
  method.addEventListener('change', syncPaymentUi);
  syncPaymentUi();
});
</script>

<jsp:include page="../layout/footer.jsp" />
