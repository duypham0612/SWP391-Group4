<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<title>Theo dõi đơn · ${session.tableNumber}</title>
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
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
  .qr-actions{display:flex;gap:10px;margin-top:8px}
  .qr-actions form{flex:1}
  .qr-actions button{width:100%}
</style>
</head>
<body>
<div class="qr-app">
    <div class="qr-top">
        <h1>Đơn của bạn</h1>
        <div class="sub">${session.tableNumber} · cập nhật tự động</div>
    </div>
    <div class="qr-body">
        <c:if test="${not empty sessionScope.qrFlash}">
            <div class="alert alert-success">${sessionScope.qrFlash}</div>
            <c:remove var="qrFlash" scope="session" />
        </c:if>

        <div class="qr-card">
            <div id="trackList">
                <c:choose>
                    <c:when test="${empty items}"><p class="muted">Chưa có món nào.</p></c:when>
                    <c:otherwise>
                        <c:forEach var="it" items="${items}">
                            <div class="qr-item">
                                <span>${it.quantity}× ${it.productName}</span>
                                <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${it.status}" /></jsp:include>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <c:if test="${not empty cancellableOrders}">
            <div class="qr-card">
                <p class="muted" style="margin:0 0 8px">Đơn chưa pha — có thể huỷ:</p>
                <c:forEach var="o" items="${cancellableOrders}">
                    <form action="${ctx}/qr/track" method="post" style="margin-bottom:8px"
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

        <div class="qr-actions">
            <form action="${ctx}/qr/track" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="callStaff">
                <input type="hidden" name="sessionId" value="${sessionId}">
                <button type="submit" class="btn btn-ghost">Gọi nhân viên</button>
            </form>
            <form action="${ctx}/qr/track" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="requestBill">
                <input type="hidden" name="sessionId" value="${sessionId}">
                <button type="submit" class="btn btn-primary">Xin thanh toán</button>
            </form>
        </div>
        <div style="margin-top:12px;text-align:center">
            <a class="btn btn-ghost btn-sm" href="${ctx}/qr/track?s=${sessionId}">Làm mới</a>
        </div>
    </div>
</div>

<script>
const CTX='${ctx}', SID=${sessionId};
function badge(st){
  const m={WAITING:['badge-waiting','Chờ pha'],MAKING:['badge-making','Đang pha'],READY:['badge-ready','Đã pha xong'],PICKED_UP:['badge-ready','Nhân viên đã nhận'],SERVED:['badge-served','Đã phục vụ'],BLOCKED:['badge-cancelled','Bị chặn'],REMAKE:['badge-waiting','Làm lại'],CANCELLED:['badge-cancelled','Đã huỷ']};
  const x=m[st]||['badge-served',st];return '<span class="badge '+x[0]+'">'+x[1]+'</span>';
}
function poll(){
  fetch(CTX+'/qr/track?action=status&s='+SID).then(r=>r.json()).then(items=>{
    const box=document.getElementById('trackList');
    if(!items.length){box.innerHTML='<p class="muted">Chưa có món nào.</p>';return;}
    box.innerHTML=items.map(it=>'<div class="qr-item"><span>'+it.qty+'× '+it.name+'</span>'+badge(it.status)+'</div>').join('');
  }).catch(()=>{});
}
setInterval(poll,5000);
</script>
</body>
</html>
