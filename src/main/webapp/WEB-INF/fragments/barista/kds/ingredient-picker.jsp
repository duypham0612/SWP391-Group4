<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Danh sách nguyên liệu của món, nạp theo yêu cầu khi mở modal "Hết nguyên liệu". --%>
<c:choose>
    <c:when test="${empty recipeLines}">
        <p class="kds-modal__hint">Món này chưa khai báo công thức nên không chọn được nguyên liệu.
            Hãy chọn lý do khác hoặc báo Quản lý bổ sung công thức.</p>
    </c:when>
    <c:otherwise>
        <p class="kds-modal__hint">Tick nguyên liệu đã hết. Tồn của những nguyên liệu này sẽ được ghi về 0 trong sổ kho.</p>
        <c:forEach var="line" items="${recipeLines}">
            <label class="kds-ing">
                <input type="checkbox" name="ingredientId" value="${line.ingredientId}">
                <span><c:out value="${line.ingredientName}" /></span>
                <%-- stripTrailingZeros: DB lưu DECIMAL nên 18 ra "18.000", mà trong tiếng Việt
                     dấu chấm là phân cách nghìn → dễ đọc nhầm thành 18 nghìn gam. --%>
                <span class="muted">${line.quantity.stripTrailingZeros().toPlainString()} <c:out value="${line.ingredientUnit}" /> / phần</span>
            </label>
        </c:forEach>
    </c:otherwise>
</c:choose>
