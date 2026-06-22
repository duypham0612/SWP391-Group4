<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản Lý Thực Đơn - Coffee POS</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <script>
            tailwind.config = {
                theme: {
                    extend: {
                        fontFamily: {sans: ['Plus Jakarta Sans', 'sans-serif']}
                    }
                }
            }
        </script>
        <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <style>
            body { font-family: 'Plus Jakarta Sans', sans-serif; }
            input[type=number]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
        </style>
    </head>
    <body class="bg-[#f8fafc] text-slate-800 antialiased">
        <div class="min-h-screen flex">
            <aside class="w-64 bg-white border-r border-slate-100 p-5 flex flex-col justify-between sticky top-0 h-screen shrink-0">
                <div>
                    <div class="flex items-center gap-3 px-2 py-3 mb-6">
                        <div class="w-9 h-9 rounded-xl bg-cyan-700 flex items-center justify-center text-white shadow-md">
                            <i class="fa-solid fa-mug-hot text-lg"></i>
                        </div>
                        <div>
                            <h1 class="text-sm font-bold text-slate-900 tracking-tight">Coffee POS</h1>
                            <p class="text-[10px] text-slate-400 font-medium">Hệ thống quản lý</p>
                        </div>
                    </div>
                    <nav class="space-y-1">
                        <a href="${pageContext.request.contextPath}/admin/admin-dashboard" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
                            <i class="fa-solid fa-chart-pie text-base"></i> Tổng quan
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/menu" class="flex items-center gap-3 px-4 py-3 bg-cyan-50 text-cyan-700 rounded-xl font-semibold text-xs transition">
                            <i class="fa-solid fa-utensils text-base"></i> Thực đơn
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/inventory" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
                            <i class="fa-solid fa-boxes-stacked text-base"></i> Kho nguyên liệu
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/employees" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
                            <i class="fa-solid fa-user-group text-base"></i> Nhân viên
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/vouchers" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
                            <i class="fa-solid fa-tags text-base"></i> Khuyến mãi
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/reports" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
                            <i class="fa-solid fa-chart-line text-base"></i> Báo cáo
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/settings" class="flex items-center gap-3 px-4 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium text-xs transition">
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

            <main class="flex-1 flex flex-col min-w-0">
                <header class="h-14 bg-white border-b border-slate-100 px-8 flex items-center justify-end sticky top-0 z-40">
                    <div class="flex items-center gap-4">
                        <a href="${pageContext.request.contextPath}/login?action=logout" class="text-rose-600 border border-rose-100 bg-rose-50/50 hover:bg-rose-50 px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition">
                            Đăng xuất <i class="fa-solid fa-arrow-right-from-bracket"></i>
                        </a>
                    </div>
                </header>

                <div class="p-8 space-y-6 max-w-[1400px] w-full mx-auto flex-1 flex flex-col justify-between">
                    <div class="space-y-6">
                        <div class="flex items-center justify-between">
                            <div>
                                <div class="text-[10px] font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1">Hệ thống <i class="fa-solid fa-chevron-right text-[8px]"></i> Quản lý thực đơn</div>
                                <h2 class="text-2xl font-bold text-slate-900 tracking-tight mt-1">Quản lý Menu</h2>
                                <p class="text-xs text-slate-400 font-medium">Các món ăn, đồ uống và danh mục trong hệ thống.</p>
                            </div>
                            <button onclick="openAddModal()" type="button" class="bg-[#0e7490] hover:bg-cyan-800 text-white px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition">
                                <i class="fa-solid fa-circle-plus text-sm"></i> Thêm món mới
                            </button>
                        </div>

                        <div class="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
                            <div class="lg:col-span-3 bg-white p-4 rounded-2xl border border-slate-100 shadow-sm flex flex-col gap-3">
                                <div class="flex items-center justify-between">
                                    <span class="text-xs font-bold text-slate-900">Danh mục món</span>
                                    <button onclick="openCategoryModal()" type="button" class="text-xs text-cyan-700 hover:text-cyan-800 font-semibold flex items-center gap-1">
                                        <i class="fa-solid fa-pen-to-square text-[10px]"></i> Quản lý danh mục
                                    </button>
                                </div>
                                <div class="flex items-center gap-2 overflow-x-auto pb-1" id="categoryTabs">
                                    <button onclick="filterCategory('all')" data-cat="all" class="cat-tab px-4 py-3 bg-cyan-500 text-white font-bold rounded-xl text-xs flex flex-col items-center gap-2 min-w-[70px] shadow-sm transition">
                                        <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-sm"><i class="fa-solid fa-border-all"></i></div>
                                        <span>Tất cả</span>
                                    </button>

                                    <c:forEach items="${categories}" var="cat">
                                        <button onclick="filterCategory('${cat.categoryId}')" data-cat="${cat.categoryId}" class="cat-tab px-4 py-3 bg-slate-50 text-slate-500 hover:bg-slate-100 font-medium rounded-xl text-xs flex flex-col items-center gap-2 min-w-[70px] transition">
                                            <div class="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-sm text-slate-600">
                                                <c:choose>
                                                    <c:when test="${cat.categoryId == 1}"><i class="fa-solid fa-mug-hot"></i></c:when>
                                                    <c:when test="${cat.categoryId == 2}"><i class="fa-solid fa-leaf"></i></c:when>
                                                    <c:when test="${cat.categoryId == 3}"><i class="fa-solid fa-snowflake"></i></c:when>
                                                    <c:when test="${cat.categoryId == 4}"><i class="fa-solid fa-cake-candles"></i></c:when>
                                                    <c:otherwise><i class="fa-solid fa-utensils"></i></c:otherwise>
                                                </c:choose>
                                            </div>
                                            <span>${cat.categoryName}</span>
                                        </button>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex justify-between items-center h-[108px]">
                                <div>
                                    <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">Tổng số món</p>
                                    <h3 class="text-3xl font-black text-slate-800 mt-0.5">${totalProducts}</h3>
                                </div>
                                <div class="text-right">
                                    <span class="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2 py-1 rounded-md">
                                        +${not empty newProductsCount ? newProductsCount : 0} món mới
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div class="flex items-center justify-between text-xs font-semibold text-slate-500">
                            <div class="flex items-center gap-2">
                                <span class="bg-white px-3 py-1.5 rounded-lg border border-slate-200 text-cyan-700">Lưới</span>
                                <span class="bg-slate-100 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-slate-200 transition">Danh sách</span>

                                <form id="searchForm" action="${pageContext.request.contextPath}/admin/menu" method="GET" class="relative w-64 mx-1">
                                    <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                                        <i class="fa-solid fa-magnifying-glass text-xs"></i>
                                    </span>
                                    <input id="menuSearch" name="search" value="${param.search}" type="text" autocomplete="off" placeholder="Tìm nhanh tên món ăn, đồ uống..." class="w-full pl-8 pr-4 py-1.5 text-xs bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-600/20 transition">
                                </form>
                                <button class="bg-white border border-slate-200 px-3 py-1.5 rounded-xl flex items-center gap-1.5"><i class="fa-solid fa-sliders text-[10px]"></i> Lọc theo trạng thái</button>
                            </div>
                            <div id="pagination-info" class="text-slate-400 font-medium">Đang hiển thị trang <span class="text-slate-700 font-bold">${currentPage}/${totalPages}</span></div>
                        </div>

                        <div id="menu-products-container" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                            <c:forEach items="${products}" var="p">
                                <div class="product-card bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden relative flex flex-col justify-between group" 
                                     data-name="${fn:toLowerCase(p.productName)}" 
                                     data-category="${p.categoryId}">
                                    <div>
                                        <div class="h-44 w-full bg-slate-100 relative overflow-hidden">
                                            <img src="${not empty p.imageUrl ? p.imageUrl : 'https://placehold.co/400x300?text=No+Image'}" class="w-full h-full object-cover group-hover:scale-105 transition duration-300" alt="${p.productName}">
                                            <span class="absolute top-3 left-3 bg-black/40 text-white font-bold text-[9px] px-2 py-0.5 rounded-md uppercase tracking-wider backdrop-blur-sm">
                                                <c:forEach items="${categories}" var="cat">
                                                    <c:if test="${p.categoryId == cat.categoryId}">${cat.categoryName}</c:if>
                                                </c:forEach>
                                            </span>

                                            <div class="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition flex items-center justify-center gap-2">
                                                <a href="${pageContext.request.contextPath}/admin/menu?action=toggle&id=${p.productId}&status=${p.available}&page=${currentPage}&search=${param.search}" class="w-8 h-8 bg-white rounded-xl flex items-center justify-center text-slate-700 hover:text-amber-500 shadow-md transition" title="Đổi trạng thái kho">
                                                    <i class="fa-solid fa-arrows-rotate text-xs"></i>
                                                </a>
                                                <button onclick="openEditModal(${p.productId}, '${fn:escapeXml(p.productName)}', ${p.categoryId}, ${p.basePrice}, '${fn:escapeXml(p.imageUrl)}', '${fn:escapeXml(p.description)}')" class="w-8 h-8 bg-white rounded-xl flex items-center justify-center text-slate-700 hover:text-cyan-600 shadow-md transition" title="Chỉnh sửa món">
                                                    <i class="fa-solid fa-pencil text-xs"></i>
                                                </button>
                                                <button onclick="confirmDelete(${p.productId}, '${fn:escapeXml(p.productName)}')" class="w-8 h-8 bg-white rounded-xl flex items-center justify-center text-slate-700 hover:text-rose-600 shadow-md transition" title="Xóa món">
                                                    <i class="fa-solid fa-trash-can text-xs"></i>
                                                </button>
                                            </div>
                                        </div>

                                        <div class="p-4 space-y-1">
                                            <div class="flex items-center justify-between gap-2">
                                                <h4 class="font-bold text-slate-900 text-xs truncate max-w-[80%]">${p.productName}</h4>
                                                <c:if test="${p.available}"><i class="fa-solid fa-circle-check text-emerald-500 text-xs"></i></c:if>
                                            </div>
                                            <p class="text-[10px] text-slate-400 line-clamp-2 h-7 font-medium leading-relaxed">${p.description}</p>
                                        </div>
                                    </div>

                                    <div class="px-4 pb-4 pt-2 flex items-center justify-between border-t border-slate-50">
                                        <span class="text-xs font-extrabold text-cyan-700">
                                            <fmt:formatNumber value="${p.basePrice}" type="currency" currencySymbol="" maxFractionDigits="0"/>₫
                                        </span>
                                        <span class="inline-flex items-center gap-1 text-[9px] font-bold ${p.available ? 'text-emerald-600 bg-emerald-50' : 'text-rose-600 bg-rose-50'} px-2 py-0.5 rounded-full">
                                            <i class="fa-solid fa-circle text-[4px]"></i> ${p.available ? 'Còn hàng' : 'Hết hàng'}
                                        </span>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <div id="pagination-container">
                        <c:if test="${totalPages > 1}">
                            <div class="flex items-center justify-center gap-1.5 pt-6 border-t border-slate-100 mt-6 select-none">
                                <a href="${pageContext.request.contextPath}/admin/menu?page=${currentPage - 1}&search=${param.search}" 
                                   class="w-8 h-8 rounded-xl border border-slate-200 bg-white flex items-center justify-center text-slate-500 hover:bg-slate-50 text-xs font-bold transition ${currentPage == 1 ? 'pointer-events-none opacity-40' : ''}">
                                    <i class="fa-solid fa-chevron-left text-[10px]"></i>
                                </a>
                                <a href="${pageContext.request.contextPath}/admin/menu?page=1&search=${param.search}" 
                                   class="w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold transition ${currentPage == 1 ? 'bg-cyan-700 text-white shadow-sm' : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'}">
                                    1
                                </a>
                                <c:if test="${currentPage > 4}">
                                    <span class="w-8 h-8 flex items-center justify-center text-slate-400 text-xs font-bold">...</span>
                                </c:if>
                                <c:forEach begin="${currentPage - 2 < 2 ? 2 : currentPage - 2}" 
                                           end="${currentPage + 2 >= totalPages ? totalPages - 1 : currentPage + 2}" 
                                           var="i">
                                    <a href="${pageContext.request.contextPath}/admin/menu?page=${i}&search=${param.search}" 
                                       class="w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold transition ${currentPage == i ? 'bg-cyan-700 text-white shadow-sm' : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'}">
                                        ${i}
                                    </a>
                                </c:forEach>
                                <c:if test="${currentPage < totalPages - 3}">
                                    <span class="w-8 h-8 flex items-center justify-center text-slate-400 text-xs font-bold">...</span>
                                </c:if>
                                <c:if test="${totalPages > 1}">
                                    <a href="${pageContext.request.contextPath}/admin/menu?page=${totalPages}&search=${param.search}" 
                                       class="w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold transition ${currentPage == totalPages ? 'bg-cyan-700 text-white shadow-sm' : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'}">
                                        ${totalPages}
                                    </a>
                                </c:if>
                                <a href="${pageContext.request.contextPath}/admin/menu?page=${currentPage + 1}&search=${param.search}" 
                                   class="w-8 h-8 rounded-xl border border-slate-200 bg-white flex items-center justify-center text-slate-500 hover:bg-slate-50 text-xs font-bold transition ${currentPage == totalPages ? 'pointer-events-none opacity-40' : ''}">
                                    <i class="fa-solid fa-chevron-right text-[10px]"></i>
                                </a>
                            </div>
                        </c:if>
                    </div>
                </div>
            </main>
        </div>

        <div id="productModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center hidden">
            <div class="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-100">
                <div class="flex items-center justify-between mb-4">
                    <h3 id="modalTitle" class="text-sm font-bold text-slate-900">Thêm sản phẩm mới vào thực đơn</h3>
                    <button onclick="closeModal()" class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-xmark"></i></button>
                </div>
                <form action="${pageContext.request.contextPath}/admin/menu" method="POST" onsubmit="return validateForm()" class="space-y-4 text-xs">
                    <input type="hidden" id="formAction" name="action" value="add">
                    <input type="hidden" id="formProductId" name="productId" value="">

                    <div>
                        <label class="block font-semibold text-slate-700 mb-1">Tên sản phẩm món *</label>
                        <input type="text" id="formProductName" name="productName" required class="w-full border border-slate-200 rounded-xl px-3 py-2 focus:outline-none focus:border-cyan-600">
                    </div>
                    <div>
                        <label class="block font-semibold text-slate-700 mb-1">Danh mục sản phẩm *</label>
                        <select id="formCategoryId" name="categoryID" required class="w-full border border-slate-200 rounded-xl px-3 py-2 focus:outline-none focus:border-cyan-600 bg-white">
                            <c:forEach items="${categories}" var="cat">
                                <option value="${cat.categoryId}">${cat.categoryName}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label class="block font-semibold text-slate-700 mb-1">Mô tả món ăn</label>
                        <textarea id="formDescription" name="description" rows="2" class="w-full border border-slate-200 rounded-xl px-3 py-2 focus:outline-none focus:border-cyan-600"></textarea>
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block font-semibold text-slate-700 mb-1">Giá bán cơ bản (₫) *</label>
                            <input type="number" id="formBasePrice" name="basePrice" min="20000" required class="w-full border border-slate-200 rounded-xl px-3 py-2 focus:outline-none focus:border-cyan-600">
                        </div>
                        <div id="availabilityWrapper">
                            <label class="block font-semibold text-slate-700 mb-1">Trạng thái kho</label>
                            <div class="flex items-center h-9">
                                <label class="inline-flex items-center cursor-pointer select-none">
                                    <input type="checkbox" id="isAvailable" name="isAvailable" value="true" checked class="sr-only peer">
                                    <div class="w-9 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-cyan-700 relative"></div>
                                    <span class="ml-2 text-slate-600 font-medium">Sẵn sàng bán</span>
                                </label>
                            </div>
                        </div>
                    </div>
                    <div>
                        <label class="block font-semibold text-slate-700 mb-1">Đường dẫn hình ảnh (URL)</label>
                        <input type="url" id="formImageUrl" name="imageURL" placeholder="https://..." class="w-full border border-slate-200 rounded-xl px-3 py-2 focus:outline-none focus:border-cyan-600">
                    </div>
                    <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
                        <button type="button" onclick="closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 font-medium text-slate-600 hover:bg-slate-50">Hủy</button>
                        <button type="submit" id="submitBtn" class="px-4 py-2 rounded-xl bg-cyan-700 hover:bg-cyan-800 font-bold text-white shadow-sm">Thêm vào Menu</button>
                    </div>
                </form>
            </div>
        </div>

        <div id="categoryModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center hidden">
            <div class="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-slate-100 flex flex-col gap-4">
                <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                    <h3 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                        <i class="fa-solid fa-folder-tree text-cyan-700"></i> Danh mục món ăn
                    </h3>
                    <button onclick="closeCategoryModal()" class="text-slate-400 hover:text-slate-600"><i class="fa-solid fa-xmark text-sm"></i></button>
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-5 gap-6">
                    <form id="catForm" action="${pageContext.request.contextPath}/admin/menu" method="POST" class="md:col-span-2 space-y-3 text-xs bg-slate-50/50 p-3 rounded-xl border border-slate-100">
                        <input type="hidden" id="catAction" name="action" value="addCategory">
                        <input type="hidden" id="catId" name="categoryId" value="">
                        
                        <div class="font-bold text-slate-800 text-[11px] uppercase tracking-wider mb-1" id="catFormTitle">Thêm danh mục</div>
                        <div>
                            <label class="block font-semibold text-slate-600 mb-1">Tên danh mục *</label>
                            <input type="text" id="catName" name="categoryName" required class="w-full border border-slate-200 rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-cyan-600 bg-white">
                        </div>
                        <div>
                            <label class="block font-semibold text-slate-600 mb-1">Mô tả chi tiết</label>
                            <textarea id="catDesc" name="catDescription" rows="3" class="w-full border border-slate-200 rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-cyan-600 bg-white"></textarea>
                        </div>
                        <div class="flex items-center gap-1.5 pt-1">
                            <button type="submit" id="catSubmitBtn" class="w-full py-2 rounded-lg bg-cyan-700 hover:bg-cyan-800 font-bold text-white shadow-sm">Lưu dữ liệu</button>
                            <button type="button" id="catResetBtn" onclick="resetCatForm()" class="px-2.5 py-2 rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-100 hidden" title="Hủy chỉnh sửa"><i class="fa-solid fa-arrow-rotate-left"></i></button>
                        </div>
                    </form>
                    
                    <div class="md:col-span-3 text-xs overflow-y-auto max-h-[280px] pr-1">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="text-[10px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-100">
                                    <th class="pb-2">Tên nhóm</th>
                                    <th class="pb-2">Mô tả</th>
                                    <th class="pb-2 text-right">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-50">
                                <c:forEach items="${categories}" var="cat">
                                    <tr class="hover:bg-slate-50/50">
                                        <td class="py-2.5 font-bold text-slate-800">${cat.categoryName}</td>
                                        <td class="py-2.5 text-slate-400 truncate max-w-[120px] font-medium" title="${cat.description}">${cat.description}</td>
                                        <td class="py-2.5 text-right space-x-1.5">
                                            <button onclick="editCategory(${cat.categoryId}, '${fn:escapeXml(cat.categoryName)}', '${fn:escapeXml(cat.description)}')" type="button" class="text-cyan-600 hover:text-cyan-800 font-semibold"><i class="fa-solid fa-pen"></i></button>
                                            <button onclick="deleteCategory(${cat.categoryId}, '${fn:escapeXml(cat.categoryName)}')" type="button" class="text-rose-600 hover:text-rose-800 font-semibold"><i class="fa-solid fa-trash"></i></button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <script>
            function openAddModal() {
                document.getElementById('modalTitle').innerText = "Thêm sản phẩm mới vào thực đơn";
                document.getElementById('formAction').value = "add";
                document.getElementById('formProductId').value = "";
                document.getElementById('formProductName').value = "";
                const firstOption = document.querySelector('#formCategoryId option');
                if (firstOption) document.getElementById('formCategoryId').value = firstOption.value;
                document.getElementById('formDescription').value = "";
                document.getElementById('formBasePrice').value = "";
                document.getElementById('formImageUrl').value = "";
                document.getElementById('availabilityWrapper').style.display = "block";
                document.getElementById('submitBtn').innerText = "Thêm vào Menu";
                document.getElementById('productModal').classList.remove('hidden');
            }

            function openEditModal(id, name, catId, price, imgUrl, desc) {
                document.getElementById('modalTitle').innerText = "Chỉnh sửa thông tin món ăn";
                document.getElementById('formAction').value = "edit";
                document.getElementById('formProductId').value = id;
                document.getElementById('formProductName').value = name;
                document.getElementById('formCategoryId').value = catId;
                document.getElementById('formDescription').value = desc;
                document.getElementById('formBasePrice').value = Math.round(price);
                document.getElementById('formImageUrl').value = imgUrl;
                document.getElementById('availabilityWrapper').style.display = "none";
                document.getElementById('submitBtn').innerText = "Lưu thay đổi";
                document.getElementById('productModal').classList.remove('hidden');
            }

            function closeModal() { document.getElementById('productModal').classList.add('hidden'); }

            function confirmDelete(id, name) {
                if (confirm('Bạn có chắc chắn muốn xóa món "' + name + '" khỏi database?')) {
                    window.location.href = '${pageContext.request.contextPath}/admin/menu?action=delete&id=' + id;
                }
            }

            function validateForm() {
                const priceInput = document.getElementById('formBasePrice');
                const priceValue = parseFloat(priceInput.value);
                if (isNaN(priceValue) || priceValue <= 20000) {
                    alert('⚠️ Lỗi: Giá bán cơ bản của món phải lớn hơn 20.000₫.');
                    priceInput.focus();
                    return false;
                }
                return true;
            }

            function filterCategory(catId) {
                document.querySelectorAll('.cat-tab').forEach(btn => {
                    if (btn.getAttribute('data-cat') == catId) {
                        btn.className = "cat-tab px-4 py-3 bg-cyan-500 text-white font-bold rounded-xl text-xs flex flex-col items-center gap-2 min-w-[70px] shadow-sm transition";
                        btn.querySelector('div').className = "w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-sm";
                    } else {
                        btn.className = "cat-tab px-4 py-3 bg-slate-50 text-slate-500 hover:bg-slate-100 font-medium rounded-xl text-xs flex flex-col items-center gap-2 min-w-[70px] transition";
                        btn.querySelector('div').className = "w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-sm text-slate-600";
                    }
                });
                let cards = document.querySelectorAll('.product-card');
                cards.forEach(card => {
                    let productCat = card.getAttribute('data-category');
                    if (catId === 'all' || productCat == catId) {
                        card.style.display = '';
                    } else {
                        card.style.display = 'none';
                    }
                });
            }

            function openCategoryModal() {
                resetCatForm();
                document.getElementById('categoryModal').classList.remove('hidden');
            }

            function closeCategoryModal() { document.getElementById('categoryModal').classList.add('hidden'); }

            function editCategory(id, name, desc) {
                document.getElementById('catFormTitle').innerText = "Chỉnh sửa danh mục";
                document.getElementById('catAction').value = "editCategory";
                document.getElementById('catId').value = id;
                document.getElementById('catName').value = name;
                document.getElementById('catDesc').value = desc;
                document.getElementById('catResetBtn').classList.remove('hidden');
            }

            function resetCatForm() {
                document.getElementById('catFormTitle').innerText = "Thêm danh mục";
                document.getElementById('catAction').value = "addCategory";
                document.getElementById('catId').value = "";
                document.getElementById('catName').value = "";
                document.getElementById('catDesc').value = "";
                document.getElementById('catResetBtn').classList.add('hidden');
            }

            function deleteCategory(id, name) {
                if (confirm('Bạn có chắc chắn muốn xóa danh mục "' + name + '"?\nHành động này có thể lỗi nếu có món ăn đang thuộc danh mục này.')) {
                    window.location.href = '${pageContext.request.contextPath}/admin/menu?action=deleteCategory&catId=' + id;
                }
            }

            // --- ĐOẠN JS ĐƯỢC THÊM MỚI: XỬ LÝ AJAX INSTANT TÌM KIẾM ---
            const searchForm = document.getElementById('searchForm');
            const searchInput = document.getElementById('menuSearch');
            let debounceTimer;

            // Chặn sự kiện nhấn Enter gửi đi làm reload trang
            searchForm.addEventListener('submit', (e) => { e.preventDefault(); });

            searchInput.addEventListener('input', function() {
                clearTimeout(debounceTimer);
                // Tránh gửi liên tục khi người dùng gõ quá nhanh (Chờ 300ms sau khi dừng gõ)
                debounceTimer = setTimeout(() => {
                    const query = searchInput.value.trim();
                    // Tạo đường dẫn AJAX gán cờ ajax=true gửi tới servlet
                    const url = `${pageContext.request.contextPath}/admin/menu?ajax=true&search=\${encodeURIComponent(query)}`;

                    fetch(url)
                        .then(response => response.text())
                        .then(html => {
                            // Tạo DOM ảo để bóc tách lấy dữ liệu từ kết quả Servlet trả về
                            const parser = new DOMParser();
                            const doc = parser.parseFromString(html, 'text/html');

                            // 1. Cập nhật danh sách thẻ món ăn
                            const newProducts = doc.getElementById('menu-products-container').innerHTML;
                            document.getElementById('menu-products-container').innerHTML = newProducts;

                            // 2. Cập nhật khu vực hiển thị số lượng trang
                            const newPaginationInfo = doc.getElementById('pagination-info').innerHTML;
                            document.getElementById('pagination-info').innerHTML = newPaginationInfo;

                            // 3. Cập nhật các nút bấm phân trang bên dưới
                            const newPagination = doc.getElementById('pagination-container').innerHTML;
                            document.getElementById('pagination-container').innerHTML = newPagination;
                        })
                        .catch(err => console.error('Lỗi tìm kiếm:', err));
                }, 300);
            });

            // --- ALERT TOAST HANDLER ---
            window.addEventListener('DOMContentLoaded', () => {
                const urlParams = new URLSearchParams(window.location.search);
                const success = urlParams.get('success');
                const error = urlParams.get('error');

                if (success === 'added') alert('🎉 Chúc mừng! Món mới đã được lưu thành công vào menu.');
                else if (success === 'updated') alert('📝 Cập nhật thông tin món ăn thành công.');
                else if (success === 'deleted') alert('🗑️ Đã thực thi xóa sản phẩm khỏi menu thành công.');
                else if (success === 'cat_added') alert('📂 Đã thêm mới danh mục thành công!');
                else if (success === 'cat_updated') alert('📂 Cập nhật thông tin danh mục thành công!');
                else if (success === 'cat_deleted') alert('🗑️ Xóa danh mục thành công khỏi hệ thống!');
                else if (error === 'duplicate') alert('⚠️ Không thể thêm! Tên món ăn này đã tồn tại trong hệ thống.');
                else if (error === 'duplicate_edit') alert('⚠️ Không thể sửa! Tên món ăn mới bị trùng với một sản phẩm khác có sẵn.');
                else if (error === 'invalid_price') alert('⚠️ Không thể thực hiện! Giá bán của món bắt buộc phải lớn hơn 20.000₫.');
                else if (error === 'cat_has_products') alert('❌ Không thể xóa danh mục này! Vẫn còn các món ăn thuộc nhóm này trong database.');
                else if (error === 'cat_failed' || error === 'failed') alert('❌ Thao tác thất bại! Kiểm tra lại dữ liệu đầu vào.');
            });
        </script>
    </body>
</html>
