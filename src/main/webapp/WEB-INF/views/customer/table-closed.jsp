<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<title>Bàn chưa mở · ${table.tableNumber}</title>
<link rel="stylesheet" href="${ctx}/assets/css/core.css?v=${applicationScope.assetVersion}">
<link rel="stylesheet" href="${ctx}/assets/css/responsive.css?v=${applicationScope.assetVersion}">
<style>
  body{background:var(--paper);margin:0}
  .qr-app{max-width:540px;margin:0 auto;padding:0 0 24px}
  .qr-top{background:linear-gradient(135deg,var(--wine-900),var(--brand-700));color:#fff;
          padding:22px 20px;box-shadow:var(--shadow)}
  .qr-top h1{margin:0;font-family:'Playfair Display',serif;font-size:1.35rem;color:#fff}
  .qr-top .sub{opacity:.82;font-size:.85rem;margin-top:2px}
  .qr-body{padding:16px}
  .qr-card{background:var(--surface);border:1px solid var(--line);border-radius:var(--radius);
           box-shadow:var(--shadow-sm);padding:20px;text-align:center}
  .qr-card .icon{font-size:2.4rem}
  .steps{text-align:left;margin:16px 0 0;padding-left:20px;font-size:.92rem;color:var(--muted)}
  .steps li{margin:6px 0}
</style>
</head>
<body>
<div class="qr-app">
    <div class="qr-top">
        <h1>Cà Phê Chain</h1>
        <div class="sub">${table.tableNumber}</div>
    </div>
    <div class="qr-body">
        <c:if test="${not empty sessionScope.qrFlash}">
            <div class="alert alert-success">${sessionScope.qrFlash}</div>
            <c:remove var="qrFlash" scope="session" />
        </c:if>

        <div class="qr-card">
            <div class="icon">☕</div>
            <h2 style="font-family:'Playfair Display',serif;margin:.4rem 0">Bàn chưa được mở</h2>
            <p class="muted" style="margin:0">
                Nhân viên thu ngân cần mở ${table.tableNumber} trước khi bạn đặt món tại bàn.
            </p>
            <ol class="steps">
                <li>Bấm nút bên dưới để báo quầy, hoặc mời bạn ghé quầy.</li>
                <li>Thu ngân mở bàn cho bạn.</li>
                <li>Quét lại mã QR trên bàn để xem thực đơn và đặt món.</li>
            </ol>

            <form action="${ctx}/qr/menu" method="post" style="margin-top:18px">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="requestOpen">
                <input type="hidden" name="qrCode" value="${qrCode}">
                <input type="hidden" name="tableNumber" value="${table.tableNumber}">
                <button type="submit" class="btn btn-primary btn-lg" style="width:100%">Báo quầy mở bàn</button>
            </form>
            <div style="margin-top:10px">
                <a class="btn btn-ghost btn-sm" href="${ctx}/qr/menu?t=${qrCode}">Đã mở bàn? Tải lại</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
