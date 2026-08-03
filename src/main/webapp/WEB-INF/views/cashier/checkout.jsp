<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="cssBundles" value="checkout" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />
<script src="${ctx}/assets/js/vendor/qrcode.min.js"></script>

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Thanh toán</h1>
        <p><c:choose>
            <c:when test="${takeawayCheckout}">Đơn mang đi #${orderId}</c:when>
            <c:when test="${not empty table}">${table.tableNumber}</c:when>
            <c:otherwise>Chọn bàn hoặc đơn mang đi để thanh toán</c:otherwise>
        </c:choose></p></div>
    <a class="btn btn-ghost" href="${takeawayCheckout ? ctx.concat('/cashier/inbox#orders') : ctx.concat('/cashier/table')}">← Quay lại</a>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${empty shift}">
    <div class="alert alert-info">Chưa mở ca thu ngân — <a href="${ctx}/cashier/shift">mở ca</a> trước khi thu tiền để doanh thu được ghi nhận đúng ca.</div>
</c:if>

<c:choose>
    <%-- chưa chọn bàn --%>
    <c:when test="${empty tableId and empty orderId}">
        <c:if test="${empty openTables and empty takeawayOrders}">
            <div class="card empty-state"><div class="icon">∅</div><p>Không có bàn hoặc đơn mang đi nào chờ thanh toán.</p></div>
        </c:if>
        <c:if test="${not empty openTables}">
            <div class="card" style="margin-bottom:18px">
                <h3 style="margin-top:0">Bàn đang phục vụ</h3>
                <table class="table">
                    <thead><tr><th>Bàn</th><th style="width:120px"></th></tr></thead>
                    <tbody>
                        <c:forEach var="t" items="${openTables}">
                            <tr><td>${t.tableNumber}</td>
                                <td><a class="btn btn-primary btn-sm" href="${ctx}/cashier/checkout?tableId=${t.diningTableId}">Thanh toán</a></td></tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>
        <c:if test="${not empty takeawayOrders}">
            <div class="card">
                <h3 style="margin-top:0">Đơn mang đi chưa thanh toán</h3>
                <table class="table">
                    <thead><tr><th>Mã nhận món</th><th>Đơn</th><th>Thời gian</th><th style="width:150px">Tạm tính</th><th style="width:150px">Trạng thái</th><th style="width:130px"></th></tr></thead>
                    <tbody>
                        <c:forEach var="o" items="${takeawayOrders}">
                            <tr>
                                <td><strong>${o.pickupCode}</strong></td>
                                <td>#${o.orderId}</td>
                                <td>${view.fullUtc(o.createdAt)}</td>
                                <td><fmt:formatNumber value="${o.total}" maxFractionDigits="0"/> ₫</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${o.status == 'COMPLETED'}"><span class="badge badge-ready">Đã bàn giao</span></c:when>
                                        <c:otherwise><span class="badge badge-making">Chờ pha / bàn giao</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${o.status == 'COMPLETED'}"><a class="btn btn-primary btn-sm" href="${ctx}/cashier/checkout?orderId=${o.orderId}">Thanh toán</a></c:when>
                                        <c:otherwise><a class="btn btn-ghost btn-sm" href="${ctx}/cashier/inbox#orders">Theo dõi đơn</a></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>
    </c:when>

    <%-- đã chọn bàn: hiển thị các bill --%>
    <c:otherwise>
        <c:if test="${empty bills}">
            <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào để thanh toán.</p></div>
        </c:if>

        <c:forEach var="b" items="${bills}">
            <div class="card checkout-bill ${b.status == 'PAID' ? 'checkout-bill--paid' : ''}">
                <div class="checkout-bill__header">
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
                    <input type="hidden" name="tableId" value="${tableId}">
                    <input type="hidden" name="orderId" value="${orderId}">
                    <table class="table">
                        <thead><tr><c:if test="${b.status == 'UNPAID' and not takeawayCheckout}"><th style="width:40px"></th></c:if><th>Món</th><th style="width:80px">SL</th><th style="width:140px">Thành tiền</th></tr></thead>
                        <tbody>
                            <c:forEach var="bi" items="${b.items}">
                                <tr>
                                    <c:if test="${b.status == 'UNPAID' and not takeawayCheckout}"><td><input type="checkbox" name="orderItemId" value="${bi.orderItemId}"></td></c:if>
                                    <td>${bi.productName}
                                        <c:if test="${not empty bi.selections}"><div class="muted" style="font-size:.85rem"><c:out value="${bi.selections}" /></div></c:if>
                                        <c:if test="${not empty bi.note}"><div class="muted" style="font-size:.85rem">Ghi chú: <c:out value="${bi.note}" /></div></c:if>
                                    </td>
                                    <td>${bi.quantity}</td>
                                    <td><fmt:formatNumber value="${bi.amount}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                    <c:if test="${b.status == 'UNPAID' and not takeawayCheckout and b.items.size() > 1}">
                        <button type="submit" class="btn btn-ghost btn-sm">Tách món đã chọn → bill mới</button>
                    </c:if>
                </form>

                <%-- Tổng tiền --%>
                <div class="bill-summary">
                    <div class="bill-summary__row"><span>Tạm tính</span><span><fmt:formatNumber value="${b.subtotal}" maxFractionDigits="0"/> ₫</span></div>
                    <c:if test="${b.discountAmount > 0}">
                        <div class="bill-summary__row bill-summary__discount"><span>Giảm giá</span><span>−<fmt:formatNumber value="${b.discountAmount}" maxFractionDigits="0"/> ₫</span></div>
                    </c:if>
                    <div class="bill-summary__row"><span>VAT 8%</span><span><fmt:formatNumber value="${b.vatAmount}" maxFractionDigits="0"/> ₫</span></div>
                    <div class="bill-summary__total"><span>Tổng cộng</span><strong><fmt:formatNumber value="${b.totalAmount}" maxFractionDigits="0"/> ₫</strong></div>
                </div>

                <c:if test="${b.status == 'UNPAID'}">
                    <div class="checkout-actions">
                        <c:choose>
                            <c:when test="${b.readyForPayment}">
                                <%-- Chỉ mở thanh toán sau khi mọi món trên bill đã SERVED. --%>
                                <form class="pay-form" action="${ctx}/cashier/checkout" method="post">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="pay">
                                    <input type="hidden" name="tableId" value="${tableId}">
                                    <input type="hidden" name="orderId" value="${orderId}">
                                    <input type="hidden" name="billId" value="${b.billId}">
                                    <div class="form-group payment-method"><label>Hình thức thanh toán</label>
                                        <select name="method" class="form-control pay-method" data-bill-id="${b.billId}">
                                            <option value="CASH">Tiền mặt</option>
                                            <option value="TRANSFER">Chuyển khoản</option>
                                            <option value="QR_BANK">QR ngân hàng</option>
                                        </select></div>
                                    <div class="cash-pay-panel"
                                         data-payable="${cashPayableAmounts[b.billId]}"
                                         data-adjustment="${cashRoundingAdjustments[b.billId]}">
                                        <div class="cash-stat">
                                            <div class="cash-stat__label">Tiền mặt cần thu</div>
                                            <strong class="cash-payable cash-stat__amount">
                                                <fmt:formatNumber value="${cashPayableAmounts[b.billId]}" maxFractionDigits="0"/> ₫
                                            </strong>
                                        </div>
                                        <div class="cash-stat cash-stat--rounding">
                                            <div class="cash-stat__label">Điều chỉnh làm tròn</div>
                                            <strong class="cash-stat__rounding">
                                                <c:if test="${cashRoundingAdjustments[b.billId] > 0}">+</c:if><fmt:formatNumber value="${cashRoundingAdjustments[b.billId]}" maxFractionDigits="0"/> ₫
                                            </strong>
                                        </div>
                                        <div class="form-group cash-tender-input">
                                            <label>Tiền khách đưa</label>
                                            <input type="number" name="cashTendered" class="form-control cash-tendered"
                                                   min="${cashPayableAmounts[b.billId]}" step="1000"
                                                   inputmode="numeric" autocomplete="off" placeholder="Nhập số tiền khách đưa">
                                        </div>
                                        <div class="cash-quick">
                                            <button type="button" class="btn btn-ghost btn-sm" data-cash-exact>Đúng số</button>
                                            <button type="button" class="btn btn-ghost btn-sm" data-cash-value="50000">50.000</button>
                                            <button type="button" class="btn btn-ghost btn-sm" data-cash-value="100000">100.000</button>
                                            <button type="button" class="btn btn-ghost btn-sm" data-cash-value="200000">200.000</button>
                                            <button type="button" class="btn btn-ghost btn-sm" data-cash-value="500000">500.000</button>
                                        </div>
                                        <div class="cash-change-row">
                                            <span>Tiền thối</span>
                                            <strong class="cash-change">—</strong>
                                        </div>
                                    </div>
                                    <div class="qr-pay-panel" id="qr-panel-${b.billId}" data-payload="<c:out value='${qrPayloads[b.billId]}'/>" style="display:none">
                                        <div class="qr-code" id="qr-code-${b.billId}"></div>
                                        <div class="qr-pay-panel__details">
                                            ${vietQrBankName} · ${vietQrAccountNo}<br>
                                            Chủ tài khoản: ${vietQrAccountName}<br>
                                            Nội dung: CAFE BILL ${b.billId}
                                        </div>
                                    </div>
                                    <button type="submit" class="btn btn-primary pay-submit">Thu tiền</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-info" style="margin:0;flex:1">
                                    Chờ Barista pha xong và Cashier bàn giao đủ món trước khi thanh toán.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>
            </div>
        </c:forEach>

        <%-- Gộp bill nếu có >1 bill chưa thu --%>
        <c:set var="unpaidCount" value="0" />
        <c:forEach var="b" items="${bills}"><c:if test="${b.status == 'UNPAID'}"><c:set var="unpaidCount" value="${unpaidCount + 1}" /></c:if></c:forEach>
        <c:if test="${unpaidCount > 1 and not takeawayCheckout}">
            <div class="card">
                <h3 style="margin-top:0">Gộp hoá đơn</h3>
                <form action="${ctx}/cashier/checkout" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="mergeBill">
                    <input type="hidden" name="tableId" value="${tableId}">
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
  const qrPanel = form.querySelector('.qr-pay-panel');
  const codeBox = qrPanel ? qrPanel.querySelector('.qr-code') : null;
  const cashPanel = form.querySelector('.cash-pay-panel');
  const tendered = form.querySelector('.cash-tendered');
  const changeBox = form.querySelector('.cash-change');
  const payable = cashPanel ? Number(cashPanel.dataset.payable || 0) : 0;
  let rendered = false;
  const money = value => new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 0}).format(value) + ' ₫';

  function syncCashChange(){
    if (!tendered || !changeBox) return;
    const value = Number(tendered.value);
    if (!tendered.value) {
      changeBox.textContent = '—';
      changeBox.style.color = '';
    } else if (value < payable) {
      changeBox.textContent = 'Thiếu ' + money(payable - value);
      changeBox.style.color = 'var(--st-cancelled)';
    } else {
      changeBox.textContent = money(value - payable);
      changeBox.style.color = 'var(--st-ready)';
    }
  }

  function syncPaymentUi(){
    const isQr = method.value === 'QR_BANK';
    const isCash = method.value === 'CASH';
    if (qrPanel) qrPanel.style.display = isQr ? 'block' : 'none';
    if (cashPanel) cashPanel.style.display = isCash ? 'grid' : 'none';
    if (tendered) {
      tendered.disabled = !isCash;
      tendered.required = isCash;
    }
    if (submit) submit.textContent = isQr ? 'Đã nhận tiền' : 'Thu tiền';
    if (isQr && qrPanel && codeBox && !rendered) {
      const payload = qrPanel.dataset.payload || '';
      codeBox.innerHTML = '';
      if (window.QRCode && payload) {
        new QRCode(codeBox, {text: payload, width: 180, height: 180, correctLevel: QRCode.CorrectLevel.M});
      } else {
        codeBox.textContent = payload || 'Không tạo được QR.';
      }
      rendered = true;
    }
    syncCashChange();
  }

  if (tendered) tendered.addEventListener('input', syncCashChange);
  form.querySelectorAll('[data-cash-value]').forEach(button => {
    const value = Number(button.dataset.cashValue || 0);
    button.disabled = value < payable;
    button.addEventListener('click', () => {
      tendered.value = value;
      syncCashChange();
    });
  });
  const exactButton = form.querySelector('[data-cash-exact]');
  if (exactButton) exactButton.addEventListener('click', () => {
    tendered.value = payable;
    syncCashChange();
  });
  form.addEventListener('submit', event => {
    let message = 'Xác nhận thu tiền hoá đơn này?';
    if (method.value === 'QR_BANK') message = 'Xác nhận đã nhận tiền QR?';
    if (method.value === 'CASH') {
      message = 'Xác nhận thu ' + money(payable)
        + ' và thối ' + money(Number(tendered.value) - payable) + '?';
    }
    if (!window.confirm(message)) event.preventDefault();
  });
  method.addEventListener('change', syncPaymentUi);
  syncPaymentUi();
});
</script>

<jsp:include page="../layout/footer.jsp" />
