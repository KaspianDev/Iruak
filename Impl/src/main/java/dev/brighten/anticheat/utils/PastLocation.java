package dev.brighten.anticheat.utils;

import cc.funkemunky.api.utils.KLocation;
import cc.funkemunky.api.utils.MathUtils;
import org.bukkit.Location;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PastLocation {
    public List<KLocation> previousLocations = new CopyOnWriteArrayList<>();

    public KLocation getPreviousLocation(long time) {
        return (this.previousLocations.stream()
                .min(Comparator.comparing(loc -> Math.abs(time - loc.timeStamp)))
                .orElse(this.previousLocations.get(0)));
    }

    public List<KLocation> getEstimatedLocation(long time, long delta) {
        return this.previousLocations
                .stream()
                .filter(loc -> time - loc.timeStamp < delta)
                .collect(Collectors.toList());
    }

    public List<KLocation> getPreviousRange(long delta) {
        long stamp = System.currentTimeMillis();

        return this.previousLocations.stream()
                .filter(loc -> stamp - loc.timeStamp < delta)
                .collect(Collectors.toList());
    }

    public void addLocation(Location location) {
        if (previousLocations.size() >= 20) {
            previousLocations.remove(0);
        }

        previousLocations.add(new KLocation(location));
    }

    public KLocation getLast() {
        if(previousLocations.size() == 0) return null;
        return previousLocations.get(previousLocations.size() - 1);
    }

    public KLocation getFirst() {
        if(previousLocations.size() == 0) return null;
        return previousLocations.get(0);
    }

    public void addLocation(KLocation location) {
        if (previousLocations.size() >= 20) {
            previousLocations.remove(0);
        }

        previousLocations.add(location.clone());
    }
}