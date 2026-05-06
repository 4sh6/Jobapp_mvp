import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import Badge from '../components/Badge';
import { getRecruiters, approveRecruiter, rejectRecruiter, suspendRecruiter } from '../api/adminApi';

export default function Recruiters() {
  const [recruiters, setRecruiters] = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [page, setPage]             = useState(0);

  function load(p = 0) {
    setLoading(true);
    setError(null);
    getRecruiters(p, 20)
      .then((r) => { setRecruiters(r.data); setPage(p); })
      .catch((e) => setError(e?.response?.data?.message || e?.message || 'Failed to load recruiters'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  async function handle(action, id) {
    const fns = { approve: approveRecruiter, reject: rejectRecruiter, suspend: suspendRecruiter };
    await fns[action](id);
    load(page);
  }

  function statusVariant(r) {
    if (r.suspended) return 'suspended';
    if (r.activationStatus === 'Active')   return 'active';
    if (r.activationStatus === 'Rejected') return 'rejected';
    return 'pending';
  }

  function statusLabel(r) {
    if (r.suspended) return 'Suspended';
    return r.activationStatus;
  }

  return (
    <Layout title="Recruiters" subtitle="Manage recruiter accounts and approvals">
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="font-bold text-slate-900">All Recruiters</div>
          <div className="text-sm text-slate-500">{recruiters.length} shown</div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-48 text-slate-400">Loading…</div>
        ) : error ? (
          <div className="flex items-center justify-center h-48 text-red-500 text-sm gap-2">
            <span>⚠️</span><span>{error}</span>
            <button onClick={() => load()} className="ml-2 underline text-indigo-600">Retry</button>
          </div>
        ) : recruiters.length === 0 ? (
          <EmptyState icon="🏢" message="No recruiters registered yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 text-left">
                  <Th>Name</Th>
                  <Th>Company</Th>
                  <Th>Email</Th>
                  <Th>Status</Th>
                  <Th>Plan</Th>
                  <Th>Joined</Th>
                  <Th align="right">Actions</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recruiters.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 transition">
                    <Td>
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-violet-100 text-violet-600 font-bold text-sm flex items-center justify-center flex-shrink-0">
                          {r.name?.[0]?.toUpperCase()}
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900">{r.name}</div>
                          <div className="text-xs text-slate-400">{r.jobTitle}</div>
                        </div>
                      </div>
                    </Td>
                    <Td><span className="text-slate-700">{r.companyName ?? '—'}</span></Td>
                    <Td><span className="text-slate-600 text-sm">{r.email}</span></Td>
                    <Td>
                      <Badge text={statusLabel(r)} variant={statusVariant(r)} />
                    </Td>
                    <Td>
                      <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${r.plan === 'PAID' ? 'bg-violet-100 text-violet-700' : 'bg-slate-100 text-slate-500'}`}>
                        {r.plan}
                      </span>
                    </Td>
                    <Td>
                      <span className="text-slate-500 text-sm">
                        {r.createdAt ? new Date(r.createdAt).toLocaleDateString('en-IN') : '—'}
                      </span>
                    </Td>
                    <Td align="right">
                      <div className="flex items-center justify-end gap-2 flex-wrap">
                        {r.activationStatus === 'Pending' && !r.suspended && (
                          <>
                            <ActionBtn color="green"  onClick={() => handle('approve', r.id)}>Approve</ActionBtn>
                            <ActionBtn color="red"    onClick={() => handle('reject', r.id)}>Reject</ActionBtn>
                          </>
                        )}
                        {r.activationStatus === 'Active' && !r.suspended && (
                          <ActionBtn color="orange" onClick={() => handle('suspend', r.id)}>Suspend</ActionBtn>
                        )}
                        {r.suspended && (
                          <ActionBtn color="green" onClick={() => handle('approve', r.id)}>Reinstate</ActionBtn>
                        )}
                        {r.activationStatus === 'Rejected' && (
                          <ActionBtn color="green" onClick={() => handle('approve', r.id)}>Approve</ActionBtn>
                        )}
                      </div>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex justify-between items-center px-6 py-3 border-t border-slate-100">
          <button onClick={() => load(page - 1)} disabled={page === 0}
            className="text-sm text-slate-500 disabled:opacity-40 hover:text-indigo-600 transition">
            ← Previous
          </button>
          <span className="text-sm text-slate-400">Page {page + 1}</span>
          <button onClick={() => load(page + 1)} disabled={recruiters.length < 20}
            className="text-sm text-slate-500 disabled:opacity-40 hover:text-indigo-600 transition">
            Next →
          </button>
        </div>
      </div>
    </Layout>
  );
}

function ActionBtn({ children, color, onClick }) {
  const colors = {
    green:  'border-emerald-200 text-emerald-700 bg-emerald-50 hover:bg-emerald-100',
    red:    'border-red-200 text-red-600 bg-red-50 hover:bg-red-100',
    orange: 'border-orange-200 text-orange-700 bg-orange-50 hover:bg-orange-100',
  };
  return (
    <button onClick={onClick} className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition ${colors[color]}`}>
      {children}
    </button>
  );
}

function Th({ children, align }) {
  return <th className={`px-5 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider ${align === 'right' ? 'text-right' : 'text-left'}`}>{children}</th>;
}
function Td({ children, align }) {
  return <td className={`px-5 py-3 text-sm ${align === 'right' ? 'text-right' : ''}`}>{children}</td>;
}
function EmptyState({ icon, message }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-slate-400">
      <span className="text-5xl mb-4">{icon}</span>
      <p className="font-medium">{message}</p>
    </div>
  );
}