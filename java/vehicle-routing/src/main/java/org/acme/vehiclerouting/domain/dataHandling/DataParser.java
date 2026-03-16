package org.acme.vehiclerouting.domain.dataHandling;

import com.opencsv.CSVReader;
import io.vertx.core.net.impl.pool.Task;
import org.acme.vehiclerouting.domain.*;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataParser
{
    //region RETURNTYPE
    public static class BrandTechPair{
        private final Map<String, BrandId> brandCatalog;
        private final List<Technician> technicians;

        public BrandTechPair(Map<String, BrandId> brandCatalog, List<Technician> technicians){
            this.brandCatalog = brandCatalog;
            this.technicians = technicians;
        }

        public Map<String, BrandId> getBrandCatalog(){return brandCatalog;}
        public List<Technician> getTechnicians(){return technicians;}
    }
    //endregion

    //region UTIL
    public static ArrayList<String> getCols(Iterator<Row> rowIterator){
        ArrayList<String> cols = new ArrayList<String>();
        if (rowIterator.hasNext()){
            for(Cell cell: rowIterator.next()){
                cols.add(cell.toString());
            }
        }
        return cols;
    }
    //endregion

    //region PLANNING PERIOD todo might need to be somewhere else!
    public static LocalDateTime parseDateTime(String date, String time){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        String text = date+ " " + time;
        return LocalDateTime.parse(text,formatter);
    }

    //todo command prompt
    public static String ask(String prompt, Scanner cmdPrompt){
        System.out.println(prompt);
        return cmdPrompt.next();
    }

    public static LocalDateTime[] askDates(String shiftStart, String shiftEnd){
        Scanner cmdPrompt = new Scanner(System.in);
//        String shiftStart = "08:30";
        String start = ask("datum start van planning: dd-mm-jjjj", cmdPrompt);
        LocalDateTime startDate = parseDateTime(start, shiftStart);
        System.out.println(startDate);

//        String shiftEnd = "16:30";
        String end = ask("datum van einde planning: dd-mm:jjj", cmdPrompt);
        LocalDateTime endDate = parseDateTime(end, shiftEnd);
        System.out.println(endDate);
        return new LocalDateTime[]{startDate, endDate};
    }

    public static String[] askShiftTimes(){
        Scanner cmdPrompt = new Scanner(System.in);
        String shiftStart = ask("begintijd van dienst: hh:mm", cmdPrompt);
        String shiftEnd = ask("eindtijd van dienst: hh:mm", cmdPrompt);
        return new String[]{shiftStart, shiftEnd};
    }

    //endregion

    //region BRANDS

    //brands from employee list. These will have to match with the brandTypes in the visits data.
    public static BrandId[] getBrands(ArrayList<String> cols){
        System.out.println("get the brands");
        // slice of the columns that contain a brand type.
        List<String> brandsSlice = cols.subList(3,cols.size());
        // make the records for the problem:
        BrandId[] brands = new BrandId[brandsSlice.size()];
        for(int i=0; i < brandsSlice.size(); i++){
            brands[i] = new BrandId(brandsSlice.get(i));
        }
        return brands;
    }

    // make catalog to be able to match the brands requirement from visit data to brandID
    public static Map<String, BrandId> getBrandCatalog(BrandId[] brands){
        System.out.println("make brand catalog");
        Map<String, BrandId> catalog = new HashMap<>();
        // each BrandID has a name, make a map between name and the record.
        for (BrandId brand: brands){
            catalog.put(brand.name(), brand);
        }

        return catalog;
    }
    //endregion

    //region EMPLOYEES
    //read employees from each row
    public static Technician employeeFromRow(Row row, ArrayList<String> cols, Map<String, BrandId> brandCatalog){
        System.out.println(row.toString());
        Iterator<Cell> cellIterator = row.cellIterator();
        // Naam
        String name = row.getCell(0).toString();
        System.out.println(name);
        // latitude
        double latitude = row.getCell(1).getNumericCellValue(); //NUMERIC
        System.out.println(latitude);
        // longitude
        double longitude = row.getCell(2).getNumericCellValue();
        System.out.println(longitude);
        Location loc = new Location(latitude, longitude);


        // brands
        Map<BrandId, Skill> skills  = new HashMap<>();
        for(int i = 3; i < row.getLastCellNum(); i++){
            String brandString = cols.get(i);
            BrandId brandId = brandCatalog.get(brandString);
            // list of values
            String[] brandSkills = row.getCell(i).getStringCellValue().split(",");
            // if there are only 3 numbers, we don't count OP-taken
            // 1 for true, 0 for false.
            int skillLvl  = Integer.parseInt(brandSkills[0]);
            List<TaskType> allowedTaskTypes = new ArrayList<>();
            if (brandSkills.length == 3 ) {
                int OP_skill = 0;
                int ODH_skill = Integer.parseInt(brandSkills[1]);
                if(ODH_skill == 1){allowedTaskTypes.add(TaskType.ONDERHOUD);}
                int INB_skill = Integer.parseInt(brandSkills[2]);
                if(INB_skill == 1){allowedTaskTypes.add(TaskType.INBEDRIJFSTELLING);}
            } else{
                int OP_skill = Integer.parseInt(brandSkills[1]);
                if(OP_skill == 1){allowedTaskTypes.add(TaskType.OP);}
                int ODH_skill = Integer.parseInt(brandSkills[2]);
                if(ODH_skill == 1){allowedTaskTypes.add(TaskType.ONDERHOUD);}
                int INB_skill = Integer.parseInt(brandSkills[3]);
                if(INB_skill == 1){allowedTaskTypes.add(TaskType.INBEDRIJFSTELLING);}
            }
            // brandId = key
            if(!allowedTaskTypes.isEmpty()){
                skills.put(brandId, new Skill(skillLvl, EnumSet.copyOf(allowedTaskTypes)));
            }

        }

        System.out.println(skills);
        Technician technician = new Technician(name, loc, skills);
        System.out.println(technician);
        return technician;
    }
    // employees from excel
//    public static BrandTechPair readEmployees() throws IOException {
//        String file_path = "data/medewerkers.xlsx";
    public static BrandTechPair readEmployees() throws IOException {
        return readEmployees("data/medewerkers.xlsx");
    }

    public static BrandTechPair readEmployees(String file_path) throws IOException {
        try (InputStream inp = new FileInputStream(file_path)) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            // get columns and brands
            ArrayList<String> cols = getCols(rowIterator);
            System.out.println("cols");
            System.out.println(cols);
            // get brands
            BrandId[] brands = getBrands(cols);
            // get brandcatalog to match skills
            Map<String, BrandId> brandCatalog = getBrandCatalog(brands);

            System.out.println("brands");
            System.out.println(brands);
            System.out.println("brandCatalog");
            System.out.println(brandCatalog);
            System.out.println(brands.length == brandCatalog.size());

            ArrayList<Technician> technicians = new ArrayList<>();
            //go through the rows, have function
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                technicians.add(employeeFromRow(row, cols, brandCatalog));
            }
            System.out.println(technicians);
            return new BrandTechPair(brandCatalog, technicians);
        }
    }
    //endregion

    //region VISITS
    public static Visit visitFromRow(Row row, Map<String, BrandId> brandCatalog, LocalDateTime minStart, LocalDateTime maxEnd){
        Iterator<Cell> cellIterator = row.cellIterator();
        // id || naam
        String id = row.getCell(0).getStringCellValue();
        String name = row.getCell(0).getStringCellValue();
        // latitude
        double lat = row.getCell(1).getNumericCellValue();
        //longitude
        double longitude = row.getCell(2).getNumericCellValue();
        Location location = new Location(lat, longitude);
        // brand String
        String brandString = row.getCell(3).getStringCellValue();
        BrandId brand = brandCatalog.get(brandString);
        // minlvl int
        int minLvl = (int) Math.round(row.getCell(4).getNumericCellValue());
        // opdrachtType String
        String opdrachtType = row.getCell(5).toString();
        TaskType taskType = TaskType.taskTypefromString(opdrachtType);
        // duur int (hours)
        double duration = row.getCell(6).getNumericCellValue();
        double durationMinutes = duration * 60;
        Duration serviceDuration = Duration.ofMinutes((long)durationMinutes);
        return new Visit(id, name, location, minStart, maxEnd, serviceDuration, brand, minLvl, taskType);


    }

//    public static List<Visit> readVisits(LocalDateTime minStart, LocalDateTime maxEnd, Map<String, BrandId> brandCatalog) throws IOException{
//        String file_path = "data/opdrachten.xlsx";

    public static List<Visit> readVisits(LocalDateTime minStart, LocalDateTime maxEnd, Map<String, BrandId> brandCatalog) throws IOException{
        return readVisits(minStart, maxEnd, brandCatalog, "data/opdrachten.xlsx");
    }

    public static List<Visit> readVisits(LocalDateTime minStart, LocalDateTime maxEnd, Map<String, BrandId> brandCatalog, String file_path) throws IOException{

        try (InputStream inp = new FileInputStream(file_path)) {
            Workbook wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            // get columns
            ArrayList<String> cols = getCols(rowIterator);
            System.out.println(cols);

            // go through the rows, have function
            List <Visit> visits = new ArrayList<>();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                visits.add(visitFromRow(row, brandCatalog, minStart, maxEnd));
            }
            System.out.println(visits);
            System.out.println(visits.size());
            return visits;
        }
    }
    //endregion
}


