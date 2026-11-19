package gearrent.service;
import gearrent.dao.*;
import gearrent.entity.Session;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
public class Reports {
    private final Db db;
    public Reports(Db db){this.db=db;}
    public List<Map<String,String>> run(Session u,String report,String branch,LocalDate from,LocalDate to)throws Exception{
        u.require(u.admin()||u.manager());
        String scope=u.admin()?branch:u.branch();
        if(to.isBefore(from))throw new IllegalArgumentException("Invalid report dates.");
        if(report.equals("Revenue"))return db.transaction(c->ReportDao.revenue(c,scope,from,to));
        if(scope.isBlank()||to.isAfter(LocalDate.now()))throw new IllegalArgumentException("Select a branch and a historical period ending today or earlier.");
        int days=Math.toIntExact(ChronoUnit.DAYS.between(from,to)+1);
        return db.transaction(c->{
            var items=ReportDao.equipment(c,scope);
            var periods=ReportDao.periods(c,scope,from,to);
            List<Map<String,String>> result=new ArrayList<>();
            for(var eq:items){
                BitSet busy=new BitSet(),rented=new BitSet();
                for(var p:periods){
                    if(!eq.get("equipment_id").equals(p.get("equipment_id")))continue;
                    int start=Math.max(0,(int)ChronoUnit.DAYS.between(from,LocalDate.parse(p.get("first_day"))));
                    int end=Math.min(days,(int)ChronoUnit.DAYS.between(from,LocalDate.parse(p.get("last_day")))+1);
                    if(end<=start)continue;
                    busy.set(start,end);if(p.get("kind").equals("Rented"))rented.set(start,end);
                }
                var row=new LinkedHashMap<>(eq);
                row.put("rented_days",""+rented.cardinality());row.put("available_days",""+(days-busy.cardinality()));
                row.put("reserved_or_maintenance_days",""+(busy.cardinality()-rented.cardinality()));
                row.put("utilization_percent",String.format(Locale.ROOT,"%.2f",100.0*rented.cardinality()/days));result.add(row);
            }
            return result;
        });
    }
}
