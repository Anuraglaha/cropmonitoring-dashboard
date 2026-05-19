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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to connect easily if served separately
public class DashboardController {

    private final CopyOnWriteArrayList<SseEmitter> axon2Emitters = new CopyOnWriteArrayList<>();

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
        axon2Emitters.add(emitter);

        emitter.onCompletion(() -> axon2Emitters.remove(emitter));
        emitter.onTimeout(() -> axon2Emitters.remove(emitter));
        emitter.onError((e) -> axon2Emitters.remove(emitter));

        // Send the current latest data immediately upon connection
        NodeMcuData latestData = nodeMcuDataRepository.findTopByOrderByTimestampDesc();
        if (latestData != null) {
            try {
                emitter.send(SseEmitter.event().data(latestData).id(String.valueOf(System.currentTimeMillis())).name("axon2-data"));
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    // --- API to accept NodeMCU POST data ---
    @PostMapping("/axon2/data")
    public ResponseEntity<String> receiveMcuData(@RequestBody NodeMcuData data) {
        if (data.getTimestamp() == null) {
            data.setTimestamp(LocalDateTime.now());
        }
        NodeMcuData savedData = nodeMcuDataRepository.save(data);

        // Broadcast the new data to all connected dashboard clients instantly
        for (SseEmitter emitter : axon2Emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .data(savedData)
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("axon2-data"));
            } catch (Exception ex) {
                axon2Emitters.remove(emitter);
            }
        }

        return ResponseEntity.ok("Data saved successfully");
    }

    // --- API to view all data saved in AWS ---
    @GetMapping("/axon2/history")
    public ResponseEntity<java.util.List<NodeMcuData>> getAllSavedData() {
        return ResponseEntity.ok(nodeMcuDataRepository.findAll());
    }
}
