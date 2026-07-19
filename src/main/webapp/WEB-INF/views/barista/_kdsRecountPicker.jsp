<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Danh sách nguyên liệu đang cạn của món, nạp theo yêu cầu khi mở modal bỏ chặn. --%>
<c:choose>
    <c:when test="${empty depletedLines}">
        <p class="kds-modal__hint">Không có nguyên liệu đang cạn; xác nhận để trả món về hàng chờ.</p>
    </c:when>
    <c:otherwise>
        <p class="kds-modal__hint">Nhập tồn thực tế sau khi kiểm lại. Để trống nguyên liệu không kiểm kê; nhập 0 nếu đã đếm và vẫn hết.</p>
        <c:forEach var="line" items="${depletedLines}">
            <label class="kds-recount">
                <input type="hidden" name="ingredientId" value="${line.ingredientId}">
                <span class="kds-recount__name"><c:out value="${line.ingredientName}" /></span>
                <span class="kds-recount__current">Hiện có ${line.branchQuantityOnHand.stripTrailingZeros().toPlainString()} <c:out value="${line.ingredientUnit}" /></span>
                <input type="number" name="actualQty" min="0" step="0.001" inputmode="decimal" placeholder="Tồn thực tế">
            </label>
        </c:forEach>
    </c:otherwise>
</c:choose>
