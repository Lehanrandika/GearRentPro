package gearrent;
import gearrent.dao.Db;
import gearrent.service.*;
import gearrent.ui.Desktop;
import javax.swing.*;
public class Main{
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());var db=new Db();
                Desktop.login(new Accounts(db),new Catalog(db),new Rentals(db),new Reports(db));
            }catch(Exception e){JOptionPane.showMessageDialog(null,e.getMessage());}
        });
    }
}
