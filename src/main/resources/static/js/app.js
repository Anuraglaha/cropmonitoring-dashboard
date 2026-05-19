document.addEventListener('DOMContentLoaded', () => {
    setupAxon1Stream();
    setupAxon2Stream();
});

let infectedCount = 0;
let previousLeafHealthy = true;

function setupAxon1Stream() {
    const source = new EventSource('/api/axon1/stream');

    source.addEventListener('axon1-data', function(event) {
        const data = JSON.parse(event.data);
        
        // 1. Overview Row
        document.getElementById('a1-robots').innerText = data.activeRobots;
        document.getElementById('a1-temp').innerText = data.temperature.toFixed(1) + '°C';
        document.getElementById('a1-humidity').innerText = data.humidity.toFixed(1) + '%';
        
        const batteryEl = document.getElementById('a1-battery');
        batteryEl.innerText = data.battery.toFixed(1) + '%';
        document.getElementById('a1-battery-bar').style.width = data.battery + '%';
        if (data.battery < 20) {
            batteryEl.style.color = 'var(--accent-danger)';
            document.getElementById('a1-battery-bar').style.background = 'var(--accent-danger)';
        } else {
            batteryEl.style.color = 'var(--text-main)';
            document.getElementById('a1-battery-bar').style.background = 'linear-gradient(90deg, var(--accent-green), var(--accent-teal))';
        }

        document.getElementById('a1-soil').innerText = data.soilMoisture.toFixed(1) + '%';

        // 2. Robot Status
        const statusText = document.getElementById('a1-status-text');
        statusText.innerText = data.robotStatus;
        if (data.robotStatus === 'Charging') {
            statusText.style.color = 'var(--accent-warning)';
            document.querySelector('.pulse-dot').style.backgroundColor = 'var(--accent-warning)';
        } else if (data.robotStatus === 'Returning to charging station') {
            statusText.style.color = 'var(--accent-danger)';
            document.querySelector('.pulse-dot').style.backgroundColor = 'var(--accent-danger)';
        } else {
            statusText.style.color = 'var(--text-main)';
            document.querySelector('.pulse-dot').style.backgroundColor = 'var(--accent-green)';
        }

        document.getElementById('a1-mission').innerText = data.missionProgress.toFixed(1) + '%';
        document.getElementById('a1-mission-bar').style.width = data.missionProgress + '%';

        // 3. Field Map
        document.getElementById('a1-coord-x').innerText = data.coordX.toFixed(1);
        document.getElementById('a1-coord-y').innerText = data.coordY.toFixed(1);
        
        // Map visual update (assuming map is ~150x150, coords range ~0-20 logically)
        // Convert to percentage for position
        const mapSize = 20; // assumed max boundary
        const xPos = Math.min(Math.max((data.coordX / mapSize) * 100, 0), 90);
        const yPos = Math.min(Math.max((data.coordY / mapSize) * 100, 0), 90);
        
        const dot = document.getElementById('a1-robot-dot');
        // We add 15px offset to match the charging station origin roughly
        dot.style.left = `calc(15px + ${xPos}%)`;
        dot.style.bottom = `calc(15px + ${yPos}%)`;

        // 4. Leaf Preview
        const leafImg = document.getElementById('a1-leaf-img');
        const leafStatus = document.getElementById('a1-leaf-status');
        
        if (data.leafHealthy) {
            leafImg.src = 'images/healthy_leaf.png';
            leafStatus.innerText = 'Healthy';
            leafStatus.className = 'leaf-status';
            previousLeafHealthy = true;
        } else {
            leafImg.src = 'images/diseased_leaf.png';
            leafStatus.innerText = 'Infected';
            leafStatus.className = 'leaf-status infected';
            
            if (previousLeafHealthy) {
                infectedCount++;
                document.getElementById('infected-count').innerText = '+' + infectedCount;
                previousLeafHealthy = false;
            }
        }
        
        document.getElementById('a1-leaf-conf').innerText = data.leafConfidence.toFixed(1) + '%';
        document.getElementById('a1-leaf-desc').innerText = data.leafScanDetails;
        
    }, false);

    source.onerror = function(error) {
        console.error("SSE Axon 1 Error", error);
    };
}

function setupAxon2Stream() {
    const source = new EventSource('/api/axon2/stream');

    source.addEventListener('axon2-data', function(event) {
        const data = JSON.parse(event.data);
        
        document.getElementById('a2-temp').innerText = data.temperature.toFixed(1) + '°C';
        document.getElementById('a2-soil').innerText = data.soilMoisture.toFixed(1) + '%';
        document.getElementById('a2-dist').innerText = data.distance.toFixed(1) + ' cm';
        
        const date = new Date(data.timestamp);
        document.getElementById('a2-timestamp').innerText = date.toLocaleString();
        
    }, false);

    source.onerror = function(error) {
        console.error("SSE Axon 2 Error", error);
    };
}
