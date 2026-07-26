<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request" />

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success" role="status"><c:out value="${sessionScope.flashOk}" /></div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error" role="alert"><c:out value="${sessionScope.flashError}" /></div>
    <c:remove var="flashError" scope="session" />
</c:if>

<%-- Cao điểm đo bằng SỐ LY đang dồn, không dính đồng hồ. Việc cần làm khi đông vẫn là pha theo
     đúng thứ tự trong danh sách, nên dòng này chỉ nhắc đúng một điều đó. --%>
<c:if test="${peakMode}">
    <div class="kds-peak" role="status">
        <strong>Cao điểm</strong>
        <span>${peakQueueCups} ly đang dồn · pha lần lượt theo số thứ tự đầu dòng</span>
    </div>
</c:if>

<%-- Dải trạng thái: chỉ đếm khối lượng việc theo số ly, không có số liệu thời gian nào. --%>
<section class="kds-summary" aria-label="Trạng thái quầy pha chế">
    <div class="kds-stat"><span class="kds-stat__label">Chờ pha</span><strong class="kds-stat__num">${waitingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Đang pha</span><strong class="kds-stat__num kds-stat__num--making">${makingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Sẵn sàng</span><strong class="kds-stat__num kds-stat__num--ready">${readyCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Cần xử lý</span><strong class="kds-stat__num">${blockedCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat kds-stat--wide">
        <span class="kds-stat__label">Đơn đang mở</span><strong class="kds-stat__num">${openOrderCount}</strong>
        <span class="kds-stat__context"><c:choose><c:when test="${waitingCount + makingCount gt 0}">${waitingCount + makingCount} ly còn phải pha</c:when><c:otherwise>Quầy đang thông thoáng</c:otherwise></c:choose></span>
    </div>
</section>

<%-- Danh sách MỘT CỘT theo đúng thứ tự pha: món làm lại lên đầu, còn lại FIFO theo giờ đặt,
     món đã pha xong dồn xuống cuối. Bàn là một cột ngay trên dòng nên không còn cần danh sách
     bàn riêng bên trái — barista đọc từ trên xuống là ra việc tiếp theo. --%>
<%-- Trang và bộ lọc mà máy chủ VỪA áp. kds-board.js đọc lại sau mỗi lần thay board để biết
     đang đứng ở trang nào (làm mới / thao tác xong phải ở lại đúng trang đó). --%>
<span id="kdsQueueState" hidden
      data-page="${queuePage.page}" data-total-pages="${queuePage.totalPages}"
      data-owner="${filterOwner}" data-station="${filterStation}" data-order-type="${filterOrderType}"></span>

<c:choose>
    <c:when test="${queueTotal == 0}">
        <div class="kds-tables-empty"><span>✓</span> Quầy đang thông thoáng — chưa có món nào cần pha.</div>
    </c:when>
    <c:when test="${queuePage.total == 0}">
        <div class="kds-filter-empty">Không có món phù hợp bộ lọc — quầy vẫn còn ${queueTotal} món.</div>
    </c:when>
    <c:otherwise>
        <div class="kds-queue" id="kdsQueue">
            <div class="kds-queue__head" aria-hidden="true">
                <span class="kds-qrow__seq">#</span>
                <span class="kds-qrow__qty">SL</span>
                <span class="kds-qrow__name">Món</span>
                <span class="kds-qrow__table">Bàn</span>
                <span class="kds-qrow__code">Đơn</span>
                <span class="kds-qrow__state">Trạng thái</span>
                <span class="kds-qrow__act"></span>
            </div>
            <c:forEach var="item" items="${queuePage.items}">
                <c:set var="cardItem" value="${item}" scope="request" />
  <jsp:include page="_queue-row.jsp" />
            </c:forEach>
        </div>

        <%-- Chân bảng: bên trái là phạm vi đang xem, bên phải là pager. Giá trị bộ lọc đã qua
             whitelist ở servlet nên ghép thẳng vào URL là an toàn. Pager vẫn là link thật để
             chạy được cả khi JS hỏng; kds-board.js chặn click và nạp trang bằng AJAX. --%>
        <c:set var="kdsPagerBase"
               value="${ctx}/barista/kds?owner=${filterOwner}&amp;station=${filterStation}&amp;orderType=${filterOrderType}&amp;page=" />
        <div class="kds-queue__foot" id="kdsQueueFoot">
            <p class="kds-queue__range">
                Đang xem <strong>${queuePage.startRow}–${queuePage.endRow}</strong> / ${queuePage.total} món<c:if test="${queuePage.total lt queueTotal}"> đã lọc (tổng ${queueTotal})</c:if>
                <c:if test="${queuePage.totalPages gt 1}"> · Trang ${queuePage.page}/${queuePage.totalPages}</c:if>
            </p>
            <c:if test="${queuePage.totalPages gt 1}">
                <nav class="pagination" aria-label="Phân trang hàng chờ pha chế">
                    <a class="page" href="${kdsPagerBase}1" data-page="1"
                       aria-disabled="${not queuePage.hasPrevious}" title="Trang đầu">«</a>
                    <a class="page" href="${kdsPagerBase}${queuePage.page - 1}" data-page="${queuePage.page - 1}"
                       aria-disabled="${not queuePage.hasPrevious}" title="Trang trước">‹</a>
                    <c:forEach var="pageNumber" items="${queuePage.visiblePages}">
                        <a class="page ${pageNumber == queuePage.page ? 'is-active' : ''}"
                           href="${kdsPagerBase}${pageNumber}" data-page="${pageNumber}"
                           aria-current="${pageNumber == queuePage.page ? 'page' : 'false'}">${pageNumber}</a>
                    </c:forEach>
                    <a class="page" href="${kdsPagerBase}${queuePage.page + 1}" data-page="${queuePage.page + 1}"
                       aria-disabled="${not queuePage.hasNext}" title="Trang sau">›</a>
                    <a class="page" href="${kdsPagerBase}${queuePage.totalPages}" data-page="${queuePage.totalPages}"
                       aria-disabled="${not queuePage.hasNext}" title="Trang cuối">»</a>
                </nav>
            </c:if>
        </div>
    </c:otherwise>
</c:choose>

<%-- Món dang dở từ ngày kinh doanh TRƯỚC KHÔNG còn hiện ở đây. Quán đã đóng cửa trước mốc cắt
     ngày kinh doanh nhiều giờ nên khách của những ly đó đã về: việc đúng là Thu ngân huỷ &
     hoàn tiền, không phải pha nốt — mà "pha nốt" ở màn này còn trừ kho thật cho ly không ai uống.
     Chỗ xử lý là màn Đơn đến của Thu ngân. --%>
