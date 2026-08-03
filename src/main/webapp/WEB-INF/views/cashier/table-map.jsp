<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="cssBundles" value="list-controls" scope="request" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Sơ đồ bàn</h1></div>
    <div style="display:flex;gap:10px;flex-wrap:wrap">
        <a class="btn btn-ghost" href="${ctx}/cashier/table-qr">In QR bàn</a>
        <a class="btn btn-ghost" href="${ctx}/cashier/pos">POS đem về (takeaway)</a>
    </div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">
        <c:out value="${sessionScope.flashError}" />
        <c:if test="${not empty sessionScope.flashErrorHref}">
            <a href="${sessionScope.flashErrorHref}" style="margin-left:8px">Mở Đơn đến</a>
        </c:if>
    </div>
    <c:remove var="flashError" scope="session" />
    <c:remove var="flashErrorHref" scope="session" />
</c:if>

<div class="table-toolbar">
    <div class="table-search">
        <input id="tableSearch" class="form-control" type="search" placeholder="Tìm bàn..." autocomplete="off">
    </div>
</div>

<div class="table-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px">
    <c:forEach var="t" items="${tables}">
        <c:set var="tblClass" value="tbl-empty" />
        <c:set var="tblLabel" value="Trống" />
        <c:set var="tblBadge" value="badge-served" />
        <c:if test="${t.status == 'OCCUPIED'}">
            <c:set var="tblClass" value="tbl-draft" />
            <c:set var="tblLabel" value="Nháp" />
            <c:set var="tblBadge" value="badge-waiting" />
        </c:if>
        <c:if test="${t.status == 'OCCUPIED' and t.activeItemCount > 0}">
            <c:set var="tblClass" value="tbl-busy" />
            <c:set var="tblLabel" value="Đang phục vụ" />
            <c:set var="tblBadge" value="badge-ready" />
        </c:if>
        <c:choose>
            <c:when test="${t.status == 'OCCUPIED'}">
                <c:set var="signal" value="${signals[t.diningTableId]}" />
                <div class="card table-card table-card-link ${tblClass}" data-name="${t.tableNumber}">
                    <a href="${ctx}/cashier/pos?tableId=${t.diningTableId}"
                       style="display:block;color:inherit;text-decoration:none">
                        <div style="display:flex;justify-content:space-between;align-items:center">
                            <strong style="font-size:1.1rem">${t.tableNumber}</strong>
                            <span class="badge ${tblBadge}">${tblLabel}</span>
                        </div>
                        <div class="muted">${t.activeItemCount} món chưa thanh toán</div>
                    </a>
                    <c:if test="${not empty signal}">
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:12px">
                            <span class="badge ${signal == 'bill.requested' ? 'badge-ready' : 'badge-waiting'}">
                                ${signal == 'bill.requested' ? 'Xin thanh toán' : 'Gọi NV'}
                            </span>
                            <form action="${ctx}/cashier/table" method="post">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="ackSignal">
                                <input type="hidden" name="tableId" value="${t.diningTableId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Đã tiếp nhận</button>
                            </form>
                        </div>
                    </c:if>
                    <c:if test="${not empty menuUrls[t.diningTableId]}">
                        <button type="button" class="btn btn-ghost btn-sm" style="width:100%;margin-top:10px"
                                data-qr-table="${t.diningTableId}"
                                onclick="showTableQr('${t.tableNumber}','${menuUrls[t.diningTableId]}')">Xem mã QR</button>
                    </c:if>
                    <form action="${ctx}/cashier/table" method="post" style="margin-top:6px"
                          onsubmit="return confirm('Đóng ${t.tableNumber}? Chỉ bàn chưa có món hoặc đã huỷ hết món mới đóng được.');">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="closeTable">
                        <input type="hidden" name="tableId" value="${t.diningTableId}">
                        <button type="submit" class="btn btn-ghost btn-sm" style="width:100%">Đóng bàn</button>
                    </form>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="waiting" value="${openRequests[t.diningTableId]}" />
                <div class="card table-card ${empty waiting ? tblClass : 'tbl-draft'}" data-name="${t.tableNumber}">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong style="font-size:1.1rem">${t.tableNumber}</strong>
                        <span class="badge ${empty waiting ? tblBadge : 'badge-waiting'}">${empty waiting ? tblLabel : 'Khách đang chờ'}</span>
                    </div>
                    <c:choose>
                        <c:when test="${not empty waiting}">
                            <div class="muted">Khách đã quét QR — xin mở bàn</div>
                        </c:when>
                        <c:otherwise><div class="muted">Bàn trống</div></c:otherwise>
                    </c:choose>
                    <%-- Hai kiểu đặt món cùng đưa DiningTable sang OCCUPIED; chỉ khác nơi điều hướng sau đó. --%>
                    <form action="${ctx}/cashier/table" method="post" style="margin-top:10px">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="openTable">
                        <input type="hidden" name="tableId" value="${t.diningTableId}">
                        <div style="display:flex;flex-direction:column;gap:6px">
                            <button type="submit" name="mode" value="counter" class="btn btn-primary btn-sm">
                                Mở bàn · đặt tại quầy
                            </button>
                            <button type="submit" name="mode" value="qr" class="btn btn-ghost btn-sm"
                                    <c:if test="${empty menuUrls[t.diningTableId]}">disabled title="Bàn chưa có mã QR"</c:if>>
                                Mở bàn · khách quét QR
                            </button>
                        </div>
                    </form>
                    <c:if test="${not empty menuUrls[t.diningTableId]}">
                        <button type="button" class="btn btn-ghost btn-sm" style="width:100%;margin-top:6px"
                                data-qr-table="${t.diningTableId}"
                                onclick="showTableQr('${t.tableNumber}','${menuUrls[t.diningTableId]}')">Xem mã QR</button>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </c:forEach>
</div>
<div id="tableNoMatch" class="card empty-state" style="display:none;margin-top:16px"><div class="icon">∅</div><p>Không tìm thấy bàn phù hợp.</p></div>

<c:if test="${empty tables}">
    <div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có bàn nào.</p></div>
</c:if>

<%-- Mã QR ngay tại sơ đồ bàn: thu ngân chìa màn hình cho khách quét, khỏi phải mở trang in. --%>
<%-- display điều khiển bằng JS, KHÔNG dùng thuộc tính hidden: inline style ghi đè luật
     [hidden]{display:none} của trình duyệt, modal sẽ hiện thường trực và phủ kín trang. --%>
<div id="qrModal"
     style="position:fixed;inset:0;z-index:60;background:rgba(20,8,10,.55);display:none;align-items:center;justify-content:center;padding:20px">
  <div class="card" style="max-width:380px;width:100%;text-align:center;padding:24px">
    <h2 id="qrModalTitle" style="margin:0 0 4px"></h2>
    <p class="muted" style="margin:0 0 14px;font-size:.86rem">Mời khách quét để xem thực đơn và đặt món</p>
    <div id="qrModalBox" style="width:220px;height:220px;margin:0 auto 12px;display:flex;align-items:center;justify-content:center"></div>
    <div id="qrModalUrl" style="font:11px/1.4 monospace;color:var(--muted);overflow-wrap:anywhere;margin-bottom:6px"></div>
    <div id="qrModalWarn" style="display:none;color:var(--st-cancelled);font-size:.82rem;margin-bottom:10px"></div>
    <div style="display:flex;gap:8px">
      <a class="btn btn-ghost btn-sm" style="flex:1" href="${ctx}/cashier/table-qr">Trang in QR</a>
      <button type="button" class="btn btn-primary btn-sm" style="flex:1" onclick="hideTableQr()">Đóng</button>
    </div>
  </div>
</div>

<script src="${ctx}/assets/js/qrcode.min.js"></script>
<script>
// Base do server dựng (đã tính X-Forwarded-* nên bản deploy ra đúng domain công khai).
var QR_SERVER_BASE = '${baseUrl}';

// Chạy local thì base là localhost — điện thoại quét sẽ không vào được. Trang "In QR bàn"
// cho sửa base và lưu vào localStorage; ở đây dùng lại đúng giá trị đó để hai màn không lệch.
function qrEffectiveUrl(url){
  try {
    var override = window.localStorage.getItem('cafeQrBase');
    if (override && QR_SERVER_BASE && url.indexOf(QR_SERVER_BASE) === 0) {
      return override.replace(/\/+$/, '') + url.substring(QR_SERVER_BASE.length);
    }
  } catch (e) { /* trình duyệt chặn localStorage — cứ dùng base của server */ }
  return url;
}

function showTableQr(tableName, rawUrl){
  var url = qrEffectiveUrl(rawUrl);
  var modal = document.getElementById('qrModal');
  var box = document.getElementById('qrModalBox');
  document.getElementById('qrModalTitle').textContent = tableName;
  document.getElementById('qrModalUrl').textContent = url;
  box.innerHTML = '';
  if (window.QRCode) {
    new QRCode(box, {text: url, width: 220, height: 220, correctLevel: QRCode.CorrectLevel.M});
  } else {
    box.textContent = 'Không tải được thư viện tạo mã QR.';
  }
  // Cảnh báo thật sự hữu ích: quét localhost bằng điện thoại luôn thất bại.
  var warn = document.getElementById('qrModalWarn');
  if (/^https?:\/\/(localhost|127\.0\.0\.1)/i.test(url)) {
    warn.textContent = 'Địa chỉ đang là localhost — điện thoại quét sẽ không vào được. '
                     + 'Mở "Trang in QR" và đổi sang IP LAN của máy này.';
    warn.style.display = 'block';
  } else {
    warn.style.display = 'none';
  }
  modal.style.display = 'flex';
}

function hideTableQr(){ document.getElementById('qrModal').style.display = 'none'; }

document.getElementById('qrModal').addEventListener('click', function(e){
  if (e.target === this) hideTableQr();
});
document.addEventListener('keydown', function(e){
  if (e.key === 'Escape') hideTableQr();
});

// Vừa mở bàn theo kiểu "khách quét QR" → bật sẵn mã của đúng bàn đó.
(function(){
  var auto = '${qrTableId}';
  if (!auto) return;
  var btn = document.querySelector('[data-qr-table="' + auto + '"]');
  if (btn) btn.click();
})();

document.getElementById('tableSearch').addEventListener('input', function(){
  const q = this.value.trim().toLowerCase();
  let shown = 0;
  document.querySelectorAll('.table-card').forEach(card => {
    const ok = !q || (card.dataset.name || '').toLowerCase().includes(q);
    card.style.display = ok ? '' : 'none';
    if (ok) shown++;
  });
  document.getElementById('tableNoMatch').style.display = shown === 0 ? '' : 'none';
});
</script>

<jsp:include page="../layout/footer.jsp" />
