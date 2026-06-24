document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("forecastForm");
    const statusEl = document.getElementById("status");
    const resultsEl = document.getElementById("results");
    const rawJsonEl = document.getElementById("rawJson");
    const submitBtn = form.querySelector("button[type=submit]");
    const resultsWrapper = document.querySelector(".results-wrapper");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const navInfo = document.getElementById("navInfo");

    let currentIndex = 0;
    let totalCards = 0;

    function updateNavigation() {
        if (totalCards === 0) {
            resultsWrapper.style.display = "none";
            return;
        }
        
        resultsWrapper.style.display = "block";
        navInfo.textContent = `${currentIndex + 1} / ${totalCards}`;
        prevBtn.disabled = currentIndex === 0;
        nextBtn.disabled = currentIndex === totalCards - 1;

        const cards = resultsEl.querySelectorAll(".location-card");
        cards.forEach((card, idx) => {
            card.style.transform = `translateX(-${currentIndex * 100}%)`;
        });
    }

    function navigateTo(index) {
        if (index < 0 || index >= totalCards) return;
        currentIndex = index;
        updateNavigation();
    }

    prevBtn.addEventListener("click", () => navigateTo(currentIndex - 1));
    nextBtn.addEventListener("click", () => navigateTo(currentIndex + 1));

    // Keyboard navigation
    document.addEventListener("keydown", (e) => {
        if (totalCards === 0) return;
        if (e.key === "ArrowLeft") navigateTo(currentIndex - 1);
        if (e.key === "ArrowRight") navigateTo(currentIndex + 1);
    });

    // Touch swipe support
    let touchStartX = 0;
    let touchEndX = 0;

    resultsEl.addEventListener("touchstart", (e) => {
        touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });

    resultsEl.addEventListener("touchend", (e) => {
        touchEndX = e.changedTouches[0].screenX;
        handleSwipe();
    }, { passive: true });

    function handleSwipe() {
        const swipeThreshold = 50;
        const diff = touchStartX - touchEndX;
        
        if (Math.abs(diff) > swipeThreshold) {
            if (diff > 0) {
                navigateTo(currentIndex + 1);
            } else {
                navigateTo(currentIndex - 1);
            }
        }
    }

    function toLocalInputValue(date) {
        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
        return local.toISOString().slice(0, 16);
    }

    function toIsoString(value) {
        if (!value) {
            return "";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }
        return date.toISOString();
    }

    function initDefaults() {
        const start = new Date();
        start.setMinutes(0, 0, 0);

        const end = new Date(start);
        end.setDate(end.getDate() + 7);

        document.getElementById("timeFrom").value = toLocalInputValue(start);
        document.getElementById("timeTo").value = toLocalInputValue(end);
    }

    function createTimeEntry(timeInfo) {
        const div = document.createElement("div");
        div.className = "time-entry";

        const { startTime, endTime, parameter } = timeInfo;
        const lines = [];
        if (startTime || endTime) {
            lines.push(`時段：${startTime ?? "-"} → ${endTime ?? "-"}`);
        }
        if (parameter) {
            const value = parameter.parameterName ?? parameter.parameterValue ?? "-";
            const unit = parameter.parameterUnit ? ` ${parameter.parameterUnit}` : "";
            lines.push(`內容：${value}${unit}`);
        }
        div.textContent = lines.join("\n");
        return div;
    }

    function renderResults(payload) {
        resultsEl.innerHTML = "";
        currentIndex = 0;
        totalCards = 0;

        if (!payload || !payload.records) {
            statusEl.textContent = "查無資料";
            updateNavigation();
            return;
        }

        const { location } = payload.records;

        if (!Array.isArray(location) || location.length === 0) {
            statusEl.textContent = "沒有符合條件的地區資料";
            updateNavigation();
            return;
        }

        totalCards = location.length;

        location.forEach(loc => {
            const card = document.createElement("div");
            card.className = "location-card";

            const title = document.createElement("h2");
            title.textContent = loc.locationName ?? "未命名地區";
            card.appendChild(title);

            const elements = Array.isArray(loc.weatherElement) ? loc.weatherElement : [];
            elements.forEach(element => {
                const block = document.createElement("div");
                block.className = "element-block";

                const heading = document.createElement("h3");
                heading.textContent = element.elementName ?? "未知元素";
                block.appendChild(heading);

                const times = Array.isArray(element.time) ? element.time : [];
                if (times.length === 0) {
                    const none = document.createElement("p");
                    none.textContent = "無時間段資料";
                    block.appendChild(none);
                } else {
                    times.forEach(info => block.appendChild(createTimeEntry(info)));
                }

                card.appendChild(block);
            });

            resultsEl.appendChild(card);
        });

        updateNavigation();
    }

    async function fetchForecast(query) {
        const response = await fetch(`/api/weather/forecast?${query}`, {
            headers: {
                "accept": "application/json"
            }
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `HTTP ${response.status}`);
        }

        return response.json();
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const params = new URLSearchParams();
        const timeFromIso = toIsoString(form.timeFrom.value);
        const timeToIso = toIsoString(form.timeTo.value);

        if (timeFromIso) {
            params.append("timeFrom", timeFromIso);
        }
        if (timeToIso) {
            params.append("timeTo", timeToIso);
        }

        ["locationName", "elementName", "limit", "offset"].forEach(key => {
            const value = form[key].value.trim();
            if (value) {
                params.append(key, value);
            }
        });

        statusEl.textContent = "查詢中...";
        submitBtn.disabled = true;
        resultsEl.innerHTML = "";
        rawJsonEl.textContent = "等待回應...";

        try {
            const payload = await fetchForecast(params.toString());
            rawJsonEl.textContent = JSON.stringify(payload, null, 2);
            renderResults(payload);
            const count = payload?.records?.location?.length ?? 0;
            statusEl.textContent = `查詢成功，共 ${count} 個地區。`;
        } catch (error) {
            statusEl.textContent = `查詢失敗：${error.message}`;
            rawJsonEl.textContent = error.stack ?? error.message;
        } finally {
            submitBtn.disabled = false;
        }
    });

    initDefaults();
});
