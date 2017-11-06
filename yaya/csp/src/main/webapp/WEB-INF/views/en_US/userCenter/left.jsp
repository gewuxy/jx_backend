<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<div class="user-left-box">
    <div class="user-userinfo clearfix item-radius">
        <div class="img"><img src="${dto.avatar}" id="image" alt="" style="widtH:126px; height:126px;"></div>
        <div class="name" id="name">${dto.userName}</div>
        <div class="email">${dto.email == null ? dto.mobile:dto.email}</div>
        <div class="binding">
            <c:if test="${fn:length(dto.bindInfoList) != 0}">
                    <c:forEach items="${dto.bindInfoList}" var="list">
                        <c:if test="${list.thirdPartyId == 4}">
                            <img src="${ctxStatic}/images/icon-user-twitter.png" alt="">
                        </c:if>
                        <c:if test="${list.thirdPartyId == 3}">
                            <img src="${ctxStatic}/images/icon-user-facebook.png" alt="">
                        </c:if>
                        <c:if test="${list.thirdPartyId == 5}">
                            <img src="${ctxStatic}/images/icon-user-medcn.png"  alt="">
                        </c:if>
                        <c:if test="${not empty dto.email}">
                            <img src="${ctxStatic}/images/icon-user-email.png"  alt="">
                        </c:if>
                        <c:if test="${not empty dto.mobile}">
                            <img src="${ctxStatic}/images/icon-user-phone.png"  alt="">
                        </c:if>
                    </c:forEach>
            </c:if>
            <c:if test="${fn:length(dto.bindInfoList) == 0}">
                        <img src="${ctxStatic}/images/default_blank.png" alt="" width="34" height="34">

            </c:if>
        </div>
        <%--<img src="./images/icon-user-facebook.png" alt="">--%>
    </div>
    <div class="user-statistics item-radius">
        <div class="title">Statistics of PPT</div>
        <div class="main line"> <img src="${ctxStatic}/images/user-statistics-icon.png" alt="" class="icon"><span class="item"><span class="num">${dto.pptCount}</span>file(s) uploaded</span></div>
        <div class="main"><img src="${ctxStatic}/images/user-statistics-icon.png" alt="" class="icon"><span class="item"><span class="num">${dto.shareCount}</span>item(s) shared</span></div>
    </div>
</div>


    <script>
        $(function(){




        })



    </script>
</body>
</html>