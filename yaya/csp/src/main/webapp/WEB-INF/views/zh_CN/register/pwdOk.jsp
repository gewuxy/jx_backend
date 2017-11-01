<%--
  Created by IntelliJ IDEA.
  User: lixuan
  Date: 2017/5/19
  Time: 9:22
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>忘记密码 - 成功</title>
    <meta content="width=device-width, initial-scale=1.0, user-scalable=no" name="viewport">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <%@include file="/WEB-INF/include/page_context.jsp"%>
    <link rel="stylesheet" href="${ctxStatic}/css/global.css">
    <link rel="stylesheet" href="${ctxStatic}/css/animate.min.css" type="text/css" />
    <link rel="stylesheet" href="${ctxStatic}/css/style.css">
</head>

<body>
<div id="wrapper">
    <div class="login login-banner" style="height:900px;">
        <div class="page-width pr">
            <div class="login-header">
                <%@include file="/WEB-INF/include/login_header_zh_CN.jsp"%>
                <%@include file="/WEB-INF/include/language_zh_CN.jsp"%>
            </div>
            <div class="login-box clearfix">
                <%@include file="/WEB-INF/include/login_left.jsp"%>

                <div class="col-lg-5 login-box-item">

                    <!--切换  输入新密码-->
                    <div class="login-box-main position-message-login">
                        <form action="">
                            <div class="login-form-item">
                                <div class="login-message-text">
                                    <p><img src="${ctxStatic}/images/icon-succeed.png" alt=""></p>
                                    <p class="t-center">密码重置成功</p>
                                </div>
                                <input id="loginBtn" type="button" class="button login-button buttonBlue last" value="重新登录">
                            </div>
                        </form>
                    </div>

                </div>
            </div>


            <%@include file="/WEB-INF/include/login_footer_zh_CN.jsp"%>

        </div>
    </div>
</div>

<script>
    $(function(){
        //让背景撑满屏幕
        $('.login-banner').height($(window).height());
        //让协议定位到底部
        $('.login-box-item').height($('.login-box').height());

        $("#loginBtn").click(function () {
            window.location.href = "${ctx}/mgr/login?thirdPartyId=7";
        });

    })
</script>


</body>
</html>
