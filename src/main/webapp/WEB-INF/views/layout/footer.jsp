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
</script>
<script src="${pageContext.request.contextPath}/assets/js/table-tools.js?v=${applicationScope.assetVersion}" defer></script>
</body>
</html>
