<%--
  Created by IntelliJ IDEA.
  User: lixuan
  Date: 2017/5/2
  Time: 15:56
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <%@include file="/WEB-INF/include/page_context.jsp"%>
    <title>Title</title>
</head>
<body>
<ul class="nav nav-tabs">
    <li class="active"><a href="${ctx}/sys/role/list">角色列表</a></li>
    <li><a href="${ctx}/sys/role/edit">添加角色</a></li>
</ul>
<%@include file="/WEB-INF/include/message.jsp"%>
<table id="contentTable" class="table table-striped table-bordered table-condensed">
    <tr><th>角色英文名称</th><th>角色中文名称</th><shiro:hasPermission name="sys:role:edit"><th>操作</th></shiro:hasPermission></tr>
    <c:forEach items="${roleList}" var="role">
        <tr>
            <td>${role.roleName}</td>
            <td>${role.roleDesc}</td>
            <shiro:hasPermission name="sys:role:edit"><td>
                <a href="${ctx}/sys/role/assign?id=${role.id}">分配</a>
                <a href="${ctx}/sys/role/edit?id=${role.id}">修改</a>
                <a data-href="${ctx}/sys/role/delete?id=${role.id}"  onclick="layerConfirm('确认要删除该角色吗？', this)">删除</a>
            </td></shiro:hasPermission>
        </tr>
    </c:forEach>
</table>
</body>
</html>
