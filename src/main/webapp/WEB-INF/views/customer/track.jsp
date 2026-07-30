<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<title>Theo dõi đơn · ${session.tableNumber}</title>
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css?v=${applicationScope.assetVersion}">
<style>
  body{background:var(--paper);margin:0}
  .qr-app{max-width:540px;margin:0 auto;padding:0 0 24px}
  .qr-top{background:linear-gradient(135deg,var(--wine-900),var(--brand-700));color:#fff;
          padding:22px 20px;position:sticky;top:0;z-index:5;box-shadow:var(--shadow)}
  .qr-top h1{margin:0;font-family:'Playfair Display',serif;font-size:1.35rem;color:#fff}
  .qr-top .sub{opacity:.82;font-size:.85rem;margin-top:2px}
  .qr-body{padding:16px}
  .qr-card{background:var(--surface);border:1px solid var(--line);border-radius:var(--radius);box-shadow:var(--shadow-sm);padding:16px;margin-bottom:12px}
  .qr-item{display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px dashed var(--line)}
  .qr-item:last-child{border-bottom:none}
</style>
</head>
<body>
<div class="qr-app">
    <div class="qr-top">
        <h1>${sessionClosed ? 'Cảm ơn quý khách' : 'Đơn của bạn'}</h1>
        <div class="sub">${session.tableNumber} · ${sessionClosed ? 'đã thanh toán xong' : 'trạng thái tự cập nhật mỗi 10 giây'}</div>
    </div>
    <div class="qr-body">
        <c:if test="${not empty sessionScope.qrFlash}">
            <div class="alert alert-success">${sessionScope.qrFlash}</div>
            <c:remove var="qrFlash" scope="session" />
        </c:if>

        <c:if test="${sessionClosed}">
            <div class="qr-card" style="text-align:center;padding:28px 18px">
                <div style="font-size:2.4rem">☕</div>
                <h2>Cảm ơn quý khách — đã thanh toán xong</h2>
                <p class="muted">Hẹn gặp lại quý khách lần sau.</p>
            </div>
        </c:if>

        <div class="qr-card">
            <div id="trackList">
                <c:choose>
                    <c:when test="${empty items}"><p class="muted">Chưa có món nào.</p></c:when>
                    <c:otherwise>
                        <%-- Nhãn hướng tới KHÁCH (thân thiện, không jargon). --%>
                        <c:forEach var="it" items="${items}">
                            <div class="qr-item" data-status="${it.status}">
                                <span>
                                    ${it.quantity}× ${it.productName}
                                    <c:if test="${it.hasIssue and not empty it.issueReason}">
                                        <br><small style="color:var(--st-cancelled)">Nhân viên sẽ hỗ trợ: <c:out value="${it.issueReason}" /></small>
                                    </c:if>
                                </span>
                                <c:choose>
                                    <c:when test="${it.status == 'WAITING'}"><span class="badge badge-waiting">Chờ pha</span></c:when>
                                    <c:when test="${it.status == 'MAKING'}"><span class="badge badge-making">Đang pha</span></c:when>
                                    <c:when test="${it.status == 'READY'}"><span class="badge badge-ready">Đã pha xong</span></c:when>
                                    <c:when test="${it.status == 'PICKED_UP'}"><span class="badge badge-ready">Nhân viên đang mang ra</span></c:when>
                                    <c:when test="${it.status == 'SERVED'}"><span class="badge badge-served">Đã phục vụ</span></c:when>
                                    <c:when test="${it.status == 'BLOCKED'}"><span class="badge badge-waiting">Tạm chưa làm được</span></c:when>
                                    <c:when test="${it.status == 'REMAKE'}"><span class="badge badge-waiting">Đang làm lại</span></c:when>
                                    <c:when test="${it.status == 'CANCELLED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
                                    <c:otherwise><span class="badge badge-served">${it.status}</span></c:otherwise>
                                </c:choose>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <c:if test="${not sessionClosed and not empty cancellableOrders}">
            <div class="qr-card">
                <p class="muted" style="margin:0 0 8px">Đơn chưa pha — có thể huỷ:</p>
                <c:forEach var="o" items="${cancellableOrders}">
                    <form class="cancel-order-form" action="${ctx}/qr/track" method="post" style="margin-bottom:8px"
                          onsubmit="return confirm('Huỷ đơn #${o.orderId}?');">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="cancel">
                        <input type="hidden" name="sessionId" value="${sessionId}">
                        <input type="hidden" name="orderId" value="${o.orderId}">
                        <button type="submit" class="btn btn-ghost" style="width:100%;color:var(--st-cancelled)">Huỷ đơn #${o.orderId}</button>
                    </form>
                </c:forEach>
            </div>
        </c:if>

        <c:if test="${not sessionClosed}">
            <div style="margin-top:12px;text-align:center;display:flex;gap:8px;justify-content:center;flex-wrap:wrap">
                <a class="btn btn-primary btn-sm" href="${ctx}/qr/menu">Gọi thêm món</a>
                <a class="btn btn-ghost btn-sm" href="${ctx}/qr/track?s=${sessionId}">Làm mới</a>
            </div>
        </c:if>
    </div>
</div>

<c:if test="${sessionClosed}">
<script>
window.addEventListener('DOMContentLoaded', function () {
  window.alert('Thanh toán thành công!');
  window.location.replace('${ctx}/home');
});
</script>
</c:if>

<c:if test="${not sessionClosed}">
<script>
const STATUS_LABELS={
  WAITING:['Chờ pha','badge-waiting'],
  MAKING:['Đang pha','badge-making'],
  READY:['Đã pha xong','badge-ready'],
  PICKED_UP:['Nhân viên đang mang ra','badge-ready'],
  SERVED:['Đã phục vụ','badge-served'],
  BLOCKED:['Tạm chưa làm được','badge-waiting'],
  REMAKE:['Đang làm lại','badge-waiting'],
  CANCELLED:['Đã huỷ','badge-cancelled']
};
function renderStatuses(items){
  const list=document.getElementById('trackList');
  list.innerHTML='';
  if(!items.length){
    const empty=document.createElement('p');
    empty.className='muted';
    empty.textContent='Chưa có món nào.';
    list.appendChild(empty);
    return;
  }
  items.forEach(item=>{
    const row=document.createElement('div');
    row.className='qr-item';
    const name=document.createElement('span');
    name.textContent=item.qty+'× '+item.name;
    if(item.issueReason){
      const issue=document.createElement('small');
      issue.style.color='var(--st-cancelled)';
      issue.textContent='Nhân viên sẽ hỗ trợ: '+item.issueReason;
      name.append(document.createElement('br'),issue);
    }
    const badge=document.createElement('span');
    const meta=STATUS_LABELS[item.status]||['Đang cập nhật','badge-served'];
    badge.className='badge '+meta[1];
    badge.textContent=meta[0];
    row.append(name,badge);
    list.appendChild(row);
  });
}
let lastStatuses=Array.from(document.querySelectorAll('#trackList .qr-item'))
  .map(row=>row.dataset.status||'');
async function pollStatuses(){
  try{
    const response=await fetch('${ctx}/qr/track?action=status&s=${sessionId}',{
      headers:{'Accept':'application/json'},
      cache:'no-store'
    });
    if(!response.ok)return;
    if(response.headers.get('X-Session-Closed')==='true'){
      window.clearInterval(statusTimer);
      window.alert('Thanh toán thành công!');
      window.location.replace('${ctx}/home');
      return;
    }
    const items=await response.json();
    const nextStatuses=items.map(item=>item.status||'');
    const cancellationTransition=nextStatuses.some((status,index)=>
      lastStatuses[index]==='WAITING' && status!=='WAITING');
    lastStatuses=nextStatuses;
    if(cancellationTransition && document.querySelector('.cancel-order-form')){
      window.location.reload();
      return;
    }
    renderStatuses(items);
  }catch(error){
    // Giữ trạng thái gần nhất khi mạng chập chờn; lượt polling sau sẽ thử lại.
  }
}
const statusTimer=window.setInterval(pollStatuses,10000);
</script>
</c:if>
</body>
</html>
