<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quên mật khẩu · Cà Phê Chain</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css?v=${applicationScope.assetVersion}">
</head>
<body>
<div class="login-wrap">
    <div class="login-brandpanel">
        <div class="lb-top">
            <span class="logo">C</span>
            <span class="name">Cà Phê Chain</span>
        </div>
        <div class="lb-mid">
            <div class="eyebrow">Khôi phục truy cập</div>
            <h1>Quên mật khẩu?</h1>
            <p>Việc đặt lại mật khẩu cần được xác minh bởi quản trị viên hệ thống.</p>
        </div>
        <div class="lb-foot">© 2026 Cà Phê Chain · SWP391</div>
    </div>

    <div class="login-formwrap">
        <div class="login-card">
            <div class="form-head">
                <h2>Khôi phục tài khoản</h2>
                <p>Vì hệ thống chưa cấu hình kênh email/OTP an toàn, chức năng tự đặt lại mật khẩu đã được tắt.</p>
            </div>

            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">${errorMsg}</div>
            </c:if>

            <div class="alert alert-info">
                Vui lòng liên hệ quản trị viên để xác minh danh tính và cấp lại mật khẩu.
                Không gửi mật khẩu hoặc mã xác minh qua kênh công khai.
            </div>

            <div class="login-hint">
                <a href="${ctx}/auth/login">← Quay lại đăng nhập</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
