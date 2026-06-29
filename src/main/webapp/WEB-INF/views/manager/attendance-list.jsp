<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Nhân sự</div><h1>Chấm công</h1><p>hr.Attendance — duyệt giờ làm của nhân viên</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<div class="tabs" style="display:flex;gap:8px;margin-bottom:16px">
    <a class="btn btn-sm ${status == 'PENDING' ? 'btn-primary' : 'btn-ghost'}"  href="${ctx}/manager/attendance?status=PENDING">Chờ duyệt</a>
    <a class="btn btn-sm ${status == 'APPROVED' ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/manager/attendance?status=APPROVED">Đã duyệt</a>
    <a class="btn btn-sm ${status == 'REJECTED' ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/manager/attendance?status=REJECTED">Từ chối</a>
</div>

<c:choose>
    <c:when test="${empty attendances}">
        <div class="card empty-state"><div class="icon">∅</div><p>Không có bản ghi chấm công ở trạng thái này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Nhân viên</th><th style="width:110px">Ngày</th><th>Ca</th>
                <th>Check-in / Check-out</th><th style="width:80px">Giờ</th>
                <th style="width:220px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="a" items="${attendances}">
                    <tr>
                        <td>${a.userName}</td>
                        <td>${a.workDate}</td>
                        <td>${a.templateName} <span class="muted">(${a.startTime}–${a.endTime})</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${status == 'PENDING'}">
                                    <form action="${ctx}/manager/attendance" method="post" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;margin:0">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="edit">
                                        <input type="hidden" name="status" value="${status}">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <input type="datetime-local" name="checkInAt"  class="form-control" style="width:200px" value="${a.checkInAt}">
                                        <input type="datetime-local" name="checkOutAt" class="form-control" style="width:200px" value="${a.checkOutAt}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Lưu giờ</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    ${a.checkInAt} → ${a.checkOutAt}
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <strong>${a.workHours}</strong>
                            <c:if test="${a.late}"><div><span class="badge badge-waiting" title="Vào trễ">Trễ ${a.lateMinutes}'</span></div></c:if>
                            <c:if test="${a.earlyLeave}"><div style="margin-top:3px"><span class="badge badge-making" title="Về sớm">Sớm ${a.earlyLeaveMinutes}'</span></div></c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${status == 'PENDING'}">
                                    <form action="${ctx}/manager/attendance" method="post" style="display:inline">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="approve">
                                        <input type="hidden" name="status" value="${status}">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <button type="submit" class="btn btn-primary btn-sm">Duyệt</button>
                                    </form>
                                    <form action="${ctx}/manager/attendance" method="post" style="display:inline" onsubmit="return confirm('Từ chối chấm công này?');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="status" value="${status}">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Từ chối</button>
                                    </form>
                                </c:when>
                                <c:when test="${status == 'APPROVED'}">
                                    <span class="badge badge-ready">Đã duyệt</span>
                                    <c:if test="${not empty a.approverName}"><span class="muted"> · ${a.approverName}</span></c:if>
                                </c:when>
                                <c:otherwise><span class="badge badge-cancelled">Từ chối</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
