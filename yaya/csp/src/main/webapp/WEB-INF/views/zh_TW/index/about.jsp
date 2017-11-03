<%--
  Created by IntelliJ IDEA.
  User: Liuchangling
  Date: 2017/10/31
  Time: 11:59
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>${article.titleTw}</title>
    <meta content="width=device-width, initial-scale=1.0, user-scalable=no" name="viewport">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <%@include file="/WEB-INF/include/page_context.jsp"%>
    <link rel="stylesheet" href="${ctxStatic}/css/global.css">
    <link rel="stylesheet" href="${ctxStatic}/css/menu.css">
    <link rel="stylesheet" href="${ctxStatic}/css/perfect-scrollbar.min.css">
    <link rel="stylesheet" href="${ctxStatic}/css/animate.min.css" type="text/css" />
    <link rel="stylesheet" href="${ctxStatic}/css/style.css">
    <style>
        html,body { background-color:#F7F9FB;}
    </style>
</head>

<body>
<div id="wrapper">
    <%@include file="/WEB-INF/include/index_header_zh_CN.jsp"%>

    <div class="admin-content bg-gray" >
        <div class="page-width clearfix">
            <div class="subPage-head item-shadow item-radius clearfix">
                <h3 class="title">
                    <c:if test="${article.titleTw eq '關於我們'}">
                        <i class="icon icon-header-point"></i>
                    </c:if>
                        ${article.titleTw}
                </h3>
            </div>
            <div class="subPage-main item-shadow item-radius" >
                ${article.contentTw}
            </div>
        </div>
    </div>

    <%@include file="/WEB-INF/include/footer_zh_CN.jsp"%>

</div>

</body>
</html>