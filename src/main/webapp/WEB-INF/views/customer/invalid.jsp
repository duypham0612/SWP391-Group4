<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mã QR không hợp lệ</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/cafe-theme.css?v=${applicationScope.assetVersion}">
<style>body{background:var(--foam)} .wrap{max-width:480px;margin:40px auto;padding:0 18px;text-align:center}</style>
</head>
<body>
<div class="wrap">
    <div class="card" style="background:var(--cream);border-radius:var(--radius);box-shadow:var(--shadow);padding:28px">
        <div style="font-size:2.4rem">☕</div>
        <h1 style="font-family:'Playfair Display',serif">Mã QR không hợp lệ</h1>
        <p class="muted">${empty invalidReason ? 'Không tìm thấy bàn ứng với mã này.' : invalidReason}
            Vui lòng quét lại mã QR dán tại bàn hoặc nhờ nhân viên hỗ trợ.</p>
    </div>
</div>
</body>
</html>
