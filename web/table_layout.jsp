<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Table"%>

<%-- Nhúng Header dùng chung --%>
<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="max-w-7xl mx-auto space-y-8 fade-in p-6">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h2 class="text-2xl font-bold text-slate-800 tracking-tight">Sơ đồ bàn (POS)</h2>
            <p class="text-xs text-slate-400 font-medium mt-1">Quản lý trạng thái và vị trí khách ngồi tại chi nhánh hiện tại.</p>
        </div>

        <div class="flex items-center gap-3">

            <div class="relative inline-block text-left" id="tableFilter">
                <button type="button" onclick="toggleDropdown()" class="inline-flex justify-between items-center w-40 bg-white border border-slate-200 text-xs font-semibold px-4 py-2.5 rounded-xl text-slate-600 shadow-sm focus:outline-none hover:border-[#006064] hover:text-[#006064] transition-all" id="filter-button">
                    <span id="filter-text">Tất cả bàn</span>
                    <i class="fa-solid fa-chevron-down text-[10px] ml-2 transition-transform duration-200" id="filter-icon"></i>
                </button>

                <div id="dropdown-menu" class="hidden absolute right-0 mt-2 w-40 bg-white rounded-xl shadow-lg border border-slate-100 focus:outline-none z-50 overflow-hidden transition-all duration-200 opacity-0 transform scale-95 origin-top-right">
                    <ul class="py-1 text-xs text-slate-600 font-medium">
                        <li><a href="#" onclick="applyFilter('all', 'Tất cả bàn', event)" class="block px-4 py-2 hover:bg-slate-50 hover:text-[#006064] transition-colors">Tất cả bàn</a></li>
                        <li><a href="#" onclick="applyFilter('empty', 'Bàn trống', event)" class="block px-4 py-2 hover:bg-slate-50 hover:text-[#006064] transition-colors">Bàn trống</a></li>
                        <li><a href="#" onclick="applyFilter('occupied', 'Đang sử dụng', event)" class="block px-4 py-2 hover:bg-slate-50 hover:text-[#006064] transition-colors">Đang sử dụng</a></li>
                    </ul>
                </div>
            </div>

            <button onclick="window.location.reload();" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006064] text-xs font-bold text-white shadow-sm hover:bg-[#004d40] transition-all">
                <i class="fa-solid fa-arrows-rotate text-teal-100"></i>
                Làm mới
            </button>
        </div>
    </div>

    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6" id="table-grid">
        <%
            List<Table> list = (List<Table>) request.getAttribute("danhSachBan");
            if (list == null || list.isEmpty()) {
        %>
            <div class="col-span-full py-12 text-center">
                <p class="text-sm font-bold text-slate-400">Chưa có dữ liệu bàn. Vui lòng thêm bàn trong DB.</p>
            </div>
        <%
            } else {
                for (Table t : list) {
                    boolean isEmpty = "Empty".equalsIgnoreCase(t.getStatus());
                    String bgClass = isEmpty ? "bg-white border-slate-200 hover:border-[#006064]" : "bg-orange-50 border-orange-200 hover:border-orange-400";
                    String textClass = isEmpty ? "text-slate-700" : "text-orange-700";
                    String statusText = isEmpty ? "Trống" : "Đang phục vụ";
                    String iconClass = isEmpty ? "text-slate-300" : "text-orange-400";

                    // Tạo data-status để JS nhận diện filter
                    String dataStatus = isEmpty ? "empty" : "occupied";
        %>
            <button class="table-card relative flex flex-col items-center justify-center p-6 rounded-3xl border-2 shadow-sm transition-all duration-300 transform hover:-translate-y-1 group <%= bgClass %>" data-status="<%= dataStatus %>">

                <i class="fa-solid fa-mug-saucer text-4xl mb-3 <%= iconClass %> group-hover:scale-110 transition-transform"></i>

                <h3 class="text-sm font-extrabold <%= textClass %>"><%= t.getTableName() %></h3>

                <span class="mt-2 px-3 py-1 rounded-full text-[10px] font-bold tracking-wider uppercase <%= isEmpty ? "bg-slate-100 text-slate-500" : "bg-orange-100 text-orange-600 animate-pulse" %>">
                    <%= statusText %>
                </span>

                <% if (t.getQrCodeUrl() != null && !t.getQrCodeUrl().isEmpty()) { %>
                    <div class="absolute top-3 right-3 text-slate-300">
                        <i class="fa-solid fa-qrcode text-xs"></i>
                    </div>
                <% } %>
            </button>
        <%
                }
            }
        %>
    </div>
</div>

<jsp:include page="common/footer.jsp" />

<script>
    // Hàm mở/đóng Dropdown với Animation mượt
    function toggleDropdown() {
        const menu = document.getElementById('dropdown-menu');
        const icon = document.getElementById('filter-icon');

        if (menu.classList.contains('hidden')) {
            menu.classList.remove('hidden');
            icon.classList.add('rotate-180');
            // Delay siêu nhỏ để animation scale/opacity hoạt động
            setTimeout(() => {
                menu.classList.remove('opacity-0', 'scale-95');
                menu.classList.add('opacity-100', 'scale-100');
            }, 10);
        } else {
            closeDropdown();
        }
    }

    // Hàm đóng Dropdown
    function closeDropdown() {
        const menu = document.getElementById('dropdown-menu');
        const icon = document.getElementById('filter-icon');

        menu.classList.remove('opacity-100', 'scale-100');
        menu.classList.add('opacity-0', 'scale-95');
        icon.classList.remove('rotate-180');

        setTimeout(() => {
            menu.classList.add('hidden');
        }, 200); // Đợi animation CSS chạy xong
    }

    // Lắng nghe sự kiện click ra ngoài để đóng menu
    window.addEventListener('click', function(e) {
        const filterContainer = document.getElementById('tableFilter');
        if (!filterContainer.contains(e.target)) {
            const menu = document.getElementById('dropdown-menu');
            if (!menu.classList.contains('hidden')) {
                closeDropdown();
            }
        }
    });

    // Hàm thực thi lọc dữ liệu bàn
    function applyFilter(status, text, event) {
        event.preventDefault(); // Ngăn trình duyệt nhảy lên đầu trang do thẻ <a>

        // 1. Cập nhật Text hiển thị trên nút
        document.getElementById('filter-text').innerText = text;

        // 2. Đóng menu lại
        closeDropdown();

        // 3. Chạy logic ẩn/hiện các bàn
        const cards = document.querySelectorAll('.table-card');
        cards.forEach(card => {
            const cardStatus = card.getAttribute('data-status');

            if (status === 'all' || cardStatus === status) {
                // Mở lại card (reset display css, fadeIn)
                card.style.display = '';
                card.classList.remove('opacity-0', 'scale-95');
                card.classList.add('opacity-100', 'scale-100');
            } else {
                // Ẩn card đi
                card.style.display = 'none';
                card.classList.remove('opacity-100', 'scale-100');
                card.classList.add('opacity-0', 'scale-95');
            }
        });
    }
</script>