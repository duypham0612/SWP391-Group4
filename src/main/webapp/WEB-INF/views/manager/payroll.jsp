<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Nhân sự</div><h1>Bảng lương</h1><p>Giờ từ chấm công đã duyệt · sửa giờ làm &amp; lương/giờ rồi lưu · tháng ${month}</p></div>
    <div style="display:flex;gap:8px;align-items:center">
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${prevMonth}">← Tháng trước</a>
        <strong>${month}</strong>
        <a class="btn btn-ghost btn-sm" href="${ctx}/manager/payroll?month=${nextMonth}">Tháng sau →</a>
        <a class="btn btn-primary btn-sm" href="${ctx}/manager/payroll?action=export&month=${month}">Xuất Excel</a>
    </div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><c:out value="${sessionScope.flashError}" /></div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty rows}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có chấm công đã duyệt trong tháng này.</p></div>
    </c:when>
    <c:otherwise>
        <form id="payrollForm" action="${ctx}/manager/payroll" method="post" data-min-rate="${minHourlyRate}" novalidate>
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="month" value="${month}">
            <table class="table" id="payTable">
                <thead><tr>
                    <th>Nhân viên</th><th style="width:150px">Vai trò</th>
                    <th style="width:90px">Số ca</th>
                    <th style="width:150px">Giờ làm</th>
                    <th style="width:170px">Lương/giờ (₫)</th>
                    <th style="width:170px">Thành tiền (₫)</th>
                </tr></thead>
                <tbody>
                    <c:set var="totalHours" value="0" />
                    <c:set var="totalSalary" value="0" />
                    <c:forEach var="r" items="${rows}">
                        <tr>
                            <td><input type="hidden" name="uid" value="${r.userId}">${r.userName}</td>
                            <td>${r.roleName}</td>
                            <td>${r.approvedShifts}
                                <c:if test="${r.overridden}"><span class="badge badge-ready" title="Đã chốt/sửa">đã chốt</span></c:if>
                            </td>
                            <td><input type="number" name="hours_${r.userId}" class="form-control payHours" data-uid="${r.userId}"
                                       min="0" step="any" value="${r.totalHours}" style="width:130px">
                                <div class="muted" style="font-size:.78rem">chấm công: ${r.computedHours}h</div></td>
                            <td><input type="number" name="rate_${r.userId}" class="form-control payRate" data-uid="${r.userId}"
                                       min="${minHourlyRate}" step="1000" value="${r.hourlyRate}" style="width:150px">
                                <div class="muted" style="font-size:.78rem">Tối thiểu <fmt:formatNumber value="${minHourlyRate}" maxFractionDigits="0"/>₫/giờ</div></td>
                            <td><strong class="paySalary" data-uid="${r.userId}"><fmt:formatNumber value="${r.salary}" maxFractionDigits="0"/></strong> ₫</td>
                        </tr>
                        <c:set var="totalHours" value="${totalHours + r.totalHours}" />
                        <c:set var="totalSalary" value="${totalSalary + r.salary}" />
                    </c:forEach>
                    <tr style="border-top:2px solid var(--line);font-weight:700">
                        <td colspan="3">Tổng cộng</td>
                        <td><span id="sumHours">${totalHours}</span> giờ</td>
                        <td></td>
                        <td><span id="sumSalary"><fmt:formatNumber value="${totalSalary}" maxFractionDigits="0"/></span> ₫</td>
                    </tr>
                </tbody>
            </table>
            <button type="submit" class="btn btn-primary btn-lg" style="margin-top:12px">Lưu bảng lương</button>
        </form>

        <script>
        (function(){
            var form=document.getElementById('payrollForm');
            var minRate=parseFloat(form.getAttribute('data-min-rate'))||25000;
            function fmt(n){ return Math.round(n).toLocaleString('vi-VN'); }
            function markRate(rt, invalid){
                rt.setAttribute('aria-invalid', invalid ? 'true' : 'false');
                rt.style.borderColor=invalid ? 'var(--st-cancelled)' : '';
                rt.style.boxShadow=invalid ? '0 0 0 3px rgba(178,58,46,.18)' : '';
            }
            function removeClientAlert(){
                var old=document.getElementById('payrollClientError');
                if(old) old.remove();
            }
            function showClientAlert(message){
                removeClientAlert();
                var alert=document.createElement('div');
                alert.id='payrollClientError';
                alert.className='alert alert-error';
                alert.textContent=message;
                form.parentNode.insertBefore(alert, form);
            }
            function invalidRates(){
                var bad=[];
                document.querySelectorAll('.payRate').forEach(function(rt){
                    var rate=parseFloat(rt.value);
                    var invalid=isNaN(rate)||rate<minRate;
                    markRate(rt, invalid);
                    if(invalid) bad.push(rt);
                });
                return bad;
            }
            function recompute(){
                var sumH=0, sumS=0;
                document.querySelectorAll('#payTable tbody tr').forEach(function(tr){
                    var h=tr.querySelector('.payHours'), rt=tr.querySelector('.payRate'), sc=tr.querySelector('.paySalary');
                    if(!h||!rt||!sc) return;
                    var hours=parseFloat(h.value)||0, rate=parseFloat(rt.value)||0, sal=hours*rate;
                    sc.textContent=fmt(sal); sumH+=hours; sumS+=sal;
                });
                document.getElementById('sumHours').textContent=(Math.round(sumH*10)/10);
                document.getElementById('sumSalary').textContent=fmt(sumS);
            }
            document.querySelectorAll('.payHours, .payRate').forEach(function(el){
                el.addEventListener('input', function(){
                    removeClientAlert();
                    if(el.classList.contains('payRate')) invalidRates();
                    recompute();
                });
            });
            form.addEventListener('submit', function(e){
                var bad=invalidRates();
                if(bad.length){
                    e.preventDefault();
                    showClientAlert('Lương/giờ tối thiểu là '+fmt(minRate)+'₫. Vui lòng kiểm tra các ô đang được đánh dấu.');
                    bad[0].focus();
                }
            });
        })();
        </script>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
