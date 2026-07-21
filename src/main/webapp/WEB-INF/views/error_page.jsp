<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Created by IntelliJ IDEA.
  User: junghyun
  Date: 26. 6. 16.
  Time: 오전 11:47
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Title</title>
</head>
<body>
<h4><c:out value="${exception.getMessage()}"></c:out></h4>

<ul>
  <c:forEach items="${exception.getStackTrace() }" var="stack">
    <li><c:out value="${stack}"></c:out></li>
  </c:forEach>
</ul>
</body>
</html>
