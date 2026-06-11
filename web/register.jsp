<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký - Coffee POS</title>
    <!-- Tailwind CSS Play CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        posblue: {
                            600: '#0284c7',
                            700: '#0369a1',
                            800: '#075985',
                            900: '#0c4a6e',
                        }
                    },
                    fontFamily: {
                        sans: ['Outfit', 'sans-serif'],
                    }
                }
            }
        }
    </script>
    <!-- Google Fonts: Outfit -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        .fade-in {
            animation: fadeIn 0.6s ease-out forwards;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
    </style>
</head>
<body class="bg-[#f3f4f6] flex items-center justify-center min-h-screen p-4 md:p-6 font-sans">

    <!-- Container Card (Split Screen Layout) -->
    <div class="w-full max-w-5xl bg-white rounded-3xl shadow-xl border border-gray-150 overflow-hidden grid grid-cols-1 md:grid-cols-2 min-h-[620px] fade-in">
        
        <!-- LEFT SIDE: Brand Banner -->
        <div class="relative hidden md:flex flex-col justify-between p-10 bg-gradient-to-tr from-[#1b3435] via-[#2c4e40] to-[#0f2c27] text-white">
            <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.08),transparent_50%)]"></div>
            
            <!-- Logo Badge -->
            <div class="relative z-10 flex items-center gap-2.5 bg-white/10 border border-white/10 backdrop-blur-md px-4 py-2.5 rounded-2xl w-fit">
                <div class="w-8 h-8 rounded-xl bg-white/20 flex items-center justify-center">
                    <i class="fa-solid fa-mug-hot text-emerald-400"></i>
                </div>
                <div>
                    <h4 class="text-xs font-bold tracking-wider uppercase text-white">Coffee POS</h4>
                    <p class="text-[9px] text-emerald-300 font-medium mt-0.5">Hệ thống quản lý chuyên nghiệp</p>
                </div>
            </div>

            <!-- Central Content -->
            <div class="relative z-10 space-y-4 my-auto">
                <h1 class="text-3xl lg:text-4xl font-bold leading-tight tracking-tight">
                    Trải nghiệm hương vị,<br>
                    <span class="text-emerald-400">tích lũy</span> ưu đãi.
                </h1>
                <p class="text-gray-300/95 text-sm leading-relaxed max-w-md">
                    Đăng ký tài khoản thành viên ngay hôm nay để nhận thông tin ưu đãi hấp dẫn, xem thực đơn online và tích điểm đổi quà từ cửa hàng của chúng tôi.
                </p>
            </div>

            <!-- Bottom Stats -->
            <div class="relative z-10 grid grid-cols-2 gap-6 pt-6 border-t border-white/10">
                <div>
                    <h3 class="text-2xl font-bold text-white tracking-tight">10s</h3>
                    <p class="text-[10px] text-gray-400 font-bold uppercase tracking-wider mt-0.5">Đăng ký nhanh chóng</p>
                </div>
                <div>
                    <h3 class="text-2xl font-bold text-white tracking-tight">Free</h3>
                    <p class="text-[10px] text-gray-400 font-bold uppercase tracking-wider mt-0.5">Mở tài khoản thành viên</p>
                </div>
            </div>
        </div>

        <!-- RIGHT SIDE: Registration Form Panel -->
        <div class="flex flex-col justify-between p-8 sm:p-10 bg-white overflow-y-auto">
            <div></div>

            <!-- Form Content -->
            <div class="space-y-5">
                <div class="space-y-1">
                    <h2 class="text-2xl font-bold text-gray-900 tracking-tight">Đăng ký tài khoản</h2>
                    <p class="text-xs text-gray-500">Tạo tài khoản khách hàng mới chỉ trong vài bước đơn giản.</p>
                </div>

                <!-- Error Message Alert -->
                <% 
                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                %>
                    <div class="flex items-center gap-2.5 p-3 rounded-xl bg-red-50 border border-red-100 text-red-700 text-xs font-semibold animate-pulse">
                        <i class="fa-solid fa-circle-exclamation text-sm"></i>
                        <span><%= error %></span>
                    </div>
                <% } %>

                <form action="register" method="POST" class="space-y-3">
                    <!-- Full Name -->
                    <div class="space-y-1">
                        <label for="fullName" class="text-[10px] font-bold text-gray-700 uppercase tracking-wider">Họ và Tên <span class="text-red-500">*</span></label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                                <i class="fa-regular fa-id-card text-xs"></i>
                            </span>
                            <input 
                                type="text" 
                                id="fullName" 
                                name="fullName" 
                                required
                                value="<%= request.getAttribute("oldFullName") != null ? request.getAttribute("oldFullName") : "" %>"
                                placeholder="Nguyễn Văn A" 
                                class="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/85 focus:bg-white transition-all text-gray-900 font-medium"
                            >
                        </div>
                    </div>

                    <!-- Email & Phone in Grid -->
                    <div class="grid grid-cols-2 gap-3">
                        <div class="space-y-1">
                            <label for="email" class="text-[10px] font-bold text-gray-700 uppercase tracking-wider">Email</label>
                            <div class="relative">
                                <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                                    <i class="fa-regular fa-envelope text-xs"></i>
                                </span>
                                <input 
                                    type="email" 
                                    id="email" 
                                    name="email" 
                                    value="<%= request.getAttribute("oldEmail") != null ? request.getAttribute("oldEmail") : "" %>"
                                    placeholder="email@example.com" 
                                    class="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/85 focus:bg-white transition-all text-gray-900 font-medium"
                                >
                            </div>
                        </div>

                        <div class="space-y-1">
                            <label for="phone" class="text-[10px] font-bold text-gray-700 uppercase tracking-wider">Số điện thoại</label>
                            <div class="relative">
                                <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                                    <i class="fa-solid fa-phone text-xs"></i>
                                </span>
                                <input 
                                    type="text" 
                                    id="phone" 
                                    name="phone" 
                                    value="<%= request.getAttribute("oldPhone") != null ? request.getAttribute("oldPhone") : "" %>"
                                    placeholder="09xxxxxxxx" 
                                    class="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/85 focus:bg-white transition-all text-gray-900 font-medium"
                                >
                            </div>
                        </div>
                    </div>

                    <!-- Username -->
                    <div class="space-y-1">
                        <label for="username" class="text-[10px] font-bold text-gray-700 uppercase tracking-wider">Tên đăng nhập <span class="text-red-500">*</span></label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                                <i class="fa-regular fa-user text-xs"></i>
                            </span>
                            <input 
                                type="text" 
                                id="username" 
                                name="username" 
                                required
                                value="<%= request.getAttribute("oldUsername") != null ? request.getAttribute("oldUsername") : "" %>"
                                placeholder="username123" 
                                class="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/85 focus:bg-white transition-all text-gray-900 font-medium"
                            >
                        </div>
                    </div>

                    <!-- Password -->
                    <div class="space-y-1">
                        <label for="password" class="text-[10px] font-bold text-gray-700 uppercase tracking-wider">Mật khẩu <span class="text-red-500">*</span></label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
                                <i class="fa-solid fa-lock text-xs"></i>
                            </span>
                            <input 
                                type="password" 
                                id="password" 
                                name="password" 
                                required
                                placeholder="••••••••" 
                                class="w-full pl-9 pr-10 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/85 focus:bg-white transition-all text-gray-900 font-medium"
                            >
                            <button 
                                type="button" 
                                onclick="togglePasswordVisibility()" 
                                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-emerald-600 transition-colors"
                            >
                                <i id="eye-icon" class="fa-regular fa-eye text-xs"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Submit Button -->
                    <button 
                        type="submit" 
                        class="w-full mt-4 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white font-bold text-xs shadow-md shadow-[#006064]/10 hover:shadow-lg transition-all focus:outline-none"
                    >
                        <span>Đăng ký thành viên</span>
                        <i class="fa-solid fa-user-plus text-[10px]"></i>
                    </button>
                </form>

                <!-- Divider -->
                <div class="relative flex items-center justify-center my-4">
                    <div class="w-full border-t border-slate-100"></div>
                    <span class="absolute bg-white px-3 text-[9px] font-bold text-slate-400 uppercase tracking-widest">Hoặc</span>
                </div>

                <!-- Link to Login -->
                <p class="text-center text-xs text-slate-500 font-medium">
                    Đã có tài khoản hệ thống? 
                    <a href="login" class="font-bold text-emerald-600 hover:text-emerald-700 transition-colors">Đăng nhập ngay</a>
                </p>
            </div>

            <!-- Footer Copy -->
            <p class="text-[9px] text-center text-slate-400 font-semibold tracking-wide pt-6">
                &copy; 2026 Coffee POS Management System. All rights reserved.
            </p>
        </div>
    </div>

    <!-- Password visibility toggle script -->
    <script>
        function togglePasswordVisibility() {
            const passwordInput = document.getElementById('password');
            const eyeIcon = document.getElementById('eye-icon');
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                eyeIcon.classList.remove('fa-eye');
                eyeIcon.classList.add('fa-eye-slash');
            } else {
                passwordInput.type = 'password';
                eyeIcon.classList.remove('fa-eye-slash');
                eyeIcon.classList.add('fa-eye');
            }
        }
    </script>
</body>
</html>
