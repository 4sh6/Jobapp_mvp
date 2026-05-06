import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import Badge from '../components/Badge';
import { getUsers, approveUser, rejectUser, blockUser, unblockUser, deleteUser } from '../api/adminApi';

export default function Users() {
  const [users, setUsers]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [page, setPage]       = useState(0);

  function load(p = 0) {
    setLoading(true);
    setError(null);
    getUsers(p, 20)
      .then((r) => { setUsers(r.data); setPage(p); })
      .catch((e) => setError(e?.response?.data?.message || e?.message || 'Failed to load users'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  async function handleApprove(id) {
    await approveUser(id);
    load(page);
  }

  async function handleReject(id, name) {
    const reason = window.prompt(`Rejection reason for "${name}" (optional):`);
    if (reason === null) return; // cancelled
    await rejectUser(id, reason || undefined);
    load(page);
  }

  async function handleBlock(id, active) {
    if (active) {
      await blockUser(id);
    } else {
      await unblockUser(id);
    }
    load(page);
  }

  async function handleDelete(id, name) {
    if (!window.confirm(`Delete user "${name}"? This cannot be undone.`)) return;
    await deleteUser(id);
    load(page);
  }

  return (
    <Layout title="Users" subtitle="Manage all jobseeker accounts">
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="font-bold text-slate-900">All Users</div>
          <div className="text-sm text-slate-500">{users.length} shown</div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-48 text-slate-400">Loading…</div>
        ) : error ? (
          <div className="flex items-center justify-center h-48 text-red-500 text-sm gap-2">
            <span>⚠️</span><span>{error}</span>
            <button onClick={() => load()} className="ml-2 underline text-indigo-600">Retry</button>
          </div>
        ) : users.length === 0 ? (
          <EmptyState icon="👤" message="No users registered yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 text-left">
                  <Th>Name</Th>
                  <Th>Email</Th>
                  <Th>Approval</Th>
                  <Th>Account</Th>
                  <Th>Verified</Th>
                  <Th>Provider</Th>
                  <Th>Joined</Th>
                  <Th align="right">Actions</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50 transition">
                    <Td>
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 font-bold text-sm flex items-center justify-center flex-shrink-0">
                          {u.fullName?.[0]?.toUpperCase()}
                        </div>
                        <span className="font-semibold text-slate-900">{u.fullName}</span>
                      </div>
                    </Td>
                    <Td><span className="text-slate-600">{u.email}</span></Td>
                    <Td>
                      <Badge
                        text={u.approvalStatus ?? 'PENDING'}
                        variant={
                          u.approvalStatus === 'APPROVED' ? 'success' :
                          u.approvalStatus === 'REJECTED' ? 'blocked' :
                          u.approvalStatus === 'HIRED'    ? 'active'  : 'warning'
                        }
                      />
                    </Td>
                    <Td>
                      <Badge
                        text={u.active ? 'Active' : 'Blocked'}
                        variant={u.active ? 'active' : 'blocked'}
                      />
                    </Td>
                    <Td>
                      <Badge
                        text={u.emailVerified ? 'Verified' : 'Unverified'}
                        variant={u.emailVerified ? 'success' : 'warning'}
                      />
                    </Td>
                    <Td>
                      <span className="text-slate-500 text-sm">{u.authProvider ?? 'Email'}</span>
                    </Td>
                    <Td>
                      <span className="text-slate-500 text-sm">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-IN') : '—'}
                      </span>
                    </Td>
                    <Td align="right">
                      <div className="flex items-center justify-end gap-2 flex-wrap">
                        {(u.approvalStatus === 'PENDING' || u.approvalStatus === 'REJECTED' || !u.approvalStatus) && (
                          <button
                            onClick={() => handleApprove(u.id)}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-emerald-200 text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition"
                          >
                            Approve
                          </button>
                        )}
                        {(u.approvalStatus === 'PENDING' || u.approvalStatus === 'APPROVED' || !u.approvalStatus) && (
                          <button
                            onClick={() => handleReject(u.id, u.fullName)}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-orange-200 text-orange-700 bg-orange-50 hover:bg-orange-100 transition"
                          >
                            Reject
                          </button>
                        )}
                        <button
                          onClick={() => handleBlock(u.id, u.active)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition ${
                            u.active
                              ? 'border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100'
                              : 'border-emerald-200 text-emerald-700 bg-emerald-50 hover:bg-emerald-100'
                          }`}
                        >
                          {u.active ? 'Block' : 'Unblock'}
                        </button>
                        <button
                          onClick={() => handleDelete(u.id, u.fullName)}
                          className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-red-200 text-red-600 bg-red-50 hover:bg-red-100 transition"
                        >
                          Delete
                        </button>
                      </div>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="flex justify-between items-center px-6 py-3 border-t border-slate-100">
          <button
            onClick={() => load(page - 1)}
            disabled={page === 0}
            className="text-sm text-slate-500 disabled:opacity-40 hover:text-indigo-600 transition"
          >
            ← Previous
          </button>
          <span className="text-sm text-slate-400">Page {page + 1}</span>
          <button
            onClick={() => load(page + 1)}
            disabled={users.length < 20}
            className="text-sm text-slate-500 disabled:opacity-40 hover:text-indigo-600 transition"
          >
            Next →
          </button>
        </div>
      </div>
    </Layout>
  );
}

function Th({ children, align }) {
  return (
    <th className={`px-5 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider ${align === 'right' ? 'text-right' : 'text-left'}`}>
      {children}
    </th>
  );
}

function Td({ children, align }) {
  return (
    <td className={`px-5 py-3 text-sm ${align === 'right' ? 'text-right' : ''}`}>
      {children}
    </td>
  );
}

function EmptyState({ icon, message }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-slate-400">
      <span className="text-5xl mb-4">{icon}</span>
      <p className="font-medium">{message}</p>
    </div>
  );
}