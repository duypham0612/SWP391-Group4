<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="nav" scope="request" value="board"/>

<%-- Header dùng chung (head + mở body) --%>
<jsp:include page="/common/header.jsp" />

<%-- Sidebar riêng cho Barista (mở aside + top header + main) --%>
<jsp:include page="/common/barista_sidebar.jsp" />

<div class="space-y-6 fade-in" data-ctx="${pageContext.request.contextPath}">

    <!-- Tiêu đề + nút tải lại -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Bảng pha chế realtime</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Theo dõi và xử lý các món đồ uống đang chờ. Tự động cập nhật mỗi 5 giây.</p>
        </div>
        <div class="flex items-center gap-3">
            <span id="last-sync" class="text-[11px] text-slate-400 font-medium"></span>
            <button onclick="baristaRefresh()" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white border border-slate-200 text-xs font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all">
                <i class="fa-solid fa-arrows-rotate text-slate-400"></i>
                Làm mới ngay
            </button>
        </div>
    </div>

    <!-- Kanban 3 cột -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <!-- Cột: Chờ pha -->
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 flex flex-col overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-amber-50/40">
                <div class="flex items-center gap-2.5">
                    <span class="w-8 h-8 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center"><i class="fa-solid fa-hourglass-half text-sm"></i></span>
                    <h3 class="text-sm font-bold text-slate-800">Chờ pha</h3>
                </div>
                <span id="count-Pending" class="text-[11px] font-bold text-amber-600 bg-amber-100 px-2.5 py-1 rounded-full">0</span>
            </div>
            <div id="col-Pending" class="p-4 space-y-3 min-h-[200px]"></div>
        </div>

        <!-- Cột: Đang pha -->
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 flex flex-col overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-sky-50/40">
                <div class="flex items-center gap-2.5">
                    <span class="w-8 h-8 rounded-xl bg-sky-100 text-sky-600 flex items-center justify-center"><i class="fa-solid fa-blender text-sm"></i></span>
                    <h3 class="text-sm font-bold text-slate-800">Đang pha</h3>
                </div>
                <span id="count-Preparing" class="text-[11px] font-bold text-sky-600 bg-sky-100 px-2.5 py-1 rounded-full">0</span>
            </div>
            <div id="col-Preparing" class="p-4 space-y-3 min-h-[200px]"></div>
        </div>

        <!-- Cột: Hoàn thành -->
        <div class="bg-white rounded-3xl shadow-sm border border-slate-200/60 flex flex-col overflow-hidden">
            <div class="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-emerald-50/40">
                <div class="flex items-center gap-2.5">
                    <span class="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center"><i class="fa-solid fa-circle-check text-sm"></i></span>
                    <h3 class="text-sm font-bold text-slate-800">Hoàn thành</h3>
                </div>
                <span id="count-Completed" class="text-[11px] font-bold text-emerald-600 bg-emerald-100 px-2.5 py-1 rounded-full">0</span>
            </div>
            <div id="col-Completed" class="p-4 space-y-3 min-h-[200px]"></div>
        </div>
    </div>
</div>

<script>
    var CTX = document.querySelector('[data-ctx]').getAttribute('data-ctx');
    var queueCache = []; // lưu dữ liệu mới nhất để tick đồng hồ

    function htmlEsc(s) {
        if (s == null) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
                        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Định dạng khoảng thời gian (giây) -> "Xp Ys"
    function fmtElapsed(sec) {
        if (sec < 0) sec = 0;
        var m = Math.floor(sec / 60), s = sec % 60;
        return m + 'p ' + (s < 10 ? '0' : '') + s + 's';
    }

    // Màu badge thời gian theo mức độ chờ lâu
    function waitClass(sec) {
        if (sec >= 600) return 'bg-red-50 text-red-600 border-red-100';      // > 10 phút
        if (sec >= 300) return 'bg-amber-50 text-amber-600 border-amber-100'; // > 5 phút
        return 'bg-emerald-50 text-emerald-600 border-emerald-100';
    }

    // Vẽ 1 thẻ món
    function renderCard(it) {
        var now = Date.now();
        var isPending = it.itemStatus === 'Pending';
        var isPreparing = it.itemStatus === 'Preparing';
        var isCompleted = it.itemStatus === 'Completed';

        // Thời gian hiển thị: Pending/Completed tính từ lúc gọi; Preparing tính từ lúc bắt đầu pha
        var baseEpoch = (isPreparing && it.startedEpoch > 0) ? it.startedEpoch : it.orderEpoch;
        var elapsedSec = Math.floor((now - baseEpoch) / 1000);
        var timeLabel = (isPreparing ? 'Đang pha ' : (isCompleted ? 'Chờ ' : 'Chờ ')) + fmtElapsed(elapsedSec);

        var pinned = it.priority > 0;
        var html = '';
        html += '<div class="rounded-2xl border ' + (pinned ? 'border-amber-300 ring-1 ring-amber-200 bg-amber-50/30' : 'border-slate-200/70 bg-white') + ' p-3.5 shadow-sm hover:shadow transition-all">';

        // Hàng đầu: mã order + bàn + ghim
        html += '<div class="flex items-center justify-between mb-2">';
        html += '  <div class="flex items-center gap-2">';
        html += '    <a href="' + CTX + '/barista-detail?orderId=' + it.orderId + '" target="_blank" title="Xem chi tiết order" class="text-[10px] font-extrabold text-[#006064] bg-[#006064]/10 px-2 py-0.5 rounded-md hover:bg-[#006064]/20">#' + it.orderId + '</a>';
        html += '    <span class="text-[11px] font-bold text-slate-500"><i class="fa-solid fa-chair text-[9px] mr-1 text-slate-400"></i>' + htmlEsc(it.tableName || 'Mang đi') + '</span>';
        if (pinned) { html += '    <span class="text-[9px] font-bold text-amber-600"><i class="fa-solid fa-thumbtack"></i> Ưu tiên</span>'; }
        html += '  </div>';
        html += '  <span class="time-badge text-[9px] font-bold px-2 py-0.5 rounded-full border ' + waitClass(elapsedSec) + '" data-base="' + baseEpoch + '" data-prep="' + (isPreparing ? 1 : 0) + '">' + timeLabel + '</span>';
        html += '</div>';

        // Tên món + số lượng
        html += '<div class="flex items-start justify-between gap-2">';
        html += '  <h4 class="text-sm font-bold text-slate-800 leading-snug">' + htmlEsc(it.productName) + '</h4>';
        html += '  <span class="shrink-0 text-xs font-extrabold text-white bg-[#006064] w-7 h-7 rounded-lg flex items-center justify-center">x' + it.quantity + '</span>';
        html += '</div>';

        // Ghi chú
        if (it.note && it.note.trim() !== '') {
            html += '<p class="mt-1.5 text-[11px] text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-2 py-1"><i class="fa-solid fa-pen-to-square text-[9px] mr-1"></i>' + htmlEsc(it.note) + '</p>';
        }

        // Nút hành động
        html += '<div class="mt-3 flex flex-wrap items-center gap-1.5">';
        if (isPending) {
            html += '<button onclick="setStatus(' + it.orderDetailId + ',\'Preparing\')" class="flex-1 inline-flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-700 text-white text-[11px] font-bold transition-all"><i class="fa-solid fa-play text-[9px]"></i> Bắt đầu pha</button>';
        } else if (isPreparing) {
            html += '<button onclick="setStatus(' + it.orderDetailId + ',\'Completed\')" class="flex-1 inline-flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-bold transition-all"><i class="fa-solid fa-check text-[9px]"></i> Hoàn thành</button>';
        } else if (isCompleted) {
            html += '<button onclick="setStatus(' + it.orderDetailId + ',\'Preparing\')" class="inline-flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 text-[11px] font-bold transition-all"><i class="fa-solid fa-rotate-left text-[9px]"></i> Hoàn tác</button>';
        }
        // In tem
        html += '<button onclick="printLabel(' + it.orderDetailId + ')" title="In tem ly" class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 transition-all"><i class="fa-solid fa-print text-[11px]"></i></button>';
        if (!isCompleted) {
            // Ghim ưu tiên
            html += '<button onclick="bumpPriority(' + it.orderId + ')" title="Ghim/bỏ ghim ưu tiên" class="inline-flex items-center justify-center w-8 h-8 rounded-lg ' + (pinned ? 'bg-amber-100 text-amber-600' : 'bg-slate-100 hover:bg-slate-200 text-slate-600') + ' transition-all"><i class="fa-solid fa-thumbtack text-[11px]"></i></button>';
            // Báo hết món
            html += '<button onclick="outOfStock(' + it.productId + ',\'' + htmlEsc(it.productName).replace(/\'/g, "\\\'") + '\')" title="Báo tạm hết món" class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-red-50 hover:bg-red-100 text-red-500 transition-all"><i class="fa-solid fa-ban text-[11px]"></i></button>';
        }
        html += '</div>';

        html += '</div>';
        return html;
    }

    function emptyCol(label) {
        return '<div class="flex flex-col items-center justify-center py-10 text-center"><i class="fa-solid fa-mug-saucer text-slate-200 text-4xl"></i><p class="text-[11px] text-slate-300 font-bold mt-2">' + label + '</p></div>';
    }

    function renderAll(data) {
        queueCache = data;
        var groups = { Pending: [], Preparing: [], Completed: [] };
        data.forEach(function (it) { if (groups[it.itemStatus]) groups[it.itemStatus].push(it); });

        ['Pending', 'Preparing', 'Completed'].forEach(function (st) {
            var col = document.getElementById('col-' + st);
            var cnt = document.getElementById('count-' + st);
            cnt.textContent = groups[st].length;
            if (groups[st].length === 0) {
                col.innerHTML = emptyCol(st === 'Pending' ? 'Chưa có món chờ' : (st === 'Preparing' ? 'Chưa pha món nào' : 'Chưa hoàn thành món nào'));
            } else {
                col.innerHTML = groups[st].map(renderCard).join('');
            }
        });
    }

    function baristaRefresh() {
        fetch(CTX + '/barista-board?action=queue')
            .then(function (r) { return r.json(); })
            .then(function (data) {
                renderAll(data);
                var d = new Date();
                var p = function (n) { return (n < 10 ? '0' : '') + n; };
                document.getElementById('last-sync').textContent = 'Đồng bộ lúc ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
            })
            .catch(function (e) { console.error('Lỗi tải hàng chờ:', e); });
    }

    function setStatus(orderDetailId, status) {
        postAction({ action: 'updateStatus', orderDetailId: orderDetailId, status: status });
    }
    function bumpPriority(orderId) {
        postAction({ action: 'bumpPriority', orderId: orderId });
    }
    function outOfStock(productId, name) {
        if (!confirm('Báo tạm hết món "' + name + '"?\nMón sẽ bị ẩn khỏi menu và các phần đang chờ sẽ chuyển sang "tạm hết".')) return;
        postAction({ action: 'outOfStock', productId: productId });
    }

    function postAction(params) {
        var body = Object.keys(params).map(function (k) {
            return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
        }).join('&');
        fetch(CTX + '/barista-board', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        }).then(function () { baristaRefresh(); })
          .catch(function (e) { console.error('Lỗi cập nhật:', e); });
    }

    function printLabel(orderDetailId) {
        window.open(CTX + '/barista-label?orderDetailId=' + orderDetailId, '_blank', 'width=380,height=480');
    }

    // Tick đồng hồ trên từng thẻ mỗi giây (không cần gọi server)
    function tickBadges() {
        var now = Date.now();
        document.querySelectorAll('.time-badge').forEach(function (b) {
            var base = parseInt(b.getAttribute('data-base'), 10);
            var prep = b.getAttribute('data-prep') === '1';
            if (!base) return;
            var sec = Math.floor((now - base) / 1000);
            b.textContent = (prep ? 'Đang pha ' : 'Chờ ') + fmtElapsed(sec);
            b.className = 'time-badge text-[9px] font-bold px-2 py-0.5 rounded-full border ' + waitClass(sec);
        });
    }

    // Khởi động
    baristaRefresh();
    setInterval(baristaRefresh, 5000); // polling realtime mỗi 5s
    setInterval(tickBadges, 1000);     // đồng hồ trên thẻ mỗi 1s
</script>

<%-- Footer dùng chung (đóng main + body) --%>
<jsp:include page="/common/footer.jsp" />
