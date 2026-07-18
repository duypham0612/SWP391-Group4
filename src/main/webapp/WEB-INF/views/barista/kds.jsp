<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Quầy pha chế</h1>
        <p>Nhận món, pha và báo hoàn thành trên cùng một màn hình. Mỗi card là một dòng món độc lập.</p>
    </div>
    <div id="kdsConnection" class="kds-connection" role="status"><span class="kds-refresh__dot"></span><span>Đang kết nối</span></div>
</div>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div class="kds-toolbar">
    <div class="kds-filters" id="kdsFilters" role="group" aria-label="Lọc món">
        <button type="button" class="chip-filter is-active" data-filter="default">Ưu tiên của tôi</button>
        <button type="button" class="chip-filter" data-filter="all">Tất cả</button>
        <button type="button" class="chip-filter" data-filter="mine">Món của tôi</button>
        <button type="button" class="chip-filter" data-filter="unassigned">Chưa có người nhận</button>
        <button type="button" class="chip-filter" data-filter="station:COFFEE">Quầy cà phê</button>
        <button type="button" class="chip-filter" data-filter="station:TEA">Quầy trà</button>
        <button type="button" class="chip-filter" data-filter="station:BLENDER">Máy xay</button>
        <button type="button" class="chip-filter" data-filter="type:DINE_IN">Tại bàn</button>
        <button type="button" class="chip-filter" data-filter="type:TAKEAWAY">Mang đi</button>
        <button type="button" class="chip-filter" data-filter="type:DELIVERY">Giao hàng</button>
    </div>
    <div class="seg" id="kdsDensity" role="group" aria-label="Mật độ hiển thị">
        <button type="button" class="seg__btn" data-dens="0">Thoáng</button><button type="button" class="seg__btn" data-dens="1">Gọn</button>
    </div>
</div>

<div id="kdsBoard" class="kds-board" data-user-id="${currentUserId}" aria-live="polite">
    <jsp:include page="kds_cards.jsp" />
</div>
<div id="kdsLiveNotice" class="kds-live-notice" role="status" aria-live="assertive" hidden></div>

<div id="issueModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="issueTitle">
        <h3 id="issueTitle">Báo sự cố</h3><p class="muted kds-modal__name" data-modal-name></p>
        <p class="kds-modal__hint">Sự cố sẽ được báo cho Thu ngân/Quản lý; món không tự động bị hủy.</p>
        <form action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="reportIssue"><input type="hidden" name="orderItemId" data-item-input>
            <label class="kds-field"><span>Lý do</span><select name="reason" required>
                <option value="">Chọn lý do</option><option value="OUT_OF_STOCK">Hết nguyên liệu</option><option value="EQUIPMENT">Máy móc gặp sự cố</option><option value="NOTE_UNSUPPORTED">Không đáp ứng được ghi chú</option><option value="DISCONTINUED">Món đã ngừng bán</option><option value="UNCLEAR_ORDER">Thông tin đơn không rõ</option><option value="OTHER">Lý do khác</option>
            </select></label>
            <label class="kds-field js-other-reason" hidden><span>Lý do khác</span><input type="text" name="otherReason" maxlength="255" autocomplete="off"></label>
            <div class="kds-field js-ingredients" hidden><span>Nguyên liệu đã hết</span><div data-ingredient-slot></div></div>
            <p class="kds-modal__hint js-blocking-note" hidden>Món sẽ chuyển sang mục <strong>Cần xử lý</strong> và rời khỏi hàng chờ pha.</p>
            <div class="kds-modal__actions"><button type="button" class="btn btn-ghost" data-close>Đóng</button><button type="submit" class="btn btn-danger">Gửi báo sự cố</button></div>
        </form>
    </div>
</div>

<div id="remakeModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="remakeTitle">
        <h3 id="remakeTitle">Làm lại món</h3><p class="muted kds-modal__name" data-modal-name></p>
        <p class="kds-modal__hint">Hệ thống ghi nhận hao hụt, giữ lịch sử lượt pha cũ và đưa món về Chờ pha với ưu tiên cao.</p>
        <form action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="remake"><input type="hidden" name="orderItemId" data-item-input>
            <label class="kds-field"><span>Lý do</span><select name="reason" required>
                <option value="">Chọn lý do</option><option value="WRONG_RECIPE">Pha sai công thức</option><option value="SPILLED">Làm đổ hoặc hư món</option><option value="QUALITY">Chất lượng không đạt</option><option value="CUSTOMER_FEEDBACK">Khách phản hồi</option><option value="WRONG_DELIVERY">Giao nhầm</option><option value="CHANGED_REQUEST">Khách thay đổi yêu cầu</option>
            </select></label>
            <div class="kds-modal__actions"><button type="button" class="btn btn-ghost" data-close>Đóng</button><button type="submit" class="btn btn-danger">Xác nhận làm lại</button></div>
        </form>
    </div>
</div>

<script>
(function(){
  var ctx='${ctx}', board=document.getElementById('kdsBoard'), connection=document.getElementById('kdsConnection');
  var currentFilter='default', refreshing=false, suppressUntil=0, known=readIds(), tiers=readTiers(),signatures=readSignatures(),noticeTimer;
  function readIds(){ var ids={}; if(board) board.querySelectorAll('[data-kds-item-id]').forEach(function(el){ids[el.dataset.kdsItemId]=true;}); return ids; }
  function readTiers(){var out={};board.querySelectorAll('[data-kds-item-id]').forEach(function(el){out[el.dataset.kdsItemId]=el.dataset.slaTier;});return out;}
  function signature(el){var copy=el.cloneNode(true);copy.querySelectorAll('.kds-sla,.kds-clock,.kds-meta-row,.kds-ready-facts').forEach(function(x){x.remove()});return copy.textContent.replace(/\s+/g,' ').trim();}
  function readSignatures(){var out={};board.querySelectorAll('[data-kds-item-id]').forEach(function(el){out[el.dataset.kdsItemId]=signature(el);});return out;}
  function notice(text){var el=document.getElementById('kdsLiveNotice');el.textContent=text;el.hidden=false;clearTimeout(noticeTimer);noticeTimer=setTimeout(function(){el.hidden=true},5000);}
  function markNew(){ var added=0,urgent=0,priority=0,changed=0,removed=0,next=readIds();board.querySelectorAll('[data-kds-item-id]').forEach(function(el){var id=el.dataset.kdsItemId;if(!known[id]){el.classList.add('kds-new');added++;if(el.dataset.priority==='true')priority++;}else if(signatures[id]&&signatures[id]!==signature(el))changed++;if(tiers[id]&&tiers[id]!==el.dataset.slaTier&&(el.dataset.slaTier==='warn'||el.dataset.slaTier==='crit'||el.dataset.slaTier==='severe'))urgent++;});Object.keys(known).forEach(function(id){if(!next[id])removed++;});if(priority)notice('Có '+priority+' món làm lại ưu tiên.');else if(urgent)notice('Có '+urgent+' dòng món gần hoặc quá SLA.');else if(added)notice('Có '+added+' dòng món mới.');else if(removed)notice('Một món đã được nhân viên nhận hoặc đơn đã thay đổi.');else if(changed)notice('Ghi chú hoặc thông tin món vừa được cập nhật.');known=next;tiers=readTiers();signatures=readSignatures(); }
  function setConnection(ok){ connection.classList.toggle('is-offline',!ok); connection.querySelector('span:last-child').textContent=ok?'Đang kết nối':'Mất kết nối — dữ liệu có thể chưa cập nhật'; }
  function applyFilter(){
    var uid=board.dataset.userId;
    board.querySelectorAll('[data-kds-item-id]').forEach(function(card){
      var owner=card.dataset.owner, show=true;
      // Món "Cần xử lý" nằm ngoài hàng chờ nên không chịu bộ lọc — nếu lọc, tiêu đề khu sẽ
      // hiện kèm số đếm mà bên dưới trống trơn.
      if(owner==='blocked'){card.hidden=false;return;}
      if(currentFilter==='default') show=owner==='unassigned'||owner===uid||owner==='ready';
      else if(currentFilter==='mine') show=owner===uid;
      else if(currentFilter==='unassigned') show=owner==='unassigned';
      else if(currentFilter.indexOf('station:')===0) show=card.dataset.station===currentFilter.split(':')[1];
      else if(currentFilter.indexOf('type:')===0) show=card.dataset.orderType===currentFilter.split(':')[1];
      card.hidden=!show;
    });
  }
  document.getElementById('kdsFilters').addEventListener('click',function(e){var b=e.target.closest('[data-filter]');if(!b)return;currentFilter=b.dataset.filter;this.querySelectorAll('[data-filter]').forEach(function(x){x.classList.toggle('is-active',x===b);});applyFilter();});
  (function(){var root=document.getElementById('kdsDensity'),btns=root.querySelectorAll('[data-dens]');function apply(){var compact=localStorage.getItem('kdsCompact')==='1';board.classList.toggle('is-compact',compact);btns.forEach(function(b){b.classList.toggle('is-active',b.dataset.dens===(compact?'1':'0'));});}btns.forEach(function(b){b.addEventListener('click',function(){localStorage.setItem('kdsCompact',b.dataset.dens);apply();});});apply();})();
  function modal(id,trigger){var m=document.getElementById(id);m.querySelector('[data-item-input]').value=trigger.dataset.itemId;m.querySelector('[data-modal-name]').textContent=trigger.dataset.name;m.dataset.productId=trigger.dataset.productId||'';m.hidden=false;m.querySelector('select').focus();}
  function close(m){m.hidden=true;m.querySelector('form').reset();var other=m.querySelector('.js-other-reason');if(other)other.hidden=true;
    var ing=m.querySelector('.js-ingredients'),note=m.querySelector('.js-blocking-note');
    if(ing){ing.hidden=true;ing.querySelector('[data-ingredient-slot]').innerHTML='';}
    if(note)note.hidden=true;}
  document.addEventListener('click',function(e){var i=e.target.closest('.js-issue'),r=e.target.closest('.js-remake'),x=e.target.closest('[data-close]');if(i)modal('issueModal',i);if(r)modal('remakeModal',r);if(x)close(x.closest('.kds-modal'));});
  // Lý do quyết định form hiện gì: hết nguyên liệu cần tick nguyên liệu (để ghi sổ kho),
  // các lý do chặn món chỉ cần báo trước rằng món sẽ rời hàng chờ.
  var BLOCKING=['EQUIPMENT','DISCONTINUED'];
  document.querySelector('#issueModal select').addEventListener('change',async function(){
    var m=document.getElementById('issueModal'),v=this.value;
    var other=m.querySelector('.js-other-reason');other.hidden=v!=='OTHER';other.querySelector('input').required=v==='OTHER';
    var ing=m.querySelector('.js-ingredients'),slot=ing.querySelector('[data-ingredient-slot]');
    var note=m.querySelector('.js-blocking-note');
    note.hidden=BLOCKING.indexOf(v)<0;
    if(v!=='OUT_OF_STOCK'){ing.hidden=true;slot.innerHTML='';return;}
    ing.hidden=false;slot.textContent='Đang tải nguyên liệu…';
    try{var r=await fetch(ctx+'/barista/kds?partial=recipe&productId='+encodeURIComponent(m.dataset.productId),{credentials:'same-origin'});
      if(!r.ok)throw new Error();slot.innerHTML=await r.text();}
    catch(e){slot.textContent='Không tải được danh sách nguyên liệu. Vui lòng thử lại.';}
  });
  document.addEventListener('keydown',function(e){if(e.key==='Escape')document.querySelectorAll('.kds-modal:not([hidden])').forEach(close);});
  async function postForm(form){
    if(form.dataset.busy==='1')return; form.dataset.busy='1'; var buttons=form.querySelectorAll('button');buttons.forEach(function(b){b.disabled=true;b.classList.add('is-loading');});
    suppressUntil=Date.now()+1800;var body=new FormData(form);body.append('ajax','1');
    try{var res=await fetch(form.action,{method:'POST',body:body,credentials:'same-origin',headers:{'X-Requested-With':'XMLHttpRequest'}});if(!res.ok)throw new Error();board.innerHTML=await res.text();markNew();applyFilter();setConnection(true);var m=form.closest('.kds-modal');if(m)close(m);}catch(err){setConnection(false);form.submit();}
  }
  document.addEventListener('submit',function(e){var form=e.target;if(!form.matches('#kdsBoard form,.kds-modal form'))return;e.preventDefault();var msg=form.dataset.confirm;if(msg&&!confirm(msg))return;postForm(form);});
  async function refresh(){if(refreshing||document.visibilityState==='hidden'||Date.now()<suppressUntil)return;refreshing=true;try{var res=await fetch(ctx+'/barista/kds?partial=1',{credentials:'same-origin'});if(!res.ok)throw new Error();board.innerHTML=await res.text();markNew();applyFilter();setConnection(true);}catch(e){setConnection(false);}finally{refreshing=false;}}
  setInterval(refresh,5000);window.addEventListener('online',refresh);window.addEventListener('offline',function(){setConnection(false);});applyFilter();
})();
</script>
<jsp:include page="../layout/footer.jsp" />
