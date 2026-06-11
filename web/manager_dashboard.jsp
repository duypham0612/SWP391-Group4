<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Attendance"%>
<%@page import="java.text.SimpleDateFormat"%>

<%-- Nhúng Header dùng chung --%>
<jsp:include page="common/header.jsp" />

<%-- Nhúng Sidebar dùng chung --%>
<jsp:include page="common/sidebar.jsp" />

<%
    // Lấy thông số từ Servlet
    int lowStockCount = request.getAttribute("lowStockCount") != null ? (Integer) request.getAttribute("lowStockCount") : 0;
    int workingCount = request.getAttribute("workingCount") != null ? (Integer) request.getAttribute("workingCount") : 0;
    List<Attendance> todayAttendance = (List<Attendance>) request.getAttribute("todayAttendance");
    
    // Định dạng giờ check-in
    SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a");
%>

<!-- Dashboard Content Wrapper -->
<div class="space-y-8 fade-in">

    <!-- Header / Subheader Section -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Chào buổi sáng, Admin!</h1>
            <p class="text-xs text-slate-400 font-medium mt-1">Đây là những gì đang diễn ra tại cửa hàng của bạn hôm nay.</p>
        </div>
        
        <div class="flex items-center gap-3">
            <!-- Date Display -->
            <div class="flex items-center gap-2 bg-white border border-slate-200/60 px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-700 shadow-sm">
                <i class="fa-regular fa-calendar-days text-[#006064]"></i>
                <span id="live-date">Hôm nay: 15 Th06, 2026</span>
            </div>
            
            <!-- Add Button -->
            <button class="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold transition-all shadow-md shadow-[#006064]/10">
                <i class="fa-solid fa-plus text-[10px]"></i>
                <span>Tạo đơn mới</span>
            </button>
        </div>
    </div>

    <!-- Stat Cards Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <!-- Card 1: Tổng doanh thu -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-40 relative overflow-hidden">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng doanh thu</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1.5">12.840k</h3>
                </div>
                <div class="px-2 py-1 rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600 text-[10px] font-bold flex items-center gap-1">
                    <span>+12.5%</span>
                    <i class="fa-solid fa-arrow-trend-up text-[8px]"></i>
                </div>
            </div>
            <!-- Mini bar chart placeholder -->
            <div class="flex items-end gap-1 h-10 w-full mt-4">
                <div class="w-full bg-slate-100 rounded-sm h-3"></div>
                <div class="w-full bg-slate-100 rounded-sm h-5"></div>
                <div class="w-full bg-slate-100 rounded-sm h-4"></div>
                <div class="w-full bg-[#006064]/40 rounded-sm h-6"></div>
                <div class="w-full bg-[#006064]/65 rounded-sm h-8"></div>
                <div class="w-full bg-[#006064] rounded-sm h-10"></div>
            </div>
        </div>

        <!-- Card 2: Tổng đơn hàng -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-40 relative overflow-hidden">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng đơn hàng</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1.5">156</h3>
                </div>
                <div class="px-2 py-1 rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600 text-[10px] font-bold flex items-center gap-1">
                    <span>+8.2%</span>
                    <i class="fa-solid fa-arrow-trend-up text-[8px]"></i>
                </div>
            </div>
            <!-- Mini bar chart placeholder -->
            <div class="flex items-end gap-1 h-10 w-full mt-4">
                <div class="w-full bg-slate-100 rounded-sm h-2"></div>
                <div class="w-full bg-[#006064]/20 rounded-sm h-4"></div>
                <div class="w-full bg-[#006064]/40 rounded-sm h-3"></div>
                <div class="w-full bg-[#006064]/60 rounded-sm h-6"></div>
                <div class="w-full bg-[#006064]/80 rounded-sm h-5"></div>
                <div class="w-full bg-[#006064] rounded-sm h-8"></div>
            </div>
        </div>

        <!-- Card 3: Sắp hết hàng -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-40 relative overflow-hidden">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Sắp hết hàng</p>
                    <h3 class="text-2xl font-extrabold text-red-500 tracking-tight mt-1.5"><%= lowStockCount %> <span class="text-xs font-semibold text-slate-400">mục</span></h3>
                </div>
                <div class="px-2 py-1 rounded-lg bg-red-50 border border-red-100 text-red-500 text-[10px] font-bold flex items-center gap-1">
                    <span>-2.4%</span>
                    <i class="fa-solid fa-arrow-trend-down text-[8px]"></i>
                </div>
            </div>
            <!-- Progress Bar -->
            <div class="space-y-1.5 mt-4">
                <div class="flex justify-between text-[10px] font-bold text-slate-500">
                    <span>Ngưỡng cảnh báo</span>
                    <span>85%</span>
                </div>
                <div class="w-full bg-slate-100 rounded-full h-2">
                    <div class="bg-red-500 h-2 rounded-full w-[85%]"></div>
                </div>
            </div>
        </div>

        <!-- Card 4: Nhân viên đang làm -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between h-40 relative overflow-hidden">
            <div class="flex items-start justify-between">
                <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Nhân viên đang làm</p>
                    <h3 class="text-2xl font-extrabold text-slate-800 tracking-tight mt-1.5"><%= workingCount %> / 12</h3>
                </div>
                <div class="px-2 py-0.5 rounded-full bg-blue-50 border border-blue-100 text-blue-600 text-[9px] font-bold">
                    Ổn định
                </div>
            </div>
            <!-- Avatar stack -->
            <div class="flex items-center gap-1.5 mt-4">
                <div class="flex -space-x-2.5 overflow-hidden">
                    <div class="inline-block h-6 w-6 rounded-full ring-2 ring-white bg-slate-200 text-[9px] font-bold flex items-center justify-center text-slate-600">MT</div>
                    <div class="inline-block h-6 w-6 rounded-full ring-2 ring-white bg-slate-200 text-[9px] font-bold flex items-center justify-center text-slate-600">HM</div>
                    <div class="inline-block h-6 w-6 rounded-full ring-2 ring-white bg-slate-200 text-[9px] font-bold flex items-center justify-center text-slate-600">DA</div>
                </div>
                <div class="text-[9px] font-bold text-slate-400 pl-1">
                    +<%= Math.max(0, workingCount - 3) %> người khác
                </div>
            </div>
        </div>
    </div>

    <!-- Central Grid (Revenue Chart + Best Sellers) -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Column 1 & 2: Revenue Chart -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm lg:col-span-2 space-y-6">
            <div class="flex items-center justify-between">
                <div>
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Biểu đồ doanh thu</h3>
                    <p class="text-[10px] text-slate-400 font-medium">Thống kê chi tiết doanh thu theo giờ trong ngày.</p>
                </div>
                
                <!-- Filter Dropdown -->
                <select class="bg-slate-50 border border-slate-200 text-xs font-semibold px-3 py-1.5 rounded-xl focus:outline-none">
                    <option>Hôm nay</option>
                    <option>Hôm qua</option>
                    <option>7 ngày trước</option>
                </select>
            </div>
            
            <!-- Spline wave chart canvas -->
            <div class="h-64 relative">
                <canvas id="revenueChart"></canvas>
            </div>
        </div>

        <!-- Column 3: Best Selling Products -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between">
            <div class="space-y-5">
                <div class="flex items-center justify-between">
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Món bán chạy</h3>
                    <a href="#" class="text-xs font-bold text-[#006064] hover:text-[#004d40]">Tất cả</a>
                </div>

                <!-- Best sellers list -->
                <div class="space-y-3.5">
                    <!-- Item 1 -->
                    <div class="flex items-center justify-between p-2.5 rounded-2xl hover:bg-slate-50 transition-colors">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-xl bg-orange-50 text-orange-500 flex items-center justify-center font-semibold text-lg">
                                <i class="fa-solid fa-mug-hot text-sm"></i>
                            </div>
                            <div>
                                <h4 class="text-xs font-bold text-slate-800">Latte Đá Hạnh Nhân</h4>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5">42 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="text-right">
                            <p class="text-xs font-bold text-slate-800">55k</p>
                            <p class="text-[9px] text-emerald-500 font-bold mt-0.5">+15%</p>
                        </div>
                    </div>

                    <!-- Item 2 -->
                    <div class="flex items-center justify-between p-2.5 rounded-2xl hover:bg-slate-50 transition-colors">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-xl bg-sky-50 text-sky-500 flex items-center justify-center font-semibold text-lg">
                                <i class="fa-solid fa-whiskey-glass text-sm"></i>
                            </div>
                            <div>
                                <h4 class="text-xs font-bold text-slate-800">Americano Cam Sả</h4>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5">38 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="text-right">
                            <p class="text-xs font-bold text-slate-800">45k</p>
                            <p class="text-[9px] text-emerald-500 font-bold mt-0.5">+8%</p>
                        </div>
                    </div>

                    <!-- Item 3 -->
                    <div class="flex items-center justify-between p-2.5 rounded-2xl hover:bg-slate-50 transition-colors">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-xl bg-amber-50 text-amber-500 flex items-center justify-center font-semibold text-lg">
                                <i class="fa-solid fa-stroopwafel text-sm"></i>
                            </div>
                            <div>
                                <h4 class="text-xs font-bold text-slate-800">Bánh Sừng Bò Bơ</h4>
                                <p class="text-[10px] text-slate-400 font-medium mt-0.5">29 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="text-right">
                            <p class="text-xs font-bold text-slate-800">35k</p>
                            <p class="text-[9px] text-slate-400 font-bold mt-0.5">0%</p>
                        </div>
                    </div>
                </div>
            </div>

            <div class="pt-4 border-t border-slate-100 flex items-center justify-between text-[10px] font-bold text-slate-400 uppercase tracking-wide">
                <span>Hiệu suất Menu</span>
                <span class="text-emerald-500 font-bold">Tốt</span>
            </div>
        </div>
    </div>

    <!-- Bottom Grid (Working Shift Employees + Recent Orders) -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Column 1: Shift Employees -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col justify-between">
            <div class="space-y-5">
                <div class="flex items-center justify-between">
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Nhân viên trực</h3>
                    <a href="manager-attendance" class="text-xs font-bold text-[#006064] hover:text-[#004d40]">Bảng công</a>
                </div>

                <!-- Employees List -->
                <div class="space-y-4">
                    <%
                        if (todayAttendance != null && !todayAttendance.isEmpty()) {
                            for (Attendance att : todayAttendance) {
                                String empName = att.getEmployeeName() != null ? att.getEmployeeName() : "Nhân viên";
                                String sName = "";
                                if (empName.split(" ").length > 0) {
                                    sName = empName.split(" ")[empName.split(" ").length - 1];
                                }
                                String checkinStr = att.getCheckInTime() != null ? sdfTime.format(att.getCheckInTime()) : "Chưa Check-in";
                    %>
                                <div class="flex items-center justify-between p-1">
                                    <div class="flex items-center gap-3">
                                        <div class="w-8 h-8 rounded-xl bg-slate-100 flex items-center justify-center font-bold text-slate-600 text-xs shadow-inner">
                                            <%= sName.substring(0, Math.min(2, sName.length())).toUpperCase() %>
                                        </div>
                                        <div>
                                            <h4 class="text-xs font-bold text-slate-800"><%= empName %></h4>
                                            <p class="text-[9px] text-slate-400 font-medium mt-0.5"><%= att.getShiftName() != null ? att.getShiftName() : "Ca làm việc" %></p>
                                        </div>
                                    </div>
                                    <div class="text-right">
                                        <span class="inline-block px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[8px] font-bold">
                                            <%= checkinStr %>
                                        </span>
                                    </div>
                                </div>
                    <%
                            }
                        } else {
                    %>
                        <div class="py-6 text-center text-xs text-slate-400 font-medium">
                            Không có nhân viên nào đang trực hôm nay.
                        </div>
                    <%
                        }
                    %>
                </div>
            </div>
            
            <div class="pt-4 border-t border-slate-100">
                <button onclick="window.location.href='manager-attendance'" class="w-full text-center py-2 bg-slate-50 hover:bg-slate-100 text-slate-600 text-xs font-bold rounded-xl transition-colors border border-slate-200/50">
                    Xem bảng chấm công
                </button>
            </div>
        </div>

        <!-- Column 2 & 3: Recent Orders -->
        <div class="bg-white p-6 rounded-3xl border border-slate-200/60 shadow-sm lg:col-span-2 space-y-5">
            <div class="flex items-center justify-between">
                <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider">Đơn hàng gần đây</h3>
                <a href="#" class="text-xs font-bold text-[#006064] hover:text-[#004d40]">Xuất báo cáo</a>
            </div>

            <!-- Table of orders -->
            <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                            <th class="py-3 px-4">Mã Đơn</th>
                            <th class="py-3 px-4">Thời Gian</th>
                            <th class="py-3 px-4">Bàn</th>
                            <th class="py-3 px-4 text-right">Giá Trị</th>
                            <th class="py-3 px-4 text-center">Trạng Thái</th>
                            <th class="py-3 px-4 text-center">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-50 text-xs font-medium text-slate-600">
                        <!-- Order Row 1 -->
                        <tr class="hover:bg-slate-50/60 transition-colors">
                            <td class="py-3.5 px-4 font-bold text-slate-800">#CF12890</td>
                            <td class="py-3.5 px-4 text-slate-400">10:45 AM</td>
                            <td class="py-3.5 px-4">Bàn 04</td>
                            <td class="py-3.5 px-4 text-right font-bold text-slate-850">145.000đ</td>
                            <td class="py-3.5 px-4 text-center">
                                <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[9px] font-bold">Đã thanh toán</span>
                            </td>
                            <td class="py-3.5 px-4 text-center">
                                <button class="w-7 h-7 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 mx-auto transition-colors">
                                    <i class="fa-regular fa-eye"></i>
                                </button>
                            </td>
                        </tr>

                        <!-- Order Row 2 -->
                        <tr class="hover:bg-slate-50/60 transition-colors">
                            <td class="py-3.5 px-4 font-bold text-slate-800">#CF12891</td>
                            <td class="py-3.5 px-4 text-slate-400">10:42 AM</td>
                            <td class="py-3.5 px-4">Mang về</td>
                            <td class="py-3.5 px-4 text-right font-bold text-slate-850">85.000đ</td>
                            <td class="py-3.5 px-4 text-center">
                                <span class="px-2 py-0.5 rounded-full bg-sky-50 text-sky-600 text-[9px] font-bold">Đang xử lý</span>
                            </td>
                            <td class="py-3.5 px-4 text-center">
                                <button class="w-7 h-7 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 mx-auto transition-colors">
                                    <i class="fa-regular fa-eye"></i>
                                </button>
                            </td>
                        </tr>

                        <!-- Order Row 3 -->
                        <tr class="hover:bg-slate-50/60 transition-colors">
                            <td class="py-3.5 px-4 font-bold text-slate-800">#CF12892</td>
                            <td class="py-3.5 px-4 text-slate-400">10:35 AM</td>
                            <td class="py-3.5 px-4">Bàn 12</td>
                            <td class="py-3.5 px-4 text-right font-bold text-slate-850">210.000đ</td>
                            <td class="py-3.5 px-4 text-center">
                                <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[9px] font-bold">Đã thanh toán</span>
                            </td>
                            <td class="py-3.5 px-4 text-center">
                                <button class="w-7 h-7 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 mx-auto transition-colors">
                                    <i class="fa-regular fa-eye"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Chart.js and Dashboard Init -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
    // Live date format
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const today = new Date();
    document.getElementById('live-date').innerText = "Hôm nay: " + today.toLocaleDateString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric'
    });

    // Chart.js initialization
    const ctx = document.getElementById('revenueChart').getContext('2d');
    
    // Gradient fill definition
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(0, 96, 100, 0.25)');
    gradient.addColorStop(1, 'rgba(0, 96, 100, 0.00)');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['07:00', '10:00', '13:00', '16:00', '19:00', '22:00'],
            datasets: [{
                data: [3.2, 2.5, 6.8, 5.2, 1.8, 5.9],
                borderColor: '#006064',
                borderWidth: 3,
                fill: true,
                backgroundColor: gradient,
                tension: 0.45,
                pointBackgroundColor: '#006064',
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    grid: { color: '#f1f5f9' },
                    ticks: {
                        color: '#94a3b8',
                        font: { size: 10, weight: 'bold' },
                        callback: function(value) { return value + 'M'; }
                    },
                    border: { dash: [5, 5] }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#94a3b8',
                        font: { size: 10, weight: 'bold' }
                    }
                }
            }
        }
    });
</script>

<%-- Nhúng Footer dùng chung --%>
<jsp:include page="common/footer.jsp" />
