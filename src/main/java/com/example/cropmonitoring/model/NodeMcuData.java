package com.example.cropmonitoring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "node_mcu_data")
@Data
@NoArgsConstructor
public class NodeMcuData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double temperature;
    private double soilMoisture;
    private double distance; // Object detection distance

    private LocalDateTime timestamp;

    public NodeMcuData(double temperature, double soilMoisture, double distance, LocalDateTime timestamp) {
        this.temperature = temperature;
        this.soilMoisture = soilMoisture;
        this.distance = distance;
        this.timestamp = timestamp;
    }
}
