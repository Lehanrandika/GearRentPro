package gearrent.service;
import gearrent.dao.*;
import gearrent.entity.Session;
import java.util.*;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public class Accounts {
    private final Db db;
    public Accounts(Db db){this.db=db;}
    public Session login(String username,char[] password)throws Exception{
        try{return db.transaction(c->{
            var u=CatalogDao.user(c,username.trim());
            if(u!=null){
                var spec=new PBEKeySpec(password,HexFormat.of().parseHex(u.get("salt_hex")),120000,256);
                try{
                    var hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                    if(MessageDigest.isEqual(hash,HexFormat.of().parseHex(u.get("hash_hex"))))
                        return new Session(u.get("username"),u.get("role"),u.get("branch_code"),"1".equals(u.get("can_edit_equipment"))||"true".equalsIgnoreCase(u.get("can_edit_equipment")));
                }finally{spec.clearPassword();}
            }
            throw new IllegalArgumentException("Incorrect username or password.");
        });}finally{Arrays.fill(password,'\0');}
    }
}
