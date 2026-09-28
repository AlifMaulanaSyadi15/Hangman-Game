package com.example.hangman

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText                       // [OPSI: INPUT KETIK]
import android.widget.GridLayout                     // [LATIHAN 1]
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton   // [LATIHAN 1]

/*
 * =====================================================================
 *  PETA PENANDA (cari dengan Ctrl+Shift+F, atau Ctrl+F di file ini)
 *
 *   [OPSI: INPUT KETIK]   -> kolom ketik huruf + tombol Tebak
 *   [LATIHAN 1]           -> tombol huruf A-Z
 *   [LATIHAN 2]           -> animasi fade gambar gantungan
 *   [LATIHAN 3]           -> petunjuk di overlay mulai (hanya XML/strings)
 *   [LATIHAN 4]           -> skor
 *   [LATIHAN 5]           -> overlay kesulitan (lapisan ke-4)
 *   [LATIHAN 6]           -> simpan status saat layar diputar
 *
 *  Blok ditandai:  // >>> [NAMA] ... // <<< [NAMA]   (hapus semuanya)
 *  Baris tunggal:  ...kode...  // [NAMA]              (hapus barisnya)
 *
 *  Catatan: minimal SALAH SATU dari [OPSI: INPUT KETIK] atau
 *  [LATIHAN 1] harus dipertahankan, kalau tidak pemain tidak punya
 *  cara menebak.
 * =====================================================================
 */
class MainActivity : AppCompatActivity() {

    // ==== data permainan ====
    private lateinit var kataRahasia: String
    private val hurufBenar = mutableSetOf<Char>()
    private val hurufSalah = mutableSetOf<Char>()
    private var maksNyawa = 6   // var (bukan val) agar bisa diubah oleh [LATIHAN 5]

    // >>> [LATIHAN 4] data skor
    private var skor = 0
    private lateinit var tvSkor: TextView
    // <<< [LATIHAN 4]

    // >>> [LATIHAN 6] penanda status permainan, dipakai untuk memulihkan layar
    private var permainanSelesai = false
    private var menangTerakhir = false
    // <<< [LATIHAN 6]

    // ==== view ====
    private lateinit var tvNyawa: TextView
    private lateinit var tvKata: TextView
    private lateinit var tvRiwayat: TextView
    private lateinit var overlayHasil: LinearLayout
    private lateinit var overlayMulai: LinearLayout
    private lateinit var tvHasil: TextView
    private lateinit var tvKataAkhir: TextView

    private lateinit var etTebak: EditText                 // [OPSI: INPUT KETIK]
    private lateinit var gridHuruf: GridLayout             // [LATIHAN 1]
    private lateinit var overlayKesulitan: LinearLayout    // [LATIHAN 5]

    // ==== gambar gantungan: 6 bagian tubuh, urut sesuai kesalahan ====
    private lateinit var bagianTubuh: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvNyawa = findViewById(R.id.tvNyawa)
        tvKata = findViewById(R.id.tvKata)
        tvRiwayat = findViewById(R.id.tvRiwayat)
        overlayHasil = findViewById(R.id.overlayHasil)
        overlayMulai = findViewById(R.id.overlayMulai)
        tvHasil = findViewById(R.id.tvHasil)
        tvKataAkhir = findViewById(R.id.tvKataAkhir)

        etTebak = findViewById(R.id.etTebak)                       // [OPSI: INPUT KETIK]
        gridHuruf = findViewById(R.id.gridHuruf)                   // [LATIHAN 1]
        tvSkor = findViewById(R.id.tvSkor)                         // [LATIHAN 4]
        overlayKesulitan = findViewById(R.id.overlayKesulitan)     // [LATIHAN 5]

        // urutan list = urutan kemunculan bagian tubuh
        bagianTubuh = listOf(
            findViewById(R.id.imgKepala),        // kesalahan ke-1
            findViewById(R.id.imgBadan),         // kesalahan ke-2
            findViewById(R.id.imgTanganKiri),    // kesalahan ke-3
            findViewById(R.id.imgTanganKanan),   // kesalahan ke-4
            findViewById(R.id.imgKakiKiri),      // kesalahan ke-5
            findViewById(R.id.imgKakiKanan)      // kesalahan ke-6
        )

        // >>> [OPSI: INPUT KETIK] tombol Tebak
        findViewById<Button>(R.id.btnTebak).setOnClickListener { prosesTebakan() }
        // <<< [OPSI: INPUT KETIK]

        findViewById<Button>(R.id.btnMainLagi).setOnClickListener { mulaiPermainan() }

        findViewById<Button>(R.id.btnMulai).setOnClickListener {
            overlayMulai.visibility = View.GONE
            overlayKesulitan.visibility = View.VISIBLE   // [LATIHAN 5]
            mulaiPermainan()
        }

        // >>> [LATIHAN 5] tombol pilihan kesulitan
        findViewById<Button>(R.id.btnMudah).setOnClickListener { pilihKesulitan(8) }
        findViewById<Button>(R.id.btnSedang).setOnClickListener { pilihKesulitan(6) }
        findViewById<Button>(R.id.btnSulit).setOnClickListener { pilihKesulitan(4) }
        // <<< [LATIHAN 5]

        buatTombolHuruf()   // [LATIHAN 1] (harus sebelum mulaiPermainan)

        mulaiPermainan()
        overlayMulai.visibility = View.VISIBLE

        // >>> [LATIHAN 6] pulihkan status bila layar baru saja diputar
        if (savedInstanceState != null) pulihkanKeadaan(savedInstanceState)
        // <<< [LATIHAN 6]
    }

    // =====================================================================
    //  INTI PERMAINAN
    // =====================================================================

    /**
     * Menampilkan gambar bagian tubuh sesuai jumlah kesalahan.
     * Jumlah bagian dihitung proporsional terhadap maksNyawa, sehingga
     * pada nyawa terakhir seluruh 6 gambar selalu lengkap
     * (untuk nyawa 6 hasilnya sama persis dengan modul: 1 salah = 1 bagian).
     */
    private fun perbaruiGantungan(kesalahan: Int) {
        val jumlahTampil = (kesalahan * bagianTubuh.size + maksNyawa - 1) / maksNyawa

        bagianTubuh.forEachIndexed { urutan, gambar ->
            if (urutan < jumlahTampil) {
                val baruMuncul = gambar.visibility != View.VISIBLE
                gambar.visibility = View.VISIBLE

                // >>> [LATIHAN 2] animasi fade untuk gambar yang baru muncul
                if (baruMuncul) {
                    gambar.alpha = 0f
                    gambar.animate().alpha(1f).setDuration(400).start()
                }
                // <<< [LATIHAN 2]
            } else {
                gambar.visibility = View.INVISIBLE
            }
        }
    }

    private fun mulaiPermainan() {
        // ambil kata acak dari string-array
        val daftarKata = resources.getStringArray(R.array.daftar_kata)
        kataRahasia = daftarKata.random()

        // bersihkan semua tebakan sebelumnya
        hurufBenar.clear()
        hurufSalah.clear()
        permainanSelesai = false      // [LATIHAN 6]

        // sembunyikan overlay hasil & siapkan input
        overlayHasil.visibility = View.GONE

        // >>> [OPSI: INPUT KETIK]
        etTebak.text.clear()
        etTebak.isEnabled = true
        // <<< [OPSI: INPUT KETIK]

        gambarPapan()
    }

    private fun gambarPapan() {
        // 1) gambar gantungan: makin banyak kesalahan, makin lengkap gambarnya
        perbaruiGantungan(hurufSalah.size)

        // 2) kata rahasia: huruf yang sudah benar dibuka, sisanya garis bawah
        tvKata.text = kataRahasia
            .map { if (hurufBenar.contains(it)) it else '_' }
            .joinToString(" ")

        // 3) sisa nyawa
        val sisaNyawa = maksNyawa - hurufSalah.size
        tvNyawa.text = "Nyawa: $sisaNyawa"

        // 4) daftar huruf yang salah
        tvRiwayat.text = if (hurufSalah.isEmpty()) {
            "Salah: -"
        } else {
            "Salah: ${hurufSalah.joinToString(" ")}"
        }

        perbaruiTombolHuruf()             // [LATIHAN 1]
        tvSkor.text = "Skor: $skor"       // [LATIHAN 4]
    }

    /**
     * Inti penebakan, dipakai bersama oleh input ketik dan tombol A-Z.
     * Jangan dihapus.
     */
    private fun tebakHuruf(huruf: Char) {
        // huruf yang sama tidak dihitung dua kali
        if (hurufBenar.contains(huruf) || hurufSalah.contains(huruf)) {
            Toast.makeText(this, "Huruf $huruf sudah ditebak", Toast.LENGTH_SHORT).show()
            return
        }

        // benar -> buka hurufnya; salah -> tambah kesalahan
        if (kataRahasia.contains(huruf)) {
            hurufBenar.add(huruf)
        } else {
            hurufSalah.add(huruf)
        }

        gambarPapan()
        periksaAkhirPermainan()
    }

    private fun periksaAkhirPermainan() {
        // Cek menang lebih dulu daripada kalah (huruf terakhir di nyawa terakhir = menang)
        val menang = kataRahasia.all { hurufBenar.contains(it) }
        val kalah = !menang && hurufSalah.size >= maksNyawa
        if (!menang && !kalah) return

        // >>> [LATIHAN 4] menang +10 poin, kalah +0
        if (menang) skor += 10
        tvSkor.text = "Skor: $skor"
        // <<< [LATIHAN 4]

        // >>> [LATIHAN 6]
        permainanSelesai = true
        menangTerakhir = menang
        // <<< [LATIHAN 6]

        tampilkanHasil(menang)
    }

    private fun tampilkanHasil(menang: Boolean) {
        // 1) isi tulisan pada overlay
        tvHasil.text = if (menang) getString(R.string.menang) else getString(R.string.kalah)
        tvHasil.setTextColor(
            if (menang) 0xFF3DDC84.toInt() else 0xFFF87171.toInt()
        )
        tvKataAkhir.text = "Kata: $kataRahasia"

        // 2) pemain tidak boleh menebak lagi
        etTebak.isEnabled = false         // [OPSI: INPUT KETIK]

        // 3) tampilkan layer overlay dengan animasi memudar
        overlayHasil.alpha = 0f
        overlayHasil.visibility = View.VISIBLE
        overlayHasil.animate()
            .alpha(1f)
            .setDuration(350)
            .start()

        // 4) jadikan overlay sebagai penerima fokus berikutnya
        overlayHasil.requestFocus()
    }

    // =====================================================================
    //  >>> [OPSI: INPUT KETIK]
    // =====================================================================
    private fun prosesTebakan() {
        val input = etTebak.text.toString().trim().uppercase()

        // harus tepat satu huruf
        if (input.length != 1 || !input[0].isLetter()) {
            Toast.makeText(this, "Masukkan satu huruf", Toast.LENGTH_SHORT).show()
            etTebak.text.clear()
            return
        }

        tebakHuruf(input[0])
        etTebak.text.clear()
    }
    // <<< [OPSI: INPUT KETIK]

    // =====================================================================
    //  >>> [LATIHAN 1] TOMBOL HURUF A-Z
    // =====================================================================
    /** Membuat 26 tombol A-Z di dalam gridHuruf (7 kolom, lebar dibagi rata). */
    private fun buatTombolHuruf() {
        val kepadatan = resources.displayMetrics.density
        val tinggi = (40 * kepadatan).toInt()
        val jarak = (2 * kepadatan).toInt()

        for (huruf in 'A'..'Z') {
            val tombol = MaterialButton(this).apply {
                text = huruf.toString()
                textSize = 14f
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                insetTop = 0
                insetBottom = 0
                setPadding(0, 0, 0, 0)
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    height = tinggi
                    setMargins(jarak, jarak, jarak, jarak)
                }
                setOnClickListener { tebakHuruf(huruf) }
            }
            gridHuruf.addView(tombol)
        }
    }

    /** Huruf yang sudah ditebak (benar/salah) dinonaktifkan: isEnabled = false. */
    private fun perbaruiTombolHuruf() {
        for (i in 0 until gridHuruf.childCount) {
            val huruf = 'A' + i
            gridHuruf.getChildAt(i).isEnabled =
                !hurufBenar.contains(huruf) && !hurufSalah.contains(huruf)
        }
    }
    // <<< [LATIHAN 1]

    // =====================================================================
    //  >>> [LATIHAN 5] KESULITAN (menentukan jumlah nyawa)
    // =====================================================================
    private fun pilihKesulitan(nyawa: Int) {
        maksNyawa = nyawa
        overlayKesulitan.visibility = View.GONE
        mulaiPermainan()
    }
    // <<< [LATIHAN 5]

    // =====================================================================
    //  >>> [LATIHAN 6] SIMPAN & PULIHKAN STATUS SAAT LAYAR DIPUTAR
    // =====================================================================
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("kata", kataRahasia)
        outState.putString("benar", hurufBenar.joinToString(""))
        outState.putString("salah", hurufSalah.joinToString(""))
        outState.putInt("maksNyawa", maksNyawa)
        outState.putBoolean("selesai", permainanSelesai)
        outState.putBoolean("menang", menangTerakhir)
        outState.putBoolean("mulaiTampil", overlayMulai.visibility == View.VISIBLE)
        outState.putInt("skor", skor)                                                         // [LATIHAN 4]
        outState.putBoolean("kesulitanTampil", overlayKesulitan.visibility == View.VISIBLE)  // [LATIHAN 5]
    }

    private fun pulihkanKeadaan(state: Bundle) {
        kataRahasia = state.getString("kata") ?: kataRahasia
        hurufBenar.clear()
        hurufBenar.addAll((state.getString("benar") ?: "").toList())
        hurufSalah.clear()
        hurufSalah.addAll((state.getString("salah") ?: "").toList())
        maksNyawa = state.getInt("maksNyawa", 6)
        permainanSelesai = state.getBoolean("selesai")
        menangTerakhir = state.getBoolean("menang")
        skor = state.getInt("skor")                                                            // [LATIHAN 4]

        gambarPapan()

        overlayMulai.visibility =
            if (state.getBoolean("mulaiTampil")) View.VISIBLE else View.GONE
        overlayKesulitan.visibility =                                                          // [LATIHAN 5]
            if (state.getBoolean("kesulitanTampil")) View.VISIBLE else View.GONE               // [LATIHAN 5]

        if (permainanSelesai) tampilkanHasil(menangTerakhir)
    }
    // <<< [LATIHAN 6]
}
