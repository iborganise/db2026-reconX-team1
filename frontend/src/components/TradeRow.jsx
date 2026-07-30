import React from 'react';

function TradeRowImpl({ trade, onClick }) {
    const symbol = trade.symbol ?? trade.instrument ?? '';
    const quantity = trade.qty ?? trade.quantity ?? '';
    const status = trade.status ?? '';

    return (
        <button
            type="button"
            className="trade-row"
            onClick={() => onClick(trade.id)}
            aria-label={`Select trade ${trade.tradeRef}`}
        >
            <span>{trade.tradeRef}</span>
            <span>{symbol}</span>
            <span>{quantity}</span>
            <span>{trade.price}</span>
            <span className={`status-pill ${status.toLowerCase()}`}>
        {status}
      </span>
        </button>
    );
}

function areEqual(previousProps, nextProps) {
    const previousTrade = previousProps.trade;
    const nextTrade = nextProps.trade;

    return (
        previousTrade.id === nextTrade.id &&
        previousTrade.tradeRef === nextTrade.tradeRef &&
        (previousTrade.symbol ?? previousTrade.instrument) ===
        (nextTrade.symbol ?? nextTrade.instrument) &&
        (previousTrade.qty ?? previousTrade.quantity) ===
        (nextTrade.qty ?? nextTrade.quantity) &&
        previousTrade.price === nextTrade.price &&
        previousTrade.status === nextTrade.status &&
        previousProps.onClick === nextProps.onClick
    );
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);