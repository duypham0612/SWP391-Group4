<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="../layout/header.jsp" />

<div class="welcome">
    <h1>Xin chào, ${sessionScope.authUser.fullName}</h1>
    <p>Pha chế · ${sessionScope.authUser.branchName}</p>
</div>

<div class="card">
    <h3 style="margin-top:0">Trạng thái món (dùng chung toàn hệ thống)</h3>
    <p class="muted" style="margin-top:0">Hàng chờ KDS &amp; auto-deduct theo modifier đã mở — vào <a href="${pageContext.request.contextPath}/barista/kds">Hàng chờ (KDS)</a>.</p>
    <div style="display:flex; gap:10px; flex-wrap:wrap; margin-top:6px">
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="WAITING" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="MAKING" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="READY" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="SERVED" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="CANCELLED" /></jsp:include>
    </div>
</div>

<jsp:include page="../layout/footer.jsp" />
