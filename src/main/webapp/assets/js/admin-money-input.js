(function () {
  'use strict';

  function clean(value) {
    return String(value || '')
      .replace(/,/g, '')
      .replace(/\s/g, '')
      .replace(/[^\d.]/g, '');
  }

  function format(value) {
    var raw = clean(value);
    if (!raw) return '';

    var parts = raw.split('.');
    var intPart = parts.shift() || '0';
    var decimalPart = parts.length ? parts.join('').replace(/\D/g, '') : '';
    if (/^0+$/.test(decimalPart)) decimalPart = '';
    intPart = intPart.replace(/^0+(?=\d)/, '');
    intPart = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

    if (String(value).endsWith('.') && decimalPart === '') return intPart + '.';
    return decimalPart ? intPart + '.' + decimalPart : intPart;
  }

  function normalizeForSubmit(input) {
    input.value = clean(input.value);
  }

  function init(input) {
    input.setAttribute('inputmode', 'decimal');
    input.setAttribute('autocomplete', 'off');
    input.value = format(input.value);

    input.addEventListener('input', function () {
      var end = input.selectionEnd;
      var before = input.value.length;
      input.value = format(input.value);
      var after = input.value.length;
      try {
        input.setSelectionRange(Math.max(0, end + (after - before)), Math.max(0, end + (after - before)));
      } catch (e) {}
    });

    var formId = input.getAttribute('form');
    var form = formId ? document.getElementById(formId) : input.form;
    if (form && !form.__moneyInputBound) {
      form.__moneyInputBound = true;
      form.addEventListener('submit', function () {
        Array.prototype.slice.call(form.querySelectorAll('[data-money-input]')).forEach(normalizeForSubmit);
        Array.prototype.slice.call(document.querySelectorAll('[data-money-input][form="' + form.id + '"]')).forEach(normalizeForSubmit);
      });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    Array.prototype.slice.call(document.querySelectorAll('[data-money-input]')).forEach(init);
  });
})();
