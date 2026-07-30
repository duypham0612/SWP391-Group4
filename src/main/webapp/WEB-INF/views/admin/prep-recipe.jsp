<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1>Công thức pha sẵn: ${prepped.name}</h1>
        <p>Nguyên liệu và định mức cho một ${prepped.unit} thành phẩm.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/admin/recipe">← Chọn nguyên liệu khác</a>
</div>

<c:if test="${not empty errorMsg}">
    <div class="alert alert-error"><c:out value="${errorMsg}"/></div>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><c:out value="${sessionScope.flashError}"/></div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success"><c:out value="${sessionScope.flashOk}"/></div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty rawIngredients}">
        <div class="alert alert-info">Tất cả nguyên liệu thô đang hoạt động đã có trong công thức.</div>
    </c:when>
    <c:otherwise>
        <div class="card recipe-compose" style="margin-bottom:18px">
            <div class="recipe-compose__header">
                <h3>Thêm nguyên liệu</h3>
                <button type="button" class="btn btn-ghost btn-sm" id="addPrepRecipeRow">+ Thêm dòng</button>
            </div>
            <form action="${ctx}/admin/recipe" method="post" id="prepRecipeBatchForm">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="addPrepLines">
                <input type="hidden" name="preppedId" value="${prepped.ingredientId}">
                <div class="table-wrap">
                    <table class="table recipe-draft-table">
                        <thead>
                            <tr>
                                <th>Nguyên liệu thô</th>
                                <th style="width:180px">Định mức</th>
                                <th style="width:120px">Đơn vị</th>
                                <th style="width:90px">Xóa</th>
                            </tr>
                        </thead>
                        <tbody id="prepRecipeDraftRows">
                            <tr class="recipe-draft-row">
                                <td>
                                    <select name="ingredientId" class="form-control recipe-ingredient" required>
                                        <option value="">-- Chọn nguyên liệu --</option>
                                        <c:forEach var="i" items="${rawIngredients}">
                                            <option value="${i.ingredientId}" data-unit="${i.unit}">
                                                ${i.name}
                                            </option>
                                        </c:forEach>
                                    </select>
                                </td>
                                <td>
                                    <input type="number" name="quantity" class="form-control"
                                           min="2" max="999999999" step="1" required>
                                </td>
                                <td class="recipe-unit muted">—</td>
                                <td>
                                    <button type="button"
                                            class="btn btn-ghost btn-sm recipe-remove">Xóa</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div class="recipe-compose__actions">
                    <button type="submit" class="btn btn-primary">Lưu nguyên liệu</button>
                </div>
            </form>
        </div>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${empty prepLines}">
        <div class="card empty-state"><p>Công thức chưa có nguyên liệu nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead>
                <tr>
                    <th>Nguyên liệu thô</th>
                    <th style="width:300px">Định mức</th>
                    <th style="width:90px">Xóa</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="line" items="${prepLines}">
                    <tr>
                        <td>${line.rawIngredientName}</td>
                        <td>
                            <form action="${ctx}/admin/recipe" method="post"
                                  style="display:inline-flex;gap:6px;align-items:center">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="updatePrepLine">
                                <input type="hidden" name="preppedId" value="${prepped.ingredientId}">
                                <input type="hidden" name="lineId" value="${line.prepRecipeId}">
                                <input type="number" name="quantity" class="form-control"
                                       style="width:120px" min="2" max="999999999"
                                       step="1" value="${line.quantityIntegerDisplay}" required>
                                <span class="muted">${line.rawIngredientUnit}/${prepped.unit}</span>
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                            </form>
                        </td>
                        <td>
                            <form action="${ctx}/admin/recipe" method="post"
                                  onsubmit="return confirm('Xoá dòng này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deletePrepLine">
                                <input type="hidden" name="preppedId" value="${prepped.ingredientId}">
                                <input type="hidden" name="lineId" value="${line.prepRecipeId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Xóa</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<script>
(function () {
    var rows = document.getElementById('prepRecipeDraftRows');
    var addButton = document.getElementById('addPrepRecipeRow');
    if (!rows || !addButton) return;

    function updateRows() {
        var selects = Array.prototype.slice.call(rows.querySelectorAll('.recipe-ingredient'));
        var selected = selects.map(function (select) { return select.value; }).filter(Boolean);

        selects.forEach(function (select) {
            Array.prototype.forEach.call(select.options, function (option) {
                option.disabled = option.value !== '' &&
                        option.value !== select.value &&
                        selected.indexOf(option.value) >= 0;
            });
            var unit = select.options[select.selectedIndex];
            select.closest('tr').querySelector('.recipe-unit').textContent =
                    unit && unit.dataset.unit ? unit.dataset.unit + '/${prepped.unit}' : '—';
        });

        var removeButtons = rows.querySelectorAll('.recipe-remove');
        Array.prototype.forEach.call(removeButtons, function (button) {
            button.disabled = removeButtons.length === 1;
        });
        addButton.disabled = selects.length >= selects[0].options.length - 1;
    }

    addButton.addEventListener('click', function () {
        var template = rows.querySelector('.recipe-draft-row');
        var row = template.cloneNode(true);
        row.querySelector('.recipe-ingredient').value = '';
        row.querySelector('input[name="quantity"]').value = '';
        rows.appendChild(row);
        updateRows();
    });

    rows.addEventListener('change', function (event) {
        if (event.target.classList.contains('recipe-ingredient')) updateRows();
    });
    rows.addEventListener('click', function (event) {
        if (!event.target.classList.contains('recipe-remove')) return;
        event.target.closest('tr').remove();
        updateRows();
    });
    updateRows();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
