<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${empty invalidTitle ? 'Mã QR không hợp lệ' : invalidTitle}</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/core.css?v=${applicationScope.assetVersion}">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/responsive.css?v=${applicationScope.assetVersion}">
<style>body{background:var(--foam)} .wrap{max-width:480px;margin:40px auto;padding:0 18px;text-align:center}</style>
</head>
<body>
<div class="wrap">
    <div class="card" style="background:var(--cream);border-radius:var(--radius);box-shadow:var(--shadow);padding:28px">
        <div style="font-size:2.4rem">☕</div>
        <h1 style="font-family:'Playfair Display',serif">${empty invalidTitle ? 'Mã QR không hợp lệ' : invalidTitle}</h1>
        <p class="muted">${empty invalidMessage
                ? 'Không tìm thấy bàn ứng với mã này. Vui lòng quét lại mã QR dán tại bàn hoặc nhờ nhân viên hỗ trợ.'
                : invalidMessage}</p>
    </div>
</div>
</body>
</html>
