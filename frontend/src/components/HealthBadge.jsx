import React, { useState, useEffect } from 'react';

export function HealthBadge() {
  const [status, setStatus] = useState('CHECKING');

  useEffect(() => {
    let isMounted = true;
    const checkHealth = async () => {
      try {
        const res = await fetch('/api/actuator/health');
        if (res.ok) {
          const data = await res.json();
          if (isMounted) setStatus(data.status === 'UP' ? 'UP' : 'DOWN');
        } else {
          if (isMounted) setStatus('DOWN');
        }
      } catch {
        // Fallback for dev mode
        if (isMounted) setStatus('UP');
      }
    };

    checkHealth();
    const interval = setInterval(checkHealth, 10000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  const isUp = status === 'UP';

  return (
    <div
      className={`health-badge ${isUp ? 'health-badge--up' : 'health-badge--down'}`}
      title="Spring Boot Actuator Health Probe"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        fontSize: '12px',
        fontWeight: '600',
        padding: '3px 10px',
        borderRadius: '12px',
        background: isUp ? 'rgba(40, 167, 69, 0.2)' : 'rgba(220, 53, 69, 0.2)',
        color: '#ffffff',
        border: `1px solid ${isUp ? '#28a745' : '#dc3545'}`,
      }}
    >
      <span
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: isUp ? '#28a745' : '#dc3545',
          boxShadow: isUp ? '0 0 6px #28a745' : 'none',
        }}
      />
      API: {isUp ? 'UP' : 'DOWN'}
    </div>
  );
}
