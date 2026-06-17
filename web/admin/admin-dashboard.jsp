<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tổng Quan - Coffee POS</title>
    
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: { sans: ['Plus Jakarta Sans', 'Inter', 'sans-serif'], }
                }
            }
        }
    </script>
    
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; }
        ::-webkit-scrollbar { width: 5px; height: 5px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
    </style>
</head>
<body class="bg-[#f8fafc] text-slate-800 antialiased">

<div class="min-h-screen flex">
    
    <aside class="w-64 bg-white border-r border-slate-100 p-5 flex flex-col justify-between sticky top-0 h-screen shrink-0 hidden md:flex">
        <div>
            <div class="flex items-center gap-3 px-2 py-3 mb-6">
                <div class="w-9 h-9 rounded-xl bg-cyan-700 flex items-center justify-center text-white shadow-md shadow-cyan-700/20">
                    <i class="fa-solid fa-mug-hot text-lg"></i>
                </div>
                <div>
                    <h1 class="text-sm font-bold text-slate-900 tracking-tight">Coffee POS</h1>
                    <p class="text-[10px] text-slate-400 font-medium">Hệ thống quản lý</p>
                </div>
            </div>

            <nav class="space-y-1">
                <a href="${pageContext.request.contextPath}/admin/admin-dashboard" class="flex items-center gap-3 px-4 py-3 bg-cyan-50 text-cyan-700 rounded-xl font-semibold text-xs transition-all">
                    <i class="fa-solid fa-chart-pie text-base"></i> Tổng quan
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-utensils text-base"></i> Thực đơn
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-boxes-stacked text-base"></i> Kho nguyên liệu
                </a>
                <a href="${pageContext.request.contextPath}/admin/employees" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-user-group text-base"></i> Nhân viên
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-tags text-base"></i> Khuyến mãi
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-chart-column text-base"></i> Báo cáo
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-gear text-base"></i> Cài đặt
                </a>
            </nav>
        </div>

        <div class="flex items-center gap-3 p-2 bg-slate-50 rounded-xl border border-slate-100">
            <div class="w-8 h-8 rounded-lg bg-cyan-700 flex items-center justify-center text-white text-xs font-bold">
                ${fn:substring(sessionScope.user.fullName, 0, 2)}
            </div>
            <div class="flex-1 min-w-0">
                <h4 class="text-xs font-bold text-slate-800 truncate">${sessionScope.user.fullName}</h4>
                <p class="text-[10px] text-slate-400 font-medium">Administrator</p>
            </div>
        </div>
    </aside>

    <main class="flex-1 flex flex-col min-w-0 overflow-x-hidden">
        
        <header class="h-14 bg-white border-b border-slate-100 px-8 flex items-center justify-between sticky top-0 z-40">
            <div class="relative w-72">
                <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                    <i class="fa-solid fa-magnifying-glass text-xs"></i>
                </span>
                <input type="text" placeholder="Tìm kiếm hóa đơn, món ăn, nhân viên..." class="w-full pl-8 pr-4 py-1.5 text-xs bg-slate-50 border border-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 transition-all">
            </div>
            <div class="flex items-center gap-4">
                <button class="text-slate-400 hover:text-slate-600 relative"><i class="fa-regular fa-bell text-sm"></i></button>
                <button class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-user-gear text-sm"></i></button>
                <a href="${pageContext.request.contextPath}/login?action=logout" class="text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-xl border border-red-100 transition">
                    Đăng xuất <i class="fa-solid fa-arrow-right-from-bracket ml-1"></i>
                </a>
            </div>
        </header>

        <div class="p-8 space-y-6 max-w-[1400px] w-full mx-auto">
            
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h2 class="text-xl font-bold text-slate-900 tracking-tight">Chào buổi sáng, Admin!</h2>
                    <p class="text-xs text-slate-400 font-medium mt-0.5">Đây là những gì đang diễn ra tại cửa hàng của bạn hôm nay.</p>
                </div>
                <div class="flex items-center gap-2 text-xs font-semibold">
                    <span class="bg-white border border-slate-200 px-4 py-2 rounded-xl text-slate-600 shadow-sm">
                        <i class="fa-regular fa-calendar mr-1.5 text-slate-400"></i> Hôm nay: <span id="currentDate">--/--/----</span>
                    </span>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
                
                <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-36">
                    <div class="flex justify-between items-start">
                        <div class="w-8 h-8 rounded-lg bg-cyan-50 text-cyan-700 flex items-center justify-center"><i class="fa-solid fa-wallet text-sm"></i></div>
                        <span class="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-1 rounded-md">+${revenueGrowth}% <i class="fa-solid fa-arrow-trend-up ml-1"></i></span>
                    </div>
                    <div>
                        <p class="text-[11px] text-slate-400 font-semibold mb-1">Tổng doanh thu</p>
                        <h3 class="text-2xl font-bold text-slate-900">
                            <fmt:formatNumber value="${totalRevenue}" type="number" maxFractionDigits="0"/>đ
                        </h3>
                    </div>
                </div>

                <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-36">
                    <div class="flex justify-between items-start">
                        <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center"><i class="fa-solid fa-cart-shopping text-sm"></i></div>
                        <span class="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-1 rounded-md">+${orderGrowth}% <i class="fa-solid fa-arrow-trend-up ml-1"></i></span>
                    </div>
                    <div>
                        <p class="text-[11px] text-slate-400 font-semibold mb-1">Tổng đơn hàng</p>
                        <h3 class="text-2xl font-bold text-slate-900">${totalOrders}</h3>
                    </div>
                </div>

                <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-36">
                    <div class="flex justify-between items-start">
                        <div class="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center"><i class="fa-solid fa-triangle-exclamation text-sm"></i></div>
                        <span class="text-[10px] font-bold text-rose-600 bg-rose-50 px-2 py-1 rounded-md">Cảnh báo</span>
                    </div>
                    <div class="w-full">
                        <p class="text-[11px] text-slate-400 font-semibold mb-1">Sắp hết hàng</p>
                        <div class="flex items-end justify-between mb-2">
                            <h3 class="text-2xl font-bold text-slate-900">${lowStockCount} <span class="text-xs text-slate-400 font-medium">mục</span></h3>
                            <span class="text-xs font-bold text-rose-600">${stockAlertPercent}%</span>
                        </div>
                        <div class="w-full bg-slate-100 rounded-full h-1.5">
                            <div class="bg-rose-600 h-1.5 rounded-full" style="width: ${stockAlertPercent}%"></div>
                        </div>
                    </div>
                </div>

                <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-36">
                    <div class="flex justify-between items-start">
                        <div class="w-8 h-8 rounded-lg bg-slate-100 text-slate-600 flex items-center justify-center"><i class="fa-solid fa-user-group text-sm"></i></div>
                        <span class="text-[10px] font-bold text-slate-500 bg-slate-50 px-2 py-1 rounded-md border border-slate-100">Ổn định</span>
                    </div>
                    <div>
                        <p class="text-[11px] text-slate-400 font-semibold mb-1">Nhân viên đang làm</p>
                        <div class="flex items-center justify-between">
                            <h3 class="text-2xl font-bold text-slate-900">${activeStaffCount} <span class="text-base text-slate-300">/ ${totalStaffCount}</span></h3>
                            <div class="flex -space-x-2">
                                <div class="w-6 h-6 rounded-full bg-cyan-700 text-white text-[8px] font-bold flex items-center justify-center border border-white">AD</div>
                                <div class="w-6 h-6 rounded-full bg-indigo-600 text-white text-[8px] font-bold flex items-center justify-center border border-white">ST</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
                
                <div class="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <div class="flex items-center justify-between mb-4">
                        <div>
                            <h3 class="text-sm font-bold text-slate-900">Biểu đồ doanh thu</h3>
                            <p class="text-[11px] text-slate-400 font-medium mt-0.5">Thống kê chi tiết theo các mốc giờ trong ngày.</p>
                        </div>
                    </div>
                    <div class="w-full h-64 relative">
                        <canvas id="revenueChart"></canvas>
                    </div>
                </div>

                <div class="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm flex flex-col">
                    <div class="flex items-center justify-between mb-4">
                        <h3 class="text-sm font-bold text-slate-900">Món bán chạy</h3>
                    </div>
                    
                    <div class="flex-1 space-y-4 overflow-y-auto">
                        <c:forEach items="${topSellingItems}" var="item">
                            <div class="flex items-center gap-3">
                                <div class="w-10 h-10 rounded-xl bg-slate-100 overflow-hidden shrink-0 flex items-center justify-center font-bold text-cyan-700 bg-cyan-50 text-xs">
                                    ${fn:substring(item.name, 0, 2)}
                                </div>
                                <div class="flex-1 min-w-0">
                                    <h4 class="text-xs font-bold text-slate-900 truncate">${item.name}</h4>
                                    <p class="text-[10px] text-slate-400">${item.soldQuantity} đơn hôm nay</p>
                                </div>
                                <div class="text-right">
                                    <p class="text-xs font-bold text-slate-900"><fmt:formatNumber value="${item.price}" type="number" maxFractionDigits="0"/>đ</p>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
                
                <div class="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
                    <h3 class="text-sm font-bold text-slate-900 mb-4">Nhân viên trực</h3>
                    <div class="space-y-4">
                        <c:forEach items="${onDutyStaffs}" var="staff">
                            <div class="flex items-center justify-between">
                                <div class="flex items-center gap-3">
                                    <div class="w-8 h-8 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px] flex items-center justify-center uppercase">
                                        ${fn:substring(staff.fullName, 0, 2)}
                                    </div>
                                    <div>
                                        <h4 class="text-xs font-bold text-slate-900">${staff.fullName}</h4>
                                        <p class="text-[10px] text-slate-400">${staff.roleName} • ${staff.shiftTime}</p>
                                    </div>
                                </div>
                                <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <div class="lg:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden flex flex-col">
                    <div class="p-6 pb-4">
                        <h3 class="text-sm font-bold text-slate-900">Đơn hàng gần đây</h3>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-xs whitespace-nowrap">
                            <thead class="bg-slate-50/70 text-[11px] text-slate-400 font-bold uppercase">
                                <tr>
                                    <th class="p-4 pl-6">Mã Đơn</th>
                                    <th class="p-4">Thời Gian</th>
                                    <th class="p-4">Bàn</th>
                                    <th class="p-4">Giá Trị</th>
                                    <th class="p-4">Trạng Thái</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-50 font-medium text-slate-700">
                                <c:forEach items="${recentOrders}" var="order">
                                    <tr class="hover:bg-slate-50/50 transition-colors">
                                        <td class="p-4 pl-6 font-bold text-cyan-700">#${order.code}</td>
                                        <td class="p-4">${order.time}</td>
                                        <td class="p-4">${order.tableName}</td>
                                        <td class="p-4 font-bold text-slate-900"><fmt:formatNumber value="${order.totalPrice}" type="number"/>đ</td>
                                        <td class="p-4">
                                            <span class="px-2 py-1 rounded-md text-[10px] font-bold ${order.status eq 'Đã thanh toán' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}">
                                                ${order.status}
                                            </span>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>

            </div>
        </div>
    </main>
</div>

<script>
    // Set ngày hiện hành
    const options = { day: '2-digit', month: 'short', year: 'numeric' };
    document.getElementById('currentDate').innerText = new Date().toLocaleDateString('vi-VN', options).replace('Thg ', 'Th');

    // Cấu hình Chart.js với mảng data động truyền từ Backend
    const ctx = document.getElementById('revenueChart').getContext('2d');
    let gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(14, 116, 144, 0.2)');
    gradient.addColorStop(1, 'rgba(14, 116, 144, 0)');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: [${chartLabels}], // Ví dụ truyền: '07:00','10:00','13:00'
            datasets: [{
                label: 'Doanh thu',
                data: [${chartData}], // Ví dụ truyền: 1200000, 2500000, 1800000
                borderColor: '#0e7490',
                backgroundColor: gradient,
                borderWidth: 2,
                fill: true,
                tension: 0.35
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { display: false }, x: { grid: { display: false } } }
        }
    });
</script>
</body>
</html>
