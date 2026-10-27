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

    if (this.currentUser) {
      this.showMainApp();
    } else {
      this.showAuthView();
    }
  }

  // ==================== AUTHENTICATION ====================
  setAuthMode(mode) {
    document.querySelectorAll('.auth-tab-pill').forEach(t => t.classList.remove('active'));
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
        this.showToast(`Welcome back, ${this.currentUser.name}!`, 'success');
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
    document.getElementById('avatarLetter').textContent = this.currentUser.name.charAt(0).toUpperCase();

    // Set dynamic greeting
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : (hour < 18 ? 'Good afternoon' : 'Good evening');
    const greetingEl = document.getElementById('greetingBadge');
    if (greetingEl) greetingEl.textContent = `${greeting}, ${this.currentUser.name.split(' ')[0]} 👋`;

    this.showView('events');
    this.loadEvents();
    if (!isCoordinator) {
      this.loadRegistrations();
    }
  }

  // ==================== NAVIGATION VIEWS ====================
  showView(viewName) {
    document.querySelectorAll('.nav-pill-btn').forEach(l => l.classList.remove('active'));
    document.querySelectorAll('.app-view').forEach(v => v.classList.remove('active'));

    if (viewName === 'events') {
      document.getElementById('navEventsBtn').classList.add('active');
      document.getElementById('viewEvents').classList.add('active');
    } else if (viewName === 'registrations') {
      document.getElementById('navMyRegistrationsBtn').classList.add('active');
      document.getElementById('viewRegistrations').classList.add('active');
      this.loadRegistrations();
    } else if (viewName === 'drafts') {
      document.getElementById('navDraftsBtn').classList.add('active');
      document.getElementById('viewDrafts').classList.add('active');
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
    document.querySelectorAll('#categoryFilters .cat-pill').forEach(pill => {
      pill.classList.toggle('active', pill.textContent.toUpperCase().includes(cat) || (cat === 'ALL' && pill.textContent.includes('All')));
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
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-dim);">
          <div style="font-size: 32px; margin-bottom: 8px;">🔍</div>
          <p style="font-size: 15px; font-weight: 600;">No campus events match your search.</p>
        </div>
      `;
      return;
    }

    const isCoordinator = this.currentUser && this.currentUser.role === 'COORDINATOR';

    container.innerHTML = filtered.map(e => {
      const cat = (e.category || 'TECHNICAL').toUpperCase();
      const catClass = `chip-${cat.toLowerCase()}`;
      const barClass = `bar-${cat.toLowerCase()}`;
      const { month, day } = this.formatDateBlock(e.date);

      const isFull = e.availableSeats <= 0;
      const statusChip = isFull
        ? `<span class="status-chip chip-full">Sold Out</span>`
        : `<span class="status-chip chip-open">Seats Open</span>`;

      const percent = Math.min(100, Math.round(((e.capacity - e.availableSeats) / e.capacity) * 100));

      return `
        <div class="creative-event-card">
          <div class="card-accent-bar ${barClass}"></div>

          <div class="card-top-row">
            <div class="date-pill-block">
              <span class="date-month">${month}</span>
              <span class="date-day">${day}</span>
            </div>

            <div class="card-badges-wrap">
              <span class="category-chip ${catClass}">${e.category}</span>
              ${statusChip}
            </div>
          </div>

          <h3 class="event-headline">${this.escapeHtml(e.name)}</h3>
          <p class="event-synopsis">${this.escapeHtml(e.description || 'Join this exciting campus event.')}</p>

          <div class="event-venue-pill">
            <span class="venue-icon">📍</span>
            <span>${this.escapeHtml(e.venue)}</span>
          </div>

          <div class="capacity-meter-box">
            <div class="capacity-labels">
              <span class="seat-counter"><strong>${e.availableSeats}</strong> of ${e.capacity} seats left</span>
              <span style="font-family: var(--font-mono); font-size: 11px; color: var(--text-dim);">${percent}% full</span>
            </div>
            <div class="meter-track">
              <div class="meter-bar" style="width: ${percent}%;"></div>
            </div>
          </div>

          <div class="card-action-bar">
            ${isCoordinator
              ? `
                <button class="btn btn-sm btn-glass" onclick="app.cloneEventAsDraft(${e.id})" title="Duplicate to staging">
                  🧬 Duplicate
                </button>
                <button class="btn btn-sm btn-glass" onclick="app.openEditModal(${e.id})" title="Edit venue & view revision history">
                  ⏳ Edit &amp; History
                </button>
              `
              : `
                <button class="btn btn-glow btn-block" ${isFull ? 'disabled' : ''} onclick="app.openRegisterModal(${e.id})">
                  ${isFull ? 'Sold Out' : '⚡ Grab Pass'}
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
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-dim);">
          <div style="font-size: 32px; margin-bottom: 8px;">🛠️</div>
          <p style="font-size: 15px; font-weight: 600;">No drafts currently in staging.</p>
          <p style="font-size: 13px; margin-top: 4px;">Click "Duplicate" on any live event or click "+ Create Event" to prepare one.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = drafts.map(e => {
      const { month, day } = this.formatDateBlock(e.date);
      return `
        <div class="creative-event-card" style="border: 1px dashed rgba(168, 85, 247, 0.4);">
          <div class="card-top-row">
            <div class="date-pill-block">
              <span class="date-month">${month}</span>
              <span class="date-day">${day}</span>
            </div>
            <div class="card-badges-wrap">
              <span class="category-chip chip-${e.category.toLowerCase()}">${e.category}</span>
              <span class="status-chip chip-draft">Draft Staging</span>
            </div>
          </div>

          <h3 class="event-headline">${this.escapeHtml(e.name)}</h3>
          <p class="event-synopsis">${this.escapeHtml(e.description || 'Draft configuration')}</p>

          <div class="event-venue-pill">
            <span class="venue-icon">📍</span>
            <span>${this.escapeHtml(e.venue)} &bull; ${e.capacity} seats</span>
          </div>

          <div class="card-action-bar" style="margin-top: auto;">
            <button class="btn btn-glow btn-block" onclick="app.publishDraft(${e.id})">
              🚀 Publish to Campus
            </button>
            <button class="btn btn-glass" onclick="app.openEditModal(${e.id})">
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
          container.innerHTML = `<div style="font-size: 12px; color: var(--text-dim);">No saved checkpoints yet.</div>`;
          return;
        }

        container.innerHTML = history.slice().reverse().map(m => `
          <div class="timeline-checkpoint-card">
            <div>
              <span class="checkpoint-ver">v${m.version}</span>
              <div class="checkpoint-note">${this.escapeHtml(m.changeSummary || 'Checkpoint')}</div>
              <div class="checkpoint-meta">
                📅 ${m.timestamp} &bull; ${m.venue} (${m.capacity} seats) &bull; ${m.author}
              </div>
            </div>
            <button type="button" class="btn btn-sm btn-glass" onclick="app.restoreEventVersion(${eventId}, ${m.version})">
              Restore
            </button>
          </div>
        `).join('');
      }
    } catch (err) {
      container.innerHTML = `<div style="font-size: 12px; color: var(--rose);">Could not load history.</div>`;
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
      tagEl.textContent = 'SCHOLARSHIP FREE PASS';
      feeEl.textContent = '$0.00';
    } else if (tier === 'STANDARD') {
      tagEl.textContent = 'STANDARD VIP DELEGATE';
      const price = this.cachedPricing ? this.cachedPricing.STANDARD.finalFee : 150;
      feeEl.textContent = `$${price.toFixed(2)}`;
    } else {
      tagEl.textContent = 'EARLY BIRD PASS (30% OFF)';
      const price = this.cachedPricing ? this.cachedPricing.EARLY_BIRD.finalFee : 105;
      feeEl.textContent = `$${price.toFixed(2)}`;
    }
  }

  async submitRegistration() {
    if (!this.selectedEventForReg) return;

    const tier = document.querySelector('input[name="ticketTier"]:checked').value;
    const channel = document.getElementById('regNotificationChannel').value;

    const btn = document.getElementById('submitRegBtn');
    btn.disabled = true;
    btn.textContent = 'Issuing Digital Pass...';

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
      btn.textContent = 'Confirm & Generate Pass →';

      if (res.ok && data.success) {
        this.showToast(`Pass issued! Status: ${data.status}`, 'success');
        this.closeModal('registerModal');
        await this.loadEvents();
        await this.loadRegistrations();
        this.showView('registrations');
      } else {
        this.showToast(data.message || 'Registration could not be completed', 'error');
      }
    } catch (err) {
      btn.disabled = false;
      btn.textContent = 'Confirm & Generate Pass →';
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
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-dim);">
          <div style="font-size: 32px; margin-bottom: 8px;">🎟️</div>
          <p style="font-size: 15px; font-weight: 600;">You don't have any event passes yet.</p>
          <button class="btn btn-glow" style="margin-top: 14px;" onclick="app.showView('events')">
            Browse &amp; Grab Pass
          </button>
        </div>
      `;
      return;
    }

    container.innerHTML = this.registrations.map(r => {
      const status = (r.status || 'PENDING').toUpperCase();
      const statusClass = status === 'APPROVED' ? 'status-approved' : (status === 'REJECTED' ? 'status-rejected' : 'status-pending');

      return `
        <div class="boarding-pass-card">
          <div class="pass-main-stub">
            <div class="pass-meta-top">
              <span class="category-chip chip-${(r.category || 'technical').toLowerCase()}">${r.category}</span>
              <span class="pass-id-badge">PASS #${r.registrationId}</span>
            </div>

            <h3 class="pass-event-title">${this.escapeHtml(r.eventName)}</h3>

            <div class="pass-details-row">
              <span>👤 ${this.escapeHtml(this.currentUser.name)}</span>
              <span>🎓 ${this.currentUser.department}</span>
            </div>
          </div>

          <div class="pass-divider-notch">
            <span class="pass-notch-circle top"></span>
            <span class="pass-notch-circle bottom"></span>
          </div>

          <div class="pass-side-stub">
            <span class="pass-status-pill ${statusClass}">${status}</span>
            <button class="btn btn-sm btn-ghost" onclick="app.cancelRegistration(${r.registrationId})" style="color: var(--rose); font-size: 11px;">
              Cancel Pass
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
    toast.className = `creative-toast toast-${type}`;
    const icon = type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ');
    toast.innerHTML = `<span style="font-weight: bold;">${icon}</span> <span>${this.escapeHtml(msg)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(12px) scale(0.96)';
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
}

const app = new App();
