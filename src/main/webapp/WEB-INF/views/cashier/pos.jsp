<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>POS - Đặt món</h1><p>Chọn size, đá, đường rồi gửi món tới quầy pha chế</p></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/table">← Sơ đồ bàn</a>
</div>

<style>
  .option-row{display:grid;gap:8px;margin-top:10px}
  .option-row .lbl{font-size:.78rem;text-transform:uppercase;color:var(--muted);font-weight:700}
  .seg-options{display:flex;gap:6px;flex-wrap:wrap}
  .seg-options input{position:absolute;opacity:0;pointer-events:none}
  .seg-options span{display:inline-flex;align-items:center;justify-content:center;min-height:34px;padding:0 11px;border:1px solid var(--line);border-radius:999px;background:var(--surface);font-weight:700;font-size:.9rem;cursor:pointer}
  .seg-options input:checked + span{background:var(--coffee);border-color:var(--coffee);color:#fff}
  .cart-option{font-size:.85rem;color:var(--muted);margin-top:2px}
</style>

<div style="display:grid;grid-template-columns:1fr 360px;gap:20px;align-items:start">
    <div>
        <c:if test="${empty menu}">
            <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào bán ở chi nhánh.</p></div>
        </c:if>
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:14px">
            <c:forEach var="m" items="${menu}">
                <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                <div class="card pos-product"
                     data-product-id="${m.productId}" data-product-name="${m.name}" data-price="${m.price}"
                     data-size-enabled="${m.sizeEnabled}" data-size-s="${m.sizeSDelta}" data-size-m="${m.sizeMDelta}" data-size-l="${m.sizeLDelta}">
                    <img class="pos-product__img" src="${imgSrc}" alt="${m.name}" loading="lazy"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                    <div class="pos-product__body">
                        <div style="display:flex;justify-content:space-between;align-items:baseline;gap:10px">
                            <strong>${m.name}</strong>
                            <span class="muted"><fmt:formatNumber value="${m.price}" maxFractionDigits="0"/> ₫</span>
                        </div>

                        <c:if test="${m.sizeEnabled}">
                            <div class="option-row">
                                <div class="lbl">Size</div>
                                <div class="seg-options">
                                    <label><input type="radio" class="pos-size" name="size-${m.productId}" value="S"><span>S<c:if test="${m.sizeSDelta > 0}"> +<fmt:formatNumber value="${m.sizeSDelta}" maxFractionDigits="0"/></c:if></span></label>
                                    <label><input type="radio" class="pos-size" name="size-${m.productId}" value="M" checked><span>M<c:if test="${m.sizeMDelta > 0}"> +<fmt:formatNumber value="${m.sizeMDelta}" maxFractionDigits="0"/></c:if></span></label>
                                    <label><input type="radio" class="pos-size" name="size-${m.productId}" value="L"><span>L<c:if test="${m.sizeLDelta > 0}"> +<fmt:formatNumber value="${m.sizeLDelta}" maxFractionDigits="0"/></c:if></span></label>
                                </div>
                            </div>
                        </c:if>

                        <div class="option-row">
                            <div class="lbl">Đá</div>
                            <select class="form-control pos-ice">
                                <option>Không đá</option><option>Ít đá</option><option selected>Bình thường</option><option>Nhiều đá</option>
                            </select>
                        </div>
                        <div class="option-row">
                            <div class="lbl">Đường</div>
                            <select class="form-control pos-sugar">
                                <option>0%</option><option>30%</option><option>50%</option><option>70%</option><option selected>100%</option>
                            </select>
                        </div>

                        <div style="display:flex;gap:8px;align-items:center;margin-top:12px">
                            <input type="number" class="form-control pos-qty" value="1" min="1" style="width:70px">
                            <button type="button" class="btn btn-primary btn-sm" onclick="addToCart(this)">Thêm vào giỏ</button>
                        </div>
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
                <option value="">- Đem về (takeaway) -</option>
                <c:forEach var="s" items="${openSessions}">
                    <option value="${s.tableSessionId}" ${sessionId == s.tableSessionId ? 'selected' : ''}>${s.tableNumber} (#${s.tableSessionId})</option>
                </c:forEach>
            </select>
        </div>
        <c:if test="${not empty sessionId}">
            <div style="border-bottom:1px solid var(--line);padding-bottom:12px;margin-bottom:12px">
                <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:8px">
                    <strong>Món đã gửi</strong>
                    <a class="btn btn-ghost btn-sm" href="${ctx}/cashier/checkout?sessionId=${sessionId}">Thanh toán</a>
                </div>
                <c:choose>
                    <c:when test="${empty sessionItems}">
                        <p class="muted" style="margin:0">Chưa gửi món nào cho phiên này.</p>
                    </c:when>
                    <c:otherwise>
                        <div style="display:flex;flex-direction:column;gap:8px">
                            <c:forEach var="it" items="${sessionItems}">
                                <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start">
                                    <div>
                                        <strong>${it.quantity}× ${it.productName}</strong>
                                        <div class="muted" style="font-size:.85rem">${it.note}</div>
                                    </div>
                                    <jsp:include page="../layout/_statusBadge.jsp">
                                        <jsp:param name="status" value="${it.status}" />
                                    </jsp:include>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <div id="cartLines"></div>
        <div style="display:flex;justify-content:space-between;margin:12px 0;font-weight:700;border-top:1px solid var(--line);padding-top:10px">
            <span>Tổng</span><span id="cartTotal">0 ₫</span>
        </div>
        <button id="submitBtn" type="button" class="btn btn-primary btn-lg" style="width:100%" onclick="submitOrder()" disabled>Gửi đơn</button>
        <c:if test="${not empty sessionId}">
            <div style="display:flex;gap:8px;margin-top:8px">
                <button type="button" class="btn btn-ghost btn-sm" style="flex:1" onclick="saveDraft()">Tạm dừng</button>
                <button type="button" class="btn btn-ghost btn-sm" style="flex:1" onclick="discardDraft()">Hủy đặt món</button>
            </div>
        </c:if>
        <div id="posMsg" class="muted" style="margin-top:10px"></div>
    </div>
</div>

<form id="draftForm" action="${ctx}/cashier/pos" method="post" style="display:none">
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="action" id="draftAction" value="">
    <input type="hidden" name="sessionId" id="draftSessionId" value="${sessionId}">
    <input type="hidden" name="cartJson" id="draftCartJson" value="">
</form>

<script>
const CSRF = '${sessionScope.csrfToken}';
const CTX = '${ctx}';
let cart = ${empty draftCartJson ? '[]' : draftCartJson};

function fmt(n){ return new Intl.NumberFormat('vi-VN').format(n) + ' ₫'; }
function money(v){ const n = parseFloat(v); return Number.isFinite(n) ? n : 0; }
function esc(v){ return String(v || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

function selectedSize(card){
  if(card.dataset.sizeEnabled !== 'true') return 'M';
  const picked = card.querySelector('.pos-size:checked');
  return picked ? picked.value : 'M';
}

function sizeDelta(card, size){
  if(card.dataset.sizeEnabled !== 'true') return 0;
  if(size === 'S') return money(card.dataset.sizeS);
  if(size === 'L') return money(card.dataset.sizeL);
  return money(card.dataset.sizeM);
}

function resetProduct(card){
  const m = card.querySelector('.pos-size[value="M"]'); if(m) m.checked = true;
  card.querySelector('.pos-ice').value = 'Bình thường';
  card.querySelector('.pos-sugar').value = '100%';
  card.querySelector('.pos-qty').value = 1;
}

function addToCart(btn){
  const card = btn.closest('.pos-product');
  const productId = parseInt(card.dataset.productId);
  const name = card.dataset.productName;
  const size = selectedSize(card);
  const iceLevel = card.querySelector('.pos-ice').value;
  const sugarLevel = card.querySelector('.pos-sugar').value;
  const base = money(card.dataset.price);
  const qty = Math.max(1, parseInt(card.querySelector('.pos-qty').value) || 1);
  const unit = base + sizeDelta(card, size);
  cart.push({productId, name, quantity: qty, unit, size, iceLevel, sugarLevel});
  resetProduct(card);
  renderCart();
}

function removeLine(i){ cart.splice(i,1); renderCart(); }

function optionText(l){
  return 'Size ' + (l.size || 'M') + ', ' + (l.iceLevel || 'Bình thường') + ', đường ' + (l.sugarLevel || '100%');
}

function renderCart(){
  const box = document.getElementById('cartLines');
  if(cart.length === 0){ box.innerHTML = '<p class="muted">Giỏ trống.</p>'; }
  else {
    box.innerHTML = cart.map((l,i) =>
      '<div style="display:flex;justify-content:space-between;gap:8px;padding:6px 0;border-bottom:1px dashed var(--line)">' +
        '<div><strong>' + esc(l.quantity) + '× ' + esc(l.name) + '</strong><div class="cart-option">' + esc(optionText(l)) + '</div></div>' +
        '<div style="text-align:right;white-space:nowrap">' + fmt((money(l.unit))*l.quantity) +
          ' <a href="javascript:void(0)" onclick="removeLine(' + i + ')" title="Xóa">×</a></div>' +
      '</div>').join('');
  }
  const total = cart.reduce((s,l)=> s + money(l.unit)*l.quantity, 0);
  document.getElementById('cartTotal').textContent = fmt(total);
  document.getElementById('submitBtn').disabled = cart.length === 0;
}

function submitOrder(){
  const sessionId = document.getElementById('sessionSelect').value;
  const payload = {
    sessionId: sessionId ? parseInt(sessionId) : null,
    orderType: sessionId ? 'DINE_IN' : 'TAKEAWAY',
    items: cart.map(l => ({productId: l.productId, quantity: l.quantity, size: l.size, iceLevel: l.iceLevel, sugarLevel: l.sugarLevel}))
  };
  const msg = document.getElementById('posMsg');
  msg.textContent = 'Đang gửi...';
  fetch(CTX + '/cashier/pos?_csrf=' + encodeURIComponent(CSRF), {
    method: 'POST', headers: {'Content-Type':'application/json','Accept':'application/json'}, body: JSON.stringify(payload)
  }).then(r => r.json().then(j => ({ok:r.ok, j}))).then(({ok,j}) => {
    if(ok){ msg.innerHTML = '<span style="color:var(--st-ready)">✓ Đã gửi đơn #' + j.orderId + ' tới quầy pha chế.</span>'; cart=[]; renderCart(); }
    else { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi: ' + esc(j.error || 'không xác định') + '</span>'; }
  }).catch(() => { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi mạng.</span>'; });
}

function submitDraftAction(action){
  const sessionId = document.getElementById('sessionSelect').value;
  const msg = document.getElementById('posMsg');
  if(!sessionId){
    msg.innerHTML = '<span style="color:var(--st-cancelled)">Chỉ lưu nháp cho phiên bàn.</span>';
    return;
  }
  document.getElementById('draftAction').value = action;
  document.getElementById('draftSessionId').value = sessionId;
  document.getElementById('draftCartJson').value = JSON.stringify(cart);
  document.getElementById('draftForm').submit();
}

function saveDraft(){ submitDraftAction('saveDraft'); }

function discardDraft(){
  if(confirm('Hủy giỏ nháp của phiên này? Nếu chưa gửi món, bàn sẽ về Trống.')){
    submitDraftAction('discardDraft');
  }
}

renderCart();
</script>

<jsp:include page="../layout/footer.jsp" />
