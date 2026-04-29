import React, { useEffect, useMemo, useState } from 'react';
import io from 'socket.io-client';
import { getTenantUsage, listTenants, TenantDto, TenantUserUsageDto } from './api';
import { bytesToMb, clamp } from './format';

const SOCKET_URL = import.meta.env.VITE_SOCKET_URL ?? 'http://localhost:9092';

function ProgressBar({ percent }: { percent: number }) {
  const p = clamp(percent, 0, 100);
  return (
    <div className="progressOuter" aria-label={`usage ${p.toFixed(0)}%`}>
      <div className="progressInner" style={{ width: `${p}%` }} />
    </div>
  );
}

export function AdminStoragePage() {
  const [tenants, setTenants] = useState<TenantDto[]>([]);
  const [tenantId, setTenantId] = useState<number | null>(null);
  const [rows, setRows] = useState<TenantUserUsageDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    listTenants()
      .then((t) => {
        if (!mounted) return;
        setTenants(t);
        setTenantId((prev) => prev ?? t[0]?.tenantId ?? null);
      })
      .catch((e: unknown) => mounted && setError(e instanceof Error ? e.message : String(e)));
    return () => {
      mounted = false;
    };
  }, []);

  const loadUsage = async (id: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getTenantUsage(id);
      setRows(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tenantId == null) return;
    loadUsage(tenantId);
  }, [tenantId]);

  const socket = useMemo(() => {
    try {
      console.log('[Socket.IO] Connecting to:', SOCKET_URL);
      const s = io(SOCKET_URL, {
        transports: ['polling', 'websocket'],
        reconnection: true,
        reconnectionDelay: 1000,
        reconnectionDelayMax: 5000,
        reconnectionAttempts: 5
      });

      s.on('connect', () => {
        console.log('[Socket.IO] ✅ Connected! ID:', s.id);
      });

      s.on('connect_error', (err) => {
        console.error('[Socket.IO] ❌ Connection error:', err.message, err);
      });

      s.on('disconnect', (reason) => {
        console.warn('[Socket.IO] ⚠️ Disconnected:', reason);
      });

      s.on('error', (err) => {
        console.error('[Socket.IO] ❌ Error:', err);
      });

      return s;
    } catch (err) {
      console.error('[Socket.IO] ❌ Failed to create socket:', err);
      return null;
    }
  }, []);

  // Socket 이벤트 리스너 등록 (tenantId 변경 시 업데이트)
  useEffect(() => {
    if (!socket) return;
    const handler = (evt: { tenantId: number }) => {
      if (tenantId != null && evt.tenantId === tenantId) {
        loadUsage(tenantId);
      }
    };
    socket.on('tenantUsageUpdated', handler);
    return () => {
      socket.off('tenantUsageUpdated', handler);
    };
  }, [socket, tenantId]);

  // Socket 연결 정리 (컴포넌트 언마운트 시에만)
  useEffect(() => {
    return () => {
      if (socket) {
        console.log('[Socket.IO] Cleaning up socket connection');
        socket.disconnect();
      }
    };
  }, [socket]);

  return (
    <div className="page">
      <header className="header">
        <div>
          <div className="title">Admin / Storage</div>
          <div className="subtitle">Tenant users used/quota (Nextcloud)</div>
        </div>

        <div className="controls">
          <label className="label">
            Company
            <select
              className="select"
              value={tenantId ?? ''}
              onChange={(e) => setTenantId(Number(e.target.value))}
              disabled={tenants.length === 0}
            >
              {tenants.map((t) => (
                <option key={t.tenantId} value={t.tenantId}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </header>

      {error && <div className="error">Error: {error}</div>}

      <div className="card">
        <div className="cardHeader">
          <div>Usage</div>
          <div className="muted">{loading ? 'Loading…' : `Users: ${rows.length}`}</div>
        </div>

        <div className="tableWrap">
          <table className="table">
            <thead>
              <tr>
                <th>User ID</th>
                <th className="right">Used (MB)</th>
                <th className="right">Quota (MB)</th>
                <th className="right">Usage (%)</th>
                <th style={{ width: 220 }}> </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.userId}>
                  <td className="mono">{r.userId}</td>
                  <td className="right">{bytesToMb(r.usedBytes).toFixed(2)}</td>
                  <td className="right">{bytesToMb(r.quotaBytes).toFixed(2)}</td>
                  <td className="right">{clamp(r.usagePercent, 0, 100).toFixed(2)}</td>
                  <td>
                    <ProgressBar percent={r.usagePercent} />
                  </td>
                </tr>
              ))}
              {rows.length === 0 && !loading && (
                <tr>
                  <td colSpan={5} className="muted" style={{ padding: 18 }}>
                    No data. (If Nextcloud group has no members, this is expected.)
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

