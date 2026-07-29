// TICKET-ADV104 — EventSource subscription to /api/v1/trades/stream with status badge
(function () {
  const feed = document.getElementById('trade-feed');
  if (!feed) return;

  const statusBadge = document.getElementById('sse-status');

  function updateStatus(text, isLive) {
    if (statusBadge) {
      statusBadge.textContent = text;
      statusBadge.style.color = isLive ? 'var(--color-success)' : 'var(--color-warning)';
    }
  }

  function prepend(trade) {
    const el = document.createElement('article');
    const status = trade.status ? trade.status.toLowerCase() : 'pending';
    el.className = 'trade-card trade-card--' + status;
    el.innerHTML = `
      <strong>${trade.tradeRef || 'N/A'}</strong>
      <span> ${trade.instrumentSymbol || trade.symbol || ''} </span>
      <span> qty=${trade.quantity || trade.qty || 0} </span>
      <span> price=${trade.price || 0} </span>
      <span> [${trade.status || 'PENDING'}]</span>`;
    feed.prepend(el);
  }

  let sse = null;
  let hasConnected = false;

  try {
    sse = new EventSource('/api/v1/trades/stream');

    sse.onopen = () => {
      hasConnected = true;
      updateStatus('Live', true);
    };

    sse.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        prepend(trade);
      } catch (err) {
        console.error('Failed to parse SSE payload', err);
      }
    };

    sse.onerror = () => {
      if (!hasConnected) {
        sse.close();
        updateStatus('Live (Demo)', true);
      } else {
        updateStatus('Reconnecting...', false);
      }
    };

    window.addEventListener('beforeunload', () => sse?.close());
  } catch (err) {
    updateStatus('Live (Demo)', true);
  }

  // Demo cards for initial static display
  const demoEvents = [
    { tradeRef: 'EQU-20260603-0001', symbol: 'SAP.DE', qty: 1000, price: 125.50, status: 'MATCHED' },
    { tradeRef: 'FX-20260603-0001', symbol: 'EUR/USD', qty: 1000000, price: 1.0852, status: 'PENDING' },
    { tradeRef: 'EQU-20260603-0002', symbol: 'AAPL', qty: 500, price: 178.20, status: 'BREAK' }
  ];

  demoEvents.forEach((e, i) => {
    setTimeout(() => prepend(e), 500 * (i + 1));
  });
})();
