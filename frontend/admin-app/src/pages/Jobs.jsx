import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import Badge from '../components/Badge';
import { getJobs, deleteJob } from '../api/adminApi';

export default function Jobs() {
  const [jobs, setJobs]       = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [page, setPage]       = useState(0);

  function load(p = 0) {
    setLoading(true);
    setError(null);
    getJobs(p, 20)
      .then((r) => { setJobs(r.data); setPage(p); })
      .catch((e) => setError(e?.response?.data?.message || e?.message || 'Failed to load jobs'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  async function handleDelete(id, title) {
    if (!window.confirm(`Delete job "${title}"? This cannot be undone.`)) return;
    await deleteJob(id);
    load(page);
  }

  function statusVariant(status) {
    return status === 'PUBLISHED' ? 'success' : status === 'CLOSED' ? 'default' : 'warning';
  }

  return (
    <Layout title="Jobs" subtitle="All job listings on the platform">
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="font-bold text-slate-900">All Jobs</div>
          <div className="text-sm text-slate-500">{jobs.length} shown</div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-48 text-slate-400">Loading…</div>
        ) : error ? (
          <div className="flex items-center justify-center h-48 text-red-500 text-sm gap-2">
            <span>⚠️</span><span>{error}</span>
            <button onClick={() => load()} className="ml-2 underline text-indigo-600">Retry</button>
          </div>
        ) : jobs.length === 0 ? (
          <EmptyState icon="💼" message="No jobs posted yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 text-left">
                  <Th>Job Title</Th>
                  <Th>Company</Th>
                  <Th>Location / Mode</Th>
                  <Th>Salary</Th>
                  <Th>Status</Th>
                  <Th>Posted</Th>
                  <Th align="right">Actions</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {jobs.map((j) => (
                  <tr key={j.id} className="hover:bg-slate-50 transition">
                    <Td>
                      <div className="font-semibold text-slate-900">{j.title}</div>
                      <div className="text-xs text-slate-400 mt-0.5">
                        {j.jobType?.replace('_', ' ') ?? 'Full Time'}
                      </div>
                    </Td>
                    <Td>
                      <div className="text-slate-700">{j.companyName ?? '—'}</div>
                      <div className="text-xs text-slate-400">{j.recruiterName}</div>
                    </Td>
                    <Td>
                      <div className="text-slate-600">{j.location}</div>
                      <div className="text-xs text-slate-400">{j.workMode}</div>
                    </Td>
                    <Td>
                      {j.salaryMin != null
                        ? <span className="text-slate-600">{j.salaryMin}–{j.salaryMax} LPA</span>
                        : <span className="text-slate-400">—</span>}
                    </Td>
                    <Td>
                      <Badge text={j.status} variant={statusVariant(j.status)} />
                    </Td>
                    <Td>
                      <span className="text-slate-500 text-sm">
                        {j.postedDate ? new Date(j.postedDate).toLocaleDateString('en-IN') : '—'}
                      </span>
                    </Td>
                    <Td align="right">
                      <button
                        onClick={() => handleDelete(j.id, j.title)}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-red-200 text-red-600 bg-red-50 hover:bg-red-100 transition"
                      >
                        Delete
                      </button>
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
          <button onClick={() => load(page + 1)} disabled={jobs.length < 20}
            className="text-sm text-slate-500 disabled:opacity-40 hover:text-indigo-600 transition">
            Next →
          </button>
        </div>
      </div>
    </Layout>
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