package com.example.photosclone

import android.net.Uri

// Lớp chứa thông tin file thô quét được
data class MediaItem(val uri: Uri, val dateModified: Long)

sealed class ListItem {
    data class Header(val date: String) : ListItem()
    data class Image(
        val uri: Uri,
        val dateModified: Long,
        var isSelected: Boolean = false
    ) : ListItem()
}