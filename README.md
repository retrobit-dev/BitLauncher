# BitLauncher

Launcher Android dengan dua gaya visual: **folio koran di atas kertas E Ink Kaleido** (default) dan **minimalis ala Niagara + Minima**, dengan kedalaman fitur ala **Nova Launcher**.

Dibangun dengan Kotlin + Jetpack Compose, tanpa iklan, tanpa analytics, dan tanpa izin yang tidak perlu.

[![Android CI](https://github.com/retrobit-dev/BitLauncher/actions/workflows/android.yml/badge.svg)](https://github.com/retrobit-dev/BitLauncher/actions/workflows/android.yml)

---

## Pratinjau tampilan

Karena launcher ini belum dipasang di perangkat, tampilannya bisa dilihat lewat **prototipe UI interaktif** di folder [`preview/`](preview/index.html). Prototipe ini memakai token warna, ukuran tipografi, jarak, dan gestur yang sama dengan kode Compose, sehingga hasilnya mendekati APK.

```bash
python3 -m http.server 8080 --directory preview
# buka http://localhost:8080
```

### Tangkapan layar

### Folio koran · E Ink Kaleido (default)

| Halaman depan | Arsip aplikasi |
|---|---|
| ![Halaman depan folio](docs/preview-folio-home.jpg) | ![Arsip folio](docs/preview-folio-archive.jpg) |

| Kisi arsip | Kaleido malam |
|---|---|
| ![Kisi arsip folio](docs/preview-folio-grid.jpg) | ![Kaleido malam](docs/preview-folio-night.jpg) |

### Minimalis (gaya alternatif)

| Beranda | Laci aplikasi |
|---|---|
| ![Beranda minimalis](docs/preview-home.jpg) | ![Laci aplikasi minimalis](docs/preview-drawer.jpg) |

| Pengaturan | Aksi aplikasi (tekan lama) |
|---|---|
| ![Pengaturan](docs/preview-settings.jpg) | ![Aksi aplikasi](docs/preview-app-actions.jpg) |

Gambar dirender oleh [`tools/render_previews.py`](tools/render_previews.py) dan [`tools/render_folio_previews.py`](tools/render_folio_previews.py), memakai palet, ukuran, dan jarak yang sama dengan Compose. Untuk membuat ulang:

```bash
pip install pillow
python3 tools/render_previews.py
python3 tools/render_folio_previews.py
```

Yang bisa dicoba di prototipe:

- Ganti gaya **Folio koran / Minimalis** dan tema **Kaleido / Kaleido malam / Terang / Gelap / AMOLED** secara langsung.
- Beralih **Daftar / Kisi** dan jumlah kolom 3–6.
- **Tekan lama** aplikasi untuk membuka lembar aksi (shortcut, pin, sembunyikan, info, uninstall).
- **Ketuk** aplikasi untuk melihat umpan balik peluncuran.
- **Geser ke atas** pada grabber bawah atau **ketuk dua kali** beranda untuk membuka laci.
- **Tekan lama** area kosong beranda untuk menu ala Nova.
- Pencarian berperingkat, rel alfabet, dan manajer aplikasi tersembunyi (3 aplikasi contoh sudah disembunyikan).

> Prototipe ini HTML murni tanpa dependensi, bukan emulator Android. Perilaku Android asli (ikon aplikasi sungguhan, shortcut dinamis, profil kerja) hanya bisa dilihat dari APK di artifact CI.

---

## Konsep desain

| Gaya | Inspirasi | Wujud di BitLauncher |
|---|---|---|
| **Folio koran** | Surat kabar + e-paper | Masthead `BITLAUNCHER`, nomor edisi, garis ganda, jam serif, indeks aplikasi dengan *dotted leader*, tombol tinta persegi |
| **Kaleido** | E Ink Kaleido | Kertas gading, tinta gelap, aksen rose/sage/slate/ochre/mauve yang desaturasi; varian malam dengan tinta kertas terang |
| **Minimalis** | Niagara + Minima | Jam besar di atas wallpaper, satu kolom favorit, ruang kosong, bar pencarian membulat |
| **Kedalaman** | Nova | Tekan lama untuk aksi, shortcut, sembunyikan aplikasi, grid, backup/restore, tema |

Prinsipnya: **satu tangan, nol kebisingan.** Folio tampil seperti halaman depan koran; fitur yang jarang dipakai tetap berada satu lapis di belakang gestur.

---

## Fitur yang sudah berjalan (0.1.0-alpha)

### Beranda
- [x] **Gaya folio koran** dengan masthead, dateline, nomor edisi, dan indeks aplikasi editorial
- [x] Palet **Kaleido** dan **Kaleido malam**, dengan aksen cetak bergilir per entri
- [x] Menjadi launcher default (`CATEGORY_HOME` + `RoleManager`)
- [x] Jam besar + tanggal, sadar format 12/24 jam (mengikuti sistem secara default)
- [x] Ketuk jam → aplikasi alarm, seperti Pixel
- [x] Daftar aplikasi favorit: indeks koran (folio) atau kolom vertikal (minimalis)
- [x] Geser ke atas pada bar bawah untuk membuka laci
- [x] Tekan lama area kosong → menu ala Nova (pengaturan, wallpaper, launcher default, refresh)
- [x] Wallpaper terlihat di belakang beranda (window transparan + `windowShowWallpaper`)
- [x] Banner halus "jadikan launcher default" bila belum aktif

### Laci aplikasi & pencarian
- [x] Daftar seluruh aplikasi via `LauncherApps` (bukan `PackageManager` langsung)
- [x] Dukungan **multi-profil / work profile** dengan badge "kerja"
- [x] Pencarian berperingkat: awalan → substring → inisial → subsequence
- [x] Tampilan daftar dan kisi (3–6 kolom, bisa diubah dari laci maupun pengaturan)
- [x] Rel alfabet untuk lompat cepat, dengan getaran halus
- [x] Seksi per huruf
- [x] Pembaruan otomatis saat aplikasi dipasang/dicopot (`LauncherApps.Callback`)

### Aksi per aplikasi
- [x] Buka aplikasi
- [x] Shortcut dinamis/manifest/pinned di bottom sheet
- [x] Pin / lepas pin dari beranda
- [x] Sembunyikan aplikasi (beranda + laci)
- [x] Info aplikasi
- [x] Copot pemasangan lewat dialog sistem (fallback ke info untuk aplikasi profil kerja)

### Pengaturan
- [x] Gaya Folio / Minimalis dan tema Kaleido / Kaleido malam / Sistem / Terang / Gelap / AMOLED
- [x] Warna dinamis Material You (Android 12+)
- [x] Toggle jam, tanggal, format 24 jam, baris ringkas
- [x] Tata letak laci, jumlah kolom, rel alfabet, autofocus pencarian, getaran
- [x] Manajer aplikasi tersembunyi dengan pencarian
- [x] Pintasan launcher default dan pemilih wallpaper sistem
- [x] Ekspor/impor pengaturan ke JSON lewat Storage Access Framework

### Teknis
- [x] Material 3 + edge-to-edge
- [x] Cache ikon `LruCache` (256 entri) agar scroll tetap 60 fps
- [x] Clock hanya bangun sekali per menit, bukan per detik
- [x] State terpusat di `ViewModel` + `StateFlow`, preferensi ringan via `SharedPreferences`
- [x] CI GitHub Actions yang membangun APK debug

---

## Struktur proyek

```
app/src/main/java/dev/retrobit/bitlauncher/
├── MainActivity.kt              # entry point, edge-to-edge, wallpaper terlihat
├── LauncherViewModel.kt         # gabungan repo + pengaturan
├── data/
│   ├── LaunchableApp.kt         # model aplikasi (identitas stabil untuk favorit)
│   ├── LauncherRepository.kt    # LauncherApps, profil, callback, shortcut
│   └── SettingsRepository.kt    # preferensi + backup JSON
├── ui/
│   ├── LauncherApp.kt           # navigasi Beranda / Laci / Pengaturan
│   ├── home/{HomeScreen,FolioHomeScreen}.kt
│   ├── drawer/{AppDrawerScreen,FolioDrawerScreen}.kt
│   ├── settings/…
│   ├── components/…             # ikon, jam, rel alfabet, bottom sheet
│   └── theme/Theme.kt
└── util/
    ├── AppSearch.kt             # pencarian berperingkat
    ├── DrawerItems.kt           # pengelompokan per huruf
    └── SystemIntents.kt         # intent sistem & cek launcher default
```

---

## Build

Kebutuhan: JDK 17, Android SDK 35.

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Pasang ke perangkat:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity dev.retrobit.bitlauncher/.MainActivity
```

Atau ambil APK dari artifact workflow **Android CI** di tab Actions GitHub.

---

## Batasan yang diketahui

Ini fondasi yang serius, bukan launcher yang sudah selesai. Yang **belum** ada (dan alasannya):

| Belum ada | Catatan |
|---|---|
| Widget (`AppWidgetHost`) | Butuh host + konfigurasi + persistensi; prioritas berikutnya |
| Icon pack | Parsing `appfilter.xml` + masking ikon |
| Rel alfabet melompat ke seksi lengket | Saat ini header ikut ter-scroll |
| Badge notifikasi | Butuh `NotificationListenerService` + izin khusus pengguna |
| Double-tap untuk matikan layar | Butuh Device Admin / Accessibility, sengaja ditunda |
| Recents kustom | Tidak mungkin sejak Android 5 (API dihapus) |

Detail rencana ada di [docs/ROADMAP.md](docs/ROADMAP.md), sedangkan wireframe dan token desain ada di [docs/DESIGN.md](docs/DESIGN.md).

---

## Lisensi

Belum ditentukan. Sebelum rilis publik, tambahkan lisensi (MIT/Apache-2.0 disarankan) dan file `LICENSE`.
