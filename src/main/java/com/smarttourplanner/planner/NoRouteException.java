package com.smarttourplanner.planner;

public class NoRouteException extends RuntimeException {

    public NoRouteException(String fromHub, String toHub, int month) {
        super("No published route from %s to %s in month %d".formatted(fromHub, toHub, month));
    }
}
