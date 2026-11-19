package gearrent.dao;
import gearrent.entity.*;
import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
public class RentalDao {
    public static Map<String,String> booking(Connection c,int id,boolean lock)throws SQLException{
        return Db.one(c,"SELECT * FROM reservation WHERE reservation_id=?"+(lock?" FOR UPDATE":""),id);
    }
    public static Map<String,String> rental(Connection c,int id,boolean lock)throws SQLException{
        return Db.one(c,"SELECT * FROM rental WHERE rental_id=?"+(lock?" FOR UPDATE":""),id);
    }
    public static BigDecimal held(Connection c,String customer)throws SQLException{
        return new BigDecimal(Db.one(c,"SELECT COALESCE(SUM(deposit),0) AS held FROM rental WHERE customer_id=? AND status='Active'",customer).get("held"));
    }
    public static boolean overlap(Connection c,String eq,LocalDate start,LocalDate end,int except)throws SQLException{
        String sql="""
            SELECT 1 FROM reservation WHERE equipment_id=? AND status='Reserved' AND reservation_id<>?
             AND start_date<=? AND end_date>=?
            UNION ALL SELECT 1 FROM rental WHERE equipment_id=? AND status<>'Cancelled' AND start_date<=?
             AND (CASE WHEN status='Returned' THEN returned_on WHEN end_date<CURRENT_DATE THEN '9999-12-31' ELSE end_date END)>=?
            """;
        return !Db.rows(c,sql,eq,except,end,start,eq,end,start).isEmpty();
    }
    public static List<Map<String,String>> candidates(Connection c,String branch)throws SQLException{
        return Db.rows(c,"SELECT e.equipment_id FROM equipment e JOIN category k ON k.category_code=e.category_code WHERE e.branch_code=? AND e.status<>'Under Maintenance' AND k.is_active=1 ORDER BY e.equipment_id",branch);
    }
    public static void reserve(Connection c,String item,String customer,String branch,LocalDate start,LocalDate end)throws SQLException{
        Db.change(c,"INSERT INTO reservation(equipment_id,customer_id,branch_code,start_date,end_date) VALUES (?,?,?,?,?)",item,customer,branch,start,end);
    }
    public static void issue(Connection c,Integer res,String item,String customer,String br,LocalDate start,LocalDate end,Quote q,String payment)throws SQLException{
        Db.change(c,"INSERT INTO rental(reservation_id,equipment_id,customer_id,branch_code,start_date,end_date,gross,long_discount,member_discount,net_rent,deposit,payable,payment) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
            res,item,customer,br,start,end,q.gross(),q.longDiscount(),q.memberDiscount(),q.net(),q.deposit(),q.payable(),payment);
    }
    public static void bookingState(Connection c,int id,String state)throws SQLException{Db.change(c,"UPDATE reservation SET status=? WHERE reservation_id=?",state,id);}
    public static void settle(Connection c,int id,LocalDate actual,String description,BigDecimal late,BigDecimal damage,BigDecimal refund,BigDecimal extra)throws SQLException{
        Db.change(c,"UPDATE rental SET status='Returned',returned_on=?,damage_description=?,late_charge=?,damage_charge=?,refund=?,extra_due=? WHERE rental_id=?",actual,description,late,damage,refund,extra,id);
    }
    public static void damaged(Connection c,String eq,LocalDate actual)throws SQLException{
        Db.change(c,"UPDATE equipment SET status='Under Maintenance' WHERE equipment_id=?",eq);
        CatalogDao.maintenance(c,eq,true,actual.plusDays(1));
    }
    public static void payment(Connection c,int id,String payment)throws SQLException{Db.change(c,"UPDATE rental SET payment=? WHERE rental_id=?",payment,id);}
    public static List<Map<String,String>> search(Connection c,Session u,String view,String branch,String customer,String state,LocalDate from,LocalDate to)throws SQLException{
        boolean reservation=view.equals("Reservations"),overdue=view.equals("Overdue");
        String sql=reservation?"SELECT t.* FROM reservation t":overdue?"SELECT t.rental_id,t.customer_id,c.name,c.contact_no,t.branch_code,t.equipment_id,t.end_date AS due_date,DATEDIFF(CURRENT_DATE,t.end_date) AS days_overdue FROM rental_list t JOIN customer c ON c.customer_id=t.customer_id":"SELECT t.* FROM rental_list t";
        StringBuilder filter=new StringBuilder(" WHERE 1=1");List<Object> args=new ArrayList<>();
        if(overdue)filter.append(" AND t.rental_status='Overdue'");
        if(!u.admin())branch=u.branch();
        if(!branch.isBlank()){filter.append(" AND t.branch_code=?");args.add(branch);}
        if(!customer.isBlank()){filter.append(" AND t.customer_id=?");args.add(customer);}
        if(!state.isBlank()){filter.append(reservation?" AND t.status=?":" AND t.rental_status=?");args.add(state);}
        if(from!=null){filter.append(" AND t.end_date>=?");args.add(from);}
        if(to!=null){filter.append(" AND t.start_date<=?");args.add(to);}
        return Db.rows(c,sql+filter+" ORDER BY t.start_date DESC",args.toArray());
    }
}
