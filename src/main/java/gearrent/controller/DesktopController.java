package gearrent.controller;
import gearrent.entity.*;
import gearrent.service.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
public class DesktopController{
    public final Session user;
    private final Catalog catalog;private final Rentals rentals;private final Reports reports;
    public DesktopController(Session u,Catalog catalog,Rentals rentals,Reports reports){user=u;this.catalog=catalog;this.rentals=rentals;this.reports=reports;}
    public boolean writable(Section s){return catalog.writable(user,s);}
    public List<Map<String,String>> list(Section s)throws Exception{return catalog.list(user,s);}
    public void save(Section s,Map<String,String> data,boolean edit)throws Exception{catalog.save(user,s,data,edit);}
    public void delete(Section s,String id)throws Exception{catalog.remove(user,s,id);}
    private LocalDate date(String value){
        try{return LocalDate.parse(value.trim());}catch(DateTimeParseException e){throw new IllegalArgumentException("Use dates like 2026-11-19 (YYYY-MM-DD).");}
    }
    public List<Map<String,String>> search(String view,String branch,String customer,String status,String from,String to)throws Exception{
        return rentals.search(user,view,branch.trim(),customer.trim(),status.trim(),from.isBlank()?null:date(from),to.isBlank()?null:date(to));
    }
    public String held(String customer)throws Exception{return rentals.held(user,customer).toPlainString();}
    public List<String> available(String branch,String start,String end)throws Exception{return rentals.available(user,branch,date(start),date(end));}
    public String book(Rentals.Action a,Map<String,String> v)throws Exception{
        return rentals.book(user,a,v.get("Equipment ID"),v.get("Customer ID"),date(v.get("Start date")),date(v.get("End date")),null,v.get("Payment"));
    }
    public String convert(int id,String payment)throws Exception{return rentals.book(user,Rentals.Action.CONVERT,null,null,null,null,id,payment);}
    public void cancel(int id)throws Exception{rentals.cancel(user,id);}
    public void payment(int id,String p)throws Exception{rentals.pay(user,id,p);}
    public String settle(int id,Map<String,String> v)throws Exception{return rentals.settle(user,id,date(v.get("Returned on")),v.get("Damage description"),v.get("Damage charge"));}
    public List<Map<String,String>> report(String type,String branch,String from,String to)throws Exception{return reports.run(user,type,branch.trim(),date(from),date(to));}
}
