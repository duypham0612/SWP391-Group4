<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Hết món (86)</h1><p>Tắt món tạm thời — khoá khỏi POS và menu QR của khách</p></div>
</div>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có món nào trên menu.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Món</th><th style="width:140px">Trạng thái</th><th style="width:160px"></th></tr></thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:if test="${m.published}">
                        <tr>
                            <td>${m.productName}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.is86}">
                                        <span class="badge badge-cancelled">86 — Hết</span>
                                        <c:if test="${not empty m.backInEtaText}">
                                            <div class="muted" style="font-size:.82em;margin-top:4px">Có lại: ${m.backInEtaText}</div>
                                        </c:if>
                                    </c:when>
                                    <c:otherwise><span class="badge badge-ready">Còn bán</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.is86}">
                                        <form action="${ctx}/barista/eightysix" method="post">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggle86">
                                            <input type="hidden" name="productId" value="${m.productId}">
                                            <input type="hidden" name="is86" value="false">
                                            <button type="submit" class="btn btn-sm btn-primary">Mở bán lại</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form action="${ctx}/barista/eightysix" method="post" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;margin:0">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggle86">
                                            <input type="hidden" name="productId" value="${m.productId}">
                                            <input type="hidden" name="is86" value="true">
                                            <input type="datetime-local" name="backInEta" class="form-control" style="width:185px" title="Dự kiến có lại (tuỳ chọn)">
                                            <button type="submit" class="btn btn-sm btn-ghost">Báo hết (86)</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:if>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
