# BitLauncher — Roadmap & Matriks Fitur

Tujuan: launcher minimalis dengan rasa Niagara + Minima, tetapi sedalam Nova.
Dokumen ini memisahkan **yang sudah jadi** dari **yang direncanakan**, supaya tidak ada janji kosong.

Legenda: ✅ sudah ada · 🚧 sedang dikerjakan · 📋 direncanakan · ⛔ tidak mungkin/tidak etis

---

## 1. Matriks fitur vs launcher acuan

### Beranda

| Fitur | BitLauncher | Niagara | Minima | Nova |
|---|---|---|---|---|
| Daftar vertikal aplikasi favorit | ✅ | ✅ | — | — |
| Jam besar + tanggal | ✅ | ✅ | ✅ | ✅ |
| Grid ikon klasik | 📋 | — | ✅ | ✅ |
| Dock | 📋 | — | — | ✅ |
| Widget | 📋 | — | — | ✅ |
| Folder | 📋 | — | — | ✅ |
| Gestur geser untuk laci | ✅ | ✅ | — | ✅ |
| Double-tap area kosong | ✅ (buka laci) | — | — | ✅ |
| Tekan lama → menu | ✅ | ✅ | ✅ | ✅ |
| Wallpaper terlihat | ✅ | ✅ | ✅ | ✅ |
| Halaman ganda + indikator | 📋 | — | — | ✅ |
| Rotasi/backup layout | ✅ (favorit & setting) | — | — | ✅ |
| Widget stack ("at a glance") | 📋 | ✅ | — | — |

### Laci aplikasi & pencarian

| Fitur | BitLauncher | Niagara | Minima | Nova |
|---|---|---|---|---|
| Daftar semua aplikasi | ✅ | ✅ | ✅ | ✅ |
| Pencarian berperingkat | ✅ | ✅ | ✅ | ✅ |
| Pencarian kontak/web/pengaturan | 📋 | ✅ | — | ✅ |
| Tampilan daftar/kisi | ✅ | — | ✅ | ✅ |
| Rel alfabet | ✅ | ✅ | — | ✅ |
| Seksi huruf | ✅ | ✅ | — | ✅ |
| Sembunyikan aplikasi | ✅ | ✅ | ✅ | ✅ |
| Multi-profil / work profile | ✅ | ✅ | ✅ | ✅ |
| Tag & grup aplikasi | 📋 | ✅ | — | ✅ |

### Aksi & shortcut

| Fitur | BitLauncher | Niagara | Minima | Nova |
|---|---|---|---|---|
| Shortcut dinamis | ✅ | ✅ | ✅ | ✅ |
| Pin shortcut ke beranda | 📋 | ✅ | — | ✅ |
| Info & uninstall | ✅ | ✅ | ✅ | ✅ |
| Aksi ketuk/geser per ikon | 📋 | — | — | ✅ |
| Swipe pada ikon | 📋 | — | — | ✅ |

### Kustomisasi

| Fitur | BitLauncher | Niagara | Minima | Nova |
|---|---|---|---|---|
| Tema terang/gelap/AMOLED | ✅ | ✅ | ✅ | ✅ |
| Material You (dynamic color) | ✅ | ✅ | — | ✅ |
| Icon pack | 📋 | ✅ | — | ✅ |
| Bentuk ikon / mask | 📋 | — | — | ✅ |
| Ukuran & gaya font | 🚧 | — | — | ✅ |
| Animasi transisi | 📋 | ✅ | — | ✅ |
| Backup/restore | ✅ | — | — | ✅ |

### Sistem

| Fitur | BitLauncher | Niagara | Minima | Nova |
|---|---|---|---|---|
| Launcher default (RoleManager) | ✅ | ✅ | ✅ | ✅ |
| Ringkasan jumlah notifikasi | ✅ (via akses) | ✅ (via akses) | — | ✅ (Nova + plugin) |
| Gestur notifikasi | ⛔ | — | — | ✅ (aksesibilitas) |
| Double-tap matikan layar | 📋 | — | — | ✅ (admin) |
| Recents kustom | ⛔ | — | — | — |
| Kunci aplikasi biometrik | 📋 | — | — | — |

---

## 2. Fase pengembangan

### 0.1 — Fondasi (rilis saat ini)
- Launcher default, daftar aplikasi `LauncherApps`, multi-profil.
- Beranda vertikal, laci dengan pencarian, rel alfabet, daftar/kisi.
- Shortcut, pin, sembunyikan, info, uninstall.
- Tema, AMOLED, dynamic color, pengaturan dasar, backup JSON.
- Beranda folio berita: foto, artikel dengan shortcut aplikasi, dan ringkasan jumlah notifikasi.

### 0.2 — Beranda yang terasa "Niagara"
- Widget host (`AppWidgetHost` + `AppWidgetManager`) dan penempatan widget.
- Ikon adaptif & icon pack (parsing `appfilter.xml`, `IconPackHelper`).
- Double-tap area kosong dapat dikonfigurasi (buka laci / kunci layar).
- Drag & drop untuk menyusun ulang favorit.
- Header "at a glance": cuaca opsional + event kalender (izin terpisah).
- Animasi masuk/keluar ikon yang lebih halus (shared element).

### 0.3 — Kedalaman ala Nova
- Folder (di beranda dan dock).
- Gestur per ikon (geser untuk buka shortcut tertentu).
- Badge per ikon dan penyaringan kategori notifikasi yang lebih rinci.
- Pencarian universal: aplikasi + kontak + pengaturan sistem + pencarian web.
- Profil launcher (mis. "Kerja" 09.00–17.00, "Malam" setelah 20.00).
- Multi-halaman beranda + indikator + wallpaper scroll.

### 0.4 — Tablet, foldable, dan performa
- Layout adaptif berdasarkan `WindowSizeClass` (grid berbeda per postur).
- Baseline Profile + startup tracing (target < 300 ms cold start).
- Cache ikon persisten (disk) agar tidak reload setelah proses mati.
- Dukungan RTL penuh dan aksesibilitas (TalkBack, pengurangan animasi).

### 1.0 — Rilis publik
- Onboarding singkat: pilih bahasa, tema, impor dari launcher lama.
- Privasi: tanpa analytics, deklarasi data Play Store.
- Monetisasi opsional: satu kali beli "Pro" untuk tema/icon pack/backup cloud.
- Dokumentasi kontribusi + lisensi.

---

## 3. Keputusan teknis

| Topik | Keputusan | Alasan |
|---|---|---|
| Sumber daftar aplikasi | `LauncherApps` | Work profile, badging, callback, shortcut — semuanya sudah disediakan sistem |
| Persistensi pengaturan | `SharedPreferences` + `StateFlow` | Startup launcher harus instan; DataStore/Room terlalu berat untuk ±15 nilai |
| Favorit & hidden | Disimpan sebagai kunci `package/activity#user` | Bertahan saat label berubah dan aman untuk multi-profil |
| Ikon | `LruCache` 256 bitmap | Menghindari decode berulang saat scroll |
| Jam | Tick per menit (delay sampai detik ke-60) | Hemat baterai dibanding tick per detik |
| Backup | JSON via SAF (`CreateDocument`/`OpenDocument`) | Tanpa izin storage, ramah scoped storage |
| Navigasi | State enum + `AnimatedVisibility` | Tiga layar saja; Navigation-Compose menambah overhead & dependensi |

### Yang sengaja **tidak** dilakukan
- **RAM cleaner / mematikan aplikasi lain** — tidak diizinkan Android, menyesatkan pengguna.
- **Recents kustom** — API-nya dihapus sejak Android 5.
- **Membaca isi notifikasi** — listener hanya mengeluarkan nama paket dan jumlah; judul, isi pesan, dan aksi tidak dikirim ke UI.
- **Overlay/draw over other apps** untuk "game booster" — di luar cakupan launcher ini.

---

## 4. Metrik keberhasilan

| Metrik | Target |
|---|---|
| Cold start beranda | < 400 ms (Pixel 6a) |
| Warm start | < 120 ms |
| Frame drop saat scroll laci | < 1% (bukan jank) |
| Ukuran APK | < 8 MB |
| Izin Android | 0 izin berbahaya di manifest |
| Crash-free session | > 99.5% |
