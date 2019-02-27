package com.example.mandeep.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();
    ArrayList<String> titles = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    ArrayList<String> urls = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();

    SQLiteDatabase articlesDB;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleURL", urls.get(position));
                i.putExtra("content", content.get(position));
                startActivity(i);
            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();

        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateListView() {
        try {

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleID DESC", null);

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            titles.clear();
            urls.clear();

            if (c != null && c.moveToFirst()) {
                do {

                    titles.add(c.getString(titleIndex));
                    urls.add(c.getString(urlIndex));
                    content.add(c.getString(contentIndex));

                } while (c.moveToNext());
            }

            c.close();
            arrayAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;

                    result += current;

                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                articlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < 20; i++) {

                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");
                    String articleContent = "";



                    articleIds.add(Integer.valueOf(articleId));
                    articleTitles.put(Integer.valueOf(articleId), articleTitle);
                    articleURLs.put(Integer.valueOf(articleId), articleURL);

                    String sql = "INSERT INTO articles (articleID, url, title, content) VALUES (?, ?, ?, ?)";
                    SQLiteStatement statement = articlesDB.compileStatement(sql);

                    statement.bindString(1, articleId);
                    statement.bindString(2, articleURL);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleContent);

                    statement.execute();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }


}
