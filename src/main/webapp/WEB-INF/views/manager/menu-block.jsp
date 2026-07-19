<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Thực đơn</div><h1>Món tạm hết</h1><p>Hàng chờ barista báo tạm hết và yêu cầu mở bán lại</p></div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<h2 style="font-size:1.1rem;margin:0 0 12px">Đang mở</h2>
<c:choose>
    <c:when test="${empty openRequests}">
        <div class="card empty-state"><div class="icon">✓</div><p>Không có món tạm hết đang chờ xử lý.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Món</th><th style="width:150px">Lý do</th><th>Ghi chú</th>
                <th style="width:150px">Dự kiến có lại</th><th style="width:150px">Người báo</th>
                <th style="width:110px">Lúc</th><th style="width:170px">Trạng thái</th><th style="width:320px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="r" items="${openRequests}">
                    <tr>
                        <td><strong>${r.productName}</strong></td>
                        <td>${r.reasonLabel}</td>
                        <td>
                            <c:choose><c:when test="${empty r.note}"><span class="muted">Không có</span></c:when><c:otherwise>${r.note}</c:otherwise></c:choose>
                        </td>
                        <td>
                            <span class="${r.overdue ? 'badge badge-cancelled' : ''}">${r.backInEtaText}</span>
                            <c:if test="${r.overdue}"><div class="muted" style="font-size:.82em;margin-top:4px;color:var(--st-cancelled)">Quá hạn</div></c:if>
                        </td>
                        <td>${r.requesterName}</td>
                        <td>${r.requestedAtText}</td>
                        <td>
                            <span class="badge badge-waiting">${r.statusLabel}</span>
                            <c:if test="${not empty r.reopenRequestedAt}">
                                <div class="badge badge-ready" style="margin-top:6px">Barista xin mở bán</div>
                                <div class="muted" style="font-size:.82em;margin-top:4px">${r.reopenRequestedAtText}</div>
                            </c:if>
                        </td>
                        <td>
                            <div style="display:grid;gap:8px">
                                <c:if test="${r.status == 'PENDING'}">
                                    <form action="${ctx}/manager/menu-block" method="post" style="display:flex;gap:6px;align-items:center;margin:0">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="requestId" value="${r.requestId}">
                                        <input type="hidden" name="action" value="approve">
                                        <input name="reviewNote" class="form-control" maxlength="255" placeholder="Ghi chú duyệt">
                                        <button type="submit" class="btn btn-sm btn-ghost">Duyệt</button>
                                    </form>
                                </c:if>
                                <div style="display:flex;gap:6px;flex-wrap:wrap">
                                    <form action="${ctx}/manager/menu-block" method="post" style="margin:0">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="requestId" value="${r.requestId}">
                                        <input type="hidden" name="action" value="reopen">
                                        <button type="submit" class="btn btn-sm btn-primary">Mở bán lại</button>
                                    </form>
                                    <c:if test="${r.status == 'PENDING'}">
                                        <form action="${ctx}/manager/menu-block" method="post" style="margin:0">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="requestId" value="${r.requestId}">
                                            <input type="hidden" name="action" value="reject">
                                            <button type="submit" class="btn btn-sm btn-ghost" style="color:var(--st-cancelled)">Từ chối</button>
                                        </form>
                                    </c:if>
                                </div>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<h2 style="font-size:1.1rem;margin:24px 0 12px">Lịch sử gần đây</h2>
<c:choose>
    <c:when test="${empty history}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có lịch sử xử lý món tạm hết.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Món</th><th style="width:150px">Lý do</th><th>Ghi chú</th>
                <th style="width:120px">Trạng thái</th><th style="width:140px">Người duyệt</th>
                <th style="width:110px">Đóng lúc</th><th>Ghi chú duyệt</th>
            </tr></thead>
            <tbody>
                <c:forEach var="r" items="${history}">
                    <tr>
                        <td><strong>${r.productName}</strong></td>
                        <td>${r.reasonLabel}</td>
                        <td><c:choose><c:when test="${empty r.note}"><span class="muted">Không có</span></c:when><c:otherwise>${r.note}</c:otherwise></c:choose></td>
                        <td><span class="badge ${r.status == 'REJECTED' ? 'badge-cancelled' : 'badge-ready'}">${r.statusLabel}</span></td>
                        <td><c:choose><c:when test="${empty r.reviewerName}"><span class="muted">Không rõ</span></c:when><c:otherwise>${r.reviewerName}</c:otherwise></c:choose></td>
                        <td>${r.closedAtText}</td>
                        <td><c:choose><c:when test="${empty r.reviewNote}"><span class="muted">Không có</span></c:when><c:otherwise>${r.reviewNote}</c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
