package gearrent.dao;
import gearrent.entity.*;
import java.sql.*;
import java.util.*;
public class CatalogDao {
    public static Map<String,String> user(Connection c,String name)throws SQLException{
        var rows=Db.rows(c,"SELECT * FROM app_user WHERE username=?",name);
        return rows.isEmpty()?null:rows.get(0);
    }
    public static List<Map<String,String>> list(Connection c,Section s,Session user)throws SQLException{
        String sql="SELECT * FROM "+(s==Section.EQUIPMENT?"inventory":s.table);
        if(!user.admin()){
            if(s==Section.CATEGORY)return Db.rows(c,sql+" WHERE branch_code IS NULL OR branch_code=?",user.branch());
            if(s==Section.EQUIPMENT||s==Section.BRANCH)return Db.rows(c,sql+" WHERE branch_code=?",user.branch());
        }
        return Db.rows(c,sql);
    }
    public static Map<String,String> lock(Connection c,Section s,String id)throws SQLException{
        return Db.one(c,"SELECT * FROM "+s.table+" WHERE "+s.key()+"=? FOR UPDATE",id);
    }
    public static void save(Connection c,Section s,List<Object> vals,boolean edit)throws SQLException{
        if(!edit){
            String marks=String.join(",",Collections.nCopies(vals.size(),"?"));
            Db.change(c,"INSERT INTO "+s.table+" ("+String.join(",",s.fields)+") VALUES ("+marks+")",vals.toArray());
        }else{
            var args=new ArrayList<>(vals.subList(1,vals.size()));args.add(vals.get(0));
            String set=String.join("=?,",Arrays.copyOfRange(s.fields,1,s.fields.length))+"=?";
            Db.change(c,"UPDATE "+s.table+" SET "+set+" WHERE "+s.key()+"=?",args.toArray());
        }
    }
    public static void remove(Connection c,Section s,String id)throws SQLException{
        Db.change(c,"DELETE FROM "+s.table+" WHERE "+s.key()+"=?",id);
    }
    public static boolean hasBookings(Connection c,String id)throws SQLException{
        return !Db.rows(c,"SELECT 1 FROM rental WHERE equipment_id=? AND status='Active' UNION ALL SELECT 1 FROM reservation WHERE equipment_id=? AND status='Reserved' AND end_date>=CURRENT_DATE",id,id).isEmpty();
    }
    public static boolean hasHistory(Connection c,String id)throws SQLException{
        return !Db.rows(c,"SELECT 1 FROM rental WHERE equipment_id=? UNION ALL SELECT 1 FROM reservation WHERE equipment_id=? UNION ALL SELECT 1 FROM maintenance WHERE equipment_id=?",id,id,id).isEmpty();
    }
    public static boolean categoryConflict(Connection c,String cat,String branch)throws SQLException{
        return !Db.rows(c,"SELECT 1 FROM equipment WHERE category_code=? AND branch_code<>? LIMIT 1",cat,branch).isEmpty();
    }
    public static void maintenance(Connection c,String id,boolean open,java.time.LocalDate from)throws SQLException{
        if(open)Db.change(c,"INSERT INTO maintenance(equipment_id,start_date) VALUES (?,?)",id,from);
        else Db.change(c,"UPDATE maintenance SET end_exclusive=GREATEST(start_date,CURRENT_DATE) WHERE equipment_id=? AND end_exclusive IS NULL",id);
    }
}
