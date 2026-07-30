// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useEffect, useRef, useState, useCallback } from 'react';

export function useWebSocket(url, { reconnect = true, maxRetries = 5 } = {}) {
  // TODO(TICKET-ADV115): open a WebSocket in a useEffect.
  //   - track readyState in `status` ('connecting' | 'open' | 'closed' | 'error').
  //   - parse incoming messages as JSON (fall back to raw string).
  //   - on close, if `reconnect` and retries < maxRetries, schedule another
  //     connect() with exponential backoff (500 * 2^attempt, capped at 30s).
  //   - cleanup must close the socket AND cancel any pending reconnect.
  const [data , setData ] = useState(null);
  const [status , setStatus ] = useState('connecting');
  const wsRef = useRef(null);
  const retriesRef = useRef(0);
  const reconnectTimeoutRef = useRef(null);
  const shouldStopRef = useRef(false);
  const connect=useCallback(()=>{
    setStatus('connecting');
    const ws=new WebSocket(url);
    wsRef.current=ws;
    ws.onopen=()=>{
      setStatus('open');
      retriesRef.current=0;
    };
    ws.onmessage=(event)=>{
      try {setData(JSON.parse(event.data));}
      catch{
        setData(event.data);
      }
    };
        ws.onclose=()=>{
      setStatus('closed');
      if(reconnect && !shouldStopRef.current && retriesRef.current<maxRetries){
        const delay=Math.min(500 * 2 ** retriesRef.current, 30000);
        retriesRef.current++;
        reconnectTimeoutRef.current=setTimeout(connect,delay);
      }
      
    };
    ws.onerror=()=>{
      setStatus('error');
    };

  },[url, reconnect, maxRetries]);
  useEffect(()=>{
    shouldStopRef.current=false;
    connect();
    return()=>{ 
      shouldStopRef.current=true;
      clearTimeout(reconnectTimeoutRef.current);
      if(wsRef.current){
        wsRef.current.close();
      }
    };
  }, [connect]);
    

  const send = useCallback((payload) => {
    if(
      wsRef.current &&
      wsRef.current.readyState === WebSocket.OPEN
    ) {
      wsRef.current.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
    // TODO(TICKET-ADV115): only send if the socket exists AND readyState === OPEN.
    //                     Serialize non-string payloads via JSON.stringify.
  }, []);
  return { data, status, send };

}
