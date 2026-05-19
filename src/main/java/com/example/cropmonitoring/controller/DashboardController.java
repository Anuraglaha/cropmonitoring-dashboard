package com.example.cropmonitoring.controller;

import com.example.cropmonitoring.model.NodeMcuData;
import com.example.cropmonitoring.repository.NodeMcuDataRepository;
import com.example.cropmonitoring.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to connect easily if served separately
public class DashboardController {

    private final SimulationService simulationService;
    private final NodeMcuDataRepository nodeMcuDataRepository;

    @Autowired
    public DashboardController(SimulationService simulationService, NodeMcuDataRepository nodeMcuDataRepository) {
        this.simulationService = simulationService;
        this.nodeMcuDataRepository = nodeMcuDataRepository;
    }

    // --- AXON 1: Dummy Simulation Stream ---
    @GetMapping("/axon1/stream")
    public SseEmitter streamSimulationData() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();

        sseMvcExecutor.execute(() -> {
            try {
                while (true) {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .data(simulationService.getSimulationState())
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name("axon1-data");
                    emitter.send(event);
                    Thread.sleep(2000); // Send updates every 2 seconds
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        emitter.onCompletion(sseMvcExecutor::shutdown);
        emitter.onTimeout(sseMvcExecutor::shutdown);

        return emitter;
    }

    // --- AXON 2: Real Data Stream ---
    @GetMapping("/axon2/stream")
    public SseEmitter streamLiveMcuData() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();

        sseMvcExecutor.execute(() -> {
            try {
                while (true) {
                    NodeMcuData latestData = nodeMcuDataRepository.findTopByOrderByTimestampDesc();
                    if (latestData != null) {
                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                                .data(latestData)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .name("axon2-data");
                        emitter.send(event);
                    }
                    Thread.sleep(2000); // Check and send updates every 2 seconds
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        emitter.onCompletion(sseMvcExecutor::shutdown);
        emitter.onTimeout(sseMvcExecutor::shutdown);

        return emitter;
    }

    // --- API to accept NodeMCU POST data ---
    @PostMapping("/axon2/data")
    public ResponseEntity<String> receiveMcuData(@RequestBody NodeMcuData data) {
        if (data.getTimestamp() == null) {
            data.setTimestamp(LocalDateTime.now());
        }
        nodeMcuDataRepository.save(data);
        return ResponseEntity.ok("Data saved successfully");
    }

    // --- API to view all data saved in AWS ---
    @GetMapping("/axon2/history")
    public ResponseEntity<java.util.List<NodeMcuData>> getAllSavedData() {
        return ResponseEntity.ok(nodeMcuDataRepository.findAll());
    }
}
