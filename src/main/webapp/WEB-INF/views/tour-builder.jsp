<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:set var="pageTitle" value="Build Tour — ${district.name} | SmartTour"/>
    <jsp:include page="fragments/head.jsp"/>
</head>
<body>
<jsp:include page="fragments/header.jsp"/>

<section class="st-hero st-hero-compact">
    <div class="container">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb st-breadcrumb mb-2">
                <li class="breadcrumb-item"><a href="/">Home</a></li>
                <li class="breadcrumb-item"><a href="/district/${district.id}">${district.name}</a></li>
                <li class="breadcrumb-item active text-white opacity-75">Build Plan</li>
            </ol>
        </nav>
        <h1>${originCity} &rarr; ${district.name}</h1>
        <p class="mb-0">Configure transport, spots, hotel and trip details</p>
    </div>
</section>

<main class="st-main">
    <div class="container pb-5">
        <form action="/plan/save" method="post">
            <input type="hidden" name="districtId" value="${district.id}">
            <input type="hidden" name="originCity" value="${originCity}">

            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="st-card mb-4">
                        <div class="st-card-header"><i class="bi bi-signpost-split"></i> Transport</div>
                        <div class="st-card-body">
                            <div class="mb-4">
                                <label class="st-form-label">Outbound (${originCity} &rarr; ${district.name})</label>
                                <select name="outboundIndex" class="form-select st-form-select" required>
                                    <c:forEach var="t" items="${district.transport.fromDhaka}" varStatus="s">
                                        <option value="${s.index}">${t.displayLabel}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-0">
                                <label class="st-form-label">Return (${district.name} &rarr; ${originCity})</label>
                                <select name="returnIndex" class="form-select st-form-select" required>
                                    <c:forEach var="t" items="${district.transport.returnToDhaka}" varStatus="s">
                                        <option value="${s.index}">${t.displayLabel}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="st-card mb-4">
                        <div class="st-card-header"><i class="bi bi-geo-alt"></i> Select Tourist Spots</div>
                        <div class="st-card-body">
                            <c:forEach var="spot" items="${visitOptions}">
                                <div class="st-check-item">
                                    <div class="form-check mb-0">
                                        <input class="form-check-input" type="checkbox" name="spotNames"
                                               value="${spot.name}" id="spot_${spot.name.hashCode()}">
                                        <label class="form-check-label w-100" for="spot_${spot.name.hashCode()}">
                                            <strong>${spot.name}</strong>
                                            <c:if test="${not empty spot.offDay}"><span class="st-badge-danger ms-1">Closed ${spot.offDay}</span></c:if>
                                            <c:if test="${not empty spot.tip}"><br><small class="text-muted">${spot.tip}</small></c:if>
                                        </label>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <div class="st-card mb-4">
                        <div class="st-card-header"><i class="bi bi-building"></i> Hotel</div>
                        <div class="st-card-body">
                            <c:choose>
                                <c:when test="${not empty district.hotels}">
                                    <select name="hotelIndex" class="form-select st-form-select mb-3">
                                        <c:forEach var="hotel" items="${district.hotels}" varStatus="s">
                                            <option value="${s.index}">${hotel.name} — ${hotel.priceLabel}</option>
                                        </c:forEach>
                                    </select>
                                    <div class="d-flex gap-3">
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="acRoom" id="nonAc" value="false" checked>
                                            <label class="form-check-label" for="nonAc">Non-AC</label>
                                        </div>
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="acRoom" id="ac" value="true">
                                            <label class="form-check-label" for="ac">AC</label>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <input type="hidden" name="hotelIndex" value="-1">
                                    <div class="st-alert-info mb-0">
                                        ${not empty district.hotelNote ? district.hotelNote : 'No hotel listings. Arrange accommodation locally and verify prices before travel.'}
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <div class="col-lg-4">
                    <div class="st-card st-sticky-sidebar">
                        <div class="st-card-header"><i class="bi bi-sliders"></i> Trip Details</div>
                        <div class="st-card-body">
                            <div class="mb-3">
                                <label class="st-form-label">Travelers</label>
                                <input type="number" name="travelers" class="form-control st-form-control" value="4" min="1" max="20" required>
                            </div>
                            <div class="mb-4">
                                <label class="st-form-label">Nights</label>
                                <input type="number" name="nights" class="form-control st-form-control" value="1" min="1" max="30" required>
                            </div>
                            <div class="st-alert-info mb-4">
                                <small>
                                    Budget includes transport, hotel, local transport
                                    (<strong>${district.budgetDefaults.localTransportPerPerson} tk</strong>/person) and food
                                    (<strong>${district.budgetDefaults.foodPerPerson} tk</strong>/person).
                                </small>
                            </div>
                            <button type="submit" class="btn btn-st-accent w-100 py-2">
                                <i class="bi bi-lightning-charge me-1"></i> Generate Tour Plan
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</main>

<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
