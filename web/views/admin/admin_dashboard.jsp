<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Coffee POS - Dashboard Admin</title>
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
        .top-navbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
        .search-wrapper { position: relative; width: 320px; }
        .search-wrapper i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: #94a3b8; font-size: 18px; }
        .search-wrapper input { width: 100%; padding: 10px 16px 10px 42px; border-radius: 10px; border: 1px solid var(--border-color); background: #fff; font-size: 14px; outline: none; transition: all 0.2s; }
        .search-wrapper input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15); }
        
        .top-right-actions { display: flex; align-items: center; gap: 16px; }
        .date-badge { background: #fff; border: 1px solid var(--border-color); padding: 10px 16px; border-radius: 10px; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; color: #475569; }
        .btn-add-order { background: #00629b; color: white; border: none; padding: 10px 18px; border-radius: 10px; font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; box-shadow: 0 4px 10px rgba(0, 98, 155, 0.2); }
        .nav-icon-btn { background: #fff; border: 1px solid var(--border-color); width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #475569; cursor: pointer; text-decoration: none; position: relative; }
        .nav-icon-btn .dot { position: absolute; width: 6px; height: 6px; background: var(--danger); border-radius: 50%; top: 8px; right: 8px; }

        .header-title { font-size: 26px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }
        .header-sub { font-size: 14px; color: var(--text-sub); margin-bottom: 24px; }
        
        /* ── CARDS STATS (GRID 4 COLUMNS) ── */
        .cards-container { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
        .card { background: #ffffff; padding: 20px; border-radius: 16px; border: 1px solid var(--border-color); display: flex; flex-direction: column; justify-content: space-between; position: relative; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
        .card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
        .card-icon-box { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
        .card-title { font-size: 13px; color: var(--text-sub); font-weight: 500; }
        .card-value { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 8px; }
        
        /* Trend Indicator Styles */
        .trend-tag { padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 600; display: flex; align-items: center; gap: 2px; }
        .trend-up { background: var(--success-light); color: var(--success); }
        .trend-down { background: var(--danger-light); color: var(--danger); }
        .trend-stable { background: #f1f5f9; color: #475569; }

        /* Progress Mini Line */
        .progress-bar-container { width: 100%; height: 6px; background: #e2e8f0; border-radius: 999px; overflow: hidden; margin-top: 8px; }
        .progress-bar-fill { height: 100%; border-radius: 999px; }

        /* Color theme mapping */
        .rev-theme .card-icon-box { background: var(--primary-light); color: var(--primary); }
        .ord-theme .card-icon-box { background: #e0f7fa; color: #00acc1; }
        .stk-theme .card-icon-box { background: var(--danger-light); color: var(--danger); }
        .emp-theme .card-icon-box { background: #eceff1; color: #455a64; }
        
        /* ── WORKSPACE SPLIT GRID ── */
        .dashboard-grid-top { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; margin-bottom: 24px; }
        .dashboard-grid-bottom { display: grid; grid-template-columns: 1fr 2fr; gap: 24px; }
        
        .panel-box { background: #ffffff; border-radius: 16px; padding: 24px; border: 1px solid var(--border-color); box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
        .panel-link { font-size: 12px; color: var(--primary); text-decoration: none; font-weight: 600; }
        
        /* MODERN CHART DESIGN VIA PLUG-IN OR CLEAN INLINE SVG */
        .chart-placeholder { width: 100%; height: 220px; display: flex; align-items: flex-end; position: relative; margin-top: 10px; }
        
        /* DATA TABLE STYLE */
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background-color: #f8fafc; padding: 14px 16px; font-weight: 600; color: var(--text-sub); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border-color); }
        td { padding: 14px 16px; border-bottom: 1px solid #f8fafc; color: #334155; font-size: 14px; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        
        /* STATUS PILL BADGES */
        .status { padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
        .status-completed { background-color: var(--success-light); color: var(--success); }
        .status-completed::before { content:''; width: 6px; height: 6px; background: var(--success); border-radius: 50%; }
        .status-pending { background-color: #e0f2fe; color: #0284c7; }
        .status-pending::before { content:''; width: 6px; height: 6px; background: #0284c7; border-radius: 50%; }
        
        /* ACTION BTN */
        .action-btn { background: none; border: none; color: #94a3b8; font-size: 18px; cursor: pointer; transition: color 0.2s; }
        .action-btn:hover { color: var(--primary); }

        /* WIDGET LIST (FOODS & STAFFS) */
        .side-list { display: flex; flex-direction: column; gap: 16px; }
        .side-item { display: flex; align-items: center; justify-content: space-between; }
        .item-info { display: flex; align-items: center; gap: 12px; }
        .item-img-mock { width: 44px; height: 44px; border-radius: 10px; object-fit: cover; background: #cbd5e1; }
        .item-text-title { font-size: 14px; font-weight: 600; color: var(--text-main); }
        .item-text-sub { font-size: 12px; color: var(--text-sub); margin-top: 1px; }
        .item-value-box { text-align: right; }
        .item-value { font-size: 14px; font-weight: 700; color: var(--text-main); }
        .item-percent { font-size: 11px; font-weight: 600; margin-top: 2px; }
        
        /* STAFF LIST AVATAR */
        .staff-avatar-status { position: relative; }
        .status-dot-active { position: absolute; width: 10px; height: 10px; background: var(--success); border: 2px solid #fff; border-radius: 50%; bottom: 2px; right: 2px; }
    </style>
</head>
<body>

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
                <li class="active"><a href="${pageContext.request.contextPath}/admin-dashboard"><i class='bx bxs-dashboard'></i>Tổng quan</a></li>
                <li><a href="${pageContext.request.contextPath}/admin-menu"><i class='bx bx-dish'></i>Thực đơn</a></li>
                <li><a href="#"><i class='bx bx-package'></i>Kho nguyên liệu</a></li>
                <li><a href="${pageContext.request.contextPath}/admin-employees"><i class='bx bx-user-voice'></i>Nhân viên</a></li>
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
            <div class="search-wrapper">
                <i class='bx bx-search'></i>
                <input type="text" placeholder="Tìm kiếm hóa đơn, món ăn, nhân viên...">
            </div>
            <div class="top-right-actions">
                <button class="nav-icon-btn"><i class='bx bx-bell'></i><span class="dot"></span></button>
                <button class="nav-icon-btn"><i class='bx bx-cog'></i></button>
                <div class="date-badge">
                    <i class='bx bx-calendar'></i>
                    <span>Hôm nay: 15 Th05, 2024</span>
                </div>
                <button class="btn-add-order"><i class='bx bx-plus'></i> Tạo đơn mới</button>
            </div>
        </div>

        <h2 class="header-title">Chào buổi sáng, Admin!</h2>
        <p class="header-sub">Đây là những gì đang diễn ra tại cửa hàng của bạn hôm nay.</p>

        <div class="cards-container">
            <div class="card rev-theme">
                <div class="card-top">
                    <div>
                        <p class="card-title">Tổng doanh thu</p>
                        <h3 class="card-value">
                            <fmt:formatNumber value="${stats.totalRevenue}" type="currency" currencySymbol="đ" maxFractionDigits="0"/>
                        </h3>
                    </div>
                    <div class="card-icon-box"><i class='bx bx-wallet'></i></div>
                </div>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <span class="trend-tag trend-up"><i class='bx bx-trending-up'></i> +12.5%</span>
                    <span style="font-size: 11px; color: var(--text-sub);">so với hôm qua</span>
                </div>
            </div>
            
            <div class="card ord-theme">
                <div class="card-top">
                    <div>
                        <p class="card-title">Tổng đơn hàng</p>
                        <h3 class="card-value">${stats.totalOrders}</h3>
                    </div>
                    <div class="card-icon-box"><i class='bx bx-shopping-bag'></i></div>
                </div>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <span class="trend-tag trend-up"><i class='bx bx-trending-up'></i> +8.2%</span>
                    <span style="font-size: 11px; color: var(--text-sub);">mượt mà</span>
                </div>
            </div>
            
            <div class="card stk-theme">
                <div class="card-top">
                    <div>
                        <p class="card-title">Sắp hết hàng</p>
                        <h3 class="card-value">${stats.lowStockCount} <span style="font-size: 14px; font-weight: normal; color: var(--text-sub);">mục</span></h3>
                    </div>
                    <div class="card-icon-box"><i class='bx bx-error-circle'></i></div>
                </div>
                <div>
                    <div class="progress-bar-container">
                        <div class="progress-bar-fill" style="width: 85%; background: var(--danger);"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-sub); margin-top: 4px;">
                        <span>Mức cảnh báo</span>
                        <span style="color: var(--danger); font-weight: 600;">85%</span>
                    </div>
                </div>
            </div>

            <div class="card emp-theme">
                <div class="card-top">
                    <div>
                        <p class="card-title">Nhân viên đang làm</p>
                        <h3 class="card-value">08/12</h3>
                    </div>
                    <div class="card-icon-box"><i class='bx bx-group'></i></div>
                </div>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <span class="trend-tag trend-stable">Ổn định</span>
                    <span style="font-size: 11px; color: var(--text-sub);">Đang trong ca trực</span>
                </div>
            </div>
        </div>

        <div class="dashboard-grid-top">
            <div class="panel-box">
                <div class="panel-title">
                    <span>Biểu đồ doanh thu</span>
                    <span style="font-size: 13px; font-weight: 500; color: var(--text-sub);">Thống kê chi tiết theo giờ</span>
                </div>
                <div class="chart-placeholder">
                    <svg viewBox="0 0 500 150" width="100%" height="100%" preserveAspectRatio="none">
                        <defs>
                            <linearGradient id="chart-grad" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stop-color="#0284c7" stop-opacity="0.25"/>
                                <stop offset="100%" stop-color="#0284c7" stop-opacity="0.0"/>
                            </linearGradient>
                        </defs>
                        <path d="M 0,110 Q 50,80 100,100 T 200,90 T 300,40 T 400,120 T 500,30 L 500,150 L 0,150 Z" fill="url(#chart-grad)" />
                        <path d="M 0,110 Q 50,80 100,100 T 200,90 T 300,40 T 400,120 T 500,30" fill="none" stroke="#0284c7" stroke-width="3" stroke-linecap="round" />
                    </svg>
                    <div style="position: absolute; width: 100%; display: flex; justify-content: space-between; padding: 0 10px; bottom: -20px; font-size: 11px; color: var(--text-sub);">
                        <span>07:00</span><span>10:00</span><span>13:00</span><span>16:00</span><span>19:00</span><span>22:00</span>
                    </div>
                </div>
            </div>

            <div class="panel-box">
                <div class="panel-title">
                    <span>Món bán chạy</span>
                    <a href="#" class="panel-link">Tất cả</a>
                </div>
                <div class="side-list">
                    <div class="side-item">
                        <div class="item-info">
                            <div class="item-img-mock" style="background: url('https://images.unsplash.com/photo-1541167760496-1628856ab772?q=80&w=100') center/cover;"></div>
                            <div>
                                <span class="item-text-title">Latte Đá Hạnh Nhân</span>
                                <p class="item-text-sub">42 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="item-value-box">
                            <span class="item-value">55k</span>
                            <p class="item-percent" style="color: var(--success);">+15%</p>
                        </div>
                    </div>
                    <div class="side-item">
                        <div class="item-info">
                            <div class="item-img-mock" style="background: url('https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?q=80&w=100') center/cover;"></div>
                            <div>
                                <span class="item-text-title">Americano Cam Sả</span>
                                <p class="item-text-sub">38 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="item-value-box">
                            <span class="item-value">45k</span>
                            <p class="item-percent" style="color: var(--success);">+8%</p>
                        </div>
                    </div>
                    <div class="side-item">
                        <div class="item-info">
                            <div class="item-img-mock" style="background: url('https://images.unsplash.com/photo-1555507036-ab1f4038808a?q=80&w=100') center/cover;"></div>
                            <div>
                                <span class="item-text-title">Bánh Sừng Bò Bơ</span>
                                <p class="item-text-sub">29 đơn hôm nay</p>
                            </div>
                        </div>
                        <div class="item-value-box">
                            <span class="item-value">35k</span>
                            <p class="item-percent" style="color: var(--text-sub);">0%</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="dashboard-grid-bottom">
            <div class="panel-box">
                <div class="panel-title">
                    <span>Nhân viên trực</span>
                </div>
                <div class="side-list">
                    <div class="side-item">
                        <div class="item-info">
                            <div class="staff-avatar-status">
                                <div class="avatar-circle" style="background: #3b82f6; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 12px;">TT</div>
                                <span class="status-dot-active"></span>
                            </div>
                            <div>
                                <span class="item-text-title">Minh Tuấn</span>
                                <p class="item-text-sub">Quầy Bar • 07:00 - 15:00</p>
                            </div>
                        </div>
                        <button class="action-btn"><i class='bx bx-dots-vertical-rounded'></i></button>
                    </div>
                    <div class="side-item">
                        <div class="item-info">
                            <div class="staff-avatar-status">
                                <div class="avatar-circle" style="background: #ec4899; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 12px;">HM</div>
                                <span class="status-dot-active"></span>
                            </div>
                            <div>
                                <span class="item-text-title">Hà My</span>
                                <p class="item-text-sub">Phục vụ • 08:00 - 16:00</p>
                            </div>
                        </div>
                        <button class="action-btn"><i class='bx bx-dots-vertical-rounded'></i></button>
                    </div>
                    <div class="side-item">
                        <div class="item-info">
                            <div class="staff-avatar-status">
                                <div class="avatar-circle" style="background: #10b981; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 12px;">AA</div>
                                <span class="status-dot-active"></span>
                            </div>
                            <div>
                                <span class="item-text-title">Đức Anh</span>
                                <p class="item-text-sub">Thu ngân • 07:00 - 15:00</p>
                            </div>
                        </div>
                        <button class="action-btn"><i class='bx bx-dots-vertical-rounded'></i></button>
                    </div>
                </div>
            </div>

            <div class="panel-box">
                <div class="panel-title">
                    <span>Đơn hàng gần đây</span>
                    <a href="#" class="panel-link">Xuất báo cáo</a>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Mã Đơn</th>
                            <th>Thời Gian</th>
                            <th>Bàn</th>
                            <th>Giá Trị</th>
                            <th>Trạng Thái</th>
                            <th style="text-align: center;">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="order" items="${stats.recentOrders}">
                            <tr>
                                <td style="font-weight: 600; color: #00629b;">#CF${order.orderId}</td>
                                <td>${order.timeOrder}</td>
                                <td>${order.tableName}</td>
                                <td style="font-weight: 600;">
                                    <fmt:formatNumber value="${order.finalAmount}" type="currency" currencySymbol="đ" maxFractionDigits="0"/>
                                </td>
                                <td>
                                    <span class="status ${order.orderStatus == 'Completed' ? 'status-completed' : 'status-pending'}">
                                        ${order.orderStatus == 'Completed' ? 'Đã thanh toán' : 'Đang xử lý'}
                                    </span>
                                </td>
                                <td style="text-align: center;">
                                    <button class="action-btn" title="Xem chi tiết đơn"><i class='bx bx-show'></i></button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty stats.recentOrders}">
                            <tr>
                                <td colspan="6" style="text-align: center; color: #94a3b8; padding: 40px;">Không có đơn hàng nào phát sinh hôm nay.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>

    </div>

</body>
</html>
