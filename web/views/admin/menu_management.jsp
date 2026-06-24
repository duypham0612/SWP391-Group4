<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Coffee POS - Quản lý thực đơn</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css" rel="stylesheet">
    
    <style>
        :root {
            --bg-main: #f4f6fa;
            --sidebar-bg: #ffffff;
            --text-main: #0f172a;
            --text-sub: #64748b;
            --primary: #0284c7;
            --primary-light: #e0f2fe;
            --success: #10b981;
            --success-light: #d1fae5;
            --warning: #f59e0b;
            --danger: #ef4444;
            --border-color: #e2e8f0;
        }

        * { box-sizing: border-box; font-family: 'Inter', sans-serif; margin: 0; padding: 0; }
        body { background-color: var(--bg-main); display: flex; color: var(--text-main); min-height: 100vh; }
        
        /* Sidebar Menu Left */
        .sidebar { width: 260px; background-color: var(--sidebar-bg); height: 100vh; position: fixed; left: 0; top: 0; border-right: 1px solid var(--border-color); display: flex; flex-direction: column; justify-content: space-between; padding-bottom: 24px; z-index: 100; }
        .logo-section { padding: 24px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid #f1f5f9; }
        .logo-icon { background: var(--primary); color: white; width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
        .logo-title { font-size: 20px; font-weight: 700; color: var(--text-main); }
        .logo-sub { font-size: 11px; color: var(--text-sub); }
        
        .menu { list-style: none; padding: 20px 16px; flex-grow: 1; }
        .menu li { margin-bottom: 6px; }
        .menu li a { display: flex; align-items: center; gap: 12px; padding: 12px 16px; color: #475569; text-decoration: none; font-size: 14px; font-weight: 500; border-radius: 10px; transition: all 0.2s ease; }
        .menu li a i { font-size: 20px; color: #94a3b8; }
        .menu li a:hover { background-color: #f1f5f9; color: var(--text-main); }
        .menu li.active a { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }
        .menu li.active a i { color: var(--primary); }
        
        .user-profile-bar { padding: 16px; margin: 0 16px; background: #f8fafc; border-radius: 12px; display: flex; align-items: center; gap: 12px; border: 1px solid var(--border-color); }
        .avatar-circle { width: 38px; height: 38px; border-radius: 50%; background: var(--primary); color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 13px; }

        /* Main Content Right */
        .main-content { margin-left: 260px; flex: 1; padding: 32px 40px; max-width: 1440px; }
        .top-navbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
        
        /* Date Badge Style */
        .date-badge { background: #ffffff; border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; color: var(--text-sub); display: flex; align-items: center; gap: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
        .date-badge i { color: var(--primary); font-size: 16px; }

        .header-title { font-size: 26px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }
        .header-sub { font-size: 14px; color: var(--text-sub); margin-bottom: 24px; }
        
        /* Grid Split workspace */
        .menu-grid { display: grid; grid-template-columns: 1fr 2.5fr; gap: 24px; }
        .panel-box { background: #ffffff; border-radius: 16px; padding: 24px; border: 1px solid var(--border-color); box-shadow: 0 1px 3px rgba(0,0,0,0.02); height: fit-content; }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 20px;}
        
        /* Search Box Style */
        .search-wrapper { position: relative; margin-bottom: 20px; width: 100%; max-width: 360px; display: flex; align-items: center; }
        .search-wrapper input { width: 100%; padding: 10px 16px 10px 40px; border-radius: 10px; border: 1px solid var(--border-color); font-size: 14px; outline: none; background-color: #f8fafc; transition: all 0.2s ease; }
        .search-wrapper input:focus { border-color: var(--primary); background-color: #fff; box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1); }
        .search-wrapper i { position: absolute; left: 14px; color: var(--text-sub); font-size: 20px; pointer-events: none; }

        /* Form Style */
        .form-group { margin-bottom: 16px; display: flex; flex-direction: column; gap: 6px; }
        .form-group label { font-size: 13px; font-weight: 600; color: #475569; }
        .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color); font-size: 14px; outline: none; }
        .form-group textarea { resize: vertical; min-height: 70px; }
        .form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--primary); }
        
        .radio-group { display: flex; gap: 16px; margin-top: 4px; font-size: 14px; }
        .radio-label { display: flex; align-items: center; gap: 6px; cursor: pointer; font-weight: 500 !important; color: #334155 !important; }
        .radio-label input { width: auto; cursor: pointer; }

        .image-preview-container { margin-top: 10px; text-align: center; border: 1px dashed var(--border-color); border-radius: 8px; padding: 10px; background: #f8fafc; display: none; }
        .image-preview-container img { max-width: 100%; max-height: 120px; border-radius: 6px; object-fit: cover; }

        .btn-submit { background: #00629b; color: white; border: none; padding: 12px; width: 100%; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; box-shadow: 0 4px 10px rgba(0, 98, 155, 0.2); margin-top: 12px; }
        .btn-cancel { background: #cbd5e1; color: #334155; border: none; padding: 10px; width: 100%; border-radius: 10px; font-size: 13px; font-weight: 500; cursor: pointer; display: none; margin-top: 8px; text-align: center; }

        /* Table Style */
        table { width: 100%; border-collapse: collapse; text-align: left; table-layout: fixed; }
        th { background-color: #f8fafc; padding: 14px 12px; font-weight: 600; color: var(--text-sub); font-size: 12px; border-bottom: 1px solid var(--border-color); }
        td { padding: 14px 12px; border-bottom: 1px solid #f8fafc; color: #334155; font-size: 14px; vertical-align: middle; }
        
        /* Cấu hình độ rộng các cột tối ưu nhất */
        .col-img { width: 55px; text-align: center; }
        .col-code { width: 80px; }
        .col-name { width: 150px; }
        .col-category { width: 140px; }
        .col-desc { width: auto; }
        .col-price { width: 100px; }
        .col-action { width: 95px; text-align: center; }

        .product-img { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border-color); background: #f1f5f9; }
        .category-badge { background: #f1f5f9; color: #475569; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 500; display: inline-block; }
        
        .desc-text { font-size: 13px; color: var(--text-sub); line-height: 1.5; word-break: break-word; white-space: normal; }
        
        .action-flex { display: flex; gap: 12px; justify-content: center; align-items: center; }
        .action-btn { background: none; border: none; color: #94a3b8; font-size: 18px; cursor: pointer; text-decoration: none; padding: 2px; }
        .action-btn.delete:hover { color: var(--danger); }
        .action-btn.edit:hover { color: var(--primary); }

        /* Toast Notification Alert Style */
        .toast-container { position: fixed; top: 24px; right: 24px; z-index: 1000; display: flex; flex-direction: column; gap: 12px; }
        .toast-alert { display: flex; align-items: center; gap: 12px; padding: 16px 20px; border-radius: 12px; background: #ffffff; border-left: 4px solid var(--primary); box-shadow: 0 10px 25px rgba(0,0,0,0.08); min-width: 320px; animation: slideIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; }
        .toast-alert.success { border-left-color: var(--success); }
        .toast-alert.success i { color: var(--success); }
        .toast-alert.error { border-left-color: var(--danger); }
        .toast-alert.error i { color: var(--danger); }
        .toast-icon { font-size: 22px; }
        .toast-msg { font-size: 14px; font-weight: 500; color: var(--text-main); }
        
        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
    </style>
</head>
<body>

    <div class="toast-container">
        <c:if test="${not empty sessionScope.successMessage}">
            <div class="toast-alert success">
                <i class='bx bxs-check-circle toast-icon'></i>
                <span class="toast-msg">${sessionScope.successMessage}</span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="toast-alert error">
                <i class='bx bxs-x-circle toast-icon'></i>
                <span class="toast-msg">${sessionScope.errorMessage}</span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>
    </div>

    <div class="sidebar">
        <div>
            <div class="logo-section">
                <div class="logo-icon"><i class='bx bxs-coffee-togo'></i></div>
                <div>
                    <h1 class="logo-title">Coffee POS</h1>
                    <p class="logo-sub">Hệ thống quản lý chuỗi</p>
                </div>
            </div>
            <ul class="menu">
                <li><a href="${pageContext.request.contextPath}/admin-dashboard"><i class='bx bxs-dashboard'></i>Tổng quan</a></li>
                <li class="active"><a href="${pageContext.request.contextPath}/admin-menu"><i class='bx bx-dish'></i>Thực đơn</a></li>
                <li><a href="#"><i class='bx bx-package'></i>Kho nguyên liệu</a></li>
                <li><a href="${pageContext.request.contextPath}/admin-employees"><i class='bx bx-user-voice'></i>Nhân viên</a></li>
                <li><a href="#"><i class='bx bx-gift'></i>Khuyến mãi</a></li>
                <li><a href="#"><i class='bx bx-bar-chart-alt-2'></i>Báo cáo</a></li>
                <li><a href="#"><i class='bx bx-cog'></i>Cài đặt</a></li>
            </ul>
        </div>
        <div class="user-profile-bar">
            <div class="avatar-circle">AD</div>
            <div class="user-info-text">
                <span class="user-name">Admin</span>
                <span class="user-role">Tổng Chi Nhánh</span>
            </div>
        </div>
    </div>

    <div class="main-content">
        <div class="top-navbar">
            <h2 class="header-title">Quản lý thực đơn</h2>
            <div class="date-badge">
                <i class='bx bx-calendar'></i>
                <span id="current-date">Hôm nay: Đang tải...</span>
            </div>
        </div>
        <p class="header-sub" style="margin-top: -20px;">Thêm mới hoặc chỉnh sửa các món ăn trực tiếp vào hệ thống cơ sở dữ liệu.</p>

        <div class="menu-grid">
            
            <div class="panel-box">
                <div class="panel-title" id="form-panel-title">
                    ${isArchiveView ? "Đang xem kho lưu trữ" : "Thêm sản phẩm mới"}
                </div>
                
                <form id="productForm" action="${pageContext.request.contextPath}/admin-menu" method="post" enctype="multipart/form-data" style="${isArchiveView ? 'opacity: 0.5; pointer-events: none;' : ''}">
                    <input type="hidden" name="action" id="formAction" value="create">
                    <input type="hidden" name="productId" id="formProductId" value="0">

                    <div class="form-group">
                        <label>Tên sản phẩm</label>
                        <input type="text" name="productName" id="inputName" placeholder="Ví dụ: Cà Phê Sữa Đá" required ${isArchiveView ? 'disabled' : ''}>
                    </div>
                    
                    <div class="form-group">
                        <label>Giá bán gốc (đ)</label>
                        <input type="number" name="basePrice" id="inputPrice" placeholder="Ví dụ: 29000" required ${isArchiveView ? 'disabled' : ''}>
                    </div>
                    
                    <div class="form-group">
                        <label>Danh mục nhóm</label>
                        <select name="categoryId" id="inputCategory" required ${isArchiveView ? 'disabled' : ''}>
                            <option value="1">Cà phê máy</option>
                            <option value="2">Cà phê truyền thống</option>
                            <option value="3">Trà trái cây</option>
                            <option value="4">Bánh ngọt</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Mô tả chi tiết</label>
                        <textarea name="productDescription" id="inputDescription" placeholder="Hương vị thơm ngon..." ${isArchiveView ? 'disabled' : ''}></textarea>
                    </div>

                    <div class="form-group">
                        <label>Hình ảnh sản phẩm</label>
                        <div class="radio-group">
                            <label class="radio-label">
                                <input type="radio" name="imageSource" value="url" checked onclick="switchImageSource('url')" ${isArchiveView ? 'disabled' : ''}> Nhập URL ảnh
                            </label>
                            <label class="radio-label">
                                <input type="radio" name="imageSource" value="file" onclick="switchImageSource('file')" ${isArchiveView ? 'disabled' : ''}> Tải từ máy tính
                            </label>
                        </div>
                        
                        <div id="urlInputContainer" style="margin-top: 8px;">
                            <input type="text" name="productImageUrl" id="inputImageUrl" placeholder="https://example.com/image.jpg" oninput="previewFromUrl(this.value)" ${isArchiveView ? 'disabled' : ''}>
                        </div>

                        <div id="fileInputContainer" style="margin-top: 8px; display: none;">
                            <input type="file" name="productImageFile" id="inputImageFile" accept="image/*" onchange="previewFromFile(this)" ${isArchiveView ? 'disabled' : ''}>
                        </div>

                        <div class="image-preview-container" id="previewContainer">
                            <img id="imagePreview" src="" alt="Xem trước ảnh">
                        </div>
                    </div>
                    
                    <button type="submit" class="btn-submit" id="btnSubmitForm" ${isArchiveView ? 'disabled' : ''}>
                        <i class='bx bx-plus-circle'></i> <span id="submitText">Thêm vào thực đơn</span>
                    </button>
                    <button type="button" class="btn-cancel" id="btnCancelEdit" onclick="resetToCreateMode()">Hủy chỉnh sửa</button>
                </form>
            </div>
            
            <div class="panel-box">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <div class="panel-title" style="margin-bottom: 0;">
                        ${isArchiveView ? "Kho lưu trữ món đã ẩn" : "Thực đơn đang kinh doanh"}
                    </div>
                    
                    <c:choose>
                        <c:when test="${isArchiveView}">
                            <a href="${pageContext.request.contextPath}/admin-menu" style="text-decoration: none; font-size: 13px; color: var(--primary); font-weight: 600; display: flex; align-items: center; gap: 4px;">
                                <i class='bx bx-arrow-back'></i> Quay lại Thực đơn chính
                            </a>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/admin-menu?action=view-hidden" style="text-decoration: none; font-size: 13px; color: var(--danger); font-weight: 600; display: flex; align-items: center; gap: 4px;">
                                <i class='bx bx-archive-in'></i> Xem món đã ẩn
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="search-wrapper">
                    <i class='bx bx-search'></i>
                    <input type="text" id="menuSearchInput" placeholder="Tìm tên món, danh mục, mô tả, giá cả..." onkeyup="filterMenuTable()">
                </div>

                <table>
                    <thead>
                        <tr>
                            <th class="col-img">Ảnh</th>
                            <th class="col-code">Mã Món</th>
                            <th class="col-name">Tên Món</th>
                            <th class="col-category">Danh Mục</th>
                            <th class="col-desc">Mô Tả</th>
                            <th class="col-price">Giá Bán</th>
                            <th class="col-action">Hành Động</th>
                        </tr>
                    </thead>
                    <tbody id="menuTableBody">
                        <c:forEach var="p" items="${productList}">
                            <tr class="menu-item-row">
                                <td class="col-img">
                                    <img src="${not empty p.imageUrl ? p.imageUrl : 'https://placehold.co/100x100?text=No+Image'}" class="product-img" alt="${p.productName}">
                                </td>
                                <td class="col-code target-code" style="font-weight: 600; color: var(--text-sub);">#PR-${p.productId}</td>
                                <td class="col-name target-name" style="font-weight: 600;">${p.productName}</td>
                                <td class="col-category">
                                    <span class="category-badge target-category">
                                        <c:choose>
                                            <c:when test="${p.categoryId == 1}">Cà phê máy</c:when>
                                            <c:when test="${p.categoryId == 2}">Cà phê truyền thống</c:when>
                                            <c:when test="${p.categoryId == 3}">Trà trái cây</c:when>
                                            <c:otherwise>Bánh ngọt</c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td class="col-desc">
                                    <div class="desc-text target-desc">${not empty p.description ? p.description : '---'}</div>
                                </td>
                                <td class="col-price target-price" data-raw-price="${p.basePrice}" style="font-weight: 700; color: #00629b;">
                                    <fmt:formatNumber value="${p.basePrice}" type="currency" currencySymbol="đ" maxFractionDigits="0"/>
                                </td>
                                <td class="col-action">
                                    <div class="action-flex">
                                        <c:choose>
                                            <c:when test="${isArchiveView}">
                                                <a href="${pageContext.request.contextPath}/admin-menu?action=restore&id=${p.productId}" 
                                                   class="action-btn" style="color: var(--success); font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 2px;"
                                                   onclick="return confirm('Bạn có chắc chắn muốn mở lại món này trên thực đơn?')">
                                                    <i class='bx bx-refresh' style="font-size: 18px;"></i> Khôi phục
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <button class="action-btn edit" title="Sửa thông tin" 
                                                        onclick="setEditMode('${p.productId}', '${p.productName}', '${p.basePrice}', '${p.categoryId}', '${p.imageUrl}', `${p.description}`)">
                                                    <i class='bx bx-edit-alt'></i>
                                                </button>
                                                <a href="${pageContext.request.contextPath}/admin-menu?action=delete&id=${p.productId}" 
                                                   class="action-btn delete" title="Gỡ bỏ"
                                                   onclick="return confirm('Bạn có chắc chắn muốn xóa món này?')">
                                                    <i class='bx bx-trash'></i>
                                                </a>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty productList}">
                            <tr id="emptyRow">
                                <td colspan="7" style="text-align: center; color: #94a3b8; padding: 40px;">Thực đơn trống.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
            
        </div>
    </div>

    <script>
        // Hàm lọc Đa Năng: Tên, Mã, Danh Mục, Mô Tả, Giá Tiền
        function filterMenuTable() {
            var input = document.getElementById('menuSearchInput');
            var filter = input.value.toLowerCase().trim();
            // Loại bỏ dấu chấm, chữ "đ" hoặc khoảng trắng khi tìm theo giá bán thương mại (ví dụ: 75.000đ -> 75000)
            var cleanFilter = filter.replace(/[\.đ\s]/g, ''); 
            
            var rows = document.querySelectorAll('.menu-item-row');
            var visibleCount = 0;

            rows.forEach(function(row) {
                var nameText = row.querySelector('.target-name').innerText.toLowerCase();
                var codeText = row.querySelector('.target-code').innerText.toLowerCase();
                var catText = row.querySelector('.target-category').innerText.toLowerCase();
                var descText = row.querySelector('.target-desc').innerText.toLowerCase();
                
                // Lấy cả chuỗi hiển thị định dạng (75.000 đ) và số gốc trong DB (75000)
                var priceTd = row.querySelector('.target-price');
                var formattedPrice = priceTd.innerText.toLowerCase();
                var rawPrice = priceTd.getAttribute('data-raw-price') || '';

                // Logic kiểm tra khớp chuỗi đầu vào
                var matchesName = nameText.indexOf(filter) > -1;
                var matchesCode = codeText.indexOf(filter) > -1;
                var matchesCategory = catText.indexOf(filter) > -1;
                var matchesDesc = descText.indexOf(filter) > -1;
                var matchesPrice = formattedPrice.indexOf(filter) > -1 || rawPrice.indexOf(cleanFilter) > -1;

                if (matchesName || matchesCode || matchesCategory || matchesDesc || matchesPrice) {
                    row.style.display = "";
                    visibleCount++;
                } else {
                    row.style.display = "none";
                }
            });

            // Quản lý hiển thị thông báo rỗng khi không có kết quả phù hợp
            var noResult = document.getElementById('noResultRow');
            if (visibleCount === 0 && rows.length > 0) {
                if (!noResult) {
                    var tbody = document.getElementById('menuTableBody');
                    var tr = document.createElement('tr');
                    tr.id = 'noResultRow';
                    tr.innerHTML = '<td colspan="7" style="text-align: center; color: #94a3b8; padding: 40px;">Không tìm thấy món ăn phù hợp với từ khóa.</td>';
                    tbody.appendChild(tr);
                }
            } else {
                if (noResult) noResult.remove();
            }
        }

        function updateCurrentDate() {
            var now = new Date();
            var day = String(now.getDate()).padStart(2, '0');
            var month = String(now.getMonth() + 1).padStart(2, '0');
            var year = now.getFullYear();
            
            var dateSpan = document.getElementById('current-date');
            if (dateSpan) {
                dateSpan.innerText = "Hôm nay: " + day + " Th" + month + ", " + year;
            }
        }

        window.addEventListener('DOMContentLoaded', function() {
            updateCurrentDate();
            setInterval(updateCurrentDate, 60000);

            var toasts = document.querySelectorAll('.toast-alert');
            toasts.forEach(function(toast) {
                setTimeout(function() {
                    toast.style.transition = "opacity 0.4s ease, transform 0.4s ease";
                    toast.style.opacity = "0";
                    toast.style.transform = "translateY(-10px)";
                    setTimeout(function() { toast.remove(); }, 400);
                }, 4000);
            });
        });

        function switchImageSource(type) {
            var urlContainer = document.getElementById('urlInputContainer');
            var fileContainer = document.getElementById('fileInputContainer');
            var urlInput = document.getElementById('inputImageUrl');
            var fileInput = document.getElementById('inputImageFile');

            if (type === 'url') {
                urlContainer.style.display = 'block';
                fileContainer.style.display = 'none';
                fileInput.value = '';
                previewFromUrl(urlInput.value);
            } else {
                urlContainer.style.display = 'none';
                fileContainer.style.display = 'block';
                previewFromFile(fileInput);
            }
        }

        function previewFromUrl(url) {
            var container = document.getElementById('previewContainer');
            var preview = document.getElementById('imagePreview');
            if(url.trim() !== "") {
                preview.src = url;
                container.style.display = 'block';
            } else {
                container.style.display = 'none';
            }
        }

        function previewFromFile(input) {
            var container = document.getElementById('previewContainer');
            var preview = document.getElementById('imagePreview');
            if (input.files && input.files[0]) {
                var reader = new FileReader();
                reader.onload = function(e) {
                    preview.src = e.target.result;
                    container.style.display = 'block';
                }
                reader.readAsDataURL(input.files[0]);
            } else {
                container.style.display = 'none';
            }
        }

        function setEditMode(id, name, price, categoryId, imageUrl, description) {
            document.getElementById('form-panel-title').innerText = "Cập nhật sản phẩm #" + id;
            document.getElementById('formAction').value = "update";
            document.getElementById('formProductId').value = id;
            
            document.getElementById('inputName').value = name;
            document.getElementById('inputPrice').value = Math.round(price);
            document.getElementById('inputCategory').value = categoryId;
            document.getElementById('inputDescription').value = (description && description !== 'null') ? description : "";
            
            document.getElementById('inputImageUrl').value = imageUrl;
            document.querySelector('input[name="imageSource"][value="url"]').checked = true;
            switchImageSource('url');

            document.getElementById('submitText').innerText = "Lưu thay đổi vào DB";
            document.getElementById('btnCancelEdit').style.display = "block";
            document.getElementById('btnSubmitForm').style.background = "#10b981";
        }

        function resetToCreateMode() {
            document.getElementById('form-panel-title').innerText = "Thêm sản phẩm mới";
            document.getElementById('formAction').value = "create";
            document.getElementById('formProductId').value = "0";
            
            document.getElementById('productForm').reset();
            document.getElementById('inputDescription').value = "";
            document.getElementById('previewContainer').style.display = 'none';
            switchImageSource('url');
            
            document.getElementById('submitText').innerText = "Thêm vào thực đơn";
            document.getElementById('btnCancelEdit').style.display = "none";
            document.getElementById('btnSubmitForm').style.background = "#00629b";
        }
    </script>
</body>
</html>
