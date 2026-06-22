<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản Lý Nhân Viên - Coffee POS</title>

        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {
                theme: {
                    extend: {
                        fontFamily: {
                            sans: ['Plus Jakarta Sans', 'Inter', 'sans-serif'],
                        },
                        colors: {
                            primary: '#0e7490',
                        }
                    }
                }
            }
        </script>

        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

        <style>
            body {
                font-family: 'Plus Jakarta Sans', sans-serif;
            }
            .modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(15, 23, 42, 0.3);
                backdrop-filter: blur(4px);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 9999;
                visibility: hidden;
                opacity: 0;
                transition: all 0.25s ease-in-out;
            }
            .modal-overlay.open {
                visibility: visible;
                opacity: 1;
            }
            input[type=number]::-webkit-inner-spin-button, 
            input[type=number]::-webkit-outer-spin-button { 
                -webkit-appearance: none;
                margin: 0; 
            }
            input[type=number] {
                -moz-appearance: textfield;
            }
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
                        <a href="${pageContext.request.contextPath}/admin/employees" class="flex items-center gap-3 px-4 py-3 bg-cyan-50 text-cyan-700 rounded-xl font-semibold text-xs transition-all">
                            <i class="fa-solid fa-user-group text-base"></i> Nhân viên
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/vouchers" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
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
                <header class="h-14 bg-white border-b border-slate-100 px-8 flex items-center justify-end sticky top-0 z-40">
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
                            <h2 class="text-xl font-bold text-slate-900 tracking-tight">Quản lý Nhân viên</h2>
                            <p class="text-xs text-slate-400 font-medium mt-0.5">Theo dõi và quản lý đội ngũ nhân sự của quán.</p>
                        </div>

                        <div class="flex items-center gap-3 self-end sm:self-auto">
                            <div class="bg-white border border-slate-100 px-4 py-2 rounded-xl text-center shadow-sm min-w-[100px]">
                                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng nhân sự</p>
                                <p id="statTotal" class="text-sm font-bold text-slate-800">${totalEmployees != null ? totalEmployees : fn:length(employees)}</p>
                            </div>
                            <div class="bg-white border border-slate-100 px-4 py-2 rounded-xl text-center shadow-sm min-w-[100px]">
                                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Đang làm việc</p>
                                <p class="text-sm font-bold text-cyan-600">
                                    <c:set var="activeCount" value="0"/>
                                    <c:forEach items="${employees}" var="e">
                                        <c:if test="${e.status eq 'Đang làm' || e.status eq 'Đang quản lý'}"><c:set var="activeCount" value="${activeCount + 1}"/></c:if>
                                    </c:forEach>
                                    ${activeCount}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div class="flex flex-col md:flex-row md:items-center justify-between gap-3 pt-2">
                        <div id="roleFilterContainer" class="flex bg-slate-100 p-1 rounded-xl w-fit text-xs font-semibold text-slate-500">
                            <button onclick="selectRoleTab(this, 'all')" class="bg-white text-slate-800 px-4 py-1.5 rounded-lg shadow-sm tab-btn">Tất cả</button>
                            <button onclick="selectRoleTab(this, 'Admin')" class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition tab-btn">Admin</button>
                            <button onclick="selectRoleTab(this, 'Manager')" class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition tab-btn">Manager</button>
                            <button onclick="selectRoleTab(this, 'Barista')" class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition tab-btn">Barista</button>
                            <button onclick="selectRoleTab(this, 'Cashier')" class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition tab-btn">Cashier</button>
                        </div>

                        <div class="flex flex-wrap items-center gap-2 self-end md:self-auto">
                            <div class="relative w-64">
                                <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                                    <i class="fa-solid fa-magnifying-glass text-xs"></i>
                                </span>
                                <input id="globalSearchInput" onkeyup="filterEmployees()" type="text" placeholder="Tìm nhanh theo tên, ID hoặc email..." class="w-full pl-8 pr-4 py-1.5 text-xs bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 transition-all shadow-sm">
                            </div>

                            <button onclick="toggleAdvancedFilter()" class="border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm">
                                <i class="fa-solid fa-sliders text-slate-400"></i> Bộ lọc nâng cao
                            </button>

                            <button type="button" class="bg-cyan-700 hover:bg-cyan-800 text-white px-4 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition" onclick="openAddModal()">
                                <i class="fa-solid fa-plus"></i> Thêm nhân viên
                            </button>
                        </div>
                    </div>

                    <div id="advancedFilterPanel" class="hidden bg-white p-5 rounded-2xl border border-slate-200 shadow-sm transition-all duration-300">
                        <div class="flex items-center justify-between mb-4 border-b border-slate-100 pb-2.5">
                            <h4 class="text-xs font-bold text-slate-900 flex items-center gap-2">
                                <i class="fa-solid fa-filter text-cyan-700"></i> Tiêu chí lọc nâng cao
                            </h4>
                            <button onclick="clearAdvancedFilters()" class="text-[11px] font-semibold text-rose-600 hover:underline">
                                <i class="fa-solid fa-arrow-rotate-left"></i> Làm mới bộ lọc
                            </button>
                        </div>

                        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-4 text-xs">
                            <div>
                                <label class="block font-bold text-slate-500 mb-1">Mã nhân viên (ID)</label>
                                <input id="filterId" oninput="filterEmployees()" type="text" placeholder="Ví dụ: 001" class="w-full px-3 py-1.5 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 font-medium bg-slate-50/50">
                            </div>
                            <div>
                                <label class="block font-bold text-slate-500 mb-1">Họ và Tên</label>
                                <input id="filterName" oninput="filterEmployees()" type="text" placeholder="Nhập tên..." class="w-full px-3 py-1.5 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 font-medium bg-slate-50/50">
                            </div>
                            <div>
                                <label class="block font-bold text-slate-500 mb-1">Vai trò / Chức vụ</label>
                                <select id="filterRole" onchange="filterEmployees()" class="w-full px-3 py-1.5 border border-slate-200 rounded-xl bg-slate-50 outline-none focus:border-cyan-600 font-semibold text-slate-600">
                                    <option value="all">-- Tất cả chức vụ --</option>
                                    <c:forEach items="${roles}" var="role">
                                        <option value="${role.roleName}">${role.roleName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div>
                                <label class="block font-bold text-slate-500 mb-1">Trạng thái làm việc</label>
                                <select id="filterStatus" onchange="filterEmployees()" class="w-full px-3 py-1.5 border border-slate-200 rounded-xl bg-slate-50 outline-none focus:border-cyan-600 font-semibold text-slate-600">
                                    <option value="all">-- Tất cả trạng thái --</option>
                                    <option value="Đang làm">Đang làm / Đang quản lý</option>
                                    <option value="Nghỉ việc">Nghỉ việc / Tạm nghỉ</option>
                                </select>
                            </div>
                            <div>
                                <label class="block font-bold text-slate-500 mb-1">Ca làm việc</label>
                                <select id="filterShift" onchange="filterEmployees()" class="w-full px-3 py-1.5 border border-slate-200 rounded-xl bg-slate-50 outline-none focus:border-cyan-600 font-semibold text-slate-600">
                                    <option value="all">-- Tất cả các ca --</option>
                                    <option value="0">Chưa xếp ca</option>
                                    <c:forEach items="${shifts}" var="s">
                                        <option value="${s.shiftId}">${s.shiftName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>

                    <c:if test="${param.success eq 'delete'}">
                        <div class="bg-emerald-50 text-emerald-700 p-3.5 border border-emerald-100 rounded-xl text-xs font-medium flex items-center gap-2">
                            <i class="fa-solid fa-circle-check text-base"></i> Đã xóa dữ liệu tài khoản nhân viên ra khỏi hệ thống thành công!
                        </div>
                    </c:if>
                    <c:if test="${param.error eq 'cannotDeleteAdmin'}">
                        <div class="bg-rose-50 text-rose-700 p-3.5 border border-rose-100 rounded-xl text-xs font-semibold flex items-center gap-2">
                            <i class="fa-solid fa-circle-exclamation text-base"></i> Lỗi bảo mật: Không được phép xóa tài khoản Quản trị viên hệ thống (System Admin)!
                        </div>
                    </c:if>
                    <c:if test="${param.error eq 'deleteFailed'}">
                        <div class="bg-rose-50 text-rose-700 p-3.5 border border-rose-100 rounded-xl text-xs flex items-center gap-2">
                            <i class="fa-solid fa-circle-exclamation text-base"></i> Không thể xóa nhân viên này vì dữ liệu đã bị ràng buộc lịch sử ở các bảng khác.
                        </div>
                    </c:if>
                    <c:if test="${param.success eq 'true'}">
                        <div class="bg-emerald-50 text-emerald-700 p-3.5 border border-emerald-100 rounded-xl text-xs font-medium flex items-center gap-2">
                            <i class="fa-solid fa-circle-check text-base"></i> Thêm nhân viên mới thành công!
                        </div>
                    </c:if>
                    <c:if test="${param.success eq 'updated'}">
                        <div class="bg-emerald-50 text-emerald-700 p-3.5 border border-emerald-100 rounded-xl text-xs font-medium flex items-center gap-2">
                            <i class="fa-solid fa-circle-check text-base"></i> Cập nhật thông tin nhân viên thành công!
                        </div>
                    </c:if>

                    <div class="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
                        <div class="overflow-x-auto">
                            <table class="w-full text-left text-xs whitespace-nowrap" id="employeeTable">
                                <thead class="bg-slate-50/70 border-b border-slate-100 text-[11px] text-slate-400 font-bold uppercase tracking-wider">
                                    <tr>
                                        <th class="p-4 pl-6">ID</th>
                                        <th class="p-4">Họ và Tên</th>
                                        <th class="p-4">Vai trò / Chức vụ</th>
                                        <th class="p-4">Trạng thái</th>
                                        <th class="p-4">Ca làm việc</th>
                                        <th class="p-4 pr-6 text-right">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-100 font-medium text-slate-700">
                                    <c:forEach items="${employees}" var="emp">
                                        <tr class="employee-row hover:bg-slate-50/50 transition-colors"
                                            data-id="${String.format('%03d', emp.id)}"
                                            data-name="${fn:toLowerCase(emp.fullName)}"
                                            data-email="${fn:toLowerCase(emp.email)}"
                                            data-role="${emp.roleName}"
                                            data-status="${emp.status}"
                                            data-shift="${emp.shiftId == null ? '0' : emp.shiftId}">

                                            <td class="p-4 pl-6 font-bold text-slate-400">#EMP-${String.format("%03d", emp.id)}</td>

                                            <td class="p-4">
                                                <div class="flex items-center gap-3">
                                                    <div class="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 text-slate-600 flex items-center justify-center font-bold text-[11px] uppercase">
                                                        ${fn:substring(emp.fullName, 0, 2)}
                                                    </div>
                                                    <div>
                                                        <p class="font-bold text-slate-900">${emp.fullName}</p>
                                                        <p class="text-[10px] text-slate-400 font-normal mt-0.5">${emp.email != null ? emp.email : 'Chưa cập nhật email'}</p>
                                                        <p class="text-[10px] text-slate-500 font-medium flex items-center gap-1 mt-0.5">
                                                            <i class="fa-solid fa-phone text-[9px] text-slate-400"></i> ${emp.phone != null ? emp.phone : 'Chưa cập nhật SĐT'}
                                                        </p>
                                                    </div>
                                                </div>
                                            </td>

                                            <td class="p-4">
                                                <form action="${pageContext.request.contextPath}/admin/employees?action=quickRole&id=${emp.id}&page=${currentPage}" method="POST" class="flex items-center gap-2 m-0">
                                                    <select name="roleId" class="select-role text-[11px] font-bold px-2.5 py-1 rounded-lg border border-slate-200 bg-slate-50" ${emp.roleName eq 'System Admin' ? 'disabled' : ''}>
                                                        <c:forEach items="${roles}" var="role">
                                                            <option value="${role.roleId}" ${emp.roleName eq role.roleName ? 'selected' : ''}>
                                                                ${role.roleName}
                                                            </option>
                                                        </c:forEach>
                                                    </select>
                                                    <c:if test="${emp.roleName ne 'System Admin'}">
                                                        <button type="submit" class="text-cyan-600 hover:text-cyan-800 p-1" title="Lưu thay đổi chức vụ">
                                                            <i class="fa-solid fa-floppy-disk text-xs"></i>
                                                        </button>
                                                    </c:if>
                                                </form>
                                            </td>

                                            <td class="p-4">
                                                <c:choose>
                                                    <c:when test="${emp.roleName eq 'System Admin' || emp.status eq 'Đang quản lý'}">
                                                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-cyan-50 text-cyan-700 border border-cyan-100">
                                                            <i class="fa-solid fa-circle text-[5px]"></i> Đang quản lý
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold ${emp.status eq 'Đang làm' ? 'bg-emerald-50 text-emerald-600' : 'bg-orange-50 text-orange-600'}">
                                                            <i class="fa-solid fa-circle text-[5px]"></i> ${emp.status}
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>

                                            <td class="p-4">
                                                <form action="${pageContext.request.contextPath}/admin/employees?action=quickShift&page=${currentPage}" method="POST" class="m-0">
                                                    <input type="hidden" name="id" value="${emp.id}" />
                                                    <select name="shiftId" class="select-role text-[11px] font-bold px-2.5 py-1 rounded-lg border border-slate-200 bg-slate-50" onchange="this.form.submit()">
                                                        <option value="0" ${emp.shiftId == 0 or empty emp.shiftId ? 'selected' : ''}>Chưa xếp ca</option>
                                                        <c:forEach items="${shifts}" var="s">
                                                            <option value="${s.shiftId}" ${emp.shiftId == s.shiftId ? 'selected' : ''}>
                                                                ${s.shiftName} (${s.startTime} - ${s.endTime})
                                                            </option>
                                                        </c:forEach>
                                                    </select>
                                                </form>
                                            </td>

                                            <td class="p-4 pr-6 text-right">
                                                <div class="flex items-center justify-end gap-2.5 text-slate-400 text-sm">
                                                    <a href="${pageContext.request.contextPath}/admin/employees?action=edit&id=${emp.id}" class="hover:text-cyan-600 transition" title="Sửa thông tin">
                                                        <i class="fa-solid fa-user-pen text-xs"></i>
                                                    </a>
                                                    <c:choose>
                                                        <c:when test="${emp.roleName eq 'System Admin'}">
                                                            <span class="text-slate-200 cursor-not-allowed" title="Không cho phép khóa Admin hệ thống!"><i class="fa-solid fa-ban text-xs"></i></span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a href="${pageContext.request.contextPath}/admin/employees?action=toggle&id=${emp.id}&page=${currentPage}" class="hover:text-orange-500 transition" title="Thay đổi trạng thái">
                                                                <i class="fa-solid fa-ban text-xs"></i>
                                                            </a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    <c:choose>
                                                        <c:when test="${emp.roleName eq 'System Admin'}">
                                                            <span class="text-slate-200 cursor-not-allowed" title="Không cho phép xóa Admin hệ thống!"><i class="fa-solid fa-trash-can text-xs"></i></span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a href="javascript:void(0);" class="hover:text-rose-600 transition" onclick="confirmDelete(${emp.id}, '${emp.fullName}')" title="Xóa vĩnh viễn">
                                                                <i class="fa-solid fa-trash-can text-xs"></i>
                                                            </a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>

                                    <tr id="noResultsRow" class="hidden">
                                        <td colspan="6" class="p-8 text-center text-slate-400 font-medium bg-white">
                                            Không tìm thấy nhân viên nào khớp với tiêu chí lựa chọn bộ lọc.
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div class="px-6 py-4 border-t border-slate-100 flex items-center justify-between text-[13px] text-slate-600 bg-white select-none">
                            <div class="flex items-center gap-4">
                                <div class="flex items-center gap-2">
                                    <span class="text-slate-500 font-medium">Jump to page</span>
                                    <input type="number" 
                                           id="jumpPageInput"
                                           min="1" 
                                           max="${totalPages > 0 ? totalPages : 1}" 
                                           value="${currentPage}" 
                                           onkeydown="if(event.key === 'Enter') jumpToPage(this.value)"
                                           class="w-10 h-7 border border-slate-200 rounded text-center font-medium text-slate-800 focus:outline-none focus:border-slate-400 transition" />
                                </div>
                                <div class="text-slate-400 font-medium">
                                    <span class="text-slate-700 font-semibold">${currentPage}</span> of <span class="text-slate-700 font-semibold">${totalPages > 0 ? totalPages : 1}</span>
                                </div>
                            </div>

                            <div class="flex items-center gap-1.5">
                                <c:choose>
                                    <c:when test="${currentPage > 1}">
                                        <a href="${pageContext.request.contextPath}/admin/employees?page=1" class="w-7 h-7 border border-slate-200 rounded flex items-center justify-center text-slate-600 hover:bg-slate-50 transition" title="Trang đầu">
                                            <i class="fa-solid fa-backward-step text-[10px]"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/admin/employees?page=${currentPage - 1}" class="w-7 h-7 border border-slate-200 rounded flex items-center justify-center text-slate-600 hover:bg-slate-50 transition" title="Trang trước">
                                            <i class="fa-solid fa-chevron-left text-[10px]"></i>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="w-7 h-7 border border-slate-100 rounded flex items-center justify-center text-slate-300 bg-slate-50 cursor-not-allowed">
                                            <i class="fa-solid fa-backward-step text-[10px]"></i>
                                        </span>
                                        <span class="w-7 h-7 border border-slate-100 rounded flex items-center justify-center text-slate-300 bg-slate-50 cursor-not-allowed">
                                            <i class="fa-solid fa-chevron-left text-[10px]"></i>
                                        </span>
                                    </c:otherwise>
                                </c:choose>

                                <c:choose>
                                    <c:when test="${currentPage < totalPages}">
                                        <a href="${pageContext.request.contextPath}/admin/employees?page=${currentPage + 1}" class="w-7 h-7 border border-slate-200 rounded flex items-center justify-center text-slate-600 hover:bg-slate-50 transition" title="Trang sau">
                                            <i class="fa-solid fa-chevron-right text-[10px]"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/admin/employees?page=${totalPages}" class="w-7 h-7 border border-slate-200 rounded flex items-center justify-center text-slate-600 hover:bg-slate-50 transition" title="Trang cuối">
                                            <i class="fa-solid fa-forward-step text-[10px]"></i>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="w-7 h-7 border border-slate-100 rounded flex items-center justify-center text-slate-300 bg-slate-50 cursor-not-allowed">
                                            <i class="fa-solid fa-chevron-right text-[10px]"></i>
                                        </span>
                                        <span class="w-7 h-7 border border-slate-100 rounded flex items-center justify-center text-slate-300 bg-slate-50 cursor-not-allowed">
                                            <i class="fa-solid fa-forward-step text-[10px]"></i>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>

        <div id="addEmployeeModal" class="modal-overlay">
            <div class="bg-white w-full max-w-md p-6 rounded-2xl shadow-xl border border-slate-100 mx-4">
                <div class="flex justify-between items-center pb-3.5 border-b border-slate-100 mb-4">
                    <h3 class="text-sm font-bold text-slate-900">Thêm Nhân Viên Mới</h3>
                    <button type="button" class="text-slate-400 hover:text-slate-600 text-base" onclick="closeAddModal()"><i class="fa-solid fa-xmark"></i></button>
                </div>

                <form action="${pageContext.request.contextPath}/admin/employees" method="POST" class="space-y-3.5 text-xs">
                    <input type="hidden" name="action" value="insert">
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Tài khoản đăng nhập (Username) <span class="text-red-500">*</span></label>
                        <input type="text" name="username" class="w-full px-3 py-2 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-600/10 transition font-medium" required placeholder="Ví dụ: duypham0612" />
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Họ và Tên nhân viên <span class="text-red-500">*</span></label>
                        <input type="text" name="fullName" class="w-full px-3 py-2 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-600/10 transition font-medium" required placeholder="Ví dụ: Nguyễn Văn A">
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Địa chỉ Email <span class="text-red-500">*</span></label>
                        <input type="email" name="email" class="w-full px-3 py-2 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-600/10 transition font-medium" required placeholder="example@mycoffee.com">
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Số điện thoại <span class="text-red-500">*</span></label>
                        <input type="text" name="phone" class="w-full px-3 py-2 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-600/10 transition font-medium" required placeholder="Nhập số điện thoại...">
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Mật khẩu khởi tạo <span class="text-red-500">*</span></label>
                        <input type="password" name="password" class="w-full px-3 py-2 border border-slate-200 rounded-xl outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-600/10 transition font-medium" required placeholder="Mật khẩu đăng nhập ban đầu">
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Chức vụ ban đầu <span class="text-red-500">*</span></label>
                        <select name="roleId" class="w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50 outline-none focus:border-cyan-600 font-semibold" required>
                            <option value="">-- Chọn chức vụ --</option>
                            <c:forEach items="${roles}" var="role">
                                <option value="${role.roleId}">${role.roleName}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label class="block font-bold text-slate-500 mb-1">Ca làm việc ban đầu</label>
                        <select name="shiftId" class="w-full px-3 py-2 border border-slate-200 rounded-xl bg-slate-50 outline-none focus:border-cyan-600 font-semibold">
                            <option value="0">Chưa xếp ca (Mặc định)</option>
                            <c:forEach items="${shifts}" var="s">
                                <option value="${s.shiftId}">${s.shiftName} (${s.startTime} - ${s.endTime})</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
                        <button type="button" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl font-semibold transition" onclick="closeAddModal()">Hủy bỏ</button>
                        <button type="submit" class="px-4 py-2 bg-cyan-700 hover:bg-cyan-800 text-white rounded-xl font-bold shadow-sm transition">Xác nhận tạo</button>
                    </div>
                </form>
            </div>
        </div>

        <script>
            let currentSelectedRoleTab = 'all';

            function jumpToPage(pageVal) {
                const page = parseInt(pageVal);
                const maxPage = parseInt("${totalPages > 0 ? totalPages : 1}");
                if (page >= 1 && page <= maxPage) {
                    window.location.href = '${pageContext.request.contextPath}/admin/employees?page=' + page;
                } else {
                    alert('Số trang nhập vào không hợp lệ! Vui lòng nhập từ 1 đến ' + maxPage);
                    document.getElementById('jumpPageInput').value = "${currentPage}";
                }
            }

            function toggleAdvancedFilter() {
                const panel = document.getElementById('advancedFilterPanel');
                panel.classList.toggle('hidden');
            }

            function selectRoleTab(btnElement, role) {
                const buttons = document.querySelectorAll('#roleFilterContainer .tab-btn');
                buttons.forEach(btn => {
                    btn.classList.remove('bg-white', 'text-slate-800', 'shadow-sm');
                    btn.classList.add('hover:text-slate-800');
                });
                btnElement.classList.add('bg-white', 'text-slate-800', 'shadow-sm');

                currentSelectedRoleTab = role;

                const filterRoleSelect = document.getElementById('filterRole');
                if (role === 'all') {
                    filterRoleSelect.value = 'all';
                } else {
                    for (let i = 0; i < filterRoleSelect.options.length; i++) {
                        if (filterRoleSelect.options[i].value.includes(role)) {
                            filterRoleSelect.value = filterRoleSelect.options[i].value;
                            break;
                        }
                    }
                }
                filterEmployees();
            }

            function filterEmployees() {
                const globalSearch = document.getElementById('globalSearchInput').value.toLowerCase().trim();
                const fId = document.getElementById('filterId').value.toLowerCase().trim();
                const fName = document.getElementById('filterName').value.toLowerCase().trim();
                const fRole = document.getElementById('filterRole').value;
                const fStatus = document.getElementById('filterStatus').value;
                const fShift = document.getElementById('filterShift').value;
                const rows = document.querySelectorAll('.employee-row');
                let matchCount = 0;

                rows.forEach(row => {
                    const id = row.getAttribute('data-id');
                    const name = row.getAttribute('data-name');
                    const email = row.getAttribute('data-email');
                    const role = row.getAttribute('data-role');
                    const status = row.getAttribute('data-status');
                    const shift = row.getAttribute('data-shift');

                    let matchGlobal = true;
                    if (globalSearch !== '') {
                        matchGlobal = id.includes(globalSearch) || name.includes(globalSearch) || email.includes(globalSearch);
                    }

                    let matchRole = true;
                    let targetRoleCriteria = (fRole !== 'all') ? fRole : currentSelectedRoleTab;
                    if (targetRoleCriteria !== 'all') {
                        matchRole = role.toLowerCase().includes(targetRoleCriteria.toLowerCase());
                    }

                    let matchId = true;
                    if (fId !== '') {
                        matchId = id.includes(fId);
                    }

                    let matchName = true;
                    if (fName !== '') {
                        matchName = name.includes(fName);
                    }

                    let matchStatus = true;
                    if (fStatus !== 'all') {
                        if (fStatus === 'Đang làm') {
                            matchStatus = (status === 'Đang làm' || status === 'Đang quản lý');
                        } else if (fStatus === 'Nghỉ việc') {
                            matchStatus = (status === 'Nghỉ việc' || status === 'Tạm nghỉ');
                        }
                    }

                    let matchShift = true;
                    if (fShift !== 'all') {
                        matchShift = (shift === fShift);
                    }

                    if (matchGlobal && matchRole && matchId && matchName && matchStatus && matchShift) {
                        row.style.display = '';
                        matchCount++;
                    } else {
                        row.style.display = 'none';
                    }
                });

                const noResultsRow = document.getElementById('noResultsRow');
                if (matchCount === 0) {
                    noResultsRow.classList.remove('hidden');
                } else {
                    noResultsRow.classList.add('hidden');
                }
                
                // Cập nhật số lượng hiển thị thực tế nếu cần thiết thiết lập phần tử hiển thị (id: displayedCount)
                const displayedCountElem = document.getElementById('displayedCount');
                if (displayedCountElem) {
                    displayedCountElem.innerText = matchCount;
                }
            }

            function clearAdvancedFilters() {
                document.getElementById('filterId').value = '';
                document.getElementById('filterName').value = '';
                document.getElementById('filterRole').value = 'all';
                document.getElementById('filterStatus').value = 'all';
                document.getElementById('filterShift').value = 'all';
                document.getElementById('globalSearchInput').value = '';
                const firstTab = document.querySelector('#roleFilterContainer .tab-btn');
                selectRoleTab(firstTab, 'all');
            }

            function openAddModal() {
                document.getElementById('addEmployeeModal').classList.add('open');
            }
            function closeAddModal() {
                document.getElementById('addEmployeeModal').classList.remove('open');
            }
            function confirmDelete(id, name) {
                if (confirm('Bạn có chắc chắn muốn xóa vĩnh viễn nhân viên "' + name + '" ra khỏi cơ sở dữ liệu hệ thống? Action này không thể hoàn tác.')) {
                    window.location.href = '${pageContext.request.contextPath}/admin/employees?action=delete&id=' + id + '&page=${currentPage}';
                }
            }
        </script>
    </body>
</html>
