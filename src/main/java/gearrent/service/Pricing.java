package gearrent.service;
import gearrent.entity.Quote;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.*;
public class Pricing {
    public static long days(LocalDate start,LocalDate end){
        long days=ChronoUnit.DAYS.between(start,end)+1;
        if(days<1||days>30)throw new IllegalArgumentException("Choose a rental period of 1 to 30 days, counting both dates.");
        return days;
    }
    private static BigDecimal money(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal discount(BigDecimal n,BigDecimal percent){return money(n.multiply(percent).movePointLeft(2));}
    public static Quote quote(LocalDate start,LocalDate end,BigDecimal base,BigDecimal factor,BigDecimal weekend,BigDecimal longRate,BigDecimal memberRate,BigDecimal deposit){
        long count=days(start,end);
        var gross=BigDecimal.ZERO.setScale(2);
        for(var d=start;!d.isAfter(end);d=d.plusDays(1)){
            var multiplier=d.getDayOfWeek().getValue()>=6?weekend:BigDecimal.ONE;
            gross=gross.add(money(base.multiply(factor).multiply(multiplier)));
        }
        var duration=discount(gross,count>=7?longRate:BigDecimal.ZERO);
        var member=discount(gross.subtract(duration),memberRate);
        return new Quote(gross,duration,member,gross.subtract(duration).subtract(member),deposit);
    }
}
