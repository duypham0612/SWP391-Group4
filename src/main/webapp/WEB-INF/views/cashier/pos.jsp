<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="cssBundles" value="pos" scope="request" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>POS — Đặt món</h1></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/table">← Sơ đồ bàn</a>
</div>

<c:if test="${not empty outOfStockItems}">
    <div class="alert alert-error">
        <strong>Món hiện không nhận đặt:</strong>
        <c:forEach var="m" items="${outOfStockItems}" varStatus="loop">
            <c:out value="${m.name}" /> (<c:out value="${view.stockMessage(m)}" />)${loop.last ? '' : ' · '}
        </c:forEach>
    </div>
</c:if>
<c:if test="${not empty lowStockItems}">
    <div class="alert alert-info">
        <strong>Cảnh báo sắp hết — vẫn có thể đặt:</strong>
        <c:forEach var="m" items="${lowStockItems}" varStatus="loop">
            <c:out value="${m.name}" /> (<c:out value="${view.stockMessage(m)}" />)${loop.last ? '' : ' · '}
        </c:forEach>
    </div>
</c:if>

<div style="display:grid;grid-template-columns:1fr 360px;gap:20px;align-items:start">
    <div>
        <c:if test="${empty menu}">
            <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào bán ở chi nhánh (Admin publish + Manager bật bán, Barista chưa 86).</p></div>
        </c:if>
        <div class="pos-search-bar">
            <label class="pos-search" for="menuSearch">
                <span aria-hidden="true">&#128269;</span>
                <input id="menuSearch" type="search" class="form-control" autocomplete="off"
                       placeholder="T&#236;m m&#243;n theo t&#234;n..." aria-label="T&#236;m m&#243;n">
            </label>
            <span id="menuSearchCount" class="muted"></span>
        </div>
        <div id="menuGrid" class="pos-menu-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px">
            <c:forEach var="m" items="${menu}">
                <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                <div class="card pos-product" data-product-id="${m.productId}" data-product-name="${m.name}"
                     data-price="${m.price}" data-orderable="${m.orderable}" data-search="${m.name}"
                     style="${m.orderable ? '' : 'opacity:.72;border-color:var(--st-cancelled)'}">
                    <img class="pos-product__img" src="${imgSrc}" alt="${m.name}" loading="lazy"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                  <div class="pos-product__body">
                    <div style="display:flex;justify-content:space-between;align-items:baseline">
                        <strong>${m.name}</strong>
                        <span class="muted">${view.grouped(m.price)} ₫</span>
                    </div>
                    <c:if test="${m.availabilityState == 'LOW'}">
                        <div class="badge badge-waiting" style="margin-top:8px">⚠ <c:out value="${view.stockMessage(m)}" /></div>
                    </c:if>
                    <c:if test="${m.availabilityState == 'OUT'}">
                        <div class="badge badge-cancelled" style="margin-top:8px">Hết món · <c:out value="${view.stockMessage(m)}" /></div>
                    </c:if>
                    <c:if test="${m.availabilityState == 'EIGHTY_SIX'}">
                        <div class="badge badge-cancelled" style="margin-top:8px"><c:out value="${view.stockMessage(m)}" /></div>
                    </c:if>
                    <div class="pos-product__config" hidden>
                    <c:forEach var="g" items="${m.groups}">
                        <div class="pos-group" style="margin-top:8px"
                             data-group-name="${g.name}" data-required="${g.required}" data-min="${g.minSelect}" data-max="${g.maxSelect}">
                            <div class="muted" style="font-size:.8rem;text-transform:uppercase;letter-spacing:.04em">${g.name}</div>
                            <c:forEach var="o" items="${g.options}">
                                <c:set var="isDefault" value="${view.modifierDefault(g.name, o.name)}" />
                                <label style="display:flex;gap:6px;align-items:center;font-size:.92rem">
                                    <input type="${g.maxSelect == 1 ? 'radio' : 'checkbox'}"
                                           name="grp-${m.productId}-${g.groupId}"
                                           class="pos-opt" data-option-id="${o.modifierOptionId}"
                                           data-delta="${o.priceDelta}" data-name="${o.name}"
                                           data-default="${isDefault}" ${isDefault ? 'checked' : ''}>
                                    ${o.name}<c:if test="${o.priceDelta > 0}"> <span class="muted">(+${view.grouped(o.priceDelta)}₫)</span></c:if>
                                </label>
                            </c:forEach>
                        </div>
                    </c:forEach>
                    </div>
                    <div class="pos-error" style="display:none;color:var(--st-cancelled);font-size:.86rem;margin-top:8px"></div>
                    <div class="form-group" style="margin:10px 0 0">
                        <label class="muted" style="font-size:.82rem">Ghi chú cho Barista</label>
                        <input type="text" class="form-control pos-note" maxlength="255" placeholder="VD: tách đá riêng">
                    </div>
                    <div style="display:flex;gap:8px;align-items:center;margin-top:10px">
                        <input type="number" class="form-control pos-qty" value="1" min="1" max="20"
                               style="width:70px" ${m.orderable ? '' : 'disabled'}>
                        <button type="button" class="btn btn-primary btn-sm" onclick="openModifierModal(this)"
                                ${m.orderable ? '' : 'disabled'}>${m.orderable ? 'Thêm vào giỏ' : 'Không thể thêm'}</button>
                    </div>
                  </div>
                </div>
            </c:forEach>
        </div>
    </div>

    <div class="card" style="position:sticky;top:16px">
        <h3 style="margin-top:0">Giỏ hàng</h3>
        <div class="form-group" style="margin-bottom:10px">
            <label>Bàn</label>
            <select id="sessionSelect" class="form-control">
                <option value="">— Đem về (takeaway) —</option>
                <c:forEach var="t" items="${openTables}">
                    <option value="${t.diningTableId}" ${tableId == t.diningTableId ? 'selected' : ''}>${t.tableNumber}</option>
                </c:forEach>
            </select>
        </div>
        <c:if test="${not empty tableId}">
            <div style="border-bottom:1px solid var(--line);padding-bottom:12px;margin-bottom:12px">
                <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:8px">
                    <strong>Món đã gửi</strong>
                    <a class="btn btn-ghost btn-sm" href="${ctx}/cashier/checkout?tableId=${tableId}">Thanh toán</a>
                </div>
                <c:choose>
                    <c:when test="${empty tableItems}">
                        <p class="muted" style="margin:0">Chưa gửi món nào cho bàn này.</p>
                    </c:when>
                    <c:otherwise>
                        <div style="display:flex;flex-direction:column;gap:8px">
                            <c:forEach var="it" items="${tableItems}">
                                <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start">
                                    <div>
                                        <strong>${it.quantity}× ${it.productName}</strong>
                                        <div class="muted" style="font-size:.85rem">#${it.orderItemId}</div>
                                    </div>
                                    <jsp:include page="/WEB-INF/fragments/cashier/status-badge.jsp">
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
        <c:if test="${not empty tableId}">
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
    <input type="hidden" name="tableId" id="draftTableId" value="${tableId}">
    <input type="hidden" name="cartJson" id="draftCartJson" value="">
</form>

<div id="modifierModal" class="pos-modal" hidden>
    <div class="pos-modal__backdrop" data-modal-close></div>
    <section class="pos-modal__panel" role="dialog" aria-modal="true" aria-labelledby="modifierModalTitle">
        <button type="button" class="icon-btn pos-modal__close" data-modal-close aria-label="&#272;&#243;ng">&times;</button>
        <p class="eyebrow">T&#249;y ch&#7885;n m&#243;n</p>
        <h2 id="modifierModalTitle"></h2>
        <p id="modifierModalPrice" class="muted"></p>
        <div id="modifierModalOptions" class="pos-modal__options"></div>
        <p id="modifierModalError" class="pos-error" role="alert"></p>
        <div class="pos-modal__fields">
            <label class="form-group"><span>S&#7889; l&#432;&#7907;ng</span><input id="modifierModalQty" type="number" class="form-control" value="1" min="1" max="20"></label>
            <label class="form-group"><span>Ghi ch&#250; cho Barista</span><input id="modifierModalNote" type="text" class="form-control" maxlength="255" placeholder="VD: t&#225;ch &#273;&#225; ri&#234;ng"></label>
        </div>
        <div class="pos-modal__actions">
            <button type="button" class="btn btn-ghost" data-modal-close>H&#7911;y</button>
            <button id="modifierModalAdd" type="button" class="btn btn-primary">Th&#234;m v&#224;o gi&#7887;</button>
        </div>
    </section>
</div>

<script>
const CSRF = '${sessionScope.csrfToken}';
const CTX = '${ctx}';
let cart = ${empty draftCartJson ? '[]' : draftCartJson};
let activeProduct = null;

const modifierModal = document.getElementById('modifierModal');
const modifierModalOptions = document.getElementById('modifierModalOptions');
const modifierModalError = document.getElementById('modifierModalError');

function optionKey(optionIds){ return optionIds.slice().sort((a, b) => a - b).join(','); }

function showModalError(text){
  modifierModalError.textContent = text || '';
  modifierModalError.style.display = text ? 'block' : 'none';
}

function validateModalModifiers(){
  for (const group of modifierModalOptions.querySelectorAll('.pos-group')) {
    const name = group.dataset.groupName || 'tuy chon';
    const min = parseInt(group.dataset.min || '0');
    const max = parseInt(group.dataset.max || '0');
    const required = group.dataset.required === 'true';
    const checked = group.querySelectorAll('.pos-opt:checked').length;
    if ((required || min > 0) && checked < min) {
      showModalError('Vui l\u00f2ng ch\u1ecdn ' + name + '.');
      return false;
    }
    if (max > 0 && checked > max) {
      showModalError(name + ' ch\u1ec9 \u0111\u01b0\u1ee3c ch\u1ecdn t\u1ed1i \u0111a ' + max + ' t\u00f9y ch\u1ecdn.');
      return false;
    }
  }
  showModalError('');
  return true;
}

function openModifierModal(btn){
  const card = btn.closest('.pos-product');
  if (!card || card.dataset.orderable !== 'true') return;
  activeProduct = card;
  modifierModalOptions.innerHTML = card.querySelector('.pos-product__config').innerHTML;
  document.getElementById('modifierModalTitle').textContent = card.dataset.productName;
  document.getElementById('modifierModalPrice').textContent = fmt(parseFloat(card.dataset.price));
  document.getElementById('modifierModalQty').value = 1;
  document.getElementById('modifierModalNote').value = '';
  showModalError('');
  modifierModal.hidden = false;
  document.body.classList.add('pos-modal-open');
  document.getElementById('modifierModalQty').focus();
}

function closeModifierModal(){
  modifierModal.hidden = true;
  activeProduct = null;
  document.body.classList.remove('pos-modal-open');
}

function addModalToCart(){
  if (!activeProduct || !validateModalModifiers()) return;
  const productId = parseInt(activeProduct.dataset.productId);
  const name = activeProduct.dataset.productName;
  const base = parseFloat(activeProduct.dataset.price);
  const qty = parseInt(document.getElementById('modifierModalQty').value);
  const note = document.getElementById('modifierModalNote').value.trim();
  const currentQty = cart.filter(line => line.productId === productId)
      .reduce((sum, line) => sum + line.quantity, 0);
  if (!Number.isInteger(qty) || qty < 1 || qty > 20 || currentQty + qty > 20) {
    showModalError('M\u1ed7i lo\u1ea1i m\u00f3n ch\u1ec9 \u0111\u01b0\u1ee3c \u0111\u1eb7t t\u1ed1i \u0111a 20 trong m\u1ed9t \u0111\u01a1n.');
    return;
  }
  let delta = 0;
  const optionIds = [];
  const optNames = [];
  modifierModalOptions.querySelectorAll('.pos-opt:checked').forEach(option => {
    delta += parseFloat(option.dataset.delta);
    optionIds.push(parseInt(option.dataset.optionId));
    optNames.push(option.dataset.name);
  });
  const key = optionKey(optionIds);
  const existing = cart.find(line => line.productId === productId
      && (line.note || '').trim() === note && optionKey(line.optionIds || []) === key);
  if (existing) existing.quantity += qty;
  else cart.push({productId, name, quantity: qty, unit: base + delta, optionIds, optNames, note});
  closeModifierModal();
  renderCart();
}

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
  card.querySelector('.pos-note').value = '';
  showProductError(card, '');
}

function addToCart(btn){
  const card = btn.closest('.pos-product');
  if(card.dataset.orderable !== 'true'){
    showProductError(card, 'Món hiện không nhận đặt. Vui lòng chọn món khác.');
    return;
  }
  if(!validateProduct(card)) return;
  const productId = parseInt(card.dataset.productId);
  const name = card.dataset.productName;
  const base = parseFloat(card.dataset.price);
  const qty = parseInt(card.querySelector('.pos-qty').value);
  const note = card.querySelector('.pos-note').value.trim();
  const currentQty = cart
      .filter(line => line.productId === productId)
      .reduce((sum, line) => sum + line.quantity, 0);
  if (!Number.isInteger(qty) || qty < 1 || qty > 20 || currentQty + qty > 20) {
    showProductError(card, 'Mỗi loại món chỉ được đặt tối đa 20 trong một đơn.');
    return;
  }
  let delta = 0; const optionIds = []; const optNames = [];
  card.querySelectorAll('.pos-opt:checked').forEach(o => {
    delta += parseFloat(o.dataset.delta);
    optionIds.push(parseInt(o.dataset.optionId));
    optNames.push(o.dataset.name);
  });
  const unit = base + delta;
  cart.push({productId, name, quantity: qty, unit, optionIds, optNames, note});
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
          (l.note ? '<br><span class="muted" style="font-size:.85rem">Ghi chú: ' + l.note.replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</span>' : '') +
          (l.unavailable ? '<br><span style="color:var(--st-cancelled);font-size:.82rem">⚠ ' + l.unavailableReason + '</span>' : '') +
        '</div>' +
        '<div style="text-align:right;white-space:nowrap">' + fmt(l.unit*l.quantity) +
          ' <a href="javascript:void(0)" onclick="removeLine(' + i + ')" title="Xoá">×</a></div>' +
      '</div>').join('');
  }
  const total = cart.reduce((s,l)=> s + l.unit*l.quantity, 0);
  document.getElementById('cartTotal').textContent = fmt(total);
  document.getElementById('submitBtn').disabled = cart.length === 0 || cart.some(l => l.unavailable);
}

function submitOrder(){
  const tableId = document.getElementById('sessionSelect').value;
  const payload = {
    tableId: tableId ? parseInt(tableId) : null,
    orderType: tableId ? 'DINE_IN' : 'TAKEAWAY',
    items: cart.map(l => ({productId: l.productId, quantity: l.quantity, optionIds: l.optionIds, note: l.note || null}))
  };
  const msg = document.getElementById('posMsg');
  msg.textContent = 'Đang gửi...';
  fetch(CTX + '/cashier/pos?_csrf=' + encodeURIComponent(CSRF), {
    method: 'POST', headers: {'Content-Type':'application/json','Accept':'application/json'}, body: JSON.stringify(payload)
  }).then(r => r.json().then(j => ({ok:r.ok, j}))).then(({ok,j}) => {
    if(ok){
      msg.innerHTML = '<span style="color:var(--st-ready)">✓ Đã gửi đơn #' + j.orderId + ' tới bếp.</span>';
      cart=[]; renderCart();
    }
    else {
      if(j.code === 'ITEM_UNAVAILABLE' && j.productId){
        cart.forEach(l => {
          if(l.productId === j.productId){
            l.unavailable = true;
            l.unavailableReason = j.error || 'Món hiện không nhận đặt.';
          }
        });
        renderCart();
      }
      msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi: ' + (j.error||'không xác định') + '</span>';
    }
  }).catch(e => { msg.innerHTML = '<span style="color:var(--st-cancelled)">Lỗi mạng.</span>'; });
}

function submitDraftAction(action){
  const tableId = document.getElementById('sessionSelect').value;
  const msg = document.getElementById('posMsg');
  if(!tableId){
    msg.innerHTML = '<span style="color:var(--st-cancelled)">Chỉ lưu nháp cho bàn.</span>';
    return;
  }
  document.getElementById('draftAction').value = action;
  document.getElementById('draftTableId').value = tableId;
  document.getElementById('draftCartJson').value = JSON.stringify(cart);
  document.getElementById('draftForm').submit();
}

function saveDraft(){ submitDraftAction('saveDraft'); }

function discardDraft(){
  if(confirm('Hủy giỏ nháp của bàn này? Nếu chưa gửi món, bàn sẽ về Trống.')){
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

modifierModal.addEventListener('click', function(event){
  if (event.target.closest('[data-modal-close]')) closeModifierModal();
});
document.getElementById('modifierModalAdd').addEventListener('click', addModalToCart);
modifierModalOptions.addEventListener('change', function(event){
  const option = event.target;
  if (!option.classList.contains('pos-opt')) return;
  const group = option.closest('.pos-group');
  const max = parseInt(group.dataset.max || '0');
  if (option.type === 'checkbox' && max > 0 && group.querySelectorAll('.pos-opt:checked').length > max) {
    option.checked = false;
  }
  validateModalModifiers();
});
document.addEventListener('keydown', function(event){
  if (event.key === 'Escape' && !modifierModal.hidden) closeModifierModal();
});

function normalized(value){
  return (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}
const menuSearch = document.getElementById('menuSearch');
const menuSearchCount = document.getElementById('menuSearchCount');
menuSearch.addEventListener('input', function(){
  const query = normalized(menuSearch.value.trim());
  const cards = Array.from(document.querySelectorAll('.pos-product'));
  let visible = 0;
  cards.forEach(card => {
    const matches = !query || normalized(card.dataset.search).includes(query);
    card.hidden = !matches;
    if (matches) visible++;
  });
  menuSearchCount.textContent = query ? visible + ' m\u00f3n ph\u00f9 h\u1ee3p' : '';
});
renderCart();
</script>

<jsp:include page="../layout/footer.jsp" />
