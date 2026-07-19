<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="isNew" value="${group.modifierGroupId == 0}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1><c:choose><c:when test="${isNew}">Thêm nhóm tuỳ chọn</c:when><c:otherwise>Nhóm: ${group.name}</c:otherwise></c:choose></h1>
        <p>Cấu hình nhóm · option · định mức nguyên liệu — tất cả trong một trang</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/admin/modifier">← Danh sách nhóm</a>
</div>

<c:if test="${not empty flashOk}"><div class="alert alert-success">${flashOk}</div></c:if>
<c:if test="${not empty flashError}"><div class="alert alert-error">${flashError}</div></c:if>

<%-- ============ Card 1: cấu hình nhóm ============ --%>
<div class="card form-card" style="margin-bottom:20px">
    <h3 style="margin-top:0">Cấu hình nhóm</h3>
    <form action="${ctx}/admin/modifier" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="saveGroup">
        <input type="hidden" name="modifierGroupId" value="${group.modifierGroupId}">

        <div class="form-group">
            <label for="name">Tên nhóm * <span class="muted">(vd Size, Sữa, Đường, Topping)</span></label>
            <input id="name" type="text" name="name" class="form-control" maxlength="80" value="${group.name}" required autofocus>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="required" value="1" <c:if test="${group.required}">checked</c:if>> Bắt buộc khách phải chọn</label>
        </div>
        <div class="form-row" style="display:flex;gap:16px">
            <div class="form-group" style="flex:1">
                <label for="minSelect">Chọn tối thiểu</label>
                <input id="minSelect" type="number" name="minSelect" class="form-control" min="0" value="${group.minSelect}">
            </div>
            <div class="form-group" style="flex:1">
                <label for="maxSelect">Chọn tối đa</label>
                <input id="maxSelect" type="number" name="maxSelect" class="form-control" min="1" value="${group.maxSelect}">
            </div>
        </div>
        <p class="muted" style="margin:-4px 0 14px;font-size:13px">
            Tối đa ≥ tối thiểu. Nhóm bắt buộc thì tối thiểu ≥ 1 (vd Size: bắt buộc, chọn đúng 1 → min=max=1).
        </p>
        <button type="submit" class="btn btn-primary">Lưu cấu hình</button>
    </form>
</div>

<c:choose>
    <c:when test="${isNew}">
        <div class="alert alert-info">Lưu cấu hình nhóm trước, sau đó bạn có thể thêm các option (Size L, Oat milk, Extra shot…) và định mức nguyên liệu ngay tại đây.</div>
    </c:when>
    <c:otherwise>
        <%-- ============ Card 2: danh sách option + định mức inline ============ --%>
        <div class="page-header" style="margin-top:8px;margin-bottom:12px">
            <div><h2 style="margin:0">Option của nhóm</h2></div>
        </div>

        <c:if test="${empty options}">
            <div class="card empty-state" style="margin-bottom:16px"><div class="icon">🧃</div><p>Nhóm chưa có option nào. Thêm option đầu tiên bên dưới.</p></div>
        </c:if>

        <c:forEach var="o" items="${options}">
            <c:set var="impacts" value="${impactsByOption[o.modifierOptionId]}" />
            <div class="card opt-card" id="opt-${o.modifierOptionId}" style="margin-bottom:14px">
                <%-- dòng sửa inline option --%>
                <form action="${ctx}/admin/modifier" method="post" class="opt-edit">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="updateOption">
                    <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                    <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                    <div class="opt-edit__name">
                        <label class="muted">Tên option</label>
                        <input type="text" name="name" class="form-control" value="${o.name}" maxlength="80" required>
                    </div>
                    <div class="opt-edit__price">
                        <label class="muted">Phụ thu (₫)</label>
                        <input type="number" name="priceDelta" class="form-control" step="500" value="${o.priceDelta}">
                    </div>
                    <label class="opt-edit__active"><input type="checkbox" name="active" ${o.active ? 'checked' : ''}> Bật</label>
                    <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                </form>
                <form action="${ctx}/admin/modifier" method="post" class="opt-del"
                      onsubmit="return confirm('Xoá option này (và định mức của nó)?');">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="deleteOption">
                    <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                    <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                    <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Xoá option</button>
                </form>

                <%-- tóm tắt định mức dạng chip --%>
                <div class="opt-impacts">
                    <c:choose>
                        <c:when test="${empty impacts}"><span class="muted" style="font-size:13px">Chưa có định mức nguyên liệu</span></c:when>
                        <c:otherwise>
                            <c:forEach var="m" items="${impacts}">
                                <span class="chip">${m.ingredientName} ${m.qtyDelta > 0 ? '+' : ''}${m.qtyDelta}${m.ingredientUnit}</span>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- panel gập: sửa định mức tại chỗ --%>
                <details class="opt-detail">
                    <summary>Định mức nguyên liệu (khi Barista bấm "Đã pha xong" sẽ cộng/trừ theo đây)</summary>
                    <div style="padding-top:12px">
                        <c:if test="${not empty impacts}">
                            <table class="table" style="margin-bottom:12px">
                                <thead><tr><th>Nguyên liệu</th><th style="width:110px">Loại</th><th style="width:150px">Lượng +/−</th><th style="width:80px"></th></tr></thead>
                                <tbody>
                                    <c:forEach var="m" items="${impacts}">
                                        <tr>
                                            <td>${m.ingredientName}</td>
                                            <td><c:choose><c:when test="${m.ingredientType == 'RAW'}"><span class="badge badge-making">Thô</span></c:when><c:otherwise><span class="badge badge-ready">Pha sẵn</span></c:otherwise></c:choose></td>
                                            <td>${m.qtyDelta > 0 ? '+' : ''}${m.qtyDelta} ${m.ingredientUnit}</td>
                                            <td>
                                                <form action="${ctx}/admin/modifier" method="post" onsubmit="return confirm('Xoá dòng định mức này?');">
                                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="deleteImpact">
                                                    <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                                                    <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                                                    <input type="hidden" name="impactId" value="${m.impactId}">
                                                    <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:if>
                        <form action="${ctx}/admin/modifier" method="post" style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="addImpact">
                            <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                            <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                            <div class="form-group" style="margin:0;flex:1;min-width:220px">
                                <label class="muted">Nguyên liệu</label>
                                <select name="ingredientId" class="form-control" required>
                                    <option value="">— Chọn nguyên liệu —</option>
                                    <c:forEach var="i" items="${ingredients}">
                                        <option value="${i.ingredientId}">${i.name} (${i.unit} · ${i.ingredientType})</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="form-group" style="margin:0;width:160px">
                                <label class="muted">Lượng +/− (vd 18 hoặc −200)</label>
                                <input type="number" name="qtyDelta" class="form-control" step="0.001" required>
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm">+ Thêm định mức</button>
                        </form>
                    </div>
                </details>
            </div>
        </c:forEach>

        <%-- thêm option mới --%>
        <div class="card" id="add-option" style="border-style:dashed">
            <h3 style="margin-top:0">Thêm option</h3>
            <form action="${ctx}/admin/modifier" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="addOption">
                <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                <div class="form-group" style="margin:0;flex:1;min-width:220px">
                    <label for="newOptName">Tên option * <span class="muted">(vd Size L, Oat milk, Extra shot)</span></label>
                    <input id="newOptName" type="text" name="name" class="form-control" maxlength="80" required>
                </div>
                <div class="form-group" style="margin:0;width:180px">
                    <label for="newOptPrice">Phụ thu (₫)</label>
                    <input id="newOptPrice" type="number" name="priceDelta" class="form-control" step="500" value="0">
                </div>
                <button type="submit" class="btn btn-primary">+ Thêm option</button>
            </form>
        </div>
    </c:otherwise>
</c:choose>

<style>
.opt-card{display:flex;flex-wrap:wrap;align-items:center;gap:10px 16px}
.opt-edit{display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap;flex:1;min-width:0}
.opt-edit__name{flex:1;min-width:180px}
.opt-edit__name input,.opt-edit__price input{margin:0}
.opt-edit__price{width:140px}
.opt-edit .muted{display:block;font-size:12px;margin-bottom:2px}
.opt-edit__active{display:inline-flex;gap:5px;align-items:center;white-space:nowrap;padding-bottom:8px}
.opt-del{margin-left:auto}
.opt-impacts{flex-basis:100%;display:flex;flex-wrap:wrap;gap:6px;align-items:center}
.opt-detail{flex-basis:100%;border-top:1px solid var(--line);padding-top:10px}
.opt-detail summary{cursor:pointer;font-weight:600;color:var(--brand-700);font-size:13.5px}
</style>

<jsp:include page="../layout/footer.jsp" />
