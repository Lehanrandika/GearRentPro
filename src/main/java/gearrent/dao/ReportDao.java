package gearrent.dao;
import java.sql.*;
import java.time.*;
import java.util.*;
public class ReportDao {
    public static List<Map<String,String>> revenue(Connection c,String branch,LocalDate from,LocalDate to)throws SQLException{
        return Db.rows(c,"""
            SELECT b.branch_code,b.name,COALESCE(i.rentals,0) AS rentals,COALESCE(i.income,0) AS rental_income,
                   COALESCE(f.late,0) AS late_fees,COALESCE(f.damage,0) AS damage_charges
            FROM branch b LEFT JOIN
             (SELECT branch_code,COUNT(*) rentals,SUM(net_rent) income FROM rental WHERE status<>'Cancelled'
              AND start_date BETWEEN ? AND ? GROUP BY branch_code) i ON i.branch_code=b.branch_code
            LEFT JOIN (SELECT branch_code,SUM(late_charge) late,SUM(damage_charge) damage FROM rental
              WHERE status='Returned' AND returned_on BETWEEN ? AND ? GROUP BY branch_code) f ON f.branch_code=b.branch_code
            WHERE (?='' OR b.branch_code=?) ORDER BY b.branch_code
            """,from,to,from,to,branch,branch);
    }
    public static List<Map<String,String>> equipment(Connection c,String b)throws SQLException{return Db.rows(c,"SELECT equipment_id,model FROM equipment WHERE branch_code=? ORDER BY equipment_id",b);}
    public static List<Map<String,String>> periods(Connection c,String b,LocalDate from,LocalDate to)throws SQLException{
        return Db.rows(c,"""
            SELECT p.* FROM (
            SELECT equipment_id,start_date AS first_day,COALESCE(returned_on,CURRENT_DATE) AS last_day,'Rented' AS kind
            FROM rental WHERE branch_code=? AND status<>'Cancelled'
            UNION ALL SELECT m.equipment_id,m.start_date,COALESCE(DATE_SUB(m.end_exclusive,INTERVAL 1 DAY),CURRENT_DATE),'Maintenance'
            FROM maintenance m JOIN equipment e ON e.equipment_id=m.equipment_id WHERE e.branch_code=?
            UNION ALL SELECT equipment_id,start_date,end_date,'Reserved' FROM reservation WHERE branch_code=? AND status='Reserved'
            ) p WHERE p.first_day<=? AND p.last_day>=?
            """,b,b,b,to,from);
    }
}
