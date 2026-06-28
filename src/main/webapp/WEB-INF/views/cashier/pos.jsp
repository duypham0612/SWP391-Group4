<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>POS — Đặt món</h1><p>Cashier sở hữu order entry · gửi bếp khi bấm "Gửi đơn"</p></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/table">← Sơ đồ bàn</a>
</div>

<div style="display:grid;grid-template-columns:1fr 360px;gap:20px;align-items:start">
    <div>
        <c:if test="${empty menu}">
            <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào bán ở chi nhánh (Admin publish + Manager bật bán, Barista chưa 86).</p></div>
        </c:if>
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px">
            <c:forEach var="m" items="${menu}">
                <div class="card pos-product" data-product-id="${m.productId}" data-product-name="${m.name}" data-price="${m.price}">
                    <div style="display:flex;justify-content:space-between;align-items:baseline">
                        <strong>${m.name}</strong>
                        <span class="muted"><fmt:formatNumber value="${m.price}" maxFractionDigits="0"/> ₫</span>
                    </div>
                    <c:forEach var="g" items="${m.groups}">
                        <div class="pos-group" style="margin-top:8px">
                            <div class="muted" style="font-size:.8rem;text-transform:uppercase;letter-spacing:.04em">${g.name}</div>
                            <c:forEach var="o" items="${g.options}">
                                <label style="display:flex;gap:6px;align-items:center;font-size:.92rem">
                                    <input type="${g.maxSelect == 1 ? 'radio' : 'checkbox'}"
                                           name="grp-${m.productId}-${g.groupId}"
                                           class="pos-opt" data-option-id="${o.modifierOptionId}"
                                           data-delta="${o.priceDelta}" data-name="${o.name}">
                                    ${o.name}<c:if test="${o.priceDelta > 0}"> <span class="muted">(+<fmt:formatNumber value="${o.priceDelta}" maxFractionDigits="0"/>₫)</span></c:if>
                                </label>
                            </c:forEach>
                        </div>
                    </c:forEach>
                    <div style="display:flex;gap:8px;align-items:center;margin-top:10px">
                        <input type="number" class="form-control pos-qty" value="1" min="1" style="width:70px">
                        <button type="button" class="btn btn-primary btn-sm" onclick="addToCart(this)">Thêm vào giỏ</button>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>

    <div class="card" style="position:sticky;top:16px">
        <h3 style="margin-top:0">Giỏ hàng</h3>
        <div class="form-group" style="margin-bottom:10px">
            <label>Bàn / phiên</label>
            <select id="sessionSelect" class="form-control">
                <option value="">— Đem về (takeaway) —</option>
                <c:forEach var="s" items="${openSessions}">
                    <option value="${s.tableSessionId}" ${sessionId == s.tableSessionId ? 'selected' : ''}>${s.tableNumber} (#${s.tableSessionId})</option>
                </c:forEach>
            </select>
        </div>
        <div id="cartLines"></div>
        <div style="display:flex;justify-content:space-between;margin:12px 0;font-weight:700;border-top:1px solid var(--line);padding-top:10px">
            <span>Tổng</span><span id="cartTotal">0 ₫</span>
        </div>
        <button id="submitBtn" type="button" class="btn btn-primary btn-lg" style="width:100%" onclick="submitOrder()" disabled>Gửi đơn</button>
        <div id="posMsg" class="muted" style="margin-top:10px"></div>
    </div>
</div>

<script>
const CSRF = '${sessionScope.csrfToken}';
const CTX = '${ctx}';
let cart = [];

function fmt(n){ return new Intl.NumberFormat('vi-VN').format(n) + ' ₫'; }

function addToCart(btn){
  const card = btn.closest('.pos-product');
  const productId = parseInt(card.dataset.productId);
  const name = card.dataset.productName;
  const base = parseFloat(card.dataset.price);
  const qty = Math.max(1, parseInt(card.querySelector('.pos-qty').value) || 1);
  let delta = 0; const optionIds = []; const optNames = [];
  card.querySelectorAll('.pos-opt:checked').forEach(o => {
    delta += parseFloat(o.dataset.delta);
    optionIds.push(parseInt(o.dataset.optionId));
    optNames.push(o.dataset.name);
  });
  const unit = base + delta;
  cart.push({productId, name, quantity: qty, unit, optionIds, optNames});
  // reset lựa chọn
  card.querySelectorAll('.pos-opt:checked').forEach(o => o.checked = false);
  card.querySelector('.pos-qty').value = 1;
  renderCart();
}

function removeLine(i){ cart.splice(i,1); renderCart(); }

function renderCart(){
  const box = document.getElementById('cartLines');
  if(cart.length === 0){ box.innerHTML = '<p class="muted">Giỏ trống.</p>'; }
  else {
    box.innerHTML = cart.map((l,i) =>
      '<div style="display:flex;justify-content:space-between;gap:8px;padding:6px 0;border-bottom:1px dashed var(--line)">' +
        '<div><strong>' + l.quantity + '× ' + l.name + '</strong>' +
          (l.optNames.length ? '<br><span class="muted" style="font-size:.85rem">' + l.optNames.join(', ') + '</span>' : '') +
        '</div>' +
        '<div style="text-align:right;white-space:nowrap">' + fmt(l.unit*l.quantity) +
          ' <a href="javascript:void(0)" onclick="removeLine(' + i + ')" title="Xoá">×</a></div>' +
      '</div>').join('');
  }
  const total = cart.reduce((s,l)=> s + l.unit*l.quantity, 0);
  document.getElementById('cartTotal').textContent = fmt(total);
  document.getElementById('submitBtn').disabled = cart.length === 0;
}

function submitOrder(){
  const sessionId = document.getElementById('sessionSelect').value;
  const payload = {
    sessionId: sessionId ? parseInt(sessionId) : null,
    orderType: sessionId ? 'DINE_IN' : 'TAKEAWAY',
    items: cart.map(l => ({productId: l.productId, quantity: l.quantity, optionIds: l.optionIds}))
  };
  const msg = document.getElementById('posMsg');
  msg.textContent = 'Đang gửi...';
  fetch(CTX + '/cashier/pos?_csrf=' + encodeURIComponent(CSRF), {
    method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload)
  }).then(r => r.json().then(j => ({ok:r.ok, j}))).then(({ok,j}) => {
    if(ok){ msg.innerHTML = '<span style="color:var(--st-ready)">✓ Đã gửi đơn #' + j.orderId + ' tới bếp.</span>'; cart=[]; renderCart(); }
    else { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi: ' + (j.error||'không xác định') + '</span>'; }
  }).catch(e => { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi mạng.</span>'; });
}

renderCart();
</script>

<jsp:include page="../layout/footer.jsp" />
