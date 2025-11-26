const API_BASE = 'http://localhost:8080/api';
const user = JSON.parse(localStorage.getItem('user'));

if (!user || user.role !== 'ADMIN') window.location.href = 'login.html';

async function fetchPendingUsers() {
    try {
        const res = await fetch(`${API_BASE}/admin/pending`);
        const users = await res.json();

        const list = document.getElementById('pendingList');
        list.innerHTML = '';

        if (users.length === 0) {
            list.innerHTML = '<p>No pending approvals</p>';
            return;
        }

        users.forEach(u => {
            const item = document.createElement('div');
            item.className = 'card';
            item.style.marginBottom = '1rem';
            item.innerHTML = `
                <div style="display:flex; justify-content:space-between; align-items:center">
                    <div>
                        <h3>${u.fullName} (${u.username})</h3>
                        <p>Type: ${u.accountType}</p>
                    </div>
                    <button onclick="approveUser(${u.id})" class="btn-sm" style="background:var(--success)">Approve</button>
                </div>
            `;
            list.appendChild(item);
        });

    } catch (err) {
        console.error(err);
    }
}

async function approveUser(userId) {
    if (!confirm('Approve this user?')) return;

    try {
        const res = await fetch(`${API_BASE}/admin/approve`, {
            method: 'POST',
            body: JSON.stringify({ userId })
        });
        const data = await res.json();

        if (data.status === 'success') {
            fetchPendingUsers();
        } else {
            alert('Failed to approve');
        }
    } catch (err) {
        alert('Error');
    }
}

function logout() {
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}

fetchPendingUsers();
