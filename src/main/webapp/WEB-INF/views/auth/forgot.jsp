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
            <h1>Quên mật khẩu? Đặt lại ngay.</h1>
            <p>Nhập tên đăng nhập và email đã đăng ký để xác minh và đặt lại mật khẩu mới.</p>
        </div>
        <div class="lb-foot">© 2026 Cà Phê Chain · SWP391</div>
    </div>

    <div class="login-formwrap">
        <div class="login-card">
            <div class="form-head">
                <h2>Đặt lại mật khẩu</h2>
                <p>Xác minh bằng tên đăng nhập và email tài khoản.</p>
            </div>

            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">${errorMsg}</div>
            </c:if>

            <form action="${ctx}/auth/forgot" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>
                    <input id="username" type="text" name="username" class="form-control"
                           value="${username}" required autofocus placeholder="admin">
                </div>
                <div class="form-group">
                    <label for="email">Email đăng ký</label>
                    <input id="email" type="email" name="email" class="form-control"
                           value="${email}" required placeholder="ban@cafechain.vn">
                </div>
                <div class="form-group">
                    <label for="newPassword">Mật khẩu mới (≥ 6 ký tự)</label>
                    <input id="newPassword" type="password" name="newPassword" class="form-control"
                           required minlength="6" placeholder="••••••••">
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Xác nhận mật khẩu mới</label>
                    <input id="confirmPassword" type="password" name="confirmPassword" class="form-control"
                           required minlength="6" placeholder="••••••••">
                </div>
                <button type="submit" class="btn btn-primary btn-full btn-lg">Đặt lại mật khẩu</button>
            </form>

            <div class="login-hint">
                <a href="${ctx}/auth/login">← Quay lại đăng nhập</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
