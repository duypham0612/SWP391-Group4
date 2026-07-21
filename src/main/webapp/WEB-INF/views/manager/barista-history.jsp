<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />
<div class="page-header"><div><div class="eyebrow">Quản lý chi nhánh</div><h1>Lịch sử thao tác pha chế</h1><p>Audit append-only của prep, hao hụt, làm lại ngoài đơn và báo hết món.</p></div></div>
<div class="card">
    <div class="table-toolbar"><input id="auditSearch" class="form-control table-search" type="search" placeholder="Tìm hành động, mã đối tượng, người thực hiện…" aria-label="Tìm lịch sử"></div>
    <div class="table-scroll"><table class="table" id="auditTable"><thead><tr><th>Thời gian</th><th>Hành động</th><th>Đối tượng</th><th>Trước</th><th>Sau</th><th>Lý do</th><th>Người thực hiện</th></tr></thead>
    <tbody><c:forEach var="h" items="${history}"><tr data-audit="${h.actionType} ${h.entityType} ${h.entityId} ${h.performedByName} ${h.reason}"><td>${h.createdAt}</td><td>${h.actionLabel}</td><td>${h.entityLabel} <c:if test="${not empty h.entityId}">#${h.entityId}</c:if></td><td><small>${h.beforeJson}</small></td><td><small>${h.afterJson}</small></td><td>${h.reason}</td><td>${h.performedByName}</td></tr></c:forEach></tbody></table></div>
    <p class="muted" style="margin-bottom:0">${fn:length(history)} sự kiện · dữ liệu không bị xoá khi sửa hoặc huỷ.</p>
</div>
<script>(function(){var i=document.getElementById('auditSearch');var rows=[].slice.call(document.querySelectorAll('#auditTable tbody tr'));if(!i)return;i.addEventListener('input',function(){var q=(i.value||'').toLowerCase();rows.forEach(function(r){r.style.display=(r.getAttribute('data-audit')||'').toLowerCase().indexOf(q)>=0?'':'none';});});})();</script>
<jsp:include page="../layout/footer.jsp" />
