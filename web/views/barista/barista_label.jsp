<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Tem ly - <c:choose><c:when test="${not empty labelItem}">#${labelItem.orderId}</c:when><c:otherwise>N/A</c:otherwise></c:choose></title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, sans-serif; background: #f1f5f9; display: flex; flex-direction: column; align-items: center; padding: 16px; }
        .label { width: 280px; background: #fff; border: 1px dashed #94a3b8; border-radius: 8px; padding: 16px 18px; }
        .shop { text-align: center; font-weight: 800; font-size: 13px; letter-spacing: 1px; color: #006064; }
        .sub  { text-align: center; font-size: 10px; color: #64748b; margin-bottom: 8px; }
        .divider { border-top: 1px dashed #cbd5e1; margin: 8px 0; }
        .order-code { text-align: center; font-size: 26px; font-weight: 800; color: #0f172a; }
        .row { display: flex; justify-content: space-between; font-size: 12px; margin: 3px 0; color: #334155; }
        .row .k { color: #94a3b8; }
        .product { font-size: 16px; font-weight: 800; color: #0f172a; margin-top: 6px; }
        .qty { font-size: 12px; font-weight: 700; color: #006064; }
        .note { font-size: 11px; color: #b45309; background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 5px 7px; margin-top: 6px; }
        .foot { text-align: center; font-size: 9px; color: #94a3b8; margin-top: 10px; }
        .actions { margin-top: 14px; display: flex; gap: 8px; }
        .btn { flex: 1; padding: 9px; border: none; border-radius: 8px; font-size: 12px; font-weight: 700; cursor: pointer; }
        .btn-print { background: #006064; color: #fff; }
        .btn-close { background: #e2e8f0; color: #334155; }
        @media print { body { background: #fff; padding: 0; } .label { border: none; width: 100%; } .actions { display: none; } }
    </style>
</head>
<body>
    <div class="label">
        <c:choose>
            <c:when test="${empty labelItem}">
                <div class="order-code">N/A</div>
                <div class="sub">Không tìm thấy món để in tem.</div>
            </c:when>
            <c:otherwise>
                <div class="shop">MY COFFEE HOUSE</div>
                <div class="sub">Tem pha chế</div>
                <div class="divider"></div>
                <div class="order-code">#${labelItem.orderId}</div>
                <div class="row"><span class="k">Bàn</span><span>${empty labelItem.tableName ? 'Mang đi' : labelItem.tableName}</span></div>
                <div class="divider"></div>
                <div class="product"><c:out value="${labelItem.productName}"/> <span class="qty">x${labelItem.quantity}</span></div>
                <c:if test="${not empty labelItem.note}">
                    <div class="note">Ghi chú: <c:out value="${labelItem.note}"/></div>
                </c:if>
                <div class="divider"></div>
                <div class="row"><span class="k">Giờ gọi</span><span><fmt:formatDate value="${labelItem.orderDate}" pattern="dd/MM/yyyy HH:mm"/></span></div>
                <div class="row"><span class="k">Mã món</span><span>OD-${labelItem.orderDetailId}</span></div>
                <div class="foot">Cảm ơn quý khách - My Coffee House</div>
            </c:otherwise>
        </c:choose>

        <div class="actions">
            <button class="btn btn-print" onclick="window.print()">In tem</button>
            <button class="btn btn-close" onclick="window.close()">Đóng</button>
        </div>
    </div>

    <script>
        window.addEventListener('load', function () { setTimeout(function () { window.print(); }, 300); });
    </script>
</body>
</html>
