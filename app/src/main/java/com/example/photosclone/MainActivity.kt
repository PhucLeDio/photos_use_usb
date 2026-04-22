package com.example.photosclone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private lateinit var btnUpload: Button
    private lateinit var btnDeleteMulti: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var tvLoadingProgress: TextView

    private var usbRootDirectory: DocumentFile? = null

    // 2 Danh sách riêng biệt cho 2 Tab
    private var standardImageList = mutableListOf<MediaItem>()
    private var otherImageList = mutableListOf<MediaItem>()
    private var currentTabIndex = 0 // 0 = Chuẩn, 1 = Khác

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
            usbRootDirectory = DocumentFile.fromTreeUri(this, uri)
            btnUpload.visibility = View.VISIBLE
            scanUsbForImages(uri)
        }
    }

    private val pickMultipleImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty() && usbRootDirectory != null) uploadMultipleImagesToUsb(uris, usbRootDirectory!!)
    }

    private val fullScreenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val deletedUri = result.data?.getStringExtra("DELETED_URI")?.let { Uri.parse(it) }
            if (deletedUri != null) {
                standardImageList.removeAll { it.uri == deletedUri }
                otherImageList.removeAll { it.uri == deletedUri }
                refreshUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpload = findViewById(R.id.btn_upload)
        btnDeleteMulti = findViewById(R.id.btn_delete_multi)
        tabLayout = findViewById(R.id.tab_layout)
        layoutLoading = findViewById(R.id.layout_loading)
        tvLoadingProgress = findViewById(R.id.tv_loading_progress)

        setupRecyclerView()
        setupTabs()

        findViewById<Button>(R.id.btn_select_usb).setOnClickListener { openDocumentTreeLauncher.launch(null) }
        btnUpload.setOnClickListener { pickMultipleImagesLauncher.launch("image/*") }
        btnDeleteMulti.setOnClickListener { deleteSelectedImages() }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabIndex = tab?.position ?: 0
                adapter.isSelectionMode = false // Hủy chọn khi chuyển tab
                refreshUI()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        adapter = GalleryAdapter(
            onImageClick = { uri ->
                val intent = Intent(this, FullScreenActivity::class.java).apply { putExtra("IMAGE_URI", uri.toString()) }
                fullScreenLauncher.launch(intent)
            },
            onSelectionChange = { isSelected ->
                btnDeleteMulti.visibility = if (isSelected) View.VISIBLE else View.GONE
                btnUpload.visibility = if (isSelected) View.GONE else View.VISIBLE
            }
        )

        val layoutManager = GridLayoutManager(this, 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == GalleryAdapter.TYPE_HEADER) 4 else 1
            }
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun scanUsbForImages(rootUri: Uri) {
        // layoutLoading.visibility = View.VISIBLE
        // tvLoadingProgress.text = "Prepare scanning..."
        standardImageList.clear()
        otherImageList.clear()
        adapter.submitList(emptyList())

        CoroutineScope(Dispatchers.IO).launch {
            val rootDir = DocumentFile.fromTreeUri(this@MainActivity, rootUri)
            var scannedCount = 0
            val chunkStandard = mutableListOf<MediaItem>()
            val chunkOther = mutableListOf<MediaItem>()

            // Hàm Regex siêu chặt để kiểm tra tên file
            val strictPattern = Regex("""(?:^|[^0-9])(19[9]\d|20[0-3]\d)[-.]?(0[1-9]|1[0-2])[-.]?(0[1-9]|[12]\d|3[01])[_-](\d{6})""")

            suspend fun traverse(dir: DocumentFile?) {
                if (!isActive) return
                dir?.listFiles()?.forEach { file ->
                    if (file.isDirectory) traverse(file)
                    else if (file.type?.startsWith("image/") == true) {
                        scannedCount++
                        val match = strictPattern.find(file.name ?: "")

                        if (match != null) {
                            // Cú pháp đúng -> Phân tích ngày và cho vào Tab 1
                            try {
                                val year = match.groupValues[1].toInt()
                                val month = match.groupValues[2].toInt()
                                val day = match.groupValues[3].toInt()
                                val cal = Calendar.getInstance().apply { set(year, month - 1, day) }
                                chunkStandard.add(MediaItem(file.uri, cal.timeInMillis))
                            } catch (e: Exception) {
                                chunkOther.add(MediaItem(file.uri, file.lastModified()))
                            }
                        } else {
                            // Sai cú pháp -> Cho ngay vào Tab 2
                            chunkOther.add(MediaItem(file.uri, file.lastModified()))
                        }

                        // Incremental update (Cứ 50 ảnh cập nhật 1 lần)
                        if (chunkStandard.size + chunkOther.size >= 50) {
                            standardImageList.addAll(chunkStandard)
                            otherImageList.addAll(chunkOther)
                            chunkStandard.clear()
                            chunkOther.clear()
                            withContext(Dispatchers.Main) {
                                tvLoadingProgress.text = "Fought $scannedCount image..."
                                refreshUI()
                            }
                        }
                    }
                }
            }
            traverse(rootDir)

            standardImageList.addAll(chunkStandard)
            otherImageList.addAll(chunkOther)

            withContext(Dispatchers.Main) {
                refreshUI()
                layoutLoading.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Done! About: $scannedCount image", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadMultipleImagesToUsb(sourceUris: List<Uri>, destinationDir: DocumentFile) {
        Toast.makeText(this, "Uploading ${sourceUris.size} images...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            sourceUris.forEachIndexed { index, sourceUri ->
                try {
                    // Cố tình cộng thêm mili-giây để tên file tăng dần, kết hợp UUID để chống trùng
                    val timeForName = System.currentTimeMillis() + (index * 1000)
                    val timestampStr = sdf.format(Date(timeForName))
                    val randomId = UUID.randomUUID().toString().take(4)

                    // Tên chuẩn mực: IMG_20260422_235501_a1b2.jpg
                    val newFileName = "IMG_${timestampStr}_${randomId}.jpg"
                    val newUsbFile = destinationDir.createFile("image/jpeg", newFileName)

                    if (newUsbFile != null) {
                        contentResolver.openInputStream(sourceUri)?.use { input ->
                            contentResolver.openOutputStream(newUsbFile.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Upload xong tự động ném vào Tab 1 (Standard)
                        standardImageList.add(MediaItem(newUsbFile.uri, timeForName))
                        successCount++
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            withContext(Dispatchers.Main) {
                refreshUI()
                Toast.makeText(this@MainActivity, "Uploaded $successCount images!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteSelectedImages() {
        val selectedUris = adapter.getSelectedUris()
        if (selectedUris.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            var count = 0
            selectedUris.forEach { uri ->
                val file = DocumentFile.fromSingleUri(this@MainActivity, uri)
                if (file?.delete() == true) {
                    if (currentTabIndex == 0) standardImageList.removeAll { it.uri == uri }
                    else otherImageList.removeAll { it.uri == uri }
                    count++
                }
            }
            withContext(Dispatchers.Main) {
                adapter.isSelectionMode = false
                refreshUI()
                Toast.makeText(this@MainActivity, "Deleted $count images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshUI() {
        val flatList = mutableListOf<ListItem>()
        if (currentTabIndex == 0) {
            // TAB 1: Có hiển thị Header ngày tháng
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val grouped = standardImageList.sortedByDescending { it.dateModified }
                .groupBy { dateFormat.format(Date(it.dateModified)) }

            grouped.forEach { (date, images) ->
                flatList.add(ListItem.Header(date))
                images.forEach { flatList.add(ListItem.Image(it.uri, it.dateModified)) }
            }
        } else {
            // TAB 2: Không hiển thị Header, chỉ dải ảnh phẳng lì
            otherImageList.sortedByDescending { it.dateModified }.forEach {
                flatList.add(ListItem.Image(it.uri, it.dateModified))
            }
        }
        adapter.submitList(flatList)
    }
}