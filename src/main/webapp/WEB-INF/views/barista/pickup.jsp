<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />
<div class="page-header"><div><div class="eyebrow">Thu ngân · Bàn giao</div><h1>Sẵn sàng bàn giao</h1><p>Xác nhận nhận món khỏi quầy trước, sau đó xác nhận đã giao khách.</p></div><div style="display:flex;align-items:center;gap:10px"><button type="button" class="btn btn-ghost btn-sm" id="pickupRefresh" title="Tải lại danh sách">↻ Làm mới</button><div class="kds-connection"><span class="kds-refresh__dot"></span><span>Đang kết nối</span></div></div></div>
<div id="pickupBoard" class="kds-board"><jsp:include page="pickup_cards.jsp" /></div>
<script>
(function(){var ctx='${ctx}',b=document.getElementById('pickupBoard'),busy=false;
async function post(f){if(f.dataset.busy)return;f.dataset.busy='1';f.querySelectorAll('button').forEach(function(x){x.disabled=true;x.classList.add('is-loading')});var d=new FormData(f);d.append('ajax','1');var r=await fetch(f.action,{method:'POST',body:d,credentials:'same-origin'});if(!r.ok)throw new Error();b.innerHTML=await r.text();}
b.addEventListener('submit',function(e){var f=e.target.closest('form');if(!f)return;e.preventDefault();if(f.dataset.confirm&&!confirm(f.dataset.confirm))return;post(f).catch(function(){f.submit()});});
// Không tự động làm mới nữa — bảng cập nhật sau mỗi thao tác, hoặc khi bấm "Làm mới".
async function reload(){if(busy)return;busy=true;try{var r=await fetch(ctx+'/cashier/handoff?partial=1',{credentials:'same-origin'});if(r.ok)b.innerHTML=await r.text();}finally{busy=false}}
var rb=document.getElementById('pickupRefresh');if(rb)rb.addEventListener('click',reload);
})();
</script>
<jsp:include page="../layout/footer.jsp" />
