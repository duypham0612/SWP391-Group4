<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Nhân sự</div><h1>Bảng lương</h1><p>Tổng giờ làm từ chấm công đã duyệt · tháng ${month}</p></div>
    <div style="display:flex;gap:8px;align-items:center">
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${prevMonth}">← Tháng trước</a>
        <strong>${month}</strong>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${nextMonth}">Tháng sau →</a>
    </div>
</div>

<c:choose>
    <c:when test="${empty rows}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có chấm công đã duyệt trong tháng này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th>Nhân viên</th><th style="width:160px">Vai trò</th>
                <th style="width:140px">Số ca</th><th style="width:160px">Tổng giờ làm</th>
            </tr></thead>
            <tbody>
                <c:set var="totalHours" value="0" />
                <c:set var="totalShifts" value="0" />
                <c:forEach var="r" items="${rows}">
                    <tr>
                        <td>${r.userName}</td>
                        <td>${r.roleName}</td>
                        <td>${r.approvedShifts}</td>
                        <td><strong>${r.totalHours}</strong> giờ</td>
                    </tr>
                    <c:set var="totalHours" value="${totalHours + r.totalHours}" />
                    <c:set var="totalShifts" value="${totalShifts + r.approvedShifts}" />
                </c:forEach>
                <tr style="border-top:2px solid var(--line);font-weight:700">
                    <td colspan="2">Tổng cộng</td>
                    <td>${totalShifts}</td>
                    <td>${totalHours} giờ</td>
                </tr>
            </tbody>
        </table>
        <p class="muted" style="margin-top:10px">Đơn giá theo giờ / hệ số lương cấu hình ở Phase sau; bảng này chốt tổng giờ công làm cơ sở tính lương.</p>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
