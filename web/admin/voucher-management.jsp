<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý Khuyến mãi - Coffee POS</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {
                theme: {
                    extend: {
                        fontFamily: {sans: ['Plus Jakarta Sans', 'Inter', 'sans-serif'], }
                    }
                }
            }
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
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
                        <a href="${pageContext.request.contextPath}/admin/admin-dashboard" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                            <i class="fa-solid fa-chart-pie text-base"></i> Tổng quan
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/menu" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                            <i class="fa-solid fa-utensils text-base"></i> Thực đơn
                        </a>
                        <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                            <i class="fa-solid fa-boxes-stacked text-base"></i> Kho nguyên liệu
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/employees" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                            <i class="fa-solid fa-user-group text-base"></i> Nhân viên
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/vouchers" class="flex items-center gap-3 px-4 py-3 bg-cyan-50 text-cyan-700 rounded-xl font-semibold text-xs transition-all">
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
                        <input type="text" id="search-input" value="${searchKeyword}" placeholder="Gõ từ khóa để tìm kiếm ngay..." class="w-full pl-8 pr-4 py-1.5 text-xs bg-slate-50 border border-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 transition-all">
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
                            <h2 class="text-xl font-bold text-slate-900 tracking-tight">Quản lý Khuyến mãi</h2>
                            <p class="text-xs text-slate-400 font-medium mt-0.5">Tối ưu doanh thu bằng các chiến dịch thông minh</p>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="openAddModal()" class="text-xs font-bold text-white bg-cyan-700 hover:bg-cyan-800 px-4 py-2.5 rounded-xl border border-cyan-700 shadow-sm shadow-cyan-700/10 transition-all flex items-center gap-2">
                                <i class="fa-solid fa-circle-plus text-sm"></i> Tạo khuyến mãi mới
                            </button>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
                        <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-32">
                            <div class="w-8 h-8 rounded-lg bg-cyan-50 text-cyan-700 flex items-center justify-center"><i class="fa-solid fa-ticket text-sm"></i></div>
                            <div>
                                <p class="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-0.5">Đang chạy</p>
                                <h3 class="text-2xl font-bold text-slate-900"><c:out value="${not empty activeCount ? activeCount : 0}" /></h3>
                            </div>
                        </div>
                        <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-32">
                            <div class="w-8 h-8 rounded-lg bg-slate-100 text-slate-500 flex items-center justify-center"><i class="fa-solid fa-clock-rotate-left text-sm"></i></div>
                            <div>
                                <p class="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-0.5">Đã kết thúc</p>
                                <h3 class="text-2xl font-bold text-slate-900"><c:out value="${not empty endedCount ? endedCount : 0}" /></h3>
                            </div>
                        </div>
                        <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-32">
                            <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center"><i class="fa-solid fa-chart-line text-sm"></i></div>
                            <div>
                                <p class="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-0.5">Tổng lượt dùng</p>
                                <h3 class="text-2xl font-bold text-slate-900">
                                    <fmt:formatNumber value="${not empty totalUses ? totalUses : 0}" type="number" maxFractionDigits="0"/>
                                </h3>
                            </div>
                        </div>
                        <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex flex-col justify-between h-32">
                            <div class="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center"><i class="fa-solid fa-arrow-up-right-dots text-sm"></i></div>
                            <div>
                                <p class="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-0.5">Hiệu quả chiến dịch</p>
                                <h3 class="text-2xl font-bold text-cyan-700">
                                    <fmt:formatNumber value="${not empty avgProfitRate ? avgProfitRate : 0}" type="number" maxFractionDigits="1"/>%
                                </h3>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden flex flex-col">
                        <div class="flex items-center border-b border-slate-100 px-6 h-12 bg-slate-50/50">
                            <div class="flex gap-6 text-xs font-semibold text-slate-400 h-full">
                                <button onclick="filterStatus('all')" class="border-b-2 ${currentStatus == null || currentStatus == 'all' ? 'border-cyan-700 text-cyan-700' : 'border-transparent hover:text-slate-600'} px-1 h-full flex items-center">Tất cả</button>
                                <button onclick="filterStatus('active')" class="border-b-2 ${currentStatus == 'active' ? 'border-cyan-700 text-cyan-700' : 'border-transparent hover:text-slate-600'} px-1 h-full flex items-center">Đang hoạt động</button>
                                <button onclick="filterStatus('ended')" class="border-b-2 ${currentStatus == 'ended' ? 'border-cyan-700 text-cyan-700' : 'border-transparent hover:text-slate-600'} px-1 h-full flex items-center">Đã kết thúc</button>
                                <button onclick="filterStatus('draft')" class="border-b-2 ${currentStatus == 'draft' ? 'border-cyan-700 text-cyan-700' : 'border-transparent hover:text-slate-600'} px-1 h-full flex items-center">Bản nháp</button>
                            </div>
                        </div>

                        <div class="overflow-x-auto">
                            <table class="w-full text-left text-xs whitespace-nowrap">
                                <thead class="bg-slate-50/70 text-[11px] text-slate-400 font-bold uppercase border-b border-slate-100">
                                    <tr>
                                        <th class="p-4 pl-6">Tên chương trình</th>
                                        <th class="p-4">Mã Code</th>
                                        <th class="p-4">Loại</th>
                                        <th class="p-4">Thời gian</th>
                                        <th class="p-4">Trạng thái</th>
                                        <th class="p-4 pr-6 text-center">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody id="vouchers-table-body" class="divide-y divide-slate-100 font-medium text-slate-700">
                                    <c:forEach items="${vouchers}" var="v">
                                        <tr class="hover:bg-slate-50/40 transition-colors">
                                            <td class="p-4 pl-6">
                                                <div class="flex items-center gap-3">
                                                    <div class="w-8 h-8 rounded-lg bg-cyan-50 flex items-center justify-center text-cyan-700">
                                                        <i class="fa-solid fa-gift"></i>
                                                    </div>
                                                    <div>
                                                        <h4 class="font-bold text-slate-900 text-xs">${v.voucherCode}</h4>
                                                        <p class="text-[10px] text-slate-400 mt-0.5">
                                                            <c:choose>
                                                                <c:when test="${v.minOrderValue gt 0}">
                                                                    Áp dụng đơn từ <fmt:formatNumber value="${v.minOrderValue}" type="number" maxFractionDigits="0"/>đ
                                                                </c:when>
                                                                <c:otherwise>Áp dụng cho mọi đơn hàng</c:otherwise>
                                                            </c:choose>
                                                        </p>
                                                    </div>
                                                </div>
                                            </td>
                                            <td class="p-4">
                                                <span class="px-2 py-1 bg-slate-100 text-slate-600 text-[11px] font-bold rounded-md tracking-wide border border-slate-200/50">${v.voucherCode}</span>
                                            </td>
                                            <td class="p-4">
                                                <c:choose>
                                                    <c:when test="${v.isPercentage}">
                                                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-cyan-50 text-cyan-600 border border-cyan-100">
                                                            Giảm <fmt:formatNumber value="${v.discountValue}" type="number" maxFractionDigits="0"/>%
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
                                                            Giảm <fmt:formatNumber value="${v.discountValue}" type="number" maxFractionDigits="0"/>đ
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="p-4 text-slate-500 font-normal">
                                                <div class="flex flex-col text-[11px]">
                                                    <span><fmt:formatDate value="${v.startDate}" pattern="dd/MM/yyyy"/></span>
                                                    <span class="text-slate-300 text-[9px] -mt-0.5">đến</span>
                                                    <span><fmt:formatDate value="${v.endDate}" pattern="dd/MM/yyyy"/></span>
                                                </div>
                                            </td>
                                            <td class="p-4">
                                                <c:choose>
                                                    <c:when test="${v.statusLabel eq 'Bản nháp'}">
                                                        <span class="inline-flex items-center gap-1.5 text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md text-[10px] font-bold"><span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span> Bản nháp</span>
                                                    </c:when>
                                                    <c:when test="${v.statusLabel eq 'Sắp diễn ra'}">
                                                        <span class="inline-flex items-center gap-1.5 text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md text-[10px] font-bold"><span class="w-1.5 h-1.5 rounded-full bg-indigo-500"></span> Sắp diễn ra</span>
                                                    </c:when>
                                                    <c:when test="${v.statusLabel eq 'Đang chạy'}">
                                                        <span class="inline-flex items-center gap-1.5 text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md text-[10px] font-bold"><span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Đang chạy</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="inline-flex items-center gap-1.5 text-slate-400 bg-slate-50 px-2 py-0.5 rounded-md text-[10px] font-bold"><span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span> Đã kết thúc</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="p-4 pr-6 text-center text-slate-400 text-sm">
                                                <button onclick="openEditModal(${v.voucherID})" class="hover:text-cyan-700 transition mr-2" title="Chỉnh sửa">
                                                    <i class="fa-regular fa-pen-to-square"></i>
                                                </button>
                                                <c:if test="${v.isActive}">
                                                    <button onclick="confirmStop(${v.voucherID})" class="hover:text-amber-600 transition mr-2" title="Ngừng kích hoạt">
                                                        <i class="fa-regular fa-circle-stop"></i>
                                                    </button>
                                                </c:if>
                                                <button onclick="confirmDelete(${v.voucherID})" class="hover:text-red-600 transition" title="Xóa bỏ">
                                                    <i class="fa-regular fa-trash-can"></i>
                                                </button>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty vouchers}">
                                        <tr>
                                            <td colspan="6" class="p-8 text-center text-slate-400 font-medium">Không tìm thấy dữ liệu chương trình khuyến mãi nào.</td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>

                        <div class="p-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
                            <span class="text-[11px] text-slate-400 font-medium">Hiển thị dữ liệu theo trang phân hoạch</span>
                            <div class="flex items-center gap-1 text-[11px] font-bold">
                                <c:set var="pageIndex" value="${not empty currentPage ? currentPage : 1}" />
                                <button onclick="changePage(${pageIndex > 1 ? pageIndex - 1 : 1})" class="w-6 h-6 rounded-md border border-slate-200 bg-white flex items-center justify-center text-slate-400 hover:bg-slate-50 transition ${pageIndex == 1 ? 'opacity-50 cursor-not-allowed' : ''}">
                                    <i class="fa-solid fa-chevron-left text-[9px]"></i>
                                </button>
                                <c:forEach var="i" begin="${pageIndex > 2 ? pageIndex - 2 : 1}" end="${pageIndex + 2 < 5 ? pageIndex + 2 : 5}">
                                    <button onclick="changePage(${i})" class="w-6 h-6 rounded-md flex items-center justify-center transition ${pageIndex == i ? 'bg-cyan-700 text-white shadow-sm' : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'}">
                                        ${i}
                                    </button>
                                </c:forEach>
                                <button onclick="changePage(${pageIndex < 5 ? pageIndex + 1 : 5})" class="w-6 h-6 rounded-md border border-slate-200 bg-white flex items-center justify-center text-slate-400 hover:bg-slate-50 transition ${pageIndex == 5 ? 'opacity-50 cursor-not-allowed' : ''}">
                                    <i class="fa-solid fa-chevron-right text-[9px]"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>

        <div id="voucherModal" class="fixed inset-0 z-50 invisible flex items-center justify-center bg-slate-900/40 backdrop-blur-sm transition-all duration-300 overflow-y-auto">
            <div class="bg-white rounded-2xl border border-slate-100 shadow-xl max-w-md w-full p-6 space-y-4 transform scale-95 opacity-0 transition-all duration-300 my-8 mx-4" id="modalContent">
                <div class="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h3 class="text-sm font-bold text-slate-900 tracking-tight">Tạo mã khuyến mãi mới</h3>
                    <button onclick="toggleModal(false)" class="text-slate-400 hover:text-slate-600 transition"><i class="fa-solid fa-xmark text-sm"></i></button>
                </div>
                
                <div id="errorAlert" class="hidden p-3 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl font-medium flex items-center gap-2">
                    <i class="fa-solid fa-triangle-exclamation text-sm shrink-0"></i>
                    <span id="errorMessage">Dữ liệu nhập không hợp lệ!</span>
                </div>
                
                <form id="voucherForm" action="${pageContext.request.contextPath}/admin/vouchers" method="POST" class="space-y-4 text-xs">
                    <input type="hidden" name="action" id="modalAction" value="add">
                    <input type="hidden" name="voucherID" id="modalVoucherID" value="">
                    
                    <div>
                        <label class="block font-bold text-slate-700 mb-1">Mã Code <span class="text-red-500">*</span></label>
                        <input type="text" id="code-input" name="voucherCode" required placeholder="Ví dụ: COFFEE2026" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 uppercase font-semibold text-slate-800 transition-all">
                        <span id="codeCheckMsg" class="block text-[10px] mt-1 font-semibold hidden"></span>
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Hình thức giảm <span class="text-red-500">*</span></label>
                            <select name="isPercentage" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 font-semibold text-slate-700 cursor-pointer">
                                <option value="false">Giảm theo Tiền (đ)</option>
                                <option value="true">Giảm theo Phần trăm (%)</option>
                            </select>
                        </div>
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Giá trị giảm <span class="text-red-500">*</span></label>
                            <input type="number" name="discountValue" required placeholder="Ví dụ: 30000" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 font-medium text-slate-800">
                        </div>
                    </div>
                    <div>
                        <label class="block font-bold text-slate-700 mb-1">Đơn hàng tối thiểu cần đạt (đ)</label>
                        <input type="number" name="minOrderValue" value="0" min="0" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 font-medium text-slate-800">
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Ngày bắt đầu <span class="text-red-500">*</span></label>
                            <input type="date" name="startDate" required class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 font-medium text-slate-700">
                        </div>
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Ngày kết thúc <span class="text-red-500">*</span></label>
                            <input type="date" name="endDate" required class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 font-medium text-slate-700">
                        </div>
                    </div>
                    <div class="flex items-center gap-2 pt-1">
                        <input type="checkbox" id="isActive" name="isActive" value="true" checked class="w-4 h-4 text-cyan-700 border-slate-300 rounded focus:ring-cyan-600/20 accent-cyan-700 cursor-pointer">
                        <label for="isActive" class="font-bold text-slate-700 select-none cursor-pointer">Kích hoạt chương trình ngay lập tức</label>
                    </div>
                    <div class="flex justify-end gap-2 border-t border-slate-100 pt-4 mt-2">
                        <button type="button" onclick="toggleModal(false)" class="px-4 py-2 font-bold text-slate-500 bg-slate-100 hover:bg-slate-200 rounded-xl transition">Hủy bỏ</button>
                        <button type="submit" id="submitBtn" class="px-4 py-2 font-bold text-white bg-cyan-700 hover:bg-cyan-800 rounded-xl shadow-md shadow-cyan-700/10 transition">Lưu vào Hệ thống</button>
                    </div>
                </form>
            </div>
        </div>

        <form id="actionForm" action="${pageContext.request.contextPath}/admin/vouchers" method="POST" style="display:none;">
            <input type="hidden" name="action" id="actionFormTask">
            <input type="hidden" name="voucherID" id="actionFormID">
        </form>

        <script>
            let timeout = null;
            let isCodeValid = true; // Biến kiểm soát trạng thái mã code trùng
            
            const searchInput = document.getElementById('search-input');
            const tableBody = document.getElementById('vouchers-table-body');
            const voucherForm = document.getElementById('voucherForm');
            const errorAlert = document.getElementById('errorAlert');
            const errorMessage = document.getElementById('errorMessage');
            
            const codeInput = document.getElementById('code-input');
            const codeCheckMsg = document.getElementById('codeCheckMsg');
            const submitBtn = document.getElementById('submitBtn');
            const contextPath = '${pageContext.request.contextPath}';

            function getUpdatedUrl(params) {
                const url = new URL(window.location.href);
                Object.keys(params).forEach(key => {
                    if (params[key] !== null && params[key] !== undefined) {
                        url.searchParams.set(key, params[key]);
                    }
                });
                return url.toString();
            }

            function changePage(pageNumber) { window.location.href = getUpdatedUrl({ page: pageNumber }); }
            function filterStatus(statusValue) { window.location.href = getUpdatedUrl({ status: statusValue, page: 1 }); }

            // 1. AJAX KIỂM TRA TRÙNG MÃ CODE REAL-TIME KHI NGƯỜI DÙNG NHẬP CHỮ
            codeInput.addEventListener('input', function() {
                const mode = document.getElementById('modalAction').value;
                if(mode === 'edit') return; // Chế độ sửa khóa input code nên không cần check trùng
                
                const codeValue = this.value.trim().toUpperCase();
                codeCheckMsg.classList.add('hidden');
                isCodeValid = true;
                submitBtn.disabled = false;
                submitBtn.classList.remove('opacity-50', 'cursor-not-allowed');

                if(codeValue.length < 3) return;

                clearTimeout(timeout);
                timeout = setTimeout(() => {
                    // Gửi request lên API ẩn checkVoucherCode của Controller
                    const url = contextPath + `/admin/vouchers?action=checkVoucherCode&code=` + encodeURIComponent(codeValue);
                    fetch(url)
                        .then(res => res.json())
                        .then(data => {
                            if(data && data.exists === true) {
                                // Nếu tồn tại => Trùng mã code => Báo đỏ khóa nút bấm
                                isCodeValid = false;
                                submitBtn.disabled = true;
                                submitBtn.classList.add('opacity-50', 'cursor-not-allowed');
                                
                                codeCheckMsg.innerText = "❌ Mã khuyến mãi này đã tồn tại trên hệ thống!";
                                codeCheckMsg.className = "block text-[10px] mt-1 font-semibold text-red-600";
                                codeCheckMsg.classList.remove('hidden');
                            } else {
                                // Hợp lệ => Báo xanh
                                codeCheckMsg.innerText = "✅ Mã code hợp lệ và có thể sử dụng.";
                                codeCheckMsg.className = "block text-[10px] mt-1 font-semibold text-emerald-600";
                                codeCheckMsg.classList.remove('hidden');
                            }
                        })
                        .catch(err => console.error("Lỗi validate code:", err));
                }, 300);
            });

            // Tìm kiếm Ajax danh sách
            searchInput.addEventListener('input', function () {
                clearTimeout(timeout);
                timeout = setTimeout(() => {
                    const keyword = searchInput.value;
                    const urlParams = new URLSearchParams(window.location.search);
                    const status = urlParams.get('status') || 'all';
                    const page = urlParams.get('page') || '1';
                    const url = contextPath + `/admin/vouchers?search=` + encodeURIComponent(keyword) + `&status=` + status + `&page=` + page;

                    fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                    .then(response => response.text())
                    .then(htmlChunk => { tableBody.innerHTML = htmlChunk; })
                    .catch(error => console.error('Lỗi tìm kiếm realtime:', error));
                }, 200);
            });

            voucherForm.elements['isPercentage'].addEventListener('change', function() {
                const isPercent = this.value === 'true';
                const discountInput = voucherForm.elements['discountValue'];
                if (isPercent) { discountInput.placeholder = "Nhập từ 10 đến 100 (%)"; } 
                else { discountInput.placeholder = "Nhập từ 10000 đến 500000 (đ)"; }
            });

            // Chặn submit form nếu mã code đang bị trùng
            voucherForm.addEventListener('submit', function (e) {
                if(!isCodeValid && document.getElementById('modalAction').value === 'add') {
                    e.preventDefault();
                    showFormError("Vui lòng đổi mã Code khác, mã hiện tại đang bị trùng!");
                    return;
                }
                
                errorAlert.classList.add('hidden');
                const isPercent = voucherForm.elements['isPercentage'].value === 'true';
                const discountValue = parseFloat(voucherForm.elements['discountValue'].value);
                const startDateStr = voucherForm.elements['startDate'].value;
                const endDateStr = voucherForm.elements['endDate'].value;

                if (startDateStr && endDateStr) {
                    const start = new Date(startDateStr);
                    const end = new Date(endDateStr);
                    if (start > end) {
                        e.preventDefault();
                        showFormError("Ngày bắt đầu không được lớn hơn ngày kết thúc!");
                        return;
                    }
                }

                if (isPercent) {
                    if (discountValue < 10 || discountValue > 100) {
                        e.preventDefault();
                        showFormError("Mức giảm theo phần trăm phải nằm trong khoảng từ 10% đến 100%!");
                        return;
                    }
                } else {
                    if (discountValue < 10000 || discountValue > 500000) {
                        e.preventDefault();
                        showFormError("Mức giảm tiền mặt phải nằm trong khoảng từ 10.000đ đến 500.000đ!");
                        return;
                    }
                }
            });

            function showFormError(message) {
                errorMessage.innerText = message;
                errorAlert.classList.remove('hidden');
                document.getElementById('voucherModal').scrollTop = 0;
            }

            document.addEventListener('keydown', function(e) { if (e.key === 'Escape') toggleModal(false); });

            function toggleModal(show) {
                const modal = document.getElementById('voucherModal');
                const content = document.getElementById('modalContent');
                if (show) {
                    modal.classList.remove('invisible');
                    setTimeout(() => {
                        content.classList.remove('scale-95', 'opacity-0');
                        content.classList.add('scale-100', 'opacity-100');
                    }, 10);
                } else {
                    content.classList.remove('scale-100', 'opacity-100');
                    content.classList.add('scale-95', 'opacity-0');
                    setTimeout(() => modal.classList.add('invisible'), 300);
                }
            }

            function openAddModal() {
                document.getElementById('modalAction').value = 'add';
                document.getElementById('modalVoucherID').value = '';
                errorAlert.classList.add('hidden');
                codeCheckMsg.classList.add('hidden');
                isCodeValid = true;
                
                const form = document.getElementById('voucherForm');
                form.reset();
                submitBtn.disabled = false;
                submitBtn.classList.remove('opacity-50', 'cursor-not-allowed');
                
                form.elements['voucherCode'].readOnly = false;
                form.elements['voucherCode'].classList.remove('opacity-60');
                form.elements['isPercentage'].dispatchEvent(new Event('change'));
                document.querySelector('#voucherModal h3').innerText = 'Tạo mã khuyến mãi mới';
                toggleModal(true);
            }

            function openEditModal(id) {
                document.getElementById('modalAction').value = 'edit';
                document.getElementById('modalVoucherID').value = id;
                errorAlert.classList.add('hidden');
                codeCheckMsg.classList.add('hidden');
                isCodeValid = true;
                
                submitBtn.disabled = false;
                submitBtn.classList.remove('opacity-50', 'cursor-not-allowed');
                
                const url = contextPath + `/admin/vouchers?action=getVoucher&id=` + id;
                fetch(url)
                    .then(res => res.json())
                    .then(data => {
                        if(data && data.code) {
                            const form = document.getElementById('voucherForm');
                            form.elements['voucherCode'].value = data.code;
                            form.elements['voucherCode'].readOnly = true;
                            form.elements['voucherCode'].classList.add('opacity-60');
                            
                            form.elements['isPercentage'].value = data.isPercentage.toString();
                            form.elements['discountValue'].value = data.discount;
                            form.elements['minOrderValue'].value = data.minOrder;
                            form.elements['startDate'].value = data.start;
                            form.elements['endDate'].value = data.end;
                            form.elements['isActive'].checked = data.isActive;
                            
                            form.elements['isPercentage'].dispatchEvent(new Event('change'));
                            document.querySelector('#voucherModal h3').innerText = 'Chỉnh sửa chương trình khuyến mãi';
                            toggleModal(true);
                        } else {
                            alert("Không thể tải thông tin khuyến mãi này!");
                        }
                    })
                    .catch(err => {
                        console.error("Lỗi lấy thông tin voucher:", err);
                        alert("Đã xảy ra lỗi hệ thống khi tải thông tin!");
                    });
            }

            function confirmStop(id) {
                if (confirm('Bạn có chắc chắn muốn ngừng kích hoạt chương trình khuyến mãi này ngay lập tức?')) {
                    document.getElementById('actionFormTask').value = 'stop';
                    document.getElementById('actionFormID').value = id;
                    document.getElementById('actionForm').submit();
                }
            }

            function confirmDelete(id) {
                if (confirm('Hành động này không thể hoàn tác! Bạn có chắc chắn muốn xóa voucher này?')) {
                    document.getElementById('actionFormTask').value = 'delete';
                    document.getElementById('actionFormID').value = id;
                    document.getElementById('actionForm').submit();
                }
            }
        </script>
    </body>
</html>
