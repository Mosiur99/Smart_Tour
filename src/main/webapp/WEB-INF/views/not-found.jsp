<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:set var="pageTitle" value="Page not found | SmartTour"/>
    <jsp:include page="fragments/head.jsp"/>
</head>
<body>
<jsp:include page="fragments/header.jsp"/>

<main class="container py-5 text-center">
    <h1>We couldn't find that district</h1>
    <p class="text-muted">Check the link, or choose a district from the home page.</p>
    <a href="/" class="btn btn-st-outline">Go to home page</a>
</main>

<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
