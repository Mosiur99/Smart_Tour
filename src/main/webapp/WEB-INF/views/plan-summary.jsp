<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:set var="pageTitle" value="Tour Plan — ${plan.districtName} | SmartTour"/>
    <jsp:include page="fragments/head.jsp"/>
</head>
<body>
<jsp:include page="fragments/header.jsp"/>

<section class="st-hero st-hero-compact">
    <div class="container">
        <h1>${plan.originCity} &rarr; ${plan.districtName}</h1>
        <p class="mb-0">Your complete tour plan is ready</p>
    </div>
</section>

<main class="st-main">
    <div class="container pb-5">
        <div class="st-page-header">
            <div>
                <p class="subtitle mb-0">${plan.travelers} traveler(s) &middot; ${plan.nights} night(s)</p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <a href="/plan/build/${plan.districtId}?origin=${plan.originCity}" class="btn btn-st-outline">
                    <i class="bi bi-pencil me-1"></i> Edit
                </a>
                <a href="/plan/export/pdf" class="btn btn-st-danger">
                    <i class="bi bi-file-earmark-pdf me-1"></i> Download PDF
                </a>
                <a href="/plan/new" class="btn btn-st-secondary">
                    <i class="bi bi-plus-lg me-1"></i> New Plan
                </a>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-lg-8">
                <div class="st-card mb-4">
                    <div class="st-card-header"><i class="bi bi-bus-front"></i> Transport</div>
                    <div class="st-card-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <div class="st-spot-tile">
                                    <small class="text-muted text-uppercase fw-bold">Outbound</small>
                                    <p class="mb-1 mt-1"><strong>${plan.outboundTransport.name}</strong></p>
                                    <span class="st-mode-pill">${plan.outboundTransport.mode}</span>
                                    <p class="small text-muted mb-0 mt-2">
                                        ${not empty plan.outboundTransport.departureTime ? plan.outboundTransport.departureTime : 'Time TBD'} &middot;
                                        <c:choose>
                                            <c:when test="${plan.outboundTransport.costPerPerson != null}">${plan.outboundTransport.costPerPerson} tk/person</c:when>
                                            <c:otherwise>fare TBD</c:otherwise>
                                        </c:choose>
                                    </p>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="st-spot-tile">
                                    <small class="text-muted text-uppercase fw-bold">Return</small>
                                    <p class="mb-1 mt-1"><strong>${plan.returnTransport.name}</strong></p>
                                    <span class="st-mode-pill">${plan.returnTransport.mode}</span>
                                    <p class="small text-muted mb-0 mt-2">
                                        ${not empty plan.returnTransport.departureTime ? plan.returnTransport.departureTime : 'Time TBD'} &middot;
                                        <c:choose>
                                            <c:when test="${plan.returnTransport.costPerPerson != null}">${plan.returnTransport.costPerPerson} tk/person</c:when>
                                            <c:otherwise>fare TBD</c:otherwise>
                                        </c:choose>
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="st-card mb-4">
                    <div class="st-card-header"><i class="bi bi-list-ol"></i> Suggested Visit Order</div>
                    <ol class="list-group list-group-numbered list-group-flush">
                        <c:forEach var="spot" items="${plan.selectedSpots}">
                            <li class="list-group-item st-list-item border-0">
                                <strong>${spot.name}</strong>
                                <c:if test="${not empty spot.offDay}"><span class="st-badge-danger ms-1">Closed ${spot.offDay}</span></c:if>
                                <c:if test="${not empty spot.tip}"><br><small class="text-muted">${spot.tip}</small></c:if>
                            </li>
                        </c:forEach>
                        <c:if test="${empty plan.selectedSpots}">
                            <li class="list-group-item text-muted">No spots selected</li>
                        </c:if>
                    </ol>
                </div>

                <div class="st-card mb-4">
                    <div class="st-card-header"><i class="bi bi-cup-hot"></i> Khabar (Food)</div>
                    <div class="p-0">
                        <c:forEach var="food" items="${plan.foods}">
                            <div class="st-list-item">${food.name}
                                <c:if test="${not empty food.location}"><small class="text-muted"> — ${food.location}</small></c:if>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <div class="st-card mb-4">
                    <div class="st-card-header"><i class="bi bi-building"></i> Hotel</div>
                    <div class="st-card-body">
                        <c:choose>
                            <c:when test="${not empty plan.selectedHotel}">
                                <strong>${plan.selectedHotel.name}</strong>
                                <p class="mb-0 mt-1 text-muted">
                                    <c:choose><c:when test="${plan.acRoom}">AC</c:when><c:otherwise>Non-AC</c:otherwise></c:choose>
                                    room &middot; ${plan.nights} night(s)
                                </p>
                                <c:if test="${not empty plan.selectedHotel.review}">
                                    <small class="text-muted">${plan.selectedHotel.review}</small>
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                <span class="text-muted">No hotel selected</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <c:if test="${not empty plan.tourGuideUrl}">
                    <div class="st-card">
                        <div class="st-card-body d-flex align-items-center gap-3">
                            <i class="bi bi-youtube fs-3 text-danger"></i>
                            <div>
                                <strong>Tour Guide</strong><br>
                                <a href="${plan.tourGuideUrl}" target="_blank" rel="noopener">${plan.tourGuideUrl}</a>
                            </div>
                        </div>
                    </div>
                </c:if>
            </div>

            <div class="col-lg-4">
                <div class="st-card st-budget-card st-sticky-sidebar">
                    <div class="st-card-header"><i class="bi bi-wallet2"></i> Estimate Cost</div>
                    <div class="p-0">
                        <table class="table mb-0">
                            <tbody>
                            <c:forEach var="item" items="${plan.budget.lineItems}">
                                <tr>
                                    <td class="ps-3 py-3">
                                        <strong>${item.label}</strong>
                                        <br><small class="text-muted">${item.detail}</small>
                                    </td>
                                    <td class="text-end pe-3 fw-semibold">${item.amount}</td>
                                </tr>
                            </c:forEach>
                            <tr class="st-budget-total">
                                <td class="ps-3">Total (BDT)</td>
                                <td class="text-end pe-3">${plan.budget.total}</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${not empty plan.budget.missingCosts}">
                        <div class="st-card-body py-2 border-top">
                            <small class="text-muted">Not included: ${plan.budget.missingCostsLabel}</small>
                        </div>
                    </c:if>
                    <div class="st-card-body py-2 border-top">
                        <small class="text-muted"><i class="bi bi-info-circle me-1"></i> Estimated planning costs — verify live fares before booking</small>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
