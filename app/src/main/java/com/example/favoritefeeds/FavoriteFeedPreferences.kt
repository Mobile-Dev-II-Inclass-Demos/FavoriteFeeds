package com.example.favoritefeeds

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.*
//import java.util.prefs.Preferences

class FavoriteFeedPreferences(private val context: Context) {

    //make this a singleton - only 1 instance exists
    companion object {
        private val Context.datastore: DataStore<Preferences> by
                preferencesDataStore("favoriteFeeds")
    } //companion object

    suspend fun getPreferences(): Flow<Preferences>{
        return context.datastore.data
    }

    suspend fun getFeed(key: String): Flow<String>{
        // gives us strings given the key
        return context.datastore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: ""
        }
    }

    suspend fun setFeed(feed: FeedData){
        context.datastore.edit { preferences ->
            preferences[stringPreferencesKey(feed.tag)] = feed.path

        }
    }

    suspend fun getValueByKey(key: Preferences.Key<*>): FeedData? {
        val value = context.datastore.data.map {
            it[key]
        }
        val actualValue = value.firstOrNull()
        if (actualValue == null){
            return null
        }
        //need to cast
        return FeedData(actualValue.toString(), key.toString())
    }

    suspend fun removeFeed(feed: FeedData){
        context.datastore.edit { preferences ->
            preferences.remove(stringPreferencesKey(feed.tag))
        }
    }

    suspend fun readAllFeeds(): SnapshotStateList<FeedData>{
        val keys = context.datastore.data.map {
            it.asMap().keys
        }
        val actualKeys = keys.firstOrNull()
        if(actualKeys == null){
            return SnapshotStateList()
        }
        val returnList = SnapshotStateList<FeedData>()
        actualKeys.forEach {
            val path = context.datastore.data.map { preferences ->
                preferences[it].toString() ?: "" //the 'it' here is from forEach
            }
            returnList.add(FeedData(path.first(), it.toString()))
        }
        return returnList
    }

    suspend fun removeAllFeeds() {
        context.datastore.edit {
            it.clear()
        }
    }

} //class