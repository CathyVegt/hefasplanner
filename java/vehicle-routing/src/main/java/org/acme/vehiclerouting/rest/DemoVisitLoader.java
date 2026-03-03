package org.acme.vehiclerouting.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.Visit;
import org.acme.vehiclerouting.rest.VehicleRouteDemoResource;
import org.acme.vehiclerouting.rest.VisitData;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class DemoVisitLoader {

    @Inject ObjectMapper objectMapper;

    public List<Visit> loadVisitsFromJson(){
        LocalDateTime minStart = tomorrowAt(VehicleRouteDemoResource.MORNING_WINDOW_START);
        LocalDateTime maxEnd   = tomorrowAt(VehicleRouteDemoResource.AFTERNOON_WINDOW_END);
        Duration service = Duration.ofHours(2);
        return loadVisitsFromJson(minStart, maxEnd, service);
    }

    public List<Visit> loadVisitsFromJson(LocalDateTime minStart, LocalDateTime maxEnd, Duration service) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("demo/visits.json")) {

            if (is == null) {
                throw new IllegalStateException("Resource demo/visits.json not found.");
            }

            List<VisitData> data = objectMapper.readValue(is, new TypeReference<List<VisitData>>() {});

//            LocalDateTime minStart = tomorrowAt(VehicleRouteDemoResource.MORNING_WINDOW_START);
//            LocalDateTime maxEnd   = tomorrowAt(VehicleRouteDemoResource.AFTERNOON_WINDOW_END);
//            Duration service = Duration.ofHours(2);

            return data.stream()
                    .map(v -> new Visit(
                            v.id,
                            v.name,
                            new Location(v.latitude, v.longitude),
                            minStart,
                            maxEnd,
                            service
                    ))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load demo visits.", e);
        }
    }

    private static LocalDateTime tomorrowAt(java.time.LocalTime time) {
        return java.time.LocalDateTime.of(java.time.LocalDate.now().plusDays(1), time);
    }
}
