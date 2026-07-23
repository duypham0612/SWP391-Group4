<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Voucher</h1><p>Quản lý mã giảm giá theo toàn chuỗi hoặc từng chi nhánh.</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/voucher?action=new">+ Thêm voucher</a>
</div>

<c:choose>
    <c:when test="${empty vouchers}">
        <div class="card empty-state"><div class="icon">--</div><p>Chưa có voucher nào.</p></div>
    </c:when>
    <c:otherwise>
        <div data-tabletools data-tt-page-size="10">
            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="voucherSearch">Tìm kiếm</label>
                    <input id="voucherSearch" class="form-control" type="search" data-tt-search placeholder="Tìm theo mã voucher">
                </div>
                <div class="form-group">
                    <label for="voucherTypeFilter">Loại</label>
                    <select id="voucherTypeFilter" class="form-control tt-filter" data-tt-filter data-tt-col="1">
                        <option value="">Tất cả</option>
                        <option value="PERCENT">PERCENT</option>
                        <option value="FIXED">FIXED</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="voucherScopeFilter">Phạm vi</label>
                    <select id="voucherScopeFilter" class="form-control tt-filter" data-tt-filter data-tt-col="4">
                        <option value="">Tất cả</option>
                        <option value="BRANCH">Chi nhánh</option>
                        <option value="CHAIN">Toàn chuỗi</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="voucherStatusFilter">Trạng thái</label>
                    <select id="voucherStatusFilter" class="form-control tt-filter" data-tt-filter data-tt-col="6">
                        <option value="">Tất cả</option>
                        <option value="UPCOMING">Sắp diễn ra</option>
                        <option value="RUNNING">Đang diễn ra</option>
                        <option value="EXPIRED">Hết hạn</option>
                    </select>
                </div>
            </div>
            <table class="table">
                <thead><tr>
                    <th data-tt-search>Mã</th>
                    <th data-tt-nosearch>Loại</th>
                    <th data-tt-nosearch>Giá trị</th>
                    <th data-tt-nosearch>Đơn tối thiểu</th>
                    <th data-tt-nosearch>Phạm vi</th>
                    <th data-tt-nosearch>Đã dùng</th>
                    <th style="width:140px" data-tt-nosearch>Trạng thái</th>
                    <th style="width:170px" data-tt-nosearch>Thao tác</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="v" items="${vouchers}">
                        <tr>
                            <td><strong>${v.code}</strong></td>
                            <td>${v.discountType}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${v.discountType == 'PERCENT'}">${v.discountValue}%</c:when>
                                    <c:otherwise><fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/> ₫</c:otherwise>
                                </c:choose>
                            </td>
                            <td><fmt:formatNumber value="${v.minOrderAmount}" maxFractionDigits="0"/> ₫</td>
                            <td data-tt-val="${v.scope}">
                                <c:choose>
                                    <c:when test="${v.scope == 'BRANCH'}">${v.branchName}</c:when>
                                    <c:otherwise>Toàn chuỗi</c:otherwise>
                                </c:choose>
                            </td>
                            <td>${v.usedCount}<c:if test="${not empty v.usageLimit}">/${v.usageLimit}</c:if></td>
                            <td data-tt-val="${v.lifecycleStatusCode}">
                                <span class="badge ${v.lifecycleBadgeClass}">${v.lifecycleStatusLabel}</span>
                            </td>
                            <td>
                                <a class="btn btn-ghost btn-sm" href="${ctx}/admin/voucher?action=edit&id=${v.voucherId}">Sửa</a>
                                <form action="${ctx}/admin/voucher" method="post" style="display:inline" onsubmit="return confirm('Đổi trạng thái voucher này?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="toggleActive">
                                    <input type="hidden" name="id" value="${v.voucherId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">${v.active ? 'Tắt' : 'Bật'}</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="table-tools-foot">
                <span class="tt-summary" data-tt-summary></span>
                <div class="pagination" data-tt-pager></div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
