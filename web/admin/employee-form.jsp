<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <c:set var="isEdit" value="${not empty employee}" />
    <title>${isEdit ? 'Cập Nhật Nhân Viên' : 'Thêm Mới Nhân Viên'} - Coffee House</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary-color: #006241;
            --bg-color: #F2F0EB;
            --text-color: #212121;
            --white: #FFFFFF;
            --border-color: #E0E0E0;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; }
        body { background-color: var(--bg-color); color: var(--text-color); padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
        
        .form-container {
            background: var(--white);
            width: 100%;
            max-width: 550px;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.08);
        }

        .form-header { border-bottom: 2px solid var(--bg-color); padding-bottom: 15px; margin-bottom: 25px; }
        .form-header h2 { color: var(--primary-color); font-size: 22px; display: flex; align-items: center; gap: 10px; }
        
        .form-group { margin-bottom: 20px; }
        .form-group label { display: block; font-weight: 600; margin-bottom: 8px; font-size: 14px; color: #444; }
        .form-group label span { color: red; }
        
        .form-control {
            width: 100%;
            padding: 10px 14px;
            border: 1px solid var(--border-color);
            border-radius: 6px;
            font-size: 14px;
            outline: none;
        }
        .form-control:focus { border-color: var(--primary-color); box-shadow: 0 0 0 3px rgba(0,98,65,0.1); }
        
        .btn-group { display: flex; gap: 12px; margin-top: 30px; border-top: 1px solid var(--bg-color); padding-top: 20px; }
        
        .btn {
            flex: 1;
            padding: 11px;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            text-align: center;
            text-decoration: none;
        }
        .btn-submit { background-color: var(--primary-color); color: white; }
        .btn-cancel { background-color: #E0E0E0; color: #444; }
    </style>
</head>
<body>
    <div class="form-container">
        <div class="form-header">
            <h2>
                <i class="${isEdit ? 'fa-solid fa-user-gear' : 'fa-solid fa-user-plus'}"></i>
                ${isEdit ? 'Cập Nhật Nhân Viên' : 'Thêm Nhân Viên Mới'}
            </h2>
        </div>

        <%-- Hiển thị thông báo lỗi cục bộ tại trang Form chỉnh sửa nếu có --%>
        <c:if test="${param.error != null}">
            <div style="background: #fee; color: #c00; padding: 12px; border-radius: 6px; margin-bottom: 20px; font-size: 14px;">
                <strong>Lỗi thực thi:</strong> 
                <c:choose>
                    <c:when test="${param.error eq 'updateDuplicateUser' || param.error eq 'duplicateUser'}">
                        ❌ Tên tài khoản (Username) đã có người sử dụng.
                    </c:when>
                    <c:when test="${param.error eq 'updateDuplicateEmail' || param.error eq 'duplicateEmail'}">
                        ❌ Địa chỉ Email đã tồn tại.
                    </c:when>
                    <c:when test="${param.error eq 'updateDuplicatePhone' || param.error eq 'duplicatePhone'}">
                        ❌ Số điện thoại đã được đăng ký.
                    </c:when>
                    <c:when test="${param.error eq 'updateAdminExists' || param.error eq 'adminExists'}">
                        ❌ Hệ thống đã tồn tại 1 Admin duy nhất! Không được chỉ định thêm quyền này.
                    </c:when>
                    <c:otherwise>
                        ❌ Thao tác không thành công, vui lòng kiểm tra lại.
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/admin/employees" method="POST" onsubmit="return validateForm()">
            <input type="hidden" name="action" value="${isEdit ? 'update' : 'insert'}">
            <c:if test="${isEdit}">
                <input type="hidden" name="id" value="${employee.id}">
            </c:if>
            
            <div class="form-group">
                <label>Tài khoản đăng nhập (Username): <c:if test="${!isEdit}"><span>*</span></c:if></label>
                <input type="text" name="username" class="form-control" ${isEdit ? 'readonly style="background:#f5f5f5;"' : 'required'} value="${isEdit ? employee.username : ''}" placeholder="Ví dụ: duypham0612">
            </div>
            
            <%-- KHÔNG KHÓA: Admin hay nhân viên thường đều sửa được họ tên --%>
            <div class="form-group">
                <label>Họ và Tên: <span>*</span></label>
                <input type="text" name="fullName" class="form-control" required value="${isEdit ? employee.fullName : ''}" placeholder="Nguyễn Văn A">
            </div>

            <%-- KHÔNG KHÓA: Admin hay nhân viên thường đều sửa được email --%>
            <div class="form-group">
                <label>Địa chỉ Email: <span>*</span></label>
                <input type="email" name="email" class="form-control" required value="${isEdit ? employee.email : ''}" placeholder="example@mycoffee.com">
            </div>

            <%-- KHÔNG KHÓA: Admin hay nhân viên thường đều sửa được số điện thoại --%>
            <div class="form-group">
                <label>Số điện thoại: <span>*</span></label>
                <input type="tel" name="phone" class="form-control" required 
                       pattern="0[0-9]{9}" title="Số điện thoại phải gồm 10 chữ số bắt đầu bằng số 0"
                       value="${isEdit ? employee.phone : ''}" placeholder="Ví dụ: 0912345678">
            </div>

            <c:if test="${!isEdit}">
                <div class="form-group">
                    <label>Mật khẩu khởi tạo: <span>*</span></label>
                    <input type="password" name="password" class="form-control" required placeholder="Mật khẩu đăng nhập ban đầu">
                </div>
            </c:if>

            <div class="form-group">
                <label>Chức vụ: <span>*</span></label>
                <%-- KHÓA: Nếu sửa tài khoản admin thì disabled select để không cho đổi chức vụ --%>
                <select name="roleId" class="form-control" required ${isEdit && employee.username eq 'admin' ? 'disabled style="background:#f5f5f5;"' : ''}>
                    <option value="">-- Chọn chức vụ --</option>
                    <c:forEach items="${roles}" var="role">
                        <option value="${role.roleId}" ${isEdit && employee.roleId == role.roleId ? 'selected' : ''}>
                            ${role.roleName}
                        </option>
                    </c:forEach>
                </select>
                <%-- Input ẩn giữ giá trị roleId truyền về cho Controller khi select bị disabled --%>
                <c:if test="${isEdit && employee.username eq 'admin'}">
                    <input type="hidden" name="roleId" value="${employee.roleId}">
                </c:if>
            </div>

            <c:if test="${isEdit}">
                <div class="form-group">
                    <label>Tình trạng: <span>*</span></label>
                    <c:choose>
                        <%-- KHÓA: Nếu là tài khoản admin, ép cứng trạng thái thành "Đang quản lý" và disabled --%>
                        <c:when test="${employee.username eq 'admin'}">
                            <select name="status" class="form-control" disabled style="background:#f5f5f5;">
                                <option value="Đang làm" selected>Đang quản lý</option>
                            </select>
                            <%-- Input ẩn giữ giá trị status truyền về cho Controller --%>
                            <input type="hidden" name="status" value="Đang làm">
                        </c:when>
                        
                        <%-- MỞ: Đối với các tài khoản nhân viên khác --%>
                        <c:otherwise>
                            <select name="status" class="form-control">
                                <option value="Đang làm" ${employee.status eq 'Đang làm' ? 'selected' : ''}>Đang làm</option>
                                <option value="Nghỉ việc" ${employee.status eq 'Nghỉ việc' ? 'selected' : ''}>Nghỉ việc</option>
                            </select>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <div class="btn-group">
                <a href="${pageContext.request.contextPath}/admin/employees" class="btn btn-cancel">Quay lại</a>
                <button type="submit" class="btn btn-submit">${isEdit ? 'Lưu thay đổi' : 'Thêm nhân viên'}</button>
            </div>
        </form>
    </div>

    <script>
        function validateForm() {
            const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"]');
            inputs.forEach(input => {
                input.value = input.value.trim();
            });
            return true;
        }
    </script>
</body>
</html>
