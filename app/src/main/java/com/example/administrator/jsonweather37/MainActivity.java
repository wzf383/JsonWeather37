package com.example.administrator.jsonweather37;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.example.administrator.jsonweather37.adapter.DictionaryAdapter;
import com.example.administrator.jsonweather37.db.DBHelper;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnClickListener,
        TextWatcher {
    private DBHelper dbHelper; // 用户输入文本框
    private AutoCompleteTextView word; // 定义数据库的名字
    private SQLiteDatabase database;
    private Button searchWord; // 搜索按钮
    private TextView showResult; // 用户显示查询结果*/

    HttpURLConnection httpConn = null;
    InputStream din =null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(getBaseContext());// 打开数据库
        database = dbHelper.openDatabase();

        init();

        searchWord.setOnClickListener(this); // 绑定监听器
        word.addTextChangedListener(this); // 绑定文字改变监听器

        setTitle("天气预报Json");
        word.setText("广州");//初始化，给个初值，方便测试
    }

    public void init() {
        searchWord = (Button) findViewById(R.id.btnSearch);
        word = (AutoCompleteTextView) findViewById(R.id.etWord);
        showResult = (TextView) findViewById(R.id.tvSearchResult);

    }

    public void afterTextChanged(Editable s) {
        Cursor cursor = database.rawQuery(
                "select  distinct(area_name) as _id from weathers where area_name like ?",
                new String[] { s.toString() + "%" });

        // 新建新的Adapter
        DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(this,cursor, true);

        // 绑定适配器
        word.setAdapter(dictionaryAdapter);
        word.setThreshold(1);

    }

    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {

    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    public void onClick(View view) {

        showResult.setText("");//清空数据
        Toast.makeText(MainActivity.this, "正在查询天气信息", Toast.LENGTH_SHORT).show();
        GetJson gd = new GetJson(word.getText().toString());//调用线程类创建的对象
        gd.start();//运行线程对象

    }

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 123:
                    showData((String)msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private  void showData(String jData){
       /* tv_show.setText(jData);*/
        //这里我直接显示json数据，没解析。解析的方法，请参考教材或网上相应的代码
       /* String r = run();*/
        String ha=jData;
        try {
            JSONObject jobj = new JSONObject(ha);
            JSONObject weather = jobj.getJSONObject("data");
            StringBuffer wbf = new StringBuffer();
            wbf.append("温度："+weather.getString("wendu")+"℃"+"\n");
            wbf.append("天气提示："+weather.getString("ganmao")+"\n");

            JSONArray jary = weather.getJSONArray("forecast");
            for(int i=0;i<jary.length();i++){
                JSONObject pobj = (JSONObject)jary.opt(i);
                wbf.append("日期："+pobj.getString("date")+"\n");
                wbf.append(pobj.getString("high")+"\n");
                wbf.append(pobj.getString("low")+"\n");
                String fengli=pobj.getString("fengli");
                int ep=fengli.indexOf("]]");
                fengli=fengli.substring(9,ep);
                wbf.append("风向："+pobj.getString("fengxiang")+"  ");   wbf.append("风力："+fengli+"\n");
                wbf.append("天气："+pobj.getString("type")+"\n");
            }
            showResult.setText(wbf.toString());
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    class GetJson extends Thread{

        private String urlstr =  "http://wthrcdn.etouch.cn/weather_mini?city=";
        public GetJson(String cityname){
            try{
                urlstr = urlstr+ URLEncoder.encode(cityname,"UTF-8");

            }catch (Exception ee){

            }
        }
        @Override
        public void run() {
            try {
                URL url = new URL(urlstr);
                httpConn = (HttpURLConnection)url.openConnection();
                httpConn.setRequestMethod("GET");
                din = httpConn.getInputStream();
                InputStreamReader in = new InputStreamReader(din);
                BufferedReader buffer = new BufferedReader(in);
                StringBuffer sbf = new StringBuffer();
                String line = null;
                while( (line=buffer.readLine())!=null) {
                    sbf.append(line);
                }
                Message msg = new Message();
                msg.obj = sbf.toString();
                msg.what = 123;
                handler.sendMessage(msg);
                Looper.prepare(); //在线程中调用Toast，要使用此方法，这里纯粹演示用:)
                Toast.makeText(MainActivity.this,"获取数据成功",Toast.LENGTH_LONG).show();
                Looper.loop(); //在线程中调用Toast，要使用此方法



            }catch (Exception ee){
                Looper.prepare(); //在线程中调用Toast，要使用此方法
                Toast.makeText(MainActivity.this,"获取数据失败，网络连接失败或输入有误",Toast.LENGTH_LONG).show();
                Looper.loop(); //在线程中调用Toast，要使用此方法
                ee.printStackTrace();
            }finally {
                try{
                    httpConn.disconnect();
                    din.close();

                }catch (Exception ee){
                    ee.printStackTrace();
                }
            }
        }
    }

}
