package com.soccertips.predictx.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.soccertips.predictx.data.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirebaseRepository @Inject constructor() {
    private val database = FirebaseDatabase.getInstance()
    private val categoriesRef = database.getReference("categories")

    fun getCategories(): Flow<Result<List<Category>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val categories = mutableListOf<Category>()
                    for (categorySnapshot in snapshot.children) {
                        val url = categorySnapshot.child("url").getValue(String::class.java) ?: ""
                        val name = categorySnapshot.child("name").getValue(String::class.java) ?: ""

                        categories.add(Category(url, name))
                    }
                    trySend(Result.success(categories))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing categories from Firebase")
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Firebase Database error: ${error.message}")
                trySend(Result.failure(error.toException()))
            }
        }

        categoriesRef.addValueEventListener(listener)
        awaitClose { categoriesRef.removeEventListener(listener) }
    }
}