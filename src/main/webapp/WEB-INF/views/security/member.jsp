<%--
  Created by IntelliJ IDEA.
  User: junghyun
  Date: 26. 6. 25.
  Time: 오전 10:26
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
  <h1>/Security/member page</h1>

  <form action="/security/logout" method="post">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
      <input type="submit" value="로그아웃" />
  </form>
</body>
</html>
