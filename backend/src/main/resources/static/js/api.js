/**
 * api.js - Centralized API communication layer
 *
 * All calls to the backend go through this file.
 * This keeps fetch() logic in one place so pages stay clean.
 *
 * How JWT works here:
 *   - After login/register, we store the token in localStorage
 *   - Every protected API call adds "Authorization: Bearer <token>"
 *   - If the server returns 401, we redirect to login
 */

const API_BASE = '/api';

// ================================================================
// Token management helpers
// ================================================================

const Auth = {
  /** Save auth data to localStorage after login/register */
  save(data) {
    localStorage.setItem('token',  data.token);
    localStorage.setItem('userId', data.userId);
    localStorage.setItem('name',   data.name);
    localStorage.setItem('email',  data.email);
    localStorage.setItem('role',   data.role);
    if (data.profileImageUrl) {
      localStorage.setItem('profileImageUrl', data.profileImageUrl);
    }
  },

  /** Clear all auth data (logout) */
  clear() {
    ['token','userId','name','email','role','profileImageUrl'].forEach(k =>
      localStorage.removeItem(k)
    );
  },

  getToken()   { return localStorage.getItem('token'); },
  getRole()    { return localStorage.getItem('role'); },
  getName()    { return localStorage.getItem('name'); },
  getUserId()  { return localStorage.getItem('userId'); },
  isLoggedIn() { return !!localStorage.getItem('token'); },
  isClient()   { return localStorage.getItem('role') === 'CLIENT'; },
  isFreelancer(){ return localStorage.getItem('role') === 'FREELANCER'; },

  /** Redirect to login if not authenticated */
  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = '/pages/login.html';
      return false;
    }
    return true;
  }
};

// ================================================================
// Core HTTP helper
// ================================================================

/**
 * Makes an API call with the JWT token automatically attached.
 *
 * @param {string} endpoint  - e.g. '/projects' or '/auth/login'
 * @param {object} options   - fetch options (method, body, etc.)
 * @returns {Promise<object>} - the parsed ApiResponse body
 * @throws {Error}           - with the server's error message
 */
async function apiCall(endpoint, options = {}) {
  const token = Auth.getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  };

  // Remove Content-Type for multipart (browser sets it with boundary automatically)
  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  const data = await response.json();

  // 401 = token expired or invalid → force re-login
  if (response.status === 401) {
    Auth.clear();
    window.location.href = '/pages/login.html?expired=true';
    return;
  }

  if (!response.ok || !data.success) {
    const message = data.message || 'Something went wrong';
    throw new Error(message);
  }

  return data;
}

// ================================================================
// Auth API
// ================================================================

const AuthAPI = {
  async register(name, email, password, role) {
    return apiCall('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password, role }),
    });
  },

  async login(email, password) {
    return apiCall('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },
};

// ================================================================
// Projects API
// ================================================================

const ProjectsAPI = {
  async getAll(page = 0, size = 9) {
    return apiCall(`/projects?page=${page}&size=${size}`);
  },

  async getById(id) {
    return apiCall(`/projects/${id}`);
  },

  async getMyProjects() {
    return apiCall('/projects/my');
  },

  async create(title, description, budget, deadline) {
    return apiCall('/projects', {
      method: 'POST',
      body: JSON.stringify({ title, description, budget, deadline }),
    });
  },

  async delete(id) {
    return apiCall(`/projects/${id}`, { method: 'DELETE' });
  },

  async search(keyword) {
    return apiCall(`/projects/search?keyword=${encodeURIComponent(keyword)}`);
  },
};

// ================================================================
// Bids API
// ================================================================

const BidsAPI = {
  async submit(projectId, proposal, bidAmount) {
    return apiCall(`/bids/projects/${projectId}`, {
      method: 'POST',
      body: JSON.stringify({ proposal, bidAmount }),
    });
  },

  async getForProject(projectId) {
    return apiCall(`/bids/projects/${projectId}`);
  },

  async getMyBids() {
    return apiCall('/bids/my');
  },

  async accept(bidId) {
    return apiCall(`/bids/${bidId}/accept`, { method: 'PUT' });
  },
};

// ================================================================
// Users API
// ================================================================

const UsersAPI = {
  async getProfile() {
    return apiCall('/users/me');
  },

  async uploadProfileImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiCall('/users/me/profile-image', {
      method: 'POST',
      body: formData,
    });
  },
};

// ================================================================
// UI Helper utilities (used by all pages)
// ================================================================

const UI = {
  /**
   * Show a toast notification.
   * @param {string} message
   * @param {'success'|'error'} type
   */
  toast(message, type = 'success') {
    const id = `toast-${type}`;
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-bg-${type === 'success' ? 'success' : 'danger'} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
      <div class="d-flex">
        <div class="toast-body fw-500">${message}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto"
                data-bs-dismiss="toast"></button>
      </div>`;
    container.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { delay: 3500 });
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
  },

  /** Format a number as USD currency */
  currency(amount) {
    return new Intl.NumberFormat('en-US', {
      style: 'currency', currency: 'USD', minimumFractionDigits: 0
    }).format(amount);
  },

  /** Format a date string nicely */
  date(dateStr) {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  },

  /** Get initials from a name (for avatar placeholder) */
  initials(name = '') {
    return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  },

  /** Get status badge HTML */
  statusBadge(status) {
    const classes = {
      OPEN: 'badge-open', IN_PROGRESS: 'badge-in-progress',
      COMPLETED: 'badge-completed', CANCELLED: 'badge-cancelled',
      PENDING: 'badge-pending', ACCEPTED: 'badge-accepted', REJECTED: 'badge-rejected',
    };
    const labels = {
      OPEN: 'Open', IN_PROGRESS: 'In Progress',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled',
      PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Rejected',
    };
    return `<span class="status-pill ${classes[status] || ''}">${labels[status] || status}</span>`;
  },

  /** Show/hide a loading spinner */
  loading(show) {
    let overlay = document.getElementById('loading-overlay');
    if (show) {
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'loading-overlay';
        overlay.className = 'spinner-overlay';
        overlay.innerHTML = '<div class="spinner-border text-primary" role="status"></div>';
        document.body.appendChild(overlay);
      }
    } else {
      overlay?.remove();
    }
  },

  /** Render an empty state in a container */
  emptyState(container, icon, title, subtitle) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="icon">${icon}</div>
        <h5 class="fw-700">${title}</h5>
        <p class="mb-0">${subtitle}</p>
      </div>`;
  },
};

/**
 * Render the navbar based on login status.
 * Call this at the top of every page's script.
 */
function renderNavbar() {
  const navbar = document.getElementById('navbar-placeholder');
  if (!navbar) return;

  const isLoggedIn = Auth.isLoggedIn();
  const name = Auth.getName() || '';
  const role = Auth.getRole() || '';

  navbar.innerHTML = `
    <nav class="navbar navbar-expand-lg bg-white border-bottom sticky-top">
      <div class="container">
        <a class="navbar-brand" href="/">
          Freelance<span>Hub</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navMenu">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navMenu">
          <ul class="navbar-nav me-auto mb-2 mb-lg-0">
            <li class="nav-item">
              <a class="nav-link" href="/pages/projects.html">Browse Projects</a>
            </li>
            ${isLoggedIn && role === 'CLIENT' ? `
            <li class="nav-item">
              <a class="nav-link" href="/pages/post-project.html">Post a Project</a>
            </li>` : ''}
          </ul>
          <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-2">
            ${isLoggedIn ? `
            <li class="nav-item">
              <a class="nav-link" href="/pages/dashboard.html">Dashboard</a>
            </li>
            <li class="nav-item dropdown">
              <a class="nav-link dropdown-toggle d-flex align-items-center gap-2"
                 href="#" data-bs-toggle="dropdown">
                <div class="avatar-placeholder" style="width:30px;height:30px;font-size:.8rem">
                  ${UI.initials(name)}
                </div>
                ${name}
              </a>
              <ul class="dropdown-menu dropdown-menu-end">
                <li><a class="dropdown-item" href="/pages/profile.html">My Profile</a></li>
                <li><hr class="dropdown-divider"></li>
                <li>
                  <button class="dropdown-item text-danger" onclick="logout()">
                    Sign Out
                  </button>
                </li>
              </ul>
            </li>` : `
            <li class="nav-item">
              <a class="btn btn-outline-primary btn-sm" href="/pages/login.html">Sign In</a>
            </li>
            <li class="nav-item">
              <a class="btn btn-primary btn-sm" href="/pages/register.html">Get Started</a>
            </li>`}
          </ul>
        </div>
      </div>
    </nav>`;
}

function logout() {
  Auth.clear();
  window.location.href = '/';
}
