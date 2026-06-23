<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.mycoffee.model.User"%>
<%
    // Lấy thông tin user đăng nhập từ Session
    User loggedInUser = (User) session.getAttribute("user");
    String fullName = (loggedInUser != null) ? loggedInUser.getFullName() : "Khách hàng";
    String shortName = "";
    if (fullName != null && !fullName.trim().isEmpty()) {
        String[] parts = fullName.split(" ");
        shortName = parts[parts.length - 1]; // Lấy tên cuối
    } else {
        shortName = "KH";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thực Đơn - Coffee House</title>
    <!-- Tailwind CSS Play CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
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
            animation: fadeIn 0.5s ease-out forwards;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
    </style>
</head>
<body class="bg-[#f4f7fc]/50 h-screen flex font-sans overflow-hidden">

    <!-- Sidebar -->
    <jsp:include page="common/sidebar_customer.jsp" />

    <!-- Main Wrapper -->
    <div class="flex-1 flex flex-col h-screen overflow-hidden relative bg-[#f4f7fc]/50">

        <!-- Top Navigation Header -->
        <header class="h-20 bg-white border-b border-slate-200/50 flex items-center justify-between px-8 shadow-sm shrink-0 z-40">
        <!-- Logo -->
        <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-[#006064] flex items-center justify-center text-white shadow-md shadow-[#006064]/20">
                <i class="fa-solid fa-mug-hot text-lg"></i>
            </div>
            <div>
                <h4 class="text-sm font-bold tracking-tight text-slate-800 leading-tight">Coffee House</h4>
                <p class="text-[10px] text-slate-400 font-medium">Hương vị thượng hạng</p>
            </div>
        </div>

        <!-- User profile & Logout -->
        <div class="flex items-center gap-6">
            <!-- User Info -->
            <div class="flex items-center gap-2.5 bg-slate-50 border border-slate-200/40 px-3 py-1.5 rounded-2xl">
                <div class="w-7 h-7 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs shadow-inner">
                    <%= shortName.toUpperCase() %>
                </div>
                <div class="hidden sm:block text-left">
                    <h5 class="text-xs font-bold text-slate-800">Xin chào, <%= fullName %>!</h5>
                    <p class="text-[9px] text-[#006064] font-bold">Thành viên Thân Thiết</p>
                </div>
            </div>

            <!-- Vertical Separator -->
            <div class="w-px h-6 bg-slate-200"></div>

            <!-- Logout Button -->
            <a 
                href="login?action=logout" 
                class="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold transition-all border border-red-100/50 shadow-sm"
            >
                <i class="fa-solid fa-right-from-bracket text-xs"></i>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <!-- Main Content Area -->
    <main class="flex-1 overflow-y-auto p-6 md:p-8">
        <div class="max-w-7xl w-full mx-auto flex flex-col lg:flex-row gap-8 relative">
        
        <!-- Left Side: Menu Grid (Width 2/3) -->
        <div class="flex-1 space-y-8 fade-in">
            <!-- Hero banner card -->
            <div class="bg-gradient-to-r from-[#006064] to-[#004d40] text-white p-8 rounded-3xl shadow-sm relative overflow-hidden flex flex-col justify-between h-44">
                <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.08),transparent_50%)]"></div>
                <div class="z-10 max-w-lg">
                    <span class="px-2.5 py-1 rounded-full bg-white/20 text-[9px] font-bold uppercase tracking-wider text-teal-100">Chào mừng bạn mới</span>
                    <h2 class="text-2xl font-bold tracking-tight mt-2.5">Thực Đơn Coffee House</h2>
                    <p class="text-xs text-teal-100/80 font-medium mt-1 leading-relaxed">
                        Tất cả sản phẩm đều được pha chế từ nguyên liệu sạch tươi 100% trong ngày. Chúc bạn có một ngày tràn đầy năng lượng!
                    </p>
                </div>
            </div>

            <!-- Categories Filter Tabs -->
            <div class="flex items-center gap-2 border-b border-slate-250/20 pb-1">
                <button onclick="filterMenu('all')" id="tab-all" class="px-4 py-2 rounded-xl text-xs font-bold bg-[#006064] text-white transition-all">
                    Tất cả
                </button>
                <button onclick="filterMenu('coffee')" id="tab-coffee" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all">
                    Cà Phê
                </button>
                <button onclick="filterMenu('tea')" id="tab-tea" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all">
                    Trà Trái Cây
                </button>
                <button onclick="filterMenu('cake')" id="tab-cake" class="px-4 py-2 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all">
                    Bánh Ngọt
                </button>
            </div>

            <!-- Products Grid -->
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6" id="products-grid">
                <!-- Product Card 1 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="coffee">
                    <div>
                        <!-- Image placeholder with icon -->
                        <div class="w-full h-44 rounded-2xl bg-orange-50/50 text-orange-500 flex items-center justify-center border border-orange-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-mug-hot text-5xl"></i>
                            <span class="absolute top-3 left-3 bg-red-500 text-white text-[9px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">Hot</span>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Latte Đá Hạnh Nhân</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Cà phê espresso hòa quyện cùng sữa tươi béo ngậy và hương vị quả hạnh nhân đặc trưng.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">55.000đ</span>
                        <button onclick="addToCart('Latte Đá Hạnh Nhân', 55000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>

                <!-- Product Card 2 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="coffee">
                    <div>
                        <div class="w-full h-44 rounded-2xl bg-blue-50/50 text-blue-500 flex items-center justify-center border border-blue-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-whiskey-glass text-5xl"></i>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Americano Cam Sả</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Cà phê Americano đá thanh mát kết hợp lát cam tươi mọng nước và sả thơm nồng.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">45.000đ</span>
                        <button onclick="addToCart('Americano Cam Sả', 45000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>

                <!-- Product Card 3 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="cake">
                    <div>
                        <div class="w-full h-44 rounded-2xl bg-amber-50/50 text-amber-500 flex items-center justify-center border border-amber-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-stroopwafel text-5xl"></i>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Bánh Sừng Bò Bơ</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Bánh sừng bò nướng chín giòn với hương thơm ngào ngạt của bơ lạt nguyên chất Pháp.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">35.000đ</span>
                        <button onclick="addToCart('Bánh Sừng Bò Bơ', 35000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>

                <!-- Product Card 4 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="tea">
                    <div>
                        <div class="w-full h-44 rounded-2xl bg-teal-50/50 text-teal-600 flex items-center justify-center border border-teal-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-leaf text-5xl"></i>
                            <span class="absolute top-3 left-3 bg-red-500 text-white text-[9px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">Hot</span>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Trà Nhãn Sen Vàng</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Nước trà lài ô long thanh tao, nhãn lồng chín ngọt lịm kết hợp củ sen bùi béo.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">50.000đ</span>
                        <button onclick="addToCart('Trà Nhãn Sen Vàng', 50000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>

                <!-- Product Card 5 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="coffee">
                    <div>
                        <div class="w-full h-44 rounded-2xl bg-rose-50/50 text-rose-500 flex items-center justify-center border border-rose-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-mug-saucer text-5xl"></i>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Cappuccino Nóng</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Cà phê espresso sánh đậm phủ một lớp bọt sữa dày mịn trang trí hình nghệ thuật.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">48.000đ</span>
                        <button onclick="addToCart('Cappuccino Nóng', 48000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>

                <!-- Product Card 6 -->
                <div class="bg-white rounded-3xl border border-slate-200/60 shadow-sm p-4 hover:shadow-md transition-all duration-300 group flex flex-col justify-between h-96 product-card" data-category="cake">
                    <div>
                        <div class="w-full h-44 rounded-2xl bg-pink-50/50 text-pink-500 flex items-center justify-center border border-pink-100/50 shadow-inner group-hover:scale-[1.02] transition-transform duration-300 relative">
                            <i class="fa-solid fa-cheese text-5xl"></i>
                        </div>
                        <h4 class="text-sm font-bold text-slate-800 mt-4">Bánh Mousse Dâu Tây</h4>
                        <p class="text-[10px] text-slate-400 font-medium mt-1 leading-relaxed">
                            Mousse dâu kem cheese dẻo mềm mọng nước, chua ngọt dễ ăn thanh mát.
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                        <span class="text-sm font-extrabold text-slate-800">40.000đ</span>
                        <button onclick="addToCart('Bánh Mousse Dâu Tây', 40000)" class="w-8 h-8 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-600 flex items-center justify-center shadow-sm transition-colors">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right Side: Interactive Cart Panel (Width 1/3) -->
        <aside class="w-full lg:w-96 bg-white rounded-3xl border border-slate-200/60 shadow-sm p-6 flex flex-col justify-between h-[600px] shrink-0 sticky top-28">
            <div class="space-y-6 flex-1 overflow-y-auto">
                <div class="flex items-center justify-between border-b border-slate-100 pb-3">
                    <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                        <i class="fa-solid fa-shopping-cart text-[#006064]"></i>
                        Giỏ hàng của bạn
                    </h3>
                    <span id="cart-count" class="bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">0</span>
                </div>

                <!-- Empty Cart State -->
                <div id="empty-cart" class="flex flex-col items-center justify-center py-12 gap-3">
                    <i class="fa-solid fa-bag-shopping text-slate-200 text-5xl"></i>
                    <p class="text-xs text-slate-400 font-medium">Giỏ hàng của bạn đang trống!</p>
                </div>

                <!-- Cart Items List -->
                <div id="cart-list" class="space-y-4 hidden">
                    <!-- Dynamic Cart Items injected here -->
                </div>
            </div>

            <!-- Summary section -->
            <div class="pt-6 border-t border-slate-100 space-y-4">
                <div class="flex justify-between text-xs font-bold text-slate-700">
                    <span>Tổng tiền thanh toán</span>
                    <span id="cart-total" class="text-sm text-red-500 font-extrabold">0đ</span>
                </div>

                <!-- Checkout button -->
                <button onclick="checkout()" class="w-full py-3 bg-[#006064] hover:bg-[#004d40] text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2">
                    <span>Đặt hàng ngay</span>
                    <i class="fa-solid fa-credit-card text-[10px]"></i>
                </button>
            </div>
        </aside>
        </div>
    </main>

    <!-- Footer -->
    <footer class="bg-white border-t border-slate-200/50 py-6 text-center text-[10px] text-slate-400 font-semibold mt-auto shrink-0 z-40 relative">
        &copy; 2026 Coffee House Management System. All rights reserved.
    </footer>
    </div>

    <!-- Interactive script -->
    <script>
        let cart = [];

        function filterMenu(category) {
            // Update tabs active state
            const tabs = ['all', 'coffee', 'tea', 'cake'];
            tabs.forEach(tab => {
                const btn = document.getElementById(`tab-${tab}`);
                if (tab === category) {
                    btn.classList.add('bg-[#006064]', 'text-white');
                    btn.classList.remove('text-slate-500', 'hover:bg-slate-100', 'hover:text-slate-800');
                } else {
                    btn.classList.remove('bg-[#006064]', 'text-white');
                    btn.classList.add('text-slate-500', 'hover:bg-slate-100', 'hover:text-slate-800');
                }
            });

            // Filter cards
            const cards = document.querySelectorAll('.product-card');
            cards.forEach(card => {
                const cat = card.getAttribute('data-category');
                if (category === 'all' || cat === category) {
                    card.classList.remove('hidden');
                } else {
                    card.classList.add('hidden');
                }
            });
        }

        function addToCart(name, price) {
            // Find item in cart
            const index = cart.findIndex(item => item.name === name);
            if (index > -1) {
                cart[index].quantity += 1;
            } else {
                cart.push({ name, price, quantity: 1 });
            }
            updateCartUI();
        }

        function changeQuantity(name, amount) {
            const index = cart.findIndex(item => item.name === name);
            if (index > -1) {
                cart[index].quantity += amount;
                if (cart[index].quantity <= 0) {
                    cart.splice(index, 1);
                }
            }
            updateCartUI();
        }

        function updateCartUI() {
            const emptyCart = document.getElementById('empty-cart');
            const cartList = document.getElementById('cart-list');
            const cartCount = document.getElementById('cart-count');
            const cartTotal = document.getElementById('cart-total');

            let count = 0;
            let total = 0;

            if (cart.length === 0) {
                emptyCart.classList.remove('hidden');
                cartList.classList.add('hidden');
                cartCount.innerText = '0';
                cartTotal.innerText = '0đ';
                return;
            }

            emptyCart.classList.add('hidden');
            cartList.classList.remove('hidden');

            cartList.innerHTML = '';
            cart.forEach(item => {
                count += item.quantity;
                total += item.price * item.quantity;

                const row = document.createElement('div');
                row.className = 'flex items-center justify-between p-2 rounded-2xl hover:bg-slate-50 border border-transparent hover:border-slate-100 transition-all';
                row.innerHTML = `
                    <div class="min-w-0 flex-1">
                        <h4 class="text-xs font-bold text-slate-800 truncate">${item.name}</h4>
                        <p class="text-[10px] text-slate-400 font-bold mt-0.5">${item.price.toLocaleString('vi-VN')}đ / ly</p>
                    </div>
                    <div class="flex items-center gap-2">
                        <button onclick="changeQuantity('${item.name}', -1)" class="w-6 h-6 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold transition-colors">-</button>
                        <span class="text-xs font-extrabold text-slate-700 w-4 text-center">${item.quantity}</span>
                        <button onclick="changeQuantity('${item.name}', 1)" class="w-6 h-6 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center text-xs font-bold transition-colors">+</button>
                    </div>
                `;
                cartList.appendChild(row);
            });

            cartCount.innerText = count;
            cartTotal.innerText = total.toLocaleString('vi-VN') + 'đ';
        }

        function checkout() {
            if (cart.length === 0) {
                alert("Giỏ hàng của bạn đang trống!");
                return;
            }
            alert("Đặt hàng thành công! Đơn hàng của bạn đã được chuyển tới quầy pha chế.");
            cart = [];
            updateCartUI();
        }
    </script>
</body>
</html>
