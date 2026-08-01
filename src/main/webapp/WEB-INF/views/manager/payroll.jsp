<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Nhân sự</div>
        <h1>Bảng lương</h1>
        <p>Tính runtime từ giờ chấm công đã duyệt và đơn giá được chụp tại thời điểm duyệt · tháng ${month}</p>
    </div>
    <div style="display:flex;gap:8px;align-items:center">
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${prevMonth}">← Tháng trước</a>
        <strong>${month}</strong>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${nextMonth}">Tháng sau →</a>
        <a class="btn btn-primary btn-sm" href="${ctx}/manager/payroll?action=export&amp;month=${month}">Xuất CSV</a>
    </div>
</div>

<div class="alert alert-info">
    Bảng này chỉ đọc. Nếu một nhân viên có nhiều đơn giá trong tháng, cột Lương/giờ hiển thị bình quân gia quyền;
    thành tiền vẫn được cộng chính xác theo snapshot của từng ca.
</div>

<c:choose>
    <c:when test="${empty rows}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có chấm công đã duyệt trong tháng này.</p></div>
    </c:when>
    <c:otherwise>
        <div class="table-scroll">
            <table class="table">
                <thead>
                <tr>
                    <th>Nhân viên</th>
                    <th style="width:150px">Vai trò</th>
                    <th style="width:90px">Số ca</th>
                    <th style="width:150px">Giờ làm</th>
                    <th style="width:180px">Lương/giờ bình quân (₫)</th>
                    <th style="width:170px">Thành tiền (₫)</th>
                </tr>
                </thead>
                <tbody>
                <c:set var="totalHours" value="0" />
                <c:set var="totalSalary" value="0" />
                <c:forEach var="r" items="${rows}">
                    <tr>
                        <td><c:out value="${r.userName}" /></td>
                        <td><c:out value="${r.roleName}" /></td>
                        <td>${r.approvedShifts}</td>
                        <td>${r.totalHours}h</td>
                        <td><fmt:formatNumber value="${r.hourlyRate}" maxFractionDigits="2" /> ₫</td>
                        <td><strong><fmt:formatNumber value="${r.salary}" maxFractionDigits="0" /></strong> ₫</td>
                    </tr>
                    <c:set var="totalHours" value="${totalHours + r.totalHours}" />
                    <c:set var="totalSalary" value="${totalSalary + r.salary}" />
                </c:forEach>
                <tr style="border-top:2px solid var(--line);font-weight:700">
                    <td colspan="3">Tổng cộng</td>
                    <td><fmt:formatNumber value="${totalHours}" maxFractionDigits="1" /> giờ</td>
                    <td></td>
                    <td><fmt:formatNumber value="${totalSalary}" maxFractionDigits="0" /> ₫</td>
                </tr>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
