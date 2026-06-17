// API基础地址
const API_BASE = 'http://localhost:8080/api';

// 当前登录用户信息
let currentUser = null;
let currentToken = localStorage.getItem('token');

// 待支付商品信息
let pendingOrder = null;

// 分类映射
let categoryMap = {};

// 页面是否已加载
let isPageReady = false;

// 当前商品数量选择（用于立即购买）
let selectedQuantity = 1;
let currentProductStock = 0;

// ============ 页面初始化 ============
document.addEventListener('DOMContentLoaded', async () => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
    }
    
    checkLoginStatus();
    
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    document.getElementById('publishForm').addEventListener('submit', handlePublish);
    
    document.querySelectorAll('input[name="payment"]').forEach(radio => {
        radio.addEventListener('change', function() {
            document.querySelectorAll('.payment-option').forEach(opt => opt.classList.remove('selected'));
            if (this.checked) {
                this.parentElement.classList.add('selected');
            }
        });
    });
    
    await loadCategories();
    isPageReady = true;
    
    showPage('home');
    
    // 登录后加载通知
    if (currentUser) {
        loadUnreadCount();
        // 每30秒检查一次未读数量
        setInterval(() => {
            if (currentUser) loadUnreadCount();
        }, 30000);
    }
    
    // 点击其他地方关闭通知面板
    document.addEventListener('click', function(e) {
        const panel = document.getElementById('notificationPanel');
        const bell = document.getElementById('navNotification');
        if (panel && !panel.classList.contains('hidden') && 
            !panel.contains(e.target) && !bell.contains(e.target)) {
            panel.classList.add('hidden');
        }
    });
});

// ============ 通知功能 ============
async function loadUnreadCount() {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/user/notifications/unread-count?userId=${currentUser.id}`);
        const data = await res.json();
        
        if (data.success) {
            const count = data.data || 0;
            const badge = document.getElementById('notificationBadge');
            if (badge) {
                if (count > 0) {
                    badge.textContent = count > 99 ? '99+' : count;
                    badge.style.display = 'flex';
                } else {
                    badge.style.display = 'none';
                }
            }
        }
    } catch (err) {
        console.error('加载未读通知数失败', err);
    }
}

function showNotificationPanel() {
    event.stopPropagation();
    const panel = document.getElementById('notificationPanel');
    panel.classList.toggle('hidden');
    
    if (!panel.classList.contains('hidden')) {
        loadNotifications(1);
    }
}

function closeNotificationPanel() {
    document.getElementById('notificationPanel').classList.add('hidden');
}

async function loadNotifications(page) {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/user/notifications?userId=${currentUser.id}&pageNum=${page}&pageSize=10`);
        const data = await res.json();
        
        if (data.success) {
            renderNotifications(data.data.records);
            loadUnreadCount(); // 刷新未读数
        }
    } catch (err) {
        console.error('加载通知失败', err);
    }
}

function renderNotifications(notifications) {
    const list = document.getElementById('notificationList');
    
    if (!notifications || notifications.length === 0) {
        list.innerHTML = '<div class="notification-empty">暂无通知</div>';
        return;
    }
    
    const typeLabels = {
        system: '系统通知',
        order: '订单通知',
        audit: '审核通知'
    };
    
    list.innerHTML = notifications.map(n => {
        const unreadClass = n.isRead === 0 ? 'unread' : '';
        const typeLabel = typeLabels[n.type] || '系统通知';
        const typeClass = 'type-' + (n.type || 'system');
        
        return `
            <div class="notification-item ${unreadClass}" onclick="markNotificationRead(${n.id}, this)">
                <h4>${escapeHtml(n.title || '')}</h4>
                <p>${escapeHtml(n.content || '')}</p>
                <div class="time">
                    <span class="type-tag ${typeClass}">${typeLabel}</span>
                    <span>${formatDateTime(n.createTime)}</span>
                    ${n.isRead === 0 ? '<span style="color:#ff4757;">· 未读</span>' : '<span style="color:#52c41a;">· 已读</span>'}
                </div>
            </div>
        `;
    }).join('');
}

async function markNotificationRead(notificationId, element) {
    if (!currentUser) return;
    
    // 如果已经是已读状态，不发送请求
    if (element && element.classList.contains('unread')) {
        try {
            const res = await fetch(`${API_BASE}/user/notification/read`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    userId: currentUser.id,
                    notificationId: notificationId
                })
            });
            const data = await res.json();
            
            if (data.success) {
                element.classList.remove('unread');
                // 更新显示
                const timeSpan = element.querySelector('.time');
                if (timeSpan) {
                    timeSpan.innerHTML = timeSpan.innerHTML.replace('未读', '已读').replace('#ff4757', '#52c41a');
                }
                loadUnreadCount();
            }
        } catch (err) {
            console.error('标记已读失败', err);
        }
    }
}

async function markAllNotificationsRead() {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/user/notifications/read-all`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                userId: currentUser.id
            })
        });
        const data = await res.json();
        
        if (data.success) {
            loadNotifications(1);
        }
    } catch (err) {
        console.error('标记全部已读失败', err);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    
    // 1分钟内
    if (diff < 60000) return '刚刚';
    // 1小时内
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    // 24小时内
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    // 超过24小时显示日期
    return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

// ============ 页面导航 ============
function showPage(page, params) {
    document.querySelectorAll('[id^="page"]').forEach(el => {
        el.classList.add('hidden');
    });
    
    const targetPage = document.getElementById('page' + capitalize(page));
    if (targetPage) {
        targetPage.classList.remove('hidden');
    }
    
    // 关闭通知面板
    closeNotificationPanel();
    
    switch(page) {
        case 'home':
            loadProducts(1);
            break;
        case 'detail':
            if (params && params.productId) {
                loadProductDetail(params.productId);
            }
            break;
        case 'payment':
            if (pendingOrder) {
                renderPaymentPage();
            }
            break;
        case 'cart':
            if (currentUser) {
                loadCart();
            } else {
                showToast('请先登录', 'error');
                showPage('login');
            }
            break;
        case 'user':
            if (currentUser) loadUserInfo();
            break;
        case 'login':
        case 'register':
            if (currentUser) showPage('home');
            break;
    }
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// ============ 登录注册 ============
async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const params = {
        username: form.username.value,
        password: form.password.value
    };
    
    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
        });
        const data = await res.json();
        
        if (data.success === true) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.data));
            currentToken = data.token;
            currentUser = data.data;
            
            showToast(`欢迎回来，${currentUser.username}！`, 'success');
            checkLoginStatus();
            showPage('home');
            form.reset();
            
            // 登录后加载通知
            loadUnreadCount();
            setInterval(() => {
                if (currentUser) loadUnreadCount();
            }, 30000);
        } else {
            showToast(data.message || '登录失败', 'error');
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const form = e.target;
    const params = {
        username: form.username.value,
        password: form.password.value,
        email: form.email.value
    };
    
    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
        });
        const data = await res.json();
        
        if (data.success === true) {
            showToast('注册成功，请登录！', 'success');
            showPage('login');
            form.reset();
        } else {
            showToast(data.message || '注册失败', 'error');
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    currentToken = null;
    currentUser = null;
    showToast('已退出登录', 'success');
    checkLoginStatus();
    showPage('home');
    
    // 隐藏通知标记
    const badge = document.getElementById('notificationBadge');
    if (badge) badge.style.display = 'none';
}

function checkLoginStatus() {
    if (currentUser && currentToken) {
        document.getElementById('navLogin').classList.add('hidden');
        document.getElementById('navLogout').classList.remove('hidden');
        document.getElementById('navPublish').classList.remove('hidden');
        document.getElementById('navCart').classList.remove('hidden');
        document.getElementById('navUser').classList.remove('hidden');
        document.getElementById('navUsername').textContent = '👤 ' + currentUser.username;
        document.getElementById('navNotification').style.display = 'inline-block';
    } else {
        document.getElementById('navLogin').classList.remove('hidden');
        document.getElementById('navLogout').classList.add('hidden');
        document.getElementById('navPublish').classList.add('hidden');
        document.getElementById('navCart').classList.add('hidden');
        document.getElementById('navUser').classList.add('hidden');
        document.getElementById('navNotification').style.display = 'none';
    }
}

// ============ 分类管理 ============
async function loadCategories() {
    try {
        const res = await fetch(`${API_BASE}/category/list`);
        const data = await res.json();
        
        if (data.success === true) {
            const categories = data.data;
            
            categories.forEach(cat => {
                categoryMap[cat.id] = cat.icon + ' ' + cat.name;
            });
            
            const filterSelect = document.getElementById('filterCategory');
            if (filterSelect) {
                filterSelect.innerHTML = '<option value="">📂 全部分类</option>';
                categories.forEach(cat => {
                    filterSelect.innerHTML += `<option value="${cat.id}">${cat.icon} ${cat.name}</option>`;
                });
            }
            
            const publishSelect = document.getElementById('publishCategory');
            if (publishSelect) {
                publishSelect.innerHTML = '<option value="">请选择分类</option>';
                categories.forEach(cat => {
                    publishSelect.innerHTML += `<option value="${cat.id}">${cat.icon} ${cat.name}</option>`;
                });
            }
        }
    } catch (err) {
        console.error('加载分类失败', err);
    }
}

// ============ 商品管理 ============
async function loadProducts(pageNum) {
    if (!isPageReady) return;
    
    const categoryId = document.getElementById('filterCategory')?.value || '';
    const keyword = document.getElementById('searchKeyword')?.value || '';
    
    let url = `${API_BASE}/product/list?pageNum=${pageNum}&pageSize=12`;
    if (categoryId) url += `&categoryId=${categoryId}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
    
    try {
        const res = await fetch(url);
        if (!res.ok) return;
        
        const data = await res.json();
        
        if (data && typeof data === 'object' && data.success === true) {
            renderProducts(data.data);
            renderPagination(data.total, pageNum);
        }
    } catch (err) {
        console.error('加载商品失败:', err);
    }
}

function renderProducts(products) {
    const grid = document.getElementById('productGrid');
    
    if (!products || products.length === 0) {
        grid.innerHTML = '<p style="text-align:center;padding:40px;color:#999;">暂无商品，快来发布第一件吧！</p>';
        return;
    }
    
    grid.innerHTML = products.map(product => `
        <div class="product-card" onclick="showPage('detail', {productId: ${product.id}})">
            <div class="product-image-wrapper">
                ${product.image ? `<img src="${product.image}" alt="${product.title}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">` : ''}
                <div class="image-placeholder" style="${product.image ? 'display:none;' : ''}">📦</div>
            </div>
            <div class="product-info">
                <h3>${product.title}</h3>
                <p class="price">¥${product.price}</p>
                <p class="meta">
                    ${product.stock > 0 ? `<span style="color:#52c41a;">在售</span> · 剩余 ${product.stock} 件` : '<span style="color:#ff4d4f;">已售罄</span>'}
                    ${product.condition ? ` · ${product.condition}` : ''}
                </p>
            </div>
        </div>
    `).join('');
}

function renderPagination(total, currentPage) {
    const totalPages = Math.ceil(total / 12);
    const pagination = document.getElementById('pagination');
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            html += `<button class="active">${i}</button>`;
        } else if (i === 1 || i === totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
            html += `<button onclick="loadProducts(${i})">${i}</button>`;
        } else if (i === currentPage - 3 || i === currentPage + 3) {
            html += '<span>...</span>';
        }
    }
    
    pagination.innerHTML = html;
}

// ============ 商品详情 ============
async function loadProductDetail(productId) {
    try {
        const res = await fetch(`${API_BASE}/product/${productId}`);
        const data = await res.json();
        
        if (data.success === true) {
            renderProductDetail(data.data);
        } else {
            showToast('商品不存在', 'error');
            showPage('home');
        }
    } catch (err) {
        showToast('加载失败', 'error');
        showPage('home');
    }
}

function renderProductDetail(product) {
    document.getElementById('detailTitle').textContent = product.title;
    document.getElementById('detailPrice').textContent = '¥' + product.price;
    document.getElementById('detailSellerId').textContent = product.sellerId;
    document.getElementById('detailCategory').textContent = categoryMap[product.categoryId] || '-';
    document.getElementById('detailCondition').textContent = product.condition || '-';
    document.getElementById('detailStock').textContent = product.stock;
    document.getElementById('detailDesc').textContent = product.description || '暂无描述';
    
    const img = document.getElementById('detailImage');
    const placeholder = document.getElementById('detailPlaceholder');
    
    if (product.image) {
        img.src = product.image;
        img.style.display = 'block';
        placeholder.style.display = 'none';
    } else {
        img.style.display = 'none';
        placeholder.style.display = 'flex';
    }
    
    currentProductStock = product.stock || 0;
    selectedQuantity = 1;
    document.getElementById('selectedQuantity').textContent = '1';
    
    // 操作按钮
    const actions = document.getElementById('detailActions');
    const quantitySelector = document.getElementById('quantitySelector');
    
    if (!currentUser) {
        actions.innerHTML = '<button class="btn" onclick="showPage(\'login\')">登录后购买</button>';
        quantitySelector.style.display = 'none';
    } else if (product.sellerId === currentUser.id) {
        actions.innerHTML = '<p style="color:#999;">这是您发布的商品</p>';
        quantitySelector.style.display = 'none';
    } else if (product.stock > 0) {
        quantitySelector.style.display = 'flex';
        actions.innerHTML = `
            <button class="btn" onclick="buyNow(${product.id})">立即购买</button>
            <button class="btn btn-secondary" onclick="addToCart(${product.id})">加入购物车</button>
        `;
    } else {
        actions.innerHTML = '<p style="color:#ff4d4f;">商品已售罄</p>';
        quantitySelector.style.display = 'none';
    }
}

function changeQuantity(delta) {
    const newQty = selectedQuantity + delta;
    if (newQty >= 1 && newQty <= currentProductStock) {
        selectedQuantity = newQty;
        document.getElementById('selectedQuantity').textContent = newQty;
    }
}

async function buyNow(productId) {
    if (!currentUser) {
        showToast('请先登录', 'error');
        showPage('login');
        return;
    }
    
    pendingOrder = {
        buyerId: currentUser.id,
        productId: productId,
        quantity: selectedQuantity,
        type: 'direct'
    };
    
    showPage('payment');
}

function renderPaymentPage() {
    const summary = document.getElementById('orderSummary');
    
    if (pendingOrder.type === 'direct') {
        summary.innerHTML = `
            <p>商品ID: ${pendingOrder.productId}</p>
            <p>购买数量: ${pendingOrder.quantity}</p>
        `;
    } else {
        summary.innerHTML = `<p>共 ${pendingOrder.items.length} 件商品</p>`;
    }
}

async function confirmPayment() {
    if (!pendingOrder || !currentUser) return;
    
    try {
        let res, data;
        
        if (pendingOrder.type === 'direct') {
            // 立即购买创建订单
            res = await fetch(`${API_BASE}/order/create`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    productId: pendingOrder.productId,
                    buyerId: pendingOrder.buyerId,
                    quantity: pendingOrder.quantity
                })
            });
        } else {
            // 购物车结算
            const orderItems = [];
            pendingOrder.items.forEach(item => {
                const qty = pendingOrder.cartQuantity[item.product.id];
                orderItems.push({
                    productId: item.product.id,
                    quantity: qty
                });
            });
            
            res = await fetch(`${API_BASE}/order/create-batch`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    buyerId: pendingOrder.buyerId,
                    items: orderItems
                })
            });
        }
        
        data = await res.json();
        
        if (data.success === true) {
            document.getElementById('successMessage').textContent = data.message || '订单创建成功';
            showPage('success');
            pendingOrder = null;
        } else {
            showToast(data.message || '下单失败', 'error');
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

function cancelPayment() {
    pendingOrder = null;
    showPage('cart');
}

// ============ 购物车 ============
async function loadCart() {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/cart/list?userId=${currentUser.id}`);
        const data = await res.json();
        
        if (data.success === true) {
            renderCart(data.data);
        }
    } catch (err) {
        showToast('加载购物车失败', 'error');
    }
}

function renderCart(cartItems) {
    const cartList = document.getElementById('cartList');
    const cartEmpty = document.getElementById('cartEmpty');
    const cartTotal = document.getElementById('cartTotal');
    const totalAmount = document.getElementById('totalAmount');
    
    if (!cartItems || cartItems.length === 0) {
        cartList.innerHTML = '';
        cartEmpty.classList.remove('hidden');
        cartTotal.classList.add('hidden');
        return;
    }
    
    cartEmpty.classList.add('hidden');
    cartTotal.classList.remove('hidden');
    
    let total = 0;
    
    cartList.innerHTML = cartItems.map(item => {
        const product = item.product;
        if (!product) return '';
        
        const canBuy = product.status === 1 && product.stock > 0 && product.sellerId !== currentUser.id;
        const subtotal = product.price * item.quantity;
        if (canBuy) total += subtotal;
        
        const stockText = canBuy ? '有货' : (product.stock <= 0 ? '已售罄' : '已下架');
        
        return `
            <div class="cart-item" style="display:flex;gap:20px;padding:15px;border-bottom:1px solid #eee;align-items:center;">
                <div class="product-image-wrapper" style="width:80px;height:80px;flex-shrink:0;">
                    ${product.image ? `<img src="${product.image}" style="width:100%;height:100%;object-fit:cover;" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">` : ''}
                    <div class="image-placeholder" style="${product.image ? 'display:none;' : ''}">📦</div>
                </div>
                <div class="cart-product-info" style="flex:1;">
                    <h3>${product.title}</h3>
                    <p>${product.condition || ''}</p>
                    <p style="margin: 5px 0;">${stockText} · 剩余 ${product.stock || 0} 件</p>
                    <p class="price" style="margin: 5px 0;">单价: ¥${product.price}</p>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <button onclick="updateCartQuantity(${product.id}, ${item.quantity - 1})" 
                            style="width: 30px; height: 30px; border: 1px solid #ddd; background: white; cursor: pointer;" ${!canBuy ? 'disabled' : ''}>-</button>
                    <span style="min-width: 30px; text-align: center;">${item.quantity}</span>
                    <button onclick="updateCartQuantity(${product.id}, ${item.quantity + 1})" 
                            style="width: 30px; height: 30px; border: 1px solid #ddd; background: white; cursor: pointer;" ${!canBuy ? 'disabled' : ''}>+</button>
                </div>
                <div style="text-align: right; min-width: 100px;">
                    <p class="price" style="font-size: 18px;">¥${subtotal.toFixed(2)}</p>
                    <button onclick="removeFromCart(${product.id})" 
                            style="color: #ff4d4f; border: none; background: none; cursor: pointer; margin-top: 10px;">删除</button>
                </div>
            </div>
        `;
    }).join('');
    
    totalAmount.textContent = total.toFixed(2);
}

async function addToCart(productId) {
    if (!currentUser) {
        showToast('请先登录', 'error');
        showPage('login');
        return;
    }
    
    try {
        const res = await fetch(`${API_BASE}/cart/add`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                userId: currentUser.id,
                productId: productId
            })
        });
        const data = await res.json();
        
        if (data.success === true) {
            showToast('已加入购物车', 'success');
        } else {
            showToast(data.message || '添加失败', 'error');
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

async function removeFromCart(productId) {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/cart/remove?userId=${currentUser.id}&productId=${productId}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        
        if (data.success === true) {
            showToast('已从购物车移除', 'success');
            loadCart();
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

async function updateCartQuantity(productId, quantity) {
    if (!currentUser) return;
    
    if (quantity < 1) {
        showToast('至少购买1件商品', 'error');
        loadCart();
        return;
    }
    
    // 检查库存上限
    try {
        const stockRes = await fetch(`${API_BASE}/product/${productId}`);
        const stockData = await stockRes.json();
        if (stockData.success && stockData.data) {
            const maxStock = stockData.data.stock;
            if (quantity > maxStock) {
                showToast(`库存不足，最多只能购买 ${maxStock} 件`, 'error');
                return;
            }
        }
    } catch (err) {
        // 忽略库存检查错误
    }
    
    try {
        const res = await fetch(`${API_BASE}/cart/update`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                userId: currentUser.id,
                productId: productId,
                quantity: quantity
            })
        });
        const data = await res.json();
        
        if (data.success === true) {
            loadCart();
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

async function clearCart() {
    if (!currentUser) return;
    
    if (!confirm('确定要清空购物车吗？')) return;
    
    try {
        const res = await fetch(`${API_BASE}/cart/clear?userId=${currentUser.id}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        
        if (data.success === true) {
            showToast('购物车已清空', 'success');
            loadCart();
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

// ============ 购物车结算 ============
async function checkout() {
    if (!currentUser) {
        showToast('请先登录', 'error');
        showPage('login');
        return;
    }
    
    try {
        const res = await fetch(`${API_BASE}/cart/list?userId=${currentUser.id}`);
        const data = await res.json();
        
        if (!data.success === true || !data.data || data.data.length === 0) {
            showToast('购物车是空的', 'error');
            return;
        }
        
        // 过滤有库存且非自己发布的商品
        const availableItems = data.data.filter(item =>
            item.product &&
            item.product.status === 1 &&
            item.product.stock > 0 &&
            item.product.sellerId !== currentUser.id
        );
        
        if (availableItems.length === 0) {
            showToast('购物车中没有可购买的商品', 'error');
            return;
        }
        
        // 构建购物车数量映射
        const cartQuantity = {};
        availableItems.forEach(item => {
            cartQuantity[item.product.id] = Math.min(item.quantity, item.product.stock);
        });
        
        // 设置待支付订单信息
        pendingOrder = {
            buyerId: currentUser.id,
            items: availableItems,
            cartQuantity: cartQuantity,
            type: 'cart'
        };
        
        // 跳转到付款页面
        showPage('payment');
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

// ============ 用户中心 ============
async function loadUserInfo() {
    if (!currentUser) return;
    
    document.getElementById('userUsername').textContent = currentUser.username;
    document.getElementById('userEmail').textContent = currentUser.email || '未设置';
    document.getElementById('userCreateTime').textContent = formatDate(currentUser.createTime);
    
    // 加载我的商品
    loadMyProducts();
}

async function loadMyProducts() {
    if (!currentUser) return;
    
    try {
        const res = await fetch(`${API_BASE}/product/list?pageNum=1&pageSize=100`);
        const data = await res.json();
        
        if (data.success && data.data) {
            const myProducts = data.data.filter(p => p.sellerId === currentUser.id);
            renderMyProducts(myProducts);
        }
    } catch (err) {
        console.error('加载我的商品失败', err);
    }
}

function renderMyProducts(products) {
    const container = document.getElementById('myProducts');
    if (!container) return;
    
    if (!products || products.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">您还没有发布商品</p>';
        return;
    }
    
    container.innerHTML = products.map(product => `
        <div class="product-card" onclick="showPage('detail', {productId: ${product.id}})">
            <div class="product-image-wrapper">
                <img src="${product.image || ''}" 
                     loading="lazy"
                     onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                <div class="image-placeholder" style="display:none;">📦</div>
            </div>
            <div class="product-info">
                <h3>${product.title}</h3>
                <p class="price">¥${product.price}</p>
                <p class="meta">
                    ${product.stock > 0 ? `<span style="color:#52c41a;">在售（剩${product.stock}件）</span>` : '<span style="color:#ff4d4f;">已售罄</span>'}
                </p>
            </div>
        </div>
    `).join('');
}

// ============ 发布商品 ============
async function handlePublish(e) {
    e.preventDefault();
    
    if (!currentUser) {
        showToast('请先登录', 'error');
        showPage('login');
        return;
    }
    
    const form = e.target;
    
    // 验证库存必须大于0
    const stock = parseInt(form.stock.value) || 0;
    if (stock < 1) {
        showToast('商品余量必须大于0', 'error');
        return;
    }
    
    const product = {
        title: form.title.value,
        description: form.description.value,
        price: parseFloat(form.price.value),
        categoryId: parseInt(form.categoryId.value),
        sellerId: currentUser.id,
        condition: form.condition.value,
        stock: stock,
        image: form.image.value || ''
    };
    
    try {
        const res = await fetch(`${API_BASE}/product`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(product)
        });
        const data = await res.json();
        
        if (data.success === true) {
            showToast('发布成功，待管理员审核', 'success');
            form.reset();
            // 重置图片预览
            document.getElementById('previewImg').style.display = 'none';
            document.getElementById('previewPlaceholder').style.display = 'flex';
            showPage('home');
        } else {
            showToast(data.message || '发布失败', 'error');
        }
    } catch (err) {
        showToast('网络错误', 'error');
    }
}

// ============ 图片上传功能 ============

/**
 * 图片预览
 */
function previewImage(input) {
    if (input.files && input.files[0]) {
        const file = input.files[0];
        
        // 验证文件大小
        if (file.size > 5 * 1024 * 1024) {
            showToast('图片大小不能超过5MB', 'error');
            input.value = '';
            return;
        }
        
        // 验证文件类型
        if (!file.type.startsWith('image/')) {
            showToast('请选择图片文件', 'error');
            input.value = '';
            return;
        }
        
        // 预览图片
        const reader = new FileReader();
        reader.onload = function(e) {
            const previewImg = document.getElementById('previewImg');
            const previewPlaceholder = document.getElementById('previewPlaceholder');
            
            if (previewImg && previewPlaceholder) {
                previewImg.src = e.target.result;
                previewImg.style.display = 'block';
                previewPlaceholder.style.display = 'none';
            }
        }
        reader.readAsDataURL(file);
        
        // 上传到服务器
        uploadImage(file);
    }
}

/**
 * 上传图片到服务器
 */
async function uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        const res = await fetch(`${API_BASE}/product/upload`, {
            method: 'POST',
            body: formData
        });
        const data = await res.json();
        
        if (data.success === true) {
            document.getElementById('productImageUrl').value = data.data;
            showToast('图片上传成功', 'success');
        } else {
            showToast(data.message || '上传失败', 'error');
            // 上传失败时重置预览
            document.getElementById('previewImg').style.display = 'none';
            document.getElementById('previewPlaceholder').style.display = 'flex';
        }
    } catch (err) {
        showToast('上传失败，请检查网络连接', 'error');
        // 上传失败时重置预览
        document.getElementById('previewImg').style.display = 'none';
        document.getElementById('previewPlaceholder').style.display = 'flex';
    }
}

// ============ 工具函数 ============
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
