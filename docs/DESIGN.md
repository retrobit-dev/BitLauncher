# BitLauncher — Bahasa Desain

Tujuan visual: **tenang, satu tangan, tanpa kebisingan.** Niagara memberi struktur, Minima memberi napas, Nova memberi kedalaman saat dibutuhkan.

---

## 1. Prinsip

1. **Wallpaper adalah latar, bukan hiasan.** Beranda transparan di atas wallpaper, dengan scrim vertikal tipis agar teks putih selalu terbaca.
2. **Satu kolom, satu keputusan.** Hal pertama yang dilihat adalah jam, lalu aplikasi yang benar-benar sering dipakai.
3. **Kedalaman lewat gestur, bukan tombol.** Aksi jarang dipakai (shortcut, sembunyikan, backup) hidup di tekan-lama dan pengaturan.
4. **Tidak ada dekorasi yang tidak berfungsi.** Tidak ada bayangan berlebih, tidak ada ikon dekoratif, tidak ada onboarding bertele-tele.
5. **Gerak yang tenang.** Transisi 250–300 ms, slide vertikal untuk laci, slide horizontal untuk pengaturan.

---

## 2. Wireframe

### Beranda (gaya Niagara)

```
┌──────────────────────────────┐
│ 09:41                        │  ← jam 76sp Light, putih
│ Sabtu, 24 Oktober            │  ← tanggal 16sp, putih 82%
│                              │
│  ▣  WhatsApp                  │  ← favorit 26sp Light
│  ▣  Spotify                   │
│  ▣  Kamera                    │
│  ▣  Catatan                   │
│                              │
│                              │
│  ╭────────────────────╮  ◯   │  ← pill pencarian + gear
│  │ ⌕  Cari aplikasi  ↑│      │
│  ╰────────────────────╯      │
│            ╭──╮               │  ← grabber, geser ke atas
└──────────────────────────────┘
```

### Laci aplikasi

```
┌──────────────────────────────┐
│            ────              │  ← grabber, tap/geser bawah = tutup
│ ╭──────────────────────╮ ▦ ⚙ │
│ │ ⌕ Cari aplikasi       │     │
│ ╰──────────────────────╯     │
├──────────────────────────────┤
│ A                            │  ← header seksi
│  ▣  Aplikasi A1              │
│  ▣  Aplikasi A2              │
│ B                            │
│  ▣  Browser             A    │  ← rel alfabet
│  ▣  Bank                B    │
│ ...                     C    │
└──────────────────────────────┘
```

Mode kisi: 3–6 kolom, label 12sp satu baris, ikon 46dp.

### Aksi aplikasi (tekan lama)

```
╭──────────────────────────────╮
│ ▣  WhatsApp                  │
│    Profil kerja · Ketuk buka │
│ ──────────────────────────── │
│ [Buka chat] [Kamera] [Baru]  │  ← shortcut dinamis
│ ──────────────────────────── │
│ ☆  Pin ke beranda            │
│ ⊘  Sembunyikan aplikasi      │
│ ⓘ  Info aplikasi             │
│ 🗑  Copot pemasangan          │
╰──────────────────────────────╯
```

### Pengaturan

Kartu bersudut 22dp di atas latar `surfaceVariant` 50%, judul kartu uppercase 11sp dengan letter-spacing 1.2.

---

## 3. Token

| Token | Nilai |
|---|---|
| Jarak | 4 / 8 / 12 / 16 / 20 / 24 dp |
| Sudut kartu | 22 dp |
| Sudut pill / bottom bar | 28 dp (atau 50%) |
| Tinggi baris favorit | 54 dp (ringkas 44 dp) |
| Ikon favorit | 30 dp (ringkas 24 dp) |
| Ikon laci daftar | 34 dp |
| Ikon laci kisi | 46 dp |
| Jam beranda | 76 sp, Light, tracking −2 |
| Label favorit | 26 sp, Light (ringkas 21 sp) |
| Target sentuh minimum | 48 dp |

### Warna

| Peran | AMOLED | Gelap | Terang |
|---|---|---|---|
| Background | `#000000` | `#101014` | `#F7F7FA` |
| Surface | `#0B0B0F` | `#15151A` | `#FFFFFF` |
| On surface | `#F3F3F6` | `#F3F3F6` | `#141418` |
| Surface variant | `#1B1B21` | `#202027` | `#ECECF1` |
| Outline | `#3B3B44` | `#3B3B44` | `#C8C8D0` |

Beranda selalu memakai teks **putih** + scrim gelap (0.38 → 0.10 → 0 → 0.58), apa pun temanya, karena latarnya wallpaper.
Material You aktif di Android 12+ kecuali tema AMOLED dipilih (agar hitam tetap hitam).

### Tipografi

- Keluarga: Roboto (default sistem) untuk v1. Font kustom (mis. Inter/Manrope) direncanakan di 0.2.
- Bobot: Light untuk judul besar, Normal untuk isi, Medium untuk aksi.

---

## 4. Gestur

| Area | Gestur | Aksi |
|---|---|---|
| Beranda (kosong) | Tekan lama | Menu launcher |
| Beranda (kosong) | Double tap | Buka laci + fokus pencarian |
| Beranda | Geser atas di bar bawah | Buka laci |
| Beranda | Ketuk jam | Buka alarm |
| Favorit | Ketuk | Buka aplikasi |
| Favorit | Tekan lama | Lembar aksi |
| Laci | Geser bawah pada grabber | Tutup laci |
| Laci | Ketuk rel alfabet | Lompat ke huruf |
| Laci | Tekan lama aplikasi | Lembar aksi |

Semua gestur punya alternatif yang terlihat (tombol/pill), sehingga pengguna baru tidak terjebak.

---

## 5. Aksesibilitas

- Semua tombol ikon punya `contentDescription`; baris aplikasi memakai labelnya sebagai teks.
- Kontras teks beranda dijaga scrim, bukan mengandalkan shadow saja.
- Target sentuh utama ≥ 48 dp (pill 48 dp, tombol lingkaran 48 dp).
- Ukuran font mengikuti skala sistem (sp), dengan batas `maxLines` + ellipsis agar tidak merusak layout.
- Getaran bisa dimatikan di pengaturan.
- Belum diuji dengan TalkBack secara menyeluruh — masuk backlog 0.4.
