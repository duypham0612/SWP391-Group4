<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Tra cứu công thức</h1><p>Xem định mức nguyên liệu thô / nguyên liệu pha sẵn &amp; tác động của modifier — chỉ đọc</p></div>
</div>

<div class="card form-card" style="margin-bottom:18px">
    <form action="${ctx}/barista/recipe" method="get" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <div class="form-group" style="margin:0;flex:1;min-width:240px"><label>Tìm món</label>
            <input type="text" name="q" class="form-control" value="${q}" placeholder="Nhập tên món...">
        </div>
        <button type="submit" class="btn btn-primary">Tìm</button>
    </form>
</div>

<div style="display:flex;gap:18px;align-items:flex-start;flex-wrap:wrap">
    <div class="card" style="flex:1;min-width:260px">
        <h3 style="margin-top:0">Món (${products.size()})</h3>
        <c:choose>
            <c:when test="${empty products}">
                <div class="empty-state"><div class="icon">∅</div><p>Không có món phù hợp.</p></div>
            </c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Món</th><th style="width:90px"></th></tr></thead>
                    <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td>${p.name}<br><small style="color:var(--muted)">${p.categoryName}</small></td>
                                <td><a class="btn btn-ghost btn-sm" href="${ctx}/barista/recipe?productId=${p.productId}<c:if test='${not empty q}'>&q=${q}</c:if>">Xem</a></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card" style="flex:1.4;min-width:320px">
        <c:choose>
            <c:when test="${empty selected}">
                <div class="empty-state"><div class="icon">☕</div><p>Chọn một món để xem công thức.</p></div>
            </c:when>
            <c:otherwise>
                <h3 style="margin-top:0">${selected.name}</h3>

                <h4>Định mức / 1 phần</h4>
                <c:choose>
                    <c:when test="${empty recipe}">
                        <p style="color:var(--muted)">Chưa khai báo công thức cho món này.</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table">
                            <thead><tr><th>Nguyên liệu</th><th style="width:120px">Định mức</th><th style="width:110px">Loại</th></tr></thead>
                            <tbody>
                                <c:forEach var="r" items="${recipe}">
                                    <tr>
                                        <td>${r.ingredientName}</td>
                                        <td><strong>${r.quantity}</strong> ${r.ingredientUnit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.ingredientType == 'PREPPED'}"><span class="badge" style="background:var(--latte)">Pha sẵn</span></c:when>
                                                <c:otherwise><span class="badge">Thô</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty preps}">
                    <h4>Định mức pha sẵn</h4>
                    <c:forEach var="ps" items="${preps}">
                        <p style="margin:6px 0"><strong>${ps.name}</strong></p>
                        <table class="table">
                            <thead><tr><th>Nguyên liệu thô</th><th style="width:120px">Dùng</th><th style="width:120px">Sản lượng (yield)</th></tr></thead>
                            <tbody>
                                <c:forEach var="l" items="${ps.lines}">
                                    <tr><td>${l.rawIngredientName}</td><td>${l.quantity} ${l.rawIngredientUnit}</td><td>${l.yieldQty}</td></tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:forEach>
                </c:if>

                <h4>Tác động của Modifier</h4>
                <c:choose>
                    <c:when test="${empty impacts}">
                        <p style="color:var(--muted)">Modifier không ảnh hưởng định mức (hoặc chưa khai báo).</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table">
                            <thead><tr><th>Nhóm</th><th>Tuỳ chọn</th><th>Nguyên liệu</th><th style="width:130px">Thay đổi</th></tr></thead>
                            <tbody>
                                <c:forEach var="im" items="${impacts}">
                                    <tr>
                                        <td>${im.groupName}</td>
                                        <td>${im.optionName}</td>
                                        <td>${im.ingredientName}</td>
                                        <td><strong>${im.qtyDelta}</strong> ${im.ingredientUnit}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<jsp:include page="../layout/footer.jsp" />
