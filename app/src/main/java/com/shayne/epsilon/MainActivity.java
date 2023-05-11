package com.shayne.epsilon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private TextView statusnetworktype, statusnetworkspeed,statusbatterytemperature,statusdevicefps,redeemadduser,adduserexit,adduserresponse,redeemtotal,redeemsuccess;
    private Button startactivity,adduserbutton,exitactivity,redeemaddcode;
    private EditText adduserplayerinfo,adduserverificationcode;
    private WindowManager redeemManagerService;
    private WindowManager.LayoutParams redeemManager;
    private ImageView redeemclipbot, redeemocr,redeemminiocr,redeemclearresult;
    private View redeemView, adduserView;
    private AlertDialog addUserDialog;
    private LinearLayout redeemholderview, redeemresultholder;
    private RecyclerView redeemprofile, redeemcodeview;
    private redeemProfileAdapter profileAdapter;
    private RequestNetwork vcrequest, ignrequest,redeemrequest;
    private RequestNetwork.RequestListener vcrequest_listener, ignrequest_listener, redeemrequest_listener;
    private final ArrayList<HashMap<String, Object>> redeemprofilelist = new ArrayList<>();
    private HashMap<String,Object> redeemprofilemap, requestnetworkmap, requestheadersmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusnetworktype = findViewById(R.id.statusnetworktype);
        statusnetworkspeed = findViewById(R.id.statusnetworkspeed);
        statusbatterytemperature = findViewById(R.id.statusbatterytemperature);
        statusdevicefps = findViewById(R.id.statusdevicefps);
        startactivity = findViewById(R.id.startactivity);
        exitactivity = findViewById(R.id.exitactivity);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                new DeviceStatusTask().execute();
                handler.postDelayed(this, 200); // Run the task again after 1 second
            }
        };
        handler.postDelayed(runnable, 200);

        implementFloating();
        requestNetworkTask();
        exitactivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExistFloating(redeemView)) {
                    redeemManagerService.removeView(redeemView);
                }
            }
        });
        startactivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Settings.canDrawOverlays(MainActivity.this)) {
                    if (!isExistFloating(redeemView)) {
                        redeemManagerService.addView(redeemView,redeemManager);
                    }
                } else {
                    requestOverlayPermission();
                }
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishAffinity();
        System.exit(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == 25) {
            if (Settings.canDrawOverlays(MainActivity.this) && !isExistFloating(redeemView)) {
                redeemManagerService.addView(redeemView, redeemManager);
            } else {
                makeToast("Overlay Permission not Granted");
            }
        }
    }

    public void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
        startActivityForResult(intent,25);
    }

    private void implementFloating() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        redeemManager = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT
        );

        redeemManagerService = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        redeemView = getLayoutInflater().inflate(R.layout.redeemlayout, null);
        redeemprofile = redeemView.findViewById(R.id.redeemprofile);
        profileAdapter = new redeemProfileAdapter(redeemprofilelist);
        redeemprofile.setAdapter(profileAdapter);
        redeemprofile.setLayoutManager(new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.HORIZONTAL,false));
        redeemadduser = redeemView.findViewById(R.id.redeemadduser);
        redeemholderview = redeemView.findViewById(R.id.redeemholderview);
        redeemresultholder = redeemView.findViewById(R.id.redeemresultholder);
        redeemcodeview = redeemView.findViewById(R.id.redeemcodeview);
        redeemaddcode = redeemView.findViewById(R.id.redeemaddcode);
        redeemclipbot = redeemView.findViewById(R.id.redeemclipbot);
        redeemocr = redeemView.findViewById(R.id.redeemocr);
        redeemminiocr = redeemView.findViewById(R.id.redeemminiocr);
        redeemtotal = redeemView.findViewById(R.id.redeemtotal);
        redeemsuccess = redeemView.findViewById(R.id.redeemsuccess);
        redeemclearresult = redeemView.findViewById(R.id.redeemclearresult);
        ArrayList<Double> result = new ArrayList<>();

        redeemView.findViewById(R.id.redeemicon).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = redeemManager.x;
                        initialY = redeemManager.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        redeemManager.x = initialX + (int) (event.getRawX() - initialTouchX);
                        redeemManager.y = initialY + (int) (event.getRawY() - initialTouchY);
                        redeemManagerService.updateViewLayout(redeemView, redeemManager);
                        break;
                }
                return false;
            }
        });

        redeemView.findViewById(R.id.redeemicon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (redeemholderview.getVisibility() == View.GONE) {
                    redeemholderview.setVisibility(View.VISIBLE);
                    redeemManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    redeemManagerService.updateViewLayout(redeemView,redeemManager);
                } else {
                    redeemholderview.setVisibility(View.GONE);
                    redeemManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    redeemManagerService.updateViewLayout(redeemView,redeemManager);
                }
            }
        });

        redeemaddcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View asd = LayoutInflater.from(getApplicationContext()).inflate(R.layout.resultdisplayview, null);
                final TextView resultdisplayname = asd.findViewById(R.id.resultdisplayname);
                final TextView resultdisplayserver = asd.findViewById(R.id.resultdisplayserver);
                if (!resultExistLookUp("TESTACLE")) {
                    result.add((double)1);
                    resultdisplayserver.setText("0");
                    resultdisplayname.setText("TESTACLE");
                    redeemresultholder.addView(asd);
                } else {
                    for (int i = 0; i < redeemresultholder.getChildCount(); i++) {
                        final LinearLayout resultdisplaytab = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplaytab);
                        final TextView resultdisplayname2 = resultdisplaytab.findViewById(R.id.resultdisplayname);
                        final TextView resultdisplayserver2 = resultdisplaytab.findViewById(R.id.resultdisplayserver);
                        if (resultdisplayname2.getText().toString().equals("TESTACLE")) {
                            result.set(i,(double)result.get((int)i).doubleValue()+1);
                            resultdisplayserver2.setText(String.valueOf((long) result.get((int)i).doubleValue()));
                        }
                    }
                }
            }
        });
        redeemaddcode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final View asd = LayoutInflater.from(getApplicationContext()).inflate(R.layout.resultdisplayview, null);
                final TextView resultdisplayname = asd.findViewById(R.id.resultdisplayname);
                final TextView resultdisplayserver = asd.findViewById(R.id.resultdisplayserver);
                if (!resultExistLookUp("POOP")) {
                    result.add((double)1);
                    resultdisplayserver.setText("0");
                    resultdisplayname.setText("POOP");
                    redeemresultholder.addView(asd);
                } else {
                    for (int i = 0; i < redeemresultholder.getChildCount(); i++) {
                        final LinearLayout resultdisplaytab = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplaytab);
                        final TextView resultdisplayname2 = resultdisplaytab.findViewById(R.id.resultdisplayname);
                        final TextView resultdisplayserver2 = resultdisplaytab.findViewById(R.id.resultdisplayserver);
                        if (resultdisplayname2.getText().toString().equals("POOP")) {
                            result.set(i,(double)result.get((int)i).doubleValue()+1);
                            resultdisplayserver2.setText(String.valueOf((long) result.get((int)i).doubleValue()));
                        }
                    }
                }
                return true;
            }
        });

        redeemManager.gravity = Gravity.TOP | Gravity.LEFT;
        redeemManager.x = 20;
        redeemManager.y = 300;
    }

    private boolean resultExistLookUp(String name) {
        boolean[] isExist = {false};
        for (int i = 0; i < redeemresultholder.getChildCount(); i++) {
            final LinearLayout resultdisplaytab = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplaytab);
            final TextView resultdisplayname = resultdisplaytab.findViewById(R.id.resultdisplayname);
            final TextView resultdisplayserver = resultdisplaytab.findViewById(R.id.resultdisplayserver);
            if (resultdisplayname.getText().toString().equals(name)) {
                isExist[0] = true;
            }
        }
        return isExist[0];
    }

    private void addFloatingDialog(boolean userinfo, boolean vc, String bt,int pos) {
        adduserView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.adduserlayout, null);
        addUserDialog = new AlertDialog.Builder(getApplicationContext()).create();
        adduserplayerinfo = adduserView.findViewById(R.id.adduserplayerinfo);
        adduserbutton = adduserView.findViewById(R.id.adduserbutton);
        adduserverificationcode = adduserView.findViewById(R.id.adduserverificationcode);
        adduserresponse = adduserView.findViewById(R.id.adduserresponse);
        adduserexit = adduserView.findViewById(R.id.adduserexit);
        adduserresponse.setVisibility(View.GONE);

        if (vc) {
            adduserverificationcode.setVisibility(View.GONE);
        }
        if (userinfo) {
            adduserplayerinfo.setVisibility(View.GONE);
        }
        if (bt != "") {
            adduserbutton.setText(bt);
        }

        adduserbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adduserresponse.setVisibility(View.GONE);
                if (adduserbutton.getText().toString().equals("REQUEST CODE")) {
                    String[] infoSeperator = adduserplayerinfo.getText().toString().split(" ");
                    if (infoSeperator.length > 1 && infoSeperator.length < 3) {
                        requestheadersmap = new HashMap<>();
                        requestnetworkmap = new HashMap<>();

                        requestheadersmap.put("Content-Type","application/json");
                        requestnetworkmap.put("token","e934b1a8d62642d788727f409c6d207c");
                        requestnetworkmap.put("appId","APP20210608084718702");
                        requestnetworkmap.put("country","country");
                        requestnetworkmap.put("language","en");
                        requestnetworkmap.put("appAlias","mlbb_diamonds");
                        requestnetworkmap.put("serverId",infoSeperator[1]);
                        requestnetworkmap.put("goodsId","G20210706061905805");
                        requestnetworkmap.put("payTypeId","587769");
                        requestnetworkmap.put("userId",infoSeperator[0]);

                        ignrequest.setHeaders(requestheadersmap);
                        ignrequest.setParams(requestnetworkmap,RequestNetworkController.REQUEST_BODY);
                        ignrequest.startRequestNetwork(RequestNetworkController.POST,"https://topup-center.shoplay365.com/shareit-topup-center/order/check-uid", infoSeperator[0].concat(":").concat(infoSeperator[1]), ignrequest_listener);
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Requesting...");
                    } else {
                        makeToast(String.valueOf((long)infoSeperator.length));
                        adduserresponse.setText("Error: Invalid Information Format.");
                    }
                } else if (adduserbutton.getText().toString().equals("UPDATE CODE")) {
                    if (adduserverificationcode.getText().toString().length() == 6) {
                        redeemprofilelist.get(pos).put("vc", adduserverificationcode.getText().toString());
                        profileAdapter.notifyDataSetChanged();
                        addUserDialog.dismiss();
                    } else {
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Error: Verification Code must be 6 digit number.");
                    }
                } else if (adduserbutton.getText().toString().equals("ADD USER")) {
                    if (adduserverificationcode.getText().toString().length() == 6) {
                        redeemprofilemap.put("vc", adduserverificationcode.getText().toString());
                        redeemprofilelist.add(redeemprofilemap);
                        profileAdapter.notifyDataSetChanged();
                        redeemprofile.scrollToPosition(redeemprofilelist.size() -1);
                        addUserDialog.dismiss();
                    } else {
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Error: Verification Code must be 6 digit number.");
                    }
                }
            }
        });
        adduserexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserDialog.dismiss();
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        addUserDialog.setView(adduserView);
        addUserDialog.getWindow().setGravity(Gravity.CENTER);
        addUserDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addUserDialog.getWindow().setAttributes(params);
        addUserDialog.setCancelable(false);
        if (!addUserDialog.isShowing()) {
            addUserDialog.show();
        }
    }

    private void requestNetworkTask() {
        vcrequest = new RequestNetwork(this);
        vcrequest_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeader) {
            }
            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };

        ignrequest = new RequestNetwork(this);
        ignrequest_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeader) {
                try {
                    JSONObject mainObject = new JSONObject(response);
                    if (mainObject.getString("code").equals("CHECK_UID_ILLEGAL")) {
                        adduserresponse.setText("Error: No User Found, Please Try Again.");
                    } else if (mainObject.getString("code").equals("200")) {
                        adduserresponse.setText("User Found: ".concat(mainObject.getJSONObject("data").getString("nickName").toString()));
                        adduserplayerinfo.setVisibility(View.GONE);
                        adduserverificationcode.setVisibility(View.VISIBLE);
                        adduserbutton.setText("ADD USER");
                        String[] infoSeperator = tag.split(":");
                        redeemprofilemap = new HashMap<>();
                        redeemprofilemap.put("username",mainObject.getJSONObject("data").getString("nickName").toString());
                        redeemprofilemap.put("server",infoSeperator[1]);
                        redeemprofilemap.put("id",infoSeperator[0]);
                        redeemprofilemap.put("active", false);
                    }
                } catch (Exception e) {
                    adduserresponse.setVisibility(View.VISIBLE);
                    adduserresponse.setText(e.getMessage().toString());
                }
            }
            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };

        redeemrequest = new RequestNetwork(this);
        redeemrequest_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeader) {
            }
            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };
    }

    private boolean isExistFloating(View view) {
        if (view.getParent() != null) {
            return true;
        } else {
            return false;
        }
    }
    public float screenFPS() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = getWindowManager().getDefaultDisplay();
        Display.Mode displayMode = display.getMode();

        return displayMode.getRefreshRate();
    }

    public String getNetworkType() {
        String networkType = "NO DATA";
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                networkType = "WLAN";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkType = "MOBILE DATA";
            }
        }

        return networkType;
    }

    public float getBatteryTemperature() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (intent != null) {
            return (float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
        }

        return -1.0f;
    }

    public long getIternetPing() {
        try {
            long beforeTime = System.currentTimeMillis();
            InetAddress inetAddress = InetAddress.getByName("www.google.com");
            long afterTime = System.currentTimeMillis();

            return afterTime - beforeTime;
        } catch (UnknownHostException e) {
            return 32000;
        }
    }

    private class DeviceStatusTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            statusnetworkspeed.setText(String.valueOf(getIternetPing()).concat("ms"));
            statusnetworktype.setText(getNetworkType());
            statusbatterytemperature.setText(String.valueOf((long)getBatteryTemperature()).concat("°C"));
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            statusdevicefps.setText(String.valueOf((long)screenFPS()));
        }
    }

    public void makeToast(String  text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public class redeemProfileAdapter extends RecyclerView.Adapter<redeemProfileAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> arrayData;
        public redeemProfileAdapter(ArrayList<HashMap<String, Object>> redeemprofilelist) {
            this.arrayData = arrayData;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.profilelayout, null);
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(layoutParams);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            View view = holder.itemView;
            final TextView profilename = view.findViewById(R.id.profilename);
            final TextView profileserver = view.findViewById(R.id.profileserver);
            final LinearLayout profilelayout = view.findViewById(R.id.profilelayout);
            final boolean[] isLongPress = {true};
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);

            profilename.setText(redeemprofilelist.get(position).get("username").toString());
            profileserver.setText(redeemprofilelist.get(position).get("server").toString());

            if (redeemprofilelist.get(position).get("active").toString().equals("false")) {
                gradientDrawable.setStroke(2, Color.parseColor("#212121"));
                profilelayout.setBackground(gradientDrawable);
            } else {
                gradientDrawable.setStroke(2, Color.WHITE);
                profilelayout.setBackground(gradientDrawable);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (redeemprofilelist.get(position).get("active").toString().equals("false")) {
                        redeemprofilelist.get(position).put("active","true");
                        profileAdapter.notifyDataSetChanged();
                    } else {
                        redeemprofilelist.get(position).put("active","false");
                        profileAdapter.notifyDataSetChanged();
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isLongPress[0]) {
                        addFloatingDialog(true,false,"UPDATE CODE",position);
                        adduserverificationcode.setText(String.valueOf(redeemprofilelist.get(position).get("vc")));
                    }
                    return false;
                }
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                private int initialY;
                private float initialTouchY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialTouchY = event.getRawY();
                            isLongPress[0] = true;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            v.setTranslationY((event.getRawY() - initialTouchY));
                            if (Math.abs(event.getRawY()-initialTouchY) > 0) {
                                redeemprofile.setLayoutFrozen(true);
                                isLongPress[0] = false;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            redeemprofile.setLayoutFrozen(false);
                            if (Math.abs(event.getRawY()-initialTouchY) > v.getHeight() * 0.7) {
                                if (v.getTranslationY() > 1) {
                                    v.animate()
                                            .translationY(v.getHeight())
                                            .setDuration(300)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animator) {
                                                    redeemprofilelist.remove(position);
                                                    profileAdapter.notifyItemRemoved(position);
                                                    profileAdapter.notifyItemRangeChanged(position, redeemprofilelist.size()-position);
                                                    v.setTranslationY(0);
                                                }
                                            });
                                } else if (v.getTranslationY() < 0) {
                                    v.animate()
                                            .translationY(-v.getHeight())
                                            .setDuration(300)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animator) {
                                                    redeemprofilelist.remove(position);
                                                    profileAdapter.notifyItemRemoved(position);
                                                    profileAdapter.notifyItemChanged(position);
                                                    v.setTranslationY(0);
                                                }
                                            });
                                }
                            } else {
                                v.animate().translationY(0).setDuration(200);
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return redeemprofilelist.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}