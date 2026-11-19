package gearrent.service;
import gearrent.dao.*;
import gearrent.entity.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.*;
import java.util.*;
public class Rentals {
    private final Db db;
    public Rentals(Db db){this.db=db;}
    public enum Action {QUOTE,RESERVE,ISSUE,CONVERT}
    public List<String> available(Session u,String branch,LocalDate start,LocalDate end)throws Exception{
        u.checkBranch(branch);Pricing.days(start,end);
        if(start.isBefore(LocalDate.now()))throw new IllegalArgumentException("Start date must be today or later.");
        return db.transaction(c->{
            List<String> ids=new ArrayList<>();
            for(var row:RentalDao.candidates(c,branch)){
                String id=row.get("equipment_id");
                if(!RentalDao.overlap(c,id,start,end,0))ids.add(id);
            }
            return ids;
        });
    }
    public void validPayment(String p){if(!Set.of("Paid","Unpaid","Partially Paid").contains(p))throw new IllegalArgumentException("Choose a payment status.");}
    public String book(Session u,Action action,String item,String customer,LocalDate start,LocalDate end,Integer booking,String payment)throws Exception{
        return db.transaction(c->{
            if(action==Action.CONVERT){
                var r=RentalDao.booking(c,booking,false);
                return process(c,u,action,r.get("equipment_id"),r.get("customer_id"),LocalDate.parse(r.get("start_date")),LocalDate.parse(r.get("end_date")),booking,payment);
            }
            return process(c,u,action,item,customer,start,end,null,payment);
        });
    }
    private String process(Connection c,Session u,Action action,String item,String customer,LocalDate start,LocalDate end,Integer booking,String payment)throws Exception{
        Pricing.days(start,end);
        if(start.isBefore(LocalDate.now()))throw new IllegalArgumentException("Start date must be today or later.");
        if((action==Action.ISSUE||action==Action.CONVERT)&&!start.equals(LocalDate.now()))throw new IllegalArgumentException("Issue equipment on its start date. Reserve it for future dates.");
        var eq=CatalogDao.lock(c,Section.EQUIPMENT,item);String branch=eq.get("branch_code");u.checkBranch(branch);
        var cust=CatalogDao.lock(c,Section.CUSTOMER,customer);
        if(booking!=null){
            var r=RentalDao.booking(c,booking,true);
            if(!"Reserved".equals(r.get("status")))throw new IllegalArgumentException("Only an open reservation can be converted.");
        }
        var cat=CatalogDao.lock(c,Section.CATEGORY,eq.get("category_code"));
        if("Under Maintenance".equals(eq.get("status"))||Set.of("0","false").contains(cat.get("is_active")))throw new IllegalArgumentException("Equipment is under maintenance or its category is inactive.");
        if(RentalDao.overlap(c,item,start,end,booking==null?0:booking))throw new IllegalArgumentException("Equipment is already booked, rented, or overdue for these dates.");
        var rules=CatalogDao.lock(c,Section.SETTINGS,"1");
        var deposit=new BigDecimal(eq.get("security_deposit"));
        if(RentalDao.held(c,customer).add(deposit).compareTo(new BigDecimal(rules.get("deposit_limit")))>0)throw new IllegalArgumentException("Customer's active deposits plus this deposit exceed the limit.");
        var member=BigDecimal.ZERO;
        if(cust.get("level_name")!=null)member=new BigDecimal(CatalogDao.lock(c,Section.MEMBERSHIP,cust.get("level_name")).get("discount_percent"));
        var q=Pricing.quote(start,end,new BigDecimal(eq.get("daily_base_price")),new BigDecimal(cat.get("base_price_factor")),new BigDecimal(cat.get("weekend_multiplier")),new BigDecimal(rules.get("long_discount")),member,deposit);
        if(action==Action.RESERVE)RentalDao.reserve(c,item,customer,branch,start,end);
        if(action==Action.ISSUE||action==Action.CONVERT){
            validPayment(payment);
            RentalDao.issue(c,booking,item,customer,branch,start,end,q,payment);
            if(booking!=null)RentalDao.bookingState(c,booking,"Converted");
        }
        return q.toString();
    }
    public void cancel(Session u,int id)throws Exception{
        db.transaction(c->{var r=RentalDao.booking(c,id,true);u.checkBranch(r.get("branch_code"));
            if(!"Reserved".equals(r.get("status")))throw new IllegalArgumentException("Reservation is already closed.");
            RentalDao.bookingState(c,id,"Cancelled");return null;});
    }
    public void pay(Session u,int id,String payment)throws Exception{
        validPayment(payment);db.transaction(c->{var r=RentalDao.rental(c,id,true);u.checkBranch(r.get("branch_code"));RentalDao.payment(c,id,payment);return null;});
    }
    public String settle(Session u,int id,LocalDate actual,String description,String charge)throws Exception{
        var damage=Catalog.amount(charge,"Damage charge");String note=description.trim();
        if(note.isEmpty()&&damage.signum()>0)throw new IllegalArgumentException("Describe the damage.");
        return db.transaction(c->{
            var initial=RentalDao.rental(c,id,false);
            CatalogDao.lock(c,Section.EQUIPMENT,initial.get("equipment_id"));
            CatalogDao.lock(c,Section.CUSTOMER,initial.get("customer_id"));
            var r=RentalDao.rental(c,id,true);u.checkBranch(r.get("branch_code"));
            if(!"Active".equals(r.get("status")))throw new IllegalArgumentException("Only an active or overdue rental can be returned.");
            if(actual.isAfter(LocalDate.now())||actual.isBefore(LocalDate.parse(r.get("start_date"))))throw new IllegalArgumentException("Return date must be between issue date and today.");
            long days=Math.max(0,ChronoUnit.DAYS.between(LocalDate.parse(r.get("end_date")),actual));
            var late=new BigDecimal(CatalogDao.lock(c,Section.SETTINGS,"1").get("late_fee")).multiply(BigDecimal.valueOf(days));
            var balance=new BigDecimal(r.get("deposit")).subtract(late).subtract(damage);
            var refund=balance.max(BigDecimal.ZERO);var extra=balance.negate().max(BigDecimal.ZERO);
            RentalDao.settle(c,id,actual,note,late,damage,refund,extra);
            if(!note.isEmpty())RentalDao.damaged(c,r.get("equipment_id"),actual);
            return "Late fee: "+late+" | Damage: "+damage+" | Deposit refund: "+refund+" | Extra fees due: "+extra;
        });
    }
    public List<Map<String,String>> search(Session u,String view,String branch,String customer,String state,LocalDate from,LocalDate to)throws Exception{
        if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("From date must come before To date.");
        return db.transaction(c->RentalDao.search(c,u,view,branch,customer,state,from,to));
    }
    public BigDecimal held(Session u,String customer)throws Exception{return db.transaction(c->RentalDao.held(c,customer));}
}
