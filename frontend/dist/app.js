/**
 * CampusPulse - Creative Next-Gen College Event Experience
 */

const API_BASE = window.location.origin.startsWith('http')
  ? window.location.origin
  : 'http://localhost:8085';

class App {
  constructor() {
    this.currentUser = null;
    this.events = [];
    this.registrations = [];
    this.activeCategory = 'ALL';
    this.searchQuery = '';
    this.selectedEventForReg = null;

    this.init();
  }

  async init() {
    // Check saved session in localStorage
    const savedUser = localStorage.getItem('campuspulse_user');
    if (savedUser) {
      try {
        this.currentUser = JSON.parse(savedUser);
      } catch (e) {
        this.currentUser = null;
      }
    }

    this.initCanvasAnimation();

    if (this.currentUser) {
      this.showMainApp();
    } else {
      this.showAuthView();
    }
  }

  // ==================== AUTHENTICATION ====================
  setAuthMode(mode) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    document.getElementById(mode === 'login' ? 'tabLoginBtn' : 'tabRegisterBtn').classList.add('active');

    document.getElementById('loginForm').classList.toggle('hidden', mode !== 'login');
    document.getElementById('registerForm').classList.toggle('hidden', mode !== 'register');
  }

  fillDemoLogin(username, password) {
    document.getElementById('loginUsername').value = username;
    document.getElementById('loginPassword').value = password;
  }

  async handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value.trim();

    try {
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });

      const data = await res.json();
      if (res.ok && data.authenticated) {
        if (data.role === 'COORDINATOR') {
          this.currentUser = {
            id: 99,
            name: 'Faculty Coordinator',
            username: 'coordinator',
            department: 'Administration',
            role: 'COORDINATOR'
          };
        } else {
          this.currentUser = {
            id: data.student.id,
            name: data.student.name,
            username: data.student.username,
            department: data.student.department,
            role: 'STUDENT'
          };
        }

        localStorage.setItem('campuspulse_user', JSON.stringify(this.currentUser));
        this.showToast(`Authenticated: ${this.currentUser.name}`, 'success');
        this.showMainApp();
      } else {
        this.showToast(data.error || 'Invalid credentials', 'error');
      }
    } catch (err) {
      this.showToast('Login request failed', 'error');
    }
  }

  async handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('regFullName').value.trim();
    const username = document.getElementById('regUsername').value.trim();
    const department = document.getElementById('regDepartment').value;
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirm = document.getElementById('regConfirmPassword').value;

    if (password !== confirm) {
      this.showToast('Passwords do not match', 'error');
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, username, department, email, password })
      });

      const data = await res.json();
      if (res.ok) {
        this.showToast('Student account created! You can now sign in.', 'success');
        this.setAuthMode('login');
        this.fillDemoLogin(username, password);
      } else {
        this.showToast(data.error || 'Registration failed', 'error');
      }
    } catch (err) {
      this.showToast('Registration request failed', 'error');
    }
  }

  logout() {
    this.currentUser = null;
    localStorage.removeItem('campuspulse_user');
    this.showAuthView();
    this.showToast('Signed out of portal', 'info');
  }

  showAuthView() {
    document.getElementById('authView').classList.remove('hidden');
    document.getElementById('mainAppView').classList.add('hidden');
  }

  showMainApp() {
    document.getElementById('authView').classList.add('hidden');
    document.getElementById('mainAppView').classList.remove('hidden');

    const isCoordinator = this.currentUser.role === 'COORDINATOR';
    document.getElementById('createEventTopBtn').classList.toggle('hidden', !isCoordinator);
    document.getElementById('navDraftsBtn').classList.toggle('hidden', !isCoordinator);
    document.getElementById('navMyRegistrationsBtn').classList.toggle('hidden', isCoordinator);

    document.getElementById('userDisplayName').textContent = this.currentUser.name;
    document.getElementById('userDisplayRole').textContent = isCoordinator
      ? 'Coordinator • Admin'
      : `Student • ${this.currentUser.department}`;

    const greetingEl = document.getElementById('greetingBadge');
    if (greetingEl) {
      greetingEl.textContent = `User: ${this.currentUser.name} (${this.currentUser.department})`;
    }

    this.showView('events');
    this.loadEvents();
    if (!isCoordinator) {
      this.loadRegistrations();
    }
  }

  // ==================== NAVIGATION VIEWS ====================
  showView(viewName) {
    document.querySelectorAll('.nav-btn').forEach(l => l.classList.remove('active'));
    document.querySelectorAll('.content-view').forEach(v => v.classList.remove('active'));

    if (viewName === 'events') {
      const b = document.getElementById('navEventsBtn');
      if (b) b.classList.add('active');
      const v = document.getElementById('viewEvents');
      if (v) v.classList.add('active');
    } else if (viewName === 'registrations') {
      const b = document.getElementById('navMyRegistrationsBtn');
      if (b) b.classList.add('active');
      const v = document.getElementById('viewRegistrations');
      if (v) v.classList.add('active');
      this.loadRegistrations();
    } else if (viewName === 'drafts') {
      const b = document.getElementById('navDraftsBtn');
      if (b) b.classList.add('active');
      const v = document.getElementById('viewDrafts');
      if (v) v.classList.add('active');
      this.renderDrafts();
    }
  }

  // ==================== EVENTS DISCOVERY ====================
  async loadEvents() {
    try {
      const res = await fetch(`${API_BASE}/api/events`);
      if (res.ok) {
        this.events = await res.json();
        const liveCount = this.events.filter(e => e.status !== 'DRAFT').length;
        document.getElementById('statLiveCount').textContent = liveCount;
        this.renderEvents();
        if (this.currentUser && this.currentUser.role === 'COORDINATOR') {
          this.renderDrafts();
        }
      }
    } catch (err) {
      this.showToast('Could not load events from server', 'error');
    }
  }

  handleSearch(e) {
    this.searchQuery = (e.target.value || '').toLowerCase().trim();
    this.renderEvents();
  }

  filterCategory(cat) {
    this.activeCategory = cat;
    document.querySelectorAll('#categoryFilters .filter-btn').forEach(pill => {
      pill.classList.toggle('active', pill.textContent.toUpperCase().includes(cat) || (cat === 'ALL' && pill.textContent.trim() === 'All'));
    });
    this.renderEvents();
  }

  formatDateBlock(dateStr) {
    if (!dateStr) return { month: 'TBD', day: '--' };
    const parts = dateStr.split('-');
    if (parts.length >= 3) {
      const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      const month = d.toLocaleString('en-US', { month: 'short' }).toUpperCase();
      return { month, day: parts[2] };
    }
    return { month: 'DATE', day: dateStr };
  }

  renderEvents() {
    const container = document.getElementById('eventsGrid');
    if (!container) return;

    let filtered = this.events.filter(e => e.status !== 'DRAFT');

    if (this.activeCategory !== 'ALL') {
      filtered = filtered.filter(e => (e.category || '').toUpperCase() === this.activeCategory);
    }

    if (this.searchQuery) {
      filtered = filtered.filter(e =>
        (e.name || '').toLowerCase().includes(this.searchQuery) ||
        (e.venue || '').toLowerCase().includes(this.searchQuery) ||
        (e.description || '').toLowerCase().includes(this.searchQuery)
      );
    }

    if (filtered.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 48px 20px; color: var(--text-secondary);">
          <div style="font-size: 15px; font-weight: 700; margin-bottom: 6px;">[ NO MATCHING RECORDS ]</div>
          <p style="font-size: 13px; margin: 0;">No active campus events match your query.</p>
        </div>
      `;
      return;
    }

    const isCoordinator = this.currentUser && this.currentUser.role === 'COORDINATOR';

    container.innerHTML = filtered.map(e => {
      const isFull = e.availableSeats <= 0;
      const statusLabel = isFull
        ? `<span class="status-label status-full">FULL</span>`
        : `<span class="status-label status-open">SEATS OPEN</span>`;

      const percent = Math.min(100, Math.round(((e.capacity - e.availableSeats) / e.capacity) * 100));

      return `
        <div class="academic-event-card">
          <div class="card-top-header">
            <span class="cat-label">${this.escapeHtml(e.category || 'GENERAL')}</span>
            ${statusLabel}
          </div>

          <h3 class="card-title-text">${this.escapeHtml(e.name)}</h3>
          <p class="card-desc-text">${this.escapeHtml(e.description || 'Institutional campus program.')}</p>

          <div class="card-metadata">
            <div class="meta-line">
              <span class="meta-k">DATE:</span>
              <span class="meta-v">${this.escapeHtml(e.date)}</span>
            </div>
            <div class="meta-line">
              <span class="meta-k">VENUE:</span>
              <span class="meta-v">${this.escapeHtml(e.venue)}</span>
            </div>
          </div>

          <div class="capacity-progress-line">
            <div class="capacity-stats">
              <span>Allotment: ${e.availableSeats} of ${e.capacity} left</span>
              <span>${percent}%</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill" style="width: ${percent}%;"></div>
            </div>
          </div>

          <div class="card-button-row">
            ${isCoordinator
              ? `
                <button class="btn btn-outline btn-sm" onclick="app.cloneEventAsDraft(${e.id})" title="Duplicate event as draft">
                  Duplicate
                </button>
                <button class="btn btn-outline btn-sm" onclick="app.openEditModal(${e.id})" title="Edit event and view audit history">
                  Audit &amp; Edit
                </button>
              `
              : `
                <button class="btn btn-primary btn-block" ${isFull ? 'disabled' : ''} onclick="app.openRegisterModal(${e.id})">
                  ${isFull ? 'Registration Full' : 'Register for Pass &rarr;'}
                </button>
              `
            }
          </div>
        </div>
      `;
    }).join('');
  }

  // Coordinator Staging View
  renderDrafts() {
    const container = document.getElementById('draftsGrid');
    if (!container) return;

    const drafts = this.events.filter(e => e.status === 'DRAFT');

    if (drafts.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 48px 20px; color: var(--text-secondary);">
          <div style="font-size: 15px; font-weight: 700; margin-bottom: 6px;">[ STAGING EMPTY ]</div>
          <p style="font-size: 13px; margin: 0;">No unpublished drafts in the repository.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = drafts.map(e => {
      return `
        <div class="academic-event-card" style="border: 1px dashed var(--warning);">
          <div class="card-top-header">
            <span class="cat-label">${this.escapeHtml(e.category)}</span>
            <span class="status-label status-draft">DRAFT STAGING</span>
          </div>

          <h3 class="card-title-text">${this.escapeHtml(e.name)}</h3>
          <p class="card-desc-text">${this.escapeHtml(e.description || 'Pre-publication draft configuration.')}</p>

          <div class="card-metadata">
            <div class="meta-line">
              <span class="meta-k">SCHEDULED:</span>
              <span class="meta-v">${this.escapeHtml(e.date)}</span>
            </div>
            <div class="meta-line">
              <span class="meta-k">HALL &amp; SEATS:</span>
              <span class="meta-v">${this.escapeHtml(e.venue)} &bull; ${e.capacity}</span>
            </div>
          </div>

          <div class="card-button-row" style="margin-top: auto;">
            <button class="btn btn-primary" style="flex: 1;" onclick="app.publishDraft(${e.id})">
              Publish to Catalog
            </button>
            <button class="btn btn-outline btn-sm" onclick="app.openEditModal(${e.id})">
              Edit
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  // ==================== COORDINATOR ACTIONS ====================
  async cloneEventAsDraft(eventId) {
    const event = this.events.find(e => e.id === eventId);
    if (!event) return;

    try {
      const res = await fetch(`${API_BASE}/api/events/${eventId}/clone`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customName: `[Draft] ${event.name}` })
      });

      if (res.ok) {
        this.showToast('Event cloned as an isolated draft in Staging!', 'success');
        await this.loadEvents();
        this.showView('drafts');
      } else {
        this.showToast('Failed to duplicate event', 'error');
      }
    } catch (err) {
      this.showToast('Duplicate request error', 'error');
    }
  }

  async publishDraft(eventId) {
    try {
      const res = await fetch(`${API_BASE}/api/events/${eventId}/workflow`, {
        method: 'POST'
      });

      if (res.ok) {
        this.showToast('Preparation workflow passed! Event is now published.', 'success');
        await this.loadEvents();
        this.showView('events');
      } else {
        this.showToast('Failed to publish event', 'error');
      }
    } catch (err) {
      this.showToast('Publish request error', 'error');
    }
  }

  async handleCreateEvent(e) {
    e.preventDefault();
    const name = document.getElementById('evtName').value.trim();
    const category = document.getElementById('evtCategory').value;
    const description = document.getElementById('evtDesc').value.trim();
    const date = document.getElementById('evtDate').value;
    const capacity = parseInt(document.getElementById('evtCapacity').value);
    const venue = document.getElementById('evtVenue').value.trim();

    try {
      const res = await fetch(`${API_BASE}/api/events`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name, category, description, date, capacity, venue,
          status: 'OPEN',
          author: this.currentUser.name
        })
      });

      if (res.ok) {
        this.showToast('Event created and published to campus!', 'success');
        this.closeModal('createEventModal');
        await this.loadEvents();
      } else {
        this.showToast('Failed to create event', 'error');
      }
    } catch (err) {
      this.showToast('Create request error', 'error');
    }
  }

  // ==================== EDIT & REVISION HISTORY ====================
  async openEditModal(eventId) {
    const event = this.events.find(e => e.id === eventId);
    if (!event) return;

    document.getElementById('editEventId').value = event.id;
    document.getElementById('editModalTitle').textContent = `Edit & History: ${event.name}`;
    document.getElementById('editVenue').value = event.venue;
    document.getElementById('editCapacity').value = event.capacity;
    document.getElementById('editStatus').value = event.status;
    document.getElementById('editReason').value = '';

    this.openModal('editEventModal');
    await this.loadEventHistory(eventId);
  }

  async loadEventHistory(eventId) {
    const container = document.getElementById('eventHistoryTimeline');
    if (!container) return;

    try {
      const res = await fetch(`${API_BASE}/api/events/${eventId}/memento/history`);
      if (res.ok) {
        const history = await res.json();
        if (history.length === 0) {
          container.innerHTML = `<div style="font-size: 12px; color: var(--text-secondary);">No checkpoints recorded.</div>`;
          return;
        }

        container.innerHTML = history.slice().reverse().map(m => `
          <div class="checkpoint-card">
            <div>
              <span class="checkpoint-version">VERSION ${m.version}</span>
              <div class="checkpoint-note">${this.escapeHtml(m.changeSummary || 'Audit checkpoint')}</div>
              <div class="checkpoint-details">
                ${m.timestamp} &bull; ${m.venue} (${m.capacity} seats) &bull; ${m.author}
              </div>
            </div>
            <button type="button" class="btn btn-outline btn-sm" onclick="app.restoreEventVersion(${eventId}, ${m.version})">
              Restore
            </button>
          </div>
        `).join('');
      }
    } catch (err) {
      container.innerHTML = `<div style="font-size: 12px; color: var(--error);">Could not load history.</div>`;
    }
  }

  async handleSaveEdit(e) {
    e.preventDefault();
    const eventId = parseInt(document.getElementById('editEventId').value);
    const event = this.events.find(e => e.id === eventId);
    if (!event) return;

    const venue = document.getElementById('editVenue').value.trim();
    const capacity = parseInt(document.getElementById('editCapacity').value);
    const status = document.getElementById('editStatus').value;
    const changeReason = document.getElementById('editReason').value.trim();

    try {
      const res = await fetch(`${API_BASE}/api/events/${eventId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: event.name,
          category: event.category,
          description: event.description,
          date: event.date,
          venue,
          capacity,
          status,
          changeReason,
          author: this.currentUser.name
        })
      });

      if (res.ok) {
        this.showToast('Event updated and revision checkpoint saved!', 'success');
        this.closeModal('editEventModal');
        await this.loadEvents();
      } else {
        this.showToast('Failed to save changes', 'error');
      }
    } catch (err) {
      this.showToast('Save changes error', 'error');
    }
  }

  async restoreEventVersion(eventId, version) {
    if (!confirm(`Restore this event state back to revision v${version}?`)) return;

    try {
      const res = await fetch(`${API_BASE}/api/events/${eventId}/memento/restore`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetVersion: version })
      });

      if (res.ok) {
        this.showToast(`State restored to checkpoint v${version}`, 'success');
        this.closeModal('editEventModal');
        await this.loadEvents();
      } else {
        this.showToast('Restore failed', 'error');
      }
    } catch (err) {
      this.showToast('Restore error occurred', 'error');
    }
  }

  // ==================== DIGITAL TICKET CHECKOUT ====================
  openRegisterModal(eventId) {
    const event = this.events.find(e => e.id === eventId);
    if (!event) return;

    this.selectedEventForReg = event;
    document.getElementById('checkoutEventTitle').textContent = event.name;
    document.getElementById('checkoutEventMeta').textContent = `${event.date} • ${event.venue}`;

    document.getElementById('ticketEventName').textContent = event.name;
    document.getElementById('ticketDateVenue').textContent = `${event.date} • ${event.venue}`;
    document.getElementById('ticketAttendee').textContent = this.currentUser ? this.currentUser.name : 'Student';
    document.getElementById('ticketDept').textContent = this.currentUser ? this.currentUser.department : 'CSE';

    this.fetchPricingAndUpdate();
    this.openModal('registerModal');
  }

  async fetchPricingAndUpdate() {
    if (!this.selectedEventForReg) return;
    const cat = this.selectedEventForReg.category || 'TECHNICAL';

    try {
      const res = await fetch(`${API_BASE}/api/fee/calculate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ category: cat, tier: 'STANDARD' })
      });

      if (res.ok) {
        const data = await res.json();
        this.cachedPricing = data.comparison;
        document.getElementById('checkoutEarlyPrice').textContent = `$${data.comparison.EARLY_BIRD.finalFee.toFixed(2)}`;
        document.getElementById('checkoutStandardPrice').textContent = `$${data.comparison.STANDARD.finalFee.toFixed(2)}`;
        this.updateInteractiveTicket();
      }
    } catch (err) {
      console.warn('Fee fetch failed', err);
    }
  }

  updateInteractiveTicket() {
    const tierRadio = document.querySelector('input[name="ticketTier"]:checked');
    const tier = tierRadio ? tierRadio.value : 'EARLY_BIRD';

    const tagEl = document.getElementById('ticketTierTag');
    const feeEl = document.getElementById('ticketFeeHighlight');

    if (tier === 'FREE') {
      if (tagEl) tagEl.textContent = 'FREE PASS';
      if (feeEl) feeEl.textContent = '$0.00';
    } else if (tier === 'STANDARD') {
      if (tagEl) tagEl.textContent = 'STANDARD PASS';
      const price = this.cachedPricing ? this.cachedPricing.STANDARD.finalFee : 150;
      if (feeEl) feeEl.textContent = `$${price.toFixed(2)}`;
    } else {
      if (tagEl) tagEl.textContent = 'EARLY BIRD PASS';
      const price = this.cachedPricing ? this.cachedPricing.EARLY_BIRD.finalFee : 105;
      if (feeEl) feeEl.textContent = `$${price.toFixed(2)}`;
    }
  }

  async submitRegistration() {
    if (!this.selectedEventForReg) return;

    const tier = document.querySelector('input[name="ticketTier"]:checked').value;
    const channel = document.getElementById('regNotificationChannel').value;

    const btn = document.getElementById('submitRegBtn');
    btn.disabled = true;
    btn.textContent = 'Recording Registration...';

    try {
      const res = await fetch(`${API_BASE}/api/registrations/mediate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          studentId: this.currentUser.id,
          eventId: this.selectedEventForReg.id,
          feeTier: tier,
          notificationChannel: channel
        })
      });

      const data = await res.json();
      btn.disabled = false;
      btn.textContent = 'Confirm Registration →';

      if (res.ok && data.success) {
        this.showToast(`Registration completed: #${data.registrationId}`, 'success');
        this.closeModal('registerModal');
        await this.loadEvents();
        await this.loadRegistrations();
        this.showView('registrations');
      } else {
        this.showToast(data.message || 'Registration could not be completed', 'error');
      }
    } catch (err) {
      btn.disabled = false;
      btn.textContent = 'Confirm Registration →';
      this.showToast('Registration error occurred', 'error');
    }
  }

  // ==================== DIGITAL PASSES (MY REGISTRATIONS) ====================
  async loadRegistrations() {
    if (!this.currentUser || this.currentUser.role === 'COORDINATOR') return;

    try {
      const res = await fetch(`${API_BASE}/api/registrations/student/${this.currentUser.id}`);
      if (res.ok) {
        this.registrations = await res.json();
        this.renderPassesWallet();
      }
    } catch (err) {
      console.warn('Failed to load student passes', err);
    }
  }

  renderPassesWallet() {
    const container = document.getElementById('registrationsPassesGrid');
    if (!container) return;

    if (this.registrations.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 48px 20px; color: var(--text-secondary);">
          <div style="font-size: 15px; font-weight: 700; margin-bottom: 6px;">[ NO ADMISSION PASSES RECORDED ]</div>
          <p style="font-size: 13px; margin-bottom: 16px;">You have not registered for any events for the current term.</p>
          <button class="btn btn-primary" onclick="app.showView('events')">
            Browse Event Catalog &rarr;
          </button>
        </div>
      `;
      return;
    }

    container.innerHTML = this.registrations.map(r => {
      const status = (r.status || 'PENDING').toUpperCase();
      const statusClass = status === 'APPROVED' ? 'status-open' : (status === 'REJECTED' ? 'status-full' : 'status-draft');

      return `
        <div class="pass-record-card">
          <div class="pass-stub-main">
            <div class="pass-code-row">
              <span class="cat-label">${this.escapeHtml(r.category || 'TECHNICAL')}</span>
              <span class="pass-id">PASS #${r.registrationId}</span>
            </div>

            <h3 class="pass-event-title">${this.escapeHtml(r.eventName)}</h3>

            <div class="pass-attendee-row">
              <span>STUDENT: ${this.escapeHtml(this.currentUser.name)}</span>
              <span>DEPT: ${this.currentUser.department}</span>
            </div>
          </div>

          <div class="pass-stub-divider"></div>

          <div class="pass-stub-side">
            <span class="status-label ${statusClass}">${status}</span>
            <button class="btn-cancel-pass" onclick="app.cancelRegistration(${r.registrationId})">
              [ Cancel ]
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  async cancelRegistration(regId) {
    if (!confirm('Cancel this event pass? This will surrender your seat.')) return;

    try {
      const res = await fetch(`${API_BASE}/api/registrations/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ registrationId: regId, studentId: this.currentUser.id })
      });

      if (res.ok) {
        this.showToast('Pass surrendered and seat released', 'success');
        await this.loadRegistrations();
        await this.loadEvents();
      } else {
        this.showToast('Failed to cancel pass', 'error');
      }
    } catch (err) {
      this.showToast('Cancellation error occurred', 'error');
    }
  }

  // ==================== MODALS & TOASTS ====================
  openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
  }

  closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
  }

  showToast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    const toastTypeClass = type === 'success' ? 'toast-success' : (type === 'error' ? 'toast-error' : '');
    toast.className = `academic-toast ${toastTypeClass}`;
    const icon = type === 'success' ? '[OK]' : (type === 'error' ? '[ERR]' : '[INFO]');
    toast.innerHTML = `<span style="font-weight: 700; margin-right: 6px;">${icon}</span> <span>${this.escapeHtml(msg)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.2s ease';
      setTimeout(() => toast.remove(), 250);
    }, 3200);
  }

  escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag));
  }

  // ==================== CAMPUS EVENT BACKDROP ANIMATION ====================
  initCanvasAnimation() {
    const canvas = document.getElementById('campusCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let width = 0;
    let height = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);

    const resize = () => {
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      ctx.scale(dpr, dpr);
    };

    window.addEventListener('resize', resize);
    resize();

    // Campus venue constellation nodes
    const venueDefinitions = [
      { name: 'INNOVATION LAB', code: 'HALL-A', xRel: 0.14, yRel: 0.22 },
      { name: 'MAIN AUDITORIUM', code: 'AUD-01', xRel: 0.84, yRel: 0.26 },
      { name: 'SPORTS ARENA', code: 'QUAD-B', xRel: 0.16, yRel: 0.76 },
      { name: 'SEMINAR HALL', code: 'ROOM-3', xRel: 0.78, yRel: 0.74 },
      { name: 'AMPHITHEATRE', code: 'OPEN-AIR', xRel: 0.48, yRel: 0.15 },
      { name: 'STUDENT UNION', code: 'WING-C', xRel: 0.52, yRel: 0.85 },
      { name: 'TECH INCUBATOR', code: 'LAB-204', xRel: 0.30, yRel: 0.40 },
      { name: 'CENTRAL ARCHIVE', code: 'LIB-08', xRel: 0.70, yRel: 0.45 },
      { name: 'DESIGN STUDIO', code: 'STUDIO-1', xRel: 0.07, yRel: 0.48 },
      { name: 'CYBER LAB', code: 'TERM-5', xRel: 0.90, yRel: 0.50 }
    ];

    const nodes = venueDefinitions.map(def => ({
      name: def.name,
      code: def.code,
      baseX: def.xRel * width,
      baseY: def.yRel * height,
      x: def.xRel * width,
      y: def.yRel * height,
      angle: Math.random() * Math.PI * 2,
      pulse: Math.random() * Math.PI * 2
    }));

    const packets = [];
    const spawnPacket = (fromIdx, toIdx) => {
      packets.push({
        from: fromIdx,
        to: toIdx,
        progress: 0,
        speed: 0.005 + Math.random() * 0.005
      });
    };

    let mouseX = -1000;
    let mouseY = -1000;
    window.addEventListener('pointermove', e => {
      mouseX = e.clientX;
      mouseY = e.clientY;
    });

    let lastPacketTime = 0;

    const animate = (timestamp) => {
      ctx.clearRect(0, 0, width, height);

      // Periodically spawn event communication pulses
      if (timestamp - lastPacketTime > 1900 && packets.length < 6) {
        lastPacketTime = timestamp;
        const from = Math.floor(Math.random() * nodes.length);
        let bestTo = -1;
        let bestDist = Infinity;
        for (let j = 0; j < nodes.length; j++) {
          if (j !== from) {
            const dx = nodes[from].x - nodes[j].x;
            const dy = nodes[from].y - nodes[j].y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 340 && dist < bestDist) {
              bestDist = dist;
              bestTo = j;
            }
          }
        }
        if (bestTo !== -1) {
          spawnPacket(from, bestTo);
        }
      }

      // Update positions with subtle floating harmonic motion
      for (let i = 0; i < nodes.length; i++) {
        const n = nodes[i];
        n.angle += 0.007;
        n.pulse += 0.025;

        const targetX = n.baseX + Math.cos(n.angle) * 20;
        const targetY = n.baseY + Math.sin(n.angle * 0.8) * 16;

        // Gentle interactive mouse repel
        const mdx = n.x - mouseX;
        const mdy = n.y - mouseY;
        const mDist = Math.sqrt(mdx * mdx + mdy * mdy);
        let pushX = 0;
        let pushY = 0;
        if (mDist < 140 && mDist > 0) {
          const force = (140 - mDist) / 140 * 16;
          pushX = (mdx / mDist) * force;
          pushY = (mdy / mDist) * force;
        }

        n.x += ((targetX + pushX) - n.x) * 0.04;
        n.y += ((targetY + pushY) - n.y) * 0.04;
      }

      // Draw dashed connection links between venues
      ctx.lineWidth = 1;
      ctx.setLineDash([3, 4]);

      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const dx = nodes[i].x - nodes[j].x;
          const dy = nodes[i].y - nodes[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < 260) {
            const alpha = (1 - dist / 260) * 0.4;
            ctx.strokeStyle = `rgba(138, 148, 120, ${alpha})`;
            ctx.beginPath();
            ctx.moveTo(nodes[i].x, nodes[i].y);
            ctx.lineTo(nodes[j].x, nodes[j].y);
            ctx.stroke();
          }
        }
      }

      ctx.setLineDash([]);

      // Draw event broadcast pulses
      for (let k = packets.length - 1; k >= 0; k--) {
        const p = packets[k];
        p.progress += p.speed;
        if (p.progress >= 1) {
          packets.splice(k, 1);
          continue;
        }
        const n1 = nodes[p.from];
        const n2 = nodes[p.to];
        const px = n1.x + (n2.x - n1.x) * p.progress;
        const py = n1.y + (n2.y - n1.y) * p.progress;

        ctx.fillStyle = 'rgba(52, 72, 59, 0.7)';
        ctx.beginPath();
        ctx.arc(px, py, 2.2, 0, Math.PI * 2);
        ctx.fill();
      }

      // Draw venue nodes and typography
      for (let i = 0; i < nodes.length; i++) {
        const n = nodes[i];
        const pulseScale = 1 + Math.sin(n.pulse) * 0.12;

        // Outer reticle circle
        ctx.strokeStyle = 'rgba(221, 214, 197, 0.75)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(n.x, n.y, 8 * pulseScale, 0, Math.PI * 2);
        ctx.stroke();

        // Inner solid dot
        ctx.fillStyle = '#34483B';
        ctx.beginPath();
        ctx.arc(n.x, n.y, 2.5, 0, Math.PI * 2);
        ctx.fill();

        // Technical venue code label
        ctx.font = '9px "Courier New", Courier, monospace';
        ctx.fillStyle = 'rgba(119, 117, 104, 0.85)';
        ctx.textAlign = 'left';
        ctx.fillText(`+ ${n.code}: ${n.name}`, n.x + 12, n.y + 3);
      }

      requestAnimationFrame(animate);
    };

    requestAnimationFrame(animate);
  }
}

const app = new App();
