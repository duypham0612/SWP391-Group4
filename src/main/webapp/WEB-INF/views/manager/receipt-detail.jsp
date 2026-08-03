<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="draft" value="${receipt.status == 'DRAFT'}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Phiếu nhập #${receipt.receiptBatchId}</div>
        <h1>
            <c:choose>
                <c:when test="${receipt.status == 'DRAFT'}"><span class="badge badge-waiting">Nháp</span></c:when>
                <c:when test="${receipt.status == 'CONFIRMED'}"><span class="badge badge-ready">Đã nhập kho</span></c:when>
                <c:otherwise><span class="badge badge-cancelled">Đã huỷ</span></c:otherwise>
            </c:choose>
        </h1>
        <p><c:if test="${not empty receipt.supplierName}">Nhà cung cấp: <c:out value="${receipt.supplierName}"/> · </c:if>Người nhập: <c:out value="${receipt.receivedByName}"/></p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/manager/receipt">← Danh sách phiếu</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:if test="${draft}">
    <%-- Đơn vị là dữ liệu catalog có hệ số; không nhận text tự do để tránh ghi sai tồn. --%>
    <div class="card" style="margin-bottom:18px">
        <h3 style="margin-top:0">Thêm dòng nguyên liệu</h3>
        <form action="${ctx}/manager/receipt" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="addLine">
            <input type="hidden" name="receiptBatchId" value="${receipt.receiptBatchId}">
            <div class="form-group" style="margin:0;flex:1;min-width:200px"><label>Nguyên liệu</label>
                <select id="ingSel" name="ingredientId" class="form-control" required>
                    <option value="">-- Chọn --</option>
                    <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}" data-base-unit="${fn:escapeXml(i.unit)}"><c:out value="${i.name}"/></option></c:forEach>
                </select></div>
            <div class="form-group" style="margin:0;width:140px"><label>Đơn vị nhập</label>
                <select id="unitConversionSel" name="unitConversionId" class="form-control" required disabled>
                    <option value="">-- Chọn --</option>
                    <c:forEach var="i" items="${ingredients}">
                    <c:forEach var="u" items="${unitChoicesByIngredient[i.ingredientId]}">
                        <option value="${u.choiceCode}" data-ingredient="${i.ingredientId}"
                                    data-factor="${u.factorToBase}" data-unit="${fn:escapeXml(u.unitName)}" hidden><c:out value="${u.unitName}"/></option>
                        </c:forEach>
                    </c:forEach>
                </select></div>
            <div class="form-group" style="margin:0;width:130px"><label>Số lượng</label>
                <input id="enteredQty" type="number" name="quantity" class="form-control" min="0.000001" step="0.000001" required></div>
            <div class="form-group" style="margin:0;width:150px"><label>Đơn giá (₫)</label>
                <input type="number" name="unitCost" class="form-control" min="0.01" step="0.01" required></div>
            <button type="submit" class="btn btn-primary">+ Thêm</button>
            <div id="conversionPreview" class="muted" style="flex-basis:100%"></div>
        </form>
        <script>
        (function(){
            var ingredient = document.getElementById('ingSel');
            var conversion = document.getElementById('unitConversionSel');
            var qty = document.getElementById('enteredQty');
            var preview = document.getElementById('conversionPreview');
            function updatePreview(){
                var io = ingredient.options[ingredient.selectedIndex];
                var uo = conversion.options[conversion.selectedIndex];
                var amount = Number(qty.value);
                if (!io || !uo || !uo.value || !Number.isFinite(amount)) { preview.textContent = ''; return; }
                var base = amount * Number(uo.dataset.factor);
                preview.textContent = amount + ' ' + uo.dataset.unit + ' = ' + base.toFixed(3) + ' ' + io.dataset.baseUnit;
            }
            function filterConversions(){
                var ingredientId = ingredient.value;
                conversion.value = '';
                Array.prototype.forEach.call(conversion.options, function(o, index){
                    if (index === 0) return;
                    o.hidden = o.dataset.ingredient !== ingredientId;
                    o.disabled = o.hidden;
                });
                conversion.disabled = !ingredientId;
                var first = Array.prototype.find.call(conversion.options, function(o){ return o.value && !o.hidden; });
                if (first) conversion.value = first.value;
                updatePreview();
            }
            ingredient.addEventListener('change', filterConversions);
            conversion.addEventListener('change', updatePreview);
            qty.addEventListener('input', updatePreview);
        })();
        </script>
    </div>

    <%-- Nhập nhiều nguyên liệu cùng lúc: tick chọn + nhập SL/đơn giá/đơn vị từng dòng --%>
    <div class="card" style="margin-bottom:18px">
        <h3 style="margin-top:0">Chọn nhiều nguyên liệu</h3>
        <p class="muted" style="margin-top:-6px">Tick các nguyên liệu cần nhập, điền số lượng rồi bấm "Thêm các mục đã chọn". (Dòng chưa nhập số lượng sẽ bỏ qua.)</p>
        <form action="${ctx}/manager/receipt" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="addLines">
            <input type="hidden" name="receiptBatchId" value="${receipt.receiptBatchId}">
            <table class="table">
                <thead><tr>
                    <th style="width:40px"><input type="checkbox" onclick="document.querySelectorAll('.pickbox').forEach(c=>c.checked=this.checked)"></th>
                    <th>Nguyên liệu</th><th style="width:150px">Đơn vị nhập</th><th style="width:140px">Số lượng</th><th style="width:160px">Đơn giá / đơn vị nhập (₫)</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="i" items="${ingredients}">
                        <tr>
                            <td><input class="pickbox" type="checkbox" name="pick" value="${i.ingredientId}"></td>
                            <td><c:out value="${i.name}"/></td>
                            <td><select name="unitConversionId_${i.ingredientId}" class="form-control" required>
                    <c:forEach var="u" items="${unitChoicesByIngredient[i.ingredientId]}">
                        <option value="${u.choiceCode}" data-factor="${u.factorToBase}"><c:out value="${u.unitName}"/></option>
                                </c:forEach>
                            </select></td>
                            <td><input type="number" name="qty_${i.ingredientId}" class="form-control" min="0.000001" step="0.000001"></td>
                            <td><input type="number" name="cost_${i.ingredientId}" class="form-control" min="0.01" step="0.01"></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <button type="submit" class="btn btn-primary">+ Thêm các mục đã chọn</button>
        </form>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty details}">
        <div class="card empty-state"><div class="icon">∅</div><p>Phiếu chưa có dòng nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:150px">Số lượng</th><th style="width:150px">Đơn giá</th><th style="width:160px">Thành tiền</th><c:if test="${draft}"><th style="width:90px"></th></c:if></tr></thead>
            <tbody>
                <c:forEach var="d" items="${details}">
                    <tr>
                        <td><c:out value="${d.ingredientName}"/></td>
                        <td>${view.plain(d.enteredQuantity)} <c:out value="${d.unitNameAtEntry}"/><c:if test="${d.factorToBaseAtEntry != 1}"><div class="muted">= ${view.plain(d.baseQuantity)} <c:out value="${d.ingredientUnit}"/></div></c:if></td>
                        <td>${view.grouped(d.unitCost)} ₫</td>
                        <td>${view.grouped(d.lineCost)} ₫</td>
                        <c:if test="${draft}">
                            <td>
                                <form action="${ctx}/manager/receipt" method="post" style="display:inline" onsubmit="return confirm('Xoá dòng?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="removeLine">
                                    <input type="hidden" name="receiptBatchId" value="${receipt.receiptBatchId}">
                                    <input type="hidden" name="lineId" value="${d.stockReceiptLineId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                                </form>
                            </td>
                        </c:if>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<c:if test="${receipt.status == 'CONFIRMED'}">
    <div class="alert alert-success" style="margin-top:18px">Đã nhập kho · Tổng tiền: <strong>${view.grouped(receipt.totalCost)} ₫</strong>. Tồn đã được cộng qua sổ cái.</div>
</c:if>

<c:if test="${draft}">
    <div style="display:flex;gap:10px;margin-top:18px">
        <form action="${ctx}/manager/receipt" method="post" onsubmit="return confirm('Xác nhận nhập kho? Tồn sẽ được cộng và không thể sửa.');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="confirm">
            <input type="hidden" name="receiptBatchId" value="${receipt.receiptBatchId}">
            <button type="submit" class="btn btn-primary btn-lg" <c:if test="${empty details}">disabled</c:if>>Xác nhận nhập kho</button>
        </form>
        <form action="${ctx}/manager/receipt" method="post" onsubmit="return confirm('Huỷ phiếu này?');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="cancel">
            <input type="hidden" name="receiptBatchId" value="${receipt.receiptBatchId}">
            <button type="submit" class="btn btn-ghost btn-lg">Huỷ phiếu</button>
        </form>
    </div>
</c:if>

<jsp:include page="../layout/footer.jsp" />
