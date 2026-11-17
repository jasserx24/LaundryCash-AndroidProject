# Laundry Cash – Android app

App para i-record ang cash sa bawat pag-out:

    Total Sales
    Less: Unpaid
    Add: Claimed
    Less: Expenses
    = TOTAL CASH ON HAND

## Paano makuha ang .apk (walang kailangang i-install sa computer)

1. Gumawa ng libreng account sa https://github.com
2. Pindutin ang **+ → New repository**, pangalanan ng `LaundryCash`, tapos **Create repository**.
3. Sa bagong repo, i-click ang **"uploading an existing file"**, i-drag ang LAHAT ng laman ng folder na ito
   (kasama ang `.github` folder), tapos **Commit changes**.
   - Tip: kung hindi lumalabas ang `.github` folder sa computer, i-enable ang "show hidden files".
4. Pumunta sa tab na **Actions** → hintayin ang **Build APK** na maging berde (mga 3–5 minuto).
5. I-click ang build → sa ibaba, i-download ang **LaundryCash-APK** (zip na may `LaundryCash.apk`).
6. Ipadala ang `LaundryCash.apk` sa phone, buksan, at payagan ang "Install unknown apps".

## Kung may Android Studio
Buksan ang folder na ito → Build → Build APK(s).

## Paalala
- Ang data ay naka-save sa phone mismo (offline). Gamitin ang **Iba pa → I-backup** paminsan-minsan.
- Kapag na-uninstall ang app, mabubura ang data maliban kung may backup.
