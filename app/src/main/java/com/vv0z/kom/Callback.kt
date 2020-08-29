package com.vv0z.kom

import java.io.File

interface Callback {
    fun getResult(imagePath: String, imageFile: File)
}