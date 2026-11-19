package gearrent.entity;
public enum Section {
    BRANCH("Branches","branch","branch_code,name,address,contact"),
    CATEGORY("Categories","category","category_code,name,description,base_price_factor,weekend_multiplier,is_active,branch_code"),
    EQUIPMENT("Equipment","equipment","equipment_id,brand,model,purchase_year,daily_base_price,security_deposit,status,branch_code,category_code"),
    CUSTOMER("Customers","customer","customer_id,name,nic_or_passport,contact_no,email,address,level_name"),
    MEMBERSHIP("Membership","membership_level","level_name,discount_percent"),
    SETTINGS("Pricing rules","settings","id,deposit_limit,long_discount,late_fee");
    public final String title,table;
    public final String[] fields;
    Section(String title,String table,String columns){this.title=title;this.table=table;this.fields=columns.split(",");}
    public String key(){return fields[0];}
}
