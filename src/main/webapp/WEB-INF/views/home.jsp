<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:set var="pageTitle" value="SmartTour — Plan Smart, Travel More"/>
    <jsp:include page="fragments/head.jsp"/>
</head>
<body>
<jsp:include page="fragments/header.jsp"/>

<section class="st-hero st-hero-home">
    <div class="container position-relative">
        <div class="row align-items-center">
            <div class="col-lg-7">
                <h1>Plan Your Bangladesh Tour</h1>
                <p>Select origin and destination to build a complete tour plan with transport, spots, hotels, and budget.</p>
                <div class="st-tagline">Plan Smart, Travel More</div>
                <div class="st-stats">
                    <span class="st-stat"><i class="bi bi-geo-alt-fill"></i> 64 Districts</span>
                    <span class="st-stat"><i class="bi bi-cash-coin"></i> Auto Budget</span>
                    <span class="st-stat"><i class="bi bi-file-earmark-pdf"></i> PDF Export</span>
                </div>
            </div>
            <div class="col-lg-5 d-none d-lg-block text-center">
                <img src="/images/logo.png" alt="SmartTour" style="max-height:180px; filter:drop-shadow(0 8px 24px rgba(0,0,0,0.2));">
            </div>
        </div>
    </div>
</section>

<main class="st-main st-main-overlap">
    <div class="container pb-5">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="st-card st-card-overlap">
                    <div class="st-card-header">
                        <i class="bi bi-compass"></i> Start Planning
                    </div>
                    <div class="st-card-body p-4">
                        <form action="/district/" method="get" id="districtForm">
                            <div class="mb-4">
                                <label for="origin" class="st-form-label">Origin City</label>
                                <select name="origin" id="origin" class="form-select st-form-select">
                                    <c:forEach var="city" items="${originCities}">
                                        <option value="${city}" ${city == 'Dhaka' ? 'selected' : ''}>${city}</option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-4">
                                <label for="division" class="st-form-label">Filter by Division</label>
                                <select id="division" class="form-select st-form-select" onchange="filterDivision(this.value)">
                                    <option value="">All Divisions</option>
                                    <c:forEach var="divisionItem" items="${divisions}">
                                        <option value="${divisionItem}" ${divisionItem == selectedDivision ? 'selected' : ''}>${divisionItem}</option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-4">
                                <label for="districtId" class="st-form-label">Destination District</label>
                                <select name="id" id="districtId" class="form-select st-form-select" required>
                                    <option value="">— Select District —</option>
                                    <c:forEach var="d" items="${districts}">
                                        <option value="${d.id}" data-division="${d.division}">
                                            ${d.name} (${d.division})<c:if test="${d.hasDetailData}"> ★</c:if>
                                        </option>
                                    </c:forEach>
                                </select>
                                <div class="st-form-hint"><i class="bi bi-star-fill text-warning"></i> = detailed tour data available</div>
                            </div>

                            <div class="d-flex flex-wrap gap-2">
                                <button type="button" class="btn btn-st-secondary" onclick="viewDistrict()">
                                    <i class="bi bi-eye me-1"></i> View District
                                </button>
                                <button type="button" class="btn btn-st-accent" onclick="startPlanning()">
                                    <i class="bi bi-arrow-right-circle me-1"></i> Start Planning
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<jsp:include page="fragments/footer.jsp"/>

<script>
    function getDistrictId() { return document.getElementById('districtId').value; }
    function getOrigin() { return document.getElementById('origin').value; }
    function viewDistrict() {
        const id = getDistrictId();
        if (!id) { alert('Please select a district'); return; }
        window.location.href = '/district/' + id;
    }
    function startPlanning() {
        const id = getDistrictId();
        if (!id) { alert('Please select a district'); return; }
        window.location.href = '/plan/build/' + id + '?origin=' + encodeURIComponent(getOrigin());
    }
    function filterDivision(division) {
        window.location.href = division ? '/?division=' + encodeURIComponent(division) : '/';
    }
</script>
</body>
</html>
