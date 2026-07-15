<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#291A0F">
    <title>Đăng nhập · Cà Phê Chain</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
</head>
<body>
<div class="login-wrap">
    <div class="login-brandpanel">
        <a href="${ctx}/home" style="text-decoration:none;color:inherit">
            <div class="lb-top">
                <span class="logo">C</span>
                <span class="name">Cà Phê Chain</span>
            </div>
        </a>

        <div class="lb-center">
            <img class="lb-hero" src="${ctx}/assets/img/login-hero.svg" alt="Tách cà phê" width="560" height="480">
            <div class="lb-mid">
                <div class="eyebrow">Hệ thống quản lý chuỗi</div>
                <h1>Vận hành quán cà phê, trọn vẹn trong một nơi.</h1>
                <p>Thực đơn, công thức, kho, ca làm và đơn tại quầy — mượt mà cho từng chi nhánh, từng vai trò.</p>
                <ul class="lb-features">
                    <li>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                        Đặt món tại quầy &amp; QR khách theo thời gian thực
                    </li>
                    <li>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                        Tồn kho trừ tự động theo công thức &amp; tuỳ chọn
                    </li>
                    <li>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                        Ca làm, chấm công, bảng lương &amp; báo cáo doanh thu
                    </li>
                </ul>
            </div>
        </div>

        <div class="lb-foot">© 2026 Cà Phê Chain · SWP391</div>
    </div>

    <div class="login-formwrap">
        <div class="login-card">
            <div class="login-hint" style="margin-bottom:12px;text-align:left">
                <a href="${ctx}/home">← Về trang chủ</a>
            </div>

            <div class="form-head">
                <h2>Đăng nhập</h2>
                <p>Chào mừng trở lại. Vui lòng nhập thông tin của bạn.</p>
            </div>

            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">${errorMsg}</div>
            </c:if>
            <c:if test="${not empty sessionScope.flashOk}">
                <div class="alert alert-success">${sessionScope.flashOk}</div>
                <c:remove var="flashOk" scope="session" />
            </c:if>

            <form action="${ctx}/auth/login" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>
                    <input id="username" type="text" name="username" class="form-control"
                           value="${username}" required autofocus autocomplete="username" placeholder="admin">
                </div>
                <div class="form-group">
                    <label for="password">Mật khẩu</label>
                    <div class="input-affix">
                        <input id="password" type="password" name="password" class="form-control"
                               required autocomplete="current-password" placeholder="••••••••">
                        <button type="button" class="input-toggle" id="pwdToggle" aria-label="Hiện/ẩn mật khẩu">
                            <svg class="eye" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
                            <svg class="eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.9 5.2A9.7 9.7 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.2 4M6.6 6.6A17 17 0 0 0 2 12s3.5 7 10 7a9.6 9.6 0 0 0 4.5-1.1"/><path d="m4 4 16 16"/></svg>
                        </button>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-full btn-lg">Đăng nhập</button>
            </form>

            <div class="login-hint" style="margin-bottom:6px">
                <a href="${ctx}/auth/forgot">Quên mật khẩu?</a>
            </div>
        </div>
    </div>
</div>

<script>
  // Ẩn/hiện mật khẩu
  (function(){
    var t=document.getElementById('pwdToggle'), p=document.getElementById('password');
    if(t&&p)t.addEventListener('click',function(){
      var show=p.type==='password'; p.type=show?'text':'password'; t.classList.toggle('is-on',show); p.focus();
    });
  })();
</script>
</body>
</html>
