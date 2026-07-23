<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="wideLayout" value="true" scope="request" />
<c:set var="bodyClass" value="page-kds" scope="request" />
<jsp:include page="../layout/header.jsp" />
<h1 class="visually-hidden">Quầy pha chế</h1>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div class="kds-toolbar">
    <div class="kds-filters" id="kdsOwnerFilters" role="group" aria-label="Lọc theo người phụ trách">
        <button type="button" class="chip-filter is-active" data-filter-group="owner" data-filter-value="all" aria-pressed="true">Tất cả món</button>
        <button type="button" class="chip-filter" data-filter-group="owner" data-filter-value="mine" aria-pressed="false">Món của tôi</button>
        <button type="button" class="chip-filter" data-filter-group="owner" data-filter-value="unassigned" aria-pressed="false">Chưa nhận</button>
    </div>
    <button type="button" class="chip-filter chip-filter--urgent" id="kdsUrgencyFilter" data-filter-group="urgency" data-filter-value="late" aria-pressed="false">Trễ giờ</button>
    <details class="kds-more" id="kdsMoreFilters">
        <summary class="chip-filter">Quầy &amp; loại đơn <span class="kds-filter-badge" id="kdsFilterBadge" hidden></span></summary>
        <div class="kds-more__panel">
            <label class="kds-filter-field" for="kdsStationFilter"><span>Quầy</span>
                <select id="kdsStationFilter" data-filter-select="station">
                    <option value="all">Tất cả quầy</option>
                    <option value="COFFEE">Quầy cà phê</option>
                    <option value="TEA">Quầy trà</option>
                    <option value="BLENDER">Máy xay</option>
                </select>
            </label>
            <label class="kds-filter-field" for="kdsOrderTypeFilter"><span>Loại đơn</span>
                <select id="kdsOrderTypeFilter" data-filter-select="orderType">
                    <option value="all">Tất cả loại đơn</option>
                    <option value="DINE_IN">Tại bàn</option>
                    <option value="TAKEAWAY">Mang đi</option>
                    <option value="DELIVERY">Giao hàng</option>
                </select>
            </label>
            <button type="button" class="btn btn-ghost btn-sm btn-full" id="kdsClearFilters">Xóa bộ lọc</button>
        </div>
    </details>
    <button type="button" class="btn btn-ghost btn-sm" id="kdsRefresh" title="Tải lại danh sách món">↻ Làm mới</button>
    <div id="kdsConnection" class="kds-connection" role="status">
        <span class="kds-refresh__dot"></span><span>Đang kết nối</span>
    </div>
</div>

<div id="kdsBoard" class="kds-board" data-user-id="${currentUserId}" data-endpoint="${ctx}/barista/kds" aria-busy="false">
    <jsp:include page="kds_cards.jsp" />
</div>
<div id="kdsLiveNotice" class="kds-live-notice" role="status" aria-live="assertive" hidden></div>

<div id="issueModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="issueTitle">
        <h3 id="issueTitle">Báo sự cố</h3><p class="muted kds-modal__name" data-modal-name></p>
        <p class="kds-modal__hint">Sự cố sẽ được báo cho Thu ngân/Quản lý; món không tự động bị hủy.</p>
        <form action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="reportIssue"><input type="hidden" name="orderItemId" data-item-input>
            <label class="kds-field"><span>Lý do</span><select name="reason" required>
                <option value="">Chọn lý do</option><option value="OUT_OF_STOCK">Hết nguyên liệu</option><option value="EQUIPMENT">Máy móc gặp sự cố</option><option value="NOTE_UNSUPPORTED">Không đáp ứng được ghi chú</option><option value="DISCONTINUED">Món đã ngừng bán</option><option value="UNCLEAR_ORDER">Thông tin đơn không rõ</option><option value="OTHER">Lý do khác</option>
            </select></label>
            <label class="kds-field js-other-reason" hidden><span>Lý do khác</span><input type="text" name="otherReason" maxlength="255" autocomplete="off"></label>
            <div class="kds-field js-ingredients" hidden><span>Nguyên liệu đã hết</span><div data-ingredient-slot></div></div>
            <p class="kds-modal__hint js-blocking-note" hidden>Món sẽ chuyển sang mục <strong>Cần xử lý</strong> và rời khỏi hàng chờ pha.</p>
            <div class="kds-modal__actions"><button type="button" class="btn btn-ghost" data-close>Đóng</button><button type="submit" class="btn btn-danger">Gửi báo sự cố</button></div>
        </form>
    </div>
</div>

<div id="remakeModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="remakeTitle">
        <h3 id="remakeTitle">Làm lại món</h3><p class="muted kds-modal__name" data-modal-name></p>
        <p class="kds-modal__hint">Hệ thống ghi nhận hao hụt, giữ lịch sử lượt pha cũ và đưa món về Chờ pha với ưu tiên cao.</p>
        <form action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="remake"><input type="hidden" name="orderItemId" data-item-input>
            <label class="kds-field"><span>Lý do</span><select name="reason" required>
                <option value="">Chọn lý do</option><option value="WRONG_RECIPE">Pha sai công thức</option><option value="SPILLED">Làm đổ hoặc hư món</option><option value="QUALITY">Chất lượng không đạt</option><option value="CUSTOMER_FEEDBACK">Khách phản hồi</option><option value="WRONG_DELIVERY">Giao nhầm</option><option value="CHANGED_REQUEST">Khách thay đổi yêu cầu</option>
            </select></label>
            <div class="kds-modal__actions"><button type="button" class="btn btn-ghost" data-close>Đóng</button><button type="submit" class="btn btn-danger">Xác nhận làm lại</button></div>
        </form>
    </div>
</div>

<div id="unblockModal" class="kds-modal" hidden>
    <div class="kds-modal__backdrop" data-close></div>
    <div class="kds-modal__panel" role="dialog" aria-modal="true" aria-labelledby="unblockTitle">
        <h3 id="unblockTitle">Trả món về chờ pha</h3><p class="muted kds-modal__name" data-modal-name></p>
        <p class="kds-modal__hint">Nếu nguyên liệu đã có lại, kiểm lại tồn thực tế trước khi trả món về hàng chờ.</p>
        <form action="${ctx}/barista/kds" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="unblock"><input type="hidden" name="recount" value="1"><input type="hidden" name="orderItemId" data-item-input>
            <div class="kds-field js-recount"><span>Kiểm kê nguyên liệu</span><div data-recount-slot></div></div>
            <div class="kds-modal__actions"><button type="button" class="btn btn-ghost" data-close>Đóng</button><button type="submit" class="btn btn-primary">Xác nhận</button></div>
        </form>
    </div>
</div>

<script src="${ctx}/assets/js/kds-board.js?v=${applicationScope.assetVersion}" defer></script>
<jsp:include page="../layout/footer.jsp" />
