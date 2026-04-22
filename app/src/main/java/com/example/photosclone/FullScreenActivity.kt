package com.example.photosclone

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class FullScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val uriString = intent.getStringExtra("IMAGE_URI")
        val photoView = findViewById<PhotoView>(R.id.img_full)
        val btnDelete = findViewById<ImageButton>(R.id.btn_delete)

        if (uriString != null) {
            val imageUri = Uri.parse(uriString)
            photoView.load(imageUri)

            btnDelete.setOnClickListener {
                deleteImage(imageUri)
            }
        }
    }

    private fun deleteImage(uri: Uri) {
        // Tạo DocumentFile từ URI
        val file = DocumentFile.fromSingleUri(this, uri)

        if (file != null && file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show()

                // Gửi kết quả về MainActivity để cập nhật UI
                val resultIntent = Intent().apply {
                    putExtra("DELETED_URI", uri.toString())
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Đóng màn hình xem ảnh
            } else {
                Toast.makeText(this, "Không thể xóa file!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}