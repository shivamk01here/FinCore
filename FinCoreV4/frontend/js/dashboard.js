const API_BASE = 'http://localhost:8080/api';
const user = JSON.parse(localStorage.getItem('user'));

if (!user) window.location.href = 'login.html';

async function fetchDashboard() {
    try {
        const res = await fetch(`${API_BASE}/dashboard?userId=${user.userId}`);
        const data = await res.json();

        document.getElementById('userFullName').innerText = data.user.username; // Or full name if available
        document.getElementById('balance').innerText = '₹ ' + data.user.balance;
        document.getElementById('userVpa').innerText = data.user.vpa;

        // Transactions
        const txTable = document.querySelector('#txTable tbody');
        txTable.innerHTML = '';
        data.transactions.forEach(tx => {
            const row = `<tr>
                <td>${tx.id}</td>
                <td>₹ ${tx.amount}</td>
                <td>${tx.senderId === user.userId ? 'Sent to ' + tx.receiverId : 'Received from ' + tx.senderId}</td>
            </tr>`;
            txTable.innerHTML += row;
        });

        // Notifications
        const notifList = document.getElementById('notifList');
        notifList.innerHTML = '';
        if (data.notifications.length === 0) {
            notifList.innerHTML = '<li>No new notifications</li>';
        } else {
            data.notifications.forEach(n => {
                notifList.innerHTML += `<li>${n.message}</li>`;
            });
        }

    } catch (err) {
        console.error(err);
    }
}

// Send Money
const sendMoneyForm = document.getElementById('sendMoneyForm');
if (sendMoneyForm) {
    sendMoneyForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const receiverVpa = document.getElementById('receiverVpa').value;
        const amount = document.getElementById('amount').value;

        try {
            const res = await fetch(`${API_BASE}/transaction/send`, {
                method: 'POST',
                body: JSON.stringify({
                    senderId: user.userId,
                    receiverVpa,
                    amount
                })
            });
            const data = await res.json();

            if (data.status === 'success') {
                document.getElementById('txMsg').className = 'success';
                document.getElementById('txMsg').innerText = 'Money Sent Successfully!';
                fetchDashboard(); // Refresh
                sendMoneyForm.reset();
            } else {
                document.getElementById('txMsg').className = 'error';
                document.getElementById('txMsg').innerText = data.error;
            }
        } catch (err) {
            document.getElementById('txMsg').innerText = 'Error processing transaction';
        }
    });
}

function logout() {
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}

// Initial Load
fetchDashboard();
