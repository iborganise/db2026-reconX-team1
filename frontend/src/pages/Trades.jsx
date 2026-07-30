// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
// TICKET-ADV121 — useCallback for stable row selection handler.
import React, { useCallback, useState } from 'react';
import { TradeRow } from '@components/TradeRow.jsx';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';

function Trades() {
    const [search, setSearch] = useState('');
    const debounced = useDebouncedSearch(search, 300);
    const [page, setPage] = useState(0);
    const [data] = useState({
        items: [],
        totalPages: 0,
    });
    const [selectedId, setSelectedId] = useState(null);

    const handleSelect = useCallback((id) => {
        setSelectedId(id);
    }, []);

    // TODO(TICKET-ADV114 + ADV117): useEffect that:
    //   - builds a query string from `page` and `debounced` (status filter)
    //   - calls api.listTrades(params) and stores the response in `data`
    //   - re-runs whenever `page` or `debounced` changes
    //   - degrades gracefully on error (set empty page).

    return (
        <section>
            <h2>Trades</h2>

            <input
                aria-label="Filter by status"
                placeholder="status filter (PENDING/MATCHED/…)"
                value={search}
                onChange={(event) => {
                    setSearch(event.target.value.toUpperCase());
                    setPage(0);
                }}
            />

            {selectedId !== null && (
                <p aria-live="polite">
                    Selected trade ID: {selectedId}
                </p>
            )}

            <DataTable>
                <DataTable.Header
                    columns={[
                        { key: 'tradeRef', label: 'Ref' },
                        { key: 'symbol', label: 'Symbol' },
                        { key: 'qty', label: 'Qty' },
                        { key: 'price', label: 'Price' },
                        { key: 'status', label: 'Status' },
                    ]}
                />

                <DataTable.Body
                    rows={data.items}
                    render={(trade) => (
                        <TradeRow
                            trade={trade}
                            onClick={handleSelect}
                        />
                    )}
                />

                <DataTable.Pagination
                    page={page}
                    totalPages={Math.max(1, data.totalPages)}
                    onChange={setPage}
                />
            </DataTable>

            <p>
                Debounced filter: {debounced || 'None'}
            </p>
        </section>
    );
}

export default withAuth(Trades);