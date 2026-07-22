<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header"><div><div class="eyebrow">Pha chế</div><h1>Bàn giao ca</h1><p>Giao việc còn lại cho đúng ca tiếp theo và theo dõi đến khi hoàn tất.</p></div></div>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" /></c:if>
<c:if test="${expiredPrepBatchCount > 0}"><div class="alert alert-warn">Có ${expiredPrepBatchCount} mẻ pha sẵn đã quá hạn. <a href="${ctx}/barista/prep">Kiểm tra Pha sẵn</a> trước khi bàn giao.</div></c:if>

<c:if test="${pendingHandoverCount > 0}"><div class="alert alert-warn"><strong>Bạn có ${pendingHandoverCount} bàn giao chưa xác nhận.</strong> Đọc nội dung và bấm “Đã nhận bàn giao” bên dưới trước khi xử lý việc.</div></c:if>

<c:choose>
  <c:when test="${onShift}">
    <div class="card form-card" style="margin-bottom:var(--s5)">
      <h3 style="margin-top:0">Tạo bàn giao cho ca sau</h3>
      <c:choose><c:when test="${not empty receiverPreview}"><p class="alert alert-info">Người nhận: <strong>${receiverPreview.label}</strong><c:if test="${receiverPreview.managerFallback}"> · chưa có ca barista kế tiếp nên quản lý sẽ nhận dự phòng</c:if></p></c:when><c:otherwise><p class="alert alert-error">${receiverPreviewError}</p></c:otherwise></c:choose>
      <c:if test="${not empty receiverPreview}">
      <form action="${ctx}/barista/handover" method="post" id="handoverCreateForm">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group"><label>Việc cần bàn giao</label><div id="handoverTasks"><input class="form-control" name="task" maxlength="500" required placeholder="VD: Kiểm tra máy xay #2 kêu lạ"></div><button type="button" class="btn btn-ghost btn-sm" id="addHandoverTask" style="margin-top:8px">+ Thêm việc</button><div class="muted" style="margin-top:6px">Tối đa 10 việc. Mỗi việc sẽ được ca nhận theo dõi riêng.</div></div>
        <div class="form-group"><label for="handoverNote">Ghi chú chung <span class="muted">(không bắt buộc)</span></label><textarea id="handoverNote" name="note" class="form-control" rows="3" maxlength="1000" placeholder="Bối cảnh chung cho ca nhận..."></textarea></div>
        <div style="display:flex;gap:8px;flex-wrap:wrap"><button type="submit" name="action" value="create" class="btn btn-ghost">Lưu bàn giao</button><button type="submit" name="action" value="createAndClockOut" class="btn btn-primary">Lưu bàn giao &amp; Tan ca</button></div>
      </form></c:if>
    </div>
  </c:when>
  <c:otherwise><div class="alert alert-info">Bạn cần <a href="${ctx}/barista/shift">vào ca</a> trước khi tạo bàn giao. Bạn vẫn có thể xem các bàn giao được gửi cho mình.</div></c:otherwise>
</c:choose>

<h2>Bàn giao cần xử lý và lịch sử</h2>
<c:choose><c:when test="${empty handovers}"><div class="card empty-state"><div class="icon">∅</div><p>Chưa có bàn giao nào.</p></div></c:when><c:otherwise>
  <c:forEach var="h" items="${handovers}">
    <article class="card" style="margin-bottom:var(--s4);${h.canAcknowledge ? 'border-color:var(--st-waiting)' : ''}">
      <div style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap"><div><h3 style="margin:0 0 4px">Bàn giao bởi ${h.createdByName}</h3><div class="muted">${h.createdDisplay}<c:if test="${not empty h.sourceShiftLabel}"> · ${h.sourceShiftLabel}</c:if></div></div><span class="badge ${h.overallStatusBadge}">${h.overallStatusLabel}</span></div>
      <c:if test="${not empty h.note}"><p style="margin:var(--s3) 0"><strong>Ghi chú:</strong> <c:out value="${h.note}" /></p></c:if>
      <c:if test="${not empty h.recipients}"><div class="muted" style="margin:var(--s3) 0"><strong>Người nhận:</strong> <c:forEach var="r" items="${h.recipients}" varStatus="loop"><c:out value="${r.recipientName}" /> <span class="badge ${r.acknowledged ? 'badge-ready' : 'badge-waiting'}">${r.acknowledged ? 'Đã nhận' : 'Chưa nhận'}</span><c:if test="${not empty r.shiftLabel}"> · ${r.shiftLabel}</c:if><c:if test="${not loop.last}">; </c:if></c:forEach></div></c:if>
      <c:if test="${h.canAcknowledge}"><form action="${ctx}/barista/handover" method="post" style="margin:var(--s3) 0"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="acknowledge"><input type="hidden" name="handoverId" value="${h.shiftHandoverId}"><button class="btn btn-primary">Đã nhận bàn giao</button></form></c:if>
      <c:if test="${not empty h.tasks}"><div class="table-scroll"><table class="table" style="min-width:620px"><thead><tr><th>Việc cần xử lý</th><th style="width:130px">Trạng thái</th><th style="width:180px">Cập nhật</th><th style="width:130px"></th></tr></thead><tbody><c:forEach var="t" items="${h.tasks}"><tr><td><c:out value="${t.content}" /></td><td><span class="badge ${t.statusBadge}">${t.statusLabel}</span></td><td class="muted"><c:out value="${empty t.updatedByName ? '—' : t.updatedByName}" /><c:if test="${not empty t.updatedAt}"> · ${t.updatedDisplay}</c:if></td><td><c:if test="${h.canUpdateTasks}"><form action="${ctx}/barista/handover" method="post"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="updateTask"><input type="hidden" name="handoverId" value="${h.shiftHandoverId}"><input type="hidden" name="taskId" value="${t.shiftHandoverTaskId}"><select name="status" class="form-control" onchange="this.form.submit()"><option value="NEW" ${t.status == 'NEW' ? 'selected' : ''}>Mới</option><option value="IN_PROGRESS" ${t.status == 'IN_PROGRESS' ? 'selected' : ''}>Đang xử lý</option><option value="DONE" ${t.status == 'DONE' ? 'selected' : ''}>Đã xử lý</option></select></form></c:if></td></tr></c:forEach></tbody></table></div></c:if>
    </article>
  </c:forEach>
</c:otherwise></c:choose>

<div class="page-header" style="margin-top:var(--s6)"><div><h2 style="margin:0">Cả quán hôm nay</h2><p>Số liệu toàn chi nhánh để ca nhận nắm tình hình.</p></div></div>
<div class="card-grid"><div class="card stat"><span class="label">Thời gian pha trung bình</span><span class="value">${kpi.avgLeadDisplay}</span></div><div class="card stat"><span class="label">Số món đã pha xong</span><span class="value">${kpi.cupCount}</span></div></div>
<script>(function(){var add=document.getElementById('addHandoverTask'),list=document.getElementById('handoverTasks');if(!add||!list)return;add.addEventListener('click',function(){if(list.querySelectorAll('input').length>=10)return;var input=document.createElement('input');input.className='form-control';input.name='task';input.maxLength=500;input.required=true;input.placeholder='Việc cần bàn giao';input.style.marginTop='8px';list.appendChild(input);});})();</script>
<jsp:include page="../layout/footer.jsp" />
