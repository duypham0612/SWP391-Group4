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

    var form = input.form;
    if (form && !form.__viNumberBound) {
      form.__viNumberBound = true;
      form.addEventListener('submit', function () {
        Array.prototype.slice.call(form.querySelectorAll('[data-vi-number]')).forEach(normalize);
      });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.slice.call(document.querySelectorAll('[data-vi-number]')).forEach(init);
  });
})();
