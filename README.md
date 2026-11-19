# GearRent Pro
Multi-branch equipment rental desktop application. Java 17+, Swing, JDBC, Maven, and an InnoDB database. The coursework allows Swing as a UI option. MariaDB Connector/J connects to both MySQL and MariaDB.

## මුලින්ම run කරන විදිහ (Windows)

1. ZIP එක **Extract All** කරන්න. GearRentPro folder එක ඇතුළේ pom.xml සහ database folder එක තියෙනවාද බලන්න.
2. XAMPP Control Panel එකෙන් **Apache** සහ **MySQL** Start කරන්න. MySQL පේළියේ **Admin** ඔබලා phpMyAdmin open කරන්න.
3. phpMyAdmin මුල් පිටුවේ **Import → Choose File** යන්න. database/gearrent.sql තෝරලා පහළ **Import / Go** ඔබන්න. සම්පූර්ණ script එක අලුත් setup එකකට **එක පාරක්** import කරන්න. වම් පැත්තේ **gearrent_coursework** පේන්න ඕන. ඔයා කලින් හදපු gearrent_pro database එකත් වෙනම තියෙනවා.
4. MySQL Server 8.0.16+ භාවිත කරනවා නම් ඒ SQL file එක MySQL Workbench එකේ **File → Open SQL Script** වලින් open කරලා මුළු script එකම execute කරන්න. Coursework එක MySQL ඉල්ලන නිසා final demonstration එකට MySQL Server එකෙන් test කරන්න.
5. GearRentPro folder එකේ db.properties.example එක copy කරලා **db.properties** කියලා නම දෙන්න. Port එක, database username එක සහ password එක ඔයාගේ database setup එකට ගැළපෙන විදිහට දාන්න. Default එක local port 3306, user root, හිස් password. මේක database login එක. App එකට log වෙන්න වෙනම credentials පහළ තියෙනවා.
6. IntelliJ IDEA වල **Open** ඔබලා GearRentPro folder එක තෝරන්න. Project SDK එකට JDK 17 හෝ 21 තෝරලා Maven project එක load වෙන්න දෙන්න. src/main/java/gearrent/Main.java open කරලා main method එක ළඟ Run ඔබන්න. පළමු build එකට dependencies download කිරීමට internet ඕන.
7. Maven install කරලා තියෙනවා නම් වෙනත් ක්‍රමයක් විදිහට pom.xml තියෙන folder එකේ terminal එක open කරලා මේ command එක run කරන්න.

```sh
mvn clean compile exec:java
```

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

## Business rules used in this implementation

- Dates count both endpoints. A same-day rental is one day. Accept 1–30 days. A rental is issued on its start date. Book future dates with a reservation. Cancel an expired uncollected booking and create a new date range to issue it.
- Gross rent is the sum of daily prices, rounded to cents per day. Saturday and Sunday use the weekend multiplier. At 7 days apply the long discount first, then the member discount on the remaining rent. Add the security deposit last. Amounts are in LKR.
- Example: seven days Monday–Sunday for EQ001 are 5 × 2,000 + 2 × 2,400 = 14,800. An 8% long discount gives 13,616. With Silver at 5% the net rent is 12,935.20. Including the 10,000 deposit gives 22,935.20.
- Demo rates are configurable assumptions: long discount 8%, Regular 0%, Silver 5%, Gold 10%, deposit cap 500,000 and late fee 500 per day. The limit applies across all active and overdue rentals. A reservation checks the projected balance, and conversion checks it again. An unissued reservation does not collect a deposit.
- Staff collect the deposit in full when issuing an item. Payment status tracks rent (Paid, Partially Paid or Unpaid); it is maintained by staff. The return dialog settles deposit against late/damage charges. Any unpaid rent is tracked separately. A nonempty damage description also supports damage with zero assessed charge.
- Booking and conversion check date overlaps on the same item. An overdue item with no actual return date is blocked until returned. Calendar availability and current inventory status are distinct. Reservation and rental actions set Reserved and Rented in the inventory view. Under Maintenance is set on damage or by inventory editing. Repair completion is recorded by setting Available.
- A return records actual return date, damage description, late/damage charges, refund and extra due in one transaction. Cancellation in this application is for open reservations. A returned item can be issued again starting the following day under the inclusive-date policy.
- InnoDB transactions lock the equipment and customer rows and recheck business rules. Reservation conversion also locks the reservation and excludes it from its own overlap test. Errors roll back the entire operation.
- Customers are shared across branches. Operational records, inventory and reports follow branch permissions. The seeded staff user has equipment-edit permission. Managers may view global categories and create/edit branch-owned categories. Admin maintains global configuration.
- Branch revenue uses billed net rent, excluding deposits, counted on the issue date. Counts include unpaid rentals; this is an accrual report. Late/damage amounts belong to their return date. Utilization = distinct rented calendar days / selected calendar days × 100. Reports are historical through today. Available days exclude reserved and maintenance days. The return day counts as rented and maintenance starts the following calendar day. Equipment with transaction history stays with its original branch. The sample assumes listed equipment was in service throughout the selected reporting period.

## Source layout

| Folder | Responsibility |
|---|---|
| src/main/java/gearrent/ui | Swing windows, input dialogs and result tables |
| src/main/java/gearrent/controller | Accept UI commands, parse dates and invoke services |
| src/main/java/gearrent/service | Authentication, permissions, pricing, booking, returns and reporting rules |
| src/main/java/gearrent/dao | JDBC connections, transaction wrapper, parameterized reads and writes |
| src/main/java/gearrent/entity | Session, quote and catalog definitions |
| database | MySQL/MariaDB schema, views, sample transactions and seed data |

## Troubleshooting

- Connection refused: start the database service and check the port in db.properties.
- Access denied: check the database username and password. Use the GearRent demo logins only in the application's login window.
- Unknown database or missing table: import database/gearrent.sql and verify the database name.
- No suitable driver: load the Maven project or use the Maven command above so the JDBC dependency is on the classpath.
- Duplicate table during import: check whether you already imported the script. For a partly imported database, export a backup and use a fresh database name in the first two SQL statements and in db.properties before retrying.
- Branch or category unavailable in an operation: check the login role, ownership, active flag, dates, maintenance and overlaps.

## GitHub submission

This project includes a local Git repository with commits for the database, Java implementation and setup guide. Extract the entire ZIP before opening it. To submit, create an empty GitHub repository under your account, then run these commands from GearRentPro, substituting your own repository URL.

```sh
git remote add origin https://github.com/YOUR-USERNAME/GearRentPro.git
git push -u origin main
```

Submit your own repository URL. Keep the SQL script and README with the source.

JDBC driver documentation: https://mariadb.com/docs/connectors/mariadb-connector-j/java-connector-using-maven
