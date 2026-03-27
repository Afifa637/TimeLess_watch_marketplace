/* ── Timeless Marketplace — Main JS ──────────────────────────────────── */

// ── Auth token helpers ──────────────────────────────────────────────────
function getAuthHeader() {
    const token = localStorage.getItem('jwt_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
}
function setAuthToken(token) {
    localStorage.setItem('jwt_token', token);
    document.cookie = `jwt_token=${token}; path=/; max-age=86400; SameSite=Lax`;
}
function clearAuthToken() {
    localStorage.removeItem('jwt_token');
    document.cookie = 'jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax';
}

// ── Central fetch wrapper ────────────────────────────────────────────────
async function apiFetch(url, method = 'GET', body = null) {
    const headers = { ...getAuthHeader() };
    const options = { method, headers, credentials: 'include' };

    if (body !== null) {
        if (body instanceof FormData) {
            options.body = body;
        } else {
            headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
    }

    const response = await fetch(url, options);
    const text = await response.text();
    let data = null;

    if (text) {
        try { data = JSON.parse(text); } catch { data = text; }
    }

    if (!response.ok) {
        const message = data?.message || data?.error || data || 'Request failed';
        throw new Error(message);
    }

    return data;
}

// ── Toast notifications ──────────────────────────────────────────────────
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container toast-container-custom';
        document.body.appendChild(container);
    }
    const el = document.createElement('div');
    el.className = `toast toast-${type} align-items-center border-0 mb-2`;
    el.setAttribute('role', 'alert');
    el.setAttribute('aria-live', 'assertive');
    el.setAttribute('aria-atomic', 'true');
    el.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>`;
    container.appendChild(el);
    const toast = new bootstrap.Toast(el, { delay: 3200 });
    toast.show();
    el.addEventListener('hidden.bs.toast', () => el.remove());
}

// ── JWT helpers ──────────────────────────────────────────────────────────
function parseJwt(token) {
    const payload = token.split('.')[1];
    const norm = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(norm.padEnd(norm.length + (4 - norm.length % 4) % 4, '=')));
}
function redirectByRole(token) {
    const p = parseJwt(token);
    window.location.href = p.role === 'ADMIN' ? '/admin/dashboard'
                         : p.role === 'SELLER' ? '/seller/dashboard'
                         : '/watches';
}

// ── Auth actions ─────────────────────────────────────────────────────────
function logoutUser() {
    clearAuthToken();
    showToast('You have been logged out.', 'info');
    setTimeout(() => { window.location.href = '/'; }, 500);
}
async function loginUser(email, password) {
    const response = await apiFetch('/api/auth/login', 'POST', { email, password });
    setAuthToken(response.token);
    showToast('Login successful.', 'success');
    setTimeout(() => redirectByRole(response.token), 300);
}
async function registerUser(payload) {
    const response = await apiFetch('/api/auth/register', 'POST', payload);
    setAuthToken(response.token);
    showToast('Registration successful.', 'success');
    setTimeout(() => redirectByRole(response.token), 300);
}

// ── Cart ─────────────────────────────────────────────────────────────────
async function addToCart(watchId) {
    try {
        await apiFetch(`/api/cart/${watchId}`, 'POST');
        showToast('Watch added to cart.', 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}

let _detailQty = 1;
function detailQty(delta) {
    const maxEl = document.getElementById('detailMaxStock');
    const max = maxEl ? parseInt(maxEl.value) : 999;
    _detailQty = Math.max(1, Math.min(_detailQty + delta, max));
    document.getElementById('detailQtyVal').textContent = _detailQty;
}
async function addToCartQty(watchId, maxStock) {
    try {
        await apiFetch(`/api/cart/${watchId}`, 'POST');
        for (let i = 1; i < _detailQty; i++) {
            await apiFetch(`/api/cart/${watchId}/quantity`, 'PATCH', { delta: 1 });
        }
        showToast(`${_detailQty} item(s) added to cart.`, 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}

async function removeFromCart(watchId) {
    try {
        await apiFetch(`/api/cart/${watchId}`, 'DELETE');
        showToast('Item removed from cart.', 'success');
        setTimeout(() => window.location.reload(), 350);
    } catch (err) { showToast(err.message, 'error'); }
}

async function cartQty(watchId, delta) {
    try {
        const res = await apiFetch(`/api/cart/${watchId}/quantity`, 'PATCH', { delta });
        if (res && res.quantity !== undefined) {
            const qtyEl = document.getElementById('qty-' + watchId);
            if (qtyEl) qtyEl.textContent = res.quantity;
        }
        setTimeout(() => window.location.reload(), 200);
    } catch (err) { showToast(err.message, 'error'); }
}

async function clearCartFull() {
    if (!confirm('Clear your entire cart?')) return;
    try {
        await apiFetch('/api/cart', 'DELETE');
        window.location.reload();
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Wishlist ──────────────────────────────────────────────────────────────
async function addToWishlist(watchId) {
    try {
        await apiFetch(`/api/wishlist/${watchId}`, 'POST');
        showToast('Watch added to wishlist.', 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}
async function removeFromWishlist(watchId) {
    try {
        await apiFetch(`/api/wishlist/${watchId}`, 'DELETE');
        showToast('Watch removed from wishlist.', 'success');
        setTimeout(() => window.location.reload(), 350);
    } catch (err) { showToast(err.message, 'error'); }
}
async function moveToCart(watchId) {
    try {
        await apiFetch(`/api/wishlist/${watchId}/move-to-cart`, 'POST');
        showToast('Moved to cart.', 'success');
        setTimeout(() => window.location.reload(), 350);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Orders ────────────────────────────────────────────────────────────────
async function cancelOrder(orderId) {
    if (!confirm('Cancel this order?')) return;
    await updateOrderStatus(orderId, 'CANCELLED');
}
async function confirmReceipt(orderId) {
    if (!confirm('Confirm you have received this order?')) return;
    try {
        await apiFetch(`/api/orders/${orderId}/status`, 'PATCH', { status: 'COMPLETED' });
        showToast('Order marked as completed. Thank you!', 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}
async function updateOrderStatus(orderId, status, trackingNumber = null) {
    try {
        await apiFetch(`/api/orders/${orderId}/status`, 'PATCH', { status, trackingNumber });
        showToast(`Order status updated to ${status}.`, 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}

async function markShipped(orderId) {
    const tracking = prompt('Enter tracking number:');
    if (!tracking || !tracking.trim()) {
        showToast('Tracking number is required.', 'error');
        return;
    }
    await updateOrderStatus(orderId, 'SHIPPED', tracking.trim());
}

// ── Watch management ──────────────────────────────────────────────────────
async function approveWatch(watchId) {
    try {
        await apiFetch(`/api/admin/watches/${watchId}/approve`, 'PATCH');
        showToast('Watch approved and now live.', 'success');
        setTimeout(() => window.location.reload(), 400);
    } catch (err) { showToast(err.message, 'error'); }
}
async function rejectWatch(watchId) {
    try {
        await apiFetch(`/api/admin/watches/${watchId}/reject`, 'PATCH');
        showToast('Watch rejected.', 'success');
        setTimeout(() => window.location.reload(), 400);
    } catch (err) { showToast(err.message, 'error'); }
}
async function deactivateWatch(watchId) {
    try {
        await apiFetch(`/api/admin/watches/${watchId}/deactivate`, 'PATCH');
        showToast('Watch deactivated.', 'success');
        setTimeout(() => window.location.reload(), 400);
    } catch (err) { showToast(err.message, 'error'); }
}
async function deleteWatch(watchId) {
    if (!confirm('Permanently delete this listing?')) return;
    try {
        await apiFetch(`/api/watches/${watchId}`, 'DELETE');
        showToast('Watch deleted.', 'success');
        setTimeout(() => {
            window.location.href = window.location.pathname.startsWith('/admin')
                ? '/admin/listings' : '/seller/dashboard';
        }, 600);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Stock adjustment (seller) ─────────────────────────────────────────────
async function adjustStock(watchId, delta) {
    try {
        await apiFetch(`/api/watches/${watchId}/stock`, 'PATCH', { delta });
        showToast('Stock updated.', 'success');
        setTimeout(() => window.location.reload(), 400);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Admin ─────────────────────────────────────────────────────────────────
async function toggleUser(userId) {
    try {
        await apiFetch(`/api/admin/users/${userId}/toggle`, 'PATCH');
        showToast('User status updated.', 'success');
        setTimeout(() => window.location.reload(), 400);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Reviews ───────────────────────────────────────────────────────────────
async function submitReview(watchId, orderId, rating, comment) {
    try {
        await apiFetch('/api/reviews', 'POST', { orderId, rating, comment });
        showToast('Thanks for your review!', 'success');
        setTimeout(() => window.location.reload(), 500);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── Watch form helpers ────────────────────────────────────────────────────
async function createWatchFromForm(form) {
    const formData = new FormData(form);

    const price = formData.get('price');
    const stockQuantity = formData.get('stockQuantity');
    const year = formData.get('year');

    if (price !== null && price !== '') {
        formData.set('price', Number(price));
    }
    if (stockQuantity !== null && stockQuantity !== '') {
        formData.set('stockQuantity', Number(stockQuantity));
    }
    if (year !== null && year !== '') {
        formData.set('year', Number(year));
    }

    try {
        await apiFetch('/api/watches', 'POST', formData);
        showToast('Listing submitted for review.', 'success');
        setTimeout(() => { window.location.href = '/seller/dashboard'; }, 500);
    } catch (err) { showToast(err.message, 'error'); }
}

async function updateWatchFromForm(watchId, form) {
    const formData = new FormData(form);

    const price = formData.get('price');
    const stockQuantity = formData.get('stockQuantity');
    const year = formData.get('year');

    if (price !== null && price !== '') {
        formData.set('price', Number(price));
    }
    if (stockQuantity !== null && stockQuantity !== '') {
        formData.set('stockQuantity', Number(stockQuantity));
    }
    if (year !== null && year !== '') {
        formData.set('year', Number(year));
    }

    try {
        await apiFetch(`/api/watches/${watchId}`, 'PUT', formData);
        showToast('Listing updated and resubmitted for review.', 'success');
        setTimeout(() => { window.location.href = '/seller/dashboard'; }, 500);
    } catch (err) { showToast(err.message, 'error'); }
}

// ── DOMContentLoaded ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-logout]').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            logoutUser();
        });
    });
});