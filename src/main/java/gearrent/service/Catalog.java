package gearrent.service;
import gearrent.dao.*;
import gearrent.entity.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
public class Catalog {
    private final Db db;
    public Catalog(Db db){this.db=db;}
    public boolean writable(Session u,Section s){
        return switch(s){
            case BRANCH,MEMBERSHIP,SETTINGS -> u.admin();
            case CATEGORY -> u.admin()||u.manager();
            case EQUIPMENT -> u.admin()||u.manager()||u.equipmentPermission();
            case CUSTOMER -> true;
        };
    }
    public List<Map<String,String>> list(Session u,Section s)throws Exception{
        if(s==Section.SETTINGS)u.require(u.admin());
        return db.transaction(c->CatalogDao.list(c,s,u));
    }
    public static BigDecimal amount(String value,String label){
        try {
            var n=new BigDecimal(value);
            if(n.signum()>=0 && n.scale()<=2)return n;
        }catch(Exception ignored){}
        throw new IllegalArgumentException(label+" must be a non-negative number with up to two decimal places.");
    }
    private void ownership(Session u,Section s,Map<String,String> row){
        if(s==Section.EQUIPMENT || s==Section.CATEGORY)u.checkBranch(row.get("branch_code"));
    }
    public void save(Session u,Section s,Map<String,String> row,boolean edit)throws Exception{
        u.require(writable(u,s)); u.require(s!=Section.SETTINGS||edit);
        var data=new LinkedHashMap<>(row);
        if(!u.admin()&&s==Section.CATEGORY)data.put("branch_code",u.branch());
        ownership(u,s,data);
        List<Object> values=new ArrayList<>();
        for(String field:s.fields){
            String v=Objects.toString(data.get(field),"").trim();
            boolean optional=(s==Section.CATEGORY&&field.equals("branch_code"))||(s==Section.CUSTOMER&&field.equals("level_name"));
            if(v.isEmpty()&&!optional)throw new IllegalArgumentException("Required field: "+field);
            if(Set.of("discount_percent","long_discount","base_price_factor","weekend_multiplier","deposit_limit","late_fee","daily_base_price","security_deposit").contains(field)){
                var n=amount(v,field);
                if((field.contains("discount") && n.compareTo(BigDecimal.valueOf(100))>0)||
                   ((field.equals("base_price_factor")||field.equals("weekend_multiplier"))&&n.signum()==0))
                    throw new IllegalArgumentException("Invalid value for "+field);
            }
            if(field.equals("email")&&!v.matches("[^ @]+@[^ @]+[.][^ @]+"))throw new IllegalArgumentException("Enter a valid email address.");
            if(field.equals("purchase_year")&&(!v.matches("[0-9]{4}")||Integer.parseInt(v)<1901||Integer.parseInt(v)>LocalDate.now().getYear()))throw new IllegalArgumentException("Enter a valid purchase year.");
            if(field.equals("is_active")&&!Set.of("0","1").contains(v))throw new IllegalArgumentException("Use 1 for active or 0 for inactive.");
            values.add(v.isEmpty()?null:v);
        }
        db.transaction(c->{
            var old=edit?CatalogDao.lock(c,s,data.get(s.key())):null;
            if(edit)ownership(u,s,old);
            if(s==Section.CATEGORY && data.get("branch_code")!=null&&!data.get("branch_code").isBlank()&&CatalogDao.categoryConflict(c,data.get(s.key()),data.get("branch_code")))throw new IllegalArgumentException("This category is used by another branch.");
            if(s==Section.EQUIPMENT){
                var cat=CatalogDao.lock(c,Section.CATEGORY,data.get("category_code"));
                if(cat.get("branch_code")!=null&&!cat.get("branch_code").equals(data.get("branch_code")))throw new IllegalArgumentException("Category belongs to a different branch.");
                String id=data.get(s.key()), state=data.get("status");
                if(!Set.of("Available","Under Maintenance").contains(state))throw new IllegalArgumentException("Use Available or Under Maintenance. Booking and issuing set Reserved and Rented automatically.");
                boolean was=edit&&old.get("status").equals("Under Maintenance"), now=state.equals("Under Maintenance");
                if(edit && !old.get("branch_code").equals(data.get("branch_code")) && CatalogDao.hasHistory(c,id))
                    throw new IllegalArgumentException("An item with recorded history must stay at its original branch.");
                if(edit && (now!=was||!old.get("branch_code").equals(data.get("branch_code")))&&CatalogDao.hasBookings(c,id))
                    throw new IllegalArgumentException("Resolve open rentals and reservations before changing branch or maintenance state.");
                CatalogDao.save(c,s,values,edit);
                if(was!=now)CatalogDao.maintenance(c,id,now,LocalDate.now());
            }else CatalogDao.save(c,s,values,edit);
            return null;
        });
    }
    public void remove(Session u,Section s,String id)throws Exception{
        u.require(writable(u,s)&&s!=Section.SETTINGS);
        db.transaction(c->{ownership(u,s,CatalogDao.lock(c,s,id));CatalogDao.remove(c,s,id);return null;});
    }
}
