package com.example.favoritefeeds

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.favoritefeeds.ui.theme.FavoriteFeedsTheme
import com.example.favoritefeeds.ui.theme.lightOrange
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("UnrememberedMutableState")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // use of courutine over threads
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val datastore = FavoriteFeedPreferences(context)

            val listItems: SnapshotStateList<FeedData> = (
                // blocks the mainthread until everything is done
                // avoids storing unnecessary data
                runBlocking {
                    val tempList = datastore.readAllFeeds()
                    return@runBlocking tempList
                }
//                FeedData("cnn_tech.rss", "Tech"),
//                FeedData("cnn_topstories.rss", "Top"),
//                FeedData("cnn_world.rss", "World")
            )
            //Todo: assignment, if path changes, just update. If tag changes, remove the entire thing and replace.
            FavoriteFeedsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Feeds(listItems=listItems)
                } // Surface
            } // Theme
        } // setContent
    } // onCreate
} // Activity

@Composable
fun Feeds(listItems: SnapshotStateList<FeedData>) {
    var feedPathState by remember {
        mutableStateOf("")
    }

    var tagState by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = FavoriteFeedPreferences(context) //singleton


//    val listItems = mutableStateListOf(
//        FeedData("cnn_tech.rss", "Tech"),
//        FeedData("cnn_topstories.rss", "Top"),
//        FeedData("cnn_world.rss", "World")
//    )

    listItems.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER, {it.tag}))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ){

        TextField(
            value = feedPathState,
            onValueChange = {
                feedPathState = it
            },
            label = {
                Text("Feed Path")
            },
            maxLines = 1,
            textStyle = TextStyle(
                color = MaterialTheme.colors.primary,
                fontSize = 18.sp
            ),
            placeholder = {
                Text(text = stringResource(id=R.string.queryPrompt))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) // Path TextField

        Spacer(modifier = Modifier.height(8.dp))

        TagRow(
            tag = tagState,
            updateTag = { newTag ->
                tagState = newTag
            },
            feedPath = feedPathState,
            updateFeedPath = { newPath ->
                feedPathState =  newPath
            },
            listItems = listItems
        ) // TagRow

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.weight(1f) // so column takes up the remaining space
        ) {

            Box(
                modifier = Modifier
                    .background(lightOrange)
                    .padding(8.dp)
            ) {
                FeedsRow(
                    listItems,
                    feedPath = feedPathState,
                    feedTag = tagState,
                    updateFeedPath = { newPath ->
                        feedPathState = newPath
                    },
                    updateTag = { newTag ->
                        tagState = newTag
                    }
                ) // FeedsRow
            } // Box

        } // Tag Buttons Column

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            listItems.removeAll(listItems) //also remove in listItems
           scope.launch {
               dataStore.removeAllFeeds() //calls Async call to remove all feeds
           }
        }){
            Text(
                text = stringResource(id=R.string.clearTags),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) // Text
        } // Button
    } // Main Column

} // Feeds

@Composable
fun TagRow(tag: String, updateTag: (String) -> Unit,
           feedPath: String,
           updateFeedPath: (String) -> Unit,
           listItems: SnapshotStateList<FeedData>
           ){

    var context = LocalContext.current // needed for a dialog
    val scope = rememberCoroutineScope() //courutine scope
    val dataStore = FavoriteFeedPreferences(context)
    val focusManager = LocalFocusManager.current //needed for dismissing the keyboard


    // ues IntrinsicSize so we can center text on the button
    // using fillMaxSize/Width/Height on children
    Row(
      modifier = Modifier
          .height(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically
    ) {
       TextField(
           value = tag,
           onValueChange = {updateTag(it)},
           label = {
               Text("Feed Tag")
           },
           maxLines = 1,
           textStyle = TextStyle(
               color = MaterialTheme.colors.primary,
            fontWeight = FontWeight.Bold,
               fontSize = 18.sp
           ),
           placeholder = {
               Text(text= stringResource(id = R.string.tagPrompt))
           },
           keyboardOptions = KeyboardOptions(
               keyboardType = KeyboardType.Text,
               imeAction = ImeAction.Next
           )
       ) // TextField

        Spacer(modifier = Modifier.width(8.dp))

        Button(onClick = {
            if (tag.isNotEmpty() && feedPath.isNotEmpty()) {
                val newFeed = FeedData(path = feedPath, tag = tag)
                listItems.add(newFeed)
                Log.d("Tesfa", "element added")
                scope.launch {
                    dataStore.setFeed(newFeed)
                }
                listItems.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.tag }))
                Log.d("Tesfa", listItems.joinToString())
                // currently wiping out the data
                updateTag("")
                updateFeedPath("")
                focusManager.clearFocus() //hides the keyboard
            }
            else{
                val builder = AlertDialog.Builder(context)
                builder.setTitle(R.string.missingTitle)
                builder.setPositiveButton(R.string.OK, null)
                builder.setMessage(R.string.missingMessage)
                val errorDialog: AlertDialog = builder.create()
                errorDialog.show()
            } //error
        }) {
            Text(
                text= stringResource(id= R.string.save),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
                ) // Text
        } // Button
    } // Row

} // Tag Row

data class FeedData(val path: String, val tag: String)

@SuppressLint("UnrememberedMutableState")
@Composable
fun FeedsRow(
    listItems: SnapshotStateList<FeedData>,
    feedPath: String,
    feedTag: String,
    updateFeedPath: (String) -> Unit,
    updateTag: (String) -> Unit
) {
    val listState = rememberLazyListState()

//    //do we need to hoist this state? moved higher
//    val listItems = mutableStateListOf(
//        FeedData("cnn_tech.rss", "Tech"),
//        FeedData("cnn_topstories.rss", "Top"),
//        FeedData("cnn_world.rss", "World")
//    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize(),
        state = listState
    ){
        itemsIndexed(
            items = listItems,
            key = { _, listItem ->
                listItem.hashCode()
            }
        ){ index, item ->
            //TODO - update state for path and tag so edit texts are filled in
            FeedItemRow(feedItem = item, updateFeedPath, updateTag)
        }//itemsIndexed
        item {
            if(listItems.count() == 0){
                //TODO - may be move to another composable
                Column(
                    modifier = Modifier.padding(all = 8.dp)
                ) {
                    Text(
                        text = "No Feeds to Display",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(all = 4.dp),
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.h4
                    )
                } //Column
            } //if count==0
        } //item
    }//LazyColumn
} // FeedsRow

@Composable
fun FeedItemRow(feedItem: FeedData,
                updateFeedPath: (String) -> Unit,
                updateTag: (String) -> Unit
                ){
    // get the context and the base url from the string resources
    val context = LocalContext.current
    val baseUrl = stringResource(id = R.string.searchURL)

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                // TODO
                var urlString = baseUrl + feedItem.path
                // create the intent to launch a web browser
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                startActivity(context, webIntent, null) //execute the intent
            }
        ) {
            Text(
                text = feedItem.tag,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        } // Button

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                // TODO
                updateFeedPath(feedItem.path)
                updateTag(feedItem.tag)
            }
        ) {
            Text(
                text = stringResource(id = R.string.edit),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        } // Button

    }
} //FeedItemRow

@Preview(showBackground = true)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DefaultPreview() {
    FavoriteFeedsTheme {
        val listItems = mutableStateListOf(
            FeedData("cnn_tech.rss", "Tech"),
            FeedData("cnn_topstories.rss", "Top"),
            FeedData("cnn_world.rss", "World")
        )
        Feeds(listItems)
    }
}