(function () {
  const table = document.getElementById('trades-table');
  const tbody = document.getElementById('trades-tbody');
  let rows = [];

  function renderRows() {
    tbody.innerHTML = rows.map(row => `
      <tr>
        <td>${row.tradeRef}</td>
        <td>${row.symbol}</td>
        <td>${row.quantity}</td>
        <td>${row.price}</td>
        <td>${row.status}</td>
      </tr>
    `).join('');
  }

  table.querySelectorAll('thead th').forEach(th => {
    th.addEventListener('click', e => {
      if (e.target.classList.contains('resize-handle')) return;
      const col = th.dataset.col;
      const type = th.dataset.type || 'string';
      const current = th.getAttribute('aria-sort');
      const direction = current === 'ascending' ? 'descending' : 'ascending';

      table.querySelectorAll('thead th').forEach(h => h.removeAttribute('aria-sort'));
      th.setAttribute('aria-sort', direction);

      const multiplier = direction === 'ascending' ? 1 : -1;
      rows.sort((a, b) => {
        const aVal = a[col];
        const bVal = b[col];
        if (type === 'number') {
          return (Number(aVal) - Number(bVal)) * multiplier;
        }
        return String(aVal).localeCompare(String(bVal)) * multiplier;
      });

      renderRows();
    });
  });

  table.querySelectorAll('.resize-handle').forEach(handle => {
    handle.addEventListener('mousedown', event => {
      event.preventDefault();
      const th = handle.closest('th');
      const startX = event.clientX;
      const startWidth = th.offsetWidth;

      function onMove(moveEvent) {
        th.style.width = `${startWidth + moveEvent.clientX - startX}px`;
      }

      function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }

      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  });

  fetch('/api/v1/trades?size=200')
    .then(res => res.json())
    .then(data => {
      rows = data.content || data;
      renderRows();
    })
    .catch(() => {
      tbody.innerHTML = '<tr><td colspan="5">Failed to load trades</td></tr>';
    });
})();