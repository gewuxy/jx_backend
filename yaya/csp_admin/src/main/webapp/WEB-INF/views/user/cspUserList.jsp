<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>CSP用户管理</title>
    <%@include file="/WEB-INF/include/page_context.jsp"%>
</head>
<body>
<ul class="nav nav-tabs">
    <li><a href="${ctx}/csp/user/list?listType=0">国内用户列表</a></li>
    <li><a href="${ctx}/csp/user/list?listType=1">海外用户列表</a></li>
    <li><a href="${ctx}/csp/user/list?listType=2">冻结用户列表</a></li>
</ul>
<%@include file="/WEB-INF/include/message.jsp"%>
<form id="pageForm" name="pageForm" action="${ctx}/csp/user/list" method="post">
    <input  name="pageNum" type="hidden" value="${page.pageNum}"/>
    <input  name="pageSize" type="hidden" value="${page.pageSize}"/>
    <input  name="userName" type="hidden" value="${userName}"/>
    <input  name="type" type="hidden" value="${type}"/>
    <input  name="listType" type="hidden" value="${listType}"/>
</form>
<form id="searchForm" method="post" action="${ctx}/csp/user/list" class="breadcrumb form-search">
    <input placeholder="输入用户名进行搜索" value="${userName}" size="40"  type="search" name="userName" maxlength="50" class="required"/>
    <input id="btnSubmit" class="btn btn-primary" type="submit" value="查询" onclick="return page();"/>
</form>
<table id="contentTable" class="table table-striped table-bordered table-condensed">
    <thead><tr><th>ID</th><th>昵称</th><th>电话</th><th>电子邮件</th><th>注册时间</th><th>操作</th></tr></thead>
    <tbody>
    <c:if test="${not empty page.dataList}">
        <c:forEach items="${page.dataList}" var="user">
            <tr>
                <td>${user.id}</td>
                <td>${user.nickName}</td>
                <td>${user.mobile}</td>
                <td>${user.email}</td>
                <td><fmt:formatDate value="${user.registerTime}" type="both"/></td>
                <td>
                    <a href="${ctx}/csp/user/viewOrRegister?id=${user.id}&actionType=3&listType=${listType}">修改</a>
                    <c:if test="${user.active eq true}">
                        <a data-href="${ctx}/csp/user/update?id=${user.id}&actionType=1&listType=${listType}"  onclick="layerConfirm('确认要冻结该用户帐号吗？', this)">冻结</a>
                    </c:if>
                    <a data-href="${ctx}/csp/user/update?id=${user.id}&actionType=2&listType=${listType}"  onclick="layerConfirm('确认要删除该用户帐号吗？', this)">删除</a>
                </td>
            </tr>
        </c:forEach>
    </c:if>
    <c:if test="${empty page.dataList}">
        <tr>
            <td colspan="7">没有查询到数据</td>
        </tr>
    </c:if>
    </tbody>
</table>
<%@include file="/WEB-INF/include/pageable.jsp"%>
<script>
    $(document).ready(function() {
        var active = '${listType}';
        $(".nav-tabs li:eq(" + active +")").addClass("active");
    });
</script>
</body>
</html>
