import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { readFileSync } from 'node:fs';
import { describe, expect, it, vi } from 'vitest';
import { TradeRow } from '../TradeRow.jsx';
import { resolve } from 'node:path';

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

describe('Tickets ADV119 and ADV121', () => {
    it('skips rendering when displayed trade fields and callback are unchanged', () => {
        const container = document.createElement('div');
        document.body.appendChild(container);

        const root = createRoot(container);
        const stableOnClick = vi.fn();

        let renderCount = 0;

        // Use the same equality function registered by TradeRow's React.memo.
        const TrackedTradeRow = React.memo(
            function TrackedTradeRow(props) {
                renderCount += 1;
                return <TradeRow {...props} />;
            },
            TradeRow.compare,
        );

        const trade = {
            id: 1,
            tradeRef: 'TRADE-001',
            symbol: 'AAPL',
            qty: 100,
            price: 210,
            status: 'MATCHED',
        };

        // Initial render.
        act(() => {
            root.render(
                <TrackedTradeRow
                    trade={trade}
                    onClick={stableOnClick}
                />,
            );
        });

        expect(renderCount).toBe(1);

        // New trade object, but all displayed values are identical.
        // React.memo should skip this render.
        act(() => {
            root.render(
                <TrackedTradeRow
                    trade={{ ...trade }}
                    onClick={stableOnClick}
                />,
            );
        });

        expect(renderCount).toBe(1);

        // Changing a displayed field must trigger a render.
        act(() => {
            root.render(
                <TrackedTradeRow
                    trade={{ ...trade, status: 'PENDING' }}
                    onClick={stableOnClick}
                />,
            );
        });

        expect(renderCount).toBe(2);

        // Changing the callback reference must also trigger a render.
        act(() => {
            root.render(
                <TrackedTradeRow
                    trade={{ ...trade, status: 'PENDING' }}
                    onClick={vi.fn()}
                />,
            );
        });

        expect(renderCount).toBe(3);

        act(() => {
            root.unmount();
        });

        container.remove();
    });

    it('uses a stable useCallback handler in Trades.jsx', () => {
        const tradesSource = readFileSync(
            resolve(process.cwd(), 'src/pages/Trades.jsx'),
            'utf8',
        );

        expect(tradesSource).toMatch(
            /const\s+handleSelect\s*=\s*useCallback\s*\(/,
        );

        expect(tradesSource).toMatch(
            /onClick\s*=\s*\{\s*handleSelect\s*\}/,
        );

        expect(tradesSource).not.toMatch(
            /onClick\s*=\s*\{\s*\([^)]*\)\s*=>/,
        );
    });
});