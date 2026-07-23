<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Nhân sự</div><h1>Lịch làm việc</h1><p>Tạo khung giờ làm và phân ca cho nhân viên, có kiểm tra trùng lịch.</p></div>
    <div style="display:flex;gap:8px;align-items:center">
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/shift?week=${prevWeek}">← Tuần trước</a>
        <strong>Tuần ${weekStart}</strong>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/shift?week=${nextWeek}">Tuần sau →</a>
    </div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-bottom:18px">
    <div class="card">
        <h3 style="margin-top:0">Tạo mẫu ca</h3>
        <form action="${ctx}/manager/shift" method="post" style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="createTemplate">
            <input type="hidden" name="week" value="${weekStart}">
            <div class="form-group" style="margin:0;flex:1;min-width:140px"><label>Tên ca</label>
                <input type="text" name="name" class="form-control" placeholder="Ca sáng" required></div>
            <div class="form-group" style="margin:0;width:120px"><label>Bắt đầu</label>
                <input type="time" name="startTime" class="form-control" required></div>
            <div class="form-group" style="margin:0;width:120px"><label>Kết thúc</label>
                <input type="time" name="endTime" class="form-control" required></div>
            <button type="submit" class="btn btn-primary">+ Thêm</button>
        </form>
        <c:if test="${not empty templates}">
            <table class="table" style="margin-top:14px">
                <thead><tr><th>Ca</th><th>Giờ</th><th style="width:70px"></th></tr></thead>
                <tbody>
                    <c:forEach var="t" items="${templates}">
                        <tr>
                            <td>${t.name}</td><td>${t.timeRange}</td>
                            <td>
                                <form action="${ctx}/manager/shift" method="post" style="display:inline" onsubmit="return confirm('Xoá mẫu ca? Các phân công của ca này cũng cần được gỡ trước.');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="deleteTemplate">
                                    <input type="hidden" name="week" value="${weekStart}">
                                    <input type="hidden" name="templateId" value="${t.shiftTemplateId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>

    <div class="card">
        <h3 style="margin-top:0">Xếp ca cho nhân viên</h3>
        <c:choose>
            <c:when test="${empty templates or empty staff}">
                <p class="muted">Cần có ít nhất 1 mẫu ca và 1 nhân viên trong chi nhánh.</p>
            </c:when>
            <c:otherwise>
                <form action="${ctx}/manager/shift" method="post" style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="assign">
                    <input type="hidden" name="week" value="${weekStart}">
                    <div class="form-group" style="margin:0;flex:1;min-width:150px"><label>Nhân viên</label>
                        <select name="userId" class="form-control" required>
                            <c:forEach var="s" items="${staff}"><option value="${s.userId}">${s.fullName}</option></c:forEach>
                        </select></div>
                    <div class="form-group" style="margin:0;width:150px"><label>Ca</label>
                        <select name="templateId" class="form-control" required>
                            <c:forEach var="t" items="${templates}"><option value="${t.shiftTemplateId}">${t.name} (${t.timeRange})</option></c:forEach>
                        </select></div>
                    <div class="form-group" style="margin:0;width:160px"><label>Ngày</label>
                        <select name="workDate" class="form-control" required>
                            <c:forEach var="d" items="${weekDays}"><option value="${d}">${d}</option></c:forEach>
                        </select></div>
                    <button type="submit" class="btn btn-primary">Xếp ca</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card">
    <h3 style="margin-top:0">Lịch tuần</h3>
    <c:choose>
        <c:when test="${empty templates}">
            <p class="muted">Chưa có mẫu ca nào.</p>
        </c:when>
        <c:otherwise>
            <table class="table">
                <thead>
                    <tr>
                        <th style="width:140px">Ca</th>
                        <c:forEach var="d" items="${weekDays}"><th>${d}</th></c:forEach>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="t" items="${templates}">
                        <tr>
                            <td><strong>${t.name}</strong><br><span class="muted">${t.timeRange}</span></td>
                            <c:forEach var="d" items="${weekDays}">
                                <td>
                                    <c:forEach var="a" items="${assignments}">
                                        <c:if test="${a.shiftTemplateId == t.shiftTemplateId and a.workDate eq d}">
                                            <span class="badge badge-making" style="display:inline-flex;gap:6px;align-items:center;margin:2px 0">
                                                ${a.userName}
                                                <form action="${ctx}/manager/shift" method="post" style="display:inline" onsubmit="return confirm('Gỡ ca này?');">
                                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="unassign">
                                                    <input type="hidden" name="week" value="${weekStart}">
                                                    <input type="hidden" name="assignmentId" value="${a.shiftAssignmentId}">
                                                    <button type="submit" title="Gỡ" style="background:none;border:none;color:inherit;cursor:pointer;padding:0;font-weight:700">×</button>
                                                </form>
                                            </span><br>
                                        </c:if>
                                    </c:forEach>
                                </td>
                            </c:forEach>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/footer.jsp" />
