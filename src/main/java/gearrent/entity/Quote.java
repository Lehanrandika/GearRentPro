package gearrent.entity;
import java.math.BigDecimal;
public record Quote(BigDecimal gross,BigDecimal longDiscount,BigDecimal memberDiscount,BigDecimal net,BigDecimal deposit){
    public BigDecimal payable(){return net.add(deposit);}
    public String toString(){return "Gross rent: "+gross+" | Long discount: "+longDiscount+" | Membership discount: "+memberDiscount+" | Net rent: "+net+" | Deposit: "+deposit+" | Total: "+payable();}
}
