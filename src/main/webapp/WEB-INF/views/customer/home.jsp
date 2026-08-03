<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#291A0F">
    <title>Cà Phê Chain<c:if test="${not empty home.name}"> · <c:out value="${home.name}"/></c:if></title>
    <script>(function(){try{var t=localStorage.getItem('cafe-theme');if(t)document.documentElement.setAttribute('data-theme',t);}catch(e){}})();</script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/core.css?v=${applicationScope.assetVersion}">
    <link rel="stylesheet" href="${ctx}/assets/css/customer.css?v=${applicationScope.assetVersion}">
    <link rel="stylesheet" href="${ctx}/assets/css/responsive.css?v=${applicationScope.assetVersion}">
</head>
<body>
<div class="site">
    <c:url var="homeUrl" value="/home">
        <c:if test="${not empty home}"><c:param name="branchId" value="${home.branchId}"/></c:if>
    </c:url>

    <nav class="site-nav">
        <a class="brand" href="${homeUrl}">
            <span class="logo">C</span>
            <span>Cà Phê Chain</span>
        </a>
        <div class="nav-actions">
            <button type="button" class="icon-btn" id="themeToggle" title="Đổi giao diện sáng/tối" aria-label="Đổi giao diện sáng/tối">
                <svg class="sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
                <svg class="moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
            </button>
            <a class="btn btn-primary btn-sm" href="${ctx}/auth/login">Đăng nhập</a>
        </div>
    </nav>

    <c:set var="heroEyebrow"  value="${empty home.heroEyebrow ? 'Chuỗi cà phê thủ công' : home.heroEyebrow}" />
    <c:set var="heroTitle"    value="${empty home.heroTitle ? 'Thực đơn của Cà Phê Chain' : home.heroTitle}" />
    <c:set var="heroSubtitle" value="${empty home.heroSubtitle ? 'Khám phá menu cà phê, trà và đá xay được pha chế tươi mỗi ngày. Đến quán, quét QR tại bàn để đặt món ngay.' : home.heroSubtitle}" />
    <c:set var="heroImg"      value="${empty home.heroImageUrl ? ctx.concat('/assets/img/login-hero.svg') : (fn:startsWith(fn:toLowerCase(home.heroImageUrl), 'http') ? home.heroImageUrl : ctx.concat(home.heroImageUrl))}" />

    <header class="home-hero">
        <div class="home-hero__text">
            <c:if test="${not empty home}">
                <div class="home-branch-context" aria-label="Chi nhánh đang xem">
                    <svg class="home-branch-context__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z"/>
                        <circle cx="12" cy="10" r="2.5"/>
                    </svg>
                    <span>
                        <small>Đang xem chi nhánh</small>
                        <strong><c:out value="${home.name}"/></strong>
                        <c:if test="${not empty home.address}"><em><c:out value="${home.address}"/></em></c:if>
                    </span>
                    <c:if test="${fn:length(branches) > 1}">
                        <form action="${ctx}/home" method="get" class="home-branch-switcher">
                            <label for="publicBranch">Xem Home chi nhánh khác</label>
                            <select id="publicBranch" name="branchId" onchange="this.form.submit()"
                                    aria-label="Chọn chi nhánh muốn xem">
                                <c:forEach var="b" items="${branches}">
                                    <option value="${b.branchId}" <c:if test="${b.branchId == home.branchId}">selected</c:if>><c:out value="${b.name}"/></option>
                                </c:forEach>
                            </select>
                            <noscript><button type="submit" class="btn btn-gold btn-sm">Xem</button></noscript>
                        </form>
                    </c:if>
                </div>
            </c:if>
            <div class="eyebrow"><c:out value="${heroEyebrow}"/></div>
            <h1><c:out value="${heroTitle}"/></h1>
            <p><c:out value="${heroSubtitle}"/></p>
            <div class="home-hero__cta">
                <a class="btn btn-gold btn-lg" href="#menu">Xem thực đơn</a>
                <a class="btn btn-outline btn-lg" href="${ctx}/auth/login">Dành cho nhân viên</a>
            </div>
        </div>
        <div class="home-hero__art">
            <img src="${fn:escapeXml(heroImg)}" alt="${fn:escapeXml(heroTitle)}" width="560" height="480"
                 onerror="this.src='${ctx}/assets/img/login-hero.svg'">
        </div>
    </header>

    <c:if test="${not empty sections}">
        <div class="cat-strip">
            <c:forEach var="s" items="${sections}" varStatus="st">
                <a class="cat-chip" href="#sec${st.index}"><c:out value="${s.name}"/></a>
            </c:forEach>
        </div>
    </c:if>

    <main class="home-main" id="menu">
        <c:choose>
            <c:when test="${empty sections}">
                <div class="card empty-state"><div class="icon">☕</div><p>Thực đơn đang được cập nhật. Vui lòng quay lại sau.</p></div>
            </c:when>
            <c:otherwise>
                <c:forEach var="s" items="${sections}" varStatus="st">
                    <section class="menu-section" id="sec${st.index}">
                        <div class="menu-section__head">
                            <h2><c:out value="${s.name}"/></h2>
                            <span class="count">${s.count} món</span>
                        </div>
                        <div class="home-grid">
                            <c:forEach var="p" items="${s.products}">
                                <c:set var="imgSrc" value="${empty p.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (fn:startsWith(fn:toLowerCase(p.imageUrl), 'http') ? p.imageUrl : ctx.concat(p.imageUrl))}" />
                                <article class="home-card">
                                    <img src="${fn:escapeXml(imgSrc)}" alt="${fn:escapeXml(p.name)}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                                    <div class="body">
                                        <div class="name"><c:out value="${p.name}"/></div>
                                        <div class="price"><fmt:formatNumber value="${p.basePrice}" maxFractionDigits="0"/> ₫</div>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>
                    </section>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </main>

    <footer class="site-foot">
        © 2026 Cà Phê Chain<c:if test="${not empty home.name}"> · <c:out value="${home.name}"/></c:if> — Dine-in &amp; đặt món tại bàn qua QR.
        <a href="${ctx}/auth/login">Đăng nhập nhân viên</a>
    </footer>
</div>

<script>
  (function(){
    var btn=document.getElementById('themeToggle'); if(!btn)return;
    btn.addEventListener('click',function(){
      var next=document.documentElement.getAttribute('data-theme')==='dark'?'light':'dark';
      document.documentElement.setAttribute('data-theme',next);
      try{localStorage.setItem('cafe-theme',next);}catch(e){}
    });
  })();
</script>
</body>
</html>
