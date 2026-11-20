# GearRent Pro
Multi-branch equipment rental desktop application. Java 17+, Swing, JDBC, Maven, and an InnoDB database. The coursework allows Swing as a UI option. MariaDB Connector/J connects to both MySQL and MariaDB

## Demo logins

| Username | Password | Role | Access |
|---|---|---|---|
| admin | GearRent123! | Admin | All branches, configuration and reports |
| manager | GearRent123! | Branch Manager | Galle inventory, branch categories, bookings, rentals and reports |
| staff | GearRent123! | Staff | Galle daily operations, customers and permitted inventory editing |

Passwords in the database are salted PBKDF2-HMAC-SHA256 hashes. The shared password is a local coursework demonstration credential. Keep personal database credentials in the ignored db.properties file. Use synthetic data for demonstrations.

## First walkthrough

1. Sign in as manager. Open Equipment, choose equipment_id, search for EQ001. It belongs to BR001 (Galle) and category CAT01 (Camera).
2. In Reservations, the sample booking for CU001 starts two days after import. Use Price quote to try the same dates. The available equipment list excludes EQ001 for that period.
3. Create a new reservation in Galle with today as both start and end. Pick any listed item and CU001, then confirm. Select the new row and choose Convert to rental. Pick a payment status.
4. Open Rentals, click Search / Refresh, select the new rental and choose Return equipment. Use today, a blank damage description and 0. The dialog shows the deposit refund. Press Refresh after changes in another tab.
5. Sign in as admin by closing and reopening the app. Overdue shows seeded rental EQ005 at Panadura. Return it with today's date, describe a damage and set a charge. The fee breakdown appears, the overdue entry disappears and Equipment shows Under Maintenance.
6. Open Revenue and run a report for the last 30 days. Open Utilization, select Galle and run the same date range. Check Customers → CU001 → Rental history. History follows login branch permissions. The deposit total covers the customer across all branches.
7. In Pricing rules, select row 1 and use Edit to change the deposit limit, long-rental discount or global late fee. In Membership edit the three membership rates. In Categories set is_active to 0 to stop new bookings. Managers can maintain their branch's local categories. Admin maintains shared categories.
8. For CRUD use Add, Edit or Delete on the appropriate tab. Select a row before editing or deleting. Linked records are protected by foreign keys. Use the field picker in Equipment to search by branch, category, status, brand or model. Reopen the window after adding a branch to refresh the branch choices in existing report and search toolbars.

## Database and sample data

database/gearrent.sql is the complete schema and sample-data script. It uses a separate database so it does not overwrite the earlier practice tables. It uses the same first customer, branch, category and camera IDs. Custom records from the practice database are not automatically copied. SQL requires permission to create the database, tables and views. Import into a fresh gearrent_coursework database once.

| Data | Included |
|---|---|
| Branches | 3 — Galle, Panadura, Colombo |
| Categories | 5 — Camera, Lens, Drone, Lighting, Audio |
| Equipment | 21 — seven at each branch |
| Customers | 10 — Regular, Silver, Gold and customers without membership |
| Transactions | Future reservation, overdue rental, returned rental with damage and settlement |

Check the import in phpMyAdmin's SQL tab:

```sql
USE gearrent_coursework;
SELECT COUNT(*) AS branches FROM branch;
SELECT COUNT(*) AS equipment_items FROM equipment;
SELECT COUNT(*) AS customers FROM customer;
SELECT rental_id, equipment_id, rental_status FROM rental_list;
```

## Source layout

| Folder | Responsibility |
|---|---|
| src/main/java/gearrent/ui | Swing windows, input dialogs and result tables |
| src/main/java/gearrent/controller | Accept UI commands, parse dates and invoke services |
| src/main/java/gearrent/service | Authentication, permissions, pricing, booking, returns and reporting rules |
| src/main/java/gearrent/dao | JDBC connections, transaction wrapper, parameterized reads and writes |
| src/main/java/gearrent/entity | Session, quote and catalog definitions |
| database | MySQL/MariaDB schema, views, sample transactions and seed data |


## GitHub submission Link

https://github.com/Lehanrandika/GearRentPro