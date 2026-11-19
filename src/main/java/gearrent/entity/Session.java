package gearrent.entity;
public record Session(String username, String role, String branch, boolean equipmentPermission) {
    public boolean admin() { return role.equals("Admin"); }
    public boolean manager() { return role.equals("Branch Manager"); }
    public void require(boolean allowed) {
        if (!allowed) throw new IllegalArgumentException("Your role or branch does not permit this operation.");
    }
    public void checkBranch(String code) { require(admin() || branch.equals(code)); }
}
