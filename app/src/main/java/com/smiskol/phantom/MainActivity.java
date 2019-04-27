package com.smiskol.phantom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.jcraft.jsch.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements WelcomeFragment.OnFragmentInteractionListener, ControlsFragment.OnFragmentInteractionListener {
    SharedPreferences preferences;
    CustomSeekBar steerSeekBar;
    TextView steerTextView;
    TextView speedTextView;
    SeekBar accelSeekBar;
    TextView listeningTextView;
    Switch connectSwitch;
    EditText ipEditText;
    TextView titleTextView;
    TextInputLayout ipEditTextLayout;
    CardView steerCard;
    CardView cardViewMain;
    CardView accelCard;
    LinearLayout welcomeLayoutTitle;
    LinearLayout welcomeLayout;
    LinearLayout connectLayout;
    LinearLayout steerLayout;
    LinearLayout accelLayout;
    Button goButton;
    LinearLayout holdButton;
    ImageButton speedPlusButton;
    ImageButton speedSubButton;
    android.support.v7.widget.Toolbar toolbar;
    ViewPager viewPager;
    ViewPagerAdapter adapter;
    Typeface semibold;
    Typeface regular;
    TabLayout tabLayout;
    TextView alertTitle;
    Session eonSession;
    String eonIP;
    SSHClass sshClass = new SSHClass();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        steerSeekBar = findViewById(R.id.steerSeekBarNew);
        steerTextView = findViewById(R.id.steerTextViewNew);
        speedTextView = findViewById(R.id.speedTextView);
        accelSeekBar = findViewById(R.id.accelSeekBar);
        listeningTextView = findViewById(R.id.connectedTextNew);
        connectSwitch = findViewById(R.id.connectSwitchNew);
        ipEditTextLayout = findViewById(R.id.ipEditTextLayoutNew);
        ipEditText = findViewById(R.id.ipEditTextNew);
        steerCard = findViewById(R.id.steerCard);
        accelCard = findViewById(R.id.accelCard);
        steerLayout = findViewById(R.id.steerLayout);
        accelLayout = findViewById(R.id.accelLayout);
        titleTextView = findViewById(R.id.titleText);
        cardViewMain = findViewById(R.id.cardViewMain);
        welcomeLayout = findViewById(R.id.welcomeLayout);
        welcomeLayoutTitle = findViewById(R.id.welcomeLayoutTitle);
        connectLayout = findViewById(R.id.connectLayout);
        goButton = findViewById(R.id.goButton);
        speedPlusButton = findViewById(R.id.speedPlusButton);
        speedSubButton = findViewById(R.id.speedSubButton);
        holdButton = findViewById(R.id.holdButton);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        viewPager = findViewById(R.id.pager);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        alertTitle = findViewById(R.id.alertTextTitle);
        semibold = ResourcesCompat.getFont(this, R.font.product_bold);
        regular = ResourcesCompat.getFont(this, R.font.product_regular);

        getSupportActionBar().hide();

        doWelcome();
        connectSwitch.setChecked(false);

        new CheckUpdate().execute();
    }

    private class CheckUpdate extends AsyncTask<Void, String, String> {
        protected String doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("https://api.github.com/repos/ShaneSmiskol/phantom-app/commits");
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");

                }

                return buffer.toString();


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                String latestCommitSHA = new JSONArray(result).getJSONObject(1).getString("sha");
                //String latestCommitSHA = new JSONObject(result).getJSONObject("object").getString("sha");
                if (getString(R.string.current_commit).equals(latestCommitSHA)) {
                    makeSnackbar("You're on the latest commit!");
                } else {
                    outOfDate();
                }
            } catch (Exception e) {
                makeSnackbar("Unable to check for updates!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    Long goDown = Long.valueOf(0);
    Long goDuration = Long.valueOf(0);
    Boolean holdMessage = false;
    Boolean buttonHeld = false;
    Boolean runPhantomThread = true;
    Integer runningProcesses = 0;
    Integer maxProcesses = 1;
    Double previousSteer = 0.0;
    Double steeringAngle = 0.0;
    Double desiredSpeed = 5.0;
    Boolean trackingSteer = false;
    Boolean steerLetGo = false;

    public class PhantomThread extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... v) {
            runPhantomThread = true;
            previousSteer = steeringAngle;
            System.out.println("started phantom thread");
            while (true) {
                System.out.println(runningProcesses);
                try {
                    Thread.sleep(250);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (runningProcesses > maxProcesses) {
                    while (true) {
                        System.out.println("waiting for excess processes to finish");
                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (runningProcesses <= maxProcesses) {
                            break;
                        }
                    }
                }

                if ((System.currentTimeMillis() - goDown) > 200 && buttonHeld) {
                    if (!previousSteer.equals(steeringAngle) && trackingSteer) {
                        previousSteer = steeringAngle;
                        publishProgress("move_with_wheel");
                    } else if (steerLetGo) {
                        previousSteer = steeringAngle;
                        steerLetGo = false;
                        publishProgress("move_with_wheel");

                    } else if (holdMessage) {
                        holdMessage = false;
                        publishProgress("move");
                    }
                } else if (!buttonHeld && !previousSteer.equals(steeringAngle) && trackingSteer) {
                    previousSteer = steeringAngle;
                    publishProgress("wheel");
                } else if (!buttonHeld && steerLetGo) {
                    previousSteer = steeringAngle;
                    steerLetGo = false;
                    publishProgress("wheel");
                }
                if (!runPhantomThread) {
                    return true;
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... method) {
            if (method[0].equals("move") || method[0].equals("move_with_wheel")) {
                String[] params = new String[]{"true", String.valueOf(desiredSpeed * 0.44704), String.valueOf(steeringAngle), "0", method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else { //must be wheel update
                String[] params = new String[]{"true", "0", String.valueOf(steeringAngle), "0", method[0]};
                new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            System.out.println("stopped phantom thread");
            if (runPhantomThread) {
                makeSnackbar("Lost connection to the EON!");
                doDisable();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setUpHoldButton() {
        holdButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("move button down");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.startTransition(175);
                    goDown = System.currentTimeMillis();
                    holdMessage = true;
                    buttonHeld = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    System.out.println("move button up");
                    TransitionDrawable transition = (TransitionDrawable) holdButton.getBackground();
                    transition.reverseTransition(175);
                    holdMessage = false;
                    buttonHeld = false;
                    goDuration = System.currentTimeMillis() - goDown;
                    if (goDuration < 200) {
                        makeSnackbar("You must hold button down for acceleration!");
                    } else {
                        String[] params = new String[]{"true", "0.0", String.valueOf(steeringAngle), "0", "brake"};
                        new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
                    }
                    System.out.println("Button held for " + goDuration + " ms");
                }
                return false;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setUpMainCard() {
        ipEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ipEditText.setFocusable(true);
                ipEditText.setFocusableInTouchMode(true);

                return false;
            }
        });
        ipEditText.setText(preferences.getString("eonIP", ""));
    }

    public void doWelcome() {
        if (preferences.getBoolean("welcome", true)) {
            welcomeDialog();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (preferences.getBoolean("welcome", true)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        writeSupportingFiles();
                        preferences.edit().putBoolean("welcome", false).apply();
                        successDialog();
                    } else {
                        uhohDialog();
                    }
                }
                return;
            }
        }
    }

    public void outOfDate() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Out of date!")
                .setMessage("You're on an old version of Phantom. Updating is highly recommended.")
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShaneSmiskol/phantom-app/blob/master/README.md"));
                        startActivity(browserIntent);
                    }

                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void successDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Granted!")
                .setMessage("You're all set now, Captain.")
                .setPositiveButton("Rad", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }

                }).show();
        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void warningDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Warning")
                .setMessage("This is extremely experimental software you are about to try. You must accept all responsibility and be vigilant in controlling your car in the event of any malfunction. Proceed at your own risk.")
                .setPositiveButton("Groovy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putBoolean("warning", true).apply();
                    }

                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void uhohDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Uh oh!")
                .setMessage("You've denied the storage permission. We need this to write the EON private key to a file so we can make connections over SSH.")
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermission();
                    }
                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void welcomeDialog() {
        AlertDialog successDialog = new AlertDialog.Builder(this).setTitle("Welcome!")
                .setMessage("Phantom is an experimental app that can remotely control your car's acceleration and wheel angle via SSH. We will now request the data permission, required to access your EON.")
                .setPositiveButton("Sick", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermission();
                    }
                }).setCancelable(false)
                .show();

        int titleText = getResources().getIdentifier("alertTitle", "id", "android");
        ((TextView) successDialog.getWindow().findViewById(titleText)).setTypeface(semibold);
        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void startListeners() {
        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                if (isChecked) {
                    if (!ipEditText.getText().toString().equals("") && ipEditText.getText().toString().length() >= 7) {
                        ipEditText.setEnabled(false);
                        connectSwitch.setEnabled(false);
                        listeningTextView.setText("Testing connection...");
                        makeSnackbar("Testing connection...");
                        String[] params = new String[]{"true", "0.0", "0", "0", "enable"};
                        new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //enable phantom mode on EON
                    } else {
                        connectSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                        ipEditTextLayout.startAnimation(mShakeAnimation);
                    }


                } else {
                    connectSwitch.setEnabled(false);
                    connectSwitch.setChecked(true);
                    String[] params = new String[]{"false", "0.0", "0", "0", "disable"};
                    new sendPhantomCommand().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params); //disable phantom mode on EON
                    listeningTextView.setText("Disabling...");
                }
            }
        });
    }

    public Boolean openSession(String eonIP) {
        try {
            eonSession = sshClass.getSession(MainActivity.this, eonIP);
            return true;
        } catch (Exception e) {
            System.out.println("HERE SADLY");
            e.printStackTrace();
            return false;
        }
    }

    public void doSuccessful() {
        adapter.setViewCount(2);
        viewPager.setCurrentItem(1);
        tabLayout.setVisibility(View.VISIBLE);
        new PhantomThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (!preferences.getBoolean("warning", false)) {
            warningDialog();
        }

        //connectSwitch.setChecked(true);
        //connectSwitch.setEnabled(true);
        //preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
        //makeSnackbar("Connected!");
        //listeningTextView.setText("Connected!");
        //ipEditTextLayout.setVisibility(View.GONE);
        //cardViewMain.animate().translationY(700).setDuration(500).setInterpolator(new FastOutSlowInInterpolator()).start();
        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //steerCard.setVisibility(View.VISIBLE);
                steerLayout.setVisibility(View.VISIBLE);
                //accelCard.setVisibility(View.VISIBLE);
                welcomeLayoutTitle.setVisibility(View.GONE);
                welcomeLayout.setVisibility(View.GONE);
                connectLayout.setVisibility(View.GONE);
                holdButton.setVisibility(View.VISIBLE);
                accelLayout.setVisibility(View.VISIBLE);
                titleTextView.setVisibility(View.VISIBLE);
            }
        }, 100);*/
    }

    public class sendPhantomCommand extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            runningProcesses += 1;
            Boolean result = sshClass.sendPhantomCommand(eonSession, ipEditText.getText().toString(), params[0], params[1], params[2], params[3]);
            return new String[]{result.toString(), params[4]};
        }

        @Override
        protected void onPostExecute(String... result) {
            runningProcesses -= 1;
            if (result[0].equals("true")) {
                if (result[1].equals("enable")) {
                    doSuccessful();
                    makeSnackbar("Enabled Phantom!");
                } else if (result[1].equals("disable")) {
                    doDisable();
                    makeSnackbar("Disabled Phantom!");
                } else if (result[1].equals("brake")) {
                    makeSnackbar("Stopping car!");
                    System.out.println("stopping car");
                } else if (result[1].equals("move")) {
                    System.out.println("moving update");
                    makeSnackbar("Moving car...");
                } else if (result[1].equals("wheel")) {
                    System.out.println("wheel update");
                } else if (result[1].equals("move_with_wheel")) {
                    System.out.println("move+wheel update");
                }
            } else {
                if (result[1].equals("disable")) {
                    connectSwitch.setEnabled(true);
                    makeSnackbar("Error disabling Phantom mode!");
                } else {
                    doDisable();
                    makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
                }
            }
        }
    }


    public void doDisable() {
        viewPager.setCurrentItem(0);
        adapter.setViewCount(1);
        tabLayout.setVisibility(View.GONE);
        runPhantomThread = false;
        //connectSwitch.setChecked(false);
        //connectSwitch.setEnabled(true);
        //listeningTextView.setText("Not Connected");
        //steerCard.setVisibility(View.GONE);
        //steerLayout.setVisibility(View.GONE);
        //accelCard.setVisibility(View.GONE);
        //welcomeLayoutTitle.setVisibility(View.VISIBLE);
        //welcomeLayout.setVisibility(View.VISIBLE);
        //connectLayout.setVisibility(View.VISIBLE);
        //holdButton.setVisibility(View.GONE);
        //accelLayout.setVisibility(View.GONE);
        //titleTextView.setVisibility(View.GONE);
        //ipEditTextLayout.setVisibility(View.VISIBLE);
        //cardViewMain.animate().translationY(0).setDuration(500).setInterpolator(new FastOutSlowInInterpolator()).start();
        //ipEditText.setEnabled(true);
    }

    public void writeSupportingFiles() {
        try {
            File file = new File(getFilesDir(), "eon_id.ppk");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

                String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                        "MIIEogIBAAKCAQEAvol16t9E6vieTSmrdylhws3JsGeeZxoeloIAKhAmuQmrAZTP\n" +
                        "VXkTqVbt23gPuYdDIm0YGw+AzLVVwbeoBL2fJ3dOBO3iwPS02chQ2e0pEjlY+KFz\n" +
                        "kLE9BpyZiqwEluSrJU1qlc036NlwrWftNOIpC8ZshXgTvDTnBK1taWvIBXUA06B/\n" +
                        "RawO5IMrInP11REkzqHu15c0aHv3mWnBEPo7Z5hXdtQOGhAA5JNNAIY69LimiYi1\n" +
                        "AD2rcbNonCF1qYGLX6qrWihdt8EretTk7unAMF2zlq95viFEkVDtCEcxCEEt89Y3\n" +
                        "3dbL4M0oEksGdS4Y+AKCsSBACHPKiazuLspgiQIDAQABAoIBAQCEhXr8RxnaC92e\n" +
                        "cZMOqDuUkCjthsRHlYUczYJrvxwPqsfDq8qg+jtQlmONN+5H7eolsZcIizncJ2tj\n" +
                        "9ubnlTNy8anUB9ikuA5pQsfpKuhcAoL9Ot30DzIQvS6Vopr2kEjxAu1VD40JaOLT\n" +
                        "2OrE02AVDodANYoUZv8e47irkAlosQqvAvw1ZwdV+Jho/lt5yXOU8FSbYCW24ga6\n" +
                        "uj1q4bwf96ppMR0S+3VNkgW9ojURdSy2N9HScf3A+91AyjR65a7I5N1CXNvTKePz\n" +
                        "JWnSr1JEajcJWMUrgLSVdJ2d/ohZC7N2nUkx3SaQpUHq+OUedaxQ5VbA89mQaW/4\n" +
                        "UTUaBg7hAoGBAOgNRIsS6u0GDod3G14cod1uJKVbwPxT3yh9TjMtzjTg/2PTmvjP\n" +
                        "8LYVtcEqES9p/rriFuTgIUyLyBIr4+mwGbE097cK7zq72Lva8fWpZ+KfAYcr3Y3l\n" +
                        "uJEu0/BT+aJei6DrdrEz909SzriTzrkLzo5SjyiDId3N0RTVk5xszD2tAoGBANIz\n" +
                        "Yjy8T9wNp619JHigyPlR7rzmPPIHYdFuQbizC6LziA5PWkBSGwWzLltTk4xyr/TS\n" +
                        "vi68PmGwhajhn9XVP1DeYEshPJV/0BbFBlKlGcee+JyWZziHMtzjTp0C3LxwEE6C\n" +
                        "xQBlHez1oD9wrR5LfYRL9pKFMC+L6IpEz9bvRpHNAoGBANmqaFsT2a2Pet1yygcT\n" +
                        "UHnGMTWyxWlquu7d6xZypvRPAQCAouM1GhOSdbTFYu1YvYpLPTJfUpzcmUUCSn0P\n" +
                        "pGnmx125MgGj5n7/tuq6hym6ANLsQJwzmVcF1+OcwZKeoNbHR8ScfCS6BhJ5AvXs\n" +
                        "r0otAv/7US8fOjoSxK18GHDZAn9YrVTESq1mKFyU1DaOrUYb6HTPPFJ5yKN7twgC\n" +
                        "44YFOLgtUUzB1eGQhgcIgDm/BqM0pbOWA9RNYisBFC5aB5yugSIej+b/Kuyern/8\n" +
                        "XaqCjI5VgR4Kuv66MSr5EjwNQzmd5Y02nXIChZ0VJnPiU/af2WwsZAPwCxYPPvhv\n" +
                        "tIIRAoGAPLxtzP7rcHi76uESO5e1O2/otgWo3ytjpszYv8boH3i42OpNrX0Bkbr+\n" +
                        "qaU43obY4trr4A1pIIyVID32aYq9yEbFTFIhYJaFhhxEzstEL3OQMLakyRS0w9Vs\n" +
                        "2trgYpUlSBLIOmPNxonJIfnozphLGOnKNe0RWgGR8BnwhRYzu+k=\n" +
                        "-----END RSA PRIVATE KEY-----\n";

                osw.write(privateKey);

                osw.close();
                System.out.println("File written");
            } else {
                System.out.println("File already written");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            makeSnackbar("Error writing support files.");
        }
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
        tv.setTypeface(regular);
        snackbar.show();
    }

}