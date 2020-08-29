package com.vv0z.kom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class GetPhoto(private val owner: Any) {

    companion object {
        private const val REQUEST_CAMERA_CAPTURE = 1
        private const val REQUEST_GALLERY_CAPTURE = 2
        private var FILE_PROVIDER_AUTHORITY = ""
        private lateinit var imageFile: File
        private lateinit var imagePath: String
    }

    private lateinit var callback: Callback

    fun getProviderAuthority(authority: String) {
        FILE_PROVIDER_AUTHORITY = authority
    }

    fun byCamera():GetPhoto {
        if (owner is Activity) {
            if (owner.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    owner.requestPermissions(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        REQUEST_CAMERA_CAPTURE
                    )
                } else {
                    dispatchTakePictureIntent()
                }
            } else {
                throw Exception("Your device doesn't have any camera!")
            }
        } else if (owner is Fragment) {
            if (owner.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    owner.requestPermissions(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        REQUEST_CAMERA_CAPTURE
                    )
                } else {
                    dispatchTakePictureIntent()
                }
            } else {
                throw Exception("Your device doesn't have any camera!")
            }
        }
        return this
    }

    fun fromGallery(): GetPhoto {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (owner is Activity) {
                owner.requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_GALLERY_CAPTURE
                )
            } else if (owner is Fragment) {
                owner.requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_GALLERY_CAPTURE
                )
            }
        } else {
            openGallery()
        }
        return this
    }

    private fun dispatchTakePictureIntent() {
        if (owner is Activity) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(owner.packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        throw Exception("Create file failed-> ${ex.message}")
                    }
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            owner, FILE_PROVIDER_AUTHORITY, it
                        )
                        imageFile = photoFile
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        owner.startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
                    }
                }
            }
        } else if (owner is Fragment) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(owner.requireContext().packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        throw Exception("Create file failed-> ${ex.message}")
                    }
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            owner.requireContext(), FILE_PROVIDER_AUTHORITY, it
                        )
                        imageFile = photoFile
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        owner.startActivityForResult(takePictureIntent, REQUEST_CAMERA_CAPTURE)
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var storageDir: File? = null
        if (owner is Activity) {
            storageDir = owner.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        } else if (owner is Fragment) {
            storageDir = owner.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            imagePath = absolutePath
        }
    }

    private fun openGallery() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (owner is Activity)
            owner.startActivityForResult(pickPhoto, REQUEST_GALLERY_CAPTURE)
        else if (owner is Fragment)
            owner.startActivityForResult(pickPhoto, REQUEST_GALLERY_CAPTURE)
    }

    private fun getPhotoFromGallery(data: Intent?) {
        try {
            val imageUri: Uri = data?.data!!
            var imageStream: InputStream? = null
            if (owner is Activity)
                imageStream = owner.contentResolver.openInputStream(imageUri)
            else if (owner is Fragment)
                imageStream = owner.requireContext().contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            imagePath = getPathFromURI(imageUri)!!
            imageFile = File(imagePath)
            callback.getResult(imagePath, imageFile)
        } catch (e: FileNotFoundException) {
            throw Exception("getPhotoFromGallery_error-> ${e.message}")
        }
    }

    @SuppressLint("Recycle")
    private fun getPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        if (owner is Activity) {
            cursor = owner.contentResolver.query(
                uri, projection, null, null, null
            )
        } else if (owner is Fragment) {
            cursor = owner.requireContext().contentResolver.query(
                uri, projection, null, null, null
            )
        }
        var res: String? = null
        if (cursor?.moveToFirst()!!) {
            res = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        cursor.close()
        return res
    }

    fun getPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_CAPTURE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            }
        } else if (requestCode == REQUEST_GALLERY_CAPTURE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            }
        }
    }

    fun getIntentResults(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CAMERA_CAPTURE && resultCode == Activity.RESULT_OK) {
            callback.getResult(imagePath, imageFile)
        } else if (requestCode == REQUEST_GALLERY_CAPTURE && resultCode == Activity.RESULT_OK) {
            getPhotoFromGallery(data)
        }
    }

    fun getResults(callback: Callback) {
        this.callback = callback
    }

}