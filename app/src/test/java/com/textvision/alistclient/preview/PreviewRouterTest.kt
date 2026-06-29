package com.textvision.alistclient.preview

import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewRouterTest {
    private fun item(
        type: FileType,
        url: String? = "https://example.test/file",
        size: Long = 1024L,
        name: String = "file.dat",
    ) = FileItem(
        name = name,
        path = "/$name",
        isDir = false,
        size = size,
        modifiedAt = null,
        extension = name.substringAfterLast('.', ""),
        type = type,
        thumbnailUrl = null,
        downloadUrl = url,
    )

    @Test fun routesImageTextAudioAndExternalTypes() {
        assertEquals(PreviewMode.Image("https://example.test/file"), PreviewRouter.route(item(FileType.Image, name = "photo.png")))
        assertEquals(PreviewMode.Text("https://example.test/file", 1024L), PreviewRouter.route(item(FileType.Text, name = "notes.md")))
        assertEquals(PreviewMode.Audio("https://example.test/file"), PreviewRouter.route(item(FileType.Audio, name = "song.mp3")))
        assertEquals(PreviewMode.External("https://example.test/file"), PreviewRouter.route(item(FileType.Pdf, name = "doc.pdf")))
        assertEquals(PreviewMode.External("https://example.test/file"), PreviewRouter.route(item(FileType.Video, name = "movie.mp4")))
    }

    @Test fun routesMissingUrlAndOversizedTextToSafeStates() {
        assertEquals(PreviewMode.Unavailable, PreviewRouter.route(item(FileType.Image, url = null, name = "photo.png")))
        assertEquals(PreviewMode.TextTooLarge(PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES + 1), PreviewRouter.route(
            item(FileType.Text, size = PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES + 1, name = "large.log")
        ))
    }
}
