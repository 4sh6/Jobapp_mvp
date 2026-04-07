import { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import StatCard from '../components/StatCard';
import { getDashboard } from '../api/adminApi';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDashboard()
      .then((r) => setStats(r.data))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout title="Dashboard" subtitle="Platform overview at a glance">
      {loading ? (
        <div className="flex items-center justify-center h-48 text-slate-400">Loading…</div>
      ) : (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <StatCard label="Total Users"       value={stats?.totalUsers}        icon="👤" color="indigo" />
            <StatCard label="Total Jobs"         value={stats?.totalJobs}         icon="💼" color="amber" />
            <StatCard label="Recruiters"         value={stats?.totalRecruiters}   icon="🏢" color="violet" />
            <StatCard label="Applications"       value={stats?.totalApplications} icon="📄" color="green" />
          </div>

          {stats?.pendingRecruiters > 0 && (
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
              <span className="text-2xl">⏳</span>
              <div>
                <div className="font-bold text-amber-800 text-sm">
                  {stats.pendingRecruiters} recruiter{stats.pendingRecruiters > 1 ? 's' : ''} awaiting approval
                </div>
                <a href="/recruiters" className="text-amber-600 text-sm underline">Review now →</a>
              </div>
            </div>
          )}

          <div className="mt-8 grid grid-cols-1 lg:grid-cols-3 gap-4">
            <QuickLink href="/users"      icon="👤" label="Manage Users"      desc="View, block, or delete users" />
            <QuickLink href="/jobs"       icon="💼" label="Manage Jobs"       desc="Review and delete job listings" />
            <QuickLink href="/recruiters" icon="🏢" label="Manage Recruiters" desc="Approve, reject or suspend" />
          </div>
        </>
      )}
    </Layout>
  );
}

function QuickLink({ href, icon, label, desc }) {
  return (
    <a href={href} className="bg-white border border-slate-200 rounded-xl p-5 hover:border-indigo-300 hover:shadow-md transition group">
      <div className="text-3xl mb-3">{icon}</div>
      <div className="font-bold text-slate-900 group-hover:text-indigo-600 transition">{label}</div>
      <div className="text-sm text-slate-500 mt-1">{desc}</div>
    </a>
  );
}