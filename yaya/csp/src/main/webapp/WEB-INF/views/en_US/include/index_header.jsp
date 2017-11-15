<%--
  Created by IntelliJ IDEA.
  User: Liuchangling
  Date: 2017/11/1
  Time: 14:02
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<div class="admin-header">
    <div class="page-width clearfix">
        <div class="fr">
            <!--登录前-->
            <div class="login-header">
                <a href="${ctx}/login" class="login-header-button" title="login">${not empty username ? username : "login"}&nbsp;&nbsp;<span><img src="${ctxStatic}/images/admin-user-icon.png" alt=""></span></a>
                <%@include file="/WEB-INF/include/switch_language.jsp"%>
            </div>
        </div>
        <div class="fl">
            <a class="logo" href="https://www.medcn.com"></a>
        </div>
    </div>
</div>