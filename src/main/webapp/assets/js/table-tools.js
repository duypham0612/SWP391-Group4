(function () {
  'use strict';

  var DEFAULT_PAGE_SIZE = 10;

  function normalizeText(value) {
    return String(value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'd')
      .toLowerCase()
      .trim();
  }

  function cellValue(row, colIndex) {
    var cell = row.cells[colIndex];
    if (!cell) return '';
    return cell.getAttribute('data-tt-val') || cell.textContent || '';
  }

  function getPageSize(root) {
    var sizeControl = root.querySelector('[data-tt-size]');
    var parsed = sizeControl ? parseInt(sizeControl.value, 10) : DEFAULT_PAGE_SIZE;
    return parsed > 0 ? parsed : DEFAULT_PAGE_SIZE;
  }

  function pageButton(label, page, disabled, active) {
    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'page';
    button.textContent = label;
    button.disabled = !!disabled;
    if (active) button.classList.add('is-active');
    if (page) button.setAttribute('data-tt-page', String(page));
    return button;
  }

  function populateFilterOptions(filters, rows) {
    filters.forEach(function (filter) {
      if (!filter.hasAttribute('data-tt-autofill')) return;

      var colIndex = parseInt(filter.getAttribute('data-tt-col'), 10);
      var known = {};
      Array.prototype.slice.call(filter.options).forEach(function (option) {
        known[normalizeText(option.value)] = true;
      });

      rows.forEach(function (row) {
        var cell = row.cells[colIndex];
        if (!cell) return;
        var rawValue = cell.getAttribute('data-tt-val') || cell.textContent || '';
        var value = String(rawValue).trim();
        var key = normalizeText(value);
        if (!key || known[key]) return;

        var option = document.createElement('option');
        option.value = value;
        option.textContent = String(cell.textContent || value).trim();
        filter.appendChild(option);
        known[key] = true;
      });
    });
  }

  function pagerPages(totalPages, currentPage) {
    var pages = [];
    var start = Math.max(1, currentPage - 2);
    var end = Math.min(totalPages, currentPage + 2);

    if (end - start < 4) {
      start = Math.max(1, Math.min(start, end - 4));
      end = Math.min(totalPages, Math.max(end, start + 4));
    }

    for (var i = start; i <= end; i += 1) {
      pages.push(i);
    }
    return pages;
  }

  function enhanceTable(root) {
    var table = root.querySelector('table');
    if (!table || !table.tBodies.length) return;

    var tbody = table.tBodies[0];
    var rows = Array.prototype.slice.call(tbody.rows);
    if (!rows.length) return;

    var searchInput = root.querySelector('[data-tt-search]');
    var filters = Array.prototype.slice.call(root.querySelectorAll('[data-tt-filter][data-tt-col]'));
    var pager = root.querySelector('[data-tt-pager]');
    var sizeControl = root.querySelector('[data-tt-size]');
    var summary = root.querySelector('[data-tt-summary]');
    var headerCells = Array.prototype.slice.call(table.tHead ? table.tHead.rows[0].cells : []);
    var markedSearchCols = [];
    var defaultSearchCols = [];
    var currentPage = 1;

    populateFilterOptions(filters, rows);

    headerCells.forEach(function (th, index) {
      if (th.hasAttribute('data-tt-search')) markedSearchCols.push(index);
      if (!th.hasAttribute('data-tt-nosearch')) defaultSearchCols.push(index);
    });

    var searchCols = markedSearchCols.length ? markedSearchCols : defaultSearchCols;
    var emptyRow = document.createElement('tr');
    var emptyCell = document.createElement('td');
    emptyRow.className = 'tt-empty';
    emptyRow.hidden = true;
    emptyCell.colSpan = Math.max(1, headerCells.length);
    emptyCell.textContent = root.getAttribute('data-tt-empty') || 'Không có kết quả';
    emptyRow.appendChild(emptyCell);
    tbody.appendChild(emptyRow);

    function matchesSearch(row, query) {
      if (!query) return true;
      return searchCols.some(function (colIndex) {
        return normalizeText(cellValue(row, colIndex)).indexOf(query) !== -1;
      });
    }

    function matchesFilters(row) {
      return filters.every(function (filter) {
        var expected = normalizeText(filter.value);
        if (!expected) return true;
        var actual = normalizeText(cellValue(row, parseInt(filter.getAttribute('data-tt-col'), 10)));
        return actual === expected;
      });
    }

    function renderPager(totalPages) {
      if (!pager) return;
      pager.innerHTML = '';
      if (totalPages <= 1) return;

      pager.appendChild(pageButton('«', 1, currentPage === 1, false));
      pager.appendChild(pageButton('‹', Math.max(1, currentPage - 1), currentPage === 1, false));

      pagerPages(totalPages, currentPage).forEach(function (page) {
        pager.appendChild(pageButton(String(page), page, false, page === currentPage));
      });

      pager.appendChild(pageButton('›', Math.min(totalPages, currentPage + 1), currentPage === totalPages, false));
      pager.appendChild(pageButton('»', totalPages, currentPage === totalPages, false));
    }

    function renderSummary(start, end, total) {
      if (!summary) return;
      summary.textContent = total ? start + '-' + end + ' / ' + total : '0 / 0';
    }

    function update() {
      var query = normalizeText(searchInput ? searchInput.value : '');
      var pageSize = getPageSize(root);
      var filteredRows = rows.filter(function (row) {
        return matchesSearch(row, query) && matchesFilters(row);
      });
      var total = filteredRows.length;
      var totalPages = Math.max(1, Math.ceil(total / pageSize));

      currentPage = Math.min(currentPage, totalPages);

      var startIndex = (currentPage - 1) * pageSize;
      var endIndex = Math.min(startIndex + pageSize, total);
      var pageRows = filteredRows.slice(startIndex, endIndex);

      rows.forEach(function (row) {
        row.hidden = pageRows.indexOf(row) === -1;
      });

      emptyRow.hidden = total !== 0;
      renderPager(totalPages);
      renderSummary(total ? startIndex + 1 : 0, endIndex, total);

      root.dispatchEvent(new CustomEvent('tabletools:updated', {
        bubbles: true,
        detail: {
          filteredCount: total,
          visibleCount: pageRows.length,
          page: currentPage,
          pageSize: pageSize
        }
      }));
    }

    function resetAndUpdate() {
      currentPage = 1;
      update();
    }

    if (searchInput) searchInput.addEventListener('input', resetAndUpdate);
    filters.forEach(function (filter) {
      filter.addEventListener('change', resetAndUpdate);
    });
    if (sizeControl) sizeControl.addEventListener('change', resetAndUpdate);
    if (pager) {
      pager.addEventListener('click', function (event) {
        var button = event.target.closest('[data-tt-page]');
        if (!button || button.disabled) return;
        currentPage = parseInt(button.getAttribute('data-tt-page'), 10);
        update();
      });
    }

    update();
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-tabletools]').forEach(enhanceTable);
  });
})();
