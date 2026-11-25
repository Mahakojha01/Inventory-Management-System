async function loadDashboard() {
  const sortBy = document.getElementById('sortSelect').value;
  const res = await fetch(`/api/dashboard?sortBy=${encodeURIComponent(sortBy)}`);
  const data = await res.json();

  document.getElementById('dbStatus').textContent = data.dbMode;
  document.getElementById('searchBadge').textContent = `Showing ${data.products.length} products`;

  renderProducts(data.products);
  renderKpis(data);
  renderAlerts(data.lowStockAlerts);
  renderBestSellers(data.bestSellers);
  renderHistory(data.history);
}

function renderKpis(data) {
  document.getElementById('kpiTotalProducts').textContent = data.products.length;
  const totalUnits = data.products.reduce((acc, p) => acc + p.quantity, 0);
  document.getElementById('kpiTotalUnits').textContent = totalUnits;
  document.getElementById('kpiLowStock').textContent = data.lowStockAlerts.length;
  document.getElementById('kpiTopSeller').textContent =
    data.bestSellers.length > 0 ? `${data.bestSellers[0].name} (${data.bestSellers[0].totalSold})` : '-';
}

function renderProducts(products) {
  const tbody = document.getElementById('inventoryBody');
  tbody.innerHTML = '';
  if (products.length === 0) {
    tbody.innerHTML = '<tr><td colspan="9">No matching products found.</td></tr>';
    return;
  }
  products.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>#${p.id}</strong></td>
      <td>${p.name}</td>
      <td>${p.category}</td>
      <td>₹${p.price.toFixed(2)}</td>
      <td><strong>${p.quantity}</strong></td>
      <td>${p.minThreshold}</td>
      <td>${p.totalSold}</td>
      <td><span class="${p.lowStock ? 'badge-low' : 'badge-ok'}">${p.lowStock ? 'Low Stock' : 'In Stock'}</span></td>
      <td><button class="btn btn-danger" onclick="deleteProduct(${p.id})">Delete</button></td>
    `;
    tbody.appendChild(tr);
  });
}

function renderAlerts(alerts) {
  const ul = document.getElementById('lowStockList');
  ul.innerHTML = '';
  if (alerts.length === 0) {
    ul.innerHTML = '<li>All products are sufficiently stocked.</li>';
    return;
  }
  alerts.forEach(p => {
    const li = document.createElement('li');
    li.innerHTML = `<span><strong>#${p.id}</strong> — ${p.name}</span><span class="status-low">${p.quantity} left (min ${p.minThreshold})</span>`;
    ul.appendChild(li);
  });
}

function renderBestSellers(sellers) {
  const ul = document.getElementById('bestSellerList');
  ul.innerHTML = '';
  sellers.forEach((p, idx) => {
    const li = document.createElement('li');
    li.innerHTML = `<span><strong>#${idx + 1}</strong> &nbsp;${p.name}</span><span><strong>${p.totalSold}</strong> sold</span>`;
    ul.appendChild(li);
  });
}

function renderHistory(records) {
  const tbody = document.getElementById('historyBody');
  tbody.innerHTML = '';
  if (records.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No stock movements recorded yet.</td></tr>';
    return;
  }
  records.forEach(r => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${r.txId}</td>
      <td>#${r.productId}</td>
      <td>${r.productName}</td>
      <td><strong>${r.actionType.replace('_', ' ')}</strong></td>
      <td>${r.quantityChanged}</td>
      <td>${r.resultingStock}</td>
      <td>${r.timestamp}</td>
    `;
    tbody.appendChild(tr);
  });
}

document.getElementById('productForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const params = new URLSearchParams({
    id: document.getElementById('prodId').value,
    name: document.getElementById('prodName').value,
    category: document.getElementById('prodCategory').value,
    price: document.getElementById('prodPrice').value,
    quantity: document.getElementById('prodQty').value,
    minThreshold: document.getElementById('prodThreshold').value
  });
  const res = await fetch('/api/product', { method: 'POST', body: params });
  if (res.ok) {
    e.target.reset();
    document.getElementById('prodThreshold').value = 10;
    loadDashboard();
  } else {
    const err = await res.json();
    alert(err.error || 'Failed to save product');
  }
});

document.getElementById('stockForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const params = new URLSearchParams({
    id: document.getElementById('stockProdId').value,
    action: document.getElementById('stockAction').value,
    amount: document.getElementById('stockAmount').value
  });
  const res = await fetch('/api/stock', { method: 'POST', body: params });
  if (res.ok) {
    e.target.reset();
    loadDashboard();
  } else {
    const err = await res.json();
    alert(err.error || 'Stock update failed');
  }
});

async function deleteProduct(id) {
  await fetch(`/api/product?id=${id}`, { method: 'DELETE' });
  loadDashboard();
}

document.getElementById('searchBtn').addEventListener('click', async () => {
  const type = document.getElementById('searchType').value;
  const q = document.getElementById('searchInput').value;
  const res = await fetch(`/api/search?type=${encodeURIComponent(type)}&q=${encodeURIComponent(q)}`);
  const data = await res.json();
  document.getElementById('searchBadge').textContent =
    q.trim() ? `Found ${data.results.length} matching item(s)` : `Showing ${data.results.length} products`;
  renderProducts(data.results);
});

document.getElementById('resetBtn').addEventListener('click', () => {
  document.getElementById('searchInput').value = '';
  loadDashboard();
});

document.getElementById('sortSelect').addEventListener('change', loadDashboard);

async function runBenchmark() {
  const size = document.getElementById('benchSize').value;
  const res = await fetch(`/api/benchmark?size=${size}`);
  const b = await res.json();
  const maxVal = Math.max(b.hashNs, b.avlNs, b.binaryNs, b.linearNs, 1);

  const rows = [
    { label: 'Hash Index Lookup', val: b.hashNs, slow: false },
    { label: 'Balanced Tree Lookup', val: b.avlNs, slow: false },
    { label: 'Binary Search (Sorted)', val: b.binaryNs, slow: false },
    { label: 'Sequential Scan', val: b.linearNs, slow: true }
  ];

  const container = document.getElementById('benchmarkBars');
  container.innerHTML = rows.map(r => {
    const pct = Math.max(4, Math.round((r.val / maxVal) * 100));
    return `
      <div class="bench-row">
        <span><strong>${r.label}</strong></span>
        <div class="bar-track"><div class="bar-fill ${r.slow ? 'slow' : ''}" style="width:${pct}%"></div></div>
        <span>${r.val.toLocaleString()} ns</span>
      </div>
    `;
  }).join('');
}

document.getElementById('runBenchBtn').addEventListener('click', runBenchmark);

loadDashboard();
runBenchmark();
