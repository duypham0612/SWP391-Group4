<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="8"> <%-- Tự làm mới mỗi 8 giây --%>
    <title>Màn gọi món - My Coffee House</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = { theme: { extend: { fontFamily: { sans: ['Outfit', 'sans-serif'] } } } };
    </script>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body class="bg-[#0c1f22] text-white font-sans min-h-screen">
    <div class="max-w-6xl mx-auto px-8 py-8">
        <div class="flex items-center justify-between mb-8">
            <div class="flex items-center gap-4">
                <div class="w-14 h-14 rounded-2xl bg-[#006064] flex items-center justify-center shadow-lg"><i class="fa-solid fa-mug-hot text-2xl"></i></div>
                <div>
                    <h1 class="text-3xl font-extrabold tracking-tight">ĐƠN ĐÃ SẴN SÀNG</h1>
                    <p class="text-sm text-teal-200/70 font-medium">My Coffee House &middot; Mời quý khách nhận đồ uống</p>
                </div>
            </div>
            <div class="text-right">
                <div id="clock" class="text-4xl font-extrabold tabular-nums">--:--</div>
                <p class="text-xs text-teal-200/60 mt-1"><i class="fa-solid fa-rotate text-[10px] mr-1"></i>Tự cập nhật mỗi 8 giây</p>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty readyList}">
                <div class="flex flex-col items-center justify-center py-32 text-center">
                    <i class="fa-solid fa-mug-saucer text-white/10 text-7xl mb-5"></i>
                    <p class="text-xl font-bold text-white/40">Chưa có đồ uống nào sẵn sàng</p>
                    <p class="text-sm text-white/25 mt-1">Các món pha xong sẽ hiển thị tại đây</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
                    <c:forEach var="it" items="${readyList}">
                        <div class="bg-white/5 border border-white/10 rounded-3xl p-5 backdrop-blur-sm hover:bg-white/10 transition-all">
                            <div class="flex items-center justify-between mb-3">
                                <span class="text-2xl font-extrabold text-[#4dd0c4]">#${it.orderId}</span>
                                <span class="text-[11px] font-bold text-white/50"><i class="fa-regular fa-clock mr-1"></i><fmt:formatDate value="${it.completedAt}" pattern="HH:mm"/></span>
                            </div>
                            <h3 class="text-lg font-bold leading-snug"><c:out value="${it.productName}"/> <span class="text-[#4dd0c4]">x${it.quantity}</span></h3>
                            <div class="mt-3 flex items-center gap-2 text-sm text-white/60 font-semibold">
                                <i class="fa-solid fa-chair text-[12px]"></i> ${empty it.tableName ? 'Mang đi' : it.tableName}
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <script>
        function tick() {
            var d = new Date();
            var p = function (n) { return (n < 10 ? '0' : '') + n; };
            document.getElementById('clock').textContent = p(d.getHours()) + ':' + p(d.getMinutes());
        }
        tick();
        setInterval(tick, 1000);
    </script>
</body>
</html>
