package com.smarttourplanner.service;

import com.smarttourplanner.model.TourPlan;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class TourPlanSessionService {

    public static final String SESSION_KEY = "currentTourPlan";

    public void savePlan(HttpSession session, TourPlan plan) {
        session.setAttribute(SESSION_KEY, plan);
    }

    public TourPlan getPlan(HttpSession session) {
        Object plan = session.getAttribute(SESSION_KEY);
        return plan instanceof TourPlan ? (TourPlan) plan : null;
    }

    public void clearPlan(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }
}
