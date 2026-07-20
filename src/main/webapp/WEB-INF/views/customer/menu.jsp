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
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css?v=${applicationScope.assetVersion}">
<style>
  body{background:var(--paper);margin:0}
  .qr-app{max-width:540px;margin:0 auto;padding:0 0 132px}
  .qr-top{background:linear-gradient(135deg,var(--wine-900),var(--brand-700));color:#fff;
          padding:22px 20px 18px;position:sticky;top:0;z-index:5;box-shadow:var(--shadow)}
  .qr-top h1{margin:0;font-family:'Playfair Display',serif;font-size:1.4rem;color:#fff}
  .qr-top .sub{opacity:.82;font-size:.85rem;margin-top:2px}
  .qr-body{padding:16px}
  .qr-card{background:var(--surface);border:1px solid var(--line);border-radius:var(--radius);box-shadow:var(--shadow-sm);padding:14px;margin-bottom:12px}
  .qr-card .name{font-weight:700;font-size:1.05rem}
  .qr-card .price{color:var(--brand);font-weight:700}
  .qr-grp{margin-top:10px}
  .qr-grp .lbl{font-size:.7rem;text-transform:uppercase;letter-spacing:.08em;color:var(--muted);font-weight:700;margin-bottom:5px}
  .seg-options{display:flex;gap:6px;flex-wrap:wrap}
  .seg-options input{position:absolute;opacity:0;pointer-events:none}
  .seg-options span{display:inline-flex;align-items:center;justify-content:center;min-height:34px;padding:0 11px;border:1px solid var(--line);border-radius:999px;background:var(--surface);font-weight:700;font-size:.9rem}
  .seg-options input:checked + span{background:var(--coffee);border-color:var(--coffee);color:#fff}
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
            <div class="qr-card pos-product"
                 data-product-id="${m.productId}" data-product-name="${m.name}" data-price="${m.price}"
                 data-size-enabled="${m.sizeEnabled}" data-size-s="${m.sizeSDelta}" data-size-m="${m.sizeMDelta}" data-size-l="${m.sizeLDelta}">
                <div style="display:flex;gap:12px;align-items:center">
                    <img class="prod-thumb lg" src="${imgSrc}" alt="${m.name}" loading="lazy"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                    <div style="flex:1;display:flex;justify-content:space-between;align-items:baseline;gap:10px">
                        <span class="name">${m.name}</span>
                        <span class="price"><fmt:formatNumber value="${m.price}" maxFractionDigits="0"/> ₫</span>
                    </div>
                </div>

                <c:if test="${m.sizeEnabled}">
                    <div class="qr-grp">
                        <div class="lbl">Size</div>
                        <div class="seg-options">
                            <label><input type="radio" class="pos-size" name="size-${m.productId}" value="S"><span>S<c:if test="${m.sizeSDelta > 0}"> +<fmt:formatNumber value="${m.sizeSDelta}" maxFractionDigits="0"/></c:if></span></label>
                            <label><input type="radio" class="pos-size" name="size-${m.productId}" value="M" checked><span>M<c:if test="${m.sizeMDelta > 0}"> +<fmt:formatNumber value="${m.sizeMDelta}" maxFractionDigits="0"/></c:if></span></label>
                            <label><input type="radio" class="pos-size" name="size-${m.productId}" value="L"><span>L<c:if test="${m.sizeLDelta > 0}"> +<fmt:formatNumber value="${m.sizeLDelta}" maxFractionDigits="0"/></c:if></span></label>
                        </div>
                    </div>
                </c:if>
                <div class="qr-grp">
                    <div class="lbl">Đá</div>
                    <select class="form-control pos-ice">
                        <option>Không đá</option><option>Ít đá</option><option selected>Bình thường</option><option>Nhiều đá</option>
                    </select>
                </div>
                <div class="qr-grp">
                    <div class="lbl">Đường</div>
                    <select class="form-control pos-sugar">
                        <option>0%</option><option>30%</option><option>50%</option><option>70%</option><option selected>100%</option>
                    </select>
                </div>

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
function money(v){const n=parseFloat(v);return Number.isFinite(n)?n:0;}
function esc(v){return String(v||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function selectedSize(card){if(card.dataset.sizeEnabled!=='true')return 'M';const picked=card.querySelector('.pos-size:checked');return picked?picked.value:'M';}
function sizeDelta(card,size){if(card.dataset.sizeEnabled!=='true')return 0;if(size==='S')return money(card.dataset.sizeS);if(size==='L')return money(card.dataset.sizeL);return money(card.dataset.sizeM);}
function resetProduct(card){
  const m=card.querySelector('.pos-size[value="M"]'); if(m)m.checked=true;
  card.querySelector('.pos-ice').value='Bình thường';
  card.querySelector('.pos-sugar').value='100%';
  card.querySelector('.pos-qty').value=1;
}
function optionText(l){return 'Size '+(l.size||'M')+', '+(l.iceLevel||'Bình thường')+', đường '+(l.sugarLevel||'100%');}
function addToCart(btn){
  const c=btn.closest('.pos-product');
  const size=selectedSize(c), iceLevel=c.querySelector('.pos-ice').value, sugarLevel=c.querySelector('.pos-sugar').value;
  const qty=Math.max(1,parseInt(c.querySelector('.pos-qty').value)||1);
  cart.push({productId:parseInt(c.dataset.productId),name:c.dataset.productName,quantity:qty,unit:money(c.dataset.price)+sizeDelta(c,size),size,iceLevel,sugarLevel});
  resetProduct(c);
  render();
}
function rm(i){cart.splice(i,1);render();}
function render(){
  const box=document.getElementById('cartList');
  box.innerHTML=cart.map((l,i)=>'<div class="qr-line"><div>'+esc(l.quantity)+'× '+esc(l.name)+'<br><span class="muted" style="font-size:.8rem">'+esc(optionText(l))+'</span></div><div style="white-space:nowrap">'+fmt(money(l.unit)*l.quantity)+' <a href="javascript:void(0)" onclick="rm('+i+')">×</a></div></div>').join('');
  document.getElementById('cartCount').textContent=cart.reduce((s,l)=>s+l.quantity,0);
  document.getElementById('cartTotal').textContent=fmt(cart.reduce((s,l)=>s+money(l.unit)*l.quantity,0));
  document.getElementById('placeBtn').disabled=cart.length===0;
}
function placeOrder(){
  const msg=document.getElementById('qrMsg');msg.textContent='Đang gửi...';
  fetch(CTX+'/qr/menu?_csrf='+encodeURIComponent(CSRF),{method:'POST',headers:{'Content-Type':'application/json','Accept':'application/json'},
    body:JSON.stringify({items:cart.map(l=>({productId:l.productId,quantity:l.quantity,size:l.size,iceLevel:l.iceLevel,sugarLevel:l.sugarLevel}))})})
   .then(r=>r.json().then(j=>({ok:r.ok,j}))).then(({ok,j})=>{
     if(ok){location.href=CTX+'/qr/track?s='+j.sessionId;}
     else{msg.innerHTML='<span style="color:var(--st-cancelled)">Lỗi: '+esc(j.error||'')+'</span>';}
   }).catch(()=>{msg.innerHTML='<span style="color:var(--st-cancelled)">Lỗi mạng.</span>';});
}
render();
</script>
</body>
</html>
