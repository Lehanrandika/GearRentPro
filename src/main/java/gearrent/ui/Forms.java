package gearrent.ui;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
class Forms {
    interface Task{void run()throws Exception;}
    static JButton button(String label,Task action){
        var b=new JButton(label);b.addActionListener(e->{try{action.run();}catch(Exception ex){error(b,ex);}});return b;
    }
    static void error(Component owner,Exception ex){
        String msg=ex.getMessage();
        if(ex instanceof java.sql.SQLException sql){
            msg=switch(sql.getErrorCode()){
                case 1062 -> "This ID or unique value already exists.";
                case 1451 -> "Other records use this entry. Keep it or deactivate it.";
                case 1452 -> "Check the linked ID. The referenced record must exist.";
                case 1406 -> "One of the entered values is too long.";
                case 1213,1205 -> "Another user is editing this data. Refresh and retry.";
                default -> msg;
            };
        }
        JOptionPane.showMessageDialog(owner,msg,"GearRent Pro",JOptionPane.ERROR_MESSAGE);
    }
    static class Grid extends JPanel{
        final JTable table=new JTable();
        List<Map<String,String>> data=List.of();
        Grid(){super(new BorderLayout());table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);add(new JScrollPane(table));}
        void showRows(List<Map<String,String>> rows){
            data=rows;
            String[] cols=rows.isEmpty()?new String[]{"No matching records"}:rows.get(0).keySet().toArray(String[]::new);
            var model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
            for(var row:rows)model.addRow(Arrays.stream(cols).map(row::get).toArray());
            table.setModel(model);
            for(int i=0;i<cols.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(Math.max(115,cols[i].length()*9+32));
        }
        Map<String,String> selected(){
            int row=table.getSelectedRow();
            if(row<0)throw new IllegalArgumentException("Select a row first.");
            return data.get(table.convertRowIndexToModel(row));
        }
    }
    static Map<String,String> dialog(Component parent,String title,Map<String,String> initial,Map<String,String[]> options,Set<String> readOnly){
        var panel=new JPanel(new GridLayout(0,2,8,6));var fields=new LinkedHashMap<String,JComponent>();
        initial.forEach((label,value)->{
            panel.add(new JLabel(label.replace('_',' ')));JComponent input;
            if(options.containsKey(label)){
                var combo=new JComboBox<>(options.get(label));if(value!=null&&!value.isEmpty())combo.setSelectedItem(value);input=combo;
            }else input=new JTextField(Objects.toString(value,""),20);
            input.setEnabled(!readOnly.contains(label));panel.add(input);fields.put(label,input);
        });
        if(JOptionPane.showConfirmDialog(parent,panel,title,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return null;
        var result=new LinkedHashMap<String,String>();
        fields.forEach((k,v)->result.put(k,v instanceof JTextField t?t.getText().trim():Objects.toString(((JComboBox<?>)v).getSelectedItem(),"")));
        return result;
    }
    static Map<String,String> fields(String... pairs){
        var v=new LinkedHashMap<String,String>();for(int i=0;i<pairs.length;i+=2)v.put(pairs[i],pairs[i+1]);return v;
    }
}
