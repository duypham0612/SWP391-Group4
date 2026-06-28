<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập · Cà Phê Chain</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
</head>
<body>
<div class="login-wrap">
    <div class="login-brandpanel">
        <div class="lb-top">
            <span class="logo">C</span>
            <span class="name">Cà Phê Chain</span>
        </div>
        <div class="lb-mid">
            <div class="eyebrow">Hệ thống quản lý chuỗi</div>
            <h1>Vận hành quán cà phê, trọn vẹn trong một nơi.</h1>
            <p>Quản lý thực đơn, công thức, kho, ca làm và đơn hàng tại quầy — mượt mà cho từng chi nhánh, từng vai trò.</p>
        </div>
        <div class="lb-foot">© 2026 Cà Phê Chain · SWP391</div>
    </div>

    <div class="login-formwrap">
        <div class="login-card">
            <div class="form-head">
                <h2>Đăng nhập</h2>
                <p>Chào mừng trở lại. Vui lòng nhập thông tin của bạn.</p>
            </div>

            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">${errorMsg}</div>
            </c:if>

            <form action="${ctx}/auth/login" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>
                    <input id="username" type="text" name="username" class="form-control"
                           value="${username}" required autofocus placeholder="admin">
                </div>
                <div class="form-group">
                    <label for="password">Mật khẩu</label>
                    <input id="password" type="password" name="password" class="form-control"
                           required placeholder="••••••••">
                </div>
                <button type="submit" class="btn btn-primary btn-full btn-lg">Đăng nhập</button>
            </form>

            <div class="login-hint">
                Tài khoản dùng thử: <strong>admin · manager1 · cashier1 · barista1</strong><br>
                Mật khẩu mặc định: <strong>123456</strong>
            </div>
        </div>
    </div>
</div>
</body>
</html>
