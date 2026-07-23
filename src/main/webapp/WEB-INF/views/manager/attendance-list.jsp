<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Nhân sự</div><h1>Chấm công</h1><p>Kiểm tra giờ vào, giờ ra và duyệt thời gian làm việc của nhân viên.</p></div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<style>
    .att-check{width:22px;height:22px;accent-color:var(--st-ready);cursor:pointer;vertical-align:middle}
</style>

<c:choose>
    <c:when test="${empty attendances}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có dữ liệu chấm công tại chi nhánh này.</p></div>
    </c:when>
    <c:otherwise>
        <%-- Form chấm công hàng loạt: tick = duyệt (✓ xanh), bỏ tick = chờ duyệt --%>
        <form id="bulkAtt" action="${ctx}/manager/attendance" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="approveMany">
        </form>
        <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center">
            <button type="submit" form="bulkAtt" class="btn btn-primary">Lưu chấm công (✓ = duyệt)</button>
            <label style="display:flex;gap:6px;align-items:center;color:var(--muted)">
                <input type="checkbox" class="att-check" onclick="document.querySelectorAll('.rowcheck').forEach(c=>c.checked=this.checked)"> Chọn tất cả
            </label>
        </div>

        <table class="table">
            <thead><tr>
                <th style="width:50px">✓</th>
                <th>Nhân viên</th>
                <th style="width:130px">Cơ sở</th>
                <th>Ca làm (ngày · giờ)</th>
                <th>Check-in / Check-out</th>
                <th style="width:80px">Giờ</th>
                <th style="width:130px">Trạng thái</th>
            </tr></thead>
            <tbody>
                <c:forEach var="a" items="${attendances}">
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${a.status != 'REJECTED'}">
                                    <input type="checkbox" form="bulkAtt" name="approve" value="${a.attendanceId}"
                                           class="att-check rowcheck" <c:if test="${a.status == 'APPROVED'}">checked</c:if>>
                                    <input type="hidden" form="bulkAtt" name="shown" value="${a.attendanceId}">
                                </c:when>
                                <c:otherwise><span class="muted">—</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <strong>${a.userName}</strong>
                            <div class="muted" style="font-size:.85rem">${a.roleName}<c:if test="${not empty a.userPhone}"> · ${a.userPhone}</c:if></div>
                        </td>
                        <td>${a.branchName}</td>
                        <td>
                            ${a.workDate}<br>
                            <span class="muted">${a.templateName} (${a.startTime}–${a.endTime})</span>
                        </td>
                        <td>
                            <form action="${ctx}/manager/attendance" method="post" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;margin:0">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="edit">
                                <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                <input type="datetime-local" name="checkInAt"  class="form-control" style="width:185px" value="${a.checkInAt}">
                                <input type="datetime-local" name="checkOutAt" class="form-control" style="width:185px" value="${a.checkOutAt}">
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu giờ</button>
                            </form>
                        </td>
                        <td>
                            <strong>${a.workHours}</strong>
                            <c:if test="${a.late}"><div><span class="badge badge-waiting" title="Vào trễ">Trễ ${a.lateMinutes}'</span></div></c:if>
                            <c:if test="${a.earlyLeave}"><div style="margin-top:3px"><span class="badge badge-making" title="Về sớm">Sớm ${a.earlyLeaveMinutes}'</span></div></c:if>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${a.status == 'APPROVED'}">
                                    <span class="badge badge-ready">✓ Đã duyệt</span>
                                    <form action="${ctx}/manager/attendance" method="post" style="display:inline" onsubmit="return confirm('Từ chối chấm công này?');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Từ chối</button>
                                    </form>
                                </c:when>
                                <c:when test="${a.status == 'REJECTED'}">
                                    <span class="badge badge-cancelled">Từ chối</span>
                                    <form action="${ctx}/manager/attendance" method="post" style="display:inline">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="reopen">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Mở lại</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-waiting">Chờ duyệt</span>
                                    <form action="${ctx}/manager/attendance" method="post" style="display:inline" onsubmit="return confirm('Từ chối chấm công này?');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="attendanceId" value="${a.attendanceId}">
                                        <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Từ chối</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <button type="submit" form="bulkAtt" class="btn btn-primary" style="margin-top:12px">Lưu chấm công (✓ = duyệt)</button>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
