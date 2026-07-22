<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />
<script src="${ctx}/assets/js/qrcode.min.js"></script>

<style>
  .floor-actions{display:flex;gap:10px;flex-wrap:wrap}
  .table-admin-panel{margin-bottom:18px}
  .table-admin-grid{display:grid;grid-template-columns:1fr 1.2fr;gap:18px}
  .table-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px}
  .table-card{position:relative;display:flex;flex-direction:column;gap:12px;min-height:260px}
  .table-card.is-hidden{opacity:.65;border-style:dashed}
  .table-card-head{display:flex;justify-content:space-between;gap:10px;align-items:flex-start}
  .table-capacity{display:inline-flex;align-items:center;gap:5px;font-size:.88rem;color:var(--muted)}
  .table-card-main{display:grid;grid-template-columns:1fr 126px;gap:12px;align-items:start}
  .table-qr-mini{background:#fff;border:1px solid #dfe5ef;border-radius:14px;padding:8px;text-align:center;box-shadow:0 6px 18px rgba(30,45,70,.08)}
  .table-qr-code{display:flex;justify-content:center;min-height:108px}
  .table-qr-code img,.table-qr-code canvas{width:108px!important;height:108px!important}
  .table-qr-label{font-size:.7rem;line-height:1.3;color:#30405c;margin-top:5px;word-break:break-all}
  .table-card-actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:auto}
  .table-edit{border-top:1px dashed var(--line);padding-top:10px}
  .table-edit summary{cursor:pointer;color:var(--brand);font-weight:700;font-size:.88rem}
  .table-edit-row{display:grid;grid-template-columns:1fr 90px auto;gap:7px;margin-top:9px;align-items:end}
  @media(max-width:760px){.table-admin-grid{grid-template-columns:1fr}.table-card-main{grid-template-columns:1fr 116px}}
</style>

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Sơ đồ bàn</h1>
        <p>Quản lý bàn, sức chứa, QR tại bàn và nhóm bàn ghép của chi nhánh</p></div>
    <div class="floor-actions">
        <a class="btn btn-ghost" href="${ctx}/cashier/table?showHidden=${showHidden ? '0' : '1'}">
            ${showHidden ? 'Ẩn bàn ngừng dùng' : 'Hiện cả bàn đã ẩn'}
        </a>
        <a class="btn btn-ghost" href="${ctx}/cashier/pos">POS đem về</a>
    </div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<details class="card table-admin-panel" open>
    <summary style="cursor:pointer;font-weight:800">Quản lý và ghép bàn</summary>
    <div class="table-admin-grid" style="margin-top:16px">
        <form action="${ctx}/cashier/table" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="saveTable">
            <h3 style="margin:0 0 10px">Thêm bàn</h3>
            <div style="display:flex;gap:8px;align-items:end;flex-wrap:wrap">
                <div class="form-group" style="margin:0;flex:1;min-width:150px"><label>Tên bàn</label>
                    <input class="form-control" name="tableNumber" maxlength="20" placeholder="Bàn 05" required></div>
                <div class="form-group" style="margin:0;width:120px"><label>Sức chứa</label>
                    <input class="form-control" type="number" name="capacity" min="1" max="30" value="4" required></div>
                <button class="btn btn-primary" type="submit">Thêm bàn</button>
            </div>
        </form>

        <form action="${ctx}/cashier/table" method="post"
              onsubmit="return this.sourceTableId.value!==this.destinationTableId.value || (alert('Hãy chọn hai bàn khác nhau.'),false)">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="mergeTables">
            <h3 style="margin:0 0 10px">Ghép bàn</h3>
            <div style="display:flex;gap:8px;align-items:end;flex-wrap:wrap">
                <div class="form-group" style="margin:0;flex:1;min-width:140px"><label>Bàn ghép thêm</label>
                    <select class="form-control" name="sourceTableId" required>
                        <option value="">Chọn bàn</option>
                        <c:forEach var="t" items="${tables}"><c:if test="${t.visible and not t.merged}">
                            <option value="${t.diningTableId}">${t.tableNumber} · ${t.capacity} người</option>
                        </c:if></c:forEach>
                    </select></div>
                <div class="form-group" style="margin:0;flex:1;min-width:140px"><label>Ghép vào bàn</label>
                    <select class="form-control" name="destinationTableId" required>
                        <option value="">Chọn bàn đích</option>
                        <c:forEach var="t" items="${tables}"><c:if test="${t.visible and not t.merged}">
                            <option value="${t.diningTableId}">${t.tableNumber} · ${t.effectiveCapacity} người</option>
                        </c:if></c:forEach>
                    </select></div>
                <button class="btn btn-primary" type="submit">Ghép bàn</button>
            </div>
        </form>
    </div>
</details>

<div class="table-toolbar">
    <div class="table-search"><input id="tableSearch" class="form-control" type="search" placeholder="Tìm tên bàn..." autocomplete="off"></div>
</div>

<div class="table-grid">
    <c:forEach var="t" items="${tables}">
        <c:set var="tblClass" value="tbl-empty" />
        <c:set var="tblLabel" value="Trống" />
        <c:set var="tblBadge" value="badge-served" />
        <c:if test="${t.status == 'CLEANING'}"><c:set var="tblLabel" value="Đang dọn"/><c:set var="tblBadge" value="badge-waiting"/></c:if>
        <c:if test="${not empty t.activeSessionId}"><c:set var="tblClass" value="tbl-draft"/><c:set var="tblLabel" value="Đã mở"/><c:set var="tblBadge" value="badge-waiting"/></c:if>
        <c:if test="${not empty t.activeSessionId and t.activeItemCount > 0}"><c:set var="tblClass" value="tbl-busy"/><c:set var="tblLabel" value="Đang phục vụ"/><c:set var="tblBadge" value="badge-ready"/></c:if>
        <c:if test="${t.merged}"><c:set var="tblLabel" value="Ghép → ${t.mergedIntoTableNumber}"/><c:set var="tblBadge" value="badge-making"/></c:if>
        <c:url var="qrPath" value="/qr/menu"><c:param name="t" value="${t.qrCode}"/></c:url>

        <div class="card table-card ${tblClass} ${t.visible ? '' : 'is-hidden'}" data-name="${t.tableNumber}">
            <div class="table-card-head">
                <div><strong style="font-size:1.15rem">${t.tableNumber}</strong>
                    <div class="table-capacity">👥 ${t.effectiveCapacity} người<c:if test="${t.effectiveCapacity != t.capacity}"> (${t.capacity} + bàn ghép)</c:if></div></div>
                <div style="text-align:right"><span class="badge ${tblBadge}">${tblLabel}</span>
                    <c:if test="${not t.visible}"><div style="margin-top:5px"><span class="badge badge-cancelled">Đã ẩn</span></div></c:if></div>
            </div>

            <div class="table-card-main">
                <div>
                    <c:choose>
                        <c:when test="${t.merged}"><p class="muted">Dùng chung phiên và đơn với <strong>${t.mergedIntoTableNumber}</strong>.</p></c:when>
                        <c:when test="${not empty t.activeSessionId}"><p class="muted">${t.activeItemCount} món · phiên #${t.activeSessionId}</p></c:when>
                        <c:otherwise><p class="muted">Sẵn sàng đón tối đa ${t.capacity} khách.</p></c:otherwise>
                    </c:choose>
                    <div class="table-card-actions">
                        <c:choose>
                            <c:when test="${not t.visible}"></c:when>
                            <c:when test="${not empty t.activeSessionId}"><a class="btn btn-primary btn-sm" href="${ctx}/cashier/pos?sessionId=${t.activeSessionId}">Vào POS</a></c:when>
                            <c:when test="${t.status != 'CLEANING'}">
                                <form action="${ctx}/cashier/table" method="post">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="openTable"><input type="hidden" name="tableId" value="${t.diningTableId}">
                                    <button type="submit" class="btn btn-primary btn-sm">Mở bàn</button>
                                </form>
                            </c:when>
                        </c:choose>
                        <c:if test="${t.merged}">
                            <form action="${ctx}/cashier/table" method="post">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="unmergeTable"><input type="hidden" name="tableId" value="${t.diningTableId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Tách bàn</button>
                            </form>
                        </c:if>
                    </div>
                </div>
                <c:if test="${t.visible}">
                    <div class="table-qr-mini">
                        <div class="table-qr-code" data-url="${qrPath}"></div>
                        <div class="table-qr-label"><strong>${t.tableNumber}</strong><br>${t.qrCode}</div>
                    </div>
                </c:if>
            </div>

            <details class="table-edit">
                <summary>Chỉnh sửa bàn</summary>
                <form action="${ctx}/cashier/table" method="post" class="table-edit-row">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="saveTable"><input type="hidden" name="tableId" value="${t.diningTableId}">
                    <div><label class="muted">Tên bàn</label><input class="form-control" name="tableNumber" maxlength="20" value="${t.tableNumber}" required></div>
                    <div><label class="muted">Sức chứa</label><input class="form-control" type="number" name="capacity" min="1" max="30" value="${t.capacity}" required></div>
                    <button class="btn btn-ghost btn-sm" type="submit">Lưu</button>
                </form>
                <form action="${ctx}/cashier/table" method="post" style="margin-top:8px">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="setVisibility"><input type="hidden" name="tableId" value="${t.diningTableId}"><input type="hidden" name="visible" value="${not t.visible}">
                    <button class="btn btn-ghost btn-sm" type="submit">${t.visible ? 'Ẩn bàn' : 'Hiện lại bàn'}</button>
                </form>
            </details>
        </div>
    </c:forEach>
</div>

<div id="tableNoMatch" class="card empty-state" style="display:none;margin-top:16px"><div class="icon">∅</div><p>Không tìm thấy bàn phù hợp.</p></div>
<c:if test="${empty tables}"><div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có bàn nào.</p></div></c:if>

<script>
document.querySelectorAll('.table-qr-code').forEach(box=>{
  const absolute=new URL(box.dataset.url,window.location.href).href;
  new QRCode(box,{text:absolute,width:108,height:108,correctLevel:QRCode.CorrectLevel.M});
});
document.getElementById('tableSearch').addEventListener('input',function(){
  const q=this.value.trim().toLowerCase();let shown=0;
  document.querySelectorAll('.table-card').forEach(card=>{const ok=!q||(card.dataset.name||'').toLowerCase().includes(q);card.style.display=ok?'':'none';if(ok)shown++;});
  document.getElementById('tableNoMatch').style.display=shown===0?'':'none';
});
</script>

<jsp:include page="../layout/footer.jsp" />
