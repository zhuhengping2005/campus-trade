function esc(t) { if(!t)return''; var d=document.createElement('div'); d.textContent=t; return d.innerHTML; }
const API_BASE = 'http://localhost:8080/api';
let adminToken = localStorage.getItem('adminToken');
let adminInfo = JSON.parse(localStorage.getItem('adminInfo')|| 'null');
let currentPendingPage = 1;
let currentProductsPage = 1;
let currentUsersPage = 1;
let currentNotificationPage = 1;

document.addEventListener('DOMContentLoaded', function() {
    if (adminToken) { showAdminPage(); loadDashboard(); }
    else { showLoginPage(); }
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('notificationForm').addEventListener('submit', handleSendNotification);
});

function showLoginPage() {
    document.getElementById('loginPage').classList.remove('hidden');
    document.getElementById('adminPage').classList.add('hidden');
}

function showAdminPage() {
    document.getElementById('loginPage').classList.add('hidden');
    document.getElementById('adminPage').classList.remove('hidden');
}

async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    try {
        const res = await fetch(API_BASE + '/admin/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username: form.username.value, password: form.password.value})
        });
        const data = await res.json();
        if (data.success) {
            adminToken = data.data.token;
            adminInfo = data.data;
            localStorage.setItem('adminToken', adminToken);
            localStorage.setItem('adminInfo', JSON.stringify(adminInfo));
            showAdminPage(); loadDashboard();
        } else { alert(data.message|| '登录失败'); }
    } catch (err) { alert('网络错误'); }
}

function logout() {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminInfo');
    adminToken = null; adminInfo = null; showLoginPage();
}

function showModule(module) {
    document.querySelectorAll('.module').forEach(function(m){m.classList.add('hidden');});
    document.querySelectorAll('.sidebar nav a').forEach(function(a){a.classList.remove('active');});
    event.target.classList.add('active');
    if (module === 'dashboard') { document.getElementById('moduleDashboard').classList.remove('hidden'); loadDashboard(); }
    else if (module === 'pending') { document.getElementById('modulePending').classList.remove('hidden'); loadPendingProducts(1); }
    else if (module === 'products') { document.getElementById('moduleProducts').classList.remove('hidden'); loadAdminProducts(1); }
    else if (module === 'users') { document.getElementById('moduleUsers').classList.remove('hidden'); loadUsers(1); }
    else if (module === 'notifications') { document.getElementById('moduleNotifications').classList.remove('hidden'); }
    else if (module === 'notification-list') { document.getElementById('moduleNotificationList').classList.remove('hidden'); loadNotificationList(1); }
}

async function handleSendNotification(e) {
    e.preventDefault();
    const form = e.target;
    const formData = {
        userId: parseInt(form.userId.value),
        type: form.type.value,
        title: form.title.value,
        content: form.content.value
    };
    
    try {
        const res = await fetch(API_BASE + '/admin/notification/send', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + adminToken},
            body: JSON.stringify(formData)
        });
        const data = await res.json();
        if (data.success) {
            alert('通知发送成功');
            form.reset();
        } else {
            alert(data.message || '发送失败');
        }
    } catch (err) {
        alert('网络错误');
    }
}

async function loadNotificationList(page) {
    currentNotificationPage = page;
    const sel = document.getElementById('filterNotifRead');
    const isRead = sel ? sel.value : '';
    
    var url = API_BASE + '/admin/notifications?pageNum=' + page + '&pageSize=10';
    if (isRead !== '') url += '&isRead=' + isRead;
    
    try {
        const res = await fetch(url, {headers: {'Authorization': 'Bearer ' + adminToken}});
        const data = await res.json();
        if (data.success) {
            renderNotificationList(data.data.records);
            renderPagination('notificationPagination', data.data.pages || 1, page, function(p) { loadNotificationList(p); });
        }
    } catch(err) { console.error(err); }
}

function renderNotificationList(notifications) {
    var c = document.getElementById('notificationList');
    if (!notifications || !notifications.length) {
        c.innerHTML = '<p style="padding:40px;text-align:center;color:#999;">暂无数据</p>';
        return;
    }
    var typeLabels = { system: '系统通知', order: '订单通知', audit: '审核通知' };
    var h = '';
    for (var i = 0; i < notifications.length; i++) {
        var n = notifications[i];
        var unreadClass = n.isRead === 0 ? 'unread' : '';
        var typeClass = 'type-' + (n.type || 'system');
        var typeLabel = typeLabels[n.type] || '系统通知';
        h += '<div class="notification-item ' + unreadClass + '">';
        h += '<div class="notification-content">';
        h += '<h4>' + esc(n.title || '') + '</h4>';
        h += '<p>' + esc(n.content || '') + '</p>';
        h += '<div class="notification-meta">';
        h += '<span class="notification-type ' + typeClass + '">' + typeLabel + '</span>';
        h += '<span>用户ID: ' + n.userId + '</span>';
        h += '<span>' + (n.createTime || '') + '</span>';
        h += '<span>' + (n.isRead === 0 ? '未读' : '已读') + '</span>';
        h += '</div></div></div>';
    }
    c.innerHTML = h;
}

async function loadDashboard() {
    try {
        const res = await fetch(API_BASE + '/admin/statistics');
        const data = await res.json();
        if (data.success) {
            document.getElementById('statToday').textContent = '¥' + (data.data.todayAmount || 0).toFixed(2);
            document.getElementById('statWeek').textContent = '¥' + (data.data.weekAmount || 0).toFixed(2);
            document.getElementById('statMonth').textContent = '¥' + (data.data.monthAmount || 0).toFixed(2);
            document.getElementById('statTotal').textContent = '¥' + (data.data.totalAmount || 0).toFixed(2);
            document.getElementById('statPending').textContent = data.data.pendingCount || 0;
            document.getElementById('statUsers').textContent = data.data.totalUsers || 0;
            document.getElementById('statProducts').textContent = data.data.totalProducts || 0;
            document.getElementById('statOrders').textContent = data.data.totalOrders || 0;
            
            // 新增统计
            document.getElementById('statTodayOrders').textContent = data.data.todayOrders || 0;
            document.getElementById('statTodayAmount').textContent = '¥' + (data.data.todayAmount || 0).toFixed(2);
            document.getElementById('statMonthOrders').textContent = data.data.monthOrders || 0;
            document.getElementById('statMonthAmount').textContent = '¥' + (data.data.monthAmount || 0).toFixed(2);
            document.getElementById('statPendingOrders').textContent = data.data.pendingOrders || 0;
            
            // 通知统计
            document.getElementById('statTotalNotifications').textContent = data.data.totalNotifications || 0;
            document.getElementById('statUnreadNotifications').textContent = data.data.unreadNotifications || 0;
        }
    } catch(err) { console.error(err); }
}

async function loadPendingProducts(page) {
    currentPendingPage = page;
    try {
        const res = await fetch(API_BASE + '/admin/products/pending?pageNum=' + page + '&pageSize=10');
        const data = await res.json();
        if (data.success) { renderPendingList(data.data.records); renderPagination('pendingPagination', data.data.pages||1, page, function(p){loadPendingProducts(p);}); }
    } catch(err) {console.error(err);}
}

function renderPendingList(products) {
    var c = document.getElementById('pendingList');
    if (!products||!products.length) { c.innerHTML='<p style=padding:40px;text-align:center;color:#999;>暂无数据</p>'; return; }
    var h='';
    for (var i=0;i<products.length;i++) { var p=products[i];
        h+='<div class=product-card><h4>'+esc(p.title||'')+' <span class=status-badge status-pending>待审核</span></h4>';
        h+='<p>用户ID:'+p.sellerId+'   | 余量:'+p.stock+'   | ¥'+p.price+'</p>';
        h+='<p>'+esc(p.description||'')+'</p>';
        h+='<div class=action-buttons><button class=btn-approve onclick=doAudit('+p.id+',1)>通过</button>';
        h+='<button class=btn-reject onclick=doAudit('+p.id+',-1)>拒绝</button></div></div>';
    }
    c.innerHTML=h;
}

async function doAudit(id, status) {
    var remark = document.getElementById('auditRemark').value;
    try {
        var res = await fetch(API_BASE+'/admin/product/audit',{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+adminToken},body:JSON.stringify({productId:id,status:status,remark:remark})});
        var data = await res.json();
        if(data.success){ alert(status===1?'审核通过':'已拒绝'); closeAuditModal(); loadPendingProducts(currentPendingPage); loadDashboard(); }
        else { alert(data.message||'操作失败'); }
    } catch(err){console.error(err);}
}

async function loadAdminProducts(page) {
    currentProductsPage = page;
    var sel = document.getElementById('filterStatus');
    var st = sel ? sel.value : '';
    
    if (st === 'pending') {
        try {
            var res = await fetch(API_BASE + '/admin/products/pending?pageNum=' + page + '&pageSize=10');
            var data = await res.json();
            if (data.success) { renderProductsList(data.data.records); renderPagination('productsPagination', data.data.pages||1, page, function(p){loadAdminProducts(p);}); }
        } catch(err){console.error(err);}
        return;
    }
    
    var url = API_BASE + '/admin/products?pageNum=' + page + '&pageSize=10';
    if (st) url += '&status=' + st;
    try {
        var res = await fetch(url);
        var data = await res.json();
        if (data.success) { renderProductsList(data.data.records); renderPagination('productsPagination', data.data.pages||1, page, function(p){loadAdminProducts(p);}); }
    } catch(err){console.error(err);}
}

function renderProductsList(products) {
    var c = document.getElementById('productsList');
    if (!products||!products.length) { c.innerHTML='<p style=padding:40px;text-align:center;color:#999;>暂无数据</p>'; return; }
    var m = {1:{t:'已上架',c:'status-approved'},0:{t:'已下架',c:'status-rejected'},'-1':{t:'已下架',c:'status-rejected'}};
    var h='';
    for (var i=0;i<products.length;i++) { var p=products[i]; var s=m[p.status]||m[0];
        var auditLabel = p.auditStatus === 0 ? '<span class=status-badge status-pending>待审核</span>' : '<span class=status-badge '+s.c+'>'+s.t+'</span>'; h+='<div class=product-card><h4>'+esc(p.title||'')+' '+auditLabel+'</h4>';
        h+='<p>用户ID:'+p.sellerId+'   | 余量:'+p.stock+'   | ¥'+p.price+'</p>';
        h+='<p>'+esc(p.description||'')+'</p>';
        h+='<div class=action-buttons><button class=btn-top onclick=topProduct('+p.id+')>置顶</button>';
        h+='<button class=btn-delete onclick=deleteProduct('+p.id+')>删除</button></div></div>';
    }
    c.innerHTML=h;
}

async function topProduct(id) {
    try {
        var res = await fetch(API_BASE+'/admin/product/top',{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+adminToken},body:JSON.stringify({productId:id,priority:1})});
        var data = await res.json();
        if (data.success) { alert('置顶成功'); loadAdminProducts(currentProductsPage); }
    } catch(err){console.error(err);}
}

async function deleteProduct(id) {
    if (!confirm('确定要删除商品吗?')) return;
    try {
        var res = await fetch(API_BASE+'/admin/product/'+id,{method:'DELETE',headers:{'Authorization':'Bearer '+adminToken}});
        var data = await res.json();
        if (data.success) { alert('删除成功'); loadAdminProducts(currentProductsPage); }
    } catch(err){console.error(err);}
}

async function loadUsers(page) {
    currentUsersPage = page;
    try {
        var res = await fetch(API_BASE+'/admin/users?pageNum='+page+'&pageSize=10');
        var data = await res.json();
        if (data.success) { renderUsersList(data.data.records); renderPagination('usersPagination', data.data.pages||1, page, function(p){loadUsers(p);}); }
    } catch(err){console.error(err);}
}

function renderUsersList(users) {
    var c = document.getElementById('usersList');
    if (!users||!users.length) { c.innerHTML='<p style=padding:40px;text-align:center;color:#999;>暂无数据</p>'; return; }
    var h='';
    for (var i=0;i<users.length;i++) { var u=users[i];
        var sc=u.status===1?'status-enabled':'status-disabled'; var st=u.status===1?'正常':'禁用';
        var bc=u.status===1?'btn-reject':'btn-approve'; var bt=u.status===1?'禁用':'启用';
        h+='<div class=product-card><h4>'+esc(u.username||'')+' <span class=status-tag '+sc+'>'+st+'</span></h4>';
        h+='<p>ID:'+u.id+'  | '+(u.createTime||'')+'</p>';
        h+='<div class=action-buttons><button class=btn-approve onclick="showUserProducts('+u.id+',\' '+esc(u.username||'')+'\')">查看商品</button>';
        h+='<button class=btn-approve onclick="sendNotificationToUser('+u.id+', \''+esc(u.username||'')+'\')">发通知</button>';
        h+='<button class='+bc+' onclick="toggleUserStatus('+u.id+','+(u.status===1?0:1)+')">'+bt+'</button></div></div>';
    }
    c.innerHTML=h;
}

function sendNotificationToUser(userId, username) {
    if (!confirm('给用户 ' + username + ' (ID:' + userId + ') 发送通知？')) return;
    document.querySelector('[onclick*="showModule(\'notifications\')"]').click();
    document.getElementById('notificationForm').userId.value = userId;
    document.getElementById('notificationForm').scrollIntoView();
}

async function showUserProducts(uid, uname) {
    document.getElementById('userProductsTitle').textContent=' - '+uname;
    document.getElementById('userProductsModal').classList.remove('hidden');
    try {
        var res = await fetch(API_BASE+'/admin/user/'+uid+'/products');
        var data = await res.json();
        var c = document.getElementById('userProductsList');
        if (!data.success||!data.data.length) { c.innerHTML='<p style=padding:20px;text-align:center;color:#999;>暂无数据</p>'; }
        else { var h=''; for(var i=0;i<data.data.length;i++){var p=data.data[i];h+='<div>商品ID:'+p.id+' |'+esc(p.title||'')+'  | 余量:'+p.stock+' |'+(p.auditStatus===0?'待审核':(p.status===1?'已上架':'已下架'))+'</div>';} c.innerHTML=h; }
    } catch(err){console.error(err);}
}

function closeUserProductsModal() { document.getElementById('userProductsModal').classList.add('hidden'); }

async function toggleUserStatus(uid, st) {
    if (!confirm((st===1?'启用':'禁用')+'?')) return;
    try {
        var res = await fetch(API_BASE+'/admin/user/status',{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+adminToken},body:JSON.stringify({userId:uid,status:st})});
        var data = await res.json();
        if (data.success) { alert('操作成功'); loadUsers(currentUsersPage); }
    } catch(err){console.error(err);}
}

function renderPagination(cid, total, cur, cb) {
    var c = document.getElementById(cid);
    if (total <= 1) { c.innerHTML=''; return; }
    var h='';
    if (cur > 1) h += '<button data-page="'+(cur-1)+'">上一页</button>';
    for (var i=1; i<=total; i++) {
        if (i===1||i===total||(i>=cur-2&&i<=cur+2)) h += '<button data-page="'+i+'"'+(i===cur?' class="active"':'')+'>'+i+'</button>';
        else if (i===cur-3||i===cur+3) h += '<span>...</span>';
    }
    if (cur < total) h += '<button data-page="'+(cur+1)+'">下一页</button>';
    c.innerHTML = h;
    c.onclick = function(e) {
        var btn = e.target.closest('button[data-page]');
        if (btn) {
            var page = parseInt(btn.getAttribute('data-page'), 10);
            if (!isNaN(page)) cb(page);
        }
    };
}

function closeAuditModal() {
    document.getElementById('auditModal').classList.add('hidden');
}
