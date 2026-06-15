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
        body { background-color: var(--bg-color); color: var(--text-color); padding: 5px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }
        
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

        <form action="${pageContext.request.contextPath}/admin/employees" method="POST">
            <input type="hidden" name="action" value="${isEdit ? 'update' : 'insert'}">
            <c:if test="${isEdit}">
                <input type="hidden" name="id" value="${employee.id}">
            </c:if>
            
            <div class="form-group">
                <label>Họ và Tên:</label>
                <input type="text" name="fullName" class="form-control" required value="${isEdit ? employee.fullName : ''}">
            </div>

            <div class="form-group">
                <label>Địa chỉ Email:</label>
                <input type="email" name="email" class="form-control" required value="${isEdit ? employee.email : ''}">
            </div>

            <c:if test="${!isEdit}">
                <div class="form-group">
                    <label>Mật khẩu khởi tạo:</label>
                    <input type="password" name="password" class="form-control" required>
                </div>
            </c:if>

            <div class="form-group">
                <label>Chức vụ:</label>
                <select name="roleId" class="form-control" required>
                    <option value="">-- Chọn chức vụ --</option>
                    <c:forEach items="${roles}" var="role">
                        <option value="${role.roleId}" 
                            ${isEdit && employee.roleName eq role.roleName ? 'selected' : ''}>
                            ${role.roleName}
                        </option>
                    </c:forEach>
                </select>
            </div>

            <c:if test="${isEdit}">
                <div class="form-group">
                    <label>Tình trạng:</label>
                    <select name="status" class="form-control">
                        <option value="Đang làm" ${employee.status eq 'Đang làm' ? 'selected' : ''}>Đang làm</option>
                        <option value="Nghỉ việc" ${employee.status eq 'Nghỉ việc' ? 'selected' : ''}>Nghỉ việc</option>
                    </select>
                </div>
            </c:if>

            <div class="btn-group">
                <a href="${pageContext.request.contextPath}/admin/employees" class="btn btn-cancel">Quay lại</a>
                <button type="submit" class="btn btn-submit">${isEdit ? 'Lưu thay đổi' : 'Thêm nhân viên'}</button>
            </div>
        </form>
    </div>
</body>
</html>
