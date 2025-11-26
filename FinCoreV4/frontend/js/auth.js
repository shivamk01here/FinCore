const API_BASE = 'http://localhost:8080/api';

// Login
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                body: JSON.stringify({ username, password })
            });
            const data = await res.json();

            if (data.status === 'success') {
                localStorage.setItem('user', JSON.stringify(data));
                if (data.role === 'ADMIN') {
                    window.location.href = 'admin_dashboard.html';
                } else {
                    window.location.href = 'dashboard.html';
                }
            } else {
                document.getElementById('errorMsg').innerText = data.error;
            }
        } catch (err) {
            console.error(err);
            document.getElementById('errorMsg').innerText = 'Connection Error';
        }
    });
}

// Register
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = {
            fullName: document.getElementById('fullName').value,
            email: document.getElementById('email').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value,
            accountType: document.getElementById('accountType').value
        };

        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                body: JSON.stringify(formData)
            });
            const data = await res.json();

            if (data.status === 'success') {
                document.getElementById('msg').innerText = data.message;
                registerForm.reset();
            } else {
                document.getElementById('errorMsg').innerText = data.error;
            }
        } catch (err) {
            document.getElementById('errorMsg').innerText = 'Connection Error';
        }
    });
}

function logout() {
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}
