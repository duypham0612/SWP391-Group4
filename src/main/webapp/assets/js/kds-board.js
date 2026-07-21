(function () {
  'use strict';

  var board = document.getElementById('kdsBoard');
  var connection = document.getElementById('kdsConnection');
  if (!board || !connection) return;

  var endpoint = board.dataset.endpoint;
  var FILTER_KEY = 'kdsFiltersV2';
  var ALERT_KEY = 'kdsAlertOpen';
  var DEFAULT_FILTERS = { owner: 'all', station: 'all', orderType: 'all' };
  var BLOCKING_REASONS = ['EQUIPMENT', 'DISCONTINUED'];
  var filters = readFilters();
  var refreshing = false;
  var suppressUntil = 0;
  var noticeTimer;
  var modalReturnFocus = null;
  var known = readIds();
  var signatures = readSignatures();

  function storageGet(key) {
    try { return localStorage.getItem(key); } catch (ignore) { return null; }
  }

  function storageSet(key, value) {
    try { localStorage.setItem(key, value); } catch (ignore) { /* private browsing */ }
  }

  function validChoice(value, choices, fallback) {
    return choices.indexOf(value) >= 0 ? value : fallback;
  }

  function readFilters() {
    var saved = storageGet(FILTER_KEY);
    if (saved) {
      try {
        var parsed = JSON.parse(saved);
        return {
          owner: validChoice(parsed.owner, ['all', 'mine', 'unassigned'], 'all'),
          station: validChoice(parsed.station, ['all', 'COFFEE', 'TEA', 'BLENDER'], 'all'),
          orderType: validChoice(parsed.orderType, ['all', 'DINE_IN', 'TAKEAWAY', 'DELIVERY'], 'all')
        };
      } catch (ignore) { /* migrate the former single filter below */ }
    }

    var migrated = Object.assign({}, DEFAULT_FILTERS);
    var old = storageGet('kdsFilter');
    if (old === 'mine' || old === 'unassigned') migrated.owner = old;
    else if (old && old.indexOf('station:') === 0) migrated.station = validChoice(old.slice(8), ['COFFEE', 'TEA', 'BLENDER'], 'all');
    else if (old && old.indexOf('type:') === 0) migrated.orderType = validChoice(old.slice(5), ['DINE_IN', 'TAKEAWAY', 'DELIVERY'], 'all');
    return migrated;
  }

  function saveFilters() {
    storageSet(FILTER_KEY, JSON.stringify(filters));
  }

  function readIds() {
    var out = {};
    board.querySelectorAll('[data-kds-item-id]').forEach(function (card) {
      out[card.dataset.kdsItemId] = true;
    });
    return out;
  }

  function signature(card) {
    var copy = card.cloneNode(true);
    copy.querySelectorAll('.kds-clock,.kds-meta-row,.kds-ready-facts').forEach(function (node) {
      node.remove();
    });
    return copy.textContent.replace(/\s+/g, ' ').trim();
  }

  function readSignatures() {
    var out = {};
    board.querySelectorAll('[data-kds-item-id]').forEach(function (card) {
      out[card.dataset.kdsItemId] = signature(card);
    });
    return out;
  }

  function notice(message) {
    var live = document.getElementById('kdsLiveNotice');
    if (!live) return;
    live.textContent = message;
    live.hidden = false;
    clearTimeout(noticeTimer);
    noticeTimer = setTimeout(function () { live.hidden = true; }, 5000);
  }

  function markChanges() {
    var added = 0;
    var priority = 0;
    var changed = 0;
    var removed = 0;
    var next = readIds();

    board.querySelectorAll('[data-kds-item-id]').forEach(function (card) {
      var id = card.dataset.kdsItemId;
      if (!known[id]) {
        card.classList.add('kds-new');
        added += 1;
        if (card.dataset.priority === 'true') priority += 1;
      } else if (signatures[id] && signatures[id] !== signature(card)) {
        changed += 1;
      }
    });
    Object.keys(known).forEach(function (id) { if (!next[id]) removed += 1; });

    if (priority) notice('Có ' + priority + ' món làm lại ưu tiên.');
    else if (added) notice('Có ' + added + ' món mới.');
    else if (removed) notice('Một món đã chuyển trạng thái hoặc đơn vừa thay đổi.');
    else if (changed) notice('Ghi chú hoặc thông tin món vừa được cập nhật.');

    known = next;
    signatures = readSignatures();
  }

  function setConnection(ok, syncing) {
    connection.classList.toggle('is-offline', !ok);
    connection.classList.toggle('is-syncing', !!syncing);
    connection.querySelector('span:last-child').textContent = ok
      ? (syncing ? 'Đang cập nhật' : 'Đã đồng bộ')
      : 'Mất kết nối — đang giữ dữ liệu gần nhất';
  }

  function syncFilterControls() {
    document.querySelectorAll('[data-filter-group="owner"]').forEach(function (button) {
      var selected = button.dataset.filterValue === filters.owner;
      button.classList.toggle('is-active', selected);
      button.setAttribute('aria-pressed', String(selected));
    });

    document.getElementById('kdsStationFilter').value = filters.station;
    document.getElementById('kdsOrderTypeFilter').value = filters.orderType;

    var extraCount = (filters.station === 'all' ? 0 : 1) + (filters.orderType === 'all' ? 0 : 1);
    var more = document.getElementById('kdsMoreFilters');
    var badge = document.getElementById('kdsFilterBadge');
    more.classList.toggle('has-active', extraCount > 0);
    badge.hidden = extraCount === 0;
    badge.textContent = String(extraCount);
  }

  function cardMatches(card) {
    var ownerMatches = filters.owner === 'all' ||
      (filters.owner === 'mine' && card.dataset.owner === board.dataset.userId) ||
      (filters.owner === 'unassigned' && card.dataset.owner === 'unassigned');
    var stationMatches = filters.station === 'all' || card.dataset.station === filters.station;
    var typeMatches = filters.orderType === 'all' || card.dataset.orderType === filters.orderType;
    return ownerMatches && stationMatches && typeMatches;
  }

  function applyFilters() {
    board.querySelectorAll('.kds-groups [data-kds-item-id]').forEach(function (card) {
      card.hidden = !cardMatches(card);
    });

    var anyVisible = false;
    board.querySelectorAll('.kds-group').forEach(function (group) {
      var groupHasVisibleCard = Array.prototype.some.call(
        group.querySelectorAll('[data-kds-item-id]'), function (card) { return !card.hidden; });
      group.hidden = !groupHasVisibleCard;
      if (groupHasVisibleCard) anyVisible = true;
    });
    var filterEmpty = board.querySelector('.kds-groups > .kds-filter-empty');
    if (filterEmpty) filterEmpty.hidden = anyVisible || !board.querySelector('.kds-group');
    syncFilterControls();
  }

  function resetFilters() {
    filters = Object.assign({}, DEFAULT_FILTERS);
    saveFilters();
    applyFilters();
  }

  function itemById(id) {
    var found = null;
    board.querySelectorAll('[data-kds-item-id]').forEach(function (card) {
      if (!found && card.dataset.kdsItemId === id) found = card;
    });
    return found;
  }

  function focusDescriptor(element) {
    if (!element || !board.contains(element)) return null;
    var card = element.closest('[data-kds-item-id]');
    if (!card) return null;
    if (element === card) return { itemId: card.dataset.kdsItemId, card: true };
    return {
      itemId: card.dataset.kdsItemId,
      tag: element.tagName,
      name: element.getAttribute('name') || '',
      action: element.closest('form') ? (element.closest('form').querySelector('[name="action"]') || {}).value : '',
      text: element.textContent.trim()
    };
  }

  function captureViewState() {
    var groups = board.querySelector('.kds-groups');
    var state = { groupScroll: groups ? groups.scrollTop : 0, handoffs: {}, menus: {}, focus: focusDescriptor(document.activeElement) };
    board.querySelectorAll('[data-kds-item-id]').forEach(function (card) {
      var select = card.querySelector('[name="handoverLocation"]');
      if (select && select.value) state.handoffs[card.dataset.kdsItemId] = select.value;
      var openMenus = [];
      card.querySelectorAll('.kds-card-menu').forEach(function (menu, index) { if (menu.open) openMenus.push(index); });
      if (openMenus.length) state.menus[card.dataset.kdsItemId] = openMenus;
    });
    var alerts = document.getElementById('kdsAlertDrawer');
    state.alertOpen = alerts ? alerts.open : storageGet(ALERT_KEY) === '1';
    return state;
  }

  function restoreFocus(descriptor) {
    if (!descriptor) return;
    var target = null;
    var card = itemById(descriptor.itemId);
    if (!card || card.hidden) return;
    if (descriptor.card) target = card;
    else {
      card.querySelectorAll(descriptor.tag || '*').forEach(function (candidate) {
        if (target) return;
        var actionInput = candidate.closest('form') && candidate.closest('form').querySelector('[name="action"]');
        var action = actionInput ? actionInput.value : '';
        if ((candidate.getAttribute('name') || '') === descriptor.name &&
            action === descriptor.action && candidate.textContent.trim() === descriptor.text) target = candidate;
      });
    }
    if (target && !target.disabled) {
      try { target.focus({ preventScroll: true }); } catch (ignore) { target.focus(); }
    }
  }

  function restoreViewState(state) {
    if (!state) return;
    Object.keys(state.handoffs).forEach(function (id) {
      var card = itemById(id);
      var select = card && card.querySelector('[name="handoverLocation"]');
      if (select) select.value = state.handoffs[id];
    });
    Object.keys(state.menus).forEach(function (id) {
      var card = itemById(id);
      if (!card) return;
      var menus = card.querySelectorAll('.kds-card-menu');
      state.menus[id].forEach(function (index) { if (menus[index]) menus[index].open = true; });
    });
    var alerts = document.getElementById('kdsAlertDrawer');
    if (alerts) alerts.open = !!state.alertOpen;
    var groups = board.querySelector('.kds-groups');
    if (groups) groups.scrollTop = state.groupScroll || 0;
    restoreFocus(state.focus);
  }

  function replaceBoard(html, state) {
    board.innerHTML = html;
    markChanges();
    applyFilters();
    restoreViewState(state);
  }

  function openModal(id, trigger) {
    var modal = document.getElementById(id);
    modalReturnFocus = trigger;
    modal.querySelector('[data-item-input]').value = trigger.dataset.itemId;
    modal.querySelector('[data-modal-name]').textContent = trigger.dataset.name;
    modal.dataset.productId = trigger.dataset.productId || '';
    modal.hidden = false;
    document.body.classList.add('kds-modal-open');
    var first = modal.querySelector('select,input:not([type="hidden"]),button:not([disabled])');
    if (first) first.focus();
  }

  function closeModal(modal, restoreTrigger) {
    if (!modal) return;
    modal.hidden = true;
    var form = modal.querySelector('form');
    form.reset();
    delete form.dataset.busy;
    form.querySelectorAll('button').forEach(function (button) {
      button.disabled = false;
      button.classList.remove('is-loading');
    });
    var other = modal.querySelector('.js-other-reason');
    if (other) {
      other.hidden = true;
      other.querySelector('input').required = false;
    }
    var ingredients = modal.querySelector('.js-ingredients');
    var recount = modal.querySelector('.js-recount');
    var note = modal.querySelector('.js-blocking-note');
    if (ingredients) {
      ingredients.hidden = true;
      ingredients.querySelector('[data-ingredient-slot]').innerHTML = '';
    }
    if (recount) recount.querySelector('[data-recount-slot]').innerHTML = '';
    if (note) note.hidden = true;
    if (!document.querySelector('.kds-modal:not([hidden])')) document.body.classList.remove('kds-modal-open');
    if (restoreTrigger && modalReturnFocus && document.contains(modalReturnFocus)) modalReturnFocus.focus();
    modalReturnFocus = null;
  }

  async function loadIngredients(select) {
    var modal = document.getElementById('issueModal');
    var value = select.value;
    var other = modal.querySelector('.js-other-reason');
    var ingredients = modal.querySelector('.js-ingredients');
    var slot = ingredients.querySelector('[data-ingredient-slot]');
    var note = modal.querySelector('.js-blocking-note');
    other.hidden = value !== 'OTHER';
    other.querySelector('input').required = value === 'OTHER';
    note.hidden = BLOCKING_REASONS.indexOf(value) < 0;
    if (value !== 'OUT_OF_STOCK') {
      ingredients.hidden = true;
      slot.innerHTML = '';
      return;
    }
    ingredients.hidden = false;
    slot.textContent = 'Đang tải nguyên liệu…';
    try {
      var response = await fetch(endpoint + '?partial=recipe&productId=' + encodeURIComponent(modal.dataset.productId), { credentials: 'same-origin' });
      if (!response.ok) throw new Error('recipe');
      slot.innerHTML = await response.text();
    } catch (ignore) {
      slot.textContent = 'Không tải được danh sách nguyên liệu. Vui lòng thử lại.';
    }
  }

  async function loadRecount(modal) {
    var slot = modal.querySelector('[data-recount-slot]');
    slot.textContent = 'Đang tải nguyên liệu…';
    try {
      var response = await fetch(endpoint + '?partial=depleted&productId=' + encodeURIComponent(modal.dataset.productId), { credentials: 'same-origin' });
      if (!response.ok) throw new Error('depleted');
      slot.innerHTML = await response.text();
    } catch (ignore) {
      slot.textContent = 'Không tải được danh sách nguyên liệu. Vui lòng thử lại.';
    }
  }

  function openUnblockModal(trigger) {
    openModal('unblockModal', trigger);
    loadRecount(document.getElementById('unblockModal'));
  }

  async function postForm(form) {
    if (form.dataset.busy === '1') return;
    form.dataset.busy = '1';
    form.querySelectorAll('button').forEach(function (button) {
      button.disabled = true;
      button.classList.add('is-loading');
    });
    suppressUntil = Date.now() + 1800;
    board.setAttribute('aria-busy', 'true');
    var state = captureViewState();
    var body = new FormData(form);
    body.append('ajax', '1');
    try {
      var response = await fetch(form.action, {
        method: 'POST', body: body, credentials: 'same-origin',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (!response.ok) throw new Error('post');
      replaceBoard(await response.text(), state);
      setConnection(true, false);
      var modal = form.closest('.kds-modal');
      if (modal) closeModal(modal, false);
    } catch (ignore) {
      setConnection(false, false);
      HTMLFormElement.prototype.submit.call(form);
    } finally {
      board.setAttribute('aria-busy', 'false');
    }
  }

  function interactionInProgress() {
    if (document.querySelector('.kds-modal:not([hidden])')) return true;
    if (board.querySelector('form[data-busy="1"]')) return true;
    var active = document.activeElement;
    return board.contains(active) && /^(INPUT|SELECT|TEXTAREA)$/.test(active.tagName);
  }

  async function refresh() {
    if (refreshing || document.visibilityState === 'hidden' || Date.now() < suppressUntil || interactionInProgress()) return;
    refreshing = true;
    board.setAttribute('aria-busy', 'true');
    setConnection(true, true);
    var state = captureViewState();
    try {
      var response = await fetch(endpoint + '?partial=1', { credentials: 'same-origin' });
      if (!response.ok) throw new Error('refresh');
      replaceBoard(await response.text(), state);
      setConnection(true, false);
    } catch (ignore) {
      setConnection(false, false);
    } finally {
      refreshing = false;
      board.setAttribute('aria-busy', 'false');
    }
  }

  document.addEventListener('click', function (event) {
    var owner = event.target.closest('[data-filter-group="owner"]');
    var issue = event.target.closest('.js-issue');
    var remake = event.target.closest('.js-remake');
    var unblock = event.target.closest('.js-unblock');
    var close = event.target.closest('[data-close]');

    if (owner) {
      filters.owner = owner.dataset.filterValue;
      saveFilters();
      applyFilters();
    } else if (issue) {
      openModal('issueModal', issue);
    } else if (remake) {
      openModal('remakeModal', remake);
    } else if (unblock) {
      event.preventDefault();
      openUnblockModal(unblock);
    } else if (close) {
      closeModal(close.closest('.kds-modal'), true);
    }
  });

  document.addEventListener('change', function (event) {
    var filterSelect = event.target.closest('[data-filter-select]');
    if (filterSelect) {
      filters[filterSelect.dataset.filterSelect] = filterSelect.value;
      saveFilters();
      applyFilters();
      return;
    }
    if (event.target.matches('#issueModal select[name="reason"]')) loadIngredients(event.target);
  });

  document.getElementById('kdsClearFilters').addEventListener('click', resetFilters);

  document.addEventListener('submit', function (event) {
    var form = event.target;
    if (!form.matches('#kdsBoard form,.kds-modal form')) return;
    event.preventDefault();
    var message = form.dataset.confirm;
    if (message && !window.confirm(message)) return;
    postForm(form);
  });

  document.addEventListener('keydown', function (event) {
    var visibleModal = document.querySelector('.kds-modal:not([hidden])');
    if (visibleModal && event.key === 'Tab') {
      var focusable = Array.prototype.slice.call(visibleModal.querySelectorAll('button:not([disabled]),select:not([disabled]),input:not([disabled]),textarea:not([disabled]),a[href]'));
      if (focusable.length) {
        var first = focusable[0];
        var last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    }
    if (event.key === 'Escape') {
      document.querySelectorAll('.kds-modal:not([hidden])').forEach(function (modal) { closeModal(modal, true); });
      return;
    }
  });

  board.addEventListener('toggle', function (event) {
    if (event.target.id === 'kdsAlertDrawer') storageSet(ALERT_KEY, event.target.open ? '1' : '0');
  }, true);

  try { localStorage.removeItem('kdsCompact'); } catch (ignore) { /* obsolete preference */ }
  saveFilters();
  syncFilterControls();
  var initialAlert = document.getElementById('kdsAlertDrawer');
  if (initialAlert) initialAlert.open = storageGet(ALERT_KEY) === '1';
  applyFilters();
  setConnection(navigator.onLine, false);

  setInterval(refresh, 5000);
  window.addEventListener('online', refresh);
  window.addEventListener('offline', function () { setConnection(false, false); });
})();
