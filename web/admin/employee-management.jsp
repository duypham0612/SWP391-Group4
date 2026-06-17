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
                        primary: '#0e7490', // Màu xanh chủ đạo của hệ thống POS
                    }
                }
            }
        }
    </script>
    
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; }
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(15, 23, 42, 0.3); backdrop-filter: blur(4px);
            display: flex; justify-content: center; align-items: center; z-index: 9999;
            visibility: hidden; opacity: 0; transition: all 0.25s ease-in-out;
        }
        .modal-overlay.open { visibility: visible; opacity: 1; }
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
                
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-utensils text-base"></i> Thực đơn
                </a>
                <a href="#" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:text-slate-900 hover:bg-slate-50 rounded-xl font-medium text-xs transition-all">
                    <i class="fa-solid fa-boxes-stacked text-base"></i> Kho nguyên liệu
                </a>
                <a href="${pageContext.request.contextPath}/admin/employees" class="flex items-center gap-3 px-4 py-3 bg-cyan-50 text-cyan-700 rounded-xl font-semibold text-xs transition-all">
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
                LMQ
            </div>
            <div class="flex-1 min-w-0">
                <h4 class="text-xs font-bold text-slate-800 truncate">Lê Minh Quân</h4>
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
                    <h2 class="text-xl font-bold text-slate-900 tracking-tight">Quản lý Nhân viên</h2>
                    <p class="text-xs text-slate-400 font-medium mt-0.5">Theo dõi và quản lý đội ngũ nhân sự của quán.</p>
                </div>
                
                <div class="flex items-center gap-3 self-end sm:self-auto">
                    <div class="bg-white border border-slate-100 px-4 py-2 rounded-xl text-center shadow-sm min-w-[100px]">
                        <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tổng nhân sự</p>
                        <p class="text-sm font-bold text-slate-800">${fn:length(employees)}</p>
                    </div>
                    <div class="bg-white border border-slate-100 px-4 py-2 rounded-xl text-center shadow-sm min-w-[100px]">
                        <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Đang làm việc</p>
                        <p class="text-sm font-bold text-cyan-600">
                            <c:set var="activeCount" value="0"/>
                            <c:forEach items="${employees}" var="e">
                                <c:if test="${e.status eq 'Đang làm'}"><c:set var="activeCount" value="${activeCount + 1}"/></c:if>
                            </c:forEach>
                            ${activeCount}
                        </p>
                    </div>
                </div>
            </div>

            <div class="flex flex-col md:flex-row md:items-center justify-between gap-3 pt-2">
                <div class="flex bg-slate-100 p-1 rounded-xl w-fit text-xs font-semibold text-slate-500">
                    <button class="bg-white text-slate-800 px-4 py-1.5 rounded-lg shadow-sm">Tất cả</button>
                    <button class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition">Admin</button>
                    <button class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition">Manager</button>
                    <button class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition">Barista</button>
                    <button class="hover:text-slate-800 px-4 py-1.5 rounded-lg transition">Cashier</button>
                </div>
                
                <div class="flex items-center gap-2 self-end md:self-auto">
                    <button class="border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm">
                        <i class="fa-solid fa-sliders text-slate-400"></i> Bộ lọc nâng cao
                    </button>
                    <button class="border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm">
                        <i class="fa-solid fa-download text-slate-400"></i> Xuất báo cáo
                    </button>
                    <button type="button" class="bg-cyan-700 hover:bg-cyan-800 text-white px-4 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition" onclick="openAddModal()">
                        <i class="fa-solid fa-plus"></i> Thêm nhân viên
                    </button>
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
                    <i class="fa-solid fa-circle-exclamation text-base"></i> Không thể xóa nhân viên này vì dữ liệu đã bị ràng buộc lịch sử (Hóa đơn, Ca làm...) ở các bảng khác.
                </div>
            </c:if>
            <c:if test="${param.success eq 'true'}">
                <div class="bg-emerald-50 text-emerald-700 p-3.5 border border-emerald-100 rounded-xl text-xs font-medium flex items-center gap-2">
                    <i class="fa-solid fa-circle-check text-base"></i> Thêm nhân viên mới thành công!
                </div>
            </c:if>
            <c:if test="${param.error != null && !param.error.startsWith('update') && param.error != 'cannotChangeAdminRoot' && param.error != 'cannotDeleteAdmin' && param.error != 'deleteFailed'}">
                <div class="bg-rose-50 text-rose-700 p-3.5 border border-rose-100 rounded-xl text-xs flex items-center gap-2">
                    <i class="fa-solid fa-circle-exclamation text-base"></i> 
                    <strong>Lỗi:</strong> 
                    ${param.error == 'missingFields' ? 'Vui lòng nhập đầy đủ thông tin bắt buộc.' : 
                      param.error == 'duplicateUser' ? 'Tên tài khoản (Username) này đã tồn tại trên hệ thống.' :
                      param.error == 'duplicateEmail' ? 'Địa chỉ Email này đã được đăng ký trước đó.' :
                      param.error == 'duplicatePhone' ? 'Số điện thoại này đã được đăng ký trước đó.' :
                      param.error == 'adminExists' ? 'Hệ thống đã có 1 tài khoản Admin! Không thể tạo thêm Admin thứ hai.' :
                      param.error == 'invalidRole' ? 'Chức vụ không hợp lệ.' : 'Có lỗi xảy ra, vui lòng thử lại.'}
                </div>
            </c:if>
            <c:if test="${param.success eq 'updated'}">
                <div class="bg-emerald-50 text-emerald-700 p-3.5 border border-emerald-100 rounded-xl text-xs font-medium flex items-center gap-2">
                    <i class="fa-solid fa-circle-check text-base"></i> Cập nhật thông tin nhân viên thành công!
                </div>
            </c:if>
            <c:if test="${not empty param.error && param.error != 'cannotDeleteAdmin' && param.error != 'deleteFailed' && !param.error.startsWith('missing') && param.error != 'duplicateUser' && param.error != 'duplicateEmail' && param.error != 'duplicatePhone' && param.error != 'adminExists'}">
                <div class="bg-rose-50 text-rose-700 p-3.5 border border-rose-100 rounded-xl text-xs">
                    <c:choose>
                        <c:when test="${param.error eq 'cannotChangeAdminRoot'}"><strong>Lỗi hệ thống:</strong> Không thể hạ cấp chức vụ của tài khoản Admin tối cao!</c:when>
                        <c:when test="${param.error eq 'updateDuplicateAdmin' || param.error eq 'duplicateAdmin'}"><strong>Lỗi cập nhật:</strong> Hệ thống đã có Admin khác!</c:when>
                        <c:when test="${param.error eq 'updateDuplicateUser' || param.error eq 'duplicateUser'}"><strong>Lỗi cập nhật:</strong> Tên tài khoản đã tồn tại!</c:when>
                        <c:when test="${param.error eq 'updateDuplicateEmail' || param.error eq 'duplicateEmail'}"><strong>Lỗi cập nhật:</strong> Địa chỉ Email đã được sử dụng!</c:when>
                        <c:when test="${param.error eq 'updateDuplicatePhone' || param.error eq 'duplicatePhone'}"><strong>Lỗi cập nhật:</strong> Số điện thoại đã tồn tại!</c:when>
                        <c:otherwise><strong>Lỗi hệ thống:</strong> Quá trình xử lý dữ liệu thất bại!</c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <div class="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-xs whitespace-nowrap">
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
                                <tr class="hover:bg-slate-50/50 transition-colors">
                                    <td class="p-4 pl-6 font-bold text-slate-400">#EMP-${String.format("%03d", emp.id)}</td>
                                    
                                    <td class="p-4">
                                        <div class="flex items-center gap-3">
                                            <div class="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 text-slate-600 flex items-center justify-center font-bold text-[11px] uppercase">
                                                ${fn:substring(emp.fullName, 0, 2)}
                                            </div>
                                            <div>
                                                <p class="font-bold text-slate-900">${emp.fullName}</p>
                                                <p class="text-[10px] text-slate-400 font-normal mt-0.5">${emp.email != null ? emp.email : 'Chưa cập nhật email'}</p>
                                            </div>
                                        </div>
                                    </td>
                                    
                                    <td class="p-4">
                                        <form action="${pageContext.request.contextPath}/admin/employees?action=quickRole&id=${emp.id}" method="POST" class="flex items-center gap-2 m-0">
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
                                        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold ${emp.status eq 'Đang làm' ? 'bg-emerald-50 text-emerald-600' : 'bg-orange-50 text-orange-600'}">
                                            <i class="fa-solid fa-circle text-[5px]"></i> ${emp.status}
                                        </span>
                                    </td>
                                    
                                    <td class="p-4">
                                        <form action="${pageContext.request.contextPath}/admin/employees?action=quickShift" method="POST" class="m-0">
                                            <input type="hidden" name="id" value="${emp.id}" />
                                            <select name="shiftId" class="select-role text-[11px] font-bold px-2.5 py-1 rounded-lg border border-slate-200 bg-slate-50" onchange="this.form.submit()">
                                                <option value="0" ${emp.shiftId == 0 ? 'selected' : ''}>Chưa xếp ca</option>
                                                <option value="1" ${emp.shiftId == 1 ? 'selected' : ''}>Ca sáng (7h30 - 12h)</option>
                                                <option value="2" ${emp.shiftId == 2 ? 'selected' : ''}>Ca chiều (12h - 18h)</option>
                                                <option value="3" ${emp.shiftId == 3 ? 'selected' : ''}>Ca tối (18h - 22h30)</option>
                                            </select>
                                        </form>
                                    </td>
                                    
                                    <td class="p-4 pr-6 text-right">
                                        <div class="flex items-center justify-end gap-2.5 text-slate-400 text-sm">
                                            <a href="${pageContext.request.contextPath}/admin/employees?action=edit&id=${emp.id}" class="hover:text-cyan-600 transition" title="Sửa thông tin">
                                                <i class="fa-solid fa-user-pen text-xs"></i>
                                            </a>
                                            
                                            <c:choose>
                                                <c:when test="${emp.status eq 'Đang làm'}">
                                                    <a href="${pageContext.request.contextPath}/admin/employees?action=toggle&id=${emp.id}" class="hover:text-orange-500 transition" onclick="return confirm('Xác nhận KHÓA tài khoản nhân viên này?')" title="Tạm dừng/Khóa">
                                                        <i class="fa-solid fa-ban text-xs"></i>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="${pageContext.request.contextPath}/admin/employees?action=toggle&id=${emp.id}" class="hover:text-emerald-600 transition" onclick="return confirm('Xác nhận MỞ KHÓA tài khoản làm việc này?')" title="Mở khóa hoạt động">
                                                        <i class="fa-solid fa-circle-check text-xs"></i>
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>

                                            <c:choose>
                                                <c:when test="${emp.roleName eq 'System Admin'}">
                                                    <span class="text-slate-200 cursor-not-allowed" title="Không cho phép xóa Admin hệ thống!">
                                                        <i class="fa-solid fa-trash-can text-xs"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="javascript:void(0);" class="hover:text-rose-600 transition" onclick="confirmDelete(${emp.id}, '${emp.fullName}')" title="Xóa vĩnh viễn khỏi DB">
                                                        <i class="fa-solid fa-trash-can text-xs"></i>
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            
                            <c:if test="${empty employees}">
                                <tr>
                                    <td colspan="6" class="p-8 text-center text-slate-400 font-medium">
                                        Không tìm thấy dữ liệu nhân viên nào trong database của hệ thống.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
                
                <div class="px-6 py-3 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-slate-500 bg-slate-50/50">
                    <div>Hiển thị <span class="text-slate-800">1-${fn:length(employees)}</span> trong số <span class="text-slate-800">${fn:length(employees)}</span> nhân viên</div>
                    <div class="flex items-center gap-1">
                        <button class="w-7 h-7 rounded-lg border border-slate-200 flex items-center justify-center bg-white hover:bg-slate-50"><i class="fa-solid fa-chevron-left text-[10px]"></i></button>
                        <button class="w-7 h-7 rounded-lg bg-cyan-700 text-white flex items-center justify-center shadow-sm shadow-cyan-700/10">1</button>
                        <button class="w-7 h-7 rounded-lg border border-slate-200 flex items-center justify-center bg-white hover:bg-slate-50">2</button>
                        <button class="w-7 h-7 rounded-lg border border-slate-200 flex items-center justify-center bg-white hover:bg-slate-50">3</button>
                        <button class="w-7 h-7 rounded-lg border border-slate-200 flex items-center justify-center bg-white hover:bg-slate-50"><i class="fa-solid fa-chevron-right text-[10px]"></i></button>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div class="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-100 shadow-sm space-y-4">
                    <div>
                        <h3 class="text-sm font-bold text-slate-900">Phân bổ nhân sự hôm nay</h3>
                        <p class="text-[11px] text-slate-400 font-medium">Trực quan hóa lượng nhân viên theo khung giờ.</p>
                    </div>
                    <div class="h-40 flex items-end justify-between gap-4 pt-4 px-2">
                        <div class="w-full flex flex-col items-center gap-2">
                            <div class="w-full bg-slate-200 rounded-t-lg transition-all duration-500" style="height: 35%;"></div>
                            <span class="text-[10px] text-slate-400 font-bold">06:00</span>
                        </div>
                        <div class="w-full flex flex-col items-center gap-2">
                            <div class="w-full bg-slate-300 rounded-t-lg transition-all duration-500" style="height: 60%;"></div>
                            <span class="text-[10px] text-slate-400 font-bold">10:00</span>
                        </div>
                        <div class="w-full flex flex-col items-center gap-2">
                            <div class="w-full bg-cyan-700 rounded-t-lg shadow-md shadow-cyan-700/10 transition-all duration-500" style="height: 90%;"></div>
                            <span class="text-[10px] text-slate-400 font-bold">14:00</span>
                        </div>
                        <div class="w-full flex flex-col items-center gap-2">
                            <div class="w-full bg-slate-300 rounded-t-lg transition-all duration-500" style="height: 50%;"></div>
                            <span class="text-[10px] text-slate-400 font-bold">18:00</span>
                        </div>
                        <div class="w-full flex flex-col items-center gap-2">
                            <div class="w-full bg-slate-200 rounded-t-lg transition-all duration-500" style="height: 40%;"></div>
                            <span class="text-[10px] text-slate-400 font-bold">22:00</span>
                        </div>
                    </div>
                </div>
                
                <div class="bg-cyan-50/50 border border-cyan-100/60 p-6 rounded-2xl flex flex-col justify-between">
                    <div class="space-y-2">
                        <div class="w-8 h-8 rounded-lg bg-cyan-100 text-cyan-700 flex items-center justify-center text-sm"><i class="fa-regular fa-calendar-check"></i></div>
                        <h4 class="text-xs font-bold text-slate-900">Ghi chú vận hành</h4>
                        <p class="text-xs text-slate-500 leading-relaxed font-medium">Sắp tới có lịch đào tạo kỹ năng Latte Art cho nhân viên Barista vào sáng Thứ Tư tuần sau. Vui lòng sắp xếp ca làm việc hợp lý.</p>
                    </div>
                    <button class="w-full mt-4 bg-cyan-700 hover:bg-cyan-800 text-white text-xs font-bold py-2 rounded-xl transition shadow-sm shadow-cyan-700/10">Xem lịch chi tiết</button>
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
            
            <div class="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
                <button type="button" class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl font-semibold transition" onclick="closeAddModal()">Hủy bỏ</button>
                <button type="submit" class="px-4 py-2 bg-cyan-700 hover:bg-cyan-800 text-white rounded-xl font-bold shadow-sm transition">Xác nhận tạo</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openAddModal() {
        document.getElementById('addEmployeeModal').classList.add('open');
    }
    function closeAddModal() {
        document.getElementById('addEmployeeModal').classList.remove('open');
    }
    function confirmDelete(empId, empName) {
        if (confirm("Bạn có chắc chắn muốn XÓA VĨNH VIỄN nhân viên '" + empName + "' khỏi hệ thống? Hành động này không thể hoàn tác!")) {
            window.location.href = "${pageContext.request.contextPath}/admin/employees?action=delete&id=" + empId;
        }
    }
</script>
</body>
</html>
