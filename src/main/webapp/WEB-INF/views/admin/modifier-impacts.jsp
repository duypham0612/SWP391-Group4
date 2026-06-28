<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Định mức theo option: ${option.name}</h1><p>catalog.ModifierIngredientImpact · QtyDelta (+ thêm / − bớt)</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/modifier?view=options&groupId=${option.modifierGroupId}">← Quay lại option</a>
</div>

<div class="alert alert-info">
    Ví dụ "Extra shot" → +18g cà phê. "Oat milk" → −200ml sữa bò, +200ml oat. QtyDelta sẽ cộng vào định mức khi Barista bấm "Xong" (Phase 4).
</div>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Thêm ảnh hưởng nguyên liệu</h3>
    <form action="${ctx}/admin/modifier" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="addImpact">
        <input type="hidden" name="optionId" value="${option.modifierOptionId}">
        <div class="form-group" style="margin:0;flex:1;min-width:240px">
            <label for="ingredientId">Nguyên liệu</label>
            <select id="ingredientId" name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}">
                    <option value="${i.ingredientId}">${i.name} (${i.unit} · ${i.ingredientType})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group" style="margin:0;width:180px">
            <label for="qtyDelta">QtyDelta (+/−)</label>
            <input id="qtyDelta" type="number" name="qtyDelta" class="form-control" step="0.001" required>
        </div>
        <button type="submit" class="btn btn-primary">+ Thêm</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty impacts}">
        <div class="card empty-state"><div class="icon">⚖️</div><p>Option này chưa có ảnh hưởng nguyên liệu.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:120px">Loại</th><th style="width:160px">QtyDelta</th><th style="width:100px">Xoá</th></tr></thead>
            <tbody>
                <c:forEach var="m" items="${impacts}">
                    <tr>
                        <td>${m.ingredientName}</td>
                        <td><c:choose><c:when test="${m.ingredientType == 'RAW'}"><span class="badge badge-making">RAW</span></c:when><c:otherwise><span class="badge badge-ready">PREPPED</span></c:otherwise></c:choose></td>
                        <td>${m.qtyDelta} ${m.ingredientUnit}</td>
                        <td>
                            <form action="${ctx}/admin/modifier" method="post" style="display:inline" onsubmit="return confirm('Xoá dòng này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deleteImpact">
                                <input type="hidden" name="optionId" value="${option.modifierOptionId}">
                                <input type="hidden" name="impactId" value="${m.impactId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
