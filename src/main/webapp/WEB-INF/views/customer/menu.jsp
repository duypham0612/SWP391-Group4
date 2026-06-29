<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<title>Đặt món · ${table.tableNumber}</title>
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
<style>
  body{background:var(--paper);margin:0}
  .qr-app{max-width:540px;margin:0 auto;padding:0 0 132px}
  .qr-top{background:linear-gradient(135deg,var(--wine-900),var(--brand-700));color:#fff;
          padding:22px 20px 18px;position:sticky;top:0;z-index:5;box-shadow:var(--shadow)}
  .qr-top h1{margin:0;font-family:'Playfair Display',serif;font-size:1.4rem;color:#fff}
  .qr-top .sub{opacity:.82;font-size:.85rem;margin-top:2px}
  .qr-body{padding:16px}
  .qr-card{background:var(--surface);border:1px solid var(--line);border-radius:var(--radius);box-shadow:var(--shadow-sm);padding:14px;margin-bottom:12px;transition:box-shadow var(--t)}
  .qr-card:active{box-shadow:var(--shadow-md)}
  .qr-card .name{font-weight:700;font-size:1.05rem}
  .qr-card .price{color:var(--brand);font-weight:700}
  .qr-grp{margin-top:10px}
  .qr-grp .lbl{font-size:.7rem;text-transform:uppercase;letter-spacing:.08em;color:var(--muted);font-weight:700}
  .qr-grp label{display:flex;gap:8px;align-items:center;padding:5px 0;font-size:.95rem}
  .qr-add{display:flex;gap:10px;align-items:center;margin-top:12px}
  .qr-add input{width:64px}
  .qr-bar{position:fixed;left:0;right:0;bottom:0;background:color-mix(in srgb,var(--surface) 92%,transparent);
          backdrop-filter:blur(10px);border-top:1px solid var(--line);
          padding:14px 16px;max-width:540px;margin:0 auto;box-shadow:0 -4px 18px rgba(42,14,18,.12)}
  .qr-bar .row{display:flex;justify-content:space-between;align-items:center;gap:12px}
  #cartList{max-height:34vh;overflow:auto;margin-bottom:8px}
  .qr-line{display:flex;justify-content:space-between;gap:8px;padding:7px 0;border-bottom:1px dashed var(--line);font-size:.9rem}
</style>
</head>
<body>
<div class="qr-app">
    <div class="qr-top">
        <h1>Cà Phê Chain</h1>
        <div class="sub">${table.tableNumber} · Quét QR đặt món tại bàn</div>
    </div>
    <div class="qr-body">
        <c:if test="${empty menu}"><div class="qr-card">Hiện chưa có món nào được phục vụ.</div></c:if>
        <c:forEach var="m" items="${menu}">
            <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
            <div class="qr-card pos-product" data-product-id="${m.productId}" data-product-name="${m.name}" data-price="${m.price}">
                <div style="display:flex;gap:12px;align-items:center">
                    <img class="prod-thumb lg" src="${imgSrc}" alt="${m.name}" loading="lazy"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                    <div style="flex:1;display:flex;justify-content:space-between;align-items:baseline">
                        <span class="name">${m.name}</span>
                        <span class="price"><fmt:formatNumber value="${m.price}" maxFractionDigits="0"/> ₫</span>
                    </div>
                </div>
                <c:forEach var="g" items="${m.groups}">
                    <div class="qr-grp">
                        <div class="lbl">${g.name}</div>
                        <c:forEach var="o" items="${g.options}">
                            <label><input type="${g.maxSelect == 1 ? 'radio' : 'checkbox'}" name="g-${m.productId}-${g.groupId}"
                                          class="pos-opt" data-option-id="${o.modifierOptionId}" data-delta="${o.priceDelta}" data-name="${o.name}">
                                ${o.name}<c:if test="${o.priceDelta > 0}"> <span class="price">+<fmt:formatNumber value="${o.priceDelta}" maxFractionDigits="0"/>₫</span></c:if></label>
                        </c:forEach>
                    </div>
                </c:forEach>
                <div class="qr-add">
                    <input type="number" class="form-control pos-qty" value="1" min="1">
                    <button type="button" class="btn btn-primary" style="flex:1" onclick="addToCart(this)">Thêm vào giỏ</button>
                </div>
            </div>
        </c:forEach>
    </div>
</div>

<div class="qr-bar">
    <div id="cartList"></div>
    <div class="row">
        <div><strong id="cartCount">0</strong> món · <strong id="cartTotal">0 ₫</strong></div>
        <button id="placeBtn" class="btn btn-primary btn-lg" onclick="placeOrder()" disabled>Đặt món</button>
    </div>
    <div id="qrMsg" style="font-size:.85rem;margin-top:6px"></div>
</div>

<script>
const CSRF='${sessionScope.csrfToken}', CTX='${ctx}', SID=${sessionId};
let cart=[];
function fmt(n){return new Intl.NumberFormat('vi-VN').format(n)+' ₫';}
function addToCart(btn){
  const c=btn.closest('.pos-product');
  let delta=0;const optionIds=[],names=[];
  c.querySelectorAll('.pos-opt:checked').forEach(o=>{delta+=parseFloat(o.dataset.delta);optionIds.push(parseInt(o.dataset.optionId));names.push(o.dataset.name);});
  const qty=Math.max(1,parseInt(c.querySelector('.pos-qty').value)||1);
  cart.push({productId:parseInt(c.dataset.productId),name:c.dataset.productName,quantity:qty,unit:parseFloat(c.dataset.price)+delta,optionIds:optionIds,names:names});
  c.querySelectorAll('.pos-opt:checked').forEach(o=>o.checked=false);c.querySelector('.pos-qty').value=1;
  render();
}
function rm(i){cart.splice(i,1);render();}
function render(){
  const box=document.getElementById('cartList');
  box.innerHTML=cart.map((l,i)=>'<div class="qr-line"><div>'+l.quantity+'× '+l.name+(l.names.length?'<br><span class="muted" style="font-size:.8rem">'+l.names.join(', ')+'</span>':'')+'</div><div style="white-space:nowrap">'+fmt(l.unit*l.quantity)+' <a href="javascript:void(0)" onclick="rm('+i+')">×</a></div></div>').join('');
  document.getElementById('cartCount').textContent=cart.reduce((s,l)=>s+l.quantity,0);
  document.getElementById('cartTotal').textContent=fmt(cart.reduce((s,l)=>s+l.unit*l.quantity,0));
  document.getElementById('placeBtn').disabled=cart.length===0;
}
function placeOrder(){
  const msg=document.getElementById('qrMsg');msg.textContent='Đang gửi...';
  fetch(CTX+'/qr/menu?_csrf='+encodeURIComponent(CSRF),{method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({items:cart.map(l=>({productId:l.productId,quantity:l.quantity,optionIds:l.optionIds}))})})
   .then(r=>r.json().then(j=>({ok:r.ok,j}))).then(({ok,j})=>{
     if(ok){location.href=CTX+'/qr/track?s='+j.sessionId;}
     else{msg.innerHTML='<span style="color:var(--st-cancelled)">Lỗi: '+(j.error||'')+'</span>';}
   }).catch(()=>{msg.innerHTML='<span style="color:var(--st-cancelled)">Lỗi mạng.</span>';});
}
render();
</script>
</body>
</html>
