document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("forecastForm");
    const statusEl = document.getElementById("status");
    const resultsEl = document.getElementById("results");
    const rawJsonEl = document.getElementById("rawJson");
    const submitBtn = form.querySelector("button[type=submit]");
    const resultsWrapper = document.querySelector(".results-wrapper");
    
    // Carousel controls
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const navInfo = document.getElementById("navInfo");
    const carouselControls = document.getElementById("carouselControls");

    // Mode Toggle controls
    const btnCarouselMode = document.getElementById("btnCarouselMode");
    const btnGridMode = document.getElementById("btnGridMode");

    let currentIndex = 0;
    let totalCards = 0;
    let viewMode = "carousel"; // "carousel" or "grid"

    function updateNavigation() {
        if (totalCards === 0) {
            resultsWrapper.style.display = "none";
            return;
        }
        
        resultsWrapper.style.display = "block";
        
        if (viewMode === "carousel") {
            navInfo.textContent = `${currentIndex + 1} / ${totalCards}`;
            prevBtn.disabled = currentIndex === 0;
            nextBtn.disabled = currentIndex === totalCards - 1;

            const cards = resultsEl.querySelectorAll(".location-card");
            cards.forEach((card, idx) => {
                card.style.transform = `translateX(-${currentIndex * 100}%)`;
            });
        }
    }

    function navigateTo(index) {
        if (viewMode !== "carousel") return;
        if (index < 0 || index >= totalCards) return;
        currentIndex = index;
        updateNavigation();
    }

    prevBtn.addEventListener("click", () => navigateTo(currentIndex - 1));
    nextBtn.addEventListener("click", () => navigateTo(currentIndex + 1));

    // View Mode toggling
    function setViewMode(mode) {
        viewMode = mode;
        if (mode === "carousel") {
            btnCarouselMode.classList.add("active");
            btnGridMode.classList.remove("active");
            resultsEl.classList.remove("view-grid");
            resultsEl.classList.add("view-carousel");
            carouselControls.style.opacity = "1";
            carouselControls.style.pointerEvents = "auto";
            
            // Restore card translations
            const cards = resultsEl.querySelectorAll(".location-card");
            cards.forEach((card) => {
                card.style.transform = `translateX(-${currentIndex * 100}%)`;
            });
            updateNavigation();
        } else {
            btnGridMode.classList.add("active");
            btnCarouselMode.classList.remove("active");
            resultsEl.classList.remove("view-carousel");
            resultsEl.classList.add("view-grid");
            carouselControls.style.opacity = "0.3";
            carouselControls.style.pointerEvents = "none";
            
            // Reset translation styles to let CSS Grid layout flow naturally
            const cards = resultsEl.querySelectorAll(".location-card");
            cards.forEach((card) => {
                card.style.transform = "none";
            });
            navInfo.textContent = `All / ${totalCards}`;
        }
    }

    btnCarouselMode.addEventListener("click", () => setViewMode("carousel"));
    btnGridMode.addEventListener("click", () => setViewMode("grid"));

    // Keyboard navigation
    document.addEventListener("keydown", (e) => {
        if (totalCards === 0 || viewMode !== "carousel") return;
        if (e.key === "ArrowLeft") navigateTo(currentIndex - 1);
        if (e.key === "ArrowRight") navigateTo(currentIndex + 1);
    });

    // Touch swipe support
    let touchStartX = 0;
    let touchEndX = 0;

    resultsEl.addEventListener("touchstart", (e) => {
        if (viewMode !== "carousel") return;
        touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });

    resultsEl.addEventListener("touchend", (e) => {
        if (viewMode !== "carousel") return;
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
        end.setDate(end.getDate() + 3); // 氣象預報一般顯示 3 天比較合適

        document.getElementById("timeFrom").value = toLocalInputValue(start);
        document.getElementById("timeTo").value = toLocalInputValue(end);
    }

    // 動態配對天氣圖示
    function getWeatherIconClass(wxText) {
        if (!wxText) return "fa-cloud icon-cloud";
        if (wxText.includes("雷")) return "fa-bolt icon-thunder";
        if (wxText.includes("雨") || wxText.includes("陣雨") || wxText.includes("驟雨")) {
            return "fa-cloud-showers-heavy icon-rain";
        }
        if (wxText.includes("晴") && (wxText.includes("多雲") || wxText.includes("陰"))) {
            return "fa-cloud-sun icon-sun";
        }
        if (wxText.includes("晴")) return "fa-sun icon-sun";
        if (wxText.includes("雪")) return "fa-snowflake icon-snow";
        return "fa-cloud icon-cloud";
    }

    function createTimeEntry(timeInfo, elementName) {
        const div = document.createElement("div");
        div.className = "time-entry";

        const { startTime, endTime, parameter } = timeInfo;
        
        // 格式化時間，以提高易讀性
        const formatTime = (timeStr) => {
            if (!timeStr) return "-";
            const d = new Date(timeStr);
            if (Number.isNaN(d.getTime())) return timeStr;
            return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
        };

        const timeLabel = document.createElement("span");
        timeLabel.className = "time-label";
        timeLabel.textContent = `時段：${formatTime(startTime)} ➔ ${formatTime(endTime)}`;
        div.appendChild(timeLabel);

        const infoWrapper = document.createElement("div");
        infoWrapper.className = "weather-info-wrapper";

        const valueGroup = document.createElement("div");
        valueGroup.className = "weather-value-group";

        const value = parameter ? (parameter.parameterName ?? parameter.parameterValue ?? "-") : "-";
        const unit = (parameter && parameter.parameterUnit) ? ` ${parameter.parameterUnit}` : "";

        // 依不同氣象要素呈現視覺效果
        let iconHtml = "";
        let visualHtml = "";
        let hasProgressBar = false;
        let progressVal = 0;

        if (elementName === "Wx") {
            const iconClass = getWeatherIconClass(value);
            iconHtml = `<i class="fas ${iconClass} weather-icon"></i>`;
            visualHtml = `<span class="value-text">${value}</span>`;
        } else if (elementName === "PoP") {
            iconHtml = `<i class="fas fa-umbrella weather-icon icon-pop"></i>`;
            visualHtml = `<span class="value-text">降雨機率 ${value}%</span>`;
            
            const popVal = parseInt(value, 10);
            if (!Number.isNaN(popVal)) {
                hasProgressBar = true;
                progressVal = popVal;
            }
        } else if (elementName === "MinT") {
            iconHtml = `<i class="fas fa-temperature-low weather-icon icon-temp-low"></i>`;
            visualHtml = `<span class="value-text">最低溫 ${value}°C</span>`;
        } else if (elementName === "MaxT") {
            iconHtml = `<i class="fas fa-temperature-high weather-icon icon-temp-high"></i>`;
            visualHtml = `<span class="value-text">最高溫 ${value}°C</span>`;
        } else {
            iconHtml = `<i class="fas fa-info-circle weather-icon" style="color: #94a3b8;"></i>`;
            visualHtml = `<span class="value-text">${value}${unit}</span>`;
        }

        valueGroup.innerHTML = `${iconHtml} ${visualHtml}`;
        infoWrapper.appendChild(valueGroup);
        div.appendChild(infoWrapper);

        if (hasProgressBar) {
            const popContainer = document.createElement("div");
            popContainer.className = "pop-container";
            popContainer.innerHTML = `
                <div class="pop-progress-bar">
                    <div class="pop-progress-fill" style="width: 0%"></div>
                </div>
            `;
            div.appendChild(popContainer);
            
            // 延遲載入進度條動畫
            setTimeout(() => {
                const fill = popContainer.querySelector(".pop-progress-fill");
                if (fill) fill.style.width = `${progressVal}%`;
            }, 100);
        }

        return div;
    }

    function renderResults(payload) {
        resultsEl.innerHTML = "";
        currentIndex = 0;
        totalCards = 0;

        if (!payload || !payload.records) {
            statusEl.textContent = "查無氣象資料";
            updateNavigation();
            return;
        }

        const { location } = payload.records;

        if (!Array.isArray(location) || location.length === 0) {
            statusEl.textContent = "沒有符合條件的地區氣象資料";
            updateNavigation();
            return;
        }

        totalCards = location.length;

        location.forEach((loc, index) => {
            const card = document.createElement("div");
            card.className = "location-card";

            const title = document.createElement("h2");
            title.innerHTML = `<i class="fas fa-map-marker-alt" style="color: #eab308; font-size: 1.2rem; margin-right: 8px;"></i> ${loc.locationName ?? "未命名地區"}`;
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
                    none.className = "time-label";
                    none.textContent = "無時間段資料";
                    block.appendChild(none);
                } else {
                    times.forEach(info => block.appendChild(createTimeEntry(info, element.elementName)));
                }

                card.appendChild(block);
            });

            resultsEl.appendChild(card);
        });

        // 重新套用當前的檢視模式
        setViewMode(viewMode);
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

        statusEl.innerHTML = `<i class="fas fa-spinner fa-spin"></i> 正在連線中央氣象署，讀取即時數據...`;
        submitBtn.disabled = true;
        resultsEl.innerHTML = "";
        rawJsonEl.textContent = "正在通訊中...";

        try {
            const payload = await fetchForecast(params.toString());
            rawJsonEl.textContent = JSON.stringify(payload, null, 2);
            renderResults(payload);
            const count = payload?.records?.location?.length ?? 0;
            statusEl.innerHTML = `<i class="fas fa-check-circle" style="color: #10b981;"></i> 監控資料讀取完成。共載入 ${count} 個地區氣象資訊。`;
        } catch (error) {
            statusEl.innerHTML = `<i class="fas fa-exclamation-triangle" style="color: #ef4444;"></i> 資料取得失敗：${error.message}`;
            rawJsonEl.textContent = error.stack ?? error.message;
        } finally {
            submitBtn.disabled = false;
        }
    });

    // 預設在 DOM 載入後自動跑一次全部地區預報
    setTimeout(() => {
        form.dispatchEvent(new Event("submit"));
    }, 150);
});
