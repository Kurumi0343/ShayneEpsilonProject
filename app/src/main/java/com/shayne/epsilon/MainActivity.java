package com.shayne.epsilon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView redeemclearcodes, redeemhideuser, miniocrlabel, editiemexit, statusnetworktype, statusnetworkspeed, statusbatterytemperature, statusdevicefps, redeemadduser, adduserexit, adduserresponse, redeemsuccess;
    private Button startactivity, adduserbutton, exitactivity, redeemaddcode, editiembutton;
    private EditText adduserplayerinfo, adduserverificationcode, editiemdata;
    private WindowManager redeemManagerService, miniOCRManagerService;
    private WindowManager.LayoutParams redeemManager, miniOCRManager;
    private ImageView redeemclipbot, redeemocr, redeemminiocr, redeemclearresult;
    private View redeemView, adduserView, editItemView, miniOCRView;
    private AlertDialog addUserDialog, editItemDialog;
    private LinearLayout redeemholderview, redeemresultholder, miniocrwatcher, redeemusertab;
    private RecyclerView redeemprofile, redeemcodeview;
    private redeemProfileAdapter profileAdapter;
    private ToggleButton miniocrtoggleswitch;
    private redeemCodeItemAdapter CodeItemAdapter;
    private RequestNetwork vcrequest, ignrequest, redeemrequest;
    private RequestNetwork.RequestListener vcrequest_listener, ignrequest_listener, redeemrequest_listener;
    private ArrayList<HashMap<String, Object>> redeemprofilelist = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> redeemcodelist = new ArrayList<>();
    private HashMap<String, Object> itemmap, requestnetworkmap, requestheadersmap;
    private final ArrayList<Double> resultcount = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private boolean listenClipboard;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private Bitmap screenBitmap, miniOCRBitmap, resizeBitmap;
    private Rect rect;
    private Timer miniOCRTimer;
    private TimerTask miniOCRTimerTask;
    private int screenWidth, screenHeight, rootHeight, rootWidth;
    private long successcounter = 0;

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
        sharedPreferences = getSharedPreferences("data", Activity.MODE_PRIVATE);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                new DeviceStatusTask().execute();
                handler.postDelayed(this, 200);
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
                        redeemManagerService.addView(redeemView, redeemManager);
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
    protected void onStart() {
        super.onStart();

        final View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootWidth = rootView.getWidth();
                rootHeight = rootView.getHeight();
            }
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 25) {
            if (Settings.canDrawOverlays(MainActivity.this) && !isExistFloating(redeemView)) {
                redeemManagerService.addView(redeemView, redeemManager);
            } else {
                makeToast("Overlay Permission not Granted");
            }
        }

        if (requestCode == 30 && resultCode == RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        }
    }

    public void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 25);
    }

    public void requestRecordingPermission() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenShot = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(screenShot, 30);
        Intent projectionServiceIntent = new Intent(this, MediaProjectionService.class);
        ContextCompat.startForegroundService(this, projectionServiceIntent);
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
        redeemcodeview = redeemView.findViewById(R.id.redeemcodeview);
        if (!sharedPreferences.getString("savedprofile", "").equals("")) {
            redeemprofilelist = new Gson().fromJson(sharedPreferences.getString("savedprofile", ""), new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType());
        }
        if (!sharedPreferences.getString("savedcode", "").equals("")) {
            redeemcodelist = new Gson().fromJson(sharedPreferences.getString("savedcode", ""), new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType());
        }
        profileAdapter = new redeemProfileAdapter(redeemprofilelist);
        CodeItemAdapter = new redeemCodeItemAdapter(redeemcodelist);
        redeemprofile.setAdapter(profileAdapter);
        redeemcodeview.setAdapter(CodeItemAdapter);
        redeemcodeview.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        redeemprofile.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        redeemadduser = redeemView.findViewById(R.id.redeemadduser);
        redeemholderview = redeemView.findViewById(R.id.redeemholderview);
        redeemresultholder = redeemView.findViewById(R.id.redeemresultholder);
        redeemaddcode = redeemView.findViewById(R.id.redeemaddcode);
        redeemclipbot = redeemView.findViewById(R.id.redeemclipbot);
        redeemocr = redeemView.findViewById(R.id.redeemocr);
        redeemminiocr = redeemView.findViewById(R.id.redeemminiocr);
        redeemsuccess = redeemView.findViewById(R.id.redeemsuccess);
        redeemclearresult = redeemView.findViewById(R.id.redeemclearresult);
        redeemhideuser = redeemView.findViewById(R.id.redeemhideuser);
        redeemclearcodes = redeemView.findViewById(R.id.redeemclearcodes);
        redeemusertab = redeemView.findViewById(R.id.redeemusertab);


        clipboardListener();

        rect = new Rect();
        int[] rectLocation = new int[2];

        miniOCRView = getLayoutInflater().inflate(R.layout.miniocrlayout, null);
        miniocrtoggleswitch = miniOCRView.findViewById(R.id.miniocrtoggleswitch);
        miniocrwatcher = miniOCRView.findViewById(R.id.miniocrwatcher);
        miniocrlabel = miniOCRView.findViewById(R.id.miniocrlabel);
        miniOCRTimer = new Timer();

        redeemhideuser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (redeemhideuser.getText().toString().equals("HIDE USER")) {
                    redeemusertab.setVisibility(View.GONE);
                    redeemhideuser.setText("SHOW USER");
                } else {
                    redeemusertab.setVisibility(View.VISIBLE);
                    redeemhideuser.setText("HIDE USER");
                }
            }
        });

        miniocrlabel.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = miniOCRManager.x;
                        initialY = miniOCRManager.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        miniOCRManager.x = initialX + (int) (event.getRawX() - initialTouchX);
                        miniOCRManager.y = initialY + (int) (event.getRawY() - initialTouchY);
                        miniOCRManagerService.updateViewLayout(miniOCRView, miniOCRManager);
                        break;
                    case MotionEvent.ACTION_UP:
                        miniocrwatcher.getGlobalVisibleRect(rect);
                        miniocrwatcher.getLocationOnScreen(rectLocation);
                        rect.offset(rectLocation[0], rectLocation[1]);
                        rect.set(rect.left, rect.top - 75, rect.right, rect.bottom - 75);
                        break;
                }
                return true;
            }
        });

        miniOCRManager = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT
        );

        miniocrtoggleswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    miniOCRTimerTask.cancel();
                } catch (Exception e) {

                }
                if (miniocrtoggleswitch.isChecked()) {
                    final GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setStroke(2, Color.WHITE);
                    miniocrtoggleswitch.setBackground(gradientDrawable);
                    miniOCRManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    miniOCRManagerService.updateViewLayout(miniOCRView, miniOCRManager);
                    miniOCRTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (miniocrtoggleswitch.isChecked()) {
                                        doMiniOCR();
                                    }
                                }
                            });
                        }
                    };
                    miniOCRTimer.scheduleAtFixedRate(miniOCRTimerTask, 20, 600);
                } else {
                    final GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setStroke(2, Color.parseColor("#212121"));
                    miniocrtoggleswitch.setBackground(gradientDrawable);
                    miniOCRManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    miniOCRManagerService.updateViewLayout(miniOCRView, miniOCRManager);
                }
            }
        });

        miniOCRManagerService = (WindowManager) getSystemService(WINDOW_SERVICE);
        redeemminiocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                miniOCRManager.gravity = Gravity.TOP | Gravity.LEFT;
                miniOCRManager.x = 20;
                miniOCRManager.y = 300;

                if (mediaProjection == null) {
                    requestRecordingPermission();
                } else {
                    if (miniOCRView.getWindowToken() == null) {
                        miniOCRManagerService.addView(miniOCRView, miniOCRManager);
                    } else {
                        miniOCRManagerService.removeView(miniOCRView);
                    }
                }
            }
        });
        redeemocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaProjection == null) {
                    requestRecordingPermission();
                } else {
                    redeemholderview.setVisibility(View.GONE);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doOCR();
                            redeemholderview.setVisibility(View.VISIBLE);
                        }
                    }, 120);
                }
            }
        });

        redeemclearcodes.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                redeemcodelist.clear();
                CodeItemAdapter.notifyDataSetChanged();
                sharedPreferences.edit().putString("savedcode", new Gson().toJson(redeemcodelist)).commit();
                return true;
            }
        });
        redeemclipbot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listenClipboard) {
                    listenClipboard = false;
                    final GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setStroke(2, Color.parseColor("#212121"));
                    redeemclipbot.setBackground(gradientDrawable);
                } else {
                    listenClipboard = true;
                    final GradientDrawable gradientDrawable = new GradientDrawable();
                    gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                    gradientDrawable.setStroke(2, Color.WHITE);
                    redeemclipbot.setBackground(gradientDrawable);
                }
            }
        });
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

        redeemclearresult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redeemresultholder.removeAllViews();
                successcounter = 0;
                resultcount.clear();
            }
        });
        redeemView.findViewById(R.id.redeemicon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (redeemholderview.getVisibility() == View.GONE) {
                    redeemholderview.setVisibility(View.VISIBLE);
                    redeemManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    redeemManagerService.updateViewLayout(redeemView, redeemManager);
                } else {
                    redeemholderview.setVisibility(View.GONE);
                    redeemManager.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    redeemManagerService.updateViewLayout(redeemView, redeemManager);
                }
            }
        });

        redeemadduser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFloatingDialog(false, true, "", 0);
            }
        });

        redeemaddcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUpdateDialog("", 0);
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
            final TextView resultdisplaycode = resultdisplaytab.findViewById(R.id.resultdisplaycode);
            if (resultdisplayname.getText().toString().equals(name)) {
                isExist[0] = true;
            }
        }
        return isExist[0];
    }

    private void addFloatingDialog(boolean userinfo, boolean vc, String bt, int pos) {
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
        if (!bt.equals("")) {
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

                        requestheadersmap.put("Content-Type", "application/json");
                        requestnetworkmap.put("token", "e934b1a8d62642d788727f409c6d207c");
                        requestnetworkmap.put("appId", "APP20210608084718702");
                        requestnetworkmap.put("country", "country");
                        requestnetworkmap.put("language", "en");
                        requestnetworkmap.put("appAlias", "mlbb_diamonds");
                        requestnetworkmap.put("serverId", infoSeperator[1]);
                        requestnetworkmap.put("goodsId", "G20210706061905805");
                        requestnetworkmap.put("payTypeId", "587769");
                        requestnetworkmap.put("userId", infoSeperator[0]);

                        ignrequest.setHeaders(requestheadersmap);
                        ignrequest.setParams(requestnetworkmap, RequestNetworkController.REQUEST_BODY);
                        ignrequest.startRequestNetwork(RequestNetworkController.POST, "https://topup-center.shoplay365.com/shareit-topup-center/order/check-uid", infoSeperator[0].concat(":").concat(infoSeperator[1]), ignrequest_listener);
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Requesting...");
                    } else {
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Error: Invalid Information Format.");
                    }
                } else if (adduserbutton.getText().toString().equals("UPDATE CODE")) {
                    if (adduserverificationcode.getText().toString().length() == 6) {
                        redeemprofilelist.get(pos).put("vc", adduserverificationcode.getText().toString());
                        profileAdapter.notifyDataSetChanged();
                        redeemholderview.removeViewAt(0);
                        redeemhideuser.setVisibility(View.VISIBLE);
                        redeemusertab.setVisibility(View.VISIBLE);
                    } else {
                        adduserresponse.setVisibility(View.VISIBLE);
                        adduserresponse.setText("Error: Verification Code must be 6 digit number.");
                    }
                } else if (adduserbutton.getText().toString().equals("ADD USER")) {
                    if (adduserverificationcode.getText().toString().length() == 6) {
                        itemmap.put("vc", adduserverificationcode.getText().toString());
                        redeemprofilelist.add(itemmap);
                        profileAdapter.notifyDataSetChanged();
                        redeemprofile.scrollToPosition(redeemprofilelist.size() - 1);
                        redeemholderview.removeViewAt(0);
                        redeemhideuser.setVisibility(View.VISIBLE);
                        redeemusertab.setVisibility(View.VISIBLE);
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
                redeemholderview.removeViewAt(0);
                redeemhideuser.setVisibility(View.VISIBLE);
                redeemusertab.setVisibility(View.VISIBLE);
            }
        });
        redeemhideuser.setVisibility(View.GONE);
        redeemusertab.setVisibility(View.GONE);
        if (redeemholderview.indexOfChild(adduserView) <= 0) {
            redeemholderview.addView(adduserView, 0);
        } else {
            redeemholderview.removeViewAt(0);
        }
    }

    private void addUpdateDialog(String bt, int pos) {
        editItemView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.edititem, null);
        editItemDialog = new AlertDialog.Builder(getApplicationContext()).create();
        editiemdata = editItemView.findViewById(R.id.editiemdata);
        editiembutton = editItemView.findViewById(R.id.editiembutton);
        editiemexit = editItemView.findViewById(R.id.editiemexit);

        if (!bt.equals("")) {
            editiembutton.setText(bt);
        }

        editiembutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editiembutton.getText().toString().equals("UPDATE")) {
                    redeemcodelist.get(pos).put("redeemcode", editiemdata.getText().toString());
                    editItemDialog.dismiss();
                    CodeItemAdapter.notifyDataSetChanged();
                } else if (editiembutton.getText().toString().equals("SAVE")) {
                    if (!editiemdata.getText().toString().equals("")) {
                        addRedeemCode(editiemdata.getText().toString());
                        editItemDialog.dismiss();
                    } else {
                        editItemDialog.dismiss();
                    }
                }
            }
        });
        editiemexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editItemDialog.dismiss();
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        editItemDialog.setView(editItemView);
        editItemDialog.getWindow().setGravity(Gravity.CENTER);
        editItemDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        editItemDialog.getWindow().setAttributes(params);
        editItemDialog.setCancelable(false);
        if (!editItemDialog.isShowing()) {
            editItemDialog.show();
        }
    }

    private void requestNetworkTask() {
        vcrequest = new RequestNetwork(this);
        vcrequest_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeader) {
                try {
                    JSONObject mainObject = new JSONObject(response);
                    if (mainObject.getString("code").equals("0")) {
                        makeToast("Verification Sent");
                    } else {
                        makeToast("Verification False");
                    }
                } catch (Exception e) {
                    makeToast(e.getMessage());
                }
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
                        adduserresponse.setText("User Found: ".concat(mainObject.getJSONObject("data").getString("nickName")));
                        adduserplayerinfo.setVisibility(View.GONE);
                        adduserverificationcode.setVisibility(View.VISIBLE);
                        adduserbutton.setText("ADD USER");
                        String[] infoSeperator = tag.split(":");
                        itemmap = new HashMap<>();
                        itemmap.put("username", mainObject.getJSONObject("data").getString("nickName"));
                        itemmap.put("server", infoSeperator[1]);
                        itemmap.put("id", infoSeperator[0]);
                        itemmap.put("active", "true");
                        requestVerification(infoSeperator[0], infoSeperator[1]);
                    }
                } catch (Exception e) {
                    adduserresponse.setVisibility(View.VISIBLE);
                    adduserresponse.setText(e.getMessage());
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
                String[] dataSeperator = tag.split(":");
                String[] responsemessage = {"message"};
                try {
                    JSONObject mainObject = new JSONObject(response);
                    switch (mainObject.getString("code")) {
                        case "-20010":
                            responsemessage[0] = "Invalid Verification Code";
                            break;
                        case "-20009":
                            responsemessage[0] = "Expired Verification Code";
                            break;
                        case "1410":
                            responsemessage[0] = "Redeem Code Is Being Redeemed By Many User";
                            break;
                        case "1407":
                            responsemessage[0] = "Exceeds Exchange Amount Limit";
                            break;
                        case "-20023":
                            responsemessage[0] = "Invalid Account";
                            break;
                        case "1412":
                            responsemessage[0] = "Redeem Code Limit Reach";
                            break;
                        case "0":
                            responsemessage[0] = "Redeem Successful";
                            successcounter = successcounter + 1;
                            redeemsuccess.setText("Success: [".concat(String.valueOf(successcounter)).concat(" ]"));
                            break;
                        case "1419":
                            responsemessage[0] = "This CDKey Is not available in your region";
                            break;
                        case "1405":
                            responsemessage[0] = "CDKey Already Redeemed";
                            break;
                        case "1404":
                            responsemessage[0] = "Incorrect Format CDKey";
                            break;
                        case "1036":
                            responsemessage[0] = "CDKey Limitation Reach";
                            break;
                        case "1416":
                            responsemessage[0] = "Use VPN";
                            break;
                        case "1415":
                            responsemessage[0] = "Your Level is too high for this CDKey";
                            break;
                        case "1414":
                            responsemessage[0] = "This CDKey is not paid yet";
                            break;
                        case "1413":
                            responsemessage[0] = "This CDKey is only for new user";
                            break;
                        case "1411":
                            responsemessage[0] = "You cant redeem this CDKey at this time, Please Wait";
                            break;
                        case "1409":
                            responsemessage[0] = "Restriction Requirement Configuration Error";
                            break;
                        case "1408":
                            responsemessage[0] = "This CDkey is not available in your ServerID";
                            break;
                        case "1406":
                            responsemessage[0] = "This CDKey is Binded, Invalid Account";
                            break;
                        case "1403":
                            responsemessage[0] = "This CDKey is Expired";
                            break;
                        case "1402":
                            responsemessage[0] = "This CDKey does not exist";
                            break;
                        case "1401":
                            responsemessage[0] = "Advance Server Only";
                            break;
                    }
                    if (dataSeperator.length > 1) {
                        if (!resultExistLookUp(dataSeperator[0])) {
                            final View resultdisplayview = LayoutInflater.from(getApplicationContext()).inflate(R.layout.resultdisplayview, null);
                            final TextView resultdisplayname = resultdisplayview.findViewById(R.id.resultdisplayname);
                            final LinearLayout resultdisplaytab = resultdisplayview.findViewById(R.id.resultdisplaytab);
                            final TextView resultdisplaycode = resultdisplayview.findViewById(R.id.resultdisplaycode);
                            final TextView resultdisplaycount = resultdisplayview.findViewById(R.id.resultdisplaycount);
                            final TextView resultdisplayserver = resultdisplayview.findViewById(R.id.resultdisplayserver);
                            final TextView resultdisplaystatus = resultdisplayview.findViewById(R.id.resultdisplaystatus);
                            resultcount.add(Double.valueOf(0));
                            resultdisplaycount.setText("[ 0 ]");
                            resultdisplaycode.setText(dataSeperator[2]);
                            resultdisplayserver.setText(dataSeperator[1]);
                            resultdisplayname.setText(dataSeperator[0]);
                            resultdisplaystatus.setText(responsemessage[0]);
                            redeemresultholder.addView(resultdisplayview);
                        } else {
                            for (int i = 0; i < redeemresultholder.getChildCount(); i++) {
                                final LinearLayout resultdisplaytab = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplaytab);
                                final LinearLayout resultdisplayextra = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplayextra);
                                final TextView resultdisplayname = resultdisplaytab.findViewById(R.id.resultdisplayname);
                                final TextView resultdisplaycode = resultdisplaytab.findViewById(R.id.resultdisplaycode);
                                final TextView resultdisplaystatus = resultdisplaytab.findViewById(R.id.resultdisplaystatus);
                                final TextView resultdisplaycount = resultdisplayextra.findViewById(R.id.resultdisplaycount);
                                final TextView resultdisplayserver = resultdisplayextra.findViewById(R.id.resultdisplayserver);
                                if (resultdisplayname.getText().toString().equals(dataSeperator[0])) {
                                    resultcount.set(i, (double) resultcount.get((int) i).doubleValue() + 1);
                                    resultdisplaycode.setText(dataSeperator[2]);
                                    resultdisplaystatus.setText(responsemessage[0]);
                                    resultdisplaycount.setText("[ ".concat(String.valueOf((long) resultcount.get(i).doubleValue())).concat(" ]"));
                                }
                            }
                        }

                        redeemresultholder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                            @Override
                            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                                for (int i = 0; i < redeemresultholder.getChildCount(); i++) {
                                    final TextView resultdisplayname = redeemresultholder.getChildAt(i).findViewById(R.id.resultdisplayname);
                                    int finalI = i;
                                    redeemresultholder.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                                        private float initialTouchX;

                                        @Override
                                        public boolean onTouch(View v, MotionEvent event) {
                                            switch (event.getAction()) {
                                                case MotionEvent.ACTION_DOWN:
                                                    initialTouchX = event.getRawX();
                                                    break;
                                                case MotionEvent.ACTION_MOVE:
                                                    v.setTranslationX((event.getRawX() - initialTouchX));
                                                    break;
                                                case MotionEvent.ACTION_UP:
                                                    if (Math.abs(event.getRawY() - initialTouchX) > v.getWidth() * 0.7) {
                                                        if (v.getTranslationX() > 1) {
                                                            redeemresultholder.removeViewAt(finalI);
                                                            resultcount.remove(finalI);
                                                            v.setTranslationX(0);
                                                        } else if (v.getTranslationX() < 0) {
                                                            redeemresultholder.removeViewAt(finalI);
                                                            resultcount.remove(finalI);
                                                            v.setTranslationX(0);
                                                        }
                                                    } else {
                                                        v.setTranslationX(0);
                                                    }
                                                    break;
                                            }
                                            return true;
                                        }
                                    });
                                }
                            }
                        });
                    }
                } catch (Exception e) {

                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };
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

    private boolean isExistFloating(View view) {
        return view.getParent() != null;
    }

    private void doOCR() {
        final Handler handler = new Handler();
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenShot", screenWidth, screenHeight, getResources().getDisplayMetrics().densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, handler);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    int pixelStride = image.getPlanes()[0].getPixelStride();
                    int rowStride = image.getPlanes()[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * screenWidth;

                    screenBitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
                    screenBitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());

                    Bitmap scaledBitmap = Bitmap.createBitmap(rootWidth, rootHeight, Bitmap.Config.ARGB_8888);
                    Canvas crop = new Canvas(screenBitmap);
                    crop.drawBitmap(screenBitmap, new Rect(rowPadding * 2, 0, scaledBitmap.getWidth() - rowPadding * 2, screenBitmap.getHeight()), new Rect(0, 0, screenBitmap.getWidth(), screenBitmap.getHeight()), null);

                    scaledBitmap = Bitmap.createScaledBitmap(screenBitmap, rootWidth, rootHeight, false);
                    readImageOCR(scaledBitmap);
                    imageReader.close();
                    virtualDisplay.release();
                }
            }
        }, handler);
    }

    private void doMiniOCR() {
        final Handler handler = new Handler();
        miniocrwatcher.setBackground(null);
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenShot", screenWidth, screenHeight, getResources().getDisplayMetrics().densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, handler);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    int pixelStride = image.getPlanes()[0].getPixelStride();
                    int rowStride = image.getPlanes()[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * screenWidth;

                    screenBitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
                    screenBitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());

                    Bitmap scaledBitmap = Bitmap.createBitmap(rootWidth, rootHeight, Bitmap.Config.ARGB_8888);
                    Canvas crop = new Canvas(screenBitmap);
                    crop.drawBitmap(screenBitmap, new Rect(rowPadding * 2, 0, scaledBitmap.getWidth() - rowPadding * 2, screenBitmap.getHeight()), new Rect(0, 0, screenBitmap.getWidth(), screenBitmap.getHeight()), null);

                    scaledBitmap = Bitmap.createScaledBitmap(screenBitmap, rootWidth, rootHeight, false);
                    miniOCRBitmap = Bitmap.createBitmap(miniocrwatcher.getWidth(), miniocrwatcher.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(miniOCRBitmap);
                    canvas.drawBitmap(scaledBitmap, rect, new Rect(0, 0, miniocrwatcher.getWidth(), miniocrwatcher.getHeight()), null);

                    readImageOCR(miniOCRBitmap);

                    imageReader.close();
                    virtualDisplay.release();
                }
            }
        }, handler);
    }

    private void readImageOCR(Bitmap toscan) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        Frame frame = new Frame.Builder().setBitmap(toscan).build();
        SparseArray<TextBlock> items = textRecognizer.detect(frame);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            TextBlock item = items.get(i);
            String build = item.getValue();
            stringBuilder.append(build);
        }

        String[] dataSeperator = stringBuilder.toString().split("[^a-zA-Z0-9]+");
        if (dataSeperator.length > 0) {
            for (int i = 0; i < dataSeperator.length; i++) {
                if (dataSeperator[i].matches("[a-zA-Z0-9]+[0-9]+[a-zA-Z0-9]*") && dataSeperator[i].length() > 7 && dataSeperator[i].length() < 20) {
                    addRedeemCode(dataSeperator[i]);
                }
            }
        }
    }

    public void makeToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public void requestVerification(String id, String server) {
        requestnetworkmap = new HashMap<>();
        requestnetworkmap.put("language", "en");
        requestnetworkmap.put("roleId", id);
        requestnetworkmap.put("zoneId", server);
        vcrequest.setParams(requestnetworkmap, RequestNetworkController.REQUEST_PARAM);
        vcrequest.startRequestNetwork(RequestNetworkController.GET, "https://api.mobilelegends.com/mlweb/sendMail", "", vcrequest_listener);

    }

    public void requestRedeemCode(String id, String server, String verification, String code, String username) {
        requestnetworkmap = new HashMap<>();
        requestnetworkmap.put("language", "en");
        requestnetworkmap.put("redeemCode", code);
        requestnetworkmap.put("roleId", id);
        requestnetworkmap.put("vCode", verification);
        requestnetworkmap.put("zoneId", server);
        redeemrequest.setParams(requestnetworkmap, RequestNetworkController.REQUEST_BODY);
        redeemrequest.startRequestNetwork(RequestNetworkController.POST, "https://api.mobilelegends.com/mlweb/sendCdk", username.concat(":").concat(server).concat(":").concat(code), redeemrequest_listener);
    }

    private void addRedeemCode(String code) {
        boolean[] isExist = {false};
        for (int i = 0; i < redeemcodelist.size(); i++) {
            if (redeemcodelist.get(i).get("redeemcode").toString().equals(code)) {
                isExist[0] = true;
            }
        }
        if (!isExist[0]) {
            itemmap = new HashMap<>();
            itemmap.put("redeemcode", code);
            redeemcodelist.add(itemmap);
            CodeItemAdapter.notifyDataSetChanged();
            for (int i = 0; i < redeemprofilelist.size(); i++) {
                if (redeemprofilelist.get(i).get("active").toString().equals("true")) {
                    requestRedeemCode(redeemprofilelist.get(i).get("id").toString(), redeemprofilelist.get(i).get("server").toString(), redeemprofilelist.get(i).get("vc").toString(), code, redeemprofilelist.get(i).get("username").toString());
                }
            }
        }
    }

    private void clipboardListener() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (listenClipboard) {
                    ClipData clipData = clipboardManager.getPrimaryClip();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        ClipData.Item item = clipData.getItemAt(0);
                        String[] dataSeperator = item.getText().toString().split("[^a-zA-Z0-9]+");
                        if (dataSeperator.length > 0) {
                            for (int i = 0; i < dataSeperator.length; i++) {
                                if (dataSeperator[i].matches("[a-zA-Z0-9]+[0-9]+[a-zA-Z0-9]*") && dataSeperator[i].length() > 7 && dataSeperator[i].length() < 20) {
                                    addRedeemCode(dataSeperator[i]);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public class redeemProfileAdapter extends RecyclerView.Adapter<redeemProfileAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> arrayData;

        public redeemProfileAdapter(ArrayList<HashMap<String, Object>> arrayList) {
            this.arrayData = arrayList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.profilelayout, null);
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(layoutParams);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            View view = holder.itemView;

            sharedPreferences.edit().putString("savedprofile", new Gson().toJson(redeemprofilelist)).commit();

            final TextView profilename = view.findViewById(R.id.profilename);
            final TextView profileserver = view.findViewById(R.id.profileserver);
            final LinearLayout profilelayout = view.findViewById(R.id.profilelayout);
            final boolean[] isLongPress = {true};
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);

            profilename.setText(arrayData.get(position).get("username").toString());
            profileserver.setText(arrayData.get(position).get("server").toString());

            if (arrayData.get(position).get("active").toString().equals("false")) {
                gradientDrawable.setStroke(2, Color.parseColor("#212121"));
                profilelayout.setBackground(gradientDrawable);
            } else {
                gradientDrawable.setStroke(2, Color.WHITE);
                profilelayout.setBackground(gradientDrawable);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = System.currentTimeMillis();

                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        if (arrayData.get(position).get("active").toString().equals("false")) {
                            arrayData.get(position).put("active", "true");
                            profileAdapter.notifyDataSetChanged();
                        } else {
                            arrayData.get(position).put("active", "false");
                            profileAdapter.notifyDataSetChanged();
                        }
                    }
                    lastClickTime = clickTime;
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isLongPress[0]) {
                        addFloatingDialog(true, false, "UPDATE CODE", position);
                        adduserverificationcode.setText(arrayData.get(position).get("vc").toString());
                        if (arrayData.get(position).get("active").toString().equals("false")) {
                            requestVerification(arrayData.get(position).get("id").toString(), arrayData.get(position).get("server").toString());
                        }
                    }
                    return false;
                }
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
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
                            if (Math.abs(event.getRawY() - initialTouchY) > 0) {
                                redeemprofile.setLayoutFrozen(true);
                                isLongPress[0] = false;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            redeemprofile.setLayoutFrozen(false);
                            if (Math.abs(event.getRawY() - initialTouchY) > v.getHeight() * 0.7) {
                                if (v.getTranslationY() > 1) {
                                    arrayData.remove(position);
                                    profileAdapter.notifyItemRemoved(position);
                                    profileAdapter.notifyItemRangeChanged(position, arrayData.size() - position);
                                    v.setTranslationY(0);
                                    sharedPreferences.edit().putString("savedprofile", new Gson().toJson(redeemprofilelist)).commit();
                                } else if (v.getTranslationY() < 0) {
                                    arrayData.remove(position);
                                    profileAdapter.notifyItemRemoved(position);
                                    profileAdapter.notifyItemRangeChanged(position, arrayData.size() - position);
                                    v.setTranslationY(0);
                                    sharedPreferences.edit().putString("savedprofile", new Gson().toJson(redeemprofilelist)).commit();
                                }
                            } else {
                                v.setTranslationY(0);
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return arrayData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    public class redeemCodeItemAdapter extends RecyclerView.Adapter<redeemCodeItemAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> arrayData;

        public redeemCodeItemAdapter(ArrayList<HashMap<String, Object>> arrayList) {
            this.arrayData = arrayList;
        }

        @Override
        public redeemCodeItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.redeemcodeitem, null);
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(layoutParams);

            return new redeemCodeItemAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull redeemCodeItemAdapter.ViewHolder holder, int position) {
            View view = holder.itemView;

            sharedPreferences.edit().putString("savedcode", new Gson().toJson(redeemcodelist)).commit();

            final TextView redeemcodelabel = view.findViewById(R.id.redeemcodelabel);
            final boolean[] isLongClick = {true};
            redeemcodelabel.setText(arrayData.get(position).get("redeemcode").toString());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 300) {
                        for (int i = 0; i < redeemprofilelist.size(); i++) {
                            if (redeemprofilelist.get(i).get("active").toString().equals("true")) {
                                requestRedeemCode(redeemprofilelist.get(i).get("id").toString(), redeemprofilelist.get(i).get("server").toString(), redeemprofilelist.get(i).get("vc").toString(), arrayData.get(position).get("redeemcode").toString(), redeemprofilelist.get(i).get("username").toString());
                            }
                        }
                    }
                    lastClickTime = clickTime;
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isLongClick[0]) {
                        addUpdateDialog("UPDATE", position);
                        editiemdata.setText(arrayData.get(position).get("redeemcode").toString());
                    }
                    return false;
                }
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isLongClick[0] = true;
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            v.setTranslationY((event.getRawY() - initialTouchY));
                            if (Math.abs(event.getRawY() - initialTouchY) > 0) {
                                redeemcodeview.setLayoutFrozen(true);
                                isLongClick[0] = false;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            redeemcodeview.setLayoutFrozen(false);
                            if (Math.abs(event.getRawY() - initialTouchY) > v.getHeight() * 0.7) {
                                if (v.getTranslationY() > 1) {
                                    arrayData.remove(position);
                                    CodeItemAdapter.notifyItemRemoved(position);
                                    CodeItemAdapter.notifyItemRangeChanged(position, arrayData.size() - position);
                                    v.setTranslationY(0);
                                } else if (v.getTranslationY() < 0) {
                                    arrayData.remove(position);
                                    CodeItemAdapter.notifyItemRemoved(position);
                                    CodeItemAdapter.notifyItemRangeChanged(position, arrayData.size() - position);
                                    v.setTranslationY(0);
                                }
                                sharedPreferences.edit().putString("savedcode", new Gson().toJson(redeemcodelist)).commit();
                            } else {
                                v.setTranslationY(0);
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return arrayData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    private class DeviceStatusTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            statusnetworkspeed.setText(String.valueOf(getIternetPing()).concat("ms"));
            statusnetworktype.setText(getNetworkType());
            statusbatterytemperature.setText(String.valueOf((long) getBatteryTemperature()).concat("°C"));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            statusdevicefps.setText(String.valueOf((long) screenFPS()));
        }
    }
}