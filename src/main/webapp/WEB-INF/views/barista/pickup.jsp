<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Món chờ giao</h1>
        <p>Món đã pha xong, chờ mang ra. Kiểm tra đủ món của bàn rồi bấm “Đã giao”.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/barista/kds">← Hàng chờ pha</a>
</div>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div id="pickupBoard" class="kds-board ${onShift ? '' : 'is-viewonly'}">
    <jsp:include page="pickup_cards.jsp" />
</div>

<div class="kds-refresh muted">
    <span class="kds-refresh__dot"></span>
    Tự cập nhật mỗi <span id="puCountdown">5</span> giây
</div>

<script>
  (function(){
    var ctx = '${ctx}';
    var board = document.getElementById('pickupBoard');
    var countdown = document.getElementById('puCountdown');
    var n = 5;
    var refreshing = false;

    function userIsWorking(){
      return board && board.contains(document.activeElement);
    }

    async function refresh(){
      if (!board || refreshing || document.visibilityState === 'hidden' || userIsWorking()) return;
      refreshing = true;
      try {
        var response = await fetch(ctx + '/barista/pickup?partial=1', {credentials:'same-origin'});
        if (!response.ok) return;
        board.innerHTML = await response.text();
      } finally {
        refreshing = false;
      }
    }

    setInterval(function(){
      if (document.visibilityState === 'hidden') return;
      n -= 1;
      if (n <= 0) { n = 5; refresh(); }
      if (countdown) countdown.textContent = n;
    }, 1000);
  })();
</script>
<jsp:include page="../layout/footer.jsp" />
