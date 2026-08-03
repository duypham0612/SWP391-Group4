(function () {
  'use strict';

  function parseDisplay(value) {
    var raw = String(value == null ? '' : value).trim().replace(/\s/g, '');
    if (!raw) return '';
    if (raw.indexOf(',') >= 0) return raw.replace(/\./g, '').replace(',', '.');
    if (/^[+-]?\d{1,3}(\.\d{3})+$/.test(raw)) return raw.replace(/\./g, '');
    return raw;
  }

  function formatStandard(value) {
    var shown = String(value == null ? '' : value).trim();
    var trailingDecimal = shown.endsWith(',');
    var raw = parseDisplay(shown).replace(/[^\d.+-]/g, '');
    if (!raw) return '';
    var negative = raw.charAt(0) === '-';
    raw = raw.replace(/[+-]/g, '');
    var pieces = raw.split('.');
    var integer = (pieces.shift() || '0').replace(/^0+(?=\d)/, '');
    var decimal = pieces.join('').replace(/\D/g, '').slice(0, 3);
    integer = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return (negative ? '-' : '') + integer + (decimal ? ',' + decimal : (trailingDecimal ? ',' : ''));
  }

  function normalize(input) {
    input.value = parseDisplay(input.value);
  }

  function moneyDigits(value) {
    return String(value == null ? '' : value).replace(/\D/g, '');
  }

  function formatMoney(value) {
    var raw = moneyDigits(value).replace(/^0+(?=\d)/, '');
    return raw.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  }

  function normalizeMoney(input) {
    input.value = moneyDigits(input.value);
  }

  function bindForm(form) {
    if (!form || form.__viNumberBound) return;
    form.__viNumberBound = true;
    form.addEventListener('submit', function () {
      Array.prototype.slice.call(form.querySelectorAll('[data-vi-number]')).forEach(normalize);
      Array.prototype.slice.call(form.querySelectorAll('[data-vi-money]')).forEach(normalizeMoney);
    });
  }

  function init(input) {
    input.setAttribute('inputmode', 'decimal');
    input.setAttribute('autocomplete', 'off');
    input.value = formatStandard(input.value);
    input.addEventListener('input', function () {
      var cursor = input.selectionEnd;
      var before = input.value.length;
      input.value = formatStandard(input.value);
      var next = Math.max(0, cursor + input.value.length - before);
      try { input.setSelectionRange(next, next); } catch (ignore) {}
    });

    bindForm(input.form);
  }

  function initMoney(input) {
    input.setAttribute('inputmode', 'numeric');
    input.setAttribute('autocomplete', 'off');
    input.value = formatMoney(input.value);

    // Không chèn dấu phân cách trong lúc người dùng đang gõ. Việc đổi độ dài chuỗi
    // sau mỗi phím làm caret nhảy vào giữa số trên một số trình duyệt/IME, khiến
    // phím tiếp theo chèn sai vị trí (ví dụ 4.000 thành 440.000).
    input.addEventListener('focus', function () {
      input.value = moneyDigits(input.value);
    });
    input.addEventListener('input', function () {
      var cursor = input.selectionStart == null ? input.value.length : input.selectionStart;
      var digitsOnLeft = moneyDigits(input.value.slice(0, cursor)).length;
      input.value = moneyDigits(input.value);
      try { input.setSelectionRange(digitsOnLeft, digitsOnLeft); } catch (ignore) {}
    });
    input.addEventListener('blur', function () {
      input.value = formatMoney(input.value);
    });
    bindForm(input.form);
  }

  document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.slice.call(document.querySelectorAll('[data-vi-number]')).forEach(init);
    Array.prototype.slice.call(document.querySelectorAll('[data-vi-money]')).forEach(initMoney);
  });
})();
