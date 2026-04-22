package com.example.photosclone

import android.net.Uri

// Lớp chứa thông tin file thô quét được
data class MediaItem(val uri: Uri, val dateModified: Long)

// Lớp đại diện cho các thành phần hiển thị trên UI
sealed class ListItem {
    data class Header(val date: String) : ListItem()
    // Thêm isSelected vào đây
    data class Image(
        val uri: Uri,
        val dateModified: Long,
        var isSelected: Boolean = false
    ) : ListItem()
}