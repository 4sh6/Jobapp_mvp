import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import Badge from '../components/Badge';
import { getReferrals, markReferralPaid } from '../api/adminApi';

const STATUS_COLORS = {
  SIGNED_UP:        'blue',
  PROFILE_APPROVED: 'indigo',
  HIRED:            'amber',
  REWARD_PAID:      'green',
};

function fmt(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export default function Referrals() {
  const [referrals, setReferrals] = useState([]);
  const [filter, setFilter]       = useState('all');
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState(null);

  function load(f = filter) {
    setLoading(true);
    setError(null);
    getReferrals(f)
      .then((r) => setReferrals(r.data))
      .catch((e) => setError(e?.response?.data?.message || e?.message || 'Failed to load referrals'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, [filter]);

  async function handleMarkPaid(id, referrerName) {
    if (!window.confirm(`Mark reward as paid for referral by "${referrerName}"?`)) return;
    try {
      await markReferralPaid(id);
      load();
    } catch (e) {
      alert(e?.response?.data?.error || 'Failed to mark reward as paid');
    }
  }

  const pendingCount = referrals.filter((r) => r.status === 'HIRED').length;

  return (
    <Layout title="Referrals" subtitle="Manage the refer & earn programme">
      {/* Stats strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total Referrals', value: referrals.length, color: 'text-slate-700' },
          { label: 'Profile Approved', value: referrals.filter(r => ['PROFILE_APPROVED','HIRED','REWARD_PAID'].includes(r.status)).length, color: 'text-indigo-600' },
          { label: 'Hired', value: referrals.filter(r => ['HIRED','REWARD_PAID'].includes(r.status)).length, color: 'text-amber-600' },
          { label: 'Rewards Pending', value: pendingCount, color: 'text-red-600' },
        ].map(({ label, value, color }) => (
          <div key={label} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 text-center">
            <div className={`text-2xl font-black ${color}`}>{value}</div>
            <div className="text-xs text-slate-500 font-semibold mt-1 uppercase tracking-wide">{label}</div>
          </div>
        ))}
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 mb-4">
        {['all', 'pending'].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold border transition-all ${
              filter === f
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-300'
            }`}
          >
            {f === 'all' ? 'All Referrals' : `Pending Payout (${pendingCount})`}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="font-bold text-slate-900">Referral List</div>
          <div className="text-sm text-slate-500">{referrals.length} shown</div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-40 text-slate-400">Loading…</div>
        ) : error ? (
          <div className="p-6 text-red-500 text-sm">{error}</div>
        ) : referrals.length === 0 ? (
          <div className="flex items-center justify-center h-40 text-slate-400 text-sm">No referrals found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-xs text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-3 text-left font-semibold">Referrer</th>
                  <th className="px-4 py-3 text-left font-semibold">Referee</th>
                  <th className="px-4 py-3 text-left font-semibold">Status</th>
                  <th className="px-4 py-3 text-left font-semibold">Joined</th>
                  <th className="px-4 py-3 text-left font-semibold">Hired On</th>
                  <th className="px-4 py-3 text-left font-semibold">Paid On</th>
                  <th className="px-4 py-3 text-left font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {referrals.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-3">
                      <div className="font-semibold text-slate-800">{r.referrerName}</div>
                      <div className="text-xs text-slate-400">{r.referrerEmail}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-800">{r.refereeName}</div>
                      <div className="text-xs text-slate-400">{r.refereeEmail}</div>
                    </td>
                    <td className="px-4 py-3">
                      <Badge color={STATUS_COLORS[r.status] || 'gray'}>
                        {r.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-slate-500">{fmt(r.createdAt)}</td>
                    <td className="px-4 py-3 text-slate-500">{fmt(r.hiredAt)}</td>
                    <td className="px-4 py-3 text-slate-500">{fmt(r.rewardPaidAt)}</td>
                    <td className="px-4 py-3">
                      {r.status === 'HIRED' ? (
                        <button
                          onClick={() => handleMarkPaid(r.id, r.referrerName)}
                          className="px-3 py-1 rounded-lg bg-green-600 text-white text-xs font-bold hover:bg-green-700 transition-colors"
                        >
                          Mark Paid ₹5,000
                        </button>
                      ) : r.status === 'REWARD_PAID' ? (
                        <span className="text-green-600 font-semibold text-xs">✓ Paid</span>
                      ) : (
                        <span className="text-slate-300 text-xs">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
