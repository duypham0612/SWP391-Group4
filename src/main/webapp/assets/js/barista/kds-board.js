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
  var queuedJump = null;      // lần đổi trang/bộ lọc bị dồn lại vì đang có yêu cầu chạy dở
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

  /* Bộ lọc nằm trên URL (mở link pager khi JS hỏng, hoặc bookmark) là nguồn đúng nhất —
     nó chính là bộ lọc máy chủ vừa dùng để dựng trang này. */
  function urlFilters() {
    var params = new URLSearchParams(window.location.search);
    if (!params.has('owner') && !params.has('station') && !params.has('orderType')) return null;
    return {
      owner: validChoice(params.get('owner'), ['all', 'mine', 'unassigned'], 'all'),
      station: validChoice(params.get('station'), ['all', 'COFFEE', 'TEA', 'BLENDER'], 'all'),
      orderType: validChoice(params.get('orderType'), ['all', 'DINE_IN', 'TAKEAWAY', 'DELIVERY'], 'all')
    };
  }

  function readFilters() {
    var fromUrl = urlFilters();
    if (fromUrl) return fromUrl;
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

    // Lọc "quá giờ" đã bỏ cùng với mọi số liệu thời gian trên màn — giá trị cũ rơi về mặc định.
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

  /* Chữ ký nội dung một dòng, ĐÃ bỏ mọi thứ tự đổi theo đồng hồ: tên người pha và số thứ tự pha
     (số này nhảy mỗi khi hàng chờ xê dịch). Không bỏ thì mỗi lần làm mới đều báo "vừa cập nhật"
     dù món không có gì đổi, và barista sẽ học cách phớt lờ thông báo. */
  function signature(card) {
    var copy = card.cloneNode(true);
    copy.querySelectorAll('.kds-sla,.kds-clock,.kds-meta-row,.kds-ready-facts,.kds-qrow__by,.kds-qrow__seq').forEach(function (node) {
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

  /* Trang và bộ lọc mà máy chủ vừa áp — đọc lại từ board sau mỗi lần thay HTML.
     Lọc và cắt trang đều nằm ở backend, trình duyệt chỉ còn việc hỏi đúng trang. */
  function queueState() {
    var node = document.getElementById('kdsQueueState');
    if (!node) return { page: 1, totalPages: 1, owner: 'all', station: 'all', orderType: 'all' };
    return {
      page: parseInt(node.dataset.page, 10) || 1,
      totalPages: parseInt(node.dataset.totalPages, 10) || 1,
      owner: node.dataset.owner || 'all',
      station: node.dataset.station || 'all',
      orderType: node.dataset.orderType || 'all'
    };
  }

  function boardQuery(page) {
    return 'partial=1&page=' + encodeURIComponent(page) +
      '&owner=' + encodeURIComponent(filters.owner) +
      '&station=' + encodeURIComponent(filters.station) +
      '&orderType=' + encodeURIComponent(filters.orderType);
  }

  /* Đổi bộ lọc thì luôn về trang 1: giữ nguyên số trang cũ dễ rơi vào trang trống
     khi tập kết quả co lại. */
  function applyFilterChange() {
    saveFilters();
    syncFilterControls();
    refresh(true, 1);
  }

  function resetFilters() {
    filters = Object.assign({}, DEFAULT_FILTERS);
    applyFilterChange();
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
    var state = { handoffs: {}, menus: {}, focus: focusDescriptor(document.activeElement) };
    // Hàng chờ dài thì phải giữ chỗ đang xem qua mỗi lần làm mới, không thì trang nhảy về đầu
    // và barista mất vị trí đang đọc. Trên màn quầy, vùng cuộn là chính danh sách (khung cố
    // định cao 100dvh) nên phải nhớ cả scrollTop của nó, không chỉ scroll của cửa sổ.
    state.pageScroll = window.scrollY || 0;
    var queue = document.getElementById('kdsQueue');
    state.queueScroll = queue ? queue.scrollTop : 0;
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
    if (target && !target.disabled && !target.hidden) {
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
    if (state.pageScroll) window.scrollTo(0, state.pageScroll);
    var queue = document.getElementById('kdsQueue');
    if (queue && state.queueScroll) queue.scrollTop = state.queueScroll;
    restoreFocus(state.focus);
  }

  /* silent = vừa đổi trang/bộ lọc: cả danh sách là món khác, so với lần trước sẽ báo nhầm
     "có N món mới". Chỉ ghi nhận lại mốc so sánh, không thông báo. */
  function replaceBoard(html, state, silent) {
    board.innerHTML = html;
    if (silent) {
      known = readIds();
      signatures = readSignatures();
    } else {
      markChanges();
    }
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
    // Đổi trang/bộ lọc trong lúc chờ thao tác phải XẾP HÀNG, không chạy chồng: hai phản hồi
    // cùng thay board thì cái về sau thắng, barista bấm sang trang 3 lại thấy trang cũ.
    refreshing = true;
    board.setAttribute('aria-busy', 'true');
    var state = captureViewState();
    var body = new FormData(form);
    body.append('ajax', '1');
    // Thao tác xong phải quay lại ĐÚNG trang và bộ lọc đang xem, không nhảy về trang 1.
    body.append('page', String(queueState().page));
    body.append('owner', filters.owner);
    body.append('station', filters.station);
    body.append('orderType', filters.orderType);
    try {
      var response = await fetch(form.action, {
        method: 'POST', body: body, credentials: 'same-origin',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (response.status === 403 && response.headers.get('X-Barista-Write-Denied') === 'off-shift') {
        var denied = await response.json();
        notice(denied.error || 'Bạn đang ngoài ca — cần vào ca trước khi thao tác.');
        window.setTimeout(function () { window.location.assign(endpoint); }, 700);
        return;
      }
      if (response.status === 400 && response.headers.get('X-Barista-Write-Denied') === 'invalid-action') {
        var invalid = await response.json();
        notice(invalid.error || 'Thao tác không hợp lệ. Vui lòng tải lại màn hình.');
        setConnection(true, false);
        return;
      }
      if (!response.ok) throw new Error('post');
      replaceBoard(await response.text(), state);
      setConnection(true, false);
      var modal = form.closest('.kds-modal');
      if (modal) closeModal(modal, false);
    } catch (ignore) {
      setConnection(false, false);
      HTMLFormElement.prototype.submit.call(form);
    } finally {
      refreshing = false;
      board.setAttribute('aria-busy', 'false');
      drainQueuedJump();
    }
  }

  function interactionInProgress() {
    if (document.querySelector('.kds-modal:not([hidden])')) return true;
    if (board.querySelector('form[data-busy="1"]')) return true;
    var active = document.activeElement;
    return board.contains(active) && /^(INPUT|SELECT|TEXTAREA)$/.test(active.tagName);
  }

  // force = người dùng bấm "Làm mới": bỏ qua các chốt vốn chỉ để chặn làm mới NGẦM
  // (tab ẩn, vừa submit xong, đang thao tác dở) — bấm tay thì luôn phải chạy, nếu không
  // nút sẽ im lặng không làm gì và trông như hỏng.
  // jumpTo = số trang cần nhảy tới (đổi trang / đổi bộ lọc); bỏ trống = giữ nguyên trang đang xem.
  async function refresh(force, jumpTo) {
    // Bấm liên tiếp (đổi bộ lọc rồi sang trang) không được rơi mất: lần bấm sau cùng được xếp lại
    // và chạy ngay khi yêu cầu đang dở kết thúc. Bỏ qua luôn thì chip bộ lọc đã đổi mà danh sách
    // vẫn là kết quả cũ — barista tưởng đã lọc rồi trong khi chưa.
    if (refreshing) {
      if (force) queuedJump = { to: jumpTo };
      return;
    }
    if (!force && (document.visibilityState === 'hidden' || Date.now() < suppressUntil || interactionInProgress())) return;
    refreshing = true;
    board.setAttribute('aria-busy', 'true');
    setConnection(true, true);
    var state = captureViewState();
    // Sang trang khác thì đọc từ đầu danh sách, giữ lại vị trí cuộn cũ là vô nghĩa.
    if (jumpTo) { state.pageScroll = 0; state.queueScroll = 0; }
    try {
      var response = await fetch(endpoint + '?' + boardQuery(jumpTo || queueState().page),
        { credentials: 'same-origin' });
      if (!response.ok) throw new Error('refresh');
      replaceBoard(await response.text(), state, !!jumpTo);
      setConnection(true, false);
    } catch (ignore) {
      setConnection(false, false);
    } finally {
      refreshing = false;
      board.setAttribute('aria-busy', 'false');
      drainQueuedJump();
    }
  }

  function drainQueuedJump() {
    var queued = queuedJump;
    queuedJump = null;
    if (queued) refresh(true, queued.to);
  }

  document.addEventListener('click', function (event) {
    var owner = event.target.closest('[data-filter-group="owner"]');
    var pageLink = event.target.closest('#kdsBoard .pagination .page[data-page]');
    var issue = event.target.closest('.js-issue');
    var remake = event.target.closest('.js-remake');
    var unblock = event.target.closest('.js-unblock');
    var close = event.target.closest('[data-close]');

    if (pageLink) {
      // Link thật (chạy được khi JS hỏng) nhưng ở đây nạp bằng AJAX để không mất trạng thái board.
      event.preventDefault();
      if (pageLink.getAttribute('aria-disabled') === 'true' || pageLink.classList.contains('is-active')) return;
      refresh(true, parseInt(pageLink.dataset.page, 10) || 1);
    } else if (owner) {
      filters.owner = owner.dataset.filterValue;
      applyFilterChange();
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
      applyFilterChange();
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
    // Mũi tên lên/xuống chạy dọc hàng chờ (chỉ giữa các dòng đang hiện) — quầy hay dùng bàn phím
    // rời, không phải lúc nào cũng chạm được màn hình.
    var row = event.target.closest && event.target.closest('[data-kds-item-id]');
    if (!row || event.target !== row || ['ArrowUp', 'ArrowDown'].indexOf(event.key) < 0) return;
    event.preventDefault();
    var rows = [];
    board.querySelectorAll('[data-kds-item-id]').forEach(function (r) { if (!r.hidden) rows.push(r); });
    if (!rows.length) return;
    var next = rows.indexOf(row) + (event.key === 'ArrowDown' ? 1 : -1);
    rows[(next + rows.length) % rows.length].focus();
  });

  board.addEventListener('toggle', function (event) {
    if (event.target.id === 'kdsAlertDrawer') storageSet(ALERT_KEY, event.target.open ? '1' : '0');
  }, true);

  try { localStorage.removeItem('kdsCompact'); } catch (ignore) { /* obsolete preference */ }
  saveFilters();
  syncFilterControls();
  var initialAlert = document.getElementById('kdsAlertDrawer');
  if (initialAlert) initialAlert.open = storageGet(ALERT_KEY) === '1';
  setConnection(navigator.onLine, false);

  // Vào màn từ menu (URL trống) thì máy chủ chưa biết bộ lọc đang lưu ở trình duyệt và dựng
  // trang chưa lọc. Lệch thì nạp lại đúng một lần để phân trang đếm trên tập đã lọc.
  var applied = queueState();
  if (applied.owner !== filters.owner || applied.station !== filters.station ||
      applied.orderType !== filters.orderType) {
    refresh(true, 1);
  }

  // Không còn tự động làm mới theo chu kỳ — barista chủ động bấm "Làm mới".
  // Bảng vẫn tự cập nhật sau mỗi thao tác (postForm trả về HTML board mới) và khi có mạng lại.
  var refreshButton = document.getElementById('kdsRefresh');
  if (refreshButton) refreshButton.addEventListener('click', function () { refresh(true); });
  window.addEventListener('online', function () { refresh(true); });
  window.addEventListener('offline', function () { setConnection(false, false); });
})();
