<%@ page contentType="text/html;charset=UTF-8" %>
        </main>
    </div>
</div>
<script>
  (function(){
    var btn=document.getElementById('themeToggle');
    if(!btn)return;
    btn.addEventListener('click',function(){
      var cur=document.documentElement.getAttribute('data-theme')==='dark'?'dark':'light';
      var next=cur==='dark'?'light':'dark';
      document.documentElement.setAttribute('data-theme',next);
      try{localStorage.setItem('cafe-theme',next);}catch(e){}
    });
  })();

  /* Thu gọn / mở rộng menu. Cờ đặt trên <html> để khớp script chống nháy màn ở <head>.
     Nút chỉ có ở màn dạng bảng điều hành nên thoát sớm khi không tìm thấy. */
  (function(){
    var btn=document.getElementById('navToggle');
    if(!btn)return;
    var label=btn.querySelector('.sidebar-toggle__text');
    function sync(){
      var collapsed=document.documentElement.classList.contains('is-nav-collapsed');
      btn.setAttribute('aria-expanded',String(!collapsed));
      if(label)label.textContent=collapsed?label.dataset.textClosed:label.dataset.textOpen;
    }
    sync();
    btn.addEventListener('click',function(){
      var collapsed=document.documentElement.classList.toggle('is-nav-collapsed');
      try{localStorage.setItem('cafe-nav',collapsed?'collapsed':'open');}catch(e){}
      sync();
    });
  })();
</script>
<%-- JS theo màn. Trang tự khai `jsBundles` ở đầu file, ví dụ:
       <c:set var="jsBundles" value="admin/table-tools" scope="request" />

     Trước đây ba script này được nạp cho MỌI trang của MỌI role: màn quầy pha chế tải 331 dòng JS
     nó không bao giờ dùng, và ngược lại. Cả ba đều chỉ gắn vào một thuộc tính data-* riêng
     (data-tabletools, data-money-input, data-vi-number) nên trang không có thuộc tính đó thì tải về
     rồi bỏ không. --%>
<c:set var="_j" value=",${jsBundles}," />
<c:if test="${_j.contains(',admin/table-tools,')}"><script src="${pageContext.request.contextPath}/assets/js/admin/table-tools.js?v=${applicationScope.assetVersion}" defer></script></c:if>
<c:if test="${_j.contains(',admin/money-input,')}"><script src="${pageContext.request.contextPath}/assets/js/admin/money-input.js?v=${applicationScope.assetVersion}" defer></script></c:if>
<c:if test="${_j.contains(',manager/vi-number-input,')}"><script src="${pageContext.request.contextPath}/assets/js/manager/vi-number-input.js?v=${applicationScope.assetVersion}" defer></script></c:if>
</body>
</html>
