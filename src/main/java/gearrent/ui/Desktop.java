package gearrent.ui;
import gearrent.controller.DesktopController;
import gearrent.entity.*;
import gearrent.service.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;
import static gearrent.ui.Forms.*;
public class Desktop extends JFrame {
    private final DesktopController ctrl;
    private final JTabbedPane tabs=new JTabbedPane();
    private static final String[] PAY={"Paid","Partially Paid","Unpaid"};
    private Desktop(DesktopController ctrl)throws Exception{
        super("GearRent Pro | "+ctrl.user.username()+" | "+ctrl.user.role()+" | "+Objects.toString(ctrl.user.branch(),"All branches"));
        this.ctrl=ctrl;
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1100,680);setLocationRelativeTo(null);
        for(var s:Section.values())if(s!=Section.SETTINGS||ctrl.user.admin())tabs.addTab(s.title,catalog(s));
        for(var name:List.of("Reservations","Rentals","Overdue"))tabs.addTab(name,transactions(name));
        if(ctrl.user.admin()||ctrl.user.manager())for(var name:List.of("Revenue","Utilization"))tabs.addTab(name,report(name));
        add(tabs);setVisible(true);
    }
    public static void login(Accounts auth,Catalog catalog,Rentals rentals,Reports reports){
        var user=new JTextField(18);var pass=new JPasswordField(18);
        var form=new JPanel(new GridLayout(0,2,5,5));form.add(new JLabel("Username"));form.add(user);form.add(new JLabel("Password"));form.add(pass);
        while(JOptionPane.showConfirmDialog(null,form,"GearRent Pro login",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            try{var session=auth.login(user.getText(),pass.getPassword());pass.setText("");
                new Desktop(new DesktopController(session,catalog,rentals,reports));return;
            }catch(Exception e){Forms.error(null,e);}
        }
    }
    private String[] ids(Section s,String field,boolean blank)throws Exception{
        List<String> out=new ArrayList<>();if(blank)out.add("");
        for(var row:ctrl.list(s))out.add(row.get(field));return out.toArray(String[]::new);
    }
    private JPanel catalog(Section s)throws Exception{
        var page=new JPanel(new BorderLayout());var grid=new Grid();var tools=new JPanel(new FlowLayout(FlowLayout.LEFT));
        var filter=new JTextField(12);var column=new JComboBox<String>();column.addItem("All fields");for(String f:s.fields)column.addItem(f);
        Task refresh=()->{
            String term=filter.getText().trim().toLowerCase(Locale.ROOT);String key=(String)column.getSelectedItem();
            var rows=ctrl.list(s).stream().filter(row->{
                Collection<String> vals="All fields".equals(key)?row.values():Collections.singletonList(row.get(key));
                return vals.stream().filter(Objects::nonNull).anyMatch(v->v.toLowerCase(Locale.ROOT).contains(term));
            }).toList();
            grid.showRows(rows);
        };
        tools.add(column);tools.add(filter);tools.add(button("Search / Refresh",refresh));
        if(ctrl.writable(s)){
            if(s!=Section.SETTINGS)tools.add(button("Add",()->{edit(s,null);refresh.run();}));
            tools.add(button("Edit",()->{edit(s,grid.selected());refresh.run();}));
            if(s!=Section.SETTINGS)tools.add(button("Delete",()->{
                String id=grid.selected().get(s.key());
                if(JOptionPane.showConfirmDialog(this,"Delete "+id+"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){ctrl.delete(s,id);refresh.run();}
            }));
        }
        if(s==Section.CUSTOMER)tools.add(button("Rental history",()->{
            var id=grid.selected().get("customer_id");var history=new Grid();
            history.showRows(ctrl.search("Rentals","",id,"","",""));
            var panel=new JPanel(new BorderLayout());panel.add(new JLabel("Total deposit held across branches: LKR "+ctrl.held(id)),BorderLayout.NORTH);panel.add(history);
            panel.setPreferredSize(new Dimension(820,330));JOptionPane.showMessageDialog(this,panel,"Customer "+id+" | visible rentals",JOptionPane.PLAIN_MESSAGE);
        }));
        page.add(tools,BorderLayout.NORTH);page.add(grid);refresh.run();return page;
    }
    private void edit(Section s,Map<String,String> old)throws Exception{
        var values=new LinkedHashMap<String,String>();for(String f:s.fields)values.put(f,old==null?"":old.get(f));
        var choices=new HashMap<String,String[]>();var locked=new HashSet<String>();if(old!=null)locked.add(s.key());
        if(s==Section.EQUIPMENT||s==Section.CATEGORY){
            choices.put("branch_code",ids(Section.BRANCH,"branch_code",s==Section.CATEGORY&&ctrl.user.admin()));
            if(!ctrl.user.admin()){values.put("branch_code",ctrl.user.branch());locked.add("branch_code");}
        }
        if(s==Section.EQUIPMENT){
            choices.put("category_code",ids(Section.CATEGORY,"category_code",false));
            choices.put("status",new String[]{"Available","Under Maintenance"});
            if(!"Under Maintenance".equals(values.get("status")))values.put("status","Available");
        }
        if(s==Section.CATEGORY){
            choices.put("is_active",new String[]{"1","0"});
            if(old==null){values.put("base_price_factor","1.00");values.put("weekend_multiplier","1.00");values.put("is_active","1");}
            else if(Set.of("true","false").contains(values.get("is_active")))values.put("is_active",Boolean.parseBoolean(values.get("is_active"))?"1":"0");
        }
        if(s==Section.CUSTOMER)choices.put("level_name",ids(Section.MEMBERSHIP,"level_name",true));
        var result=dialog(this,(old==null?"Add ":"Edit ")+s.title,values,choices,locked);
        if(result!=null)ctrl.save(s,result,old!=null);
    }
    private JPanel transactions(String view)throws Exception{
        var page=new JPanel(new BorderLayout());var grid=new Grid();
        var branch=new JComboBox<>(ids(Section.BRANCH,"branch_code",ctrl.user.admin()));
        var customer=new JTextField(7);var from=new JTextField(9);var to=new JTextField(9);
        boolean bookings=view.equals("Reservations"),overdue=view.equals("Overdue");
        var state=new JComboBox<>(bookings?new String[]{"","Reserved","Cancelled","Converted"}:new String[]{"","Active","Returned","Overdue","Cancelled"});
        Task refresh=()->grid.showRows(ctrl.search(view,Objects.toString(branch.getSelectedItem(),""),customer.getText(),overdue?"":(String)state.getSelectedItem(),from.getText(),to.getText()));
        var filter=new JPanel(new FlowLayout(FlowLayout.LEFT));
        filter.add(new JLabel("Branch"));filter.add(branch);filter.add(new JLabel("Customer ID"));filter.add(customer);
        if(!overdue){filter.add(new JLabel("Status"));filter.add(state);}
        filter.add(new JLabel("From"));filter.add(from);filter.add(new JLabel("To"));filter.add(to);filter.add(button("Search / Refresh",refresh));
        var actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        if(!overdue){
            actions.add(button("Price quote",()->book(Rentals.Action.QUOTE)));
            actions.add(button(bookings?"New reservation":"Issue rental",()->{book(bookings?Rentals.Action.RESERVE:Rentals.Action.ISSUE);refresh.run();}));
        }
        if(bookings){
            actions.add(button("Convert to rental",()->{
                int id=id(grid,"reservation_id");String p=choosePayment();if(p!=null){message(ctrl.convert(id,p));refresh.run();}
            }));
            actions.add(button("Cancel reservation",()->{
                int id=id(grid,"reservation_id");if(JOptionPane.showConfirmDialog(this,"Cancel reservation "+id+"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){ctrl.cancel(id);refresh.run();}
            }));
        }else{
            actions.add(button("Return equipment",()->{
                int id=id(grid,"rental_id");var v=dialog(this,"Return rental "+id,fields("Returned on",LocalDate.now().toString(),"Damage description","","Damage charge","0.00"),Map.of(),Set.of());
                if(v!=null){message(ctrl.settle(id,v));refresh.run();}
            }));
            actions.add(button("Update payment",()->{int id=id(grid,"rental_id");String p=choosePayment();if(p!=null){ctrl.payment(id,p);refresh.run();}}));
        }
        var top=new JPanel(new GridLayout(2,1));top.add(filter);top.add(actions);page.add(top,BorderLayout.NORTH);page.add(grid);refresh.run();return page;
    }
    private void book(Rentals.Action action)throws Exception{
        String today=LocalDate.now().toString();
        var dates=dialog(this,"Choose branch and inclusive dates",fields("Branch","","Start date",today,"End date",today),
            Map.of("Branch",ids(Section.BRANCH,"branch_code",false)),Set.of());
        if(dates==null)return;
        var items=ctrl.available(dates.get("Branch"),dates.get("Start date"),dates.get("End date"));
        if(items.isEmpty())throw new IllegalArgumentException("No equipment available in this branch for these dates.");
        var values=fields("Equipment ID","","Customer ID","","Start date",dates.get("Start date"),"End date",dates.get("End date"));
        var options=new HashMap<String,String[]>();options.put("Equipment ID",items.toArray(String[]::new));options.put("Customer ID",ids(Section.CUSTOMER,"customer_id",false));
        if(action==Rentals.Action.ISSUE){values.put("Payment","Paid");options.put("Payment",PAY);}
        var v=dialog(this,action.toString()+" | "+dates.get("Branch"),values,options,Set.of("Start date","End date"));
        if(v!=null)message(ctrl.book(action,v));
    }
    private JPanel report(String name)throws Exception{
        var p=new JPanel(new BorderLayout());var g=new Grid();var filter=new JPanel();
        var br=new JComboBox<>(ids(Section.BRANCH,"branch_code",name.equals("Revenue")&&ctrl.user.admin()));
        var from=new JTextField(LocalDate.now().minusDays(29).toString(),10);var to=new JTextField(LocalDate.now().toString(),10);
        Task run=()->g.showRows(ctrl.report(name,Objects.toString(br.getSelectedItem(),""),from.getText(),to.getText()));
        filter.add(new JLabel("Branch"));filter.add(br);filter.add(new JLabel("From"));filter.add(from);filter.add(new JLabel("To"));filter.add(to);filter.add(button("Run report",run));
        p.add(filter,BorderLayout.NORTH);p.add(g);return p;
    }
    private int id(Grid grid,String field){return Integer.parseInt(grid.selected().get(field));}
    private String choosePayment(){return (String)JOptionPane.showInputDialog(this,"Payment status","Payment",JOptionPane.QUESTION_MESSAGE,null,PAY,PAY[0]);}
    private void message(String msg){JOptionPane.showMessageDialog(this,msg);}
}
