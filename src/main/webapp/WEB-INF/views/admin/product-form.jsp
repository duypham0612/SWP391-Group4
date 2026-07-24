<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${product.productId > 0}">Sửa sản phẩm</c:when><c:otherwise>Thêm sản phẩm</c:otherwise></c:choose></h1><p>Cập nhật món, giá bán, size và hình ảnh.</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/product">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/product" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="productId" value="${product.productId}">

        <div class="form-group">
            <label for="name">Tên sản phẩm *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="150" value="${product.name}" required autofocus>
        </div>
        <div class="form-group">
            <label for="categoryId">Danh mục *</label>
            <select id="categoryId" name="categoryId" class="form-control" required>
                <option value="">-- Chọn danh mục --</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" <c:if test="${cat.categoryId == product.categoryId}">selected</c:if>>${cat.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="basePrice">Giá gốc (₫) *</label>
            <input id="basePrice" type="text" name="basePrice" class="form-control" value="${product.basePrice}" data-money-input required>
        </div>
        <section style="border:1px solid var(--line);border-radius:var(--radius-sm);margin:0 0 18px;padding:16px">
            <h3 style="margin-top:0">Size</h3>
            <p class="muted" style="margin-top:-6px">Giá gốc áp dụng cho Size S. Size M/L chỉ cộng thêm số tiền bên dưới. Đường và đá được tạo tự động, không tính phí.</p>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px">
                <div class="form-group" style="margin:0">
                    <label for="sizeSDelta">Size S</label>
                    <input id="sizeSDelta" class="form-control" value="+0" disabled>
                </div>
                <div class="form-group" style="margin:0">
                    <label for="sizeMDelta">Size M cộng thêm (₫)</label>
                    <input id="sizeMDelta" type="text" name="sizeMDelta" class="form-control" value="${sizeConfig.sizeMDelta}" data-money-input>
                </div>
                <div class="form-group" style="margin:0">
                    <label for="sizeLDelta">Size L cộng thêm (₫)</label>
                    <input id="sizeLDelta" type="text" name="sizeLDelta" class="form-control" value="${sizeConfig.sizeLDelta}" data-money-input>
                </div>
            </div>
        </section>
        <div class="form-group">
            <label for="prepMinutes">Thời gian pha chuẩn (phút) *</label>
            <input id="prepMinutes" type="number" name="prepMinutes" class="form-control" min="1" step="1"
                   value="${product.prepMinutes}" required>
            <small class="muted">Dùng để tô màu hàng chờ ở Quầy pha chế — món chờ quá mốc này mới báo trễ.</small>
        </div>
        <div class="form-group">
            <label for="imageUrl">Ảnh (URL)</label>
            <input id="imageUrl" type="text" name="imageUrl" class="form-control" maxlength="255" value="${product.imageUrl}"
                   placeholder="/assets/img/products/ten-mon.svg hoặc https://..." oninput="updatePreview()">
            <small class="muted">Dán URL ảnh ngoài (https://…) hoặc đường dẫn ảnh có sẵn trong <code>/assets/img/products/</code>.</small>
            <c:set var="pImg" value="${empty product.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (product.imageUrl.startsWith('http') ? product.imageUrl : ctx.concat(product.imageUrl))}" />
            <img id="imgPreview" class="img-preview" src="${pImg}" alt="Xem trước" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${product.active or product.productId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<script>
  const CTX = '${ctx}', PH = CTX + '/assets/img/products/_placeholder.svg';
  function updatePreview(){
    const v = document.getElementById('imageUrl').value.trim();
    const img = document.getElementById('imgPreview');
    img.src = !v ? PH : (v.startsWith('http') ? v : CTX + v);
  }
</script>

<jsp:include page="../layout/footer.jsp" />
