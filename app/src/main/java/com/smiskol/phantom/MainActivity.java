package com.smiskol.phantom;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {
    SharedPreferences preferences;
    CustomSeekBar steerSeekBar;
    TextView steerTextView;
    TextView accelTextView;
    SeekBar accelSeekBar;
    TextView listeningTextView;
    Switch connectSwitch;
    EditText ipEditText;
    TextView titleTextView;
    TextInputLayout ipEditTextLayout;
    CardView steerCard;
    CardView cardViewMain;
    CardView accelCard;
    Button goButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        steerSeekBar = findViewById(R.id.steerSeekBar);
        steerTextView = findViewById(R.id.steerTextView);
        accelTextView = findViewById(R.id.accelTextView);
        accelSeekBar = findViewById(R.id.accelSeekBar);
        listeningTextView = findViewById(R.id.connectedText);
        connectSwitch = findViewById(R.id.connectSwitch);
        ipEditTextLayout = findViewById(R.id.ipEditTextLayout);
        ipEditText = findViewById(R.id.ipEditText);
        steerCard = findViewById(R.id.steerCard);
        accelCard = findViewById(R.id.accelCard);
        titleTextView = findViewById(R.id.titleText);
        cardViewMain = findViewById(R.id.cardViewMain);
        goButton = findViewById(R.id.goButton);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        doWelcome();
        startListeners();
        setUpMainCard();
        setUpButton();

        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                steerTextView.setText(progress - 20 + "°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        accelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                accelTextView.setText(progress / 10.0 + " mph");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    Long goDown = Long.valueOf(0);
    Long goDuration = Long.valueOf(0);
    Boolean buttonHeld = false;
    Boolean holdMessage=true;

    public void setUpButton() {
        new Thread(new Runnable() { //TODO: move to async task later
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    System.out.println(System.currentTimeMillis() - goDown);
                    if ((System.currentTimeMillis() - goDown) > 150 && holdMessage) {
                        holdMessage = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeSnackbar("Moving car...");
                            }
                        });
                    }
                }
            }
        }).start();

        goButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    goDown = System.currentTimeMillis();
                    buttonHeld = true;
                    holdMessage = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    buttonHeld = false;
                    holdMessage = false;
                    goDuration = System.currentTimeMillis() - goDown;
                    if (goDuration < 150) {
                        makeSnackbar("You must hold button down for acceleration!");
                    } else {
                        //makeSnackbar("Button held for " + goDuration + " ms.");
                        makeSnackbar("Stopping car!");
                    }
                    System.out.println(buttonHeld);
                    System.out.println(goDuration);
                }
                return false;
            }
        });
    }

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
                        new AlertDialog.Builder(this).setTitle("Granted!")
                                .setMessage("Awesome! Permission granted. I've written the EON private key to a file for us to read when we make connections to the EON later.")
                                .setPositiveButton("Cool", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                    } else {
                        new AlertDialog.Builder(this).setTitle("Uh oh!")
                                .setMessage("You've denied the storage permission. We need this to write the EON private key to a file so we can make connections over SSH. Please accept the permission.")
                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermission();
                                    }
                                })
                                .show();
                    }
                }
                return;
            }
        }
    }

    public void welcomeDialog() {
        new AlertDialog.Builder(this).setTitle("Welcome!")
                .setMessage("Phantom is an app that can remotely control your car's acceleration and turning angle via SSH. We will now request the data permission, required to access your EON.")
                .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setCancelable(false)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermission();
                    }
                })
                .show();
    }

    public void startListeners() {
        /*addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RunSpeedChange().execute(8);
            }
        });

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RunSpeedChange().execute(-8);
            }
        });*/

        //final Intent listenerService = new Intent(MainActivity.this, ListenerService.class);

        connectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!ipEditText.getText().toString().equals("") && ipEditText.getText().toString().length() >= 7) {
                        ipEditText.setEnabled(false);
                        listeningTextView.setText("Testing connection");
                        makeSnackbar("Testing connection...");
                        //new CheckEON().execute();
                        testConnectionSuccessful();
                    } else {
                        connectSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                        ipEditTextLayout.startAnimation(mShakeAnimation);
                    }


                } else {
                    //stopService(listenerService);
                    listeningTextView.setText("Not Connected");
                    steerCard.setVisibility(View.GONE);
                    accelCard.setVisibility(View.GONE);
                    titleTextView.setVisibility(View.GONE);
                    ipEditTextLayout.setVisibility(View.VISIBLE);
                    cardViewMain.animate().translationY(0).setDuration(500).setInterpolator(new FastOutSlowInInterpolator()).start();
                    makeSnackbar("Disconnected!");
                    ipEditText.setEnabled(true);
                }
            }
        });
    }

    public void testConnectionSuccessful(){
        preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
        makeSnackbar("Connected!");
        listeningTextView.setText("Connected!");
        ipEditTextLayout.setVisibility(View.GONE);
        cardViewMain.animate().translationY(700).setDuration(500).setInterpolator(new FastOutSlowInInterpolator()).start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                steerCard.setVisibility(View.VISIBLE);
                accelCard.setVisibility(View.VISIBLE);
                titleTextView.setVisibility(View.VISIBLE);
            }
        }, 100);
                /*if (!preferences.getBoolean("warning", false)) {
                    warningDialog();
                }*/
    }

    public class CheckEON extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... v) {
            return new SSHClass().testConnection(MainActivity.this, ipEditText.getText().toString());
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                testConnectionSuccessful();
            } else {
                connectSwitch.setChecked(false);
                makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
            }
        }
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

        try {
            File file = new File(getFilesDir(), "controller_listener.py");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

                String controllerListener = "import sys\n" +
                        "live_controller_file = '/data/live_controller_file'\n" +
                        "\n" +
                        "def write_file(a):\n" +
                        "    try:\n" +
                        "        with open(live_controller_file, 'r') as speed:\n" +
                        "            modified_speed=float(speed.read())+a\n" +
                        "        with open(live_controller_file, 'w') as speed:\n" +
                        "            speed.write(str(modified_speed))\n" +
                        "    except: #in case file doesn't exist or is empty\n" +
                        "        with open(live_controller_file, 'w') as speed:\n" +
                        "            speed.write(str(28.0))\n" +
                        "\n" +
                        "if __name__ == \"__main__\":\n" +
                        "    write_file(int(sys.argv[1]))";

                osw.write(controllerListener);

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

        snackbar.show();
    }

}
