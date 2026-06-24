<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - Coffee POS</title>
    <!-- Tailwind CSS Play CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        coffee: {
                            50: '#fdf8f6',
                            100: '#f2e8e5',
                            200: '#e6d0cb',
                            350: '#c6a58b',
                            800: '#4a2c2a',
                            900: '#3c2221',
                            950: '#2b1716',
                        },
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
        /* Smooth transitions */
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
    <div class="w-full max-w-5xl bg-white rounded-3xl shadow-xl border border-gray-150 overflow-hidden grid grid-cols-1 md:grid-cols-2 min-h-[580px] fade-in">
        
        <!-- LEFT SIDE: Brand Banner (Dark Gradient) -->
        <div class="relative hidden md:flex flex-col justify-between p-10 bg-gradient-to-tr from-[#1b3435] via-[#2c4e40] to-[#0f2c27] text-white">
            <!-- Background Decorative Blur -->
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
                    Nâng tầm vận hành,<br>
                    <span class="text-emerald-400">trọn vẹn</span> hương vị.
                </h1>
                <p class="text-gray-300/95 text-sm leading-relaxed max-w-md">
                    Giải pháp quản lý quán cafe toàn diện giúp bạn tối ưu hóa quy trình phục vụ, quản lý kho hàng chuẩn xác và gia tăng doanh thu mỗi ngày.
                </p>
            </div>

            <!-- Bottom Stats -->
            <div class="relative z-10 grid grid-cols-2 gap-6 pt-6 border-t border-white/10">
                <div>
                    <h3 class="text-2xl font-bold text-white tracking-tight">500+</h3>
                    <p class="text-[10px] text-gray-400 font-bold uppercase tracking-wider mt-0.5">Cửa hàng tin dùng</p>
                </div>
                <div>
                    <h3 class="text-2xl font-bold text-white tracking-tight">99.9%</h3>
                    <p class="text-[10px] text-gray-400 font-bold uppercase tracking-wider mt-0.5">Độ ổn định</p>
                </div>
            </div>
        </div>

        <!-- RIGHT SIDE: Login Form Panel -->
        <div class="flex flex-col justify-between p-8 sm:p-12 md:p-14 bg-white">
            
            <!-- Empty element just to push content down (flex layout helper) -->
            <div></div>

            <!-- Form Content -->
            <div class="space-y-6">
                <!-- Header -->
                <div class="space-y-2">
                    <h2 class="text-2xl font-bold text-gray-900 tracking-tight">Chào mừng trở lại!</h2>
                    <p class="text-sm text-gray-500">Vui lòng đăng nhập vào tài khoản quản lý của bạn.</p>
                </div>

                <!-- Success Message Alert -->
                <% 
                    String registered = request.getParameter("registered");
                    if ("success".equals(registered)) {
                %>
                    <div class="flex items-center gap-2.5 p-3.5 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-700 text-xs font-semibold animate-bounce">
                        <i class="fa-solid fa-circle-check text-sm"></i>
                        <span>Đăng ký thành công! Vui lòng đăng nhập tài khoản của bạn.</span>
                    </div>
                <% } %>

                <!-- Error Message Alert -->
                <% 
                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                %>
                    <div class="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 border border-red-100 text-red-700 text-xs font-semibold animate-pulse">
                        <i class="fa-solid fa-circle-exclamation text-sm"></i>
                        <span><%= error %></span>
                    </div>
                <% } %>

                <!-- Input Forms -->
                <form action="login" method="POST" class="space-y-4">
                    <!-- Username/Email Field -->
                    <div class="space-y-1.5">
                        <label for="username" class="text-xs font-bold text-gray-700 uppercase tracking-wider">Email hoặc Tên đăng nhập</label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 flex items-center pl-3.5 text-gray-400">
                                <i class="fa-regular fa-user"></i>
                            </span>
                            <input 
                                type="text" 
                                id="username" 
                                name="username" 
                                required
                                value="<%= request.getAttribute("savedUsername") != null ? request.getAttribute("savedUsername") : "" %>"
                                placeholder="example@cafe.com" 
                                class="w-full pl-10 pr-4 py-3 text-sm bg-slate-50 border border-slate-200/80 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 focus:bg-white transition-all text-gray-900 placeholder:text-gray-400 font-medium"
                            >
                        </div>
                    </div>

                    <!-- Password Field -->
                    <div class="space-y-1.5">
                        <label for="password" class="text-xs font-bold text-gray-700 uppercase tracking-wider">Mật khẩu</label>
                        <div class="relative">
                            <span class="absolute inset-y-0 left-0 flex items-center pl-3.5 text-gray-400">
                                <i class="fa-solid fa-lock"></i>
                            </span>
                            <input 
                                type="password" 
                                id="password" 
                                name="password" 
                                required
                                placeholder="••••••••" 
                                class="w-full pl-10 pr-10 py-3 text-sm bg-slate-50 border border-slate-200/80 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 focus:bg-white transition-all text-gray-900 placeholder:text-gray-400 font-medium"
                            >
                            <!-- Eye icon button to toggle password visibility -->
                            <button 
                                type="button" 
                                onclick="togglePasswordVisibility()" 
                                class="absolute inset-y-0 right-0 flex items-center pr-3.5 text-gray-400 hover:text-emerald-600 transition-colors"
                            >
                                <i id="eye-icon" class="fa-regular fa-eye"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Remember & Forgot Password -->
                    <div class="flex items-center justify-between pt-1">
                        <label class="flex items-center gap-2 cursor-pointer select-none">
                            <input type="checkbox" name="remember" class="w-4 h-4 rounded border-gray-300 text-emerald-600 focus:ring-emerald-500/20">
                            <span class="text-xs text-gray-600 font-medium">Ghi nhớ đăng nhập</span>
                        </label>
                        <a href="#" class="text-xs font-semibold text-emerald-600 hover:text-emerald-700 transition-colors">Quên mật khẩu?</a>
                    </div>

                    <!-- Submit Button -->
                    <button 
                        type="submit" 
                        class="w-full mt-4 flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-[#006064] hover:bg-[#004d40] text-white font-bold text-sm shadow-md shadow-[#006064]/10 hover:shadow-lg transition-all focus:outline-none focus:ring-2 focus:ring-[#006064]/30"
                    >
                        <span>Đăng nhập</span>
                        <i class="fa-solid fa-arrow-right-long text-xs"></i>
                    </button>
                </form>

                <!-- Divider -->
                <div class="relative flex items-center justify-center my-5">
                    <div class="w-full border-t border-slate-100"></div>
                    <span class="absolute bg-white px-3 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Hoặc</span>
                </div>

                <!-- Registration / Support -->
                <p class="text-center text-xs text-slate-500 font-medium">
                    Chưa có tài khoản hệ thống? 
                    <a href="register" class="font-bold text-emerald-600 hover:text-emerald-700 transition-colors">Đăng ký ngay</a>
                </p>
            </div>

            <!-- Footer Copy -->
            <p class="text-[10px] text-center text-slate-400 font-semibold tracking-wide pt-8">
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
