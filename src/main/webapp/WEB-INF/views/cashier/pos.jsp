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
                <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                <div class="card pos-product" data-product-id="${m.productId}" data-product-name="${m.name}" data-price="${m.price}">
                    <img class="pos-product__img" src="${imgSrc}" alt="${m.name}" loading="lazy"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                  <div class="pos-product__body">
                    <div style="display:flex;justify-content:space-between;align-items:baseline">
                        <strong>${m.name}</strong>
                        <span class="muted"><fmt:formatNumber value="${m.price}" maxFractionDigits="0"/> ₫</span>
                    </div>
                    <c:forEach var="g" items="${m.groups}">
                        <div class="pos-group" style="margin-top:8px"
                             data-group-name="${g.name}" data-required="${g.required}" data-min="${g.minSelect}" data-max="${g.maxSelect}">
                            <div class="muted" style="font-size:.8rem;text-transform:uppercase;letter-spacing:.04em">${g.name}</div>
                            <c:forEach var="o" items="${g.options}">
                                <c:set var="isDefault" value="${(g.name == 'Size' and o.name == 'Size S') or ((g.name == 'Đá' or g.name == 'Đường') and o.name == 'Bình thường')}" />
                                <label style="display:flex;gap:6px;align-items:center;font-size:.92rem">
                                    <input type="${g.maxSelect == 1 ? 'radio' : 'checkbox'}"
                                           name="grp-${m.productId}-${g.groupId}"
                                           class="pos-opt" data-option-id="${o.modifierOptionId}"
                                           data-delta="${o.priceDelta}" data-name="${o.name}"
                                           data-default="${isDefault}" ${isDefault ? 'checked' : ''}>
                                    ${o.name}<c:if test="${o.priceDelta > 0}"> <span class="muted">(+<fmt:formatNumber value="${o.priceDelta}" maxFractionDigits="0"/>₫)</span></c:if>
                                </label>
                            </c:forEach>
                        </div>
                    </c:forEach>
                    <div class="pos-error" style="display:none;color:var(--st-cancelled);font-size:.86rem;margin-top:8px"></div>
                    <div style="display:flex;gap:8px;align-items:center;margin-top:10px">
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
                <option value="">— Đem về (takeaway) —</option>
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
                                        <div class="muted" style="font-size:.85rem">#${it.orderItemId}</div>
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

function showProductError(card, text){
  const box = card.querySelector('.pos-error');
  if(!box) return;
  box.textContent = text || '';
  box.style.display = text ? 'block' : 'none';
}

function validateProduct(card){
  for (const group of card.querySelectorAll('.pos-group')) {
    const name = group.dataset.groupName || 'Tuỳ chọn';
    const min = parseInt(group.dataset.min || '0');
    const max = parseInt(group.dataset.max || '0');
    const required = group.dataset.required === 'true';
    const checked = group.querySelectorAll('.pos-opt:checked').length;
    if ((required || min > 0) && checked < min) {
      showProductError(card, 'Vui lòng chọn ' + name + '.');
      return false;
    }
    if (max > 0 && checked > max) {
      showProductError(card, name + ' chỉ được chọn tối đa ' + max + ' tuỳ chọn.');
      return false;
    }
  }
  showProductError(card, '');
  return true;
}

function resetProduct(card){
  card.querySelectorAll('.pos-opt').forEach(o => { o.checked = o.dataset.default === 'true'; });
  card.querySelector('.pos-qty').value = 1;
  showProductError(card, '');
}

function addToCart(btn){
  const card = btn.closest('.pos-product');
  if(!validateProduct(card)) return;
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
  resetProduct(card);
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
    method: 'POST', headers: {'Content-Type':'application/json','Accept':'application/json'}, body: JSON.stringify(payload)
  }).then(r => r.json().then(j => ({ok:r.ok, j}))).then(({ok,j}) => {
    if(ok){ msg.innerHTML = '<span style="color:var(--st-ready)">✓ Đã gửi đơn #' + j.orderId + ' tới bếp.</span>'; cart=[]; renderCart(); }
    else { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi: ' + (j.error||'không xác định') + '</span>'; }
  }).catch(e => { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi mạng.</span>'; });
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

document.querySelectorAll('.pos-opt[type="checkbox"]').forEach(opt => {
  opt.addEventListener('change', function(){
    const group = this.closest('.pos-group');
    const card = this.closest('.pos-product');
    const max = parseInt(group.dataset.max || '0');
    if (max > 0 && group.querySelectorAll('.pos-opt:checked').length > max) {
      this.checked = false;
      showProductError(card, (group.dataset.groupName || 'Tuỳ chọn') + ' chỉ được chọn tối đa ' + max + ' tuỳ chọn.');
    } else {
      validateProduct(card);
    }
  });
});
document.querySelectorAll('.pos-opt[type="radio"]').forEach(opt => {
  opt.addEventListener('change', function(){ validateProduct(this.closest('.pos-product')); });
});
renderCart();
</script>

<jsp:include page="../layout/footer.jsp" />
