<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản Lý Nhân Viên - Coffee House</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <style>
            :root {
                --primary-color: #006241;
                --bg-color: #F2F0EB;
                --text-color: #212121;
                --white: #FFFFFF;
                --border-color: #E0E0E0;
            }

            * {
                box-sizing: border-box;
                margin: 0;
                padding: 0;
                font-family: 'Segoe UI', sans-serif;
            }
            body {
                background-color: var(--bg-color);
                color: var(--text-color);
                padding: 30px;
            }

            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: var(--white);
                padding: 25px;
                border-radius: 10px;
                box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            }

            .header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 25px;
                border-bottom: 2px solid var(--bg-color);
                padding-bottom: 15px;
            }

            .header h2 {
                color: var(--primary-color);
                font-size: 24px;
            }

            .btn-add {
                background-color: var(--primary-color);
                color: var(--white);
                padding: 10px 18px;
                border: none;
                border-radius: 5px;
                text-decoration: none;
                font-weight: 600;
                display: inline-flex;
                align-items: center;
                gap: 8px;
                cursor: pointer;
            }
            .btn-add:hover {
                opacity: 0.9;
            }

            .table-section {
                width: 100%;
                border-collapse: collapse;
                margin-top: 15px;
            }
            .table-section th {
                background-color: #F9F9F9;
                color: #555;
                padding: 14px;
                text-align: left;
                border-bottom: 2px solid var(--border-color);
            }
            .table-section td {
                padding: 14px;
                border-bottom: 1px solid var(--border-color);
                vertical-align: middle;
            }
            .table-section tr:hover {
                background-color: #FBFBFA;
            }

            .role-form {
                display: inline-flex;
                align-items: center;
                gap: 8px;
            }
            .select-role {
                padding: 6px 12px;
                border-radius: 20px;
                border: 1px solid var(--border-color);
                font-size: 13px;
                font-weight: 600;
                background: #F9F9F9;
                color: #333;
                outline: none;
                cursor: pointer;
            }
            .select-role:focus {
                border-color: var(--primary-color);
                background: #FFF;
            }

            .btn-save-role {
                background: none;
                border: none;
                color: #888;
                cursor: pointer;
                font-size: 15px;
                padding: 4px;
            }
            .btn-save-role:hover {
                color: var(--primary-color);
            }

            .status-on {
                color: #2ECC71;
                font-weight: bold;
            }
            .status-off {
                color: #E74C3C;
                font-weight: bold;
            }

            .actions a {
                color: #555;
                margin-right: 12px;
                text-decoration: none;
                font-size: 16px;
            }
            .actions a:hover {
                color: var(--primary-color);
            }

            /* Modal */
            .modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.4);
                backdrop-filter: blur(3px);
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

            .modal-container {
                background: var(--white);
                width: 100%;
                max-width: 460px;
                padding: 25px;
                border-radius: 12px;
                box-shadow: 0 10px 25px rgba(0,0,0,0.15);
            }
            .modal-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 20px;
                padding-bottom: 12px;
                border-bottom: 1px solid var(--border-color);
            }
            .modal-header h3 {
                color: var(--primary-color);
                font-size: 20px;
                font-weight: 700;
            }
            .modal-close-btn {
                background: none;
                border: none;
                font-size: 20px;
                cursor: pointer;
                color: #aaa;
            }
            .modal-close-btn:hover {
                color: #333;
            }

            .form-group {
                margin-bottom: 16px;
            }
            .form-group label {
                display: block;
                font-size: 13px;
                font-weight: 600;
                color: #444;
                margin-bottom: 6px;
            }
            .form-control {
                width: 100%;
                padding: 10px 12px;
                border: 1px solid var(--border-color);
                border-radius: 6px;
                font-size: 14px;
                outline: none;
            }
            .form-control:focus {
                border-color: var(--primary-color);
            }

            .modal-footer {
                display: flex;
                justify-content: flex-end;
                gap: 10px;
                margin-top: 22px;
                padding-top: 10px;
            }
            .btn-close {
                background: #E5E5E5;
                color: #444;
                border: none;
                padding: 10px 16px;
                border-radius: 5px;
                cursor: pointer;
                font-weight: 600;
            }
            .btn-submit-form {
                background: var(--primary-color);
                color: var(--white);
                border: none;
                padding: 10px 20px;
                border-radius: 5px;
                cursor: pointer;
                font-weight: 600;
            }
        </style>
    </head>
    <body>

        <div class="container">
            <div class="header">
                <div>
                    <h2>Danh Sách Nhân Viên</h2>
                    <p style="color: #666; font-size: 14px; margin-top: 4px;">Hệ thống quản lý nhân sự MyCoffeeHouse</p>
                </div>
                <button type="button" class="btn-add" onclick="openAddModal()">
                    <i class="fa-solid fa-plus"></i> Thêm nhân viên
                </button>
            </div>

            <c:if test="${param.success eq 'true'}">
                <div style="background: #e6ffe6; color: #006400; padding: 12px; border-radius: 6px; margin-bottom: 20px; font-weight: 600;">
                    ✅ Thêm nhân viên thành công!
                </div>
            </c:if>
            <c:if test="${param.error != null && !param.error.startsWith('update')}">
                <div style="background: #fee; color: #c00; padding: 12px; border-radius: 6px; margin-bottom: 20px;">
                    <strong>Lỗi:</strong> 
                    ${param.error == 'missingFields' ? 'Vui lòng nhập đầy đủ thông tin (Tên, Email, Mật khẩu, Chức vụ)' : 
                      param.error == 'duplicateUser' ? 'Tên tài khoản (Username) này đã tồn tại trên hệ thống.' :
                      param.error == 'duplicateEmail' ? 'Địa chỉ Email này đã được đăng ký trước đó.' :
                      param.error == 'duplicatePhone' ? 'Số điện thoại này đã được đăng ký trước đó.' :
                      param.error == 'invalidRole' ? 'Chức vụ không hợp lệ.' : 'Có lỗi xảy ra khi thêm mới, vui lòng thử lại.'}
                </div>
            </c:if>

            <c:if test="${param.success eq 'updated'}">
                <div style="background: #e6ffe6; color: #006400; padding: 12px; border-radius: 6px; margin-bottom: 20px; font-weight: 600;">
                    🎉 Cập nhật thông tin nhân viên thành công!
                </div>
            </c:if>
            
            <c:if test="${param.error != null && param.error.startsWith('update')}">
                <div style="background: #fee; color: #c00; padding: 12px; border-radius: 6px; margin-bottom: 20px;">
                    <strong>Lỗi cập nhật:</strong> 
                    <c:choose>
                        <c:when test="${param.error eq 'updateDuplicateUser'}">
                            ❌ Tên tài khoản (Username) mới đã bị người khác sử dụng!
                        </c:when>
                        <c:when test="${param.error eq 'updateDuplicateEmail'}">
                            ❌ Địa chỉ Email mới đã có người khác đăng ký!
                        </c:when>
                        <c:when test="${param.error eq 'updateDuplicatePhone'}">
                            ❌ Số điện thoại mới đã tồn tại trên hệ thống!
                        </c:when>
                        <c:otherwise>
                            ❌ Quá trình cập nhật thất bại hoặc lỗi hệ thống!
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>
            <table class="table-section">
                <thead>
                    <tr>
                        <th>Mã số</th>
                        <th>Họ và Tên</th>
                        <th>Email</th>
                        <th style="width: 240px;">Chức vụ</th>
                        <th>Trạng thái</th>
                        <th>Ca làm việc</th>
                        <th>Hành động</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${employees}" var="emp">
                        <tr>
                            <td><strong>#EMP-${emp.id}</strong></td>
                            <td>${emp.fullName}</td>
                            <td>${emp.email != null ? emp.email : 'Chưa cập nhật'}</td>
                            <td>
                                <form action="${pageContext.request.contextPath}/admin/employees?action=quickRole&id=${emp.id}" method="POST" class="role-form">
                                    <select name="roleId" class="select-role">
                                        <c:forEach items="${roles}" var="role">
                                            <option value="${role.roleId}" ${emp.roleName eq role.roleName ? 'selected' : ''}>
                                                ${role.roleName}
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <button type="submit" class="btn-save-role" title="Lưu thay đổi">
                                        <i class="fa-solid fa-floppy-disk"></i>
                                    </button>
                                </form>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${emp.status eq 'Đang làm'}">
                                        <span class="status-on"><i class="fa-solid fa-circle"></i> Hoạt động</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="status-off"><i class="fa-solid fa-circle"></i> Đã khóa</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td><i class="fa-regular fa-clock"></i> ${emp.shiftInfo}</td>
                            <td class="actions">
                                <a href="${pageContext.request.contextPath}/admin/employees?action=edit&id=${emp.id}" title="Sửa">
                                    <i class="fa-solid fa-user-pen"></i>
                                </a>
                                <c:choose>
                                    <c:when test="${emp.status eq 'Đang làm'}">
                                        <a href="${pageContext.request.contextPath}/admin/employees?action=toggle&id=${emp.id}" 
                                           onclick="return confirm('Xác nhận KHÓA tài khoản nhân viên này?')">
                                            <i class="fa-solid fa-ban"></i>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${pageContext.request.contextPath}/admin/employees?action=toggle&id=${emp.id}" 
                                           onclick="return confirm('Xác nhận MỞ KHÓA tài khoản?')">
                                            <i class="fa-solid fa-circle-check"></i>
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty employees}">
                        <tr>
                            <td colspan="7" style="text-align: center; padding: 30px; color: #999;">
                                Không có dữ liệu nhân viên.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <div id="addEmployeeModal" class="modal-overlay">
            <div class="modal-container">
                <div class="modal-header">
                    <h3>Thêm Nhân Viên Mới</h3>
                    <button type="button" class="modal-close-btn" onclick="closeAddModal()"><i class="fa-solid fa-xmark"></i></button>
                </div>

                <form action="${pageContext.request.contextPath}/admin/employees" method="POST">
                    <input type="hidden" name="action" value="insert">

                    <div class="form-group">
                        <label class="form-label">Tài khoản đăng nhập (Username)</label>
                        <input type="text" name="username" class="form-control" required placeholder="Ví dụ: duypham0612" />
                    </div>

                    <div class="form-group">
                        <label>Họ và Tên nhân viên <span style="color:red">*</span></label>
                        <input type="text" name="fullName" class="form-control" required placeholder="Ví dụ: Nguyễn Văn A">
                    </div>

                    <div class="form-group">
                        <label>Địa chỉ Email <span style="color:red">*</span></label>
                        <input type="email" name="email" class="form-control" required placeholder="example@mycoffee.com">
                    </div>

                    <div class="form-group">
                        <label class="form-label">Số điện thoại</label>
                        <input type="text" name="phone" class="form-control" required placeholder="Nhập số điện thoại...">
                    </div>

                    <div class="form-group">
                        <label>Mật khẩu khởi tạo <span style="color:red">*</span></label>
                        <input type="password" name="password" class="form-control" required placeholder="Mật khẩu đăng nhập ban đầu">
                    </div>

                    <div class="form-group">
                        <label>Chức vụ <span style="color:red">*</span></label>
                        <select name="roleId" class="form-control" required>
                            <option value="">-- Chọn chức vụ --</option>
                            <c:forEach items="${roles}" var="role">
                                <option value="${role.roleId}">${role.roleName}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn-close" onclick="closeAddModal()">Hủy</button>
                        <button type="submit" class="btn-submit-form">Xác nhận thêm</button>
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

            window.onclick = function (event) {
                const modal = document.getElementById('addEmployeeModal');
                if (event.target === modal)
                    closeAddModal();
            }
        </script>
    </body>
</html>