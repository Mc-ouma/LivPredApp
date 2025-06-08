package com.soccertips.predictx.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.soccertips.predictx.R
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
    private val apiConfig = database.getReference("api-config") // Updated to use hyphen instead of underscore

    fun getCategories(): Flow<Result<List<Category>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val categories = mutableListOf<Category>()
                    for (categorySnapshot in snapshot.children) {
                        val url = categorySnapshot.child("url").getValue(String::class.java) ?: ""
                        val name = categorySnapshot.child("name").getValue(String::class.java) ?: ""
                        val iconResIdString = categorySnapshot.child("iconResId")
                            .getValue(String::class.java)
                        val colorHex = categorySnapshot.child("colorHex")
                            .getValue(String::class.java)

                        val iconResId = getIconResourceId(iconResIdString)


                        categories.add(Category(url, name, iconResId, colorHex))
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
    fun getApiConfig(): Flow<Result<Map<String, String>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val configMap = mutableMapOf<String, String>()

                    // Get values with hypen keys and convert to underscore keys for app consistency
                    val apiKey = snapshot.child("API-KEY").getValue(String::class.java)
                    val apiHost = snapshot.child("API-HOST").getValue(String::class.java)

                    if (apiKey != null) {
                        configMap["API_KEY"] = apiKey
                        Timber.d("Found API key in Firebase: $apiKey")
                    } else {
                        Timber.w("API-KEY not found in Firebase")
                    }

                    if (apiHost != null) {
                        configMap["API_HOST"] = apiHost
                        Timber.d("Found API host in Firebase: $apiHost")
                    } else {
                        Timber.w("API-HOST not found in Firebase")
                    }

                    trySend(Result.success(configMap))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing API config from Firebase")
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Firebase Database error: ${error.message}")
                trySend(Result.failure(error.toException()))
            }
        }

        apiConfig.addValueEventListener(listener)
        awaitClose { apiConfig.removeEventListener(listener) }
    }

    private fun getIconResourceId(iconName: String?): Int {
        if (iconName == null) return R.drawable.outline_add_circle_outline_24 // Default icon

        return when (iconName) {
            "ic_trending_up_24" -> R.drawable.ic_trending_up_24
            "ic_compare_arrows_24" -> R.drawable.ic_compare_arrows_24
            "ic_filter_2_24" -> R.drawable.ic_filter_2_24
            "ic_star_24" -> R.drawable.ic_star_24
            "ic_dashboard_customize_24" -> R.drawable.ic_dashboard_customize_24
            "ic_hourglass_split_24" -> R.drawable.ic_hourglass_split_24
            "ic_house_24" -> R.drawable.ic_house_24
            "ic_add_circle_24" -> R.drawable.outline_add_circle_outline_24
            // Add more mappings as needed
            else -> R.drawable.outline_add_circle_outline_24 // Default icon
        }
    }
}
