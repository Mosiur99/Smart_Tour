#!/usr/bin/env python3
"""Sync district JSON files from bangladesh_64_district_tourism_dataset_v4_with_transport_costs.json."""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "bangladesh_64_district_tourism_dataset_v4_with_transport_costs.json")
OUT_DIR = os.path.join(ROOT, "src/main/resources/data/districts")
INDEX_PATH = os.path.join(ROOT, "src/main/resources/data/districts-index.json")

RAJSHAHI_ENRICHMENT = {
    "extraTransportFromDhaka": [{
        "mode": "train", "name": "Banalata Express", "departureTime": "13:30",
        "ticketBookingNote": "Ticket katar somoy", "costPerPerson": 450, "note": "night journey"
    }],
    "extraTransportReturn": [{
        "mode": "train", "name": "Dhumketu Express", "departureTime": "23:30",
        "ticketBookingNote": "Ticket katar somoy", "costPerPerson": 495, "note": ""
    }],
    "extraHotels": [
        {"name": "Hotel Swiss International", "acPrice": 1800, "nonAcPrice": 1000, "review": "Review eto valo na"},
        {"name": "Hotel Nice International", "acPrice": 2200, "nonAcPrice": 1500, "review": "Valo maner hotel"},
        {"name": "Laxmipur Mor (Mintu Chattar) Area Hotels", "acPrice": 800, "nonAcPrice": 300,
         "review": "Ekhan kom damer onk gulo avashik hotel pawoa jay", "location": "Laxmipur Mor (Mintu Chattar)"}
    ],
    "tourGuideUrl": "https://youtu.be/BMW19WuApt0"
}


def clean_transport_option(opt):
    result = {"mode": opt.get("mode") or "", "name": opt.get("name") or "", "note": opt.get("note") or ""}
    if opt.get("departureTime"):
        result["departureTime"] = opt["departureTime"]
    if opt.get("ticketBookingNote"):
        result["ticketBookingNote"] = opt["ticketBookingNote"]
    if opt.get("costPerPerson") is not None:
        result["costPerPerson"] = opt["costPerPerson"]
    if opt.get("costVerificationStatus"):
        result["costVerificationStatus"] = opt["costVerificationStatus"]
    estimate = opt.get("estimatedCostPerPerson")
    if estimate and estimate.get("min") is not None and estimate.get("max") is not None:
        result["costEstimateNote"] = f"Est. {estimate['min']}-{estimate['max']} BDT (verify before booking)"
    return result


def clean_spot(spot):
    s = {"name": spot["name"], "area": spot.get("area", ""), "priority": spot.get("priority", 99)}
    if spot.get("offDay"):
        s["offDay"] = spot["offDay"]
    if spot.get("tip"):
        s["tip"] = spot["tip"]
    return s


def clean_hotel(hotel):
    if not hotel.get("name"):
        return None
    out = {"name": hotel["name"]}
    if hotel.get("acPrice") is not None:
        out["acPrice"] = hotel["acPrice"]
    if hotel.get("nonAcPrice") is not None:
        out["nonAcPrice"] = hotel["nonAcPrice"]
    if hotel.get("pricePerNight") is not None and "nonAcPrice" not in out and "acPrice" not in out:
        out["nonAcPrice"] = hotel["pricePerNight"]
    if hotel.get("review"):
        out["review"] = hotel["review"]
    location = hotel.get("location") or hotel.get("area")
    if location:
        out["location"] = location
    if hotel.get("note"):
        out["review"] = (out.get("review") or "") + (" " if out.get("review") else "") + hotel["note"]
    if hotel.get("website"):
        out["website"] = hotel["website"]
    return out


def clean_hotel_for_stay(hotel):
    out = {}
    for key in ("name", "type", "recommendedArea", "area", "currency", "verificationStatus", "note", "website"):
        if hotel.get(key):
            out[key if key != "area" else "recommendedArea"] = hotel[key]
    if hotel.get("pricePerNight") is not None:
        out["pricePerNight"] = hotel["pricePerNight"]
    return out


def clean_university(u):
    out = {"name": u["name"]}
    for key in ("type", "location", "note"):
        if u.get(key):
            out[key] = u[key]
    return out


def clean_bus_train_transport(btt):
    if not btt:
        return None
    out = {}
    if btt.get("bus"):
        out["bus"] = [clean_transport_option(o) for o in btt["bus"]]
    if btt.get("train"):
        out["train"] = [clean_transport_option(o) for o in btt["train"]]
    if btt.get("note"):
        out["note"] = btt["note"]
    return out if out else None


def merge_unique_transport(existing, extra):
    names = {t["name"].lower() for t in existing}
    merged = list(extra)
    for t in existing:
        if t["name"].lower() not in {e["name"].lower() for e in extra}:
            merged.append(t)
    return merged


def merge_unique_hotels(existing, extra):
    names = {h["name"].lower() for h in existing}
    merged = list(existing)
    for h in extra:
        if h["name"].lower() not in names:
            merged.append(h)
            names.add(h["name"].lower())
    return merged


def district_to_app_format(d):
    foods = d.get("popularFoods") or d.get("foods") or []
    hotels = [h for h in (clean_hotel(h) for h in d.get("hotels") or []) if h]
    out = {
        "id": d["id"],
        "name": d["name"],
        "division": d["division"],
        "transport": {
            "fromDhaka": [clean_transport_option(o) for o in d.get("transport", {}).get("fromDhaka", [])],
            "returnToDhaka": [clean_transport_option(o) for o in d.get("transport", {}).get("returnToDhaka", [])]
        },
        "spots": [clean_spot(s) for s in d.get("spots", [])],
        "hotels": hotels,
        "foods": [{"name": f["name"], "location": f.get("location") or ""} for f in foods],
        "budgetDefaults": {
            "localTransportPerPerson": d.get("budgetDefaults", {}).get("localTransportPerPerson", 1000),
            "foodPerPerson": d.get("budgetDefaults", {}).get("foodPerPerson", 1000)
        },
        "tourGuideUrl": d.get("tourGuideUrl") or "",
        "historicalPlaces": [clean_spot(s) for s in d.get("historicalPlaces", [])],
        "popularUniversities": [clean_university(u) for u in d.get("popularUniversities", [])],
        "hotelsForStay": [clean_hotel_for_stay(h) for h in d.get("hotelsForStay", [])]
    }
    btt = clean_bus_train_transport(d.get("busTrainTransport"))
    if btt:
        out["busTrainTransport"] = btt
    if d.get("hotelNote"):
        out["hotelNote"] = d["hotelNote"]
    if d.get("travelNote"):
        out["travelNote"] = d["travelNote"]
    if d.get("universityNote"):
        out["universityNote"] = d["universityNote"]
    return out


def enrich_rajshahi(district):
    district["transport"]["fromDhaka"] = merge_unique_transport(
        district["transport"]["fromDhaka"], RAJSHAHI_ENRICHMENT["extraTransportFromDhaka"])
    district["transport"]["returnToDhaka"] = merge_unique_transport(
        district["transport"]["returnToDhaka"], RAJSHAHI_ENRICHMENT["extraTransportReturn"])
    district["hotels"] = merge_unique_hotels(district["hotels"], RAJSHAHI_ENRICHMENT["extraHotels"])
    district["tourGuideUrl"] = RAJSHAHI_ENRICHMENT["tourGuideUrl"]
    for s in [
        {"name": "Usha Silk / Sopura Silk Mill", "tip": "best", "area": "city", "priority": 4},
        {"name": "Puthia Rajbari", "offDay": "Sunday", "tip": "Robibar off day", "area": "puthia", "priority": 6},
        {"name": "Sharif Tea Garden", "area": "outskirts", "priority": 10},
    ]:
        if s["name"].lower() not in {x["name"].lower() for x in district["spots"]}:
            district["spots"].append(s)
    district["spots"].sort(key=lambda x: x.get("priority", 99))
    for f in [
        {"name": "Rana Mishti Ghor", "location": "CNG Mor"},
        {"name": "Kala Vuna", "location": "Hanif Kala Vuna, Gorur Hat"},
        {"name": "Street Food", "location": "C & B Mor"}
    ]:
        if f["name"].lower() not in {x["name"].lower() for x in district["foods"]}:
            district["foods"].append(f)


def main():
    with open(SOURCE, encoding="utf-8") as f:
        data = json.load(f)

    os.makedirs(OUT_DIR, exist_ok=True)
    index = []
    for d in data["districts"]:
        district = district_to_app_format(d)
        if d["id"] == "rajshahi":
            enrich_rajshahi(district)
        path = os.path.join(OUT_DIR, f"{d['id']}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(district, f, indent=2, ensure_ascii=False)
            f.write("\n")
        has_detail = len(district["spots"]) > 0 or len(district.get("historicalPlaces", [])) > 0
        index.append({
            "id": d["id"],
            "name": d["name"],
            "division": d["division"],
            "hasDetailData": has_detail
        })

    with open(INDEX_PATH, "w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"Synced {len(index)} districts from {SOURCE}")


if __name__ == "__main__":
    main()
