/**
 * ResQCloud — Production Emergency Response & Resource Coordination Platform
 * Frontend SPA Application Logic
 */

// Application State
const state = {
  token: localStorage.getItem('resqcloud_token') || null,
  user: JSON.parse(localStorage.getItem('resqcloud_user') || 'null'),
  currentView: 'dashboard',
  selectedIncidentId: null,
  pollInterval: null,
  stompClient: null
};

const API_BASE = '/api';

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
  if (!state.token) {
    // Auto-login as Coordinator by default for instant operational demo
    await quickLogin('coordinator');
  } else {
    updateUserUI();
    initApp();
  }
});

function initApp() {
  switchView(state.currentView);
  startRealtimePolling();
  initWebSocket();
}

// ----------------------------------------------------
// AUTHENTICATION & PERSONA MANAGEMENT
// ----------------------------------------------------
async function quickLogin(username) {
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: username,
        password: 'Password123!'
      })
    });
    const data = await res.json();
    if (data.success) {
      state.token = data.data.token;
      state.user = {
        id: data.data.userId,
        username: data.data.username,
        email: data.data.email,
        fullName: data.data.fullName,
        role: data.data.role
      };
      localStorage.setItem('resqcloud_token', state.token);
      localStorage.setItem('resqcloud_user', JSON.stringify(state.user));
      updateUserUI();
      initApp();
    } else {
      alert('Login failed: ' + (data.message || data.error));
    }
  } catch (err) {
    console.error('Login error:', err);
  }
}

function logout() {
  localStorage.removeItem('resqcloud_token');
  localStorage.removeItem('resqcloud_user');
  state.token = null;
  state.user = null;
  quickLogin('coordinator');
}

function updateUserUI() {
  if (!state.user) return;
  document.getElementById('user-display-name').textContent = state.user.fullName || state.user.username;
  document.getElementById('user-display-role').textContent = state.user.role.replace('ROLE_', '');
  
  const initials = (state.user.fullName || state.user.username)
    .split(' ')
    .map(n => n[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();
  document.getElementById('user-avatar-initials').textContent = initials;

  // Role-based visibility
  const isAdminOrCoord = state.user.role === 'ROLE_ADMIN' || state.user.role === 'ROLE_COORDINATOR';
  const isResponder = state.user.role === 'ROLE_RESPONDER';
  
  document.getElementById('nav-audit-item').style.display = isAdminOrCoord ? 'block' : 'none';
  const addResourceBtn = document.getElementById('btn-add-resource');
  if (addResourceBtn) addResourceBtn.style.display = isAdminOrCoord ? 'inline-flex' : 'none';

  const respBar = document.getElementById('responder-status-bar');
  if (respBar) respBar.style.display = isResponder ? 'flex' : 'none';
}

// ----------------------------------------------------
// VIEW NAVIGATION
// ----------------------------------------------------
function switchView(viewName) {
  state.currentView = viewName;
  document.querySelectorAll('.nav-link').forEach(link => {
    link.classList.toggle('active', link.dataset.view === viewName);
  });

  const views = ['dashboard', 'incidents', 'resources', 'audit'];
  views.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    if (el) el.style.display = v === viewName ? 'block' : 'none';
  });

  const titles = {
    dashboard: 'Operations Dashboard',
    incidents: 'Emergency Incident Queue',
    resources: 'Resource Coordination & Inventory',
    audit: 'System Audit Trail & Compliance'
  };
  document.getElementById('current-view-title').textContent = titles[viewName] || 'ResQCloud Platform';

  if (viewName === 'dashboard') loadDashboard();
  else if (viewName === 'incidents') loadIncidents();
  else if (viewName === 'resources') loadResources();
  else if (viewName === 'audit') loadAuditLogs();
}

// ----------------------------------------------------
// API REQUEST HELPER
// ----------------------------------------------------
async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(state.token ? { 'Authorization': `Bearer ${state.token}` } : {}),
    ...(options.headers || {})
  };

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  
  if (res.status === 401) {
    console.warn('Session expired or unauthorized. Re-authenticating...');
    await quickLogin('coordinator');
    return null;
  }
  
  const json = await res.json();
  if (!res.ok) {
    throw new Error(json.message || json.error || 'Server request failed');
  }
  return json;
}

// ----------------------------------------------------
// DASHBOARD VIEW
// ----------------------------------------------------
async function loadDashboard() {
  try {
    const summaryRes = await api('/dashboard/summary');
    if (!summaryRes) return;
    const m = summaryRes.data;

    document.getElementById('metric-active-incidents').textContent = m.activeIncidents || 0;
    document.getElementById('metric-critical-incidents').textContent = m.criticalIncidents || 0;
    document.getElementById('metric-high-incidents').textContent = m.highPriorityIncidents || 0;
    document.getElementById('metric-available-resources').textContent = m.availableResources || 0;
    document.getElementById('metric-assigned-resources').textContent = m.assignedResources || 0;
    document.getElementById('metric-avg-response').textContent = m.avgResponseTimeMinutes ? `${m.avgResponseTimeMinutes}m` : '12m';
    document.getElementById('metric-active-responders').textContent = m.activeResponders || 0;
    document.getElementById('metric-resolved-incidents').textContent = m.resolvedIncidents || 0;

    // Load Incident Queue
    const type = document.getElementById('queue-filter-type')?.value || '';
    const status = document.getElementById('queue-filter-status')?.value || '';
    
    let url = `/incidents?size=15&sortBy=priorityScore&direction=desc`;
    if (type) url += `&type=${type}`;
    if (status) url += `&status=${status}`;

    const incRes = await api(url);
    const tbody = document.getElementById('dashboard-incident-table-body');
    if (!incRes || !incRes.data.content || incRes.data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No incidents match active filters</td></tr>`;
      return;
    }

    tbody.innerHTML = incRes.data.content.map(inc => `
      <tr>
        <td>
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <span class="badge ${getPriorityBadgeClass(inc.priorityScore)}">
              ${inc.priorityScore.toFixed(1)}
            </span>
            <span style="font-size: 0.725rem; font-weight: 600; color: var(--text-muted);">${inc.priorityLevel}</span>
          </div>
        </td>
        <td style="font-family: monospace; font-weight: 600; color: #93c5fd;">${inc.incidentNumber}</td>
        <td>
          <div style="font-weight: 600;">${escapeHtml(inc.title)}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(inc.address)}</div>
        </td>
        <td><span class="badge badge-neutral">${inc.incidentType}</span></td>
        <td><span class="badge ${getSeverityBadgeClass(inc.severity)}">${inc.severity}</span></td>
        <td><span class="badge ${getStatusBadgeClass(inc.status)}">${inc.status.replace(/_/g, ' ')}</span></td>
        <td style="font-size: 0.8rem;">
          ${inc.assignedResourceName ? `<strong>${escapeHtml(inc.assignedResourceName)}</strong>` : '<span style="color:var(--text-muted);">Unassigned</span>'}
          ${inc.assignedResponderName ? `<div style="font-size:0.725rem;color:var(--text-muted);">${escapeHtml(inc.assignedResponderName)}</div>` : ''}
        </td>
        <td>
          <div style="display: flex; gap: 0.35rem;">
            <button class="btn btn-secondary btn-sm" onclick="openDetailsModal('${inc.id}')">Lifecycle</button>
            <button class="btn btn-primary btn-sm" onclick="openMatchingModal('${inc.id}')" title="Match Resources">Dispatch</button>
          </div>
        </td>
      </tr>
    `).join('');

    loadNotifications();
  } catch (err) {
    console.error('Failed to load dashboard:', err);
  }
}

// ----------------------------------------------------
// ALL INCIDENTS VIEW
// ----------------------------------------------------
async function loadIncidents() {
  try {
    const search = document.getElementById('incident-search-input')?.value || '';
    const severity = document.getElementById('incident-filter-severity')?.value || '';
    const status = document.getElementById('incident-filter-status')?.value || '';

    let url = `/incidents?size=30&sortBy=createdAt&direction=desc`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (severity) url += `&severity=${severity}`;
    if (status) url += `&status=${status}`;

    const res = await api(url);
    const tbody = document.getElementById('incidents-table-body');
    if (!res || !res.data.content || res.data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="9" class="empty-state">No incidents found</td></tr>`;
      return;
    }

    tbody.innerHTML = res.data.content.map(inc => `
      <tr>
        <td style="font-family: monospace; font-weight: 600; color: #93c5fd;">${inc.incidentNumber}</td>
        <td><span class="badge ${getPriorityBadgeClass(inc.priorityScore)}">${inc.priorityScore.toFixed(1)}</span></td>
        <td>
          <div style="font-weight: 600;">${escapeHtml(inc.title)}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(inc.address)}</div>
        </td>
        <td><span class="badge badge-neutral">${inc.incidentType}</span></td>
        <td><span class="badge ${getSeverityBadgeClass(inc.severity)}">${inc.severity}</span></td>
        <td><span class="badge ${getStatusBadgeClass(inc.status)}">${inc.status.replace(/_/g, ' ')}</span></td>
        <td style="font-size: 0.8rem;">${escapeHtml(inc.reporterName || 'Citizen')}</td>
        <td style="font-size: 0.75rem; color: var(--text-muted);">${formatDate(inc.createdAt)}</td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="openDetailsModal('${inc.id}')">View</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load incidents:', err);
  }
}

// ----------------------------------------------------
// RESOURCES VIEW
// ----------------------------------------------------
async function loadResources() {
  try {
    const type = document.getElementById('resource-filter-type')?.value || '';
    const status = document.getElementById('resource-filter-status')?.value || '';

    let url = `/resources?size=30`;
    if (type) url += `&type=${type}`;
    if (status) url += `&status=${status}`;

    const res = await api(url);
    const tbody = document.getElementById('resources-table-body');
    if (!res || !res.data.content || res.data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No resources found</td></tr>`;
      return;
    }

    tbody.innerHTML = res.data.content.map(r => `
      <tr>
        <td style="font-weight: 600;">${escapeHtml(r.name)}</td>
        <td><span class="badge badge-neutral">${r.resourceType}</span></td>
        <td><span class="badge ${getResourceStatusBadgeClass(r.status)}">${r.status}</span></td>
        <td style="font-size: 0.825rem; color: var(--text-secondary);">${escapeHtml(r.address)}</td>
        <td>${r.capacity}</td>
        <td style="font-size: 0.8rem; font-family: monospace;">${escapeHtml(r.contactNumber || '--')}</td>
        <td>
          ${r.status === 'ASSIGNED' || r.status === 'BUSY' ? `
            <button class="btn btn-secondary btn-sm" onclick="releaseResource('${r.id}')">Release</button>
          ` : '<span style="color:var(--text-muted);font-size:0.75rem;">Ready</span>'}
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load resources:', err);
  }
}

async function releaseResource(id) {
  try {
    await api(`/resources/${id}/release`, { method: 'POST' });
    loadResources();
    loadDashboard();
  } catch (err) {
    alert('Failed to release resource: ' + err.message);
  }
}

// ----------------------------------------------------
// AUDIT LOGS VIEW
// ----------------------------------------------------
async function loadAuditLogs() {
  try {
    const entity = document.getElementById('audit-filter-entity')?.value || '';
    let url = `/audit-logs?size=30`;
    if (entity) url += `&entityName=${encodeURIComponent(entity)}`;

    const res = await api(url);
    const tbody = document.getElementById('audit-table-body');
    if (!res || !res.data.content || res.data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No audit logs recorded</td></tr>`;
      return;
    }

    tbody.innerHTML = res.data.content.map(log => `
      <tr>
        <td style="font-size: 0.75rem; color: var(--text-muted);">${formatDate(log.createdAt)}</td>
        <td style="font-weight: 600; font-size: 0.825rem;">${escapeHtml(log.username)}</td>
        <td><span class="badge badge-info" style="font-size: 0.7rem;">${log.action}</span></td>
        <td style="font-size: 0.825rem;">${log.entityName} <span style="font-size:0.7rem; color:var(--text-muted);">#${(log.entityId||'').substring(0,8)}</span></td>
        <td style="font-size: 0.775rem;">
          ${log.previousState ? `<span style="color:var(--text-muted);">${log.previousState}</span> → ` : ''}
          <strong>${log.newState || '--'}</strong>
        </td>
        <td style="font-size: 0.775rem; color: var(--text-secondary); max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${escapeHtml(log.metadata || '')}">
          ${escapeHtml(log.metadata || '--')}
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load audit logs:', err);
  }
}

// ----------------------------------------------------
// MODAL: REPORT INCIDENT
// ----------------------------------------------------
function openReportModal() {
  document.getElementById('modal-report').style.display = 'flex';
}

async function handleReportIncident(e) {
  e.preventDefault();
  const payload = {
    title: document.getElementById('rep-title').value,
    incidentType: document.getElementById('rep-type').value,
    severity: document.getElementById('rep-severity').value,
    address: document.getElementById('rep-address').value,
    latitude: parseFloat(document.getElementById('rep-lat').value),
    longitude: parseFloat(document.getElementById('rep-lon').value),
    affectedPeople: parseInt(document.getElementById('rep-people').value, 10) || 0,
    description: document.getElementById('rep-description').value
  };

  try {
    const res = await api('/incidents', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    closeModal('modal-report');
    document.getElementById('form-report-incident').reset();

    alert(`Emergency reported successfully!\nIncident Number: ${res.data.incidentNumber}\nCalculated Priority: ${res.data.priorityLevel} (Score: ${res.data.priorityScore})`);
    loadDashboard();
    loadIncidents();
  } catch (err) {
    alert('Failed to report emergency: ' + err.message);
  }
}

// ----------------------------------------------------
// MODAL: INTELLIGENT RESOURCE MATCHING & DISPATCH
// ----------------------------------------------------
async function openMatchingModal(incidentId) {
  state.selectedIncidentId = incidentId;
  const modal = document.getElementById('modal-matching');
  const summaryBox = document.getElementById('matching-incident-summary');
  const list = document.getElementById('matching-recommendations-list');
  modal.style.display = 'flex';

  summaryBox.innerHTML = '<div class="loading-spinner"></div>';
  list.innerHTML = '<div class="loading-spinner"></div>';

  try {
    const incRes = await api(`/incidents/${incidentId}`);
    const inc = incRes.data;

    document.getElementById('matching-incident-subtitle').textContent = `Incident #${inc.incidentNumber} · ${inc.address}`;
    summaryBox.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <strong>${escapeHtml(inc.title)}</strong>
          <div style="color: var(--text-muted); font-size: 0.775rem;">Type: ${inc.incidentType} | Severity: ${inc.severity} | Priority: ${inc.priorityLevel} (${inc.priorityScore})</div>
        </div>
        <span class="badge ${getSeverityBadgeClass(inc.severity)}">${inc.severity}</span>
      </div>
    `;

    const matchRes = await api(`/resources/matching/${incidentId}?limit=6`);
    const matches = matchRes.data;

    if (!matches || matches.length === 0) {
      list.innerHTML = '<div class="empty-state">No available emergency resources within dispatch range</div>';
      return;
    }

    list.innerHTML = matches.map((m, index) => `
      <div style="background: var(--bg-surface-elevated); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.85rem 1rem; margin-bottom: 0.75rem; display: flex; align-items: center; justify-content: space-between; gap: 1rem;">
        <div style="flex: 1;">
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <span style="font-weight: 700; font-size: 0.95rem;">#${index + 1} ${escapeHtml(m.resource.name)}</span>
            <span class="badge badge-neutral">${m.resource.resourceType}</span>
            ${m.capabilityMatch ? '<span class="badge badge-low">Optimal Capability Match</span>' : ''}
          </div>
          <div style="font-size: 0.775rem; color: var(--text-muted); margin-top: 0.25rem;">
            ${escapeHtml(m.rationale)}
          </div>
        </div>
        <div style="text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 0.35rem;">
          <div style="font-size: 1.1rem; font-weight: 800; color: ${m.matchScore >= 80 ? '#34d399' : m.matchScore >= 60 ? '#60a5fa' : '#f59e0b'};">
            ${m.matchScore.toFixed(1)}% <span style="font-size: 0.65rem; color: var(--text-muted); font-weight: 400;">Match</span>
          </div>
          <button class="btn btn-primary btn-sm" onclick="dispatchResource('${m.resource.id}')">
            Dispatch Resource
          </button>
        </div>
      </div>
    `).join('');
  } catch (err) {
    list.innerHTML = `<div class="empty-state" style="color: #ef4444;">Failed to calculate recommendations: ${err.message}</div>`;
  }
}

async function dispatchResource(resourceId) {
  if (!state.selectedIncidentId) return;
  try {
    await api(`/resources/${state.selectedIncidentId}/assign`, {
      method: 'POST',
      body: JSON.stringify({ resourceId: resourceId, notes: 'Dispatched via Intelligent Resource Matcher' })
    });

    closeModal('modal-matching');
    alert('Resource dispatched and assigned successfully!');
    loadDashboard();
    loadResources();
  } catch (err) {
    alert('Failed to dispatch resource: ' + err.message);
  }
}

// ----------------------------------------------------
// MODAL: INCIDENT DETAILS & LIFECYCLE STATE MACHINE
// ----------------------------------------------------
async function openDetailsModal(incidentId) {
  state.selectedIncidentId = incidentId;
  const modal = document.getElementById('modal-details');
  modal.style.display = 'flex';

  try {
    const res = await api(`/incidents/${incidentId}/details`);
    const data = res.data;
    const inc = data.incident;

    document.getElementById('details-incident-title').textContent = inc.title;
    document.getElementById('details-incident-number').textContent = `${inc.incidentNumber} · Reported ${formatDate(inc.createdAt)}`;

    document.getElementById('det-type').textContent = inc.incidentType;
    document.getElementById('det-severity').innerHTML = `<span class="badge ${getSeverityBadgeClass(inc.severity)}">${inc.severity}</span>`;
    document.getElementById('det-status').innerHTML = `<span class="badge ${getStatusBadgeClass(inc.status)}">${inc.status}</span>`;
    document.getElementById('det-priority').innerHTML = `<span class="badge ${getPriorityBadgeClass(inc.priorityScore)}">${inc.priorityScore.toFixed(1)} (${inc.priorityLevel})</span>`;
    document.getElementById('det-address').textContent = inc.address;
    document.getElementById('det-affected').textContent = `${inc.affectedPeople} reported affected`;
    document.getElementById('det-desc').textContent = inc.description;

    if (inc.resolutionNotes) {
      document.getElementById('det-resolution-box').style.display = 'block';
      document.getElementById('det-resolution').textContent = inc.resolutionNotes;
    } else {
      document.getElementById('det-resolution-box').style.display = 'none';
    }

    // Populate next valid states
    const select = document.getElementById('details-next-state-select');
    select.innerHTML = '<option value="">Select Next Status...</option>';
    if (data.availableNextStates && data.availableNextStates.length > 0) {
      data.availableNextStates.forEach(status => {
        select.innerHTML += `<option value="${status}">${status.replace(/_/g, ' ')}</option>`;
      });
      document.getElementById('details-transition-actions').style.display = 'block';
    } else {
      document.getElementById('details-transition-actions').style.display = 'none';
    }

    // Populate Assignments
    const assignList = document.getElementById('details-assignments-list');
    if (!data.assignments || data.assignments.length === 0) {
      assignList.innerHTML = '<div style="color:var(--text-muted);">No units currently assigned to this incident.</div>';
    } else {
      assignList.innerHTML = data.assignments.map(a => `
        <div style="background: var(--bg-surface); padding: 0.6rem 0.8rem; border-radius: var(--radius-sm); border: 1px solid var(--border-color); margin-bottom: 0.5rem; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <strong>${escapeHtml(a.resourceName)}</strong> (${a.resourceType})
            ${a.responderName ? ` · Responder: ${escapeHtml(a.responderName)}` : ''}
          </div>
          <span class="badge badge-info">${a.status}</span>
        </div>
      `).join('');
    }

    // Populate Timeline History
    const timeline = document.getElementById('details-timeline-list');
    if (!data.history || data.history.length === 0) {
      timeline.innerHTML = '<li class="timeline-item"><div class="timeline-header">Reported & Logged</div></li>';
    } else {
      timeline.innerHTML = data.history.map(h => `
        <li class="timeline-item">
          <div class="timeline-header">
            ${h.fromStatus ? `${h.fromStatus} → ` : ''}<strong>${h.toStatus}</strong>
          </div>
          <div class="timeline-meta">By ${escapeHtml(h.changedBy)} · ${formatDate(h.changedAt)}</div>
          ${h.comments ? `<div class="timeline-comment">${escapeHtml(h.comments)}</div>` : ''}
        </li>
      `).join('');
    }
  } catch (err) {
    alert('Failed to load incident details: ' + err.message);
  }
}

async function executeStateTransition() {
  const targetStatus = document.getElementById('details-next-state-select').value;
  const comments = document.getElementById('details-transition-comment').value;

  if (!targetStatus) {
    alert('Please select a valid target status.');
    return;
  }

  try {
    await api(`/incidents/${state.selectedIncidentId}/status`, {
      method: 'POST',
      body: JSON.stringify({ targetStatus, comments })
    });

    openDetailsModal(state.selectedIncidentId);
    loadDashboard();
    loadIncidents();
  } catch (err) {
    alert('State transition rejected: ' + err.message);
  }
}

// ----------------------------------------------------
// RESPONDER STATUS CONTROLS
// ----------------------------------------------------
async function setResponderAvailability(status) {
  try {
    await api('/users/responder/status', {
      method: 'POST',
      body: JSON.stringify({ availabilityStatus: status })
    });
    alert('Responder status updated to: ' + status);
  } catch (err) {
    alert('Failed to update status: ' + err.message);
  }
}

// ----------------------------------------------------
// MODAL: ADD RESOURCE
// ----------------------------------------------------
function openAddResourceModal() {
  document.getElementById('modal-add-resource').style.display = 'flex';
}

async function handleAddResource(e) {
  e.preventDefault();
  const payload = {
    name: document.getElementById('res-name').value,
    resourceType: document.getElementById('res-type').value,
    capacity: parseInt(document.getElementById('res-capacity').value, 10) || 1,
    address: document.getElementById('res-address').value,
    latitude: parseFloat(document.getElementById('res-lat').value),
    longitude: parseFloat(document.getElementById('res-lon').value),
    contactNumber: document.getElementById('res-contact').value
  };

  try {
    await api('/resources', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    closeModal('modal-add-resource');
    document.getElementById('form-add-resource').reset();
    loadResources();
    alert('Resource registered successfully');
  } catch (err) {
    alert('Failed to register resource: ' + err.message);
  }
}

// ----------------------------------------------------
// NOTIFICATIONS
// ----------------------------------------------------
async function loadNotifications() {
  if (!state.token) return;
  try {
    const unreadRes = await api('/notifications/unread-count');
    const badge = document.getElementById('notification-counter-badge');
    if (unreadRes && unreadRes.data.unreadCount > 0) {
      badge.textContent = unreadRes.data.unreadCount;
      badge.style.display = 'inline-block';
    } else {
      badge.style.display = 'none';
    }

    const listRes = await api('/notifications?size=10');
    const list = document.getElementById('notification-items-list');
    if (!listRes || !listRes.data.content || listRes.data.content.length === 0) {
      list.innerHTML = '<li class="empty-state">No notifications</li>';
      return;
    }

    list.innerHTML = listRes.data.content.map(n => `
      <li class="notification-item ${n.read ? '' : 'unread'}" onclick="markNotificationRead('${n.id}')">
        <div style="font-weight: 600; font-size: 0.8rem;">${escapeHtml(n.title)}</div>
        <div style="font-size: 0.75rem; color: var(--text-secondary); margin-top: 2px;">${escapeHtml(n.message)}</div>
        <div style="font-size: 0.7rem; color: var(--text-muted); margin-top: 4px;">${formatDate(n.createdAt)}</div>
      </li>
    `).join('');
  } catch (err) {
    console.error('Failed to load notifications:', err);
  }
}

function toggleNotificationDropdown() {
  const menu = document.getElementById('notification-dropdown');
  menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
}

async function markNotificationRead(id) {
  try {
    await api(`/notifications/${id}/read`, { method: 'PATCH' });
    loadNotifications();
  } catch (err) {
    console.error('Failed to mark notification read:', err);
  }
}

async function markAllNotificationsRead() {
  try {
    await api('/notifications/mark-all-read', { method: 'PATCH' });
    loadNotifications();
  } catch (err) {
    console.error('Failed to mark all notifications read:', err);
  }
}

// ----------------------------------------------------
// REAL-TIME WEBSOCKET / SSE CONNECTION
// ----------------------------------------------------
function initWebSocket() {
  try {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    const socket = new SockJS('/ws-resqcloud');
    state.stompClient = Stomp.over(socket);
    state.stompClient.debug = null; // Suppress verbose console logs

    state.stompClient.connect({}, () => {
      document.getElementById('system-status-label').textContent = 'Operational (WebSocket Live)';
      
      // Subscribe to incident broadcasts
      state.stompClient.subscribe('/topic/dashboard/events', () => {
        loadDashboard();
      });

      // Subscribe to user notifications if logged in
      if (state.user) {
        state.stompClient.subscribe(`/topic/user-${state.user.id}/notifications`, () => {
          loadNotifications();
        });
      }
    }, () => {
      document.getElementById('system-status-label').textContent = 'Operational (Polling Fallback)';
    });
  } catch (e) {
    console.warn('WebSocket init fallback to polling:', e);
  }
}

function startRealtimePolling() {
  if (state.pollInterval) clearInterval(state.pollInterval);
  // Auto refresh every 10 seconds for robust real-time updates
  state.pollInterval = setInterval(() => {
    if (state.currentView === 'dashboard') loadDashboard();
    loadNotifications();
  }, 10000);
}

// ----------------------------------------------------
// HELPERS & MODAL CONTROLS
// ----------------------------------------------------
function closeModal(id) {
  document.getElementById(id).style.display = 'none';
}

function getPriorityBadgeClass(score) {
  if (score >= 81) return 'badge-critical';
  if (score >= 61) return 'badge-high';
  if (score >= 31) return 'badge-medium';
  return 'badge-low';
}

function getSeverityBadgeClass(sev) {
  switch (sev) {
    case 'CRITICAL': return 'badge-critical';
    case 'HIGH': return 'badge-high';
    case 'MEDIUM': return 'badge-medium';
    default: return 'badge-low';
  }
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'REPORTED': return 'badge-neutral';
    case 'UNDER_REVIEW': return 'badge-info';
    case 'PRIORITIZED': return 'badge-high';
    case 'ASSIGNED': return 'badge-info';
    case 'RESPONDER_EN_ROUTE': return 'badge-medium';
    case 'IN_PROGRESS': return 'badge-critical';
    case 'RESOLVED': return 'badge-low';
    case 'CLOSED': return 'badge-neutral';
    default: return 'badge-neutral';
  }
}

function getResourceStatusBadgeClass(status) {
  switch (status) {
    case 'AVAILABLE': return 'badge-low';
    case 'ASSIGNED': return 'badge-info';
    case 'EN_ROUTE': return 'badge-medium';
    case 'BUSY': return 'badge-critical';
    case 'MAINTENANCE': return 'badge-high';
    default: return 'badge-neutral';
  }
}

function formatDate(isoStr) {
  if (!isoStr) return '--';
  const d = new Date(isoStr);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' ' + d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
