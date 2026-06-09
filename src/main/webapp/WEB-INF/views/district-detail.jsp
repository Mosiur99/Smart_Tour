<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:set var="pageTitle" value="${district.name} — SmartTour"/>
    <jsp:include page="fragments/head.jsp"/>
</head>
<body>
<jsp:include page="fragments/header.jsp"/>

<section class="st-hero st-hero-compact">
    <div class="container">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb st-breadcrumb mb-2">
                <li class="breadcrumb-item"><a href="/">Home</a></li>
                <li class="breadcrumb-item active text-white opacity-75">${district.name}</li>
            </ol>
        </nav>
        <h1>${district.name}</h1>
        <p class="mb-0">${district.division} Division &mdash; Explore spots, transport, food &amp; hotels</p>
    </div>
</section>

<main class="st-main">
    <div class="container pb-5">
        <div class="st-page-header">
            <div></div>
            <a href="/plan/build/${district.id}" class="btn btn-st-accent">
                <i class="bi bi-map me-1"></i> Build Tour Plan
            </a>
        </div>

        <c:if test="${not empty district.travelNote}">
            <div class="st-alert-warn mb-4"><i class="bi bi-exclamation-triangle me-2"></i>${district.travelNote}</div>
        </c:if>

        <div class="row g-4">
            <div class="col-md-6">
                <div class="st-card h-100">
                    <div class="st-card-header"><i class="bi bi-bus-front"></i> Transport from Dhaka</div>
                    <div class="p-0">
                        <c:forEach var="t" items="${district.transport.fromDhaka}">
                            <div class="st-list-item">
                                <div class="d-flex justify-content-between align-items-start gap-2">
                                    <div>
                                        <strong>${t.name}</strong>
                                        <span class="st-mode-pill ms-1">${t.mode}</span>
                                    </div>
                                    <c:if test="${t.costPerPerson != null}">
                                        <span class="st-badge-teal">${t.costPerPerson} tk</span>
                                    </c:if>
                                </div>
                                <small class="text-muted d-block mt-1">
                                    Departure: ${not empty t.departureTime ? t.departureTime : 'TBD'}
                                    <c:if test="${t.costPerPerson == null}"> &middot; Fare TBD</c:if>
                                </small>
                                <c:if test="${not empty t.note}"><small class="d-block mt-1">${t.note}</small></c:if>
                            </div>
                        </c:forEach>
                        <c:if test="${empty district.transport.fromDhaka}">
                            <div class="st-list-item text-muted">No transport data</div>
                        </c:if>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="st-card h-100">
                    <div class="st-card-header"><i class="bi bi-arrow-return-left"></i> Return to Dhaka</div>
                    <div class="p-0">
                        <c:forEach var="t" items="${district.transport.returnToDhaka}">
                            <div class="st-list-item">
                                <div class="d-flex justify-content-between align-items-start gap-2">
                                    <div>
                                        <strong>${t.name}</strong>
                                        <span class="st-mode-pill ms-1">${t.mode}</span>
                                    </div>
                                    <c:if test="${t.costPerPerson != null}">
                                        <span class="st-badge-teal">${t.costPerPerson} tk</span>
                                    </c:if>
                                </div>
                                <small class="text-muted d-block mt-1">
                                    Departure: ${not empty t.departureTime ? t.departureTime : 'TBD'}
                                </small>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <div class="col-12">
                <div class="st-card">
                    <div class="st-card-header"><i class="bi bi-camera"></i> Tourist Spots</div>
                    <div class="st-card-body">
                        <div class="row g-3">
                            <c:forEach var="spot" items="${district.spots}">
                                <div class="col-md-4">
                                    <div class="st-spot-tile">
                                        <strong>${spot.name}</strong>
                                        <div class="mt-1">
                                            <c:if test="${not empty spot.area}"><span class="st-badge-navy">${spot.area}</span></c:if>
                                            <c:if test="${not empty spot.offDay}"><span class="st-badge-danger ms-1">Closed ${spot.offDay}</span></c:if>
                                        </div>
                                        <c:if test="${not empty spot.tip}"><p class="small text-muted mb-0 mt-2">${spot.tip}</p></c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>

            <c:if test="${not empty district.historicalPlaces}">
                <div class="col-12">
                    <div class="st-card">
                        <div class="st-card-header"><i class="bi bi-bank"></i> Historical Places</div>
                        <div class="st-card-body">
                            <div class="row g-3">
                                <c:forEach var="place" items="${district.historicalPlaces}">
                                    <div class="col-md-4">
                                        <div class="st-spot-tile">
                                            <strong>${place.name}</strong>
                                            <c:if test="${not empty place.area}"><span class="st-badge-navy ms-1">${place.area}</span></c:if>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>

            <c:if test="${not empty district.popularUniversities}">
                <div class="col-12">
                    <div class="st-card">
                        <div class="st-card-header"><i class="bi bi-mortarboard"></i> Popular Universities</div>
                        <div class="st-card-body">
                            <c:if test="${not empty district.universityNote}">
                                <p class="small text-muted mb-3">${district.universityNote}</p>
                            </c:if>
                            <div class="row g-2">
                                <c:forEach var="uni" items="${district.popularUniversities}">
                                    <div class="col-md-6">
                                        <div class="st-spot-tile">
                                            <strong>${uni.name}</strong>
                                            <c:if test="${not empty uni.type}"><span class="st-badge-teal ms-1">${uni.type}</span></c:if>
                                            <c:if test="${not empty uni.location}"><br><small class="text-muted">${uni.location}</small></c:if>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>

            <div class="col-md-6">
                <div class="st-card h-100">
                    <div class="st-card-header"><i class="bi bi-building"></i> Hotels</div>
                    <div class="p-0">
                        <c:forEach var="hotel" items="${district.hotels}">
                            <div class="st-list-item">
                                <strong>${hotel.name}</strong>
                                <div class="mt-1"><span class="st-badge-teal">${hotel.priceLabel}</span></div>
                                <c:if test="${not empty hotel.review}"><small class="text-muted d-block mt-1">${hotel.review}</small></c:if>
                                <c:if test="${not empty hotel.location}"><small class="d-block">${hotel.location}</small></c:if>
                                <c:if test="${not empty hotel.website}"><a href="${hotel.website}" target="_blank" rel="noopener" class="small">Official listing <i class="bi bi-box-arrow-up-right"></i></a></c:if>
                            </div>
                        </c:forEach>
                        <c:if test="${empty district.hotels}">
                            <c:forEach var="stay" items="${district.hotelsForStay}">
                                <div class="st-list-item">
                                    <strong>${stay.displayName}</strong>
                                    <c:if test="${not empty stay.note}"><br><small class="text-muted">${stay.note}</small></c:if>
                                </div>
                            </c:forEach>
                            <c:if test="${empty district.hotelsForStay}">
                                <div class="st-list-item text-muted">${not empty district.hotelNote ? district.hotelNote : 'Verify local accommodation before travel.'}</div>
                            </c:if>
                        </c:if>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="st-card h-100">
                    <div class="st-card-header"><i class="bi bi-cup-hot"></i> Khabar (Food)</div>
                    <div class="p-0">
                        <c:forEach var="food" items="${district.foods}">
                            <div class="st-list-item">
                                <i class="bi bi-dot text-teal"></i> ${food.name}
                                <c:if test="${not empty food.location}"><br><small class="text-muted ms-3">${food.location}</small></c:if>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <c:if test="${not empty district.tourGuideUrl}">
                <div class="col-12">
                    <div class="st-card">
                        <div class="st-card-body d-flex align-items-center gap-3">
                            <i class="bi bi-youtube fs-3 text-danger"></i>
                            <div>
                                <strong>Tour Guide Video</strong><br>
                                <a href="${district.tourGuideUrl}" target="_blank" rel="noopener">${district.tourGuideUrl}</a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
        </div>
    </div>
</main>

<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
