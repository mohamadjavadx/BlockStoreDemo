package com.example.blockstoredemo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse
import com.google.android.gms.auth.blockstore.StoreBytesData


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "blockstoredemo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etToken = findViewById<EditText>(R.id.etToken)
        val tvToken = findViewById<TextView>(R.id.tvToken)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnRetrieve = findViewById<Button>(R.id.btnRetrieve)
        val btnDelete = findViewById<Button>(R.id.btnDelete)


        val blockStoreClient = Blockstore.getClient(this)

        fun saveToken() {
            val token: String = etToken.text?.toString().orEmpty()
            val tokenBytes = token.toByteArray()

            val chunks = tokenBytes.size % 16
            Log.d(TAG, "tokenBytes: $tokenBytes and total chunks: $chunks")

            val tokenChunks = tokenBytes.chunked(16)
            tokenChunks.forEachIndexed { chunk, bytes ->
                val storeRequest = StoreBytesData.Builder()
                    .setKey("com.example.blockstoredemo.token.chunk$chunk")
                    .setBytes(bytes)

                blockStoreClient.isEndToEndEncryptionAvailable.addOnSuccessListener { isE2EEAvailable ->
                    if (isE2EEAvailable) {
                        storeRequest.setShouldBackupToCloud(true)
                    }
                }

                blockStoreClient.storeBytes(storeRequest.build())
                    .addOnSuccessListener { _: Int ->
                        Log.d(TAG, "Stored chunk: $bytes")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to store chunk: $bytes", e)
                    }
            }
        }

        fun retrieveToken() {

            val retrieveRequest = RetrieveBytesRequest.Builder()
                .setRetrieveAll(true)
                .build()

            blockStoreClient.retrieveBytes(retrieveRequest)
                .addOnSuccessListener { result: RetrieveBytesResponse ->
                    runCatching {
                        var retrievedToken = ""
                        result.blockstoreDataMap.entries.sortedBy { entry ->
                            runCatching {
                                entry.key.substringAfterLast("chunk").toInt()
                            }.getOrDefault(0)

                        }.forEach { entry ->
                            retrievedToken += String(entry.value.bytes)
                            Log.d(
                                TAG,
                                "Retrieved bytes ${String(entry.value.bytes)} associated with key ${entry.key}"
                            )
                        }
                        tvToken.text = "retrieved token: $retrievedToken"
                    }
                }
                .addOnFailureListener { e: Exception? ->
                    Log.e(TAG, "Failed to store bytes", e)
                }

        }

        btnSave.setOnClickListener {
            saveToken()
        }

        btnRetrieve.setOnClickListener {
            retrieveToken()
        }

        btnDelete.setOnClickListener {
            val deleteAllRequest = DeleteBytesRequest.Builder()
                .setDeleteAll(true)
                .build()
            blockStoreClient.deleteBytes(deleteAllRequest).addOnSuccessListener { result: Boolean ->
                Log.d(TAG, "Any data found and deleted? $result")
            }
        }


    }
}

fun ByteArray.chunked(chunkSize: Int): List<ByteArray> {
    val chunks = mutableListOf<ByteArray>()
    var currentIndex = 0

    while (currentIndex < size) {
        val remainingSize = size - currentIndex
        val currentChunkSize = if (chunkSize <= remainingSize) chunkSize else remainingSize
        val chunk = copyOfRange(currentIndex, currentIndex + currentChunkSize)
        chunks.add(chunk)
        currentIndex += currentChunkSize
    }

    return chunks
}