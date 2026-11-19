package gearrent.dao;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
public final class Db {
    private final Properties config=new Properties();
    public Db() throws IOException {
        Path path=Path.of("db.properties");
        if(Files.exists(path)) try(var in=Files.newInputStream(path)){config.load(in);}
    }
    public interface Work<T>{T run(Connection c) throws Exception;}
    public <T>T transaction(Work<T> work)throws Exception{
        try(var c=DriverManager.getConnection(config.getProperty("url","jdbc:mariadb://localhost:3306/gearrent_coursework"),config.getProperty("user","root"),config.getProperty("password",""))){
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try{T result=work.run(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}
        }
    }
    private static PreparedStatement prepare(Connection c,String sql,Object... args)throws SQLException{
        var p=c.prepareStatement(sql);
        try {for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;}
        catch(SQLException e){p.close();throw e;}
    }
    public static List<Map<String,String>> rows(Connection c,String sql,Object... args)throws SQLException{
        try(var p=prepare(c,sql,args);var r=p.executeQuery()){
            List<Map<String,String>> rows=new ArrayList<>();
            var meta=r.getMetaData();
            while(r.next()){
                Map<String,String> row=new LinkedHashMap<>();
                for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),r.getString(i));
                rows.add(row);
            }
            return rows;
        }
    }
    public static Map<String,String> one(Connection c,String sql,Object... args)throws SQLException{
        var list=rows(c,sql,args);
        if(list.isEmpty())throw new IllegalArgumentException("Record not found. Check the ID and branch.");
        return list.get(0);
    }
    public static int change(Connection c,String sql,Object... args)throws SQLException{
        try(var p=prepare(c,sql,args)){return p.executeUpdate();}
    }
}
