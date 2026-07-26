<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>In mã QR bàn</title>
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css?v=${applicationScope.assetVersion}">
<style>
  body{background:var(--paper);margin:0}
  .qr-page{max-width:1080px;margin:0 auto;padding:28px 20px 48px}
  .qr-head{display:flex;justify-content:space-between;gap:20px;align-items:flex-start;margin-bottom:20px}
  .qr-head h1{margin:4px 0 6px;font-family:'Playfair Display',serif}
  .qr-tools{display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap}
  .base-field{min-width:min(520px,80vw)}
  .base-field label{display:block;font-weight:700;margin-bottom:6px}
  .base-warning{font-size:.86rem;color:var(--st-cancelled);margin:6px 0 0}
  .qr-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:18px}
  .qr-card{background:#fff;border:1px solid var(--line);border-radius:16px;padding:20px;text-align:center;break-inside:avoid}
  .qr-card h2{margin:0 0 14px}
  .qr-box{width:180px;height:180px;margin:0 auto 12px;display:flex;align-items:center;justify-content:center}
  .qr-code{font:12px/1.35 monospace;color:var(--muted);overflow-wrap:anywhere}
  .qr-missing{height:180px;display:flex;align-items:center;justify-content:center;color:var(--st-cancelled);font-weight:700}
  @media print{
    @page{margin:12mm}
    body{background:#fff}
    .no-print{display:none!important}
    .qr-page{max-width:none;padding:0}
    .qr-grid{grid-template-columns:repeat(2,1fr)}
    .qr-card{box-shadow:none;border-color:#aaa}
  }
</style>
</head>
<body>
<main class="qr-page">
  <div class="qr-head no-print">
    <div>
      <a href="${ctx}/cashier/table">← Về sơ đồ bàn</a>
      <h1>In mã QR bàn</h1>
      <p class="muted">Kiểm tra đúng mã bàn trước khi in và dán.</p>
    </div>
    <button type="button" class="btn btn-primary" onclick="window.print()">In</button>
  </div>

  <div class="card no-print" style="padding:16px;margin-bottom:20px">
    <div class="qr-tools">
      <div class="base-field">
        <label for="baseUrl">Địa chỉ khách truy cập</label>
        <input id="baseUrl" class="form-control" type="url"
               value="<c:out value="${baseUrl}"/>" oninput="renderQrCodes()">
        <p class="base-warning">Nếu đang chạy trên máy nội bộ, hãy thay localhost bằng IP LAN mà điện thoại truy cập được.</p>
      </div>
    </div>
  </div>

  <div class="qr-grid">
    <c:forEach var="t" items="${tables}">
      <article class="qr-card">
        <h2><c:out value="${t.tableNumber}"/></h2>
        <c:choose>
          <c:when test="${empty t.qrCode}">
            <div class="qr-missing">Bàn chưa có mã QR</div>
          </c:when>
          <c:otherwise>
            <div class="qr-box" data-qr-code="<c:out value="${t.qrCode}"/>"
                 data-default-url="<c:out value="${menuUrls[t.diningTableId]}"/>"></div>
            <div class="qr-code"><c:out value="${t.qrCode}"/></div>
          </c:otherwise>
        </c:choose>
      </article>
    </c:forEach>
  </div>

  <c:if test="${empty tables}">
    <div class="card empty-state"><p>Chi nhánh chưa có bàn nào.</p></div>
  </c:if>
</main>

<script src="${ctx}/assets/js/qrcode.min.js"></script>
<script>
const QR_SERVER_BASE='${baseUrl}';

// Nhớ base đã sửa để sơ đồ bàn (/cashier/table) hiện QR cùng địa chỉ — sửa một nơi, đúng cả hai.
function rememberBase(base){
  try{
    if(base && base!==QR_SERVER_BASE) window.localStorage.setItem('cafeQrBase',base);
    else window.localStorage.removeItem('cafeQrBase');
  }catch(e){ /* trình duyệt chặn localStorage — bỏ qua, trang này vẫn chạy */ }
}

function renderQrCodes(){
  const rawBase=document.getElementById('baseUrl').value.trim();
  const base=rawBase.replace(/\/+$/,'');
  rememberBase(base);
  document.querySelectorAll('.qr-box').forEach(box=>{
    box.innerHTML='';
    if(!base)return;
    if(!window.QRCode){
      box.textContent='Không tải được thư viện QR. Vui lòng kiểm tra kết nối và tải lại trang.';
      return;
    }
    const defaultBase='<c:out value="${baseUrl}"/>';
    const url=base===defaultBase.replace(/\/+$/,'') && box.dataset.defaultUrl
      ? box.dataset.defaultUrl
      : base+'/qr/menu?t='+encodeURIComponent(box.dataset.qrCode);
    new QRCode(box,{text:url,width:180,height:180,correctLevel:QRCode.CorrectLevel.M});
  });
}
// Khôi phục địa chỉ đã sửa lần trước (vd IP LAN lúc chạy local) để khỏi gõ lại mỗi lần.
(function(){
  try{
    const saved=window.localStorage.getItem('cafeQrBase');
    if(saved) document.getElementById('baseUrl').value=saved;
  }catch(e){ /* localStorage bị chặn — dùng base mặc định của server */ }
})();
renderQrCodes();
</script>
</body>
</html>
