// TICKET-ADV118 — useInfiniteScroll: invokes loadMore() when sentinel is visible.
import { useEffect, useRef } from 'react';

export function useInfiniteScroll(loadMore) {

  const sentinelRef = useRef(null);

  // Keep latest loadMore callback (avoid stale closure)
  const loadMoreRef = useRef(loadMore);

  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);


  // Create IntersectionObserver once
  useEffect(() => {

    const sentinel = sentinelRef.current;

    if (!sentinel) return;


    const observer = new IntersectionObserver((entries) => {

      if (entries[0].isIntersecting) {

        loadMoreRef.current();

      }

    });


    observer.observe(sentinel);


    // Cleanup observer on unmount
    return () => {
      observer.disconnect();
    };


  }, []);


  return sentinelRef;
}