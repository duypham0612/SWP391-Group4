<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<section id="menu86" class="card" style="margin-top:24px">
    <div class="page-header" style="margin-bottom:16px">
        <div><div class="eyebrow">Tình trạng phục vụ</div><h2 style="margin:0">Món tạm hết</h2>
            <p>Tiếp nhận báo cáo từ quầy pha chế và quyết định thời điểm mở bán lại.</p></div>
        <span class="badge badge-waiting">${fn:length(openRequests)} yêu cầu đang mở</span>
    </div>
    <c:choose>
        <c:when test="${empty openRequests}">
            <div class="empty-state"><div class="icon">✓</div><p>Không có món tạm hết đang chờ xử lý.</p></div>
        </c:when>
        <c:otherwise><div class="table-scroll"><table class="table">
            <thead><tr><th>Món</th><th>Lý do</th><th>Ghi chú</th><th>Dự kiến có lại</th><th>Người báo</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody><c:forEach var="r" items="${openRequests}"><tr>
                <td><strong>${r.productName}</strong></td><td>${r.reasonLabel}</td>
                <td><c:choose><c:when test="${empty r.note}"><span class="muted">Không có</span></c:when><c:otherwise><c:out value="${r.note}" /></c:otherwise></c:choose></td>
                <td><span class="${r.overdue ? 'badge badge-cancelled' : ''}">${r.backInEtaText}</span><c:if test="${r.overdue}"><small class="muted" style="display:block;color:var(--st-cancelled)">Đã quá thời gian dự kiến</small></c:if></td>
                <td>${r.requesterName}<small class="muted" style="display:block">${r.requestedAtText}</small></td>
                <td><span class="badge badge-waiting">${r.statusLabel}</span><c:if test="${not empty r.reopenRequestedAt}"><small class="muted" style="display:block">Quầy pha chế đề nghị mở lại</small></c:if></td>
                <td><div class="btn-row">
                    <form action="${ctx}/manager/menu-block" method="post" style="margin:0"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="requestId" value="${r.requestId}"><input type="hidden" name="action" value="reopen"><button type="submit" class="btn btn-sm btn-primary">Mở bán lại</button></form>
                    <c:if test="${r.status == 'PENDING'}"><form action="${ctx}/manager/menu-block" method="post" style="margin:0"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="requestId" value="${r.requestId}"><input type="hidden" name="action" value="reject"><button type="submit" class="btn btn-sm btn-ghost">Không chấp nhận</button></form></c:if>
                </div></td>
            </tr></c:forEach></tbody>
        </table></div></c:otherwise>
    </c:choose>
</section>

<details class="card" style="margin-top:16px">
    <summary style="cursor:pointer;font-weight:700">Lịch sử xử lý món tạm hết (${fn:length(requestHistory)})</summary>
    <c:choose><c:when test="${empty requestHistory}"><p class="muted">Chưa có lịch sử xử lý.</p></c:when>
        <c:otherwise><div class="table-scroll" style="margin-top:12px"><table class="table">
            <thead><tr><th>Món</th><th>Lý do</th><th>Trạng thái</th><th>Người xử lý</th><th>Thời điểm đóng</th><th>Ghi chú</th></tr></thead>
            <tbody><c:forEach var="r" items="${requestHistory}"><tr><td>${r.productName}</td><td>${r.reasonLabel}</td><td><span class="badge ${r.status == 'REJECTED' ? 'badge-cancelled' : 'badge-ready'}">${r.statusLabel}</span></td><td>${empty r.reviewerName ? 'Hệ thống' : r.reviewerName}</td><td>${r.closedAtText}</td><td><c:out value="${r.reviewNote}" default="Không có" /></td></tr></c:forEach></tbody>
        </table></div></c:otherwise>
    </c:choose>
</details>
