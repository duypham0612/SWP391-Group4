<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="u" value="${sessionScope.authUser}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#291A0F">
    <title>${pageTitle} · Cafe Chain</title>
    <script>
      /* Đặt theme trước khi paint để tránh nháy màn (FOUC). */
      (function(){try{var t=localStorage.getItem('cafe-theme');if(t)document.documentElement.setAttribute('data-theme',t);}catch(e){}})();
    </script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
</head>
<body>
<div class="app-shell">
    <jsp:include page="sidebar.jsp" />
    <div class="app-main">
        <header class="topbar">
            <div class="topbar-title">${pageTitle}</div>
            <div class="topbar-user">
                <button type="button" class="icon-btn" id="themeToggle" title="Đổi giao diện sáng/tối" aria-label="Đổi giao diện sáng/tối">
                    <svg class="sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
                    <svg class="moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
                </button>
                <span class="user-meta">
                    <strong>${u.fullName}</strong>
                    <small>${u.roleName}<c:if test="${not empty u.branchName}"> · ${u.branchName}</c:if></small>
                </span>
                <span class="avatar">${u.fullName.substring(0,1)}</span>
                <a class="btn btn-ghost btn-sm" href="${ctx}/auth/logout">Đăng xuất</a>
            </div>
        </header>
        <main class="app-content">
