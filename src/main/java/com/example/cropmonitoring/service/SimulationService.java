package com.example.cropmonitoring.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class SimulationService {

    private final Random random = new Random();

    private int activeRobots = 1;
    private double temperature = 24.5;
    private double humidity = 60.0;
    private double battery = 100.0;
    private double soilMoisture = 45.0;

    private String robotStatus = "Scouting the canopy";
    private double missionProgress = 0.0;
    
    private double coordX = 0.0;
    private double coordY = 0.0;

    // Leaf inspection
    private boolean isLeafHealthy = true;
    private double leafConfidence = 95.0;
    private String leafScanDetails = "Normal chlorophyll levels";

    @Data
    public static class SimulationData {
        private int activeRobots;
        private double temperature;
        private double humidity;
        private double battery;
        private double soilMoisture;
        private String robotStatus;
        private double missionProgress;
        private double coordX;
        private double coordY;
        private boolean isLeafHealthy;
        private double leafConfidence;
        private String leafScanDetails;
        private LocalDateTime timestamp;
    }

    public SimulationData getSimulationState() {
        updateState();
        
        SimulationData data = new SimulationData();
        data.setActiveRobots(activeRobots);
        data.setTemperature(Math.round(temperature * 10.0) / 10.0);
        data.setHumidity(Math.round(humidity * 10.0) / 10.0);
        data.setBattery(Math.round(battery * 10.0) / 10.0);
        data.setSoilMoisture(Math.round(soilMoisture * 10.0) / 10.0);
        data.setRobotStatus(robotStatus);
        data.setMissionProgress(Math.round(missionProgress * 10.0) / 10.0);
        data.setCoordX(Math.round(coordX * 10.0) / 10.0);
        data.setCoordY(Math.round(coordY * 10.0) / 10.0);
        data.setLeafHealthy(isLeafHealthy);
        data.setLeafConfidence(Math.round(leafConfidence * 10.0) / 10.0);
        data.setLeafScanDetails(leafScanDetails);
        data.setTimestamp(LocalDateTime.now());
        
        return data;
    }

    private void updateState() {
        // Temperature and humidity variations
        temperature += (random.nextDouble() * 0.4) - 0.2;
        humidity += (random.nextDouble() * 1.0) - 0.5;
        soilMoisture += (random.nextDouble() * 0.6) - 0.3;

        if ("Charging".equals(robotStatus)) {
            battery += 25.0; // Charge in 4 steps (from ~20 to 100)
            if (battery >= 100.0) {
                battery = 100.0;
                robotStatus = "Scouting the canopy";
            }
        } else if ("Returning to charging station".equals(robotStatus)) {
            // Move back towards 0,0
            coordX = Math.max(0, coordX - 2.0);
            coordY = Math.max(0, coordY - 2.0);
            if (coordX <= 0.5 && coordY <= 0.5) {
                coordX = 0;
                coordY = 0;
                robotStatus = "Charging";
            }
        } else {
            // Active state
            battery -= 1.5;
            missionProgress += 2.0;
            coordX += (random.nextDouble() * 2.0) - 0.5;
            coordY += (random.nextDouble() * 2.0) - 0.5;

            // Randomly switch active states
            if (random.nextDouble() > 0.8) {
                robotStatus = random.nextBoolean() ? "Scouting the canopy" : "Sampling soil";
            }

            if (missionProgress >= 100.0) {
                missionProgress = 0.0;
            }

            if (battery < 20.0) {
                robotStatus = "Returning to charging station";
            }
        }

        // Randomize leaf inspection every few ticks roughly
        if (random.nextDouble() > 0.8) {
            isLeafHealthy = random.nextDouble() > 0.3; // 70% chance healthy
            if (isLeafHealthy) {
                leafConfidence = 70.0 + random.nextDouble() * 29.0;
                leafScanDetails = "Normal chlorophyll levels, optimal structure.";
            } else {
                leafConfidence = 60.0 + random.nextDouble() * 35.0;
                leafScanDetails = random.nextBoolean() ? "Signs of Early Blight detected." : "Possible nutrient deficiency.";
            }
        }
    }
}
