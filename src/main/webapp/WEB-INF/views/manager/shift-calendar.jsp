<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Nhân sự</div>
        <h1>Lịch làm việc</h1>
        <p>Nhập trực tiếp tên và khung giờ khi phân ca; hệ thống kiểm tra trùng lịch.</p>
    </div>
    <div style="display:flex;gap:8px;align-items:center">
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/shift?week=${prevWeek}">← Tuần trước</a>
        <strong>Tuần ${weekStart}</strong>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/shift?week=${nextWeek}">Tuần sau →</a>
    </div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><c:out value="${sessionScope.flashError}" /></div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success"><c:out value="${sessionScope.flashOk}" /></div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Xếp ca cho nhân viên</h3>
    <p class="muted">Có thể xếp ca trước giờ bắt đầu hoặc muộn tối đa 10 phút. Ca qua đêm được phép.</p>
    <c:choose>
        <c:when test="${empty staff}">
            <p class="muted">Chi nhánh chưa có nhân viên để xếp ca.</p>
        </c:when>
        <c:otherwise>
            <form action="${ctx}/manager/shift" method="post"
                  style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="assign">
                <input type="hidden" name="week" value="${weekStart}">
                <div class="form-group" style="margin:0;min-width:180px;flex:1">
                    <label>Nhân viên</label>
                    <select name="userId" class="form-control" required>
                        <c:forEach var="s" items="${staff}">
                            <option value="${s.userId}"><c:out value="${s.fullName}" /></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group" style="margin:0;min-width:150px;flex:1">
                    <label>Tên ca</label>
                    <input type="text" name="shiftName" class="form-control"
                           maxlength="60" placeholder="Ca sáng" required>
                </div>
                <div class="form-group" style="margin:0;width:125px">
                    <label>Bắt đầu</label>
                    <input type="time" name="startTime" class="form-control" required>
                </div>
                <div class="form-group" style="margin:0;width:125px">
                    <label>Kết thúc</label>
                    <input type="time" name="endTime" class="form-control" required>
                </div>
                <div class="form-group" style="margin:0;width:160px">
                    <label>Ngày</label>
                    <select name="workDate" class="form-control" required>
                        <c:forEach var="d" items="${weekDays}">
                            <option value="${d}">${d}</option>
                        </c:forEach>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary">Xếp ca</button>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h3 style="margin-top:0">Lịch tuần</h3>
    <c:choose>
        <c:when test="${empty assignments}">
            <div class="empty-state"><div class="icon">∅</div><p>Chưa có ca nào trong tuần này.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-scroll">
                <table class="table">
                    <thead>
                    <tr>
                        <th>Ngày</th>
                        <th>Nhân viên</th>
                        <th>Tên ca</th>
                        <th>Bắt đầu</th>
                        <th>Kết thúc</th>
                        <th style="width:170px">Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${assignments}">
                        <tr>
                            <td>
                                <select name="workDate" class="form-control"
                                        form="edit_${a.shiftAssignmentId}" required>
                                    <c:forEach var="d" items="${weekDays}">
                                        <option value="${d}" <c:if test="${d eq a.workDate}">selected</c:if>>${d}</option>
                                    </c:forEach>
                                </select>
                            </td>
                            <td>
                                <select name="userId" class="form-control"
                                        form="edit_${a.shiftAssignmentId}" required>
                                    <c:forEach var="s" items="${staff}">
                                        <option value="${s.userId}" <c:if test="${s.userId == a.userId}">selected</c:if>>
                                            <c:out value="${s.fullName}" />
                                        </option>
                                    </c:forEach>
                                </select>
                            </td>
                            <td>
                                <input type="text" name="shiftName" class="form-control"
                                       form="edit_${a.shiftAssignmentId}" maxlength="60"
                                       value="${fn:escapeXml(a.shiftName)}" required>
                            </td>
                            <td><input type="time" name="startTime" class="form-control"
                                       form="edit_${a.shiftAssignmentId}" value="${a.startTime}" required></td>
                            <td><input type="time" name="endTime" class="form-control"
                                       form="edit_${a.shiftAssignmentId}" value="${a.endTime}" required></td>
                            <td>
                                <form id="edit_${a.shiftAssignmentId}" action="${ctx}/manager/shift" method="post"
                                      style="display:inline">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="week" value="${weekStart}">
                                    <input type="hidden" name="assignmentId" value="${a.shiftAssignmentId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                                </form>
                                <form action="${ctx}/manager/shift" method="post" style="display:inline"
                                      onsubmit="return confirm('Gỡ ca này?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="unassign">
                                    <input type="hidden" name="week" value="${weekStart}">
                                    <input type="hidden" name="assignmentId" value="${a.shiftAssignmentId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Gỡ</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/footer.jsp" />
