package org.example.foreignsalary.services;

import org.example.foreignsalary.model.SicknessReports;
import org.example.foreignsalary.repositories.SicknessReportsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;

@Service
public class SicknessReportsService {

    private final SicknessReportsRepository sicknessReportsRepository;



    @Autowired
    public SicknessReportsService(SicknessReportsRepository sicknessReportsRepository) {
        this.sicknessReportsRepository = sicknessReportsRepository;

    }

    public SicknessReports getSicknessReports(int month, int year) {
       HashMap<LocalDate, Integer> testMap = new HashMap<>();
       testMap.put(LocalDate.now(), 1);

        if (sicknessReportsRepository.findAll().isEmpty())
            return null;

            //saveSicknessReports(1,1,2000);

        return sicknessReportsRepository.findAll().get(0);

    }

    public SicknessReports saveSicknessReports(SicknessReports sicknessReports) {
        // gets a monthly record and adds/ updates the map an saves it to the DB
        //SicknessReports sicknessReports = getSicknessReports();

        //sicknessReports.setSicknessReports(calcLastDayOfMonth( month,  year), count);
        sicknessReportsRepository.save(sicknessReports);
        return sicknessReports;
    }














/*
    public Map<LocalDate, Integer> convertSicknessReportsListToMap(List<LocalDate> sicknessReportsList) {
        for (LocalDate sicknessReports : sicknessReportsList) {
            sicknessReportsMap.put(sicknessReports, 1);
        }
        return sicknessReportsMap;
    }

    public List<LocalDate> convertSicknessReportsMapToList (Map<LocalDate, Integer> map) {
        List<LocalDate> sicknessReportsList = new ArrayList<LocalDate>();
        for(Map.Entry<LocalDate, Integer> entry : map.entrySet()) {
            sicknessReportsList.add(entry.getKey());
        }
        return sicknessReportsList;
    }

 */
}
