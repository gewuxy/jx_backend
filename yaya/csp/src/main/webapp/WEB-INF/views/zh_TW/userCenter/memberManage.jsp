<%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 2017/12/14
  Time: 13:59
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>個人中心 - 會員權限</title>
    <%@include file="/WEB-INF/include/page_context.jsp" %>
    <%--<link rel="SHORTCUT ICON" href="./images/v2/icon.ico" />--%>
    <meta content="width=device-width, initial-scale=1.0, user-scalable=no" name="viewport">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <link rel="stylesheet" href="${ctxStatic}/css/global.css">
    <link rel="stylesheet" href="${ctxStatic}/css/menu.css">
    <link rel="stylesheet" href="${ctxStatic}/css/perfect-scrollbar.min.css">
    <link rel="stylesheet" href="${ctxStatic}/css/animate.min.css" type="text/css" />
    <link rel="stylesheet" href="${ctxStatic}/css/style.css">

    <script src="${ctxStatic}/js/jquery.min.js"></script>
    <script src="${ctxStatic}/js/perfect-scrollbar.jquery.min.js"></script>
    <script src="${ctxStatic}/js/layer/layer.js"></script>
    <!--[if lt IE 9]>
    <script src="${ctxStatic}/js/html5.js"></script>
    <![endif]-->
</head>
<body >
<div id="wrapper">
    <%@include file="../include/header.jsp" %>
    <div class="admin-content bg-gray" >
        <div class="page-width clearfix">
            <div class="user-module clearfix">
                <div class="row clearfix">
                    <div class="col-lg-4">
                        <%@include file="./left.jsp" %>
                    </div>
                    <div class="col-lg-8">
                        <div class="user-menu clearfix item-radius">
                            <ul>
                                <li ><a href="${ctx}/mgr/user/info">我的信息</a></li>
                                <li ><a href="${ctx}/mgr/user/toAccount">賬號管理</a></li>
                                <li><a href="${ctx}/mgr/user/toFlux">流量管理</a></li>
                                <li class="cur"><a href="${ctx}/mgr/user/memberManage">會員管理</a></li>
                                <li class="last"><a href="${ctx}/mgr/user/toReset">修改密碼</a></li>
                            </ul>
                        </div>
                        <div class="user-content user-content-levelHeight item-radius member-mode">
                            <div class="member-mode-header clearfix">
                                <div class="fr">
                                    <div class="resource-label">
                                        <c:if test="${cspPackage.packageTw == '標準版' || cspPackage.packageTw == '高級版'}" >
                                            <span>

                                                    <c:if test="${cspPackage.usedMeetCount > cspPackage.limitMeets}">
                                                        <i class="hot" style="color: red">
                                                                ${cspPackage.usedMeetCount}
                                                        </i>
                                                    </c:if>
                                                    <i class="hot">
                                                            ${cspPackage.usedMeetCount}
                                                    </i>

                                                <i class="muted">|</i>${cspPackage.limitMeets}</span>
                                        </c:if>
                                        <c:if test="${cspPackage.packageTw == '專業版'}">
                                            <span><i class="hot">${cspPackage.usedMeetCount}</i><i class="muted">|</i>∞</span>
                                        </c:if>
                                    </div>
                                    <p class="t-center">會議數量</p>
                                </div>
                                <div class="fl">
                                    <div class="clearfix">
                                        <div class="fl"></div>
                                        <c:if test="${cspPackage.packageTw == '標準版'}">
                                            <div class="oh">
                                                <h5 class="title">${cspPackage.packageTw}</h5>
                                                <div class="member-mode-tips">已生效</div>
                                            </div>
                                        </c:if>
                                        <c:if test="${cspPackage.packageTw == '高級版' || cspPackage.packageTw == '專業版' }">
                                            <div class="fl member-grade"><img src="${ctxStatic}/images/member-icon-grade-01.png" alt=""></div>
                                            <div class="oh">
                                                <h5 class="title">${cspPackage.packageTw}</h5>
                                                <div class="member-mode-tips"><fmt:formatDate value="${cspPackage.packageStart}" type="both" pattern="yyyy-MM-dd"/>至<fmt:formatDate value="${cspPackage.packageEnd}" type="both" pattern="yyyy-MM-dd"/></div>
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                            <div class="member-mode-main">
                                <div class="member-mode-fnList">
                                    <ul>
                                        <c:forEach var="info" items="${cspPackageInfos}">
                                            <c:if test="${info.iden =='LB'}">
                                                <li>
                                                    <p><img src="${ctxStatic}/images/member-icon-01.png" alt=""></p>
                                                    <p>${info.descriptTw}</p>
                                                </li>
                                            </c:if>
                                            <c:if test="${info.iden =='ZB'}"><li>
                                                <p><img src="${ctxStatic}/images/member-icon-02.png" alt=""></p>
                                                <p>${info.descriptTw}</p>
                                            </li>
                                            </c:if>
                                            <c:if test="${info.iden =='LB' && info.limitMeets == 3}"><li>
                                                <p><img src="${ctxStatic}/images/member-icon-05.png" alt=""></p>
                                                <p>${info.limitMeets}個會議</p>
                                            </li>
                                            </c:if>
                                            <c:if test="${info.iden =='LB' && info.limitMeets == 10}"><li>
                                                <p><img src="${ctxStatic}/images/member-icon-06.png" alt=""></p>
                                                <p>${info.limitMeets}個會議</p>
                                            </li>
                                            </c:if>
                                            <c:if test="${info.iden =='LB' && info.limitMeets == 0}"><li>
                                                <p><img src="${ctxStatic}/images/member-icon-07.png" alt=""></p>
                                                <p>無限會議</p>
                                            </li>
                                            </c:if>

                                            <c:if test="${info.iden =='GG' && info.state == false}">
                                                <li>
                                                    <p><img src="${ctxStatic}/images/member-icon-03-not.png" alt=""></p>
                                                    <p class="color-gray-03">${info.descriptTw}</p>
                                                </li>
                                            </c:if>
                                            <c:if test="${info.iden =='GG' && info.state == true}">
                                                <li>
                                                    <p><img src="${ctxStatic}/images/member-icon-03.png" alt=""></p>
                                                    <p>${info.descriptTw}</p>
                                                </li>
                                            </c:if>
                                            <c:if test="${info.iden =='SY' && info.state == false}">
                                                <li>
                                                    <p><img src="${ctxStatic}/images/member-icon-04-not.png" alt=""></p>
                                                    <p class="color-gray-03">${info.descriptTw}</p>
                                                </li>
                                            </c:if>
                                            <c:if test="${info.iden =='SY' && info.state == true}">
                                                <li>
                                                    <p><img src="${ctxStatic}/images/member-icon-04.png" alt=""></p>
                                                    <p>${info.descriptTw}</p>
                                                </li>
                                            </c:if>
                                        </c:forEach>
                                    </ul>


                                </div>
                            </div>
                            <div class="member-mode-footer member-footer-position t-center">
                                <a href="javascript:;" type="button" class="button login-button buttonBlue member-buy-hook" id="btn">升級續費</a>
                                <p><a href="${ctx}/index/17103116063862386794" target="_blank">有疑問，幫助中心</a></p>

                            </div>


                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="admin-bottom">
        <div class="page-width clearfix">
            <p class="t-center">粵ICP備12087993號 © 2012-2017 敬信科技 版權所有 </p>
        </div>

    </div>
</div>

<script>
    $(function(){
        $("#config_6").parent().attr("class","cur");
        $("#btn").click(function () {
            layer.open({
                type: 2,
                title: false,
                skin: 'layui-layer-nobg', //没有背景色
                shadeClose: false,
                closeBtn: 0, //不显示关闭按钮
                shade: 0.1,
                area: ['1116px', '900px'],
                content: '${ctx}/mgr/pay/mark'
            })
        })
    })
</script>
</body>
</html>