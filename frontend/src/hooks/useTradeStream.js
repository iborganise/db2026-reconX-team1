// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useState, useEffect } from 'react';
const MAX_BUFFER=200;
export function useTradeStream(url = '/api/v1/trades/stream') {
  // TODO(TICKET-ADV116): subscribe to the SSE endpoint with `new EventSource(url)`.
  //                     - onopen   -> setConnected(true)
  //                     - onmessage(e) -> JSON.parse(e.data), prepend to `trades`,
  //                       cap the list at ~200 items so the UI doesn't blow up.
  //                     - onerror  -> setConnected(false)
  //                     Close the EventSource in the effect cleanup.
  const [trades , setTrades ] = useState([]);
  const [isConnected , setConnected ] = useState(false);
    useEffect(()=>{
    const es = new EventSource(url);

  es.onopen=()=>{
    setConnected(true);
  };
  es.onmessage=(event)=>{
    try{
    const trade=JSON.parse(event.data);
    setTrades((prev)=>[trade, ...prev.slice(0, MAX_BUFFER - 1)]);}catch(error){
      console.error('Error parsing trade data:', error);
    }
  

  };
  es.onerror=()=>{
    setConnected(false);
  };
  return ()=>{
    es.close();
  };
  }, [url]);

  return { trades, isConnected };
}
