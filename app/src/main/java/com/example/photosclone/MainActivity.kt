package com.example.photosclone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.ui.test.isSelected
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private lateinit var btnUpload: Button
    private lateinit var btnDeleteMulti: Button

    // Biến lưu trữ thư mục gốc của USB và danh sách ảnh hiện tại trên RAM
    private var usbRootDirectory: DocumentFile? = null
    private var currentImageList = mutableListOf<MediaItem>()

    // 1. Trình lắng nghe chọn thư mục USB
    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // QUAN TRỌNG: Xin thêm quyền GHI (FLAG_GRANT_WRITE_URI_PERMISSION)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            // Lưu lại thư mục gốc để lát nữa upload file vào đây
            usbRootDirectory = DocumentFile.fromTreeUri(this, uri)

            // Hiện nút Tải lên và bắt đầu quét ảnh
            btnUpload.visibility = View.VISIBLE
            scanUsbForImages(uri)
        }
    }

    // 2. Trình lắng nghe chọn ảnh từ điện thoại để upload
    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // uris lúc này là một danh sách các đường dẫn ảnh
        if (uris.isNotEmpty() && usbRootDirectory != null) {
            uploadMultipleImagesToUsb(uris, usbRootDirectory!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpload = findViewById(R.id.btn_upload)
        setupRecyclerView()

        findViewById<Button>(R.id.btn_select_usb).setOnClickListener {
            openDocumentTreeLauncher.launch(null)
        }

        btnUpload.setOnClickListener {
            // Khởi chạy trình chọn nhiều ảnh
            pickMultipleImagesLauncher.launch("image/*")
        }

        btnDeleteMulti = findViewById(R.id.btn_delete_multi)

        btnDeleteMulti.setOnClickListener {
            deleteSelectedImages()
        }
    }

    // 1. Khai báo launcher để đợi kết quả từ màn hình FullScreen
    private val fullScreenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val deletedUriString = result.data?.getStringExtra("DELETED_URI")
            if (deletedUriString != null) {
                val deletedUri = Uri.parse(deletedUriString)

                // Xóa ảnh khỏi danh sách đang lưu trên RAM
                currentImageList.removeAll { it.uri == deletedUri }

                // Vẽ lại giao diện ngay lập tức
                refreshUI()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)

        adapter = GalleryAdapter(
            // 1. Phục hồi lại code mở ảnh Full Screen ở đây
            onImageClick = { uri ->
                val intent = Intent(this, FullScreenActivity::class.java).apply {
                    putExtra("IMAGE_URI", uri.toString())
                }
                // Gọi launcher để lỡ người dùng xóa ảnh trong màn hình FullScreen thì bên ngoài còn biết mà cập nhật
                fullScreenLauncher.launch(intent)
            },
            // 2. Logic ẩn/hiện nút khi chọn nhiều ảnh
            onSelectionChange = { isSelected ->
                btnDeleteMulti.visibility = if (isSelected) View.VISIBLE else View.GONE
                btnUpload.visibility = if (isSelected) View.GONE else View.VISIBLE
            }
        )

        // Setup Grid
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
        CoroutineScope(Dispatchers.IO).launch {
            val rootDir = DocumentFile.fromTreeUri(this@MainActivity, rootUri)
            val tempList = mutableListOf<MediaItem>()

            fun traverse(dir: DocumentFile?) {
                dir?.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        traverse(file)
                    } else if (file.type?.startsWith("image/") == true) {
                        tempList.add(MediaItem(file.uri, file.lastModified()))
                    }
                }
            }
            traverse(rootDir)

            // Cập nhật danh sách trên RAM
            currentImageList = tempList

            withContext(Dispatchers.Main) {
                refreshUI()
            }
        }
    }

    // 3. Hàm cốt lõi: Copy luồng byte từ Điện thoại sang USB
    private fun uploadMultipleImagesToUsb(sourceUris: List<Uri>, destinationDir: DocumentFile) {
        // Báo cho người dùng biết số lượng ảnh đang được xử lý
        Toast.makeText(this, "Đang tải lên ${sourceUris.size} ảnh...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            val newItems = mutableListOf<MediaItem>() // Chứa danh sách ảnh vừa tải thành công

            // Lặp qua từng ảnh mà người dùng đã chọn
            for (sourceUri in sourceUris) {
                try {
                    // Thêm UUID để tên file không bao giờ bị trùng lặp
                    val randomText = java.util.UUID.randomUUID().toString().take(5)
                    val fileName = "IMG_${System.currentTimeMillis()}_$randomText.jpg"

                    val newUsbFile = destinationDir.createFile("image/jpeg", fileName)

                    if (newUsbFile != null) {
                        // Copy từng file một
                        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            contentResolver.openOutputStream(newUsbFile.uri)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        // Copy xong thì gom vào danh sách tạm
                        newItems.add(MediaItem(newUsbFile.uri, newUsbFile.lastModified()))
                        successCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Sau khi vòng lặp kết thúc, cập nhật toàn bộ vào giao diện 1 lần duy nhất
            if (newItems.isNotEmpty()) {
                currentImageList.addAll(newItems)

                withContext(Dispatchers.Main) {
                    refreshUI() // Vẽ lại lưới ảnh
                    Toast.makeText(this@MainActivity, "Đã tải xong $successCount/${sourceUris.size} ảnh!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Tách riêng hàm Refresh UI để dùng chung cho lúc Scan và lúc Upload
    private fun refreshUI() {
        val groupedImages = groupImagesByDate(currentImageList)
        val flatList = mutableListOf<ListItem>()

        groupedImages.forEach { (date, images) ->
            flatList.add(ListItem.Header(date))
            images.forEach { flatList.add(ListItem.Image(it.uri, it.dateModified)) }
        }

        adapter.submitList(flatList)
    }

    private fun groupImagesByDate(images: List<MediaItem>): Map<String, List<MediaItem>> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return images
            .sortedByDescending { it.dateModified }
            .groupBy { dateFormat.format(Date(it.dateModified)) }
    }

    private fun deleteSelectedImages() {
        // Lấy danh sách các URI đang được chọn từ Adapter thay vì từ currentImageList
        val selectedUris = adapter.getSelectedUris()
        if (selectedUris.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            var count = 0

            // Duyệt qua danh sách các Uri cần xóa
            selectedUris.forEach { uri ->
                val file = DocumentFile.fromSingleUri(this@MainActivity, uri)
                if (file?.delete() == true) {
                    // Xóa thành công trên USB thì mới gỡ khỏi danh sách gốc trên RAM
                    currentImageList.removeAll { it.uri == uri }
                    count++
                }
            }

            withContext(Dispatchers.Main) {
                adapter.isSelectionMode = false // Thoát chế độ chọn UI
                refreshUI() // Vẽ lại lưới ảnh mới
                Toast.makeText(this@MainActivity, "Đã xóa $count ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }
}