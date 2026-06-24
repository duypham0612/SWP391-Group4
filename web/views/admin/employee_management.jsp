<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Coffee POS - Quản Lý Nhân Viên</title>
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
            --warning-light: #fef3c7;
            --danger: #ef4444;
            --danger-light: #fee2e2;
            --border-color: #e2e8f0;
        }

        * { box-sizing: border-box; font-family: 'Inter', sans-serif; margin: 0; padding: 0; }
        body { background-color: var(--bg-main); display: flex; color: var(--text-main); min-height: 100vh; }
        
        /* ── SIDEBAR MENU LEFT ── */
        .sidebar { width: 260px; background-color: var(--sidebar-bg); height: 100vh; position: fixed; left: 0; top: 0; border-right: 1px solid var(--border-color); display: flex; flex-direction: column; justify-content: space-between; padding-bottom: 24px; z-index: 100; }
        .logo-section { padding: 24px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid #f1f5f9; }
        .logo-icon { background: var(--primary); color: white; width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
        .logo-title { font-size: 20px; font-weight: 700; color: var(--text-main); }
        .logo-sub { font-size: 11px; color: var(--text-sub); }
        
        .menu { list-style: none; padding: 20px 16px; flex-grow: 1; }
        .menu li { margin-bottom: 6px; }
        .menu li a { display: flex; align-items: center; gap: 12px; padding: 12px 16px; color: #475569; text-decoration: none; font-size: 14px; font-weight: 500; border-radius: 10px; transition: all 0.2s ease; }
        .menu li a i { font-size: 20px; color: #94a3b8; transition: all 0.2s; }
        .menu li a:hover { background-color: #f1f5f9; color: var(--text-main); }
        .menu li a:hover i { color: var(--text-main); }
        .menu li.active a { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }
        .menu li.active a i { color: var(--primary); }
        
        .user-profile-bar { padding: 16px; margin: 0 16px; background: #f8fafc; border-radius: 12px; display: flex; align-items: center; gap: 12px; border: 1px solid var(--border-color); }
        .avatar-circle { width: 38px; height: 38px; border-radius: 50%; background: #cbd5e1; object-fit: cover; }
        .user-info-text { display: flex; flex-direction: column; }
        .user-name { font-size: 13px; font-weight: 600; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--text-sub); }

        /* ── MAIN CONTENT RIGHT ── */
        .main-content { margin-left: 260px; flex: 1; padding: 32px 40px; max-width: 1440px; }
        
        /* ── TOP BAR CONTROL ── */
        .top-navbar { display: flex; justify-content: flex-end; align-items: center; margin-bottom: 32px; }
        
        .top-right-actions { display: flex; align-items: center; gap: 16px; }
        .date-badge { background: #fff; border: 1px solid var(--border-color); padding: 10px 16px; border-radius: 10px; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; color: #475569; }
        .btn-add-order { background: #00629b; color: white; border: none; padding: 10px 18px; border-radius: 10px; font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; box-shadow: 0 4px 10px rgba(0, 98, 155, 0.2); }

        .header-title { font-size: 26px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }
        .header-sub { font-size: 14px; color: var(--text-sub); margin-bottom: 24px; }
        
        /* ── WORKSPACE PANEL (TABLE AREA) ── */
        .panel-box { background: #ffffff; border-radius: 16px; padding: 24px; border: 1px solid var(--border-color); box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
        
        /* ── NEW SEARCH COMPONENT INSIDE PANEL ── */
        .search-wrapper { position: relative; width: 360px; }
        .search-wrapper i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: #94a3b8; font-size: 18px; }
        .search-wrapper input { width: 100%; padding: 10px 16px 10px 42px; border-radius: 10px; border: 1px solid var(--border-color); background: #fff; font-size: 13px; font-weight: 500; outline: none; transition: all 0.2s; }
        .search-wrapper input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15); }

        /* DATA TABLE STYLE */
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background-color: #f8fafc; padding: 14px 16px; font-weight: 600; color: var(--text-sub); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border-color); }
        td { padding: 14px 16px; border-bottom: 1px solid #f8fafc; color: #334155; font-size: 14px; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background-color: #f8fafc; }
        
        /* BADGES FOR ROLES & STATUS */
        .badge { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
        .badge-admin { background-color: var(--warning-light); color: #b45309; }
        .badge-manager { background-color: #e0f2fe; color: #0284c7; }
        .badge-staff { background-color: #f1f5f9; color: #475569; }
        .badge-customer { background-color: #f3e8ff; color: #6b21a8; }
        
        .badge-active { background-color: var(--success-light); color: #047857; }
        .badge-locked { background-color: var(--danger-light); color: #b91c1c; }
        
        /* ACTION BUTTONS */
        .action-container { display: flex; gap: 8px; justify-content: center; align-items: center; }
        .btn-action-edit { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; border: 1px solid var(--border-color); background-color: #ffffff; color: var(--text-main); cursor: pointer; display: inline-flex; align-items: center; gap: 4px; transition: all 0.2s; }
        .btn-action-edit:hover { background-color: #f8fafc; border-color: #cbd5e1; }
        
        .btn-action-delete { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; border: 1px solid #fecdd3; background-color: #fff1f2; color: #e11d48; text-decoration: none; display: inline-flex; align-items: center; gap: 4px; transition: all 0.2s; }
        .btn-action-delete:hover { background-color: #ffe4e6; }

        /* BUTTONS LOCK / UNLOCK */
        .btn-status-toggle { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; text-decoration: none; display: inline-flex; align-items: center; gap: 4px; transition: all 0.2s; cursor: pointer; border: 1px solid transparent; }
        .btn-lock { background-color: #fff7ed; color: #c2410c; border-color: #ffedd5; }
        .btn-lock:hover { background-color: #ffedd5; }
        .btn-unlock { background-color: #f0fdf4; color: #15803d; border-color: #dcfce7; }
        .btn-unlock:hover { background-color: #dcfce7; }

        /* NOTIFICATION BANNER */
        .alert-banner { padding: 12px 16px; border-radius: 10px; font-size: 14px; font-weight: 500; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }
        .alert-danger { background-color: var(--danger-light); color: var(--danger); border: 1px solid #fecdd3; }
        .alert-success { background-color: var(--success-light); color: var(--success); border: 1px solid #bbf7d0; }

        /* ── MODAL FORM (POPUP) ── */
        .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px); z-index: 1000; display: none; align-items: center; justify-content: center; }
        .modal-box { background: #fff; width: 460px; border-radius: 16px; padding: 24px; border: 1px solid var(--border-color); box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); animation: modalFadeIn 0.25s ease; }
        @keyframes modalFadeIn { from { transform: translateY(15px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .modal-title { font-size: 18px; font-weight: 700; color: var(--text-main); }
        .btn-close-modal { background: none; border: none; font-size: 22px; color: var(--text-sub); cursor: pointer; }
        
        .form-group { margin-bottom: 16px; }
        .form-label { display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px; }
        .form-control { width: 100%; padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color); font-size: 14px; outline: none; transition: border 0.2s; }
        .form-control:focus { border-color: var(--primary); }
        .form-select { width: 100%; padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color); font-size: 14px; background-color: #fff; outline: none; }
        
        .modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
        .btn-secondary { background: #f1f5f9; color: #475569; border: none; padding: 10px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; }
        .btn-primary { background: var(--primary); color: #fff; border: none; padding: 10px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; }
        .btn-primary:hover { background: #0369a1; }
    </style>
</head>
<body>

    <jsp:useBean id="now" class="java.util.Date" />

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
                <li><a href="${pageContext.request.contextPath}/admin-menu"><i class='bx bx-dish'></i>Thực đơn</a></li>
                <li><a href="#"><i class='bx bx-package'></i>Kho nguyên liệu</a></li>
                <li class="active"><a href="${pageContext.request.contextPath}/admin-employees"><i class='bx bx-user-voice'></i>Nhân viên</a></li>
                <li><a href="#"><i class='bx bx-gift'></i>Khuyến mãi</a></li>
                <li><a href="#"><i class='bx bx-bar-chart-alt-2'></i>Báo cáo</a></li>
                <li><a href="#"><i class='bx bx-cog'></i>Cài đặt</a></li>
            </ul>
        </div>
        
        <div class="user-profile-bar">
            <div class="avatar-circle" style="background: var(--primary); color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 13px;">AD</div>
            <div class="user-info-text">
                <span class="user-name">Admin</span>
                <span class="user-role">Tổng Chi Nhánh</span>
            </div>
        </div>
    </div>

    <div class="main-content">
        
        <div class="top-navbar">
            <div class="top-right-actions">
                <div class="date-badge">
                    <i class='bx bx-calendar'></i>
                    <span>Hôm nay: <fmt:formatDate value="${now}" pattern="dd 'Th'MM, yyyy" /></span>
                </div>
                <button class="btn-add-order" onclick="openModal('add')"><i class='bx bx-plus'></i> Thêm thành viên</button>
            </div>
        </div>

        <h2 class="header-title">Quản lý đội ngũ nhân sự</h2>
        <p class="header-sub">Thiết lập tài khoản, phân quyền chức vụ và quản lý danh sách thông tin người dùng.</p>

        <c:if test="${not empty errorMessage}">
            <div class="alert-banner alert-danger">
                <i class='bx bx-error-circle'></i> <span>${errorMessage}</span>
            </div>
        </c:if>
        <c:if test="${not empty successMessage}">
            <div class="alert-banner alert-success">
                <i class='bx bx-check-circle'></i> <span>${successMessage}</span>
            </div>
        </c:if>

        <div class="panel-box">
            <div class="panel-title">
                <span>Danh sách tài khoản hệ thống</span>
                <div class="search-wrapper">
                    <i class='bx bx-search'></i>
                    <input type="text" id="instantSearch" onkeyup="filterTable()" placeholder="Tìm theo TK, Tên, Email, SĐT, Chức vụ...">
                </div>
            </div>
            
            <table id="employeeTable">
                <thead>
                    <tr>
                        <th>Mã Số</th>
                        <th>Tài Khoản</th>
                        <th>Họ và Tên</th>
                        <th>Email</th>
                        <th>Số Điện Thoại</th>
                        <th>Chức Vụ</th>
                        <th style="text-align: center;">Trạng Thái / Tác Vụ</th>
                        <th style="text-align: center;">Hành Động</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="emp" items="${employeeList}">
                        <tr>
                            <td style="font-weight: 600; color: #00629b;">#USR${emp.userId}</td>
                            <td><strong><c:out value="${emp.username}"/></strong></td>
                            <td><c:out value="${emp.fullName}"/></td>
                            <td><c:out value="${emp.email}"/></td>
                            <td>${not empty emp.phone ? emp.phone : '<span style="color:#cbd5e1; font-style:italic;">Chưa có</span>'}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${emp.roleId == 1}"><span class="badge badge-admin">Admin</span></c:when>
                                    <c:when test="${emp.roleId == 2}"><span class="badge badge-manager">Manager</span></c:when>
                                    <c:when test="${emp.roleId == 3}"><span class="badge badge-staff">Staff</span></c:when>
                                    <c:otherwise><span class="badge badge-customer">Customer</span></c:otherwise>
                                </c:choose>
                            </td>
                            
                            <td style="text-align: center;">
                                <div style="display: flex; flex-direction: column; align-items: center; gap: 6px;">
                                    <c:choose>
                                        <c:when test="${emp.isActive}">
                                            <span class="badge badge-active"><i class='bx bx-check-shield'></i> Hoạt động</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-locked"><i class='bx bx-lock-alt'></i> Đang khóa</span>
                                        </c:otherwise>
                                    </c:choose>
                                    
                                    <c:choose>
                                        <c:when test="${emp.roleId == 1}">
                                            <button class="btn-status-toggle" style="opacity: 0.4; cursor: not-allowed; background: #f1f5f9; color: #94a3b8;" title="Không thể thao tác trên tài khoản hệ thống chính" disabled>
                                                Hệ thống
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <c:choose>
                                                <c:when test="${emp.isActive}">
                                                    <a href="${pageContext.request.contextPath}/admin-employees?action=toggleStatus&id=${emp.userId}&status=false" 
                                                       class="btn-status-toggle btn-lock" 
                                                       onclick="return confirm('Bạn có chắc chắn muốn TẠM KHÓA tài khoản này không?')">
                                                        <i class='bx bx-lock-open-alt'></i> Khóa lại
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="${pageContext.request.contextPath}/admin-employees?action=toggleStatus&id=${emp.userId}&status=true" 
                                                       class="btn-status-toggle btn-unlock" 
                                                       onclick="return confirm('Bạn có muốn MỞ KHÓA kích hoạt lại tài khoản này?')">
                                                        <i class='bx bx-key'></i> Mở khóa
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </td>
                            
                            <td style="text-align: center;">
                                <div class="action-container">
                                    <button class="btn-action-edit" 
                                            onclick="openModal('edit', '${emp.userId}', '${emp.username}', '${emp.fullName}', '${emp.email}', '${emp.phone}', '${emp.roleId}')">
                                        <i class='bx bx-edit-alt'></i> Sửa
                                    </button>

                                    <c:choose>
                                        <c:when test="${emp.roleId == 1}">
                                            <button class="btn-action-delete" style="opacity: 0.5; background-color: #f1f5f9; color: #94a3b8; border-color: #e2e8f0; cursor: not-allowed;" title="Không thể xóa Admin gốc" disabled>
                                                <i class='bx bx-trash'></i> Xóa
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <a href="${pageContext.request.contextPath}/admin-employees?action=delete&id=${emp.userId}" 
                                               class="btn-action-delete"
                                               onclick="return confirm('Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này khỏi hệ thống không?')">
                                                <i class='bx bx-trash'></i> Xóa
                                            </a>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty employeeList}">
                        <tr id="noDataRow">
                            <td colspan="8" style="text-align: center; color: #94a3b8; padding: 40px;">Hệ thống chưa ghi nhận tài khoản nào.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="modal-overlay" id="employeeModal">
        <div class="modal-box">
            <div class="modal-header">
                <h3 class="modal-title" id="modalTitle">Thêm thành viên mới</h3>
                <button class="btn-close-modal" onclick="closeModal()">&times;</button>
            </div>
            <form action="${pageContext.request.contextPath}/admin-employees" method="POST" id="employeeForm">
                <input type="hidden" name="action" id="formAction" value="add">
                <input type="hidden" name="userId" id="formUserId" value="">

                <div class="form-group">
                    <label class="form-label">Tên tài khoản (Username)</label>
                    <input type="text" name="username" id="username" class="form-control" placeholder="Ví dụ: tuannm" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Họ và tên</label>
                    <input type="text" name="fullName" id="fullName" class="form-control" placeholder="Ví dụ: Nguyễn Minh Tuấn" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Địa chỉ Email</label>
                    <input type="email" name="email" id="email" class="form-control" placeholder="Ví dụ: tuannm@gmail.com" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Số điện thoại</label>
                    <input type="text" name="phone" id="phone" class="form-control" placeholder="Ví dụ: 0987654321">
                </div>
                <div class="form-group">
                    <label class="form-label">Phân quyền chức vụ</label>
                    <select name="roleId" id="roleId" class="form-select">
                        <option value="3">Staff (Nhân viên phục vụ/quầy bar)</option>
                        <option value="2">Manager (Quản lý cửa hàng)</option>
                        <option value="1" id="optAdmin">Admin (Quản trị viên hệ thống)</option>
                        <option value="4">Customer (Khách hàng thành viên)</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label class="form-label" id="passwordLabel">Mật khẩu tài khoản</label>
                    <input type="password" class="form-control" name="newPassword" id="newPassword" placeholder="Nhập mật khẩu mới nếu muốn đổi">
                </div>
                
                <div class="modal-footer">
                    <button type="button" class="btn-secondary" onclick="closeModal()">Hủy bỏ</button>
                    <button type="submit" class="btn-primary" id="submitBtn">Lưu thông tin</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        /* ── CHỨC NĂNG TÌM KIẾM TỨC THỜI (GÕ TỚI ĐÂU TÌM TỚI ĐÓ) ── */
        function filterTable() {
            const input = document.getElementById("instantSearch");
            const filter = input.value.toLowerCase().trim();
            const table = document.getElementById("employeeTable");
            const tr = table.getElementsByTagName("tbody")[0].getElementsByTagName("tr");
            let visibleRows = 0;

            for (let i = 0; i < tr.length; i++) {
                // Bỏ qua dòng thông báo trống "Hệ thống chưa ghi nhận tài khoản nào" nếu có
                if (tr[i].id === "noDataRow") continue;

                const tdUsername  = tr[i].getElementsByTagName("td")[1]; // Tài Khoản
                const tdFullName  = tr[i].getElementsByTagName("td")[2]; // Họ và Tên
                const tdEmail     = tr[i].getElementsByTagName("td")[3]; // Email
                const tdPhone     = tr[i].getElementsByTagName("td")[4]; // Số điện thoại
                const tdRole      = tr[i].getElementsByTagName("td")[5]; // Chức Vụ
                const tdStatus    = tr[i].getElementsByTagName("td")[6]; // Trạng Thái

                if (tdUsername || tdFullName || tdEmail || tdPhone || tdRole || tdStatus) {
                    const txtUsername = tdUsername ? tdUsername.textContent || tdUsername.innerText : "";
                    const txtFullName = tdFullName ? tdFullName.textContent || tdFullName.innerText : "";
                    const txtEmail    = tdEmail ? tdEmail.textContent || tdEmail.innerText : "";
                    const txtPhone    = tdPhone ? tdPhone.textContent || tdPhone.innerText : "";
                    const txtRole     = tdRole ? tdRole.textContent || tdRole.innerText : "";
                    const txtStatus   = tdStatus ? tdStatus.textContent || tdStatus.innerText : "";

                    // Hợp nhất chuỗi dữ liệu của các cột để tìm kiếm toàn diện
                    const combinedText = (txtUsername + " " + txtFullName + " " + txtEmail + " " + txtPhone + " " + txtRole + " " + txtStatus).toLowerCase();

                    if (combinedText.indexOf(filter) > -1) {
                        tr[i].style.display = "";
                        visibleRows++;
                    } else {
                        tr[i].style.display = "none";
                    }
                }
            }

            // Xử lý hiển thị thông báo nếu tìm không thấy kết quả phù hợp
            let noResultRow = document.getElementById("noMatchRow");
            if (visibleRows === 0 && tr.length > 0 && (tr[0].id !== "noDataRow")) {
                if (!noResultRow) {
                    noResultRow = document.createElement("tr");
                    noResultRow.id = "noMatchRow";
                    noResultRow.innerHTML = `<td colspan="8" style="text-align: center; color: #94a3b8; padding: 30px; font-style: italic;">Không tìm thấy tài khoản nào phù hợp từ khóa.</td>`;
                    table.getElementsByTagName("tbody")[0].appendChild(noResultRow);
                }
            } else if (noResultRow) {
                noResultRow.remove();
            }
        }

        /* ── CÁC HÀM XỬ LÝ MODAL POPUP ── */
        const modal = document.getElementById('employeeModal');

        function openModal(mode, id = '', username = '', fullName = '', email = '', phone = '', roleId = 3) {
            document.getElementById('formAction').value = mode;
            const roleSelect = document.getElementById('roleId');
            const optAdmin = document.getElementById('optAdmin');
            const passwordLabel = document.getElementById('passwordLabel');
            
            document.getElementById('newPassword').value = "";
            
            if (mode === 'add') {
                document.getElementById('modalTitle').innerText = "Thêm thành viên mới";
                document.getElementById('formUserId').value = "";
                document.getElementById('username').value = "";
                document.getElementById('username').removeAttribute('readonly');
                document.getElementById('username').style.backgroundColor = "#ffffff";
                document.getElementById('fullName').value = "";
                document.getElementById('email').value = "";
                document.getElementById('phone').value = "";
                
                passwordLabel.innerText = "Mật khẩu tài khoản";
                document.getElementById('newPassword').setAttribute('placeholder', 'Nhập mật khẩu cho tài khoản mới');
                document.getElementById('newPassword').setAttribute('required', 'true');
                
                optAdmin.style.display = "none";
                roleSelect.value = "3"; 
                roleSelect.disabled = false;
                
                document.getElementById('submitBtn').innerText = "Lưu thông tin";
            } else {
                document.getElementById('modalTitle').innerText = "Chỉnh sửa thông tin tài khoản";
                document.getElementById('formUserId').value = id;
                document.getElementById('username').value = username;
                document.getElementById('username').setAttribute('readonly', 'true'); 
                document.getElementById('username').style.backgroundColor = "#f1f5f9"; 
                document.getElementById('fullName').value = fullName;
                document.getElementById('email').value = email;
                document.getElementById('phone').value = (phone === 'null' || phone === undefined || phone === 'undefined') ? '' : phone;
                
                passwordLabel.innerText = "Mật khẩu mới (Để trống nếu giữ nguyên)";
                document.getElementById('newPassword').setAttribute('placeholder', 'Nhập mật khẩu mới nếu muốn đổi');
                document.getElementById('newPassword').removeAttribute('required');
                
                if (roleId == 1) {
                    optAdmin.style.display = "block"; 
                    roleSelect.value = roleId;
                    roleSelect.disabled = true; 
                } else {
                    optAdmin.style.display = "none"; 
                    roleSelect.value = roleId;
                    roleSelect.disabled = false;
                }
                
                document.getElementById('submitBtn').innerText = "Cập nhật tài khoản";
            }
            modal.style.display = 'flex';
        }

        function closeModal() {
            modal.style.display = 'none';
        }

        window.onclick = function(event) {
            if (event.target === modal) {
                closeModal();
            }
        }

        document.getElementById('employeeForm').addEventListener('submit', function(e) {
            document.getElementById('roleId').disabled = false;
        });
    </script>
</body>
</html>
