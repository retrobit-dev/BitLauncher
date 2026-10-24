# BitLauncher — Bahasa Desain

Tujuan visual: **tenang, satu tangan, tanpa kebisingan.** Gaya utama adalah **folio koran di atas kertas E Ink Kaleido**. Gaya minimalis Niagara + Minima tetap tersedia; Nova memberi kedalaman saat dibutuhkan.

---

## Gaya utama — Folio koran + E Ink Kaleido

Beranda diperlakukan sebagai **halaman depan surat kabar**, bukan sebagai deretan ikon di atas wallpaper. Hierarkinya adalah masthead dengan pencarian yang dapat dibuka, headline, foto, artikel dengan tautan aplikasi, shelf aplikasi pilihan, lalu kartu editorial Kalender dan Catatan. Tidak ada indeks aplikasi permanen atau tombol navigasi di bagian bawah.

```
┌──────────────────────────────┐
│ BITLAUNCHER    EDISI 297  ⌕ │  ← ⌕ membuka kolom di samping nama
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│     SABTU, 24 OKTOBER 2026   │
│ KABAR UTAMA            HLM. 1│
│ 3 pesan menanti di WhatsApp  │  ← headline serif
│ ┌──────────────────────────┐ │
│ │      FOTO MONOKROM       │ │  ← foto utama
│ └──────────────────────────┘ │
│ Rembang, 24 Oktober 2026 —   │  ← tanggal bergaris bawah = kalender
│ Pada pukul 09.41 terjadi ... │
│ ada 3 pesan belum dibaca di  │
│ WhatsApp. Buka Catatan.      │  ← frasa bergaris bawah = app
│ APLIKASI DALAM ARTIKEL       │
│ [W WhatsApp] [S Spotify] [K]│  ← chip + badge hanya app pilihan
│ ┌────────────┐ ┌───────────┐ │
│ │ KALENDER   │ │ CATATAN   │ │  ← kartu editorial interaktif
│ │ 24         │ │ Catatan   │ │
│ └────────────┘ └───────────┘ │
│             ────             │  ← geser atas = arsip, bukan tombol
└──────────────────────────────┘
```

Foto artikel dapat diganti melalui pemilih dokumen sistem. Decode dilakukan di luar thread utama dengan downsample maksimum 1600 px; pada Kaleido, bitmap memakai filter chroma yang sama dengan ikon. Kegagalan membaca URI selalu kembali ke ilustrasi bawaan, bukan meninggalkan area kosong atau menghentikan launcher.

Artikel memakai kota, tanggal, dan jam sistem. Frasa aplikasi dicetak tebal dengan garis bawah dan dapat diketuk. Tanggal membuka kalender sistem; tautan dan kartu **Catatan** membuka aplikasi catatan yang terdeteksi otomatis atau dipilih di pengaturan. Kedua kartu dapat disembunyikan. Kartu ini adalah alat editorial internal, bukan host untuk widget pihak ketiga.

Jumlah pesan berasal dari **notifikasi aktif**, bukan dari membaca isi pesan. Jika akses notifikasi belum diberikan, artikel menampilkan keadaan kosong yang jujur serta tombol menuju pengaturan khusus Android; jumlah lama tidak tetap tampil di headline.

**Aplikasi di artikel** adalah daftar yang dipilih pengguna, bukan daftar favorit yang dipaksakan. Satu resolver memasok aplikasi yang sama ke headline, prosa, chip shortcut, dan badge shelf. Mode Otomatis mempertahankan perilaku lama; pilihan kustom dapat mencakup aplikasi tersembunyi; daftar kosong berarti benar-benar tanpa aplikasi/notifikasi artikel. Shelf menampilkan semua pilihan dalam grid tiga kolom, sedangkan prosa menyebut maksimal tiga aplikasi. Filter tidak mengubah badge global pada pencarian atau laci.

Laci menjadi **arsip**: judul besar `ARSIP`, jumlah judul, kolom pencarian bergaris tinta, huruf seksi dengan rule, dan nomor entri berkelanjutan. Mode kisi memakai sel persegi dengan garis tipis, bukan kartu membulat.

### Palet Kaleido

| Peran | Kaleido | Kaleido malam |
|---|---|---|
| Kertas / latar | `#E3DFD5` | `#191816` |
| Permukaan | `#EAE6DD` | `#201F1C` |
| Tinta utama | `#1D1C19` | `#E2DED2` |
| Tinta sekunder | `#666159` | `#A29C90` |
| Garis | `#979185` | `#6B665D` |
| Aksen utama | `#3E433D` | `#B9B09C` |

Aksen metadata arsip hanya memakai tiga tingkat abu-abu hangat. Ikon aplikasi dan shortcut digambar dengan **8% chroma** (92% menuju luminance abu-abu), seperti foto berwarna yang dicetak sangat lemah pada e-paper. Bitmap asli tetap disimpan di cache, sehingga warna langsung pulih saat pengguna keluar dari tema Kaleido.

### Paket ikon

- **Ikon tema** memakai foreground adaptive icon atau glyph monochrome sebagai alpha mask. Warna primer menjadi tinta, `surfaceVariant` menjadi bidang, dan garis `outline` memberi batas.
- Folio memakai sudut **2dp** seperti potongan kolom koran; Minimal memakai lingkaran. Ikon sistem mempertahankan bitmap asli dan filter 8% chroma pada Kaleido.
- Pack pihak ketiga tidak ditinta ulang agar desain pembuatnya tetap utuh. Aplikasi tanpa pemetaan kembali ke ikon asli, sehingga tidak ada placeholder kosong.
- Pilihan pack berlaku pada seluruh permukaan ikon dan diperbarui melalui composition local tanpa menyalin bitmap ke preferensi.

### Tipografi dan bentuk

- Secara default, headline, artikel, dan nama aplikasi memakai **serif**; metadata memakai sans uppercase dengan tracking 1.2–3.4sp.
- Pengguna dapat mengganti seluruh skala tipografi dengan **Lora**, **Roboto**, **Courier Prime (Typewriter)**, **Silkscreen (Pixel)**, atau **Space Mono (Operator-style)**. Semua font dikemas lokal; tidak ada unduhan saat runtime.
- Headline 34sp; artikel 17sp dengan line-height 26sp; nama favorit 23sp; label arsip 18sp; metadata 8–11sp.
- Sudut tombol dan sel kisi **2dp**; tidak ada pill, glass, blur, atau bayangan pada gaya folio.
- Garis ganda 2.5dp + 0.8dp menjadi penanda masthead; *dotted leader* menghubungkan nama aplikasi dengan chevron.
- Transisi beranda → arsip memakai fade agar terasa seperti pergantian halaman, bukan laci mengambang.


---

## 1. Prinsip — gaya minimalis

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

- Bawaan gaya minimalis: sans sistem. Pilihan eksplisit: Lora, Roboto, Courier Prime, Silkscreen, dan Space Mono.
- Bobot: Light/Regular untuk judul besar dan isi, Medium/Bold untuk aksi. Pilihan font diterapkan ke seluruh `Typography`, bukan hanya headline.
- Gambar artikel mengikuti SAF + persistable read permission; file tidak disalin atau diunggah oleh launcher.

---

## 4. Gestur

| Area | Gestur | Aksi |
|---|---|---|
| Beranda (kosong) | Tekan lama | Menu launcher |
| Beranda folio/minimal | Double tap | Aksi pilihan; default pencarian |
| Beranda folio/minimal | Tekan lama | Aksi pilihan; default menu launcher |
| Beranda folio | Geser atas di indikator bawah | Buka laci |
| Beranda folio/minimal | Geser kiri | Aksi pilihan; default pengaturan |
| Beranda folio/minimal | Geser kanan | Aksi pilihan; default panel notifikasi |
| Beranda folio/minimal | Geser dua jari atas/bawah | Aksi pilihan; default tidak aktif |
| Beranda | Ketuk jam | Buka alarm |
| Favorit | Ketuk | Buka aplikasi |
| Favorit | Tekan lama | Lembar aksi |
| Laci | Geser bawah pada grabber | Tutup laci |
| Laci | Ketuk rel alfabet | Lompat ke huruf |
| Laci | Tekan lama aplikasi | Lembar aksi |

Semua gestur di atas dapat dipetakan ke pencarian, arsip, pengaturan, menu, kalender, catatan, panel notifikasi, pengaturan cepat, recents, kunci layar, screenshot, atau **Tidak ada**. Gaya minimal mempertahankan alternatif terlihat berupa tombol/pill. Pada folio, indikator bawah memberi petunjuk aksi yang sedang dipilih.

Artikel, daftar aplikasi artikel, tautan aplikasi, ringkasan notifikasi, foto, widget, badge, dan paket ikon mempunyai pengaturan terpisah serta ikut dalam backup JSON. Pilihan font dan URI gambar juga disimpan; URI yang tidak lagi valid jatuh ke gambar bawaan.

---

## 5. Aksesibilitas

- Semua tombol ikon punya `contentDescription`; baris aplikasi memakai labelnya sebagai teks.
- Kontras teks beranda dijaga scrim, bukan mengandalkan shadow saja.
- Target sentuh utama ≥ 48 dp (pill 48 dp, tombol lingkaran 48 dp).
- Ukuran font mengikuti skala sistem (sp), dengan batas `maxLines` + ellipsis agar tidak merusak layout.
- Getaran bisa dimatikan di pengaturan.
- Layanan Aksesibilitas khusus gestur hanya menjalankan aksi global yang dipilih; konfigurasinya tidak meminta event atau konten jendela.
- Belum diuji dengan TalkBack secara menyeluruh — masuk backlog 0.4.
