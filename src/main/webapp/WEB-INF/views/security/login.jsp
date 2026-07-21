<%--
  Created by IntelliJ IDEA.
  User: junghyun
  Date: 26. 6. 25.
  Time: 오전 11:00
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
    <title>Title</title>
</head>
<body>
    <h1>login</h1>
<form name="f" action="/security/login" method="post">
    <input type="text" name="${_csrf.parameterName}" value="${_csrf.token}" />
    <c:if test="${param.error != null}">
        <div style="color: red">사용자 ID 또는 비밀번호가 틀립니다.</div>
    </c:if>
    <table>
        <tr>
            <td>User:</td>
            <td><input type="text" name="username" value="" /></td>
        </tr>
        <tr>
            <td>Password:</td>
            <td><input type="password" name="password" /></td>
        </tr>
        <tr>
            <td colspan="2">
                <input name="submit" type="submit" value="Login" />
            </td>
        </tr>
    </table>
</form>
</body>
</html>
