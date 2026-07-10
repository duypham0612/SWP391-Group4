<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Hàng chờ pha</h1>
        <p>Pha xong bấm “Xong” — hệ thống tự trừ nguyên liệu. Hết nguyên liệu thì bấm “Không pha được”.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/barista/pickup">Món chờ giao →</a>
</div>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div class="kds-toolbar">
    <div class="seg" id="kdsDensity" role="group" aria-label="Mật độ hiển thị">
        <button type="button" class="seg__btn" data-dens="0">Thoáng</button>
        <button type="button" class="seg__btn" data-dens="1">Gọn</button>
    </div>
</div>

<div id="kdsBoard" class="kds-board ${onShift ? '' : 'is-viewonly'}">
    <jsp:include page="kds_cards.jsp" />
</div>

<div class="kds-refresh muted">
    <span class="kds-refresh__dot"></span>
    Tự cập nhật mỗi <span id="kdsCountdown">5</span> giây
</div>

<%-- Dialog "Không pha được" — huỷ đúng món + tuỳ chọn đánh dấu hết món (86) --%>
<div id="cantMakeModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="cantMakeTitle">
        <h3 id="cantMakeTitle">Không pha được món</h3>
        <p class="muted kds-modal__name" id="cantMakeName"></p>
        <form id="cantMakeForm" action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="cantMake">
            <input type="hidden" name="orderItemId" id="cantItemId">
            <input type="hidden" name="productId" id="cantProductId">
            <label class="kds-field">
                <span>Lý do (tuỳ chọn)</span>
                <input type="text" name="reason" maxlength="120" placeholder="VD: hết sữa tươi" autocomplete="off">
            </label>
            <label class="kds-check">
                <input type="checkbox" name="also86" value="true" id="cantAlso86">
                <span>Đánh dấu hết món này (86) — khoá khỏi POS &amp; QR khách</span>
            </label>
            <label class="kds-field kds-eta" hidden>
                <span>Dự kiến có lại (tuỳ chọn)</span>
                <input type="datetime-local" name="backInEta">
            </label>
            <div class="kds-modal__actions">
                <button type="button" class="btn btn-ghost" data-close>Huỷ</button>
                <button type="submit" class="btn btn-danger">Xác nhận huỷ món</button>
            </div>
        </form>
    </div>
</div>

<script>
  (function(){
    var ctx = '${ctx}';
    var board = document.getElementById('kdsBoard');
    var countdown = document.getElementById('kdsCountdown');
    var modal = document.getElementById('cantMakeModal');
    var cantForm = document.getElementById('cantMakeForm');
    var also86 = document.getElementById('cantAlso86');
    var etaWrap = modal ? modal.querySelector('.kds-eta') : null;
    var n = 5;
    var knownIds = readIds();
    var refreshing = false;
    var suppressUntil = 0;   // tạm ngưng polling ngay sau một thao tác để không đè kết quả

    // Chế độ Thoáng / Gọn (mật độ) — lưu localStorage, áp lên #kdsBoard (bền qua các lần refresh AJAX)
    (function(){
      var seg = document.getElementById('kdsDensity');
      if (!board || !seg) return;
      var btns = Array.prototype.slice.call(seg.querySelectorAll('.seg__btn[data-dens]'));
      function apply(){
        var compact = localStorage.getItem('kdsCompact') === '1';
        board.classList.toggle('is-compact', compact);
        btns.forEach(function(b){ b.classList.toggle('is-active', b.getAttribute('data-dens') === (compact ? '1' : '0')); });
      }
      btns.forEach(function(b){
        b.addEventListener('click', function(){ localStorage.setItem('kdsCompact', b.getAttribute('data-dens')); apply(); });
      });
      apply();
    })();

    function readIds(){
      var ids = {};
      if (board) board.querySelectorAll('[data-kds-ticket-id]').forEach(function(c){
        ids[c.getAttribute('data-kds-ticket-id')] = true;
      });
      return ids;
    }

    function markNew(){
      board.querySelectorAll('[data-kds-ticket-id]').forEach(function(c){
        if (!knownIds[c.getAttribute('data-kds-ticket-id')]) c.classList.add('kds-new');
      });
      knownIds = readIds();
    }

    async function postForm(form){
      suppressUntil = Date.now() + 1500;
      var body = new FormData(form);
      body.append('ajax', '1');
      var res = await fetch(form.action, {method:'POST', body: body, credentials:'same-origin'});
      if (!res.ok) throw new Error('post failed');
      board.innerHTML = await res.text();
      markNew();
    }

    // Mọi form trong bảng gửi qua AJAX → không reload cả trang, giữ nguyên chỗ cuộn
    if (board) board.addEventListener('submit', function(e){
      var form = e.target.closest('form');
      if (!form) return;
      e.preventDefault();
      var msg = form.getAttribute('data-confirm');
      if (msg && !window.confirm(msg)) return;
      postForm(form).catch(function(){ form.submit(); });   // lỗi mạng → submit thường (fallback)
    });

    // Mở dialog "Không pha được"
    function toggleEta(){ if (etaWrap) etaWrap.hidden = !(also86 && also86.checked); }
    function closeModal(){ if (modal) modal.hidden = true; }

    if (board) board.addEventListener('click', function(e){
      var btn = e.target.closest('.js-cantmake');
      if (!btn || !modal) return;
      cantForm.reset();
      document.getElementById('cantItemId').value = btn.dataset.itemId;
      document.getElementById('cantProductId').value = btn.dataset.productId;
      document.getElementById('cantMakeName').textContent = btn.dataset.name;
      toggleEta();
      modal.hidden = false;
    });

    if (also86) also86.addEventListener('change', toggleEta);
    if (modal) modal.addEventListener('click', function(e){ if (e.target.hasAttribute('data-close')) closeModal(); });
    document.addEventListener('keydown', function(e){ if (e.key === 'Escape') closeModal(); });
    if (cantForm) cantForm.addEventListener('submit', function(e){
      e.preventDefault();
      postForm(cantForm).then(closeModal).catch(function(){ cantForm.submit(); });
    });

    async function refresh(){
      if (!board || refreshing || document.visibilityState === 'hidden' || Date.now() < suppressUntil) return;
      refreshing = true;
      try {
        var res = await fetch(ctx + '/barista/kds?partial=1', {credentials:'same-origin'});
        if (!res.ok) return;
        board.innerHTML = await res.text();
        markNew();
      } finally { refreshing = false; }
    }

    setInterval(function(){
      if (document.visibilityState === 'hidden') return;
      n -= 1;
      if (n <= 0) { n = 5; refresh(); }
      if (countdown) countdown.textContent = n;
    }, 1000);
  })();
</script>
<jsp:include page="../layout/footer.jsp" />
