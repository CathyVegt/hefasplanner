package org.acme.vehiclerouting.rest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam; //ch
import jakarta.ws.rs.core.MediaType;

import org.acme.vehiclerouting.domain.*;
import org.acme.vehiclerouting.domain.dataHandling.DataParser;
import org.apache.poi.ss.formula.functions.DMax;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Demo data", description = "Timefold-provided demo vehicle routing data.")
@Path("demo-data")
public class VehicleRouteDemoResource {

    public enum DemoData{
        LOADDATA;
    }

    public VehicleRoutePlan build(DemoData demoData){
//        return loadDataWithParser(demoData);
        if (demoData == DemoData.LOADDATA) { //ch
            return loadDataWithParameters(LocalDate.now(), LocalDate.now().plusDays(1), LocalTime.of(8, 30), LocalTime.of(16, 30));
        }
        throw new IllegalArgumentException("Unsupported demo data: " + demoData);

    }

//    public VehicleRoutePlan loadDataWithParser(DemoData demoData){
    public VehicleRoutePlan loadDataWithParameters(LocalDate startDate, LocalDate endDate, LocalTime shiftStart,
            LocalTime shiftEnd) {

            String name = "demo";
        try{

//            String[] shiftTimes = DataParser.askShiftTimes();
//            LocalDateTime[] horizon = DataParser.askDates(shiftTimes[0], shiftTimes[1]);
            LocalDateTime horizonStart = LocalDateTime.of(startDate, shiftStart);
            LocalDateTime horizonEnd = LocalDateTime.of(endDate, shiftEnd);
            if (horizonEnd.isBefore(horizonStart)) {
                throw new IllegalArgumentException("endDate/shiftEnd must be equal to or after startDate/shiftStart.");
            }

            DataParser.BrandTechPair brandTechPair =  DataParser.readEmployees();
            Map<String, BrandId> brandCatalog = brandTechPair.getBrandCatalog();
            List<Technician> technicians = brandTechPair.getTechnicians();
//            List<Visit> visits  = DataParser.readVisits(horizon[0], horizon[1], brandCatalog);
            List<Visit> visits  = DataParser.readVisits(horizonStart, horizonEnd, brandCatalog);
            // create vehicles:
            List<Vehicle> vehicles = new ArrayList<>();
//            long n_days = ChronoUnit.DAYS.between(horizon[0].toLocalDate(), horizon[1].toLocalDate()) + 1; // +1 for including.
            long n_days = ChronoUnit.DAYS.between(horizonStart.toLocalDate(), horizonEnd.toLocalDate()) + 1; // +1 for including.
            System.out.println(String.format("n_days %d", n_days) );
            for(int i = 0; i <n_days; i++){
//                LocalDateTime minStartTime = horizon[0].plusDays(i);
//                LocalDateTime maxEndTime = horizon[1].minusDays(n_days-i);
                LocalDateTime minStartTime = horizonStart.plusDays(i);
                LocalDateTime maxEndTime = horizonEnd.minusDays(n_days-i);

                System.out.println(minStartTime);
                System.out.println(maxEndTime);
                for(Technician technician : technicians){
                    vehicles.add(new Vehicle(
                            technician.getName() +  "-" + Integer.toString(minStartTime.getDayOfMonth()),
                            technician.getLocation(),
                            minStartTime,
                            maxEndTime,
                            minStartTime,
                            technician
                    ));
                }
            }

            Location[] bbox = computeBoundingBox(vehicles, visits);
            Location southWestCorner = bbox[0];
            Location northEastCorner = bbox[1];

            return  new VehicleRoutePlan(
                    name,
                    southWestCorner,
                    northEastCorner,
//                    horizon[0],
//                    horizon[1],
                    horizonStart,
                    horizonEnd,
                    vehicles,
                    visits
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // region API get list and section functionality. from enum, should work
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "List of demo data represented as IDs.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = DemoData.class, type = SchemaType.ARRAY))) })
    @Operation(summary = "List demo data.")
    @GET
    public DemoData[] list() {
        return DemoData.values();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Unsolved demo route plan.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = VehicleRoutePlan.class))) })
    @Operation(summary = "Find an unsolved demo route plan by ID.")
    @GET
    @Path("/{demoDataId}")
    public VehicleRoutePlan generate(@Parameter(description = "Unique identifier of the demo data.",
//            required = true) @PathParam("demoDataId") DemoData demoData) {
        required = true) @PathParam("demoDataId") DemoData demoData,
        @QueryParam("startDate") String startDate,
        @QueryParam("endDate") String endDate,
        @QueryParam("shiftStart") String shiftStart,
        @QueryParam("shiftEnd") String shiftEnd) {
            if (demoData == DemoData.LOADDATA && startDate != null && endDate != null && shiftStart != null && shiftEnd != null) {
                return loadDataWithParameters(
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate),
                        LocalTime.parse(shiftStart),
                        LocalTime.parse(shiftEnd));
            }

            return build(demoData);
    }

    //endregion

    private static Location[] computeBoundingBox(List<Vehicle> vehicles, List<Visit> visits) {
        double minLat = Double.POSITIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (Vehicle v : vehicles) {
            Location l = v.getHomeLocation(); // adjust getter name if different
            minLat = Math.min(minLat, l.getLatitude());
            minLon = Math.min(minLon, l.getLongitude());
            maxLat = Math.max(maxLat, l.getLatitude());
            maxLon = Math.max(maxLon, l.getLongitude());
        }
        for (Visit v : visits) {
            Location l = v.getLocation();
            minLat = Math.min(minLat, l.getLatitude());
            minLon = Math.min(minLon, l.getLongitude());
            maxLat = Math.max(maxLat, l.getLatitude());
            maxLon = Math.max(maxLon, l.getLongitude());
        }

        // Add a tiny padding so pins aren't on the edge
        double pad = 0.01;
        return new Location[] {
                new Location(minLat - pad, minLon - pad),
                new Location(maxLat + pad, maxLon + pad)
        };
    }




//    @Inject
//    DemoVisitLoader demoVisitLoader;
    //region OLDFIELDS
//    private static final String[] FIRST_NAMES = { "Amy", "Beth", "Carl", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy", "Jay" };
//    private static final String[] LAST_NAMES = { "Cole", "Fox", "Green", "Jones", "King", "Li", "Poe", "Rye", "Smith", "Watt" };
//    private static final int[] SERVICE_DURATION_MINUTES = { 10, 20, 30, 40 };
//    static final LocalTime MORNING_WINDOW_START = LocalTime.of(8, 0);
//    private static final LocalTime MORNING_WINDOW_END = LocalTime.of(12, 0);
//    private static final LocalTime AFTERNOON_WINDOW_START = LocalTime.of(13, 0);
//    static final LocalTime AFTERNOON_WINDOW_END = LocalTime.of(16, 30);
//    public enum DemoDataMode { RANDOM_BOUNDS, FIXED_DATA, LARGE_FIXED}
//   //endregion
//
//    //region OLD DEMODATA ENUM AND FIELDS
//    public enum DemoData {
//        PHILADELPHIA(DemoDataMode.RANDOM_BOUNDS, 0, 55, 6, LocalTime.of(7, 30),
////                1, 2, 15, 30,
//                new Location(39.7656099067391, -76.83782328143754),
//                new Location(40.77636644354855, -74.9300739430771)),
//        HARTFORT(DemoDataMode.RANDOM_BOUNDS, 1, 50, 6, LocalTime.of(7, 30),
////                1, 3, 20, 30,
//                new Location(41.48366520850297, -73.15901689943055),
//                new Location(41.99512052869307, -72.25114548877427)),
//        FIRENZE(DemoDataMode.RANDOM_BOUNDS, 2, 77, 6, LocalTime.of(7, 30),
////                1, 2, 20, 40,
//                new Location(43.751466, 11.177210), new Location(43.809291, 11.290195)),
//        AMSTERDAM(DemoDataMode.RANDOM_BOUNDS, 0, 300, 20, LocalTime.of(8, 0),
////                1, 2, 15, 30,
//                new Location(51.494918582785814, 4.369823378776931),
//                new Location(53.19245509228434, 6.718563029509456)),
//        MY_FIXED_DEMO(DemoDataMode.FIXED_DATA),
//        LARGE_DEMO(DemoDataMode.LARGE_FIXED); // <-- new dataset
//
//        private final DemoDataMode mode;
//
//        //random-bounds fields
//        private long seed;
//        private int visitCount;
//        private int vehicleCount;
//        private LocalTime vehicleStartTime;
//        private int minDemand;
//        private int maxDemand;
//        private int minVehicleCapacity;
//        private int maxVehicleCapacity;
//        private Location southWestCorner;
//        private Location northEastCorner;
//
//
//        // constructor for FIXED_DATA
//        DemoData(DemoDataMode mode) {
//            this.mode = mode;
//        }
//        // constructor for RANDOM_BOUNDS
//        DemoData(DemoDataMode mode, long seed, int visitCount, int vehicleCount, LocalTime vehicleStartTime,
//                 //int minDemand, int maxDemand, int minVehicleCapacity, int maxVehicleCapacity,
//                 Location southWestCorner, Location northEastCorner) {
//            this.mode = mode;
////            if (minDemand < 1) {
////                throw new IllegalStateException("minDemand (%s) must be greater than zero.".formatted(minDemand));
////            }
////            if (maxDemand < 1) {
////                throw new IllegalStateException("maxDemand (%s) must be greater than zero.".formatted(maxDemand));
////            }
////            if (minDemand >= maxDemand) {
////                throw new IllegalStateException("maxDemand (%s) must be greater than minDemand (%s)."
////                        .formatted(maxDemand, minDemand));
////            }
////            if (minVehicleCapacity < 1) {
////                throw new IllegalStateException(
////                        "Number of minVehicleCapacity (%s) must be greater than zero.".formatted(minVehicleCapacity));
////            }
////            if (maxVehicleCapacity < 1) {
////                throw new IllegalStateException(
////                        "Number of maxVehicleCapacity (%s) must be greater than zero.".formatted(maxVehicleCapacity));
////            }
////            if (minVehicleCapacity >= maxVehicleCapacity) {
////                throw new IllegalStateException("maxVehicleCapacity (%s) must be greater than minVehicleCapacity (%s)."
////                        .formatted(maxVehicleCapacity, minVehicleCapacity));
////            }
//            if (visitCount < 1) {
//                throw new IllegalStateException(
//                        "Number of visitCount (%s) must be greater than zero.".formatted(visitCount));
//            }
//            if (vehicleCount < 1) {
//                throw new IllegalStateException(
//                        "Number of vehicleCount (%s) must be greater than zero.".formatted(vehicleCount));
//            }
//            if (northEastCorner.getLatitude() <= southWestCorner.getLatitude()) {
//                throw new IllegalStateException(
//                        "northEastCorner.getLatitude (%s) must be greater than southWestCorner.getLatitude(%s)."
//                                .formatted(northEastCorner.getLatitude(), southWestCorner.getLatitude()));
//            }
//            if (northEastCorner.getLongitude() <= southWestCorner.getLongitude()) {
//                throw new IllegalStateException(
//                        "northEastCorner.getLongitude (%s) must be greater than southWestCorner.getLongitude(%s)."
//                                .formatted(northEastCorner.getLongitude(), southWestCorner.getLongitude()));
//            }
//
//            this.seed = seed;
//            this.visitCount = visitCount;
//            this.vehicleCount = vehicleCount;
//            this.vehicleStartTime = vehicleStartTime;
////            this.minDemand = minDemand;
////            this.maxDemand = maxDemand;
////            this.minVehicleCapacity = minVehicleCapacity;
////            this.maxVehicleCapacity = maxVehicleCapacity;
//            this.southWestCorner = southWestCorner;
//            this.northEastCorner = northEastCorner;
//        }
//
//        public DemoDataMode getMode() {
//            return mode;
//        }
//    }
//
//    //endregion


    //region BUILD SWITCH on DEMODATA
//    public VehicleRoutePlan build(DemoData demoData){
//        return switch(demoData.getMode()){
//            case RANDOM_BOUNDS -> buildRandomBounds(demoData);
//            case FIXED_DATA -> buildFixedData(demoData);
//            case LARGE_FIXED -> buildLargeFixedData(demoData);
//        };
//    }

    //endregion



//    private static Vehicle createVehicleWithShift(String id, Location homeLocation, LocalDateTime shiftStart) {
//        LocalDateTime shiftEnd = shiftStart.plusHours(8);
//        return new Vehicle(id, homeLocation, shiftStart, shiftEnd, shiftStart);
//    }
//
//
//    public VehicleRoutePlan buildRandomBounds(DemoData demoData) {
//        String name = "demo";
//
//        Random random = new Random(demoData.seed);
//        PrimitiveIterator.OfDouble latitudes = random
//                .doubles(demoData.southWestCorner.getLatitude(), demoData.northEastCorner.getLatitude()).iterator();
//        PrimitiveIterator.OfDouble longitudes = random
//                .doubles(demoData.southWestCorner.getLongitude(), demoData.northEastCorner.getLongitude()).iterator();
//
//        // PrimitiveIterator.OfInt demand = random.ints(demoData.minDemand, demoData.maxDemand + 1)
//        //         .iterator();
//        // PrimitiveIterator.OfInt vehicleCapacity = random.ints(demoData.minVehicleCapacity, demoData.maxVehicleCapacity + 1)
//        //         .iterator();
//
//        /**
//         * Create the vehicles
//         */
//
//
//        AtomicLong vehicleSequence = new AtomicLong();
////        Supplier<Vehicle> vehicleSupplier = () -> new Vehicle(
//        Supplier<Vehicle> vehicleSupplier = () -> createVehicleWithShift(
//                String.valueOf(vehicleSequence.incrementAndGet()),
//                //vehicleCapacity.nextInt(),
//                new Location(latitudes.nextDouble(), longitudes.nextDouble()),
//                tomorrowAt(demoData.vehicleStartTime));
//
//        List<Vehicle> vehicles = Stream.generate(vehicleSupplier)
//                .limit(demoData.vehicleCount)
//                .collect(Collectors.toList());
//
//        Supplier<String> nameSupplier = () -> {
//            Function<String[], String> randomStringSelector = strings -> strings[random.nextInt(strings.length)];
//            String firstName = randomStringSelector.apply(FIRST_NAMES);
//            String lastName = randomStringSelector.apply(LAST_NAMES);
//            return firstName + " " + lastName;
//        };
//
//        /**
//         * Create the Visits
//         */
//        AtomicLong visitSequence = new AtomicLong();
//        Supplier<Visit> visitSupplier = () -> {
//            boolean morningTimeWindow = random.nextBoolean();
//
//            LocalDateTime minStartTime = morningTimeWindow ? tomorrowAt(MORNING_WINDOW_START) : tomorrowAt(AFTERNOON_WINDOW_START);
//            LocalDateTime maxEndTime = morningTimeWindow ? tomorrowAt(MORNING_WINDOW_END) : tomorrowAt(AFTERNOON_WINDOW_END);
//            int serviceDurationMinutes = SERVICE_DURATION_MINUTES[random.nextInt(SERVICE_DURATION_MINUTES.length)];
//            return new Visit(
//                    String.valueOf(visitSequence.incrementAndGet()),
//                    nameSupplier.get(),
//                    new Location(latitudes.nextDouble(), longitudes.nextDouble()),
////                    demand.nextInt(),
//                    minStartTime,
//                    maxEndTime,
//                    Duration.ofMinutes(serviceDurationMinutes));
//        };
//
//        List<Visit> visits = Stream.generate(visitSupplier)
//                .limit(demoData.visitCount)
//                .collect(Collectors.toList());
//
//        return new VehicleRoutePlan(name, demoData.southWestCorner, demoData.northEastCorner,
//                tomorrowAt(demoData.vehicleStartTime), tomorrowAt(LocalTime.MIDNIGHT).plusDays(1L),
//                vehicles, visits);
//    }
//
//    private static LocalDateTime tomorrowAt(LocalTime time) {
//        return LocalDateTime.of(LocalDate.now().plusDays(1L), time);
//    }
//
//    // region MYCUSTOMBUILDERS
//    private VehicleRoutePlan buildFixedData(DemoData demoData) {
//        String name = "fixedDemo";
//        int nrVehicles = 2; // 4
//        int planningDays = 3; // 15
//        int nrVisits = 15;
//        LocalDateTime vehicleStart = LocalDateTime.of(2026, 3, 1, 8, 30);
////                tomorrowAt(LocalTime.of(8,30));
//        LocalDateTime horizonEnd = tomorrowAt(LocalTime.of(22,0)).plusDays(planningDays);
//
//        String[] ids = {"1-%d","2-%d", "3-%d", "4-%d"};
//        double[] lats = {51.9280971524736, 52.3664726276756, 51.9280971524736, 51.2497466602742};
//        double[] longs = {6.07789246810776, 5.2098416475017, 6.07789246810776, 5.89418211851084};
//
//        List<Vehicle> vehicles = new ArrayList<Vehicle>();
//        System.out.println(String.format(ids[0],1));
//
//        for(int d = 0; d < planningDays; d++){
//            for(int i=0; i < nrVehicles; i++){
//                vehicles.add(
//                        new Vehicle(
//                                String.format(ids[i], d+1),
//                                new Location(lats[i], longs[i]),
//                                vehicleStart.plusDays(d),
//                                vehicleStart.plusDays(d).plusHours(8),
//                                vehicleStart.plusDays(d)
//                        )
//                );
//            }
//        }
////        // Vehicles:
////        List<Vehicle> vehicles = List.of(
////                new Vehicle("1", new Location(51.9280971524736, 6.07789246810776), vehicleStart),
////                new Vehicle("2", new Location(52.3664726276756, 5.2098416475017), vehicleStart),
////                new Vehicle("3", new Location(51.9280971524736, 6.07789246810776), vehicleStart),
////                new Vehicle("4", new Location(51.2497466602742, 5.89418211851084), vehicleStart)
////        );
//
//        // Visits:
//        LocalDateTime minStart = vehicleStart;
//        LocalDateTime maxEnd = tomorrowAt(AFTERNOON_WINDOW_END).plusDays(planningDays);
//        Duration serviceTime = Duration.ofMinutes(80);
//
//        List<Visit> visits = demoVisitLoader.loadVisitsFromJson(minStart, maxEnd, serviceTime)
//                .stream().limit(nrVisits)
//                .collect(Collectors.toList());
//
////        List<Visit> visits = List.of(
////                new Visit("1", "00622/C-BMI1", new Location(52.1638234, 5.6053827), minStart, maxEnd, serviceTime),
////                new Visit("2", "00699/C-BMI1", new Location(52.1458265, 5.5823862), minStart, maxEnd, serviceTime),
////                new Visit("3", "66-1057-001/C-BMI1", new Location(52.3658233, 4.8602224), minStart, maxEnd, serviceTime),
////                new Visit("4", "01238/C-BMI1", new Location(51.995138, 5.9502543), minStart, maxEnd, serviceTime),
////                new Visit("5", "01662/C-BMI1", new Location(52.1911653, 5.9436608), minStart, maxEnd, serviceTime),
////                new Visit("6", "01764/C-BMI4", new Location(52.9773069, 6.5724337), minStart, maxEnd, serviceTime),
////                new Visit("7", "01956/C-BMI6", new Location(51.9878404, 4.3714649), minStart, maxEnd, serviceTime),
////                new Visit("8", "03311/C-BMI1", new Location(51.8699448, 6.2540368), minStart, maxEnd, serviceTime),
////                new Visit("9", "60-7891-001/C-BMI1", new Location(52.72501094163305, 6.988343810499081), minStart, maxEnd, serviceTime),
////                new Visit("10", "60-6971-002/C-BMI2", new Location(52.1018798, 6.1577038), minStart, maxEnd, serviceTime),
////                new Visit("11", "66-6961-001/C-BMI2", new Location(52.0930704, 6.0474534), minStart, maxEnd, serviceTime),
////                new Visit("12", "66-8919-072/C-BMI1", new Location(53.2216566, 5.7728296), minStart, maxEnd, serviceTime),
////                new Visit("13", "66-6931-002/C-BMI1", new Location(51.9502006, 5.9701163), minStart, maxEnd, serviceTime),
////                new Visit("14", "66-7091-001/C-BMI1", new Location(51.8609559, 6.4870697), minStart, maxEnd, serviceTime),
////                new Visit("15", "66-3207-001/C-BMI1", new Location(51.845359, 4.3491887), minStart, maxEnd, serviceTime),
////                new Visit("16", "66-7551-004/C-BMI1", new Location(52.2634611, 6.7954748), minStart, maxEnd, serviceTime),
////                new Visit("17", "66-7512-002/C-BMI1", new Location(52.2106345, 6.8907707), minStart, maxEnd, serviceTime),
////                new Visit("18", "66-6416-002/C-BMI1", new Location(50.8892501, 5.9860876), minStart, maxEnd, serviceTime),
////                new Visit("19", "66-6522-002/C-BMI1", new Location(51.8408263, 5.8908849), minStart, maxEnd, serviceTime),
////                new Visit("20", "61-1098-001/C-BMI1", new Location(52.3556476, 4.9313297), minStart, maxEnd, serviceTime)
////        );
//
//        // VehicleRouteplan needs the coordinate corners. Compute from fixed locations.
//        Location[] bbox = computeBoundingBox(vehicles, visits);
//        Location southWestCorner = bbox[0];
//        Location northEastCorner = bbox[1];
//
//        return new VehicleRoutePlan(
//                name,
//                southWestCorner,
//                northEastCorner,
//                vehicleStart,
//                horizonEnd,
//                vehicles,
//                visits
//        );
//
//    }
//
//    private VehicleRoutePlan buildLargeFixedData(DemoData demoData) {
//        String name = "largeDemo";
//        List<Visit> visits = demoVisitLoader.loadVisitsFromJson();
//        int planningDays = 10;
//
//        LocalDateTime vehicleStart = tomorrowAt(LocalTime.of(8,0));
//        LocalDateTime horizonEnd = tomorrowAt(LocalTime.of(16, 30)).plusDays(planningDays);
//
//
//        // Vehicles:
//        List<Vehicle> vehicles = List.of(
//                createVehicleWithShift("0", new Location(51.9280971524736, 6.07789246810776), vehicleStart),
//                createVehicleWithShift("1", new Location(52.3664726276756, 5.2098416475017), vehicleStart),
//                createVehicleWithShift("2", new Location(51.9280971524736, 6.07789246810776), vehicleStart),
//                createVehicleWithShift("3", new Location(51.2497466602742, 5.89418211851084), vehicleStart),
//                createVehicleWithShift("4", new Location(51.2497466602742, 6.02661734201217), vehicleStart.plusDays(1)),
//                createVehicleWithShift("5", new Location(51.9858536164876, 5.91924254633475), vehicleStart.plusDays(1)),
//                createVehicleWithShift("6", new Location(51.9748575231563, 5.95151488262964), vehicleStart.plusDays(1)),
//                createVehicleWithShift("7", new Location(52.1500753018447, 5.38056735258708), vehicleStart.plusDays(1)),
//                createVehicleWithShift("8", new Location(51.9751431916251, 5.92852542621516), vehicleStart.plusDays(2)),
//                createVehicleWithShift("9", new Location(51.8547099653457, 5.81186839499179), vehicleStart.plusDays(2)),
//                createVehicleWithShift("10", new Location(51.5066098160184, 5.39401492165828), vehicleStart.plusDays(2)),
//                createVehicleWithShift("11", new Location(51.8226143523152, 5.0512912803051), vehicleStart.plusDays(2)),
//                createVehicleWithShift("12", new Location(51.3339490380399, 5.98121147978097), vehicleStart.plusDays(3)),
//                createVehicleWithShift("13", new Location(51.3339490380399, 5.98121147978097), vehicleStart.plusDays(3)),
//                createVehicleWithShift("14", new Location(51.3339490380399, 5.98121147978097), vehicleStart.plusDays(3)),
//                createVehicleWithShift("15", new Location(51.3339490380399, 5.98121147978097), vehicleStart.plusDays(3))
//
//        );
//
//        // Visits:
//        LocalDateTime minStart = tomorrowAt(MORNING_WINDOW_START);
//        LocalDateTime maxEnd = tomorrowAt(AFTERNOON_WINDOW_END).plusDays(10);
//        Duration serviceTime = Duration.ofMinutes(80);
//
//        // VehicleRouteplan needs the coordinate corners. Compute from fixed locations.
//        Location[] bbox = computeBoundingBox(vehicles, visits);
//        Location southWestCorner = bbox[0];
//        Location northEastCorner = bbox[1];
//
//        return new VehicleRoutePlan(
//                name,
//                southWestCorner,
//                northEastCorner,
//                vehicleStart,
//                horizonEnd,
//                vehicles,
//                visits
//        );
//
//    }

    //endregion
}
