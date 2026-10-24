package com.smartparking.notification;

import com.smartparking.model.SpotType;

import java.io.PrintStream;
import java.util.Map;
import java.util.StringJoiner;

public class DisplayBoard implements AvailabilityListener {

    private final PrintStream out;

    public DisplayBoard(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onAvailabilityChanged(Map<SpotType, Integer> available) {
        StringJoiner line = new StringJoiner(" | ", "[Board] ", "");
        available.forEach((type, count) -> line.add(type + ": " + count));
        out.println(line);
    }
}
