// TICKET-ADV120 — useMemo for portfolio-value calculations.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
    return (
        <article className="stat-card">
            <h3>{label}</h3>
            <p>{value}</p>
        </article>
    );
}

function Dashboard() {
    const { trades, isConnected } = useTradeStream();

    const portfolioValue = useMemo(() => {
        return trades.reduce((total, trade) => {
            const quantity = Number(trade.quantity ?? trade.qty);
            const price = Number(trade.price);

            const notional = quantity * price;

            return total + (Number.isFinite(notional) ? notional : 0);
        }, 0);
    }, [trades]);

    const { matched, breaks } = useMemo(() => {
        let matchedCount = 0;
        let breakCount = 0;

        for (const trade of trades) {
            if (trade.status === 'MATCHED') {
                matchedCount += 1;
            }

            if (
                trade.status === 'UNMATCHED' ||
                trade.status === 'DISPUTED'
            ) {
                breakCount += 1;
            }
        }

        return {
            matched: matchedCount,
            breaks: breakCount,
        };
    }, [trades]);

    return (
        <section>
            <h2>Dashboard</h2>

            <div className="stat-grid">
                <StatCard
                    label="Portfolio value (USD)"
                    value={portfolioValue.toLocaleString()}
                />

                <StatCard
                    label="Trades streamed"
                    value={trades.length}
                />

                <StatCard
                    label="Matched"
                    value={matched}
                />

                <StatCard
                    label="Open breaks"
                    value={breaks}
                />
            </div>

            <div role="status" aria-live="polite">
                SSE: {isConnected ? 'connected' : 'disconnected'}
            </div>
        </section>
    );
}

export default withAuth(Dashboard);